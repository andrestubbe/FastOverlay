#ifndef FASTOVERLAY_H
#define FASTOVERLAY_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_initEngine(JNIEnv* env, jclass clazz);
JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_disposeEngine(JNIEnv* env, jclass clazz);
JNIEXPORT jlong JNICALL Java_fastoverlay_FastOverlay_createWindow(JNIEnv* env, jclass clazz, jint x, jint y, jint width, jint height, jboolean transparent, jboolean topmost);
JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_destroyWindow(JNIEnv* env, jclass clazz, jlong windowId);
JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setWindowPosition(JNIEnv* env, jclass clazz, jlong windowId, jint x, jint y);
JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setWindowSize(JNIEnv* env, jclass clazz, jlong windowId, jint width, jint height);
JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_updateWindowBitmap(JNIEnv* env, jclass clazz, jlong windowId, jbyteArray rgba, jint width, jint height);
JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_setWindowVisible(JNIEnv* env, jclass clazz, jlong windowId, jboolean visible);

#ifdef __cplusplus
}
#endif

#endif // FASTOVERLAY_H
