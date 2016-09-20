package com.clj.blesample;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.clj.fastble.BleManager;
import com.clj.fastble.bluetooth.BleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.BluetoothUtil;
import com.clj.fastble.utils.HexUtil;

import java.util.Arrays;

/**
 * Created by 陈利健 on 2016/9/20.
 * 如何按照此框架编写代码
 */
public class DemoActivity extends AppCompatActivity {

    // 下面的所有UUID及指令请根据实际设备替换
    private static final String UUID_SERVICE_LISTEN = "00001810-0000-1000-8000-00805f9b34fb";       // 下面两个特征值所对应的service的UUID
    private static final String UUID_LISTEN_INDICATE = "00002A35-0000-1000-8000-00805f9b34fb";      // indicate特征值的UUID
    private static final String UUID_LISTEN_NOTIFY = "00002A36-0000-1000-8000-00805f9b34fb";        // notify特征值的UUID

    private static final String UUID_SERVICE_OPERATE = "0000fff0-0000-1000-8000-00805f9b34fb";      // 下面两个特征值所对应的service的UUID
    private static final String UUID_OPERATE_WRITE = "0000fff1-0000-1000-8000-00805f9b34fb";        // 设备写特征值的UUID
    private static final String UUID_OPERATE_NOTIFY = "0000fff2-0000-1000-8000-00805f9b34fb";       // 设备监听写完之后特征值数据改变的UUID

    private static final String SAMPLE_WRITE_DATA = "55aa0bb2100705100600ee";     // 要写入设备某一个特征值的指令

    private static final long TIME_OUT = 10000;                                   // 扫描超时时间
    private static final String DEVICE_NAME = "这里写你的设备名";                   // 符合连接规则的蓝牙设备名，即：device.getName
    private static final String TAG = "ble_sample";

    private BleManager bleManager;                                                // Ble核心管理类

    private BluetoothDevice[] bluetoothDevices;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        initView();

        bleManager = BleManager.getInstance();
        bleManager.init(this);
    }

    private void initView() {

        /*******************************关键操作示例**********************************/


        /**扫描出周围所有设备*/
        findViewById(R.id.btn_0).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.scanDevice(new ListScanCallback(TIME_OUT) {
                    @Override
                    public void onDeviceFound(BluetoothDevice[] devices) {
                        Log.i(TAG, "共发现" + devices.length + "台设备");
                        for (int i = 0; i < devices.length; i++) {
                            Log.i(TAG, "name:" + devices[i].getName() + "------mac:" + devices[i].getAddress());
                        }
                        bluetoothDevices = devices;
                    }

                    @Override
                    public void onScanTimeout() {
                        super.onScanTimeout();
                        Log.i(TAG, "搜索时间结束");
                    }
                });
            }
        });

        /**当搜索到周围有设备之后，可以选择直接连某一个设备*/
        findViewById(R.id.btn_01).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (bluetoothDevices == null || bluetoothDevices.length < 1)
                    return;
                BluetoothDevice sampleDevice = bluetoothDevices[0];


                bleManager.connectDevice(sampleDevice, new BleGattCallback() {
                    @Override
                    public void onConnectSuccess(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "连接成功！");
                        gatt.discoverServices();                // 连接上设备后搜索服务
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        Log.i(TAG, "服务被发现！");
                        BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
                        bleManager.getBluetoothState();               // 打印与该设备的当前状态
                    }

                    @Override
                    public void onConnectFailure(BleException exception) {
                        Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                        bleManager.handleException(exception);
                    }
                });
            }
        });

        /**扫描出周围指定名称设备、并连接*/
        findViewById(R.id.btn_1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.connectDevice(
                        DEVICE_NAME,
                        TIME_OUT,
                        new BleGattCallback() {
                            @Override
                            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                                Log.i(TAG, "连接成功！");
                                gatt.discoverServices();                // 连接上设备后搜索服务
                            }

                            @Override
                            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                                Log.i(TAG, "服务被发现！");
                                BluetoothUtil.printServices(gatt);            // 打印该设备所有服务、特征值
                                bleManager.getBluetoothState();               // 打印与该设备的当前状态
                            }

                            @Override
                            public void onConnectFailure(BleException exception) {
                                Log.i(TAG, "连接失败或连接中断：" + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }

                        });
            }
        });

        /**notify*/
        findViewById(R.id.btn_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.notifyDevice(
                        UUID_SERVICE_LISTEN,
                        UUID_LISTEN_NOTIFY,
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "特征值Notify通知数据回调： " + '\n' + Arrays.toString(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "特征值Notify通知回调失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });

        /**indicate*/
        findViewById(R.id.btn_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.indicateDevice(
                        UUID_SERVICE_LISTEN,
                        UUID_LISTEN_INDICATE,
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "特征值Indicate通知数据回调： " + '\n' + Arrays.toString(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "特征值Indicate通知回调失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });

        /**write*/
        findViewById(R.id.btn_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.writeDevice(
                        UUID_SERVICE_OPERATE,
                        UUID_OPERATE_WRITE,
                        HexUtil.hexStringToBytes(SAMPLE_WRITE_DATA),
                        new BleCharacterCallback() {
                            @Override
                            public void onSuccess(BluetoothGattCharacteristic characteristic) {
                                Log.d(TAG, "写特征值成功: " + '\n' + Arrays.toString(characteristic.getValue()));
                            }

                            @Override
                            public void onFailure(BleException exception) {
                                Log.e(TAG, "写读特征值失败: " + '\n' + exception.toString());
                                bleManager.handleException(exception);
                            }
                        });
            }
        });


        /**刷新缓存操作*/
        findViewById(R.id.btn_6).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.refreshDeviceCache();
            }
        });

        /**关闭操作*/
        findViewById(R.id.btn_7).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bleManager.closeBluetoothGatt();
            }
        });
    }


    /*******************************移除某一个回调示例**********************************/

    /**
     * 将回调实例化，而不是以匿名对象的形式
     */
    BleCharacterCallback bleCharacterCallback = new BleCharacterCallback() {
        @Override
        public void onSuccess(BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "特征值Notification通知数据回调： "
                    + '\n' + Arrays.toString(characteristic.getValue())
                    + '\n' + HexUtil.encodeHexStr(characteristic.getValue()));
        }

        @Override
        public void onFailure(BleException exception) {
            Log.e(TAG, "特征值Notification通知回调失败: " + '\n' + exception.toString());
            bleManager.handleException(exception);
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.closeBluetoothGatt();
        bleManager.disableBluetooth();
    }



}
