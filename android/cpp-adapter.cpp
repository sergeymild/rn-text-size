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

void install(jsi::Runtime &jsiRuntime, /*std::function<byte *(int size)> createRandomBytes,*/ JNIEnv *env) {
    registerOnJSRuntimeDestroy(jsiRuntime);


    auto measureText = jsi::Function::createFromHostFunction(
            jsiRuntime,
            jsi::PropNameID::forUtf8(jsiRuntime, "getRandomValues"),
            3,
            [=](jsi::Runtime &runtime,
                const jsi::Value &thisArg,
                const jsi::Value *args,
                size_t count) -> jsi::Value {
                auto result = jsi::Object(runtime);

                std::string rawString = args[0].asString(runtime).utf8(runtime);
                double fontSize = args[1].asNumber();
                double width = args[2].asNumber();
                jstring text = env->NewStringUTF(rawString.c_str());

                jclass clazz = env->FindClass("com/reactnativerandomvaluesjsihelper/textSize/RNTextSizeModule");
                jmethodID method = env->GetStaticMethodID(clazz, "measure",
                                                          "(Ljava/lang/String;DD)[D");

                auto methodResult = env->CallStaticObjectMethod(clazz, method, text, fontSize, width);
                _jdoubleArray* jarray1 = reinterpret_cast<_jdoubleArray*>(methodResult);
                double *data = env->GetDoubleArrayElements(jarray1, NULL);
                auto length = env->GetArrayLength(jarray1);
                int i;
                for (i = 0; i < length; i++) {
                    if (i == 0) {
                        result.setProperty(runtime, "height", data[i]);
                    }
                    if (i == 1) {
                        result.setProperty(runtime, "lineCount", data[i]);
                    }
                }


                env->DeleteLocalRef(text);
                env->ReleaseDoubleArrayElements(jarray1, data, 0);

                return result;
            });

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
        install(*runtime, env);
    }
}
