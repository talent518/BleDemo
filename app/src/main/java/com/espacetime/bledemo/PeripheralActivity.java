package com.espacetime.bledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Created by abao on 2017/6/29.
 */

public class PeripheralActivity extends AppCompatActivity {
    BluetoothManager mBluetoothManager;
    BluetoothGattServer bluetoothGattServer;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    BluetoothGattService service;
    BluetoothGattCharacteristic readCharacteristic;
    BluetoothGattCharacteristic writeCharacteristic;

    TextView tv;

    Handler handler;
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append("onStartSuccess: 广播成功\n\n");
                }
            });
        }

        @Override
        public void onStartFailure(int errorCode) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append("onStartFailure: 广播失败\n\n");
                }
            });
        }
    };
    private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {
        private byte[] recvs = new byte[]{0x4f, 0x4b};

        @Override
        public void onConnectionStateChange(final BluetoothDevice device, final int status, final int newState) {
            // 连接状态改变
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append(String.format("onConnectionStateChange: name(%s), address(%s), status(%d), newState(%d), UUID(%s)\n\n", device.getName(), device.getAddress(), status, newState, service.getUuid()));
                }
            });

//          如果只与IOS(Apple)设备连接时只使用如下注释代码即可，把 switch (newState) { ... } 语句及语句体全删除即可正常使用，不然会报配对异常：device.setPairingConfirmation(true);
//          if(newState == BluetoothProfile.STATE_CONNECTED) {
//              bluetoothGattServer.connect(device, true);
//          }

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    // check bond status
                    if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                        PeripheralActivity.this.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                            @Override
                            public void onReceive(final Context context, final Intent intent) {
                                final String action = intent.getAction();

                                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                                    final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                                    switch (state) {
                                        case BluetoothDevice.BOND_BONDED:
                                            tv.append("onConnectionStateChange: BOND_STATE: BOND_BONDED\n\n");
                                            final BluetoothDevice bondedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                                            // successfully bonded
                                            context.unregisterReceiver(this);

                                            bluetoothGattServer.connect(bondedDevice, true);
                                            break;
                                        case BluetoothDevice.BOND_BONDING:
                                            tv.append("onConnectionStateChange: BOND_STATE: BOND_BONDING\n\n");
                                            break;
                                        case BluetoothDevice.BOND_NONE:
                                            tv.append("onConnectionStateChange: BOND_STATE: BOND_NONE\n\n");
                                            break;
                                        case BluetoothDevice.ERROR:
                                            tv.append("onConnectionStateChange: BOND_STATE: ERROR\n\n");
                                            break;
                                        default:
                                            tv.append("onConnectionStateChange: BOND_STATE: unknown\n\n");
                                            break;
                                    }
                                } else {
                                    tv.append("onConnectionStateChange: " + action + "\n\n");
                                }
                            }
                        }, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

                        // create bond
                        try {
                            device.setPairingConfirmation(true);
                        } catch (final SecurityException e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    tv.append("setPairingConfirmation Error(建议: 中心设备取消蓝牙配对): " + e.getMessage() + "\n\n");
                                }
                            });
                        }
                        device.createBond();
                    } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        bluetoothGattServer.connect(device, true);
                    }
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    // gattServer.cancelConnection(device);
                    bluetoothGattServer.connect(device, true);
                    break;

                default:
                    // do nothing
                    break;
            }
        }

        @Override
        public void onServiceAdded(final int status, final BluetoothGattService service) {
            // 成功添加服务
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append(String.format("onServiceAdded: status(%d), UUID(%s)\n\n", status, service.getUuid()));
                }
            });
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, final int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            // 远程设备请求读取数据
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        tv.append(String.format("响应(%d): %s\n\n", requestId, new String(recvs, "utf-8")));
                    } catch (UnsupportedEncodingException e) {
                        tv.append(String.format("响应(%d): %s\n\n", requestId, MiscHelper.bytes2hex(recvs)));
                    }
                }
            });

            final UUID characteristicUuid = characteristic.getUuid();
//            if (matches(READ_UUID, characteristicUuid)) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, recvs);
//            } else {
//                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
//            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, final int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, final byte[] value) {
            // 远程设备请求写入数据
            handler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        tv.append(String.format("请求(%d): %s\n\n", requestId, new String(value, "utf-8")));
                    } catch (UnsupportedEncodingException e) {
                        tv.append(String.format("请求(%d): %s\n\n", requestId, MiscHelper.bytes2hex(value)));
                    }
                }
            });
            recvs = value;
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
//            if (matches(WRITE_UUID, characteristic.getUuid())) {
//                if (characteristic.getProperties() == (BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
//                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
//                } else {
//                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, new byte[]{});
//                }
//            }
        }

        @Override
        public void onDescriptorReadRequest(final BluetoothDevice device, final int requestId, final int offset, final BluetoothGattDescriptor descriptor) {
            // 远程设备请求读取描述器
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append(String.format("onDescriptorReadRequest: name(%s), address(%s), requestId(%d), offset(%d), descriptor(%s)\n\n", device.getName(), device.getAddress(), requestId, offset, MiscHelper.bytes2hex(descriptor.getValue())));
                }
            });
        }

        @Override
        public void onDescriptorWriteRequest(final BluetoothDevice device, final int requestId, final BluetoothGattDescriptor descriptor, final boolean preparedWrite, final boolean responseNeeded, final int offset, final byte[] value) {
            // 远程设备请求写入描述器
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append(String.format("onDescriptorWriteRequest: name(%s), address(%s), requestId(%d), descriptor(%s), preparedWrite(%b), responseNeeded(%b), offset(%d), value(%s)\n\n", device.getName(), device.getAddress(), requestId, MiscHelper.bytes2hex(descriptor.getValue()), preparedWrite, responseNeeded, offset, MiscHelper.bytes2hex(value)));
                }
            });
        }

        @Override
        public void onExecuteWrite(final BluetoothDevice device, final int requestId, final boolean execute) {
            // 执行挂起写入操作
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append(String.format("onExecuteWrite: name(%s), address(%s), requestId(%d), execute(%b)\n\n", device.getName(), device.getAddress(), requestId, execute));
                }
            });
        }

        @Override
        public void onNotificationSent(final BluetoothDevice device, final int status) {
            // 通知发送
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append(String.format("onNotificationSent: name(%s), address(%s), status(%d)\n\n", device.getName(), device.getAddress(), status));
                }
            });
        }

        @Override
        public void onMtuChanged(final BluetoothDevice device, final int mtu) {
            // mtu改变
            handler.post(new Runnable() {
                @Override
                public void run() {
                    tv.append(String.format("onNotificationSent: name(%s), address(%s), mtu(%d)\n\n", device.getName(), device.getAddress(), mtu));
                }
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral);

        handler = new Handler(getMainLooper());

        tv = (TextView) findViewById(R.id.tv_peripherial);

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }

        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            tv.append("Bluetooth LE Advertising not supported on this device.\n\n");
        }

        bluetoothGattServer = mBluetoothManager.openGattServer(this, bluetoothGattServerCallback);

        readCharacteristic = new BluetoothGattCharacteristic(Settings.READ_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        writeCharacteristic = new BluetoothGattCharacteristic(Settings.WRITE_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
        service = new BluetoothGattService(Settings.SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        while (!service.addCharacteristic(readCharacteristic)) ;
        while (!service.addCharacteristic(writeCharacteristic)) ;
        bluetoothGattServer.addService(service);

        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(false)
                .setIncludeDeviceName(true)
                .addServiceUuid(new ParcelUuid(Settings.SERVICE_UUID))
                .addServiceUuid(new ParcelUuid(Settings.READ_UUID))
                .addServiceUuid(new ParcelUuid(Settings.WRITE_UUID))
                .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(Settings.SERVICE_UUID))
                .addServiceUuid(new ParcelUuid(Settings.READ_UUID))
                .addServiceUuid(new ParcelUuid(Settings.WRITE_UUID))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponse, mAdvertiseCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        bluetoothGattServer.close();
    }

    public void onCleanLog(View view) {
        tv.setText("");
    }
}
