package com.example.dell.coolweather.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dell.coolweather.R;
import com.example.dell.coolweather.db.CoolWeatherDB;
import com.example.dell.coolweather.model.City;
import com.example.dell.coolweather.model.County;
import com.example.dell.coolweather.model.Province;
import com.example.dell.coolweather.util.HttpCallbackListener;
import com.example.dell.coolweather.util.HttpUtil;
import com.example.dell.coolweather.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dell on 2016/3/10.
 */
public class ChooseAreaActivity extends AppCompatActivity {

    //是否从WeatherAcitivity中跳转出来
    private boolean isFromWeatherActivity;

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleTxt;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<String>();

    //省,市，县列表
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    //选中省份，城市
    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        isFromWeatherActivity=getIntent().getBooleanExtra("from_weather_activity",false);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(this);
        //已经选择了城市且不是从WeatherActivity跳转出来，才会直接跳转到WeatherActivity
        if (prefs.getBoolean("city_selected",false)&&!isFromWeatherActivity){//getboolean的第二个参数是默认的boolean
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.choose_area);
        listView = (ListView) findViewById(R.id.list_view);
        titleTxt = (TextView) findViewById(R.id.txt_title);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        queryProvince();//首先把Province列表打印出来
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //一个listview可以给多个不同的列表实现点击事件，通过判断当前的currentLevel
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }else if (currentLevel==LEVEL_COUNTY){
                    //找到具体的县后跳转界面
                    String countyCode=countyList.get(position).getCountyCode();
                    Intent intent=new Intent(ChooseAreaActivity.this,WeatherActivity.class);
                    intent.putExtra("county_code",countyCode);//如190404,打开新Activity
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private void queryCounties() {
        countyList = coolWeatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleTxt.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        } else {
            queryFromServer(selectedCity.getCityCode(), "county");
        }
    }

    private void queryProvince() {
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleTxt.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        } else {
            queryFromServer(null, "province");//未知province的Code，已经当前要查询的数据类型为province，以及要存入哪个表
        }
    }

    private void queryCities() {
        //cityList保存的是city的所有信息，这里只需要name信息，所以需要一个dataList
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);//显示到这个position
            titleTxt.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        } else {
            //传入选中的province的省级代号，以及
            queryFromServer(selectedProvince.getProvinceCode(), "city");
        }
    }

    private void queryFromServer(final String code, final String type) {
        String address;
        //分析：一级：list3/city.xml罗列了所有的省份，返回文本信息，包含了中国所有的省份名称（provinceName）以及省份代号provinceCode，
        // 格式：01|北京，02|上海，03|天津........
        //二级：list3/city19.xml表示江苏省
        //三级：list3/city1904.xml表示江苏省苏州市  1901|南京,1902|无锡
        //四级：list3/city190404.xml表示江苏省苏州市昆山县 190401|苏州，190402|常熟
        //五级：四级后返回一个数据如：190404|101190404，后半部分表示昆山县的天气代号，
        //六级：再访问http://www.weather.com.cn/data/cityinfo/101190404.html
        //七级:服务器已JSON返回数据给我们
        // {"weatherinfo":
        //      {"city":"昆山","cityid"：”101190404“，"temp":"21.C"....}
        //  }
        //八级开始解析Json数据
        if (!TextUtils.isEmpty(code)) {
            address = "http://www.weather.com.cn/data/list3/city" + code + ".xml";
        } else {
            //第一次查询有哪些省级城市的时候调用
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvincesResponse(coolWeatherDB, response);
                } else if ("city".equals(type)) {
                    result = Utility.handleCitiesResponse(coolWeatherDB, response, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountiesResponse(coolWeatherDB, response, selectedCity.getId());
                }
                //result表示,已经把从网络上的数据存储到数据库中
                if (result) {
                    //通过runOnUIThread方法回到主线程处理逻辑
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //在主线程关闭dialog
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvince();
                                //更新列表
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });

                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog==null){
            progressDialog=new ProgressDialog(this);
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);//点外部不可取消
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    //捕获Back键，根据当前级别来判断，此时应该返回市列表，省列表，还是直接退出
    @Override
    public void onBackPressed() {
        if (currentLevel==LEVEL_COUNTY){
            queryCities();
        }else if (currentLevel==LEVEL_CITY){
            queryProvince();
        }else {
            if (isFromWeatherActivity){
                Intent intent=new Intent(this,WeatherActivity.class);
                startActivity(intent);
            }
            finish();
        }
    }
}
