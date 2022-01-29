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

You can now use the `global.measureText` method in your React Native app.

```ts
declare global {
  function measureText(
    text: string,
    fontSize: number,
    maxWidth: number
  ): { height: number; width: number; lineCount: number; lastLineWidth: number };
}
```

## Performance

This module is written in C++ JSI.

Basically there is no over the bridge traffic overhead and no serialization/deserialization.
