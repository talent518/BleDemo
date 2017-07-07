package com.espacetime.bledemo;

import android.bluetooth.BluetoothClass;

import java.util.UUID;

/**
 * Created by abao on 2017/7/7.
 */

public abstract class Settings {
    public static final UUID SERVICE_UUID = MiscHelper.fromShortValue(BluetoothClass.Device.WEARABLE_GLASSES);
    public static final UUID READ_UUID = MiscHelper.fromShortValue(BluetoothClass.Device.Major.WEARABLE | 0x20);
    public static final UUID WRITE_UUID = MiscHelper.fromShortValue(BluetoothClass.Device.Major.WEARABLE | 0x21);
}
