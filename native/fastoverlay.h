#ifndef FASTOverlay_H
#define FASTOverlay_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// Export declarations (Matches fastoverlay.def)
JNIEXPORT void JNICALL Java_fastoverlay_FastOverlay_doSomethingNative(JNIEnv* env, jobject obj);

#ifdef __cplusplus
}
#endif

#endif // FASTOverlay_H
