#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <pthread.h>
#include <stdlib.h>

#define LOG_TAG "ByeDPI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern int main(int argc, char **argv);
extern void clear_params(char *line, char **argv);
extern int server_fd;

static int g_proxy_running = 0;
static pthread_t g_proxy_thread;
static char *g_argv[64];

static void *proxy_thread_func(void *arg) {
    int argc = *(int*)arg;
    LOGI("Running ByeDPI main in thread");
    int result = main(argc, g_argv);
    LOGI("ByeDPI main returned: %d", result);
    g_proxy_running = 0;
    return NULL;
}

JNIEXPORT jint JNICALL
Java_io_element_android_x_dpi_ByeDpiProxy_nativeStart(JNIEnv *env, jobject thiz, jobjectArray args) {
    if (g_proxy_running) {
        LOGI("proxy already running");
        return -1;
    }

    int argc = (*env)->GetArrayLength(env, args);
    if (argc > 63) argc = 63;

    memset(g_argv, 0, sizeof(g_argv));

    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        if (arg) {
            const char *arg_str = (*env)->GetStringUTFChars(env, arg, 0);
            g_argv[i] = arg_str ? strdup(arg_str) : NULL;
            if (arg_str) (*env)->ReleaseStringUTFChars(env, arg, arg_str);
            (*env)->DeleteLocalRef(env, arg);
        }
    }

    LOGI("starting ByeDPI proxy with %d args", argc);
    for (int i = 0; i < argc; i++) {
        if (g_argv[i]) LOGI("  arg[%d]: %s", i, g_argv[i]);
    }

    g_proxy_running = 1;

    pthread_create(&g_proxy_thread, NULL, proxy_thread_func, &argc);

    return 0;
}

JNIEXPORT jint JNICALL
Java_io_element_android_x_dpi_ByeDpiProxy_nativeStop(JNIEnv *env, jobject thiz) {
    LOGI("stopping ByeDPI proxy, server_fd=%d", server_fd);
    if (server_fd > 0) {
        shutdown(server_fd, SHUT_RDWR);
        close(server_fd);
    }
    g_proxy_running = 0;
    return 0;
}

JNIEXPORT jint JNICALL
Java_io_element_android_x_dpi_ByeDpiProxy_nativeIsRunning(JNIEnv *env, jobject thiz) {
    return g_proxy_running ? 1 : 0;
}

JNIEXPORT jint JNICALL
Java_io_1github_1romanvht_1byedpi_1library_1server_1ByeDpiServer_startNativeProxy(JNIEnv *env, jobject thiz, jobjectArray args) {
    if (g_proxy_running) {
        LOGI("proxy already running");
        return -1;
    }

    int argc = (*env)->GetArrayLength(env, args);
    if (argc > 63) argc = 63;

    memset(g_argv, 0, sizeof(g_argv));

    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
        if (arg) {
            const char *arg_str = (*env)->GetStringUTFChars(env, arg, 0);
            g_argv[i] = arg_str ? strdup(arg_str) : NULL;
            if (arg_str) (*env)->ReleaseStringUTFChars(env, arg, arg_str);
            (*env)->DeleteLocalRef(env, arg);
        }
    }

    LOGI("starting ByeDPI proxy with %d args", argc);
    for (int i = 0; i < argc; i++) {
        if (g_argv[i]) LOGI("  arg[%d]: %s", i, g_argv[i]);
    }

    g_proxy_running = 1;

    pthread_create(&g_proxy_thread, NULL, proxy_thread_func, &argc);

    return 0;
}

JNIEXPORT jint JNICALL
Java_io_1github_1romanvht_1byedpi_1library_1server_1ByeDpiServer_stopNativeProxy(JNIEnv *env, jobject thiz) {
    LOGI("stopping ByeDPI proxy, server_fd=%d", server_fd);
    if (server_fd > 0) {
        shutdown(server_fd, SHUT_RDWR);
        close(server_fd);
    }
    g_proxy_running = 0;
    return 0;
}