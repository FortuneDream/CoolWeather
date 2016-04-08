package com.example.dell.coolweather.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.example.dell.coolweather.db.CoolWeatherDB;
import com.example.dell.coolweather.model.City;
import com.example.dell.coolweather.model.County;
import com.example.dell.coolweather.model.Json;
import com.example.dell.coolweather.model.Province;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by dell on 2016/3/10.
 */
public class Utility {
    //解析数据
    public synchronized static boolean handleProvincesResponse(CoolWeatherDB coolWeatherDB, String response) {
        if (!TextUtils.isEmpty(response)) {
            //regex分解字符串
            String[] allProvinces = response.split(",");
            if (allProvinces.length > 0) {
                for (String p : allProvinces) {
                    String[] array = p.split("\\|");
                    Province province = new Province();
                    province.setProvinceCode(array[0]);
                    province.setProvinceName(array[1]);
                    //将解析出来的数据存储到Province表
                    coolWeatherDB.SaveProvince(province);
                }
                return true;
            }
        }
        return false;
    }

    public synchronized static boolean handleCitiesResponse(CoolWeatherDB coolWeatherDB, String response, int provinceId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCities = response.split(",");
            if (allCities.length > 0) {
                for (String c : allCities) {
                    String[] array = c.split("\\|");
                    City city = new City();
                    city.setCityCode(array[0]);
                    city.setCityName(array[1]);
                    city.setProvinceId(provinceId);
                    //将解析出来的数据存储到City表
                    coolWeatherDB.saveCity(city);
                }
                return true;
            }
        }
        return false;
    }

    public synchronized static boolean handleCountiesResponse(CoolWeatherDB coolWeatherDB, String response, int cityId) {
        if (!TextUtils.isEmpty(response)) {
            String[] allCounties = response.split(",");
            if (allCounties.length > 0) {
                for (String c : allCounties) {
                    String[] array = c.split("\\|");
                    County county = new County();
                    county.setCountyCode(array[0]);
                    county.setCountyName(array[1]);
                    county.setCityId(cityId);
                    //将解析出来的数据存储到County表
                    coolWeatherDB.savaCounty(county);
                }
                return true;
            }
        }
        return false;
    }

    public static void handleWeatherResponse(Context context, String response) {
        try {
            //Gson解析数据标准步骤，注意JAVABean的要嵌套，且每一个元素的变量名字要和JSON数据一致，
            Gson gson=new GsonBuilder().create();
            Json json=gson.fromJson(response,Json.class);
            //之后存储数据
            saveWeatherInfo(context,json.getWeatherInfo().getCity(),json.getWeatherInfo().getCityid(),json.getWeatherInfo().getTemp1(),json.getWeatherInfo().getTemp2(),json.getWeatherInfo().getWeather(),json.getWeatherInfo().getPtime());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveWeatherInfo(Context context, String cityName, String weatherCode, String temp1, String temp2, String weatherDesp, String publishTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINA);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean("city_selected", true);
        editor.putString("city_name", cityName);
        editor.putString("weather_code", weatherCode);
        editor.putString("temp1", temp1);
        editor.putString("temp2", temp2);
        editor.putString("weather_desp", weatherDesp);
        editor.putString("publish_time", publishTime);
        editor.putString("current_date", sdf.format(new Date()));
        editor.apply();//尽量用apply
    }


}
