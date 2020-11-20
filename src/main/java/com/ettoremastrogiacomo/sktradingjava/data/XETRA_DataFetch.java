/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Init;
import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.computeHashcode;
import com.ettoremastrogiacomo.utils.HttpFetch;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Optional;
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
    public static java.util.HashMap<String, java.util.HashMap<String, String>> fetchListDE() throws Exception {
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
    
    static public JSONArray fetchXETRAEOD(String isin,boolean xetra)throws Exception{
        //NB utilizzare mic=XFRA per dati eod altri mercati europei altrimenti XETR
        String market=xetra?"XETR":"XFRA";
        HttpFetch http = new HttpFetch();
        //https://api.boerse-frankfurt.de/data/price_history?limit=10000&offset=0&mic=XETR&minDate=2010-01-01&maxDate=2099-01-01&isin=NL0000226223        
        //https://api.boerse-frankfurt.de/v1/tradingview/history?symbol=XETR%3ADE0006047004&resolution=1D&from=1578667823&to=1605455103983
        String url= "https://api.boerse-frankfurt.de/data/price_history?limit=10000&offset=0&mic="+market+"&minDate=2010-01-01&maxDate=2099-01-01&isin="+isin;
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port),Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        String res= new String(http.HttpGetUrl(url,Optional.empty(),Optional.empty()));
        JSONObject    obj = new JSONObject(res);
        JSONArray arr= obj.getJSONArray("data");
        JSONArray totalarr= new JSONArray();
        //TreeMap<UDate,ArrayList<Double>> values= new TreeMap<>();        
        for(int i=0;i<arr.length();i++){                           
            JSONObject e = arr.getJSONObject(i);
            JSONObject sv= new JSONObject();                        
            try {
                String []dateel=e.getString("date").split("-");
                UDate datev=UDate.genDate(Integer.parseInt(dateel[0]) , Integer.parseInt(dateel[1])-1, Integer.parseInt(dateel[2]), 0, 0, 0);                                              
                double close=e.getDouble("close");
                double volume=e.getLong("turnoverPieces");
                double open=e.isNull("open") ? close:e.getDouble("open");
                double high=e.isNull("high") ? close:e.getDouble("high");
                double low=e.isNull("low") ? close:e.getDouble("low");
                sv.put("date", datev.toYYYYMMDD());
                sv.put("open", open);
                sv.put("high", high);
                sv.put("low", low);
                sv.put("close", close);
                sv.put("volume", volume);
                sv.put("oi", 0);                
                totalarr.put(sv);
        //        values.put(datev, new ArrayList<>(Arrays.asList(open,high,low,close,volume)) );
            } catch (Exception ex) {
                LOG.warn(e.toString()+"\t"+ex);
            }                        
        }             
        LOG.debug("samples fetched for "+isin+" = "+totalarr.length() );
        return totalarr;
    }

    static public JSONArray fetchXETRAEOD2(String isin,boolean xetra)throws Exception{
        //NB utilizzare mic=XFRA per dati eod altri mercati europei altrimenti XETR
        String market=xetra?"XETR":"XFRA";
        HttpFetch http = new HttpFetch();
        Long LT=System.currentTimeMillis();
        
        //https://api.boerse-frankfurt.de/data/price_history?limit=10000&offset=0&mic=XETR&minDate=2010-01-01&maxDate=2099-01-01&isin=NL0000226223        
        String url="https://api.boerse-frankfurt.de/v1/tradingview/history?symbol="+market+"%3A"+isin+"&resolution=1D&from=1000000000&to="+LT.toString().substring(0, 10);
        //String url= "https://api.boerse-frankfurt.de/data/price_history?limit=10000&offset=0&mic="+market+"&minDate=2010-01-01&maxDate=2099-01-01&isin="+isin;
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port),Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        String res= new String(http.HttpGetUrl(url,Optional.empty(),Optional.empty()));
        JSONObject    obj = new JSONObject(res);
        if (!obj.get("s").equals("ok")) throw new Exception("data for isin "+isin +" not fetched");        
        JSONArray t_arr=obj.getJSONArray("t");
        JSONArray o_arr=obj.getJSONArray("o");
        JSONArray h_arr=obj.getJSONArray("h");
        JSONArray l_arr=obj.getJSONArray("l");
        JSONArray c_arr=obj.getJSONArray("c");
        JSONArray v_arr=obj.getJSONArray("v");
        int len=t_arr.length();
        JSONArray totalarr= new JSONArray();
        for (int i=0;i<len;i++){
            JSONObject sv= new JSONObject();  
            UDate datev=new UDate(t_arr.getLong(i)*1000 );
            double close=c_arr.getDouble(i);
            double volume=Math.floor(v_arr.getDouble(i));
            double open=o_arr.getDouble(i);
            double high=h_arr.getDouble(i);
            double low=l_arr.getDouble(i);
            open=open==0?close:open;
            high=high==0?close:high;
            low=low==0?close:low;           
            sv.put("date", datev.toYYYYMMDD());
            sv.put("open", open);
            sv.put("high", high);
            sv.put("low", low);
            sv.put("close", close);
            sv.put("volume", volume);
            sv.put("oi", 0);                
            totalarr.put(sv);                    
        }
        
        /*JSONArray arr= obj.getJSONArray("data");
        
        //TreeMap<UDate,ArrayList<Double>> values= new TreeMap<>();        
        for(int i=0;i<arr.length();i++){                           
            JSONObject e = arr.getJSONObject(i);
            JSONObject sv= new JSONObject();                        
            try {
                String []dateel=e.getString("date").split("-");
                UDate datev=UDate.genDate(Integer.parseInt(dateel[0]) , Integer.parseInt(dateel[1])-1, Integer.parseInt(dateel[2]), 0, 0, 0);                                              
                double close=e.getDouble("close");
                double volume=e.getLong("turnoverPieces");
                double open=e.isNull("open") ? close:e.getDouble("open");
                double high=e.isNull("high") ? close:e.getDouble("high");
                double low=e.isNull("low") ? close:e.getDouble("low");
                sv.put("date", datev.toYYYYMMDD());
                sv.put("open", open);
                sv.put("high", high);
                sv.put("low", low);
                sv.put("close", close);
                sv.put("volume", volume);
                sv.put("oi", 0);                
                totalarr.put(sv);
        //        values.put(datev, new ArrayList<>(Arrays.asList(open,high,low,close,volume)) );
            } catch (Exception ex) {
                LOG.warn(e.toString()+"\t"+ex);
            }                        
        }             */
        LOG.debug("samples fetched for "+isin+" = "+totalarr.length() );
        return totalarr;
    }
public static void main(String[] args)throws Exception{
    String isin="DE000A2JNWZ9";
      JSONArray j=fetchXETRAEOD2(isin,true);
      Fints f= Database.getFintsQuotes(Database.getHashcodefromIsin(isin, "XETRA"));
      LOG.debug(f.getMaxDaysDateGap());
      LOG.debug(f.toString());
      LOG.debug(f.getMaxDaysDateGap());

}
    //
}
