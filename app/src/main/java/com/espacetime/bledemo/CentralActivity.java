package com.espacetime.bledemo;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CentralActivity extends AppCompatActivity {
    private static final String[] DeviceMajors = new String[]{
            "混合", "电脑", "手机", "网络",
            "A/V", "外设", "成像", "穿戴/佩带",
            "玩具", "医疗保健", "-", "-",
            "-", "-", "-", "未分类"};
    private static final BleUtil mBLE = BleUtil.getInstance();
    Button mBtnScanBlue;
    GridView mGvDevice;

    ArrayList<HashMap<String, String>> mArrayList = new ArrayList<HashMap<String, String>>();
    SimpleAdapter adapter;
    private BleUtil.BTUtilListener mBtUtilListener = new BleUtil.BTUtilListener() {
        private void log(String str) {
            Log.v("BTUtilListener", str);
            Toast.makeText(CentralActivity.this, str, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onLeScanStart() {
            log("onLeScanStart");
        }

        @Override
        public void onLeScanStop() {
            log("onLeScanStop");
        }

        @Override
        public void onLeScanDevices(List<BluetoothDevice> listDevice) {
            mArrayList.clear();

            HashMap<String, String> map;
            for (BluetoothDevice device : listDevice) {
                map = new HashMap<String, String>();
                map.put("name", device.getName() == null ? "unknown" : device.getName());
                map.put("address", device.getAddress());
                map.put("class", DeviceMajors[device.getBluetoothClass().getMajorDeviceClass() >> 8 & 0xf]);
                mArrayList.add(map);
            }

            log("onLeScanDevices: " + mArrayList);

            adapter.notifyDataSetChanged();
        }

        @Override
        public void onConnected(BluetoothDevice mCurDevice) {
            log("onConnected: name = " + mCurDevice.getName() + ", address = " + mCurDevice.getAddress());
        }

        @Override
        public void onDisConnected(BluetoothDevice mCurDevice) {
            log("onDisConnected: name = " + mCurDevice.getName() + ", address = " + mCurDevice.getAddress());
        }

        @Override
        public void onConnecting(BluetoothDevice mCurDevice) {
            log("onConnecting: name = " + mCurDevice.getName() + ", address = " + mCurDevice.getAddress());
        }

        @Override
        public void onDisConnecting(BluetoothDevice mCurDevice) {
            log("onDisConnecting: name = " + mCurDevice.getName() + ", address = " + mCurDevice.getAddress());
        }

        @Override
        public void onStrength(int strength) {
            log("onConnected: " + strength);
        }

        @Override
        public void onModel(int model) {
            log("onModel: " + model);
        }
    };
    private AdapterView.OnItemClickListener onLvDeviceItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mBLE.connectLeDevice((int) id);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central);

        adapter = new SimpleAdapter(this, mArrayList, R.layout.lv_central_item, new String[]{"name", "address", "class"}, new int[]{R.id.tv_central_item_name, R.id.tv_central_item_address, R.id.tv_central_item_class});

        mBtnScanBlue = (Button) findViewById(R.id.btn_central);
        mGvDevice = (GridView) findViewById(R.id.gv_central);
        mGvDevice.setOnItemClickListener(onLvDeviceItemClickListener);
        mGvDevice.setAdapter(adapter);

        mBLE.setContext(this);
        mBLE.setBTUtilListener(mBtUtilListener);
    }

    public void onBtnScanBlueClient(View view) {
        mBLE.scanLeDevice(true);
    }
}
