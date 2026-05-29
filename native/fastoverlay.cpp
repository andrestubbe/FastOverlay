#define UNICODE
#define _UNICODE
#include "fastoverlay.h"
#include <windows.h>
#include <gdiplus.h>
#include <d3d11.h>
#include <d2d1_1.h>
#include <dcomp.h>
#include <dxgi1_2.h>
#include <mutex>
#include <map>
#include <vector>
#include <thread>
#include <condition_variable>
#include <jawt_md.h>

#pragma comment(lib, "gdiplus.lib")
#pragma comment(lib, "msimg32.lib")
#pragma comment(lib, "d3d11.lib")
#pragma comment(lib, "d2d1.lib")
#pragma comment(lib, "dcomp.lib")
#pragma comment(lib, "dxgi.lib")

using namespace Gdiplus;

// Engine state
ID3D11Device*            g_d3dDevice = nullptr;
ID3D11DeviceContext*     g_d3dCtx    = nullptr;
ID2D1Factory1*           g_d2dFactory = nullptr;
ID2D1Device*             g_d2dDevice = nullptr;
ID2D1DeviceContext*      g_d2dCtx = nullptr;
IDCompositionDevice*     g_dcompDevice = nullptr;
IDXGIFactory2*           g_dxgiFactory = nullptr;
ULONG_PTR                g_gdiplusToken;

std::mutex g_mutex;
long g_nextWindowId = 1;

struct OverlayWindow {
    std::mutex mutex;
    HWND hWnd;
    IDXGISwapChain1* swapChain;
    IDCompositionTarget* dcompTarget;
    IDCompositionVisual* dcompVisual;
    ID2D1Bitmap* d2dSourceBitmap;
    std::vector<unsigned int> premultiplyBuffer;
    std::thread* msgThread;
    int x, y, width, height;
};

std::map<long, OverlayWindow*> g_windows;

LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam) {
    if (message == WM_CLOSE) {
        DestroyWindow(hWnd);
        return 0;
    }
    if (message == WM_DESTROY) {
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProc(hWnd, message, wParam, lParam);
}

void InitGraphics() {
    D3D_FEATURE_LEVEL fl;
    D3D11CreateDevice(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr,
        D3D11_CREATE_DEVICE_BGRA_SUPPORT, nullptr, 0, D3D11_SDK_VERSION,
        &g_d3dDevice, &fl, &g_d3dCtx);

    D2D1_FACTORY_OPTIONS opts = {};
    D2D1CreateFactory(D2D1_FACTORY_TYPE_MULTI_THREADED, __uuidof(ID2D1Factory1), &opts, (void**)&g_d2dFactory);

    if (g_d3dDevice && g_d2dFactory) {
        IDXGIDevice* dxgiDevice = nullptr;
        g_d3dDevice->QueryInterface(&dxgiDevice);
        g_d2dFactory->CreateDevice(dxgiDevice, &g_d2dDevice);
        
        IDXGIAdapter* adapter = nullptr;
        dxgiDevice->GetAdapter(&adapter);
        adapter->GetParent(__uuidof(IDXGIFactory2), (void**)&g_dxgiFactory);
        adapter->Release();

        g_d2dDevice->CreateDeviceContext(D2D1_DEVICE_CONTEXT_OPTIONS_NONE, &g_d2dCtx);

        DCompositionCreateDevice(dxgiDevice, __uuidof(IDCompositionDevice), (void**)&g_dcompDevice);
        dxgiDevice->Release();
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_initEngine(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_d3dDevice) return;

    GdiplusStartupInput gdiplusStartupInput;
    GdiplusStartup(&g_gdiplusToken, &gdiplusStartupInput, NULL);

    WNDCLASSEXW wc{ sizeof(WNDCLASSEXW) };
    wc.lpfnWndProc = WndProc;
    wc.hInstance = GetModuleHandle(NULL);
    wc.lpszClassName = L"FastOverlayClass";
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = nullptr;
    RegisterClassExW(&wc);

    InitGraphics();
}

struct WindowCreationParams {
    int x, y, width, height;
    bool transparent, topmost;
    HWND outHwnd = nullptr;
    std::mutex mtx;
    std::condition_variable cv;
    bool ready = false;
};

JNIEXPORT jlong JNICALL Java_fastoverlay_FastOverlay_createWindow(JNIEnv* env, jclass, jint x, jint y, jint width, jint height, jboolean transparent, jboolean topmost) {
    std::lock_guard<std::mutex> lock(g_mutex);

    OverlayWindow* win = new OverlayWindow();
    win->x = x; win->y = y;
    win->width = width; win->height = height;
    win->dcompTarget = nullptr;
    win->dcompVisual = nullptr;
    win->d2dSourceBitmap = nullptr;
    win->swapChain = nullptr;

    WindowCreationParams params;
    params.x = x; params.y = y; params.width = width; params.height = height;
    params.transparent = transparent; params.topmost = topmost;

    win->msgThread = new std::thread([&params]() {
        DWORD exStyle = WS_EX_NOREDIRECTIONBITMAP | WS_EX_TOOLWINDOW;
        if (params.transparent) exStyle |= (WS_EX_TRANSPARENT | WS_EX_LAYERED);
        if (params.topmost) exStyle |= WS_EX_TOPMOST;

        HWND hWnd = CreateWindowExW(
            exStyle, L"FastOverlayClass", L"", WS_POPUP,
            params.x, params.y, params.width, params.height,
            nullptr, nullptr, GetModuleHandle(NULL), nullptr
        );

        {
            std::lock_guard<std::mutex> plock(params.mtx);
            params.outHwnd = hWnd;
            params.ready = true;
        }
        params.cv.notify_one();

        if (!hWnd) return;

        MSG msg;
        while (GetMessage(&msg, nullptr, 0, 0)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
    });

    std::unique_lock<std::mutex> waitLock(params.mtx);
    params.cv.wait(waitLock, [&params] { return params.ready; });
    
    if (!params.outHwnd) {
        win->msgThread->join();
        delete win->msgThread;
        delete win;
        return 0;
    }
    
    win->hWnd = params.outHwnd;

    // Create Flip-Model SwapChain
    if (g_dxgiFactory && g_d3dDevice) {
        DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
        swapChainDesc.Width = width;
        swapChainDesc.Height = height;
        swapChainDesc.Format = DXGI_FORMAT_B8G8R8A8_UNORM;
        swapChainDesc.Stereo = FALSE;
        swapChainDesc.SampleDesc.Count = 1;
        swapChainDesc.SampleDesc.Quality = 0;
        swapChainDesc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
        swapChainDesc.BufferCount = 2; // Required for Flip-Model
        swapChainDesc.Scaling = DXGI_SCALING_STRETCH;
        swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL;
        swapChainDesc.AlphaMode = DXGI_ALPHA_MODE_PREMULTIPLIED;
        swapChainDesc.Flags = 0;

        g_dxgiFactory->CreateSwapChainForComposition(g_d3dDevice, &swapChainDesc, nullptr, &win->swapChain);
    }

    // Setup DirectComposition
    if (g_dcompDevice && win->swapChain) {
        g_dcompDevice->CreateTargetForHwnd(win->hWnd, true, &win->dcompTarget);
        g_dcompDevice->CreateVisual(&win->dcompVisual);
        
        win->dcompVisual->SetContent(win->swapChain);
        win->dcompTarget->SetRoot(win->dcompVisual);
        g_dcompDevice->Commit();
    }

    long id = g_nextWindowId++;
    g_windows[id] = win;
    return id;
}

JNIEXPORT jlong JNICALL Java_fastoverlay_FastOverlay_createChildWindow(JNIEnv* env, jclass, jobject component, jint x, jint y, jint width, jint height) {
    JAWT awt;
    awt.version = JAWT_VERSION_1_4;
    if (JAWT_GetAWT(env, &awt) == JNI_FALSE) return 0;
    
    JAWT_DrawingSurface* ds = awt.GetDrawingSurface(env, component);
    if (!ds) return 0;
    
    jint lockStatus = ds->Lock(ds);
    if ((lockStatus & JAWT_LOCK_ERROR) != 0) {
        awt.FreeDrawingSurface(ds);
        return 0;
    }
    
    HWND parentHwnd = nullptr;
    JAWT_DrawingSurfaceInfo* dsi = ds->GetDrawingSurfaceInfo(ds);
    if (dsi) {
        JAWT_Win32DrawingSurfaceInfo* dsi_win = (JAWT_Win32DrawingSurfaceInfo*)dsi->platformInfo;
        if (dsi_win) {
            parentHwnd = dsi_win->hwnd;
        }
        ds->FreeDrawingSurfaceInfo(dsi);
    }
    
    ds->Unlock(ds);
    awt.FreeDrawingSurface(ds);
    
    if (!parentHwnd) return 0;

    std::lock_guard<std::mutex> lock(g_mutex);

    OverlayWindow* win = new OverlayWindow();
    win->x = x; win->y = y;
    win->width = width; win->height = height;
    win->dcompTarget = nullptr;
    win->dcompVisual = nullptr;
    win->d2dSourceBitmap = nullptr;
    win->swapChain = nullptr;

    WindowCreationParams params;
    params.x = x; params.y = y; params.width = width; params.height = height;
    params.transparent = false; params.topmost = false;

    win->msgThread = new std::thread([&params, parentHwnd]() {
        DWORD style = WS_CHILD | WS_VISIBLE | WS_CLIPCHILDREN | WS_CLIPSIBLINGS;
        DWORD exStyle = WS_EX_NOREDIRECTIONBITMAP;

        HWND hWnd = CreateWindowExW(
            exStyle, L"FastOverlayClass", L"", style,
            params.x, params.y, params.width, params.height,
            parentHwnd, nullptr, GetModuleHandle(NULL), nullptr
        );

        {
            std::lock_guard<std::mutex> plock(params.mtx);
            params.outHwnd = hWnd;
            params.ready = true;
        }
        params.cv.notify_one();

        if (!hWnd) return;

        MSG msg;
        while (GetMessage(&msg, nullptr, 0, 0)) {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
    });

    std::unique_lock<std::mutex> waitLock(params.mtx);
    params.cv.wait(waitLock, [&params] { return params.ready; });
    
    if (!params.outHwnd) {
        win->msgThread->join();
        delete win->msgThread;
        delete win;
        return 0;
    }
    
    win->hWnd = params.outHwnd;

    // Create Flip-Model SwapChain
    if (g_dxgiFactory && g_d3dDevice) {
        DXGI_SWAP_CHAIN_DESC1 swapChainDesc = {};
        swapChainDesc.Width = width;
        swapChainDesc.Height = height;
        swapChainDesc.Format = DXGI_FORMAT_B8G8R8A8_UNORM;
        swapChainDesc.Stereo = FALSE;
        swapChainDesc.SampleDesc.Count = 1;
        swapChainDesc.SampleDesc.Quality = 0;
        swapChainDesc.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
        swapChainDesc.BufferCount = 2;
        swapChainDesc.Scaling = DXGI_SCALING_STRETCH;
        swapChainDesc.SwapEffect = DXGI_SWAP_EFFECT_FLIP_SEQUENTIAL;
        swapChainDesc.AlphaMode = DXGI_ALPHA_MODE_PREMULTIPLIED;
        swapChainDesc.Flags = 0;

        g_dxgiFactory->CreateSwapChainForComposition(g_d3dDevice, &swapChainDesc, nullptr, &win->swapChain);
    }

    // Setup DirectComposition
    if (g_dcompDevice && win->swapChain) {
        g_dcompDevice->CreateTargetForHwnd(win->hWnd, true, &win->dcompTarget);
        g_dcompDevice->CreateVisual(&win->dcompVisual);
        
        win->dcompVisual->SetContent(win->swapChain);
        win->dcompTarget->SetRoot(win->dcompVisual);
        g_dcompDevice->Commit();
    }

    long id = g_nextWindowId++;
    g_windows[id] = win;
    return id;
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_destroyWindow(JNIEnv* env, jclass, jlong windowId) {
    OverlayWindow* win = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_windows.find(windowId);
        if (it != g_windows.end()) {
            win = it->second;
            g_windows.erase(it);
        }
    }

    if (win) {
        if (win->d2dSourceBitmap) win->d2dSourceBitmap->Release();
        if (win->dcompVisual) win->dcompVisual->Release();
        if (win->dcompTarget) win->dcompTarget->Release();
        if (win->swapChain) win->swapChain->Release();
        
        // Post WM_CLOSE to the window thread to break the message loop
        if (win->hWnd) {
            PostMessage(win->hWnd, WM_CLOSE, 0, 0);
        }
        
        if (win->msgThread && win->msgThread->joinable()) {
            win->msgThread->join();
            delete win->msgThread;
        }
        
        delete win;
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setWindowPosition(JNIEnv* env, jclass, jlong windowId, jint x, jint y) {
    OverlayWindow* win = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_windows.find(windowId);
        if (it != g_windows.end()) win = it->second;
    }
    if (win) {
        std::lock_guard<std::mutex> lock(win->mutex);
        win->x = x;
        win->y = y;
        SetWindowPos(win->hWnd, nullptr, x, y, 0, 0, SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setVisualOffset(JNIEnv* env, jclass, jlong windowId, jint x, jint y) {
    OverlayWindow* win = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_windows.find(windowId);
        if (it != g_windows.end()) win = it->second;
    }
    if (win) {
        std::lock_guard<std::mutex> lock(win->mutex);
        if (win->dcompVisual && g_dcompDevice) {
            win->dcompVisual->SetOffsetX((float)x);
            win->dcompVisual->SetOffsetY((float)y);
            g_dcompDevice->Commit();
        }
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setWindowProperties(JNIEnv* env, jclass, jlong windowId, jboolean alwaysOnTop, jboolean clickThrough) {
    OverlayWindow* win = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_windows.find(windowId);
        if (it != g_windows.end()) win = it->second;
    }
    if (win) {
        std::lock_guard<std::mutex> lock(win->mutex);
        LONG exStyle = GetWindowLong(win->hWnd, GWL_EXSTYLE);
        if (clickThrough) {
            exStyle |= (WS_EX_TRANSPARENT | WS_EX_LAYERED);
        } else {
            exStyle &= ~(WS_EX_TRANSPARENT | WS_EX_LAYERED);
        }
        SetWindowLong(win->hWnd, GWL_EXSTYLE, exStyle);
        
        HWND insertAfter = alwaysOnTop ? HWND_TOPMOST : HWND_NOTOPMOST;
        SetWindowPos(win->hWnd, insertAfter, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE | SWP_FRAMECHANGED);
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setWindowSize(JNIEnv* env, jclass, jlong windowId, jint width, jint height) {
    std::lock_guard<std::mutex> lock(g_mutex);
    auto it = g_windows.find(windowId);
    if (it != g_windows.end()) {
        OverlayWindow* win = it->second;
        if (win->width == width && win->height == height) return;
        
        win->width = width;
        win->height = height;
        SetWindowPos(win->hWnd, nullptr, 0, 0, width, height, SWP_NOMOVE | SWP_NOZORDER | SWP_NOACTIVATE);

        if (win->swapChain) {
            // Must release buffers before resizing
            if (win->d2dSourceBitmap) {
                win->d2dSourceBitmap->Release();
                win->d2dSourceBitmap = nullptr;
            }

            win->swapChain->ResizeBuffers(2, width, height, DXGI_FORMAT_B8G8R8A8_UNORM, 0);
        }
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_updateWindowBitmap(JNIEnv* env, jclass, jlong windowId, jintArray rgba, jint width, jint height) {
    OverlayWindow* win = nullptr;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_windows.find(windowId);
        if (it != g_windows.end()) win = it->second;
    }
    if (!win) return;
    
    std::lock_guard<std::mutex> lock(win->mutex);
    if (!win->swapChain || !g_d2dCtx) return;

    if (rgba && width > 0 && height > 0) {
        jsize arrayLen = env->GetArrayLength(rgba);
        if (arrayLen < width * height) {
            return; // Safety guard: array too small
        }
        
        jint* srcPixels = (jint*)env->GetPrimitiveArrayCritical(rgba, nullptr);
        if (!srcPixels) return;
        
        // Recreate source bitmap if needed (PREMULTIPLIED required for hardware D2D)
        if (!win->d2dSourceBitmap) {
            D2D1_BITMAP_PROPERTIES props = D2D1::BitmapProperties(
                D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_PREMULTIPLIED)
            );
            g_d2dCtx->CreateBitmap(D2D1::SizeU(width, height), nullptr, 0, &props, &win->d2dSourceBitmap);
        }

        if (win->d2dSourceBitmap) {
            // Extremely fast GPU upload (Direct from Java Array, GPU hardware premultiplies)
            D2D1_RECT_U rect = D2D1::RectU(0, 0, width, height);
            win->d2dSourceBitmap->CopyFromMemory(&rect, srcPixels, width * 4);
            
            // Acquire the current flip backbuffer
            IDXGISurface* dxgiBackBuffer = nullptr;
            win->swapChain->GetBuffer(0, IID_PPV_ARGS(&dxgiBackBuffer));
            
            ID2D1Bitmap1* currentTarget = nullptr;
            if (dxgiBackBuffer) {
                D2D1_BITMAP_PROPERTIES1 targetProps = D2D1::BitmapProperties1(
                    D2D1_BITMAP_OPTIONS_TARGET | D2D1_BITMAP_OPTIONS_CANNOT_DRAW,
                    D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_PREMULTIPLIED)
                );
                g_d2dCtx->CreateBitmapFromDxgiSurface(dxgiBackBuffer, &targetProps, &currentTarget);
                dxgiBackBuffer->Release();
            }

            if (currentTarget) {
                // Draw source to backbuffer
                g_d2dCtx->SetTarget(currentTarget);
                g_d2dCtx->BeginDraw();
                g_d2dCtx->Clear(D2D1::ColorF(0, 0, 0, 0));
                g_d2dCtx->DrawBitmap(win->d2dSourceBitmap);
                g_d2dCtx->EndDraw();

                g_d2dCtx->SetTarget(nullptr);
                currentTarget->Release();
            }

            // Present without sync interval to ensure it queues immediately
            win->swapChain->Present(0, DXGI_PRESENT_DO_NOT_WAIT);
            g_dcompDevice->Commit();
        }
        
        env->ReleasePrimitiveArrayCritical(rgba, srcPixels, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setWindowVisible(JNIEnv* env, jclass, jlong windowId, jboolean visible) {
    std::lock_guard<std::mutex> lock(g_mutex);
    auto it = g_windows.find(windowId);
    if (it != g_windows.end()) {
        ShowWindow(it->second->hWnd, visible ? SW_SHOW : SW_HIDE);
    }
}

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_disposeEngine(JNIEnv* env, jclass) {
    std::lock_guard<std::mutex> lock(g_mutex);
    
    for (auto& pair : g_windows) {
        OverlayWindow* win = pair.second;
        if (win->d2dSourceBitmap) win->d2dSourceBitmap->Release();
        if (win->dcompVisual) win->dcompVisual->Release();
        if (win->dcompTarget) win->dcompTarget->Release();
        if (win->swapChain) win->swapChain->Release();
        DestroyWindow(win->hWnd);
        delete win;
    }
    g_windows.clear();
    
    if (g_dcompDevice) { g_dcompDevice->Release(); g_dcompDevice = nullptr; }
    if (g_dxgiFactory) { g_dxgiFactory->Release(); g_dxgiFactory = nullptr; }
    if (g_d2dCtx) { g_d2dCtx->Release(); g_d2dCtx = nullptr; }
    if (g_d2dDevice) { g_d2dDevice->Release(); g_d2dDevice = nullptr; }
    if (g_d2dFactory) { g_d2dFactory->Release(); g_d2dFactory = nullptr; }
    if (g_d3dCtx) { g_d3dCtx->Release(); g_d3dCtx = nullptr; }
    if (g_d3dDevice) { g_d3dDevice->Release(); g_d3dDevice = nullptr; }
    
    GdiplusShutdown(g_gdiplusToken);
}
