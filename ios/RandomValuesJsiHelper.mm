#import "RandomValuesJsiHelper.h"
#import <React/RCTBlobManager.h>
#import <React/RCTUIManager.h>
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
    
    auto measureView = jsi::Function::createFromHostFunction(runtime,
                                                                 jsi::PropNameID::forUtf8(runtime, "measureView"),
                                                                 1,
                                                                 [bridge](jsi::Runtime& runtime,
                                                                    const jsi::Value& thisArg,
                                                                    const jsi::Value* args,
                                                                    size_t count) -> jsi::Value {
        
        auto viewId = args[0].asNumber();
        __block CGRect viewFrame = CGRectZero;
        __block CGRect globalBounds = CGRectZero;
        dispatch_sync(dispatch_get_main_queue(), ^{
            auto idNumber = [[NSNumber alloc] initWithDouble:viewId];
            auto view = [bridge.uiManager viewForReactTag: idNumber];
            UIView *rootView = view;
            if (view != nil) {
                viewFrame = view.frame;
                while (rootView.superview && ![rootView isReactRootView]) {
                    rootView = rootView.superview;
                }
                if (rootView) {
                    globalBounds = [view convertRect:view.bounds toView:rootView];
                }
            }
        });
        
        
        
        if (CGRectIsEmpty(globalBounds)) return jsi::Value::undefined();
        
        jsi::Object result = jsi::Object(runtime);
        result.setProperty(runtime, "width", jsi::Value(globalBounds.size.width));
        result.setProperty(runtime, "height", jsi::Value(globalBounds.size.height));
        result.setProperty(runtime, "x", jsi::Value(globalBounds.origin.x));
        result.setProperty(runtime, "y", jsi::Value(globalBounds.origin.y));
        
        return result;
    });

    runtime.global().setProperty(runtime, "measureText", measureText);
    runtime.global().setProperty(runtime, "measureView", measureView);
    
    return @true;
    
}
@end
