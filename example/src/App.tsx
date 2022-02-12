import * as React from 'react';
import { useEffect, useRef } from 'react';

import { StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { RNViewHelpers } from 'react-native-random-values-jsi-helper';

const text =
  'The default is the same applied by React Native: Roboto in Android, San Francisco in iOS.\n' +
  'Note: Device manufacturer or custom ROM can change the default font';

export default function App() {
  const viewRef: React.RefObject<View> = useRef<View>(null);
  const fs = 14;
  const w = 53;
  //const h = RNViewHelpers.measureText({ text, fontSize: fs, maxWidth: w });
  //console.log('[App.measure]', h);

  useEffect(() => {
    setTimeout(() => {

      console.log('[App.]', RNViewHelpers.measureView(viewRef));
    }, 1000);

    RNViewHelpers.registerCallback(() => {
      console.log('[App.callback]')
    })
  }, []);

  return (

    <View style={styles.container}>
      <Text
        ref={viewRef}
        style={{
          fontSize: fs,
          height: 2,
          width: 2,
          backgroundColor: 'red',
        }}
      >
        {text}
      </Text>

      <TouchableOpacity style={{marginTop: 100}} onPress={() => {
        RNViewHelpers.invokeCallback()
      }}>
        <Text>Press</Text>
      </TouchableOpacity>

      <TouchableOpacity style={{marginTop: 100}} onPress={() => {
        RNViewHelpers.unregisterCallback()
      }}>
        <Text>Unregister</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingTop: 10,
    marginStart: 10,
    marginTop: 0,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
