package com.hone.zebra_rfid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.INVENTORY_STATE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.SESSION;
import com.zebra.rfid.api3.SL_FLAG;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;
import com.zebra.rfid.api3.Events;
import com.zebra.rfid.api3.BEEPER_VOLUME;
import com.zebra.rfid.api3.ReaderCapabilities;
import com.zebra.rfid.api3.RFModeTable;
import com.zebra.rfid.api3.RFModeTableEntry;
import com.zebra.rfid.api3.DYNAMIC_POWER_OPTIMIZATION;



import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.LogManager;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel.Result;

public class RFIDHandler implements Readers.RFIDReaderEventHandler {
    private String TAG = "RFIDHandler";
    Context context;

    public Handler mEventHandler = new Handler(Looper.getMainLooper());
    private AsyncTask<Void, Void, String> AutoConnectDeviceTask;
    private static Readers readers;
    //    private static ArrayList<ReaderDevice> availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private Base.ConnectionStatus status = Base.ConnectionStatus.UnConnection;
    private boolean isConnecting = false ;
    private int maxPower = 270;
    private boolean isLocating = false ;
    private String tagLocationId ;
    private IEventHandler eventHandler = new IEventHandler();
    private Function<String, Map<String, Object>> _emit;
    private EventChannel.EventSink sink = null;



    private void emit(final String eventName, final HashMap map) {
        map.put("eventName", eventName);
        mEventHandler.post(new Runnable() {
            @Override
            public void run() {
                if (sink != null) {
                    sink.success(map);
                }
            }
        });
    }

    RFIDHandler(Context _context) {
        context = _context;

    }

   public void setEventSink(EventChannel.EventSink _sink){
        sink = _sink;
    }

    @SuppressLint("StaticFieldLeak")
    public void connect(final Result result) {
        Readers.attach(this);
        isConnecting = true ;
        if (readers == null) {
                readers = new Readers(context,ENUM_TRANSPORT.ALL);
            //readers = new Readers(context, ENUM_TRANSPORT.SERVICE_SERIAL);
        }
        AutoConnectDevice(result);
    }

     void AutoConnectDevice2(Result result){
        Log.d(TAG, "CreateInstanceTask");
                try {
                    isConnecting = true;

                    if (readers != null) {
                        if (readers.GetAvailableRFIDReaderList() != null) {
                        ArrayList<ReaderDevice> readersListArray = readers.GetAvailableRFIDReaderList();
                        if (readersListArray.size() != 0) {
                            readerDevice = readersListArray.get(0);
                            reader = readerDevice.getRFIDReader();
                            status = Base.ConnectionStatus.UnConnection;
                            if (!reader.isConnected()) {
                                // Establish connection to the RFID Reader
                                reader.connect();
                                ConfigureReader();
                                isConnecting = false;
                                status = Base.ConnectionStatus.ConnectionRealy;
                            }
                        } else {
                            status = Base.ConnectionStatus.ConnectionError;
                            isConnecting = false;
                        }
                    }
                }

                
                } catch (OperationFailureException e) {
                    status=Base.ConnectionStatus.UnConnection;

                    String details = e.getStatusDescription();
                    String a = e.getVendorMessage();
                    e.printStackTrace();
                } catch (InvalidUsageException er){
                    status=Base.ConnectionStatus.UnConnection;

                    er.printStackTrace();
                }
                HashMap<String, Object> map =new HashMap<>();
                map.put("status",status.ordinal());
                emit(Base.RfidEngineEvents.ConnectionStatus,map);

    }


    @SuppressLint("StaticFieldLeak")
    public void AutoConnectDevice(final Result result) {
        AutoConnectDeviceTask = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Log.d(TAG, "CreateInstanceTask");
                try {
                    isConnecting = true;

                    if (isCancelled()) {
                        isConnecting = false; 
                        Log.d("is connecting :" , String.valueOf(isConnecting) );
                        return "Task cancelled";
                    }
                    if (readers != null) {
                        if (readers.GetAvailableRFIDReaderList() != null) {
                        ArrayList<ReaderDevice> readersListArray = readers.GetAvailableRFIDReaderList();
                        if (readersListArray.size() != 0) {
                            readerDevice = readersListArray.get(0);
                            reader = readerDevice.getRFIDReader();
                            if (!reader.isConnected()) {
                                // Establish connection to the RFID Reader
                                reader.connect();
                                ConfigureReader();
                                isConnecting = false;
                                
                            }
                        } else {
                            isConnecting = false;
                            return "No connectable device detected";
                        }
                    }
                }

                
                } catch (OperationFailureException e) {
                    String details = e.getStatusDescription();
                    String a = e.getVendorMessage();
                    e.printStackTrace();
                } catch (InvalidUsageException er){
                    er.printStackTrace();
                }
                return null;
            }


            @Override
            protected void onPostExecute(String error) {
                Base.ConnectionStatus status=Base.ConnectionStatus.ConnectionRealy;
                super.onPostExecute(error);
                isConnecting = false;

                if (error != null) {
                    emit(Base.RfidEngineEvents.Error, transitionEntity(Base.ErrorResult.error(error)));
                    status=Base.ConnectionStatus.ConnectionError;
                }
                HashMap<String, Object> map =new HashMap<>();
                map.put("status",status.ordinal());
                emit(Base.RfidEngineEvents.ConnectionStatus,map);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                isConnecting = false;

                AutoConnectDeviceTask = null;
            }

    }.execute();


}


    private synchronized void ConfigureReader() {
       
            if (reader == null) {
                throw new NullPointerException("Reader is not connected");
            }
        
            // Configure the reader here
        
        
        if (reader != null && reader.isConnected()) {
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);
            try {
                // receive events from reader
                reader.Events.addEventsListener(eventHandler);
                // HH event
                reader.Events.setHandheldEvent(true);
                // tag event with tag data
                reader.Events.setTagReadEvent(true);
                reader.Events.setAttachTagDataWithReadEvent(false);
                // set trigger mode as rfid so scanner beam will not come
                reader.Config.setTriggerMode(ENUM_TRIGGER_MODE.RFID_MODE, true);
                // set start and stop triggers
                reader.Config.setStartTrigger(triggerInfo.StartTrigger);
                reader.Config.setStopTrigger(triggerInfo.StopTrigger);
                // power levels are index based so maximum power supported get the last one
                maxPower = reader.ReaderCapabilities.getTransmitPowerLevelValues().length - 1;
                // set antenna configurations
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                config.setTransmitPowerIndex(maxPower);
                config.setrfModeTableIndex(0);
                config.setTari(0);
                reader.Config.Antennas.setAntennaRfConfig(1, config);
                // Set the singulation control
                Antennas.SingulationControl s1_singulationControl = reader.Config.Antennas.getSingulationControl(1);
                s1_singulationControl.setSession(SESSION.SESSION_S0);
                s1_singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);
                s1_singulationControl.Action.setSLFlag(SL_FLAG.SL_ALL);
                reader.Config.Antennas.setSingulationControl(1, s1_singulationControl);
                // delete any prefilters
                reader.Actions.PreFilters.deleteAll();
                //
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
            isConnecting = false;

        }
    }



    
    public void startLocate(final String tagID, final Result result) {
        isLocating = true ;
        tagLocationId = tagID ;
        Log.d("locating: " , tagID);
        Log.d("isLocating: ", String.valueOf(isLocating));
    }


    public void stopLocate() {
        isLocating = false ;
        tagLocationId = "" ;
        Log.d("locating: ",  String.valueOf(isLocating) );
        Log.d("isLocating: ", String.valueOf(isLocating));
    }


    public void dispose() {
        try {
            if (reader != null && !isConnecting) {
                 if (AutoConnectDeviceTask != null) {
                    AutoConnectDeviceTask.cancel(true);
                }
                Log.d("status: before : ", String.valueOf(reader.isConnected()));
                reader.Events.removeEventsListener(eventHandler);
                reader.disconnect();
                reader = null;
                status = Base.ConnectionStatus.UnConnection;
                
                readers.Dispose();
                readers = null;

                 HashMap<String, Object> map =new HashMap<>();
                map.put("status",status.ordinal());
                emit(Base.RfidEngineEvents.ConnectionStatus,map);
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // try {

        //     if (readers != null && isConnecting) {
               
        //         readerDevice=null;
        //         reader = null;
        //         readers.Dispose();
        //         readers = null;
        //         HashMap<String, Object> map =new HashMap<>();
        //         map.put("status", Base.ConnectionStatus.UnConnection.ordinal());
        //         emit(Base.RfidEngineEvents.ConnectionStatus,map);
        //     }
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
    }




    private boolean isReaderConnected() {
        if (reader != null && reader.isConnected())
            return true;
        else if( isConnecting ){
            return false ;
        }
            else {
            Log.d(TAG, "reader is not connected");
            return false;
        }
    }

    public void setMaxPower(int newMaxPower) {
        maxPower = newMaxPower;

        Log.d("setMaxPower", "hello");
        try {
            if (reader != null && reader.Config != null && reader.Config.Antennas != null) {
                Log.d("setMaxPower", "reader ... ");
                Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
                Log.d("setMaxPower", "config Done");
                if (config != null) {
                    Log.d("setMaxPower", "config not NULL");
                    config.setTransmitPowerIndex(maxPower);
                    Log.d("setMaxPower", "setTransmitPowerIndex");
                    reader.Config.Antennas.setAntennaRfConfig(1, config);
                    Log.d("setMaxPower", "setAntennaRfConfig");
                } else {
                    // Handle case when AntennaRfConfig is null
                    // Log an error or throw an exception as needed
                }
            } else {
                // Handle case when reader or its properties are null
                // Log an error or throw an exception as needed
            }
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
            // Handle exception as needed
        }
    }
    

   
    public  int getMaxPower(){
        return maxPower;
    }

    public boolean isConnected(){
        return isReaderConnected();
    }

    public int getLinkedProfile() {
        try {
            Antennas.AntennaRfConfig antennaRfConfig = reader.Config.Antennas.getAntennaRfConfig(1);
            Long x = antennaRfConfig.getrfModeTableIndex();
            if (x != null) {
                return x.intValue();
            } else {
                // Handle the case when x is null
                return -1; // or any other appropriate default value
            }
        } catch (InvalidUsageException | OperationFailureException e) {
            e.printStackTrace();
            // Handle the exception
        }
        return -1; // or any other appropriate default value
    }
    
    public int setLinkedProfile(int i) {
        try {
            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            if (config.getrfModeTableIndex() != i ) {
                config.setTari(0);
                config.setrfModeTableIndex(i);
            }
            reader.Config.Antennas.setAntennaRfConfig(1, config);
            Log.d("setLinkedProfile: success = " ,String.valueOf(i));          
            return (int)  config.getrfModeTableIndex();
        } catch (InvalidUsageException e) {
            Log.d("InvalidUsageException , Error message: " , e.getMessage());
            e.printStackTrace();
            return 0;
        } catch (OperationFailureException e) {
            Log.d("Status description: OperationFailureException details:   " , e.getStatusDescription());
            Log.d("Status code: " ,  e.	getVendorMessage() );
            e.printStackTrace();
            return 0;
        }
    }

    public int getSControl(){
        try {
            Antennas.SingulationControl singulationControl;
            Log.d("Get Singulation Control:  " ,"----------------");   

            singulationControl = reader.Config.Antennas.getSingulationControl(1);
            singulationControl.getSession().getValue();
            Log.d("Get Singulation Control:  success = " ,String.valueOf(singulationControl.getSession().getValue()));          
            return   singulationControl.getSession().getValue();
        } catch (InvalidUsageException e) {
            Log.d("InvalidUsageException , Error message: " , e.getMessage());
            e.printStackTrace();
            return 0;
        } catch (OperationFailureException e) {
            Log.d("Status description: OperationFailureException details:   " , e.getStatusDescription());
            Log.d("Status code: " ,  e.	getVendorMessage() );
            e.printStackTrace();
            return 0;
        }
    }

    

    public int setSControl(int s){
        try {
            Antennas.SingulationControl singulationControl;
            singulationControl = reader.Config.Antennas.getSingulationControl(1);
            singulationControl.setSession(SESSION.GetSession(s));
            if (s == 0 )
                singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_AB_FLIP);
            else if (s == 5 )
                singulationControl.Action.setInventoryState(INVENTORY_STATE.INVENTORY_STATE_A);

            reader.Config.Antennas.setSingulationControl(1, singulationControl);
            Log.d("setLinkedProfile: success = " ,String.valueOf(s));          
            return   singulationControl.getSession().getValue();
        } catch (InvalidUsageException e) {
            Log.d("InvalidUsageException , Error message: " , e.getMessage());
            e.printStackTrace();
            return 0;
        } catch (OperationFailureException e) {
            Log.d("Status description: OperationFailureException details:   " , e.getStatusDescription());
            Log.d("Status code: " ,  e.	getVendorMessage() );
            e.printStackTrace();
            return 0;
        }
    }

    public int getDPower(){
        try {
            
             
            return  reader.Config.getDPOState().getValue();
        } catch (InvalidUsageException e) {
            Log.d("InvalidUsageException , Error message: " , e.getMessage());
            e.printStackTrace();
            return 0;
        } catch (OperationFailureException e) {
            Log.d("Status description: OperationFailureException details:   " , e.getStatusDescription());
            Log.d("Status code: " ,  e.	getVendorMessage() );
            e.printStackTrace();
            return 0;
        }
    }

    public int setDPower(int s){
        try {
            if (s == 1)
                        reader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.ENABLE);
            else
                reader.Config.setDPOState(DYNAMIC_POWER_OPTIMIZATION.DISABLE);
             
            return  reader.Config.getDPOState().getValue();
        } catch (InvalidUsageException e) {
            Log.d("InvalidUsageException , Error message: " , e.getMessage());
            e.printStackTrace();
            return 0;
        } catch (OperationFailureException e) {
            Log.d("Status description: OperationFailureException details:   " , e.getStatusDescription());
            Log.d("Status code: " ,  e.	getVendorMessage() );
            e.printStackTrace();
            return 0;
        }
    }

    
    

    public String  getLinkedProfiles() {
        RFModeTable rfModeTable = reader.ReaderCapabilities.RFModes.getRFModeTableInfo(0);
        RFModeTableEntry rfModeTableEntry = null;
        List<String> linkedProfiles = new ArrayList<>(); 
        for (int i = 0; i < rfModeTable.length(); i++) {
            rfModeTableEntry = rfModeTable.getRFModeTableEntryInfo(i);
            linkedProfiles.add(
                rfModeTableEntry.getBdrValue() + " " +
                rfModeTableEntry.getModulation().getValue()  + " " + 
                rfModeTableEntry.getMaxTariValue() + " " +
                rfModeTableEntry.getMinTariValue()  + " " + 
                rfModeTableEntry.getStepTariValue() + " " + 
                rfModeTableEntry.getPieValue()
            );
        }
        Log.d("getLinkedProfiles", String.valueOf(linkedProfiles));
        return String.valueOf(linkedProfiles);
    }
    

    // public void getRFModeTableInfo(){
    //     try{ 

    //         reader.ReaderCapabilities.RFModes.getRFModeTableInfo(0)
    //     } catch (InvalidUsageException | OperationFailureException e) {
    //         e.printStackTrace();
    //     }

    // }

    public int getBatteryLevel() {
        if (isReaderConnected()) {
            try {
                reader.Events.setBatteryEvent(true);
                reader.Config.getDeviceStatus(true, true, false);
                return 1;
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
        return -1; // Return a default value or handle the error case appropriately
    }

    public int setBeeperVolume(int type) {
        if (isReaderConnected()) {
            try {
                if(type == 0 ) {               
                    reader.Config.setBeeperVolume(BEEPER_VOLUME.QUIET_BEEP);
                    return 0;
                } else if( type == 1 ){
                    reader.Config.setBeeperVolume(BEEPER_VOLUME.LOW_BEEP);
                    return 1;
                }else if( type == 2 ){
                    reader.Config.setBeeperVolume(BEEPER_VOLUME.MEDIUM_BEEP);
                    return 2;
                } else if( type == 3 ){
                    reader.Config.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP);
                    return 3;
                }
                
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
        return -1; // Return a default value or handle the error case appropriately
    }

    public int enableLED(int i ) {
        if (isReaderConnected()) {
            try {
                Log.d("getting Led enabled : " ,  "Starting ...." );
                if(i == 1 ) {
                    reader.Config.setLedBlinkEnable(true);
                    return 1;
                } else if(i == 0 ){
                    reader.Config.setLedBlinkEnable(false);
                    return 0;
                }    
            } catch (InvalidUsageException | OperationFailureException e) {
                e.printStackTrace();
            }
        }
        return -1; // Return a default value or handle the error case appropriately
    }
    






    ///获取读取器信息
    public   List getReadersList() {
        ArrayList<ReaderDevice> readersListArray=new  ArrayList<ReaderDevice>();
        List<HashMap<String, Object>> items = new ArrayList<>();

        try {
            if(readers!=null) {
                readersListArray = readers.GetAvailableRFIDReaderList();
                Log.d("readersListArray",String.valueOf(readersListArray.size()));

                for (ReaderDevice reader : readersListArray) {
                    HashMap<String, Object> item = new HashMap<>();
                    item.put("name", reader.getName());
                    item.put("id", reader.getAddress());
                    items.add(item);
                }

                return items;
            }
        }catch (InvalidUsageException e){
//            emit(Base.RfidEngineEvents.Error, transitionEntity(Base.ErrorResult.error(error)));
        }
        return  items;
    }


    public class IEventHandler implements RfidEventsListener {
        @Override
        public void eventReadNotify(RfidReadEvents rfidReadEvents) {
            // Recommended to use new method getReadTagsEx for better performance in case of large tag population
            TagData[] myTags = reader.Actions.getReadTags(100);


            if (myTags != null) {
                ArrayList<HashMap<String, Object>> datas= new ArrayList<>();
                for (int index = 0; index < myTags.length; index++) {
                    TagData tagData=myTags[index];
                    Log.d(TAG, "Tag ID " +tagData.getTagID());
                    Log.d(TAG, "Tag getOpCode " +tagData.getOpCode());
                    Log.d(TAG, "Tag getOpStatus " +tagData.getOpStatus());

                    ///读取操作
                    if(tagData.getOpCode()==null || tagData.getOpCode()== ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ){
                        //&&tagData.getOpStatus()== ACCESS_OPERATION_STATUS.ACCESS_SUCCESS
                        Base.RfidData data=new Base.RfidData();
                        data.tagID=tagData.getTagID();
                        data.antennaID=tagData.getAntennaID();
                        data.peakRSSI=tagData.getPeakRSSI(); 
                        data.opStatus=tagData.getOpStatus();
                        data.allocatedSize=tagData.getTagIDAllocatedSize();
                        data.lockData=tagData.getPermaLockData();
                        if(tagData.isContainsLocationInfo()){
                            data.relativeDistance=tagData.LocationInfo.getRelativeDistance();
                        }
                        data.memoryBankData=tagData.getMemoryBankData();
                        datas.add(transitionEntity(data) );
                    }
                }

                if(datas.size()>0){
                    new AsyncDataNotify().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, datas);
                }
            }
        }

        @Override
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                HANDHELD_TRIGGER_EVENT_TYPE handheldEvent = rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent();
                if (handheldEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            
                            handleTriggerPress(true);
                            return null;
                        }
                    }.execute();
                } else if (handheldEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                          
                            handleTriggerPress(false);

                            return null;
                        }
                    }.execute();
                }
            }
        
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.BATTERY_EVENT) {
                final Events.BatteryData batteryData = rfidStatusEvents.StatusEventData.BatteryData;
                Log.d("BatteryData", String.valueOf(batteryData.getCause()));
                Log.d("BatteryData", String.valueOf(batteryData.getLevel()));
                Log.d("BatteryData", String.valueOf(batteryData.getCharging()));
                List<String> xx = Arrays.asList(
                        String.valueOf(batteryData.getCause()),
                        String.valueOf(batteryData.getLevel()),
                        String.valueOf(batteryData.getCharging())
                );
                ArrayList<HashMap<String, Object>> dataArray = new ArrayList<>();
                HashMap<String, Object> item = new HashMap<>();
                item.put("data", xx);
                dataArray.add(item);
                new AsyncBatterieNotify().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, dataArray);
            }
        
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.INVENTORY_START_EVENT ){
                HashMap<String, Object> map =new HashMap<>();
                map.put("L1Status",true);
                emit(Base.RfidEngineEvents.L1Status,map);
            }
            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.INVENTORY_STOP_EVENT ){
                HashMap<String, Object> map =new HashMap<>();
                map.put("L1Status",false);
                emit(Base.RfidEngineEvents.L1Status,map);
            }
            
        
        }
        


    }




    public void handleTriggerPress(boolean pressed) {
        HashMap<String, Object> map =new HashMap<>();
        map.put("L1Status",pressed);
        emit(Base.RfidEngineEvents.L1Status,map);
        if (pressed) {
            
            performInventory();
        } else{
            
            stopInventory();

        }
    }


    synchronized void performInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            if(isLocating ){
                if( tagLocationId !="" && tagLocationId != null){
                    reader.Actions.TagLocationing.Perform(tagLocationId, null, null);

                    Log.d("here is the error", tagLocationId);

                }else {
                    Log.d("here is the error", tagLocationId);


                }
            }else {
                reader.Actions.Inventory.perform();
            }
            
        } catch (InvalidUsageException e) {
            Log.d("here is the error", "lets fix it ");
            e.printStackTrace();
        } catch (OperationFailureException e) {
            Log.d("here is the error 2 ", "lets fix it1  ");

            e.printStackTrace();
        }
    }

    synchronized void stopInventory() {
        // check reader connection
        if (!isReaderConnected())
            return;
        try {
            if(isLocating){
                reader.Actions.TagLocationing.Stop();
            }else {
                reader.Actions.Inventory.stop();
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void RFIDReaderAppeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderAppeared " + readerDevice.getName());
//        new ConnectionTask().execute();
    }

    @Override
    public void RFIDReaderDisappeared(ReaderDevice readerDevice) {
        Log.d(TAG, "RFIDReaderDisappeared " + readerDevice.getName());
//        if (readerDevice.getName().equals(reader.getHostName()))
//            disconnect();
        dispose();
    }

    private  class AsyncDataNotify extends AsyncTask<ArrayList<HashMap<String, Object>>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList<HashMap<String, Object>>... params) {
            HashMap<String,Object> hashMap=new HashMap<>();
            hashMap.put("datas",params[0]);
            emit(Base.RfidEngineEvents.ReadRfid,hashMap);
            return null;
        }
    }
    private  class AsyncBatterieNotify extends AsyncTask<ArrayList<HashMap<String, Object>>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList ... params) {
            HashMap<String,Object> hashMap=new HashMap<>();
            hashMap.put("datas",params[0]);
            emit(Base.RfidEngineEvents.batterieData,hashMap);
            return null;
        }
    }


    //实体类转HashMap
    public static HashMap<String, Object> transitionEntity(Object onClass) {
        HashMap<String, Object> hashMap = new HashMap<String, Object>();
        Field[] fields = onClass.getClass().getDeclaredFields();
        for (Field field : fields) {
            //反射时让私有变量变成可访问
            field.setAccessible(true);
            try {
                hashMap.put(field.getName(), field.get(onClass));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return hashMap;
    }
}