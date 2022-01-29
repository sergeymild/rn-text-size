#import "RandomValuesJsiHelper.h"
#import "react-native-random-values-jsi-helper.h"
#import <React/RCTBlobManager.h>
#import <React/RCTBridge+Private.h>
#import <jsi/jsi.h>
#import "TypedArray.hpp"

#import <memory>
#import "RNTextSize.h"

using namespace facebook;



constexpr const char *OnJSRuntimeDestroyPropertyName = "__RandomValuesOnJsRuntimeDestroy";

void registerOnJSRuntimeDestroy(jsi::Runtime &runtime) {
    runtime.global().setProperty(
                               runtime,
                               OnJSRuntimeDestroyPropertyName,
                               jsi::Object::createFromHostObject(
                                   runtime, std::make_shared<InvalidateCacheOnDestroy>(runtime)));
}

@implementation RandomValuesJsiHelper

jsi::Object convertNSDictionaryToJSIObject(jsi::Runtime &runtime, NSDictionary *value)
{
  jsi::Object result = jsi::Object(runtime);
  for (NSString *k in value) {
    result.setProperty(runtime, [k UTF8String], convertObjCObjectToJSIValue(runtime, value[k]));
  }
  return result;
}

jsi::String convertNSStringToJSIString(jsi::Runtime &runtime, NSString *value)
{
  return jsi::String::createFromUtf8(runtime, [value UTF8String] ?: "");
}

jsi::Value convertNSNumberToJSINumber(jsi::Runtime &runtime, NSNumber *value)
{
  return jsi::Value([value doubleValue]);
}

jsi::Value convertObjCObjectToJSIValue(jsi::Runtime &runtime, id value)
{
  if (value == nil) {
    return jsi::Value::undefined();
  } else if ([value isKindOfClass:[NSString class]]) {
    return convertNSStringToJSIString(runtime, (NSString *)value);
  } else if ([value isKindOfClass:[NSNumber class]]) {
    return convertNSNumberToJSINumber(runtime, (NSNumber *)value);
  }
  return jsi::Value::undefined();
}


RCT_EXPORT_MODULE()

RCT_EXPORT_BLOCKING_SYNCHRONOUS_METHOD(install) {
    
    NSLog(@"Installing crypto.getRandomValues polyfill Bindings..."); 
    RCTBridge* bridge = [RCTBridge currentBridge];
    RCTCxxBridge* cxxBridge = (RCTCxxBridge*)bridge;
    if (cxxBridge == nil) {
        return @false;
    }
    auto jsiRuntime = (jsi::Runtime*) cxxBridge.runtime;
    
    if (jsiRuntime == nil) {
        return @false;
    }
    
    auto& runtime = *jsiRuntime;

    registerOnJSRuntimeDestroy(runtime);
    
    auto getRandomValues = jsi::Function::createFromHostFunction(runtime,
                                                                 jsi::PropNameID::forUtf8(runtime, "getRandomValues"),
                                                                 1,
                                                                 [](jsi::Runtime& runtime,
                                                                    const jsi::Value& thisArg,
                                                                    const jsi::Value* args,
                                                                    size_t count) -> jsi::Value {
        auto byteLength = args[0].asNumber();

        NSMutableData *data = [NSMutableData dataWithLength:byteLength];
        int result = SecRandomCopyBytes(kSecRandomDefault, byteLength, data.mutableBytes);
        if (result != errSecSuccess) {
        }
        
        auto typedArray = TypedArray<TypedArrayKind::Uint8Array>(runtime, byteLength);
        auto arrayBuffer = typedArray.getBuffer(runtime);
        memcpy(arrayBuffer.data(runtime), data.bytes, data.length);
        return typedArray;
    });
    
    
    auto measureText = jsi::Function::createFromHostFunction(runtime,
                                                                 jsi::PropNameID::forUtf8(runtime, "getRandomValues"),
                                                                 3,
                                                                 [](jsi::Runtime& runtime,
                                                                    const jsi::Value& thisArg,
                                                                    const jsi::Value* args,
                                                                    size_t count) -> jsi::Value {
        auto rawText = args[0].asString(runtime).utf8(runtime);
        auto fontSize = args[1].asNumber();
        auto width = args[2].asNumber();
        
        auto text = [NSString stringWithUTF8String:rawText.c_str()];

        auto result = [[[RNTextSize alloc] init] measure:@{
            @"text": text,
            @"width": [[NSNumber alloc] initWithDouble:width],
            @"fontSize": [[NSNumber alloc] initWithDouble:fontSize]
        }];
        
        return convertNSDictionaryToJSIObject(runtime, result);
    });

    runtime.global().setProperty(runtime, "getRandomValues", getRandomValues);
    runtime.global().setProperty(runtime, "measureText", measureText);
    
    return @true;
    
}
@end
