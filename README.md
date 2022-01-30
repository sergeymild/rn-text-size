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
RNViewHelpers.measureText(
  text: string,
  fontSize: number,
  maxWidth: number
): { height: number; width: number; lineCount: number; lastLineWidth: number };

RNViewHelpers.measureView(
  ref: ref: React.RefObject<any>
): { height: number; width: number; x: number; y: number };
```

## Performance

This module is written in C++ JSI.

Basically there is no over the bridge traffic overhead and no serialization/deserialization.
