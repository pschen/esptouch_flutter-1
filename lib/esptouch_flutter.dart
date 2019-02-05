import 'dart:async';

import 'package:flutter/services.dart';

class EsptouchFlutter {
  static const METHOD_CHANNEL_ID = 'me.zeltrix.esptouch_flutter/method_channel';
  static const EVENT_STREAM_ID = 'me.zeltrix.esptouch_flutter/event_stream';

  static const MethodChannel _methodChannel =
      const MethodChannel(METHOD_CHANNEL_ID);
  static const EventChannel _eventChannel = const EventChannel(EVENT_STREAM_ID);

  static StreamSubscription _eventSubscriber;

  List<EspTouchResult> results;

  static Future<bool> initEspTouch(WifiCred cred) async {
    final bool isInit = await _methodChannel.invokeMethod('initEspTouch', cred.toMap());
    return isInit;
  }

  static Stream getStream() {
    print("subscribe");
    // _eventSubscriber =
        // _eventChannel.receiveBroadcastStream().listen(onDeviceAdded);
    return _eventChannel.receiveBroadcastStream();
  }

  // static unsubscribe() {
  //   if (_eventSubscriber != null) {
  //     _eventSubscriber.cancel();
  //     _eventSubscriber = null;
  //   }
  // }
}

class WifiCred {
  String ssid;
  String bssid;
  String pass;
  int deviceCount;
  bool broadcast;

  WifiCred({this.ssid,this.bssid,this.pass, this.deviceCount = 1, this.broadcast = true});

  Map<String, String> toMap() => {
    'ssid': ssid,
    'bssid': bssid,
    'pass': pass,
    'deviceCount': deviceCount.toString(),
    'broadcast': broadcast?'1':'0'
  };
}

class EspTouchResult {

  String bssid;
  String inetAddress;
  bool isSuc;

  EspTouchResult.fromMap(Map<String, String> map) {
    this.bssid = map['bssid'];
    this.inetAddress = map['inetAddress'];
    this.isSuc = map['isSuc']=='true'?true:false;
  }
}
