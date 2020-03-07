
#include "tun2http.h"

JavaVM *jvm = NULL;
int pipefds[2];
pthread_t thread_id = 0;
pthread_mutex_t lock;
int loglevel = ANDROID_LOG_WARN;

extern int max_tun_msg;
extern struct ng_session *ng_session;

// JNI

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    log_android(ANDROID_LOG_INFO, "JNI load");

    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        log_android(ANDROID_LOG_INFO, "JNI load GetEnv failed");
        return -1;
    }

    // Raise file number limit to maximum
    struct rlimit rlim;
    if (getrlimit(RLIMIT_NOFILE, &rlim))
        log_android(ANDROID_LOG_WARN, "getrlimit error %d: %s", errno, strerror(errno));
    else {
        rlim_t soft = rlim.rlim_cur;
        rlim.rlim_cur = rlim.rlim_max;
        if (setrlimit(RLIMIT_NOFILE, &rlim))
            log_android(ANDROID_LOG_WARN, "setrlimit error %d: %s", errno, strerror(errno));
        else
            log_android(ANDROID_LOG_WARN, "raised file limit from %d to %d", soft, rlim.rlim_cur);
    }

    return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved) {
    log_android(ANDROID_LOG_INFO, "JNI unload");

    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_6) != JNI_OK)
        log_android(ANDROID_LOG_INFO, "JNI load GetEnv failed");
}

// JNI ServiceSinkhole

JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1init(JNIEnv *env, jobject instance) {
    loglevel = ANDROID_LOG_WARN;

    struct arguments args;
    args.env = env;
    args.instance = instance;
    init(&args);

    if (pthread_mutex_init(&lock, NULL))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_init failed");

    // Create signal pipe
    if (pipe(pipefds))
        log_android(ANDROID_LOG_ERROR, "Create pipe error %d: %s", errno, strerror(errno));
    else
        for (int i = 0; i < 2; i++) {
            int flags = fcntl(pipefds[i], F_GETFL, 0);
            if (flags < 0 || fcntl(pipefds[i], F_SETFL, flags | O_NONBLOCK) < 0)
                log_android(ANDROID_LOG_ERROR, "fcntl pipefds[%d] O_NONBLOCK error %d: %s",
                            i, errno, strerror(errno));
        }
}

JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1start(
        JNIEnv *env, jobject instance, jint tun, jboolean fwd53, jint rcode, jstring proxyIp, jint proxyPort) {

    const char *proxy_ip = (*env)->GetStringUTFChars(env, proxyIp, 0);

    max_tun_msg = 0;

    // Set blocking
    int flags = fcntl(tun, F_GETFL, 0);
    if (flags < 0 || fcntl(tun, F_SETFL, flags & ~O_NONBLOCK) < 0)
        log_android(ANDROID_LOG_ERROR, "fcntl tun ~O_NONBLOCK error %d: %s",
                    errno, strerror(errno));

    if (thread_id && pthread_kill(thread_id, 0) == 0)
        log_android(ANDROID_LOG_ERROR, "Already running thread %x", thread_id);
    else {
        jint rs = (*env)->GetJavaVM(env, &jvm);
        if (rs != JNI_OK)
            log_android(ANDROID_LOG_ERROR, "GetJavaVM failed");

        // Get arguments
        struct arguments *args = malloc(sizeof(struct arguments));
        // args->env = will be set in thread
        args->instance = (*env)->NewGlobalRef(env, instance);
        args->tun = tun;
        args->fwd53 = fwd53;
        args->rcode = rcode;
        strcpy(args->proxyIp, proxy_ip);
        args->proxyPort = proxyPort;


        // Start native thread
        int err = pthread_create(&thread_id, NULL, handle_events, (void *) args);
        if (err == 0)
            log_android(ANDROID_LOG_WARN, "Started thread %x", thread_id);
        else
            log_android(ANDROID_LOG_ERROR, "pthread_create error %d: %s", err, strerror(err));


    }

    (*env)->ReleaseStringUTFChars(env, proxyIp, proxy_ip);
}

JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1stop(
        JNIEnv *env, jobject instance, jint tun) {
    pthread_t t = thread_id;
    log_android(ANDROID_LOG_WARN, "Stop tun %d  thread %x", tun, t);
    if (t && pthread_kill(t, 0) == 0) {
        log_android(ANDROID_LOG_WARN, "Write pipe thread %x", t);
        if (write(pipefds[1], "x", 1) < 0)
            log_android(ANDROID_LOG_WARN, "Write pipe error %d: %s", errno, strerror(errno));
        else {
            log_android(ANDROID_LOG_WARN, "Join thread %x", t);
            int err = pthread_join(t, NULL);
            if (err != 0)
                log_android(ANDROID_LOG_WARN, "pthread_join error %d: %s", err, strerror(err));
        }

        clear();

        log_android(ANDROID_LOG_WARN, "Stopped thread %x", t);
    } else
        log_android(ANDROID_LOG_WARN, "Not running thread %x", t);
}

JNIEXPORT jint JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1get_1mtu(JNIEnv *env, jobject instance) {
    return get_mtu();
}


JNIEXPORT void JNICALL
Java_tun_proxy_service_Tun2HttpVpnService_jni_1done(JNIEnv *env, jobject instance) {
    log_android(ANDROID_LOG_INFO, "Done");

    clear();

    if (pthread_mutex_destroy(&lock))
        log_android(ANDROID_LOG_ERROR, "pthread_mutex_destroy failed");

    for (int i = 0; i < 2; i++)
        if (close(pipefds[i]))
            log_android(ANDROID_LOG_ERROR, "Close pipe error %d: %s", errno, strerror(errno));
}

// JNI Util

JNIEXPORT jstring JNICALL
Java_tun_utils_Util_jni_1getprop(JNIEnv *env, jclass type, jstring name_) {
    const char *name = (*env)->GetStringUTFChars(env, name_, 0);

    char value[PROP_VALUE_MAX + 1] = "";
    __system_property_get(name, value);

    (*env)->ReleaseStringUTFChars(env, name_, name);

    return (*env)->NewStringUTF(env, value);
}

static jmethodID midProtect = NULL;


int protect_socket(const struct arguments *args, int socket) {
    jclass cls = (*args->env)->GetObjectClass(args->env, args->instance);
    if (midProtect == NULL)
        midProtect = jniGetMethodID(args->env, cls, "protect", "(I)Z");

    jboolean isProtected = (*args->env)->CallBooleanMethod(
            args->env, args->instance, midProtect, socket);
    jniCheckException(args->env);

    if (!isProtected) {
        log_android(ANDROID_LOG_ERROR, "protect socket failed");
        return -1;
    }

    (*args->env)->DeleteLocalRef(args->env, cls);

    return 0;
}


jobject jniGlobalRef(JNIEnv *env, jobject cls) {
    jobject gcls = (*env)->NewGlobalRef(env, cls);
    if (gcls == NULL)
        log_android(ANDROID_LOG_ERROR, "Global ref failed (out of memory?)");
    return gcls;
}

jclass jniFindClass(JNIEnv *env, const char *name) {
    jclass cls = (*env)->FindClass(env, name);
    if (cls == NULL)
        log_android(ANDROID_LOG_ERROR, "Class %s not found", name);
    else
        jniCheckException(env);
    return cls;
}

jmethodID jniGetMethodID(JNIEnv *env, jclass cls, const char *name, const char *signature) {
    jmethodID method = (*env)->GetMethodID(env, cls, name, signature);
    if (method == NULL) {
        log_android(ANDROID_LOG_ERROR, "Method %s %s not found", name, signature);
        jniCheckException(env);
    }
    return method;
}

int jniCheckException(JNIEnv *env) {
    jthrowable ex = (*env)->ExceptionOccurred(env);
    if (ex) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        (*env)->DeleteLocalRef(env, ex);
        return 1;
    }
    return 0;
}
