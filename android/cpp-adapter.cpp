#include <jni.h>
#include <jsi/jsi.h>
#include <string>
#include <vector>
#include "../cpp/TypedArray.hpp"
#include <android/log.h>

using namespace facebook;

typedef u_int8_t byte;

constexpr const char *OnJSRuntimeDestroyPropertyName = "__RandomValuesOnJsRuntimeDestroy";

void registerOnJSRuntimeDestroy(jsi::Runtime &runtime) {
    runtime.global().setProperty(
            runtime,
            OnJSRuntimeDestroyPropertyName,
            jsi::Object::createFromHostObject(
                    runtime, std::make_shared<InvalidateCacheOnDestroy>(runtime)));
}

void install(jsi::Runtime &jsiRuntime, std::function<byte *(int size)> createRandomBytes, JNIEnv *env) {
    jclass clazz = env->FindClass("com/reactnativerandomvaluesjsihelper/textSize/RNTextSizeModule");
    jclass clazz = env->GetStaticMethodID(clazz, "measure", "(Ljava/lang/String;D;D)D;");

    registerOnJSRuntimeDestroy(jsiRuntime);


    auto measureText = jsi::Function::createFromHostFunction(
            jsiRuntime,
            jsi::PropNameID::forUtf8(jsiRuntime, "getRandomValues"),
            3,
            [=](jsi::Runtime &runtime,
                const jsi::Value &thisArg,
                const jsi::Value *args,
                size_t count) -> jsi::Value {

                _jobject *result = env->CallStaticObjectMethod(clazz, height, value, width);

                auto result = jsi::Object(runtime);
                result.setProperty(runtime, "height", 100);

                return result;
            });

    jsiRuntime.global().setProperty(jsiRuntime, "getRandomValues", std::move(getRandomValues));
    jsiRuntime.global().setProperty(jsiRuntime, "measureText", std::move(measureText));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_reactnativerandomvaluesjsihelper_RandomValuesJsiHelperModule_nativeInstall(JNIEnv *env, jclass _, jlong jsiPtr, jobject instance) {
    auto instanceGlobal = env->NewGlobalRef(instance);
    auto measureText = [=](int size) -> byte* {
        if (!env) throw std::runtime_error("JNI Environment is gone!");

        jclass clazz = env->GetObjectClass(instanceGlobal);
        /*
         * https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html
         * 4.3.2. Field Descriptors
         */
        jmethodID getRandomBytes = env->GetMethodID(clazz, "getRandomBytes", "(I)[B");
        auto b = (jbyteArray) env->CallObjectMethod(instanceGlobal,
                                                    getRandomBytes,
                                                    size);
        jboolean isCopy = true;
        jbyte* bytes = env->GetByteArrayElements(b, &isCopy);
        env->DeleteLocalRef(b);
        return reinterpret_cast<byte*>(bytes);
    };

    auto runtime = reinterpret_cast<jsi::Runtime*>(jsiPtr);
    if (runtime) {
        install(*runtime, createRandomBytes, env);
    }
}
