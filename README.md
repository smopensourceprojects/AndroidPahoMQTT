# AndroidPahoMQTT
This is Android Websocket Connection library over MQTT Protocol by using Eclipse Paho

## Overview
The Paho project has been created to provide reliable open-source implementations of open and standard messaging protocols aimed at new, existing, and emerging applications for Machine-to-Machine (M2M) and Internet of Things (IoT). Paho reflects the inherent physical and cost constraints of device connectivity. Its objectives include effective levels of decoupling between devices and applications, designed to keep markets open and encourage the rapid growth of scalable Web and Enterprise middleware and applications.

Add library as AAR dependency. Create the AAR file from console by using command __./gradlew assembleRelease__

__This project is copied from https://github.com/eclipse/paho.mqtt.android/tree/master/org.eclipse.paho.android.service.__
__If you encounter bugs with it or need enhancements, you can fork it and modify it as the project is under the Apache License 2.0.__
__This is created for Open Source to use any applications/projects. Few codes changed based on my requirement.__

**Check out the changes from eclipse.paho.android.service Library**
``` 
1. Upgraded to 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
2. Supported Android 11+
3. Removed Local Broadcast Receiver and replaced with EventBus 
4. Removed AlarmManager to support Android 13
5. Implemented removeMessage(), reconnect() and getInFlightMessageCount() in MqttService 
```
