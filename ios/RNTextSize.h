//
//  RNTextSize.h
//  RandomValuesJsiHelper
//
//  Created by Sergei Golishnikov on 28/01/2022.
//  Copyright © 2022 Facebook. All rights reserved.
//


//#import "RCTBridge+Private.h"
//#import "RCTBridge.h"
#import <React/RCTBridgeModule.h>
#import <React/RCTBridge+Private.h>
#import <React/RCTBridge.h>
#import <React/RCTAccessibilityManager.h>

@interface RNTextSize : NSObject
-(NSDictionary*) measure:(NSString*)text
                   width:(NSNumber*)width
                fontSize:(NSNumber*)fontSize
         usePreciseWidth:(BOOL)usePreciseWidth
        allowFontScaling:(BOOL)allowFontScaling
              fontFamily:(NSString*)fontFamily;
@end
