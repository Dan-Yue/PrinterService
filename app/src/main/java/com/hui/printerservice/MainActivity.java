package com.hui.printerservice;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends Activity implements Printer.PrinterStateCallback{
    private String TAG = MainActivity.class.getSimpleName();
    Printer printer;
    TextView show_info;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        show_info = (TextView) findViewById(R.id.show_info);

        printer = Printer.getPrinter();
        printer.initPrinter(this);
        printer.registerPrinterStateCallback(this);

        PollingUtil.startPrintService(MainActivity.this,PrintService.class, (long) 0.1,PrintService.ACTION);
    }

    @Override
    public void onPrinterAttached(UsbDevice printer) {
        Log.d(TAG,"onPrinterAttached-->");
    }

    @Override
    public void onPrinterDetached(UsbDevice printer) {
        Log.d(TAG,"onPrinterDetached-->");
    }
}
