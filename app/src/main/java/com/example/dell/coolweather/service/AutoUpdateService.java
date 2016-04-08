package com.example.dell.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.example.dell.coolweather.receiver.AutoUpdataReceiver;
import com.example.dell.coolweather.util.HttpCallbackListener;
import com.example.dell.coolweather.util.HttpUtil;
import com.example.dell.coolweather.util.Utility;

/**
 * Created by dell on 2016/3/14.
 */
public class AutoUpdateService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeather();
            }
        }).start();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);//定时任务
        int anHour = 8 * 60 * 60 * 1000;//这是8小时的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;//开机后8小时
        Intent i = new Intent(this, AutoUpdataReceiver.class);//每8个小时去启动一次Service，然后开新线程更新天气
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);//参数一context，第二0，第二个intent意图，第四个参数行为：
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    //将最新的天气信息，存入pref中，在再次打开APP的时候，调用initview取出pref中的数据更新界面
    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherCode = prefs.getString("weather_code", "");
        String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Utility.handleWeatherResponse(AutoUpdateService.this, response);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
