#import "RandomValuesJsiHelper.h"
#import <React/RCTBlobManager.h>
#import <React/RCTBridge+Private.h>
#import <jsi/jsi.h>

#import <memory>
#import "RNTextSize.h"

using namespace facebook;

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
    
    auto measureText = jsi::Function::createFromHostFunction(runtime,
                                                                 jsi::PropNameID::forUtf8(runtime, "measureText"),
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
            @"fontSize": [[NSNumber alloc] initWithDouble:fontSize],
            @"usePreciseWidth": @true
        }];
        
        return convertNSDictionaryToJSIObject(runtime, result);
    });

    runtime.global().setProperty(runtime, "measureText", measureText);
    
    return @true;
    
}
@end
