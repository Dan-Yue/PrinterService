package com.hui.printerservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 轮询启动service
 * Created by liuhui on 16/3/25.
 */
public class PollingUtil {

    public static void startPrintService(Context mContext,Class<?> cls,long seconds,String action){
        AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(mContext,cls);
        intent.setAction(action);

        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //触发服务的起始时间
        //long triggerAtTime = SystemClock.elapsedRealtime();
        long triggerAtTime = System.currentTimeMillis();

        //manager.cancel(pendingIntent);

        //使用AlarmManger的setRepeating方法设置定期执行的时间间隔（seconds秒）和需要执行的Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            am.setWindow(AlarmManager.RTC_WAKEUP,triggerAtTime+seconds*1000*60,0,pendingIntent);
        }else {
            am.set(AlarmManager.RTC_WAKEUP,triggerAtTime+seconds*6000*1000*60,pendingIntent);
        }
    }
}
