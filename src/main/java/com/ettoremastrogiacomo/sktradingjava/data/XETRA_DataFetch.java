/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Init;
import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.computeHashcode;
import com.ettoremastrogiacomo.utils.HttpFetch;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author sk
 */
public class XETRA_DataFetch {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(XETRA_DataFetch.class);
    static java.util.HashMap<String, java.util.HashMap<String, String>> fetchListDE() throws Exception {
        //String XetraSuffix="ETR";
        String XetraURL = "http://www.xetra.com/xetra-en/instruments/shares/list-of-tradable-shares";
        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
        String url, type = "STOCK", currency = "EUR", market = "XETRA";
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
        }

        String s = new String(http.HttpGetUrl(XetraURL, Optional.empty(), Optional.empty()));
        Document doc = Jsoup.parse(s);
        Elements buttons = doc.select("button[name='PageNum'");
        Elements forms = doc.select("form[class='pagination pagination-top']");
        Elements state = doc.select("input[name='state']");

        int maxpg = 0;
        for (Element x : buttons) {
            if (x.text().matches("\\d*")) {

                if (Integer.parseInt(x.text()) > maxpg) {
                    maxpg = Integer.parseInt(x.text());
                }
            }
        }
        //for (Element x : ll) {LOG.debug(x.text());}
        XetraURL = "http://www.xetra.com" + forms.attr("action") + "?state=" + state.attr("value") + "&sort=sTitle+asc&hitsPerPage=10&pageNum=#";

        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        for (int i = 0; i < maxpg; i++) {
            url = XetraURL.replace("#", Integer.toString(i));

            s = new String(http.HttpGetUrl(url, Optional.empty(), Optional.empty()));

            doc = Jsoup.parse(s);
            Elements ll = doc.select("ol[class='list search-results '] p:containsOwn(ISIN:)");
            Elements names = doc.select("ol[class='list search-results ']  li  h4  a");
            //Elements links = doc.select("ol[class='list search-results ']  li  h4  a[href]");
            if (ll.size() != names.size()) {
                throw new Exception("xml mismatch");
            }
            for (int j = 0; j < ll.size(); j++) {
                Element x = ll.get(j);
                Element y = names.get(j);
                if (!list.contains(x.text().replace("ISIN: ", ""))) {
                    String isin = x.text().replace("ISIN: ", "").trim();
                    String tolink = names.get(j).attr("href");
                    String name = names.get(j).text();
                    list.add(isin);
                    //LOG.info(det.replace("#", isin));
                    String isindet = new String(http.HttpGetUrl("https://www.xetra.com" + tolink, Optional.empty(), Optional.empty()));

                    String symbol = Jsoup.parse(isindet).select("dt:containsOwn(Mnemonic) + dd").text();
                    //String isindet = new String(http.HttpGetUrl(det.replace("#", isin), Optional.empty(), Optional.empty()));
                    //String symbol = Jsoup.parse(isindet).select("div[class='ln_symbol line']  span").text();
                    //String sector = Jsoup.parse(isindet).select("div[class='ln_sector line']  span").text();
                    //String market= Jsoup.parse(isindet).select("div[class='ln_homemarket line']  span").text();
                    java.util.HashMap<String, String> map = new java.util.HashMap<>();
                    map.put("type", "STOCK");
                    map.put("market", "XETRA");
                    map.put("currency", "EUR");
                    map.put("sector", "NA");
                    map.put("isin", isin);
                    map.put("code", symbol);
                    map.put("name", name);
                    if (!symbol.isEmpty()) {

                        String hash = computeHashcode(map.get("isin"), map.get("market")) ;//Encoding.base64encode(getSHA1(String2Byte((map.get("isin") + map.get("market")))));
                        if (!all.containsKey(hash)) {
                            all.put(hash, map);
                        }
                        LOG.debug(isin + "\t" + symbol + "\t" + y.text());
                    }
                }

            }

        }
        LOG.debug("all = " + list.size());
        return all;
    }
    
    static public TreeMap<UDate,ArrayList<Double>> fetchXETRAEOD(String isin,boolean xetra,UDate mindate,UDate maxdate)throws Exception{
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
    
    static public void fetchAndLoadXETRAEOD() throws Exception {
        java.util.HashMap<String, java.util.HashMap<String, String>> m = fetchListDE();
        m.keySet().forEach((x) -> {
            LOG.debug(x);
            m.get(x).keySet().forEach((y) -> {
                LOG.debug(y + "\t" + m.get(x).get(y));
            });            
        });        
        com.ettoremastrogiacomo.utils.HttpFetch httpf = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            httpf.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
        }        
        HashMap<String, TreeMap<UDate, ArrayList<Double>>> datamap = new HashMap<>();        
        for (String x : m.keySet()) {
            String isin = m.get(x).get("isin");
            UDate maxdate= new UDate();
            UDate mindate=maxdate.getDayOffset(-365*10);//10 y ago
            TreeMap<UDate, ArrayList<Double>> data = new TreeMap<>();
            fetchXETRAEOD(isin, true, mindate, maxdate);
            datamap.put(x, data);
            LOG.debug("fetched data from " + m.get(x).get("name"));
        }

        String sql1="insert or replace into eoddatav2(hashcode,date,open,high,low,close,volume,oi,provider) values(?,?,?,?,?,?,?,?,?)";        
        String sql2="insert or replace into shares(hashcode,isin,name,code,type,market,currency,sector) values(?,?,?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(Init.db_url);                
            PreparedStatement ps = conn.prepareStatement(sql1);PreparedStatement ps2 = conn.prepareStatement(sql2);                
                ) {
            conn.setAutoCommit(false);
            for (String s:m.keySet()){                
                ps2.setString(1, s);
                ps2.setString(2, m.get(s).get("isin"));
                ps2.setString(3, m.get(s).get("name"));
                ps2.setString(4, m.get(s).get("code"));
                ps2.setString(5, m.get(s).get("type"));
                ps2.setString(6, m.get(s).get("market"));
                ps2.setString(7, m.get(s).get("currency"));
                ps2.setString(8, m.get(s).get("sector"));
                ps2.addBatch();                
                TreeMap<UDate, ArrayList<Double>> data=datamap.get(s);                
                for (UDate d : data.keySet()){
                    ps.setString(1, s);
                    ps.setString(2, d.toYYYYMMDD());
                    ps.setFloat(3, data.get(d).get(0).floatValue());
                    ps.setFloat(4, data.get(d).get(1).floatValue());
                    ps.setFloat(5, data.get(d).get(2).floatValue());
                    ps.setFloat(6, data.get(d).get(3).floatValue());
                    ps.setFloat(7, data.get(d).get(4).floatValue());
                    ps.setFloat(8, 0.0f);
                    ps.setString(9,"XETRA");                    
                    ps.addBatch();                    
                }
            }
            LOG.debug(Arrays.toString(ps.executeBatch()) );            
            LOG.debug(Arrays.toString(ps2.executeBatch()));                        
            conn.commit();
           } catch (SQLException e) {
               LOG.warn(e);
           }        
        
    }        
    
}
