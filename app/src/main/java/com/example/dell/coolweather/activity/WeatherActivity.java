package com.example.dell.coolweather.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.dell.coolweather.R;
import com.example.dell.coolweather.service.AutoUpdateService;
import com.example.dell.coolweather.util.HttpCallbackListener;
import com.example.dell.coolweather.util.HttpUtil;
import com.example.dell.coolweather.util.Utility;

/**
 * Created by dell on 2016/3/14.
 */
public class WeatherActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout weatherInfoLayout;

    //用于显示城市名
    private TextView cityNameText;

    //用于显示发布时间
    private TextView publishText;

    //用于显示天气描述信息
    private TextView weatherDespText;

    //用于显示气温1
    private TextView temp1Text;

    //用于显示气温2
    private TextView temp2Text;

    //用于显示当前日期
    private TextView currentDataText;

    //切换城市按钮
    private Button switchCity;

    //更新天气按钮
    private Button refreshWeather;

    //用于显示
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_layout);
        initView();
    }

    private void initView() {
        weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
        cityNameText = (TextView) findViewById(R.id.city_name);
        publishText = (TextView) findViewById(R.id.txt_publish);
        weatherDespText = (TextView) findViewById(R.id.weather_desp);
        temp1Text = (TextView) findViewById(R.id.temp1);
        temp2Text = (TextView) findViewById(R.id.temp2);
        currentDataText = (TextView) findViewById(R.id.current_data);
        switchCity = (Button) findViewById(R.id.switch_city);
        refreshWeather = (Button) findViewById(R.id.refresh_weather);
        //得到county_code
        String countyCode = getIntent().getStringExtra("county_code");

        if (!TextUtils.isEmpty(countyCode)) {
            //县级代号时就去查询天气
            publishText.setText("同步中...");
            weatherInfoLayout.setVisibility(View.INVISIBLE);//
            cityNameText.setVisibility(View.INVISIBLE);//同步时隐藏着两个属性，不能用gone
            queryWeatherCode(countyCode);
        } else {
            //没有县级代号时就直接显示天气
            showWeather();
        }
        switchCity.setOnClickListener(this);
        refreshWeather.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_city:
                Intent intent = new Intent(this, ChooseAreaActivity.class);
                intent.putExtra("from_weather_activity", true);
                startActivity(intent);
                finish();
                break;
            case R.id.refresh_weather:
                publishText.setText("同步中...");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                String weatherCode = prefs.getString("weather_code", "");
                if (!TextUtils.isEmpty(weatherCode)) {
                    queryWeatherInfo(weatherCode);
                }
                break;
            default:
                break;
        }
    }

    //查询县级代号所对应天气代号
    private void queryWeatherCode(String countyCode) {
        String address = "http://www.weather.com.cn/data/list3/city" + countyCode + ".xml";
        queryFromServer(address, "countyCode");
    }

    //查询天气代号所对应的天气
    private void queryWeatherInfo(String weatherCode) {
        String address = "http://www.weather.com.cn/data/cityinfo/" + weatherCode + ".html";
        queryFromServer(address, "weatherCode");
    }


    private void queryFromServer(final String address, final String type) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                if ("countyCode".equals(type)) {
                    if (!TextUtils.isEmpty(response)) {
                        //从服务器返回的数据解析出天气代号
                        String[] array = response.split("\\|");
                        if (array.length == 2) {
                            String weatherCode = array[1];//第二个参数才是天气代号
                            queryWeatherInfo(weatherCode);//开始查询天气
                        }
                    }
                } else if ("weatherCode".equals(type)) {
                    //处理服务器返回的天气信息,传入context是因为sharepreference需要
                    Utility.handleWeatherResponse(WeatherActivity.this, response);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showWeather();
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        publishText.setText("同步失败....");
                    }
                });
            }
        });
    }

    private void showWeather() {
        SharedPreferences prefs =PreferenceManager.getDefaultSharedPreferences(this);//从prefs中得到数据
        cityNameText.setText(prefs.getString("city_name",""));
        temp1Text.setText(prefs.getString("temp1",""));
        temp2Text.setText(prefs.getString("temp2",""));
        weatherDespText.setText(prefs.getString("weather_desp",""));
        publishText.setText("今天"+prefs.getString("publish_time","")+"发布");
        currentDataText.setText(prefs.getString("current_data",""));
        weatherInfoLayout.setVisibility(View.VISIBLE);
        cityNameText.setVisibility(View.VISIBLE);
        Intent intent=new Intent(this, AutoUpdateService.class);//启动自动更新服务
        startService(intent);
    }
}
