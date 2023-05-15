import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:zebra_rfid/base.dart';
import 'package:zebra_rfid/zebra_rfid.dart';

class Locate extends StatefulWidget {
  @override
  _LocateState createState() => _LocateState();
}

class _LocateState extends State<Locate> {
  String? _platformVersion = 'Unknown';
  int power = 200;
  TextEditingController controller = TextEditingController();

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
    super.initState();
    initPlatformState();
  }

  Map<String?, RfidData> rfidDatas = {};
  ReaderConnectionStatus connectionStatus = ReaderConnectionStatus.UnConnection;
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
          title: Text('Status  ${connectionStatus.index}'),
        ),
        body: Center(
            child: Column(
          children: [
            Text('Running on: $_platformVersion\n'),
            Text('count:${rfidDatas.length.toString()}'),
            TextField(
              controller: controller,
            ),
            Row(children: [
              MaterialButton(
                onPressed: () async {
                  ZebraRfid.setEventHandler(ZebraEngineEventHandler(
                    readRfidCallback: (datas) async {
                      print("-------------------------------");
                      print(datas.first.toJson());
                      addDatas(datas);
                    },
                    errorCallback: (err) {
                      ZebraRfid.toast(err.errorMessage!);
                    },
                    connectionStatusCallback: (status) {
                      setState(() {
                        connectionStatus = status;
                      });
                    },
                  ));
                  ZebraRfid.connect();
                  ZebraRfid.StartLocate(controller.value.text);

                  setState(() {
                    isZebraConnected = true;
                  });
                },
                child: Text("read"),
              ),
              MaterialButton(
                onPressed: () async {
                  setState(() {
                    rfidDatas = {};
                  });
                },
                child: Text("clear"),
              ),
              MaterialButton(
                onPressed: () async {
                  try {
                    ZebraRfid.StopLocate();
                    ZebraRfid.disconnect();
                  } catch (e) {
                    print(e);
                  }
                  isZebraConnected
                      ? ZebraRfid.disconnect()
                      : print(
                          "--------------------------working-----------------------");
                },
                child: Text("stop"),
              ),
            ]),
            // Slider(
            //     max: powerMax / 1000,
            //     min: powerMin / 1000,
            //     label: power.toString(),
            //     value: powerValue,
            //     onChanged: (value) {
            //       powerValue = value;
            //       setState(() {});
            //     }),
            Expanded(
                child: Scrollbar(
              child: ListView.builder(
                itemBuilder: (context, index) {
                  var key = rfidDatas.keys.toList()[index];
                  return ListTile(
                      title: rfidDatas[key]!.tagID == null
                          ? Text(" ")
                          : Text(rfidDatas[key]!.tagID!));
                },
                itemCount: rfidDatas.length,
              ),
            ))
          ],
        )));
  }
}
