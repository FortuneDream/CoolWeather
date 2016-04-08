package com.example.dell.coolweather.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.dell.coolweather.service.AutoUpdateService;

/**
 * Created by dell on 2016/3/14.
 */
public class AutoUpdataReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i=new Intent(context, AutoUpdateService.class);
        context.startService(i);
    }
}
