

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.temp;

import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.Security.secType;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.sktradingjava.data.Database.Markets;
import com.ettoremastrogiacomo.sktradingjava.data.FetchData;
import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.loadintoDB;
import static com.ettoremastrogiacomo.sktradingjava.data.MLSE_DataFetch.fetchMLSEList;
import com.ettoremastrogiacomo.utils.HttpFetch;
import com.ettoremastrogiacomo.utils.Misc;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.utils.UDate;

import java.util.ArrayList;
import java.util.Calendar;
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
 * @author a241448
 */
public class Temp {

    static Logger LOG = Logger.getLogger(Temp.class);

    public static JSONArray tree2json(TreeMap<UDate,ArrayList<Double>> m){
        
       JSONArray array= new JSONArray();
       for (UDate d: m.keySet()) 
       {
            JSONObject jsonObject = new JSONObject();
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("open", m.get(d).get(0));
            jsonObject2.put("high", m.get(d).get(1));
            jsonObject2.put("low", m.get(d).get(2));
            jsonObject2.put("close", m.get(d).get(3));
            jsonObject2.put("volume", m.get(d).get(4));
            if (m.get(d).size()<6) jsonObject2.put("oi", 0);
            else jsonObject2.put("oi", m.get(d).get(5));            
            jsonObject.put("date", d.toYYYYMMDD());            
            jsonObject.put("data", jsonObject2);
            array.put(jsonObject);
       }                
        return array;
    }
    public static String fetchISINsole24ore(String symbol,Database.Markets market) throws Exception {
        String post="";
        String u1="";
        switch (market){
                case EURONEXT:{post=".PAR";u1="https://mercati.ilsole24ore.com/azioni/borse-europee/parigi/dettaglio/";}
                break;
                case MLSE:{post=".MI";u1="https://mercati.ilsole24ore.com/azioni/borsa-italiana/dettaglio-completo/";}
                break;
                case NYSE:{post=".NY";u1="https://mercati.ilsole24ore.com/azioni/borse-extra-europee/usa-nyse/dettaglio/";}
                break;
                case NASDAQ:{post=".Q";u1="https://mercati.ilsole24ore.com/azioni/borse-extra-europee/usa-nasdaq/dettaglio/";}
                break;                
                default:
                    throw new Exception("cannot fetch isin form this market : "+market);
        }
        HttpFetch http = new HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        u1=u1+symbol+post;
        String res1 = new String(http.HttpGetUrl(u1, Optional.empty(), Optional.empty()));
        if (!res1.contains("boxDettaglioColumn")) throw new Exception("cannot find "+symbol);
        LOG.debug("getting " + symbol + "\tURL=" + u1);
        Document doc= Jsoup.parse(res1);        
        Element table=doc.select("table[class='boxDettaglioColumn']").get(3);
        Element row=table.select("tr").first();
        String visin=row.select("td").get(1).text();
        row=table.select("tr").get(1);
        String vmkt=row.select("td").get(1).text();
        LOG.debug(visin+"\t"+vmkt);
        return visin;        
    }    
    
    public static JSONArray fetchEODsole24ore(String symbol,Database.Markets market)throws Exception{
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
    }
    
    
    public static void main(String[] args) throws Exception {
       // com.ettoremastrogiacomo.sktradingjava.data.Database.getFintsQuotes(Optional.of("ENGI"),Optional.of("EURONEXT-XPAR") , Optional.empty());
             //LOG.debug(FetchData.fetchYahooQuotes("MSFT"));   
             
             Database.clearSharesTable(Markets.NYSE);
             Database.clearSharesTable(Markets.NASDAQ);
             Database.clearEODTable();
             Database.clearEODTable();
             
             
             HashMap<String,HashMap<String,String>> map=FetchData.fetchNYSEList();
             HttpFetch.disableSSLcheck();
             for (String isin: map.keySet()){
                 try {                 
                     String symbol=map.get(isin).get("code");
                     String market=map.get(isin).get("market");
                     //https://mercati.ilsole24ore.com/azioni/borse-extra-europee/usa-nyse/dettaglio/ACN.NY      nyse
                     //https://mercati.ilsole24ore.com/azioni/borse-extra-europee/usa-nasdaq/dettaglio/ACER.Q     nasdaq
                     
                     String res=fetchISINsole24ore(symbol,Markets.valueOf(market));
                     JSONArray data= fetchEODsole24ore(symbol,Markets.valueOf(market)) ;                     
                     
                /*for (int i = 1; i < lines.length; i++) {//skip first header line
                    try {
                        JSONObject sv = new JSONObject();
                        String[] row = lines[i].split(",");
                        String[] date = row[0].split("-");
                        Calendar c = new java.util.GregorianCalendar(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]));
                        double fact = Double.parseDouble(row[5]) / Double.parseDouble(row[4]);

                        sv.put("date", (new UDate(c.getTimeInMillis())).toYYYYMMDD());
                        sv.put("close", Double.parseDouble(row[4]) * fact);
                        sv.put("open", Double.parseDouble(row[1]) * fact);
                        sv.put("high", Double.parseDouble(row[2]) * fact);
                        sv.put("low", Double.parseDouble(row[3]) * fact);
                        sv.put("volume", Double.parseDouble(row[6]));
                        sv.put("oi", 0);
                        data.put(sv);
                    } catch (Exception e) {//LOG.warn("skip row "+i+"\t"+e);
                        LOG.warn(e);
                    }*/
                }
                  catch (Exception e){e.printStackTrace();}
             }
             
    }
}
