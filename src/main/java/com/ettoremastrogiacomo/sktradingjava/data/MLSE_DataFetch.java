/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.Security.secType;
import com.ettoremastrogiacomo.utils.HttpFetch;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
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
public class MLSE_DataFetch {

    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FetchData.class);
    static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    static UDate StrBIT2UDate(String s) {
        String pdr = s;
        UDate d;
        int y = pdr.length() >= 8 ? Integer.parseInt(pdr.substring(6, 8)) : Integer.parseInt(pdr.substring(6));
        int M = Integer.parseInt(pdr.substring(3, 5));
        int day = Integer.parseInt(pdr.substring(0, 2));
        d = UDate.genDate(y + 2000, M - 1, day, 22, 0, 0);
        return d;
    }

    public static UDate ultimoGiornoContrattazioni() throws Exception {
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }

        String s = new String(http.HttpGetUrl("https://www.borsaitaliana.it/borsa/indici/indici-in-continua/dettaglio.html?indexCode=FTSEMIB&lang=it", Optional.of(20), Optional.empty()));
        Document doc = Jsoup.parse(s);
        String t = "Data - Ora Ultimo Valore ";
        Elements el = doc.select("span[class=\"t-text -block -size-xs | u-hidden -xs\"]");
        LOG.debug("ultima data contrattazione #" + el.get(0).text().substring(t.length(), t.length() + 8) + "#");
        return StrBIT2UDate(el.get(0).text().substring(t.length(), t.length() + 8));
    }

    public static java.util.HashMap<String, java.util.HashMap<String, String>> fetchMLSEList(secType st) throws Exception {
        int cnt = 1;
        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
        String url, urldet, type, currency, market;
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }//javascript:loadBoxContentCustom('tableResults','/borsa/etf/search.html?comparto=ETC&idBenchmarkStyle=&idBenchmark=&indexBenchmark=&sectorization=&lang=it&page=4')
        final String FUTURES_URL = "https://www.borsaitaliana.it/borsa/derivati/mini-ftse-mib/lista.html";
        final String FUTURES_URL_DETAILS = "https://www.borsaitaliana.it/borsa/derivati/mini-ftse-mib/dati-completi.html?isin=#";
        final String ALLSHARE_URL_DETAILS = "https://www.borsaitaliana.it/borsa/azioni/dati-completi.html?&isin=#";
        final String ETF_DETAILS = "https://www.borsaitaliana.it/borsa/etf/dettaglio.html?isin=#";
        final String ETCETN_DETAILS = "https://www.borsaitaliana.it/borsa/etc-etn/dettaglio.html?isin=#";
        final String ALLSHARE_URL = "https://www.borsaitaliana.it/borsa/azioni/all-share/lista.html?&page=#";
        final String ETF_URL = "https://www.borsaitaliana.it/borsa/etf/search.html?comparto=ETF&idBenchmarkStyle=&idBenchmark=&indexBenchmark=&lang=it&sectorization=&page=#";
        final String ETCETN_URL = "https://www.borsaitaliana.it/borsa/etf/search.html?comparto=ETC&idBenchmarkStyle=&idBenchmark=&indexBenchmark=&lang=it&sectorization=&page=#";
        UDate ultimacontr = ultimoGiornoContrattazioni();
        switch (st) {
            case STOCK:
                url = ALLSHARE_URL;
                urldet = ALLSHARE_URL_DETAILS;
                type = "STOCK";
                currency = "EUR";
                market = "MLSE";
                break;
            case ETF:
                url = ETF_URL;
                urldet = ETF_DETAILS;
                type = "ETF";
                currency = "EUR";
                market = "MLSE";
                break;
            case ETCETN:
                url = ETCETN_URL;
                urldet = ETCETN_DETAILS;
                type = "ETCETN";
                currency = "EUR";
                market = "MLSE";
                break;
            case FUTURE:
                url = FUTURES_URL;
                urldet = FUTURES_URL_DETAILS;
                type = "FUTURE";
                currency = "EUR";
                market = "MLSE";
                break;
            default:
                throw new Exception(st + " not yet implemented");
        }
        ArrayList<String> urls = new ArrayList<>();
        while (true) {
            try {
                String s = url.replace("#", Integer.toString(cnt));
                LOG.debug(s);
                s = new String(http.HttpGetUrl(s, Optional.of(20), Optional.empty()));

                Document doc = Jsoup.parse(s);
                Elements links = doc.select("a");
                Elements span;
                if (null == st) {
                    throw new Exception("not yet implemented");
                } else {
                    switch (st) {
                        case STOCK:
                            span = doc.select("span[class=\"m-icon -pagination-dright\"]");
                            break;
                        case ETF:
                        case ETCETN:
                            span = doc.select("a[title=\"Successiva\"]");
                            break;
                        case FUTURE:
                            span = doc.select("a[class=\"u-hidden -xs\"]");
                            break;
                        default:
                            throw new Exception("not yet implemented");
                    }
                }
                links.forEach((x) -> {
                    if (x.attr("href").contains("/scheda/")) {
                        //if (st==secType.FUTURE && !x.attr("class").contains("u-hidden -xs")) return;
                        int idx = x.attr("href").indexOf("/scheda/");
                        String isin = x.attr("href").substring(idx + 8, idx + 8 + 12);
                        //LOG.debug(hashcode + "\t" + x.text().toUpperCase());
                        java.util.HashMap<String, String> map = new java.util.HashMap<>();
                        map.put("isin", isin);
                        if (st != secType.FUTURE) {
                            map.put("name", x.text().toUpperCase());
                        } else {
                            map.put("name", "MINIFTSEMIB");
                        }
                        map.put("type", type);
                        map.put("currency", currency);
                        map.put("market", market);

                        String sd = urldet.replace("#", isin);
                        if (urls.contains(sd)) {
                            return;
                        } else {
                            urls.add(sd);
                        }
                        try {
                            sd = new String(http.HttpGetUrl(sd, Optional.of(30), Optional.empty()));
                            Document docd = Jsoup.parse(sd);
                            //ArrayList<Element> tables=new ArrayList<>();
                            Elements tables = docd.select("table[class=\"m-table -clear-m\"]");
                            HashMap<String, String> m = new HashMap<>();
                            for (Element table1 : tables) {
                                Elements rows = table1.select("tr");
                                for (int i = 0; i < rows.size(); i++) {
                                    Element row = rows.get(i);
                                    Elements cols = row.select("td");
                                    if (cols.size() == 2) {
                                        m.put(cols.get(0).text(), cols.get(1).text());
                                        if (cols.get(0).text().equalsIgnoreCase("Codice Alfanumerico")) {
                                            map.put("code", cols.get(1).text());
                                        }
                                    }
                                }
                            }

                            //if (!(m.get("Fase di Mercato").equalsIgnoreCase("Chiusura") || m.get("Fase di Mercato").equalsIgnoreCase("Fine Servizio"))) {
                            //   LOG.warn("mercato non chiuso per " + map.get("isin"));
                            //  return;
                            //}
                            map.put("sector", m.toString());
                            map.keySet().forEach((xx) -> {
                                LOG.debug(xx + "\t" + map.get(xx));
                            });
                            LOG.debug("********************");
                            String hash = FetchData.computeHashcode(map.get("isin"), market);//Encoding.base64encode(getSHA1(String2Byte((map.get("isin") + market))));                            
                            if (st == secType.FUTURE) {
                                if (all.size() == 1) {
                                    return;
                                } else {
                                    all.put(hash, map);
                                }
                            } else if (!all.containsKey(hash)) {
                                all.put(hash, map);
                            }
                        } catch (Exception e) {
                            LOG.warn("error fetching " + urldet.replace("#", isin) + "\n" + e);
                        }
                    }
                });
                if (span.isEmpty() || st == secType.FUTURE) {
                    break;
                }
                cnt++;
            } catch (Exception e) {
                LOG.error(e, e);
                break;
            }
        }

        LOG.debug("#" + all.size());
        return all;
    }

    public static JSONArray fetchMLSEEOD(String symbol, Security.secType sec) throws Exception {
        String market = "";
        if (null == sec) {
            throw new Exception(sec + " non gestito");
        } else {
            switch (sec) {
                case STOCK:
                    market = "MTA";
                    break;
                case ETF:
                case ETCETN:
                    market = "ETF";
                    break;
                default:
                    throw new Exception(sec + " non gestito");
            }
        }
        String url = "https://charts.borsaitaliana.it/charts/services/ChartWService.asmx/GetPricesWithVolume";
        String jsonstriday = "{\"request\":{\"SampleTime\":\"1mm\",\"TimeFrame\":\"1d\",\"RequestedDataSetType\":\"ohlc\",\"ChartPriceType\":\"price\",\"Key\":\"" + symbol + "." + market + "\",\"OffSet\":0,\"FromDate\":null,\"ToDate\":null,\"UseDelay\":true,\"KeyType\":\"Topic\",\"KeyType2\":\"Topic\",\"Language\":\"it-IT\"}}";
        String jsonstr = "{\"request\":{\"SampleTime\":\"1d\",\"TimeFrame\":\"10y\",\"RequestedDataSetType\":\"ohlc\",\"ChartPriceType\":\"price\",\"Key\":\"" + symbol + "." + market + "\",\"OffSet\":0,\"FromDate\":null,\"ToDate\":null,\"UseDelay\":true,\"KeyType\":\"Topic\",\"KeyType2\":\"Topic\",\"Language\":\"it-IT\"}}";
        HttpFetch http = new HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        String res = http.sendjsonPostRequest(url, jsonstr);
        JSONObject o = new JSONObject(res);
        JSONArray arr = o.getJSONArray("d");
        //TreeMap<UDate,ArrayList<Double>> map= new TreeMap<>()  ;
        JSONArray totalarr = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {

            JSONObject sv = new JSONObject();
            UDate d = new UDate(arr.getJSONArray(i).getLong(0));
            d = UDate.getNewDate(d, 0, 0, 0);
            if (!arr.getJSONArray(i).isEmpty()) {
                sv.put("date", d.toYYYYMMDD());
                sv.put("close", arr.getJSONArray(i).getDouble(5));
                sv.put("open", arr.getJSONArray(i).isNull(2) ? arr.getJSONArray(i).getDouble(5) : arr.getJSONArray(i).getDouble(2));
                sv.put("high", arr.getJSONArray(i).isNull(3) ? arr.getJSONArray(i).getDouble(5) : arr.getJSONArray(i).getDouble(3));
                sv.put("low", arr.getJSONArray(i).isNull(4) ? arr.getJSONArray(i).getDouble(5) : arr.getJSONArray(i).getDouble(4));
                sv.put("volume", arr.getJSONArray(i).isNull(6) ? 0.0 : (double) arr.getJSONArray(i).getDouble(6));
                sv.put("oi", 0);
                totalarr.put(sv);

                //if (!arr.getJSONArray(i).isNull(6)) map.put(d, new ArrayList<>(Arrays.asList(arr.getJSONArray(i).getDouble(2),arr.getJSONArray(i).getDouble(3),arr.getJSONArray(i).getDouble(4),arr.getJSONArray(i).getDouble(5),(double)arr.getJSONArray(i).getInt(6))));
                //else map.put(d, new ArrayList<>(Arrays.asList(arr.getJSONArray(i).getDouble(2),arr.getJSONArray(i).getDouble(3),arr.getJSONArray(i).getDouble(4),arr.getJSONArray(i).getDouble(5),0.0)));
            }

        }
        LOG.debug("samples fetched for " + symbol + " = " + totalarr.length());
        return totalarr;
    }

    public static JSONArray fetchMLSEEODintraday(String symbol, Security.secType sec) throws Exception {
        String market = "";
        if (null == sec) {
            throw new Exception(sec + " non gestito");
        } else {
            switch (sec) {
                case STOCK:
                    market = "MTA";
                    break;
                case ETF:
                case ETCETN:
                    market = "ETF";
                    break;
                default:
                    throw new Exception(sec + " non gestito");
            }
        }
        String url = "https://charts.borsaitaliana.it/charts/services/ChartWService.asmx/GetPricesWithVolume";
        String jsonstr = "{\"request\":{\"SampleTime\":\"1mm\",\"TimeFrame\":\"1d\",\"RequestedDataSetType\":\"ohlc\",\"ChartPriceType\":\"price\",\"Key\":\"" + symbol + "." + market + "\",\"OffSet\":0,\"FromDate\":null,\"ToDate\":null,\"UseDelay\":true,\"KeyType\":\"Topic\",\"KeyType2\":\"Topic\",\"Language\":\"it-IT\"}}";
        HttpFetch http = new HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        String res = http.sendjsonPostRequest(url, jsonstr);
        JSONObject o = new JSONObject(res);
        JSONArray arr = o.getJSONArray("d");
        //TreeMap<UDate,ArrayList<Double>> map= new TreeMap<>()  ;
        JSONArray totalarr = new JSONArray();
        UDate today = new UDate();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject sv = new JSONObject();
            UDate d = new UDate(arr.getJSONArray(i).getLong(0) - 1000 * 60 * 120);
            today = UDate.getNewDate(d, 0, 0, 0);
            if (!arr.getJSONArray(i).isEmpty()) {
                try {
                    sv.put("date", d.toString());
                    sv.put("close", arr.getJSONArray(i).getDouble(5));
                    sv.put("open", arr.getJSONArray(i).isNull(2) ? arr.getJSONArray(i).getDouble(5) : arr.getJSONArray(i).getDouble(2));
                    sv.put("high", arr.getJSONArray(i).isNull(3) ? arr.getJSONArray(i).getDouble(5) : arr.getJSONArray(i).getDouble(3));
                    sv.put("low", arr.getJSONArray(i).isNull(4) ? arr.getJSONArray(i).getDouble(5) : arr.getJSONArray(i).getDouble(4));
                    sv.put("volume", arr.getJSONArray(i).isNull(6) ? 0.0 : (double) arr.getJSONArray(i).getDouble(6));
                    sv.put("oi", 0);
                    totalarr.put(sv);
                } catch (Exception e) {
                }//skip row
            }
        }
        LOG.debug("intraday samples fetched for " + symbol + " = " + totalarr.length());
        return totalarr;
    }

    /*public static JSONArray fetchMLSEEODsole24ore(String symbol,Database.Markets market)throws Exception{
        String post="";
        switch (market){
                case EURONEXT:{post=".PAR";}
                break;
                case MLSE:{post=".MI";}
                break;
                case NYSE:{post=".NY";}
                break;
                case NASDAQ:{post=".Q";}
                break;                
                default:
                    throw new Exception("cannot fetch isin form this market : "+market);
        }
        
        String url="https://vwd-proxy.ilsole24ore.com/FinanzaMercati/api/TimeSeries/GetTimeSeries/"+symbol+post+"?timeWindow=TenYears";                
        //.PAR per parigi euronext        
        HttpFetch http= new HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
        }
        String res=new String(http.HttpGetUrl(url, Optional.empty(), Optional.empty()));
        JSONObject o= new JSONObject(res);
        JSONArray arr= o.getJSONArray("series");                
        JSONArray totalarr= new JSONArray();
        for (int i=0;i<arr.length();i++){
            JSONObject sv= new JSONObject();                         
            sv.put ("open",arr.getJSONObject(i).getDouble("open") );
            sv.put ("high",arr.getJSONObject(i).getDouble("high"));
            sv.put ("low",arr.getJSONObject(i).getDouble("low"));
            sv.put ("close",arr.getJSONObject(i).getDouble("close"));
            sv.put ("volume",arr.getJSONObject(i).getDouble("volume"));
            String timestamp=arr.getJSONObject(i).getString("timestamp").substring(0, 10);
            sv.put ("date",UDate.parseYYYYmMMmDD(timestamp).toYYYYMMDD());            
            sv.put("oi", 0);                
            totalarr.put(sv);                             
        }
        LOG.debug("samples fetched for "+symbol+" = "+totalarr.length());
        return totalarr;
    }*/
//vwd-proxy.ilsole24ore.com/FinanzaMercati/api/TimeSeries/GetTimeSeries/4AIM.MI?timeWindow=TenYears	
}
