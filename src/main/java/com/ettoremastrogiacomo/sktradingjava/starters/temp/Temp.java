

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.temp;

import java.util.Arrays;
import org.apache.log4j.Logger;
import org.jfree.chart.plot.XYPlot;
import com.ettoremastrogiacomo.sktradingjava.Charts;
import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.HttpFetch;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.ArrayUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.fetchNYSE;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author a241448
 */
public class Temp {

    static Logger LOG = Logger.getLogger(Temp.class);

  

    public TreeMap<UDate,ArrayList<Double>> fetchXETRAEOD(String isin,boolean xetra,UDate mindate,UDate maxdate)throws Exception{
        //NB utilizzare mic=XFRA per dati eod altri mercati europei altrimenti XETR
        String market=xetra?"XETR":"XFRA";
        HttpFetch http = new HttpFetch();
        String url= "https://api.boerse-frankfurt.de/data/price_history?limit=10000&offset=0&mic="+market+"&minDate="+mindate.toYYYYmMMmDD()+"&maxDate="+maxdate.toYYYYmMMmDD()+"&isin="+isin;
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port),Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        String res= new String(http.HttpGetUrl(url,Optional.empty(),Optional.empty()));
        JSONObject    obj = new JSONObject(res);
        JSONArray arr= obj.getJSONArray("data");
        TreeMap<UDate,ArrayList<Double>> values= new TreeMap<>();
        for(int i=0;i<arr.length();i++){                           
            JSONObject e = arr.getJSONObject(i);
            String []dateel=e.getString("date").split("-");
            UDate datev=UDate.genDate(Integer.parseInt(dateel[0]) , Integer.parseInt(dateel[1])-1, Integer.parseInt(dateel[2]), 0, 0, 0);            
            double open=e.getDouble("open");
            double high=e.getDouble("high");
            double low=e.getDouble("low");
            double close=e.getDouble("close");
            double volume=e.getLong("turnoverPieces");
            values.put(datev, new ArrayList<>(Arrays.asList(open,high,low,close,volume)) );
        }                
        return values;
    }
    
    
    
    static public TreeMap<UDate,ArrayList<Double>> fetchEURONEXTEOD(String isin, String market) throws Exception {
        String val=market.contains("EURONEXT")? isin+market.substring(market.indexOf("-")):isin+"-"+market;        
        //https://live.euronext.com/intraday_chart/getChartData/FR0010242511-XPAR/max
        String url="https://live.euronext.com/intraday_chart/getChartData/"+val+"/max";
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
        }
    TreeMap<UDate,ArrayList<Double>> values= new TreeMap<>();
        String res= new String(http.HttpGetUrl(url, Optional.empty(), Optional.empty()));
        JSONArray arr = new JSONArray(res);
        for(int i=0;i<arr.length();i++){                           
            JSONObject e = arr.getJSONObject(i);
            String[] dateel=e.getString("time").substring(0, 10).split("-");
            UDate datev=UDate.genDate(Integer.parseInt(dateel[0]) , Integer.parseInt(dateel[1])-1, Integer.parseInt(dateel[2]), 0, 0, 0);            
            double close=e.getDouble("price");
            double volume=e.getLong("volume");
            values.put(datev, new ArrayList<>(Arrays.asList(close,close,close,close,volume)) );
        }        
            return values;
    }
    

    public static TreeMap<UDate,ArrayList<Double>> fetchMLSEEOD(String symbol,Security.secType sec)throws Exception{
        String market="";
        if (null==sec) throw new Exception(sec+" non gestito");
        else switch (sec) {
            case STOCK:
                market="MTA";
                break;
            case ETF:
            case ETCETN:
                market="ETF";
                break;
            default:
                throw new Exception(sec+" non gestito");
        }
        String url="https://charts.borsaitaliana.it/charts/services/ChartWService.asmx/GetPricesWithVolume";        
        String jsonstr="{\"request\":{\"SampleTime\":\"1d\",\"TimeFrame\":\"5y\",\"RequestedDataSetType\":\"ohlc\",\"ChartPriceType\":\"price\",\"Key\":\""+symbol+"."+market+"\",\"OffSet\":0,\"FromDate\":null,\"ToDate\":null,\"UseDelay\":true,\"KeyType\":\"Topic\",\"KeyType2\":\"Topic\",\"Language\":\"it-IT\"}}";
        HttpFetch http= new HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
        }
        String res=http.sendjsonPostRequest(url, jsonstr);
        JSONObject o= new JSONObject(res);
        JSONArray arr= o.getJSONArray("d");
        TreeMap<UDate,ArrayList<Double>> map= new TreeMap<>()                ;
        for (int i=0;i<arr.length();i++){            
            UDate d=new UDate(arr.getJSONArray(i).getLong(0));
            d=UDate.getNewDate(d, 0, 0, 0);
            map.put(d, new ArrayList<>(Arrays.asList(arr.getJSONArray(i).getDouble(2),arr.getJSONArray(i).getDouble(3),arr.getJSONArray(i).getDouble(4),arr.getJSONArray(i).getDouble(5),arr.getJSONArray(i).getDouble(6))));
        }
        return map;
    }
    
    public static void main(String[] args) throws Exception {
        
    }
}
