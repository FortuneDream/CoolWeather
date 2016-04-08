package com.example.dell.coolweather.util;

/**
 * Created by dell on 2016/3/10.
 */
public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}
