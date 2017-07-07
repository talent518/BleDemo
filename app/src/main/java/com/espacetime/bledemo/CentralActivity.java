package com.espacetime.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CentralActivity extends AppCompatActivity {
    private static final String[] DeviceMajors = new String[]{
            "混合", "电脑", "手机", "网络",
            "A/V", "外设", "成像", "穿戴/佩带",
            "玩具", "医疗保健", "-", "-",
            "-", "-", "-", "未分类"
    };
    // 蓝牙相关
    private static final long SCAN_PERIOD = 10000; // 10秒
    Button mBtnScanBlue;
    GridView mGvDevice;
    TextView mTvLog;
    ArrayList<HashMap<String, String>> mArrayList = new ArrayList<HashMap<String, String>>();
    SimpleAdapter adapter;
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeScanner mBluetoothLeScanner;
    boolean mScanning = false;

    ArrayList<ScanResult> mScanResults = new ArrayList<ScanResult>();
    HashMap<String, Boolean> mAddressMap = new HashMap<String, Boolean>();

    BluetoothGatt mBluetoothGatt = null;
    boolean isAutoConnect = true;

    public BluetoothGattService mService = null;
    public BluetoothGattCharacteristic mReadCharacteristic = null;
    public BluetoothGattCharacteristic mWriteCharacteristic = null;

    private android.bluetooth.BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    log("onConnectionStateChange: Connecting");
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    log("onConnectionStateChange: Connected");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    log("onConnectionStateChange: Disconnecting");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    log("onConnectionStateChange: Disconnected");

                    if(isAutoConnect) {
                        log("AutoConnecting");
                        gatt.connect();
                    }
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            log("onServicesDiscovered: status = " + status);

            if(BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }

            mService = gatt.getService(Settings.SERVICE_UUID);
            if(mService == null) {
                return;
            }

            mReadCharacteristic = mService.getCharacteristic(Settings.READ_UUID);
            mWriteCharacteristic = mService.getCharacteristic(Settings.WRITE_UUID);

            log("mReadCharacteristic: " + mBluetoothGatt.readCharacteristic(mReadCharacteristic));
        }

        private boolean isReaded = false;

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            log("onCharacteristicRead: characteristic = " + MiscHelper.bytes2hex(characteristic.getValue()) + ", status = " + status);

            if(!isReaded) {
                isReaded = true;
                try {
                    mWriteCharacteristic.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    mWriteCharacteristic.setValue(((new Date()).getTime() + "测试BLE消息发送。").getBytes("utf-8"));
                    log("mWriteCharacteristic: " + mBluetoothGatt.writeCharacteristic(mWriteCharacteristic));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean isWrited = false;

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            log("onCharacteristicWrite: characteristic = " + MiscHelper.bytes2hex(characteristic.getValue()) + ", status = " + status);

            if(!isWrited) {
                isWrited = true;
                log("mReadCharacteristic: " + mBluetoothGatt.readCharacteristic(mReadCharacteristic));
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            log("onCharacteristicChanged: characteristic = " + MiscHelper.bytes2hex(characteristic.getValue()));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            log("onDescriptorRead: descriptor = " + MiscHelper.bytes2hex(descriptor.getValue()) + ", status = " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            log("onDescriptorWrite: descriptor = " + MiscHelper.bytes2hex(descriptor.getValue()) + ", status = " + status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            log("onReliableWriteCompleted: status = " + status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            log("onReadRemoteRssi: rssi = " + rssi + ", status = " + status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            log("onReadRemoteRssi: mtu = " + mtu + ", status = " + status);
        }
    };

    // 单击某个列表选项的事件
    private AdapterView.OnItemClickListener onLvDeviceItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ScanResult result = mScanResults.get(position);
            log("onLvDeviceItemClickListener(position = " + position + ", id = " + id + "): " + result);

            mBluetoothGatt = result.getDevice().connectGatt(CentralActivity.this, false, mGattCallback);

            for(int i = 0; i<parent.getCount(); i++) {
                if(position == i) {
                    view.setBackgroundColor(0xFFCCCCCC);
                } else {
                    parent.getChildAt(i).setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    };

    // 蓝牙扫描事件
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (mAddressMap.get(device.getAddress()) != null) {
                return;
            }
            mAddressMap.put(device.getAddress(), true);

            log("onScanResult: callbackType = " + callbackType + ", result = " + result);

            HashMap<String, String> map;
            map = new HashMap<String, String>();
            map.put("name", device.getName() == null ? "unknown" : device.getName());
            map.put("address", device.getAddress());
            map.put("class", DeviceMajors[device.getBluetoothClass().getMajorDeviceClass() >> 8 & 0xf]);
            map.put("rssi", Integer.toString(result.getRssi()));
            mArrayList.add(map);
            mScanResults.add(result);

            adapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            log("onBatchScanResults: " + results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            log("onScanFailed: " + errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central);

        adapter = new SimpleAdapter(this, mArrayList, R.layout.lv_central_item, new String[]{"name", "address", "class", "rssi"}, new int[]{R.id.tv_central_item_name, R.id.tv_central_item_address, R.id.tv_central_item_class, R.id.tv_central_item_rssi});

        mBtnScanBlue = (Button) findViewById(R.id.btn_central);
        mGvDevice = (GridView) findViewById(R.id.gv_central);
        mGvDevice.setOnItemClickListener(onLvDeviceItemClickListener);
        mGvDevice.setAdapter(adapter);

        mTvLog = (TextView) findViewById(R.id.tv_central);

        // 蓝牙相关
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public void onBtnScanBlueClient(View view) {
        // 经过预定扫描期后停止扫描
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBtnScanBlue.setEnabled(true);
                mBluetoothLeScanner.stopScan(mScanCallback);
            }
        }, SCAN_PERIOD);
        mArrayList.clear();
        mScanResults.clear();
        mAddressMap.clear();
        mBtnScanBlue.setEnabled(false);

        mScanning = true;
        mBluetoothLeScanner.startScan(mScanCallback);
    }

    public void log(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvLog.append(str);
                mTvLog.append("\n\n");
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mScanning) {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
        }

        isAutoConnect = false;
        if(mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        super.onDestroy();
    }

    public void onClearLog(View view) {
        mTvLog.setText("");
    }
}
