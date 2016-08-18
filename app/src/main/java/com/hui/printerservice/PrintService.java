package com.hui.printerservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by liuhui on 16/8/17.
 */
public class PrintService extends Service{
    private String TAG = PrintService.class.getSimpleName();
    public static String ACTION = "my_Service";
    private static final byte[] ALIGN_CENTER = new byte[]{27, 97, 1};// 居中
    private static final byte[] ALIGN_LEFT = new byte[]{27, 97, 0};// 左对齐
    private static final byte[] ALIGN_RIGHT = new byte[]{27, 97, 2};// 右对齐
    private static final byte[] RESET = new byte[]{27, 64};// 重置格式
    int count = 0;

    @Override
    public void onCreate() {
        Log.d(TAG,TAG+" onCreate-->");
        // 初始化打印机
        Printer.getPrinter().print(RESET);
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG,TAG+" onStartCommand-->");
        PollingUtil.startPrintService(this,PrintService.class,1,PrintService.ACTION);

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        byte[] b = new byte[0];
        try {
            b = (count+++"> "+sdf.format(date).toString()+"\n").getBytes("GB18030");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Printer.getPrinter().print(b);
        return super.onStartCommand(intent, flags, startId);
    }

    private static byte[] convertArray(List<byte[]> bytes) {
        int lenght = 0;
        for (int i = 0; i < bytes.size(); i++) {
            lenght += bytes.get(i).length;
        }
        byte[] res = new byte[lenght];
        int index = 0;
        for (int i = 0; i < bytes.size(); i++) {
            byte[] b = bytes.get(i);
            for (int j = 0; j < b.length; j++) {
                res[index] = b[j];
                index++;
            }
        }
        return res;
    }
}
