package com.hui.printerservice;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ShenJian on 16/5/16.
 */
public class Printer {

    private static final String TAG = "Printer";

    private static final String ACTION_USB_PERMISSION = "com.zline.zlinekitchen.USB_PERMISSION";

    private PendingIntent mPermissionIntent;

    private UsbManager mUsbManager = null;

    private UsbDeviceConnection mConnection = null;

    private UsbDevice mPrintDevice = null;

    private UsbInterface mUsbInterface = null;

    private static Printer sPrinter = new Printer();

    private Printer() {
    }

    public static Printer getPrinter() {
        return sPrinter;
    }

    public void initPrinter(Context context) {
        Log.d(TAG, "initPrinter");
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mUsbReceiver, filter);

        if (findPrintDevice()) {
            // 寻找到新的打印机
            for (PrinterStateCallback callback : mPrinterCallbacks) {
                callback.onPrinterAttached(mPrintDevice);
            }
        }
    }

    public void closePrinter(Context context) {
        context.unregisterReceiver(mUsbReceiver);
        close();
    }

    private boolean findPrintDevice() {
        Log.d(TAG, "正在寻找打印机……");
        mPrintDevice = null;
        mUsbInterface = null;
        Map<String, UsbDevice> map = mUsbManager.getDeviceList();
        for (Iterator<UsbDevice> iter = map.values().iterator(); iter.hasNext(); ) {
            UsbDevice dev = iter.next();
            if (isPrinter(dev)) {
                mPrintDevice = dev;
                mUsbInterface = dev.getInterface(0);
                break;
            }
        }
        if (mPrintDevice != null) {
            Log.d(TAG, "找到打印机：" + mPrintDevice.toString());
            mUsbManager.requestPermission(mPrintDevice, mPermissionIntent);
            return true;
        } else {
            Log.d(TAG, "没有找到打印机");
            return false;
        }
    }

    private boolean isPrinter(UsbDevice device) {
        UsbInterface usbInterface = device.getInterface(0);

        // USB协议规定的打印机类别代码为07H，可用以区分打印机和其他USB设备
        // http://www.usb.org/developers/defined_class
        if (usbInterface != null && usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_PRINTER) {
            return true;
        }
        return false;
    }

    private void close() {
        if (mConnection != null) {
            if (mUsbInterface != null) {
                mConnection.releaseInterface(mUsbInterface);
            }
            mConnection.close();
        }
        mUsbInterface = null;
        mPrintDevice = null;
        mConnection = null;
    }

    public UsbDevice getPrinterDevice() {
        return mPrintDevice;
    }

    public void print(byte[] content) {
        if (mPrintDevice == null) {
            Log.d(TAG, "没有连接打印机，打印失败");
            return;
        }
        UsbEndpoint outPoint = null;
        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
            UsbEndpoint point = mUsbInterface.getEndpoint(i);
            if (point.getDirection() == UsbConstants.USB_DIR_OUT) {
                outPoint = point;
                break;
            }
        }
        if (outPoint == null) {
            return;
        }
        new Thread(new PrintTask(content, outPoint)).start();
    }

    class PrintTask implements Runnable {
        byte[] content;
        UsbEndpoint point = null;

        public PrintTask(byte[] content, UsbEndpoint point) {
            this.content = content;
            this.point = point;
        }

        @Override
        public void run() {
            synchronized (Printer.this) {
                Log.d(TAG, "开始打印……");
                long startTime = System.currentTimeMillis();

                mConnection.claimInterface(mUsbInterface, true);
                int ret = mConnection.bulkTransfer(point, content, content.length, 20 * 1000);
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "输出" + ret + "字节，耗时：" + (endTime - startTime) + "ms");
                //mConnection.releaseInterface(mUsbInterface);
            }
        }
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    Log.d(TAG, "获取打印机权限成功");
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (device == null) {
                        return;
                    }
                    if (device.getDeviceId() == mPrintDevice.getDeviceId()) {
                        Log.d(TAG, "打开打印机连接");
                        mConnection = mUsbManager.openDevice(mPrintDevice);
                        mConnection.claimInterface(mUsbInterface, true);
                    }
                } else {
                    Log.d(TAG, "获取打印机权限失败");
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device == null) {
                    return;
                }
                Log.d(TAG, "USB设备已断开：" + device.toString());
                if (mPrintDevice != null && mPrintDevice.getDeviceId() == device.getDeviceId()) {
                    Log.d(TAG, "打印机断开");
                    close();
                    if (findPrintDevice()) {
                        // 寻找到新的打印机
                        for (PrinterStateCallback callback : mPrinterCallbacks) {
                            callback.onPrinterAttached(mPrintDevice);
                        }
                    } else {
                        for (PrinterStateCallback callback : mPrinterCallbacks) {
                            callback.onPrinterDetached(device);
                        }
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.d(TAG, "USB设备已连接：" + device.toString());
                    if (mPrintDevice == null) {
                        Log.d(TAG, "当前没有连接打印机，开始搜索打印机");
                        if (findPrintDevice()) {
                            // 寻找到新的打印机
                            for (PrinterStateCallback callback : mPrinterCallbacks) {
                                callback.onPrinterAttached(mPrintDevice);
                            }
                        }
                    }
                }
            }
        }
    };

    private List<PrinterStateCallback> mPrinterCallbacks = new ArrayList<PrinterStateCallback>();

    public void registerPrinterStateCallback(PrinterStateCallback callback) {
        if (!mPrinterCallbacks.contains(callback)) {
            mPrinterCallbacks.add(callback);
        }
    }

    public void unRegisterPrinterStateCallback(PrinterStateCallback callback) {
        if (mPrinterCallbacks.contains(callback)) {
            mPrinterCallbacks.remove(callback);
        }
    }

    public interface PrinterStateCallback {
        public void onPrinterAttached(UsbDevice printer);

        public void onPrinterDetached(UsbDevice printer);
    }
}
