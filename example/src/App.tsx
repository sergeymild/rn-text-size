import * as React from 'react';

import { StyleSheet, Text, View } from 'react-native';
import 'react-native-random-values-jsi-helper';


const text = 'The default is the same applied by React Native: Roboto in Android, San Francisco in iOS.\n' +
  'Note: Device manufacturer or custom ROM can change the default font'

export default function App() {
  const fs = 14
  const w = 200
  const h = global.measureText(text, fs, w).height
  console.log('[App.measure]', h);

  return (
    <View style={styles.container}>
      <Text style={{fontSize: fs, height: h, maxWidth: w, backgroundColor: 'red'}}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 50,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
