import { findNodeHandle, NativeModules, Platform } from 'react-native';
import type * as React from 'react';

const LINKING_ERROR =
  `The package 'react-native-random-values-jsi-helper' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const RandomValuesJsiHelper = NativeModules.RandomValuesJsiHelper
  ? NativeModules.RandomValuesJsiHelper
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

RandomValuesJsiHelper.install();

export interface MeasureParams {
  text: string;
  fontSize: number;
  maxWidth: number;
  allowFontScaling?: boolean;
  usePreciseWidth?: boolean;
  fontFamily?: string;
}

export interface MeasureResult {
  height: number;
  width: number;
  lineCount: number;
  lastLineWidth: number;
}

export interface MeasureViewResult {
  height: number;
  width: number;
  x: number;
  y: number;
}

export class RNViewHelpers {
  static measureText(params: MeasureParams): MeasureResult {
    // @ts-ignore
    return global.measureText(params);
  }

  static measureView(ref: React.RefObject<any>): MeasureViewResult {
    const viewId = findNodeHandle(ref.current);
    if (!viewId) return { width: 0, height: 0, x: 0, y: 0 };
    // @ts-ignore
    return global.measureView(viewId);
  }
}
