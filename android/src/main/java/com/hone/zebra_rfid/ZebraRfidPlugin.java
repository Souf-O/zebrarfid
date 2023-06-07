package com.hone.zebra_rfid;

import android.content.Context;
import android.util.Log;
import java.util.List;
import android.widget.Toast;


import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;


/**
 * ZebraRfidPlugin
 */
public class ZebraRfidPlugin implements FlutterPlugin, MethodCallHandler, StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private EventChannel eventChannel;
    private RFIDHandler rfidHandler;
    private Context context;
    private EventChannel.EventSink sink = null;

    private String TAG = "ZebraRfidPlugin";

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        rfidHandler = new RFIDHandler(context);

        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "com.hone.zebraRfid/plugin");
        channel.setMethodCallHandler(this);


        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "com.hone.zebraRfid/event_channel");
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case "toast":
                String txt=call.argument("text");
                Toast.makeText(context, txt, Toast.LENGTH_LONG).show();
                break;
            case "connect":
               // boolean  isBluetooth=call.argument("isBluetooth");
                rfidHandler.connect(result);
                break;
            case "getReadersList":
                List y = rfidHandler.getReadersList();                
                result.success(y);
                break;

            case "disconnect":
                rfidHandler.dispose();
                result.success(null);
                break;
            case "write":
                ///[TODO] implement write
                break;
            case  "setPower":
                int powerIndex = call.argument("powerIndex");
                rfidHandler.setMaxPower(powerIndex);
                result.success(null);
                break;
            case "startLocate" : 
                String tagId = call.argument("tagId");
                rfidHandler.startLocate(tagId, result);
                break ; 
            case "stopLocate" : 
                rfidHandler.stopLocate();
                break ;     
            case "getbatterie":
                int x = rfidHandler.getBatteryLevel();
                result.success(x);
                break ;    
            case "setBeeperVolume":
                int i =   call.argument("volume"); 
                int j =rfidHandler.setBeeperVolume(i);
                result.success(j);
                break;
            case "getRFModeTableInfo" : 
                String _results = rfidHandler.getLinkedProfiles();
                result.success(_results);
                break;
            case "getLinkedProfile":
                int res = rfidHandler.getLinkedProfile();
                result.success(res);   
                break; 
                
            case "setSControl":
                int sIndex = call.argument("SIndex");

                int _Sres = rfidHandler.setSControl(sIndex);
                result.success(_Sres);   
                break;    
            case "getSControl":
                //int sIndex = call.argument("SIndex");

                int _SControl = rfidHandler.getSControl();
                result.success(_SControl);   
                break;   
            case "getDPower":
                int getDPower = rfidHandler.getDPower();
                result.success(getDPower);   
                break;              
            case "setDPower":
                int dpo = call.argument("DPower");
                int dpores = rfidHandler.setDPower(dpo);
                result.success(dpores);   
                break;          
            case "setLinkedProfile":
                int profileIndex = call.argument("profileIndex");

                int _res = rfidHandler.setLinkedProfile(profileIndex);
                result.success(_res);   
                break; 
            // case "getBeeperVolume":
            //     String z = rfidHandler.getBeeperVolume();
            //     result.success(z);

            //     break;    
            case "enableLED":
                int enable = call.argument("enable");
                int led = rfidHandler.enableLED(enable);
                result.success(led);
                break;    
            case "getPower":
                int power = rfidHandler.getMaxPower();
                result.success(power);
                break;
            case "isConnected":
                boolean isConnected = rfidHandler.isConnected();
                result.success(isConnected);
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
    }


    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        Log.w(TAG, "adding listener");
        sink = events;
        rfidHandler.setEventSink(sink);
    }

    @Override
    public void onCancel(Object arguments) {
        Log.w(TAG, "cancelling listener");
        sink = null;
    }




}
