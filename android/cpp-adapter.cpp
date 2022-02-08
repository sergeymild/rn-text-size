#include <jni.h>
#include <jsi/jsi.h>
#include <string>
#include <vector>
#include <android/log.h>

using namespace facebook;

typedef u_int8_t byte;

constexpr const char *OnJSRuntimeDestroyPropertyName = "__RandomValuesOnJsRuntimeDestroy";



void install(jsi::Runtime &jsiRuntime, /*std::function<byte *(int size)> createRandomBytes,*/ JNIEnv *env) {
    auto measureText = jsi::Function::createFromHostFunction(
            jsiRuntime,
            jsi::PropNameID::forUtf8(jsiRuntime, "measureText"),
            1,
            [=](jsi::Runtime &runtime,
                const jsi::Value &thisArg,
                const jsi::Value *args,
                size_t count) -> jsi::Value {
                auto result = jsi::Object(runtime);

                auto params = args[0].asObject(runtime);

                jstring fontFamily;

                std::string rawString = params
                        .getProperty(runtime, "text")
                        .asString(runtime)
                        .utf8(runtime);

                if (params.hasProperty(runtime, "fontFamily")) {
                    std::string rawFontFamily = params
                            .getProperty(runtime, "fontFamily")
                            .asString(runtime)
                            .utf8(runtime);
                    fontFamily = env->NewStringUTF(rawFontFamily.c_str());
                }

                double fontSize = params
                        .getProperty(runtime, "fontSize")
                        .asNumber();

                double width = params
                        .getProperty(runtime, "maxWidth")
                        .asNumber();

                jstring text = env->NewStringUTF(rawString.c_str());

                jclass clazz = env->FindClass("com/reactnativerandomvaluesjsihelper/textSize/RNTextSizeModule");
                jmethodID method = env->GetStaticMethodID(clazz, "measure",
                                                          "(Ljava/lang/String;Ljava/lang/String;DD)[D");

                auto methodResult = env->CallStaticObjectMethod(clazz, method, text, fontFamily, fontSize, width);
                _jdoubleArray* jarray1 = reinterpret_cast<_jdoubleArray*>(methodResult);
                double *data = env->GetDoubleArrayElements(jarray1, NULL);
                // {height, width, lineCount, lastLineWidth}

                result.setProperty(runtime, "height", data[0]);
                result.setProperty(runtime, "width", data[1]);
                result.setProperty(runtime, "lineCount", data[2]);
                result.setProperty(runtime, "lastLineWidth", data[3]);


                env->DeleteLocalRef(fontFamily);
                env->DeleteLocalRef(text);
                env->ReleaseDoubleArrayElements(jarray1, data, 0);

                return result;
            });


    auto measureView = jsi::Function::createFromHostFunction(
            jsiRuntime,
            jsi::PropNameID::forUtf8(jsiRuntime, "measureView"),
            1,
            [=](jsi::Runtime &runtime,
                const jsi::Value &thisArg,
                const jsi::Value *args,
                size_t count) -> jsi::Value {
                auto result = jsi::Object(runtime);


                jclass clazz = env->FindClass("com/reactnativerandomvaluesjsihelper/textSize/RNTextSizeModule");
                jmethodID method = env->GetStaticMethodID(clazz, "measureView",
                                                          "(D)[D");
                auto methodResult = env->CallStaticObjectMethod(clazz, method, args[0].asNumber());
                _jdoubleArray* jarray1 = reinterpret_cast<_jdoubleArray*>(methodResult);
                double *data = env->GetDoubleArrayElements(jarray1, NULL);
                result.setProperty(runtime, "x", data[2]);
                result.setProperty(runtime, "y", data[3]);
                result.setProperty(runtime, "width", data[4]);
                result.setProperty(runtime, "height", data[5]);

                env->ReleaseDoubleArrayElements(jarray1, data, 0);

                return result;
            });

    jsiRuntime.global().setProperty(jsiRuntime, "measureView", std::move(measureView));
    jsiRuntime.global().setProperty(jsiRuntime, "measureText", std::move(measureText));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_reactnativerandomvaluesjsihelper_RandomValuesJsiHelperModule_nativeInstall(JNIEnv *env, jclass _, jlong jsiPtr, jobject instance) {
    auto runtime = reinterpret_cast<jsi::Runtime*>(jsiPtr);
    if (runtime) {
        install(*runtime, env);
    }
}
