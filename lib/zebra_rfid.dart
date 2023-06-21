import 'dart:async';

import 'package:flutter/services.dart';
import 'package:zebra_rfid/base.dart';

class ZebraRfid {
  static const MethodChannel _channel =
      const MethodChannel('com.hone.zebraRfid/plugin');
  static const EventChannel _eventChannel =
      const EventChannel('com.hone.zebraRfid/event_channel');
  static ZebraEngineEventHandler? _handler;

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<String?> toast(String text) async {
    return _channel.invokeMethod('toast', {"text": text});
  }

  ///
  static Future<String?> onRead() async {
    return _channel.invokeMethod('startRead');
  }

  static Future<dynamic> getReadersList() async {
    return _channel.invokeMethod('getReadersList');
  }

  ///写
  static Future<String?> write() async {
    return _channel.invokeMethod('write');
  }

  static Future<String?> linkProfiles() {
    return _channel.invokeMethod('getRFModeTableInfo');
  }

  static Future<int?> getlinkProfile() {
    return _channel.invokeMethod('getLinkedProfile');
  }

  static Future<int?> setlinkProfile(int i) {
    return _channel.invokeMethod('setLinkedProfile', {"profileIndex": i});
  }

  static Future<int?> setDPO(int i) {
    return _channel.invokeMethod('setDPower', {"DPower": i});
  }

  static Future<int?> getDPO() {
    return _channel.invokeMethod('getDPower');
  }

  static Future<int?> setSControl(int i) {
    return _channel.invokeMethod('setSControl', {"SIndex": i});
  }

  static Future<int?> getSControl() {
    return _channel.invokeMethod('getSControl');
  }

  ///连接设备
  static Future<dynamic> connect() async {
    try {
      await _addEventChannelHandler();
      var result = await _channel.invokeMethod('connect');
      return result;
    } catch (e) {
      var a = e;
    }
  }

  static Future<dynamic> StartInvo() async {
    try {
      await _channel.invokeMethod('startInvo');
    } catch (e) {
      var a = e;
    }
  }

  static Future<void> StopInvo() async {
    try {
      await _channel.invokeMethod('stopInvo');
    } catch (e) {
      print(e);
    }
  }

  static Future<dynamic> StartLocate(String tagId) async {
    try {
      await _addEventChannelHandler();
      var result = await _channel.invokeMethod('startLocate', {"tagId": tagId});
      return result;
    } catch (e) {
      var a = e;
    }
  }

  static Future<dynamic> StopLocate() async {
    try {
      var result = await _channel.invokeMethod('stopLocate');
      return result;
    } catch (e) {
      var a = e;
    }
  }

  static Future<dynamic> getbatterie() async {
    return _channel.invokeMethod('getbatterie');
  }

  static Future<int> setBeeperVolume(int volume) async {
    final x = _channel.invokeMethod('setBeeperVolume', {"volume": volume});
    return await x;
  }

  static Future<String> getBeeperVolume() async {
    final x = _channel.invokeMethod('getBeeperVolume');
    return await x;
  }

  static Future<int> enableLED(int i) async {
    final y = _channel.invokeMethod('enableLED', {"enable": i});
    return await y;
  }

  ///Disconnect the device
  static Future<String?> disconnect() async {
    return _channel.invokeMethod('disconnect');
  }

  /// Sets the engine event handler.
  ///
  /// After setting the engine event handler, you can listen for engine events and receive the statistics of the corresponding [RtcEngine] instance.
  ///
  /// **Parameter** [handler] The event handler.
  static void setEventHandler(ZebraEngineEventHandler handler) {
    _handler = handler;
  }

  static StreamSubscription<dynamic>? _sink;
  static Future<void> _addEventChannelHandler() async {
    if (_sink == null) {
      _sink = _eventChannel.receiveBroadcastStream().listen((event) {
        final eventMap = Map<String, dynamic>.from(event);
        final eventName = eventMap['eventName'] as String?;
        print(eventName);
        print("-----------------------------------------");
        // final data = List<dynamic>.from(eventMap['data']);
        _handler?.process(eventName, eventMap);
      });
    }
  }

  ///DisConnect device
  static Future<String?> dispose() async {
    _sink = null;
    return _channel.invokeMethod('dispose');
  }

  static Future<bool> get isConnected async {
    return await _channel.invokeMethod('isConnected');
  }

  /// set power
  /// [power] 0-270
  static Future<void> setPower(int power) async {
    // return error if power is not in range
    assert(power >= 0 && power <= 270);
    // if not connected show error
    assert(await isConnected);
    return _channel.invokeMethod('setPower', {"powerIndex": power});
  }

  static Future<int> getPower() async {
    final power = await _channel.invokeMethod('getPower');
    return power;
  }
}
