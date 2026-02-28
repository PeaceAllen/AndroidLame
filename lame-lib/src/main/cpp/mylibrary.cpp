
#include "lame/lame.h"
#include <jni.h>

static lame_global_flags *glf = NULL;

extern "C"
JNIEXPORT void JNICALL
Java_com_fjz_android_mylibrary_LameUtil_init(JNIEnv *env, jobject thiz, jint in_sample_rate,
        jint out_channel, jint out_sample_rate,
        jint out_bitrate, jint quality) {
    if (glf != NULL) {
        lame_close(glf);
        glf = NULL;
    }
    glf = lame_init();
    lame_set_in_samplerate(glf, in_sample_rate);
    lame_set_num_channels(glf, out_channel);
    lame_set_out_samplerate(glf, out_sample_rate);
    lame_set_brate(glf, out_bitrate);
    lame_set_quality(glf, quality);
    lame_init_params(glf);
}



extern "C"
JNIEXPORT jint JNICALL
Java_com_fjz_android_mylibrary_LameUtil_encode(JNIEnv *env, jobject thiz,
        jshortArray buffer_left,
        jshortArray buffer_right,
        jint samples,
        jbyteArray mp3buf) {
    if (glf == NULL) return -1;

    // 1. 获取输入数据（PCM）和输出数据（MP3）的 native 指针
    jshort *j_buffer_left = env->GetShortArrayElements(buffer_left, NULL);
    jshort *j_buffer_right = NULL;
    if (buffer_right != NULL) {
        j_buffer_right = env->GetShortArrayElements(buffer_right, NULL);
    }

    jbyte *j_mp3buf = env->GetByteArrayElements(mp3buf, NULL);
    const jsize mp3buf_size = env->GetArrayLength(mp3buf);

    // 2. 调用 LAME 编码函数
    // 如果是单声道，lame_encode_buffer 的右声道传 NULL 即可
    int result = lame_encode_buffer(glf,
            j_buffer_left,
            j_buffer_right,
            samples,
            (unsigned char *) j_mp3buf,
            mp3buf_size);

    // 3. 释放 native 资源 (JNI_ABORT 表示不写回 Java 数组，因为 PCM 是只读的)
    env->ReleaseShortArrayElements(buffer_left, j_buffer_left, JNI_ABORT);
    if (buffer_right != NULL) {
        env->ReleaseShortArrayElements(buffer_right, j_buffer_right, JNI_ABORT);
    }
    // 注意：mp3buf 必须写回，所以最后参数传 0
    env->ReleaseByteArrayElements(mp3buf, j_mp3buf, 0);

    return result;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_fjz_android_mylibrary_LameUtil_flush(JNIEnv *env, jobject thiz, jbyteArray mp3buf) {
    if (glf == NULL) return -1;

    jbyte *j_mp3buf = env->GetByteArrayElements(mp3buf, NULL);
    const jsize mp3buf_size = env->GetArrayLength(mp3buf);

    // 获取最后的编码残余数据
    int result = lame_encode_flush(glf, (unsigned char *) j_mp3buf, mp3buf_size);

    env->ReleaseByteArrayElements(mp3buf, j_mp3buf, 0);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_fjz_android_mylibrary_LameUtil_close(JNIEnv *env, jobject thiz) {
    if (glf != NULL) {
        lame_close(glf);
        glf = NULL;
    }
}
