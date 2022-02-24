# React Native Text Size

## Installation

```sh
yarn add react-native-random-values-jsi-helper
npx pod-install
```

## Usage

Import ```react-native-random-values-jsi-helper``` in your index.js file.

```js
//index.js
import "react-native-random-values-jsi-helper";
```

You can now use the `RNViewHelpers.measureText` and `RNViewHelpers.measureView` method in your React Native app.

```ts
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
  static measureText(params: MeasureParams): MeasureResult
  static measureView(ref: React.RefObject<any>): MeasureViewResult
}
```

## Performance

This module is written in C++ JSI.

Basically there is no over the bridge traffic overhead and no serialization/deserialization.
