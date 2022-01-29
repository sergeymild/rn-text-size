import * as React from 'react';

import { StyleSheet, View, Text, TouchableOpacity } from 'react-native';
import 'react-native-random-values-jsi-helper';
import { useCallback } from 'react';
import { v4 as uuid } from 'uuid';


const text = 'The default is the same applied by React Native: Roboto in Android, San Francisco in iOS.\n' +
  'Note: Device manufacturer or custom ROM can change the default font'

export default function App() {
  const [_uuid, setUuid] = React.useState<string | undefined>(undefined);

  const generateUuid = useCallback(() => {
    // const generatedUUID = uuid();
    // setUuid(generatedUUID);

  }, []);

  console.log('[App.measure]', global.measureText(text, 20, 50));

  return (
    <View style={styles.container}>
      <TouchableOpacity onPress={generateUuid}>
        <Text>Generate UUID</Text>
      </TouchableOpacity>
      <Text style={{fontSize: 14, height: global.measureText(text, 14, 80).height, maxWidth: 80, backgroundColor: 'red'}}>{text}</Text>
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
