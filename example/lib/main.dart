import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:zebra_rfid/base.dart';
import 'package:zebra_rfid/zebra_rfid.dart';
import 'package:zebra_rfid_example/locate.dart';

void main() {
  runApp(MaterialApp(
    title: 'Navigation Basics',
    home: MyApp(),
  ));
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? _platformVersion = 'Unknown';
  int power = 200;
  dynamic yy = 0;

  bool isZebraConnected = false;

  double get powerValue => power / 1000;

  final powerMin = 0;
  final powerMax = 270;
  set powerValue(double value) {
    power = (value * 1000).toInt();
    print('powerValue $power');
    ZebraRfid.setPower(power);
  }

  @override
  void initState() {
    ZebraRfid.setEventHandler(
      ZebraEngineEventHandler(readRfidCallback: (datas) async {
        print(datas.first.toJson());
        addDatas(datas);
      }, errorCallback: (err) {
        ZebraRfid.toast(err.errorMessage!);
      }, connectionStatusCallback: (status) {
        setState(() {
          connectionStatus = status;
        });
      }, l1statusCallback: (status) {
        setState(() {
          L1Status = status;
        });
      }),
    );
    ZebraRfid.connect().then((value) {
      print("--------------------value---${value}------------");

      setState(() {
        isZebraConnected = true;
      });
    });
    super.initState();
    initPlatformState();
  }

  Map<String?, RfidData> rfidDatas = {};
  ReaderConnectionStatus connectionStatus = ReaderConnectionStatus.UnConnection;
  bool L1Status = false;
  addDatas(List<RfidData> datas) async {
    for (var item in datas) {
      var data = rfidDatas[item.tagID];
      if (data != null) {
        if (data.count == null) data.count = 0;
        data.count = data.count! + 1;
        data.peakRSSI = item.peakRSSI;
        data.relativeDistance = item.relativeDistance;
      } else
        rfidDatas.addAll({item.tagID: item});
    }
    setState(() {});
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String? platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await ZebraRfid.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text(
              'Status  ${connectionStatus.index} and l1Status ${L1Status.toString()}'),
        ),
        body: Center(
            child: Column(
          children: [
            Text('Running on: $_platformVersion\n'),
            Text('count:${rfidDatas.length.toString()}'),
            Row(children: [
              MaterialButton(
                onPressed: () async {
                  await ZebraRfid.StartLocate("4F4C30C30C30C30CF6C71DB3DE18");
                },
                child: Text("read"),
              ),
              MaterialButton(
                onPressed: () async {
                  await ZebraRfid.StopInvo(); // await ZebraRfid.linkProfiles();
                  // yy = i.toString();
                  setState(() {
                    rfidDatas = {};
                  });
                },
                child: Text("clear + \n "),
              ),
              MaterialButton(
                onPressed: () async {
                  // try {
                  //   ZebraRfid.disconnect();
                  // } catch (e) {
                  //   print(e);
                  // }

                  ZebraRfid.disconnect();
                },
                child: Text("stop"),
              ),
            ]),
            Text(yy.toString()),
            Slider(
                max: powerMax / 1000,
                min: powerMin / 1000,
                label: power.toString(),
                value: powerValue,
                onChanged: (value) {
                  powerValue = value;
                  setState(() {});
                }),
            Expanded(
                child: Scrollbar(
              child: ListView.builder(
                itemBuilder: (context, index) {
                  var key = rfidDatas.keys.toList()[index];
                  return ListTile(title: Text(rfidDatas[key]!.tagID!));
                },
                itemCount: rfidDatas.length,
              ),
            ))
          ],
        )));
  }
}
