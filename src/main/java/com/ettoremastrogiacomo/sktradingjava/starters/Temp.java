/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import java.util.Arrays;
import org.apache.log4j.Logger;
import org.jfree.chart.plot.XYPlot;
import com.ettoremastrogiacomo.sktradingjava.Charts;
import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.HttpFetch;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.TreeSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author a241448
 */
public class Temp {

    static Logger LOG = Logger.getLogger(Temp.class);

    /*    public static String yq2(String symbol) throws Exception {
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
        }    
        String url1="https://finance.yahoo.com/quote/"+symbol+"/history?p="+symbol;
        String result=new String(http.HttpGetUrl(url1));
        List<String> cookie=http.getCookies();//.get(0).split(";")[0];
        //LOG.debug("cookie="+cookie);
        int k1=result.indexOf("CrumbStore");
        int k2=result.indexOf("\"",k1+22);
        String crumb=result.substring(k1+21,k2).replace("\"", "").replace("\\u00", "%");
        //LOG.info("crumb="+crumb);        

        String u2="https://query1.finance.yahoo.com/v7/finance/download/"+symbol+"?period1=0&period2="+System.currentTimeMillis()+"&interval=1d&events=history&crumb="+crumb;        
        //LOG.debug("url="+u2);
        result=new String(http.HttpGetUrlSession(u2, cookie));
        return result;
    }*/
    public static String getYahooQuotes(String symbol) throws Exception {
        //http://real-chart.finance.yahoo.com/table.csv?s=ENEL.MI&d=0&e=26&f=2017&g=d&a=6&b=9&c=2001&ignore=.csv
        URL url = new URL("https://finance.yahoo.com/quote/" + symbol + "/history?p=" + symbol);

        HttpFetch http = new HttpFetch();
        String res=new String(http.HttpGetUrl(url.toString(), Optional.empty(), Optional.empty()));
        int k0 = res.indexOf("consent-form single-page-form single-page-agree-form");

        if (k0 > 0) {
            java.util.HashMap<String, String> pmap = new java.util.HashMap<>();
            Document dy = Jsoup.parse(res);
            Elements els = dy.select("form[class='consent-form single-page-form single-page-agree-form'] input[type='hidden']");
            els.forEach((x) -> {
                pmap.put(x.attr("name"), x.attr("value"));
            });
            HttpURLConnection huc = http.sendPostRequest("https://guce.oath.com/consent", pmap);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    huc.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            res = response.toString();
            //cookieList = cookieManager.getCookieStore().getCookies();

        }
        int k1 = res.indexOf("CrumbStore");
        int k2 = res.indexOf("\"", k1 + 22);
        String crumb = res.substring(k1 + 21, k2).replace("\"", "").replace("\\u00", "%");
        LOG.info("crumb=" + crumb);
        String u2 = "https://query1.finance.yahoo.com/v7/finance/download/" + symbol + "?period1=0&period2=" + System.currentTimeMillis() + "&interval=1d&events=history&crumb=" + crumb;
        res = new String(http.HttpGetUrl(u2, Optional.empty(), Optional.of(http.getCookies())));
        LOG.debug("getting " + u2);
        LOG.debug(res);
        return res;
    }



    public static void fetchEuroNext() throws Exception {

        String u0 = "https://www.euronext.com/en/equities/directory";
        com.ettoremastrogiacomo.utils.HttpFetch httpf = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            httpf.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
        }
        String s = new String(httpf.HttpGetUrl(u0, Optional.empty(), Optional.empty()));

        int k1 = s.indexOf("\\/en\\/popup\\/data\\/download?");
        int k2 = s.indexOf("\"", k1);
        String u1 = s.substring(k1, k2 - 1);
        //LOG.debug(u1);
        u1 = u1.replace("\\u0026", "&");
        u1 = "https://www.euronext.com" + u1.replace("/", "");
        u1 = u1.replace("\\", "/");
        LOG.debug(u1);
        s = new String(httpf.HttpGetUrl(u1, Optional.empty(), Optional.empty()));
        Document doc = Jsoup.parse(s);
        java.util.HashMap<String, String> vmap = new java.util.HashMap<>();
        vmap.put("format", "1");
        vmap.put("layout", "2");
        vmap.put("decimal_separator", "1");
        vmap.put("date_format", "1");
        vmap.put("op", "Go");
        Elements links = doc.select("input[name=\"form_build_id\"]");
        links.forEach((x) -> {
            vmap.put("form_build_id", x.attr("value"));
        });
        links = doc.select("input[name=\"form_id\"]");
        links.forEach((x) -> {
            vmap.put("form_id", x.attr("value"));
        });
        HttpURLConnection post = httpf.sendPostRequest(u1, vmap);
        StringBuffer response;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(post.getInputStream()))) {
            String inputLine;
            response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append("\n").append(inputLine);
            }
        }
        String res = response.toString();
        String[] lines = res.split("\n");
        for (String line : lines) {
            String[] row = line.split("\t");
            if (row.length == 13) {
                LOG.debug(row[0] + "\t" + row[1] + "\t" + row[2] + "\t" + row[3] + "\t" + row[4] + "\t" + row[5]);
            }
        }
    }

    private List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        final InputStream in
                = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        try (
                //InputStream in = getResourceAsStream( path );
                BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }

    private InputStream getResourceAsStream(String resource) {
        final InputStream in
                = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }
    public static void testCookies() throws Exception {
        URL url = new URL("http://www.repubblica.it");
        URLConnection conn = url.openConnection();
        //conn.setRequestProperty("Cookie", "name1=value1; name2=value2");
        conn.connect();
        Map<String,List<String>> map=conn.getHeaderFields();
        map.keySet().forEach((s) -> {
            LOG.debug(s+"\t"+map.get(s));
        });
    
    }
    
 
    public static void main(String[] args) throws Exception {
  //      testCookies();
//LOG.debug(Database.getGoogleQuotes("BIT:ENEL"));
        /*java.util.HashMap<String,TreeSet<UDate>> map=new HashMap<String, TreeSet<UDate>>();
        map=Database.intradayDates();
        for (String h : map.keySet()){
            TreeSet<UDate> ts= map.get(h);
            ts.forEach((d)->{LOG.debug(h+"\t"+d);});
        
        }*/
        
        Fints.frequency[] f=Fints.frequency.values();
        for (Fints.frequency v: f ) LOG.debug(v);
        
        //com.ettoremastrogiacomo.utils.HttpFetch http= new HttpFetch();
        //String s= new String(http.HttpGetUrl("https://www.borsaitaliana.it/borsa/etf/contratti.html?isin=IE00B6YX5B26&lang=it&page=0", Optional.empty(), Optional.empty()));
        //LOG.debug(Database.getYahooQuotes("ENEL.MI"));
        //LOG.debug(Database.getGoogleQuotes("BIT:ENEL"));
        if (true) {
            return;
        }
        ArrayList<String> h = new ArrayList<>();
        ArrayList<String> i = new ArrayList<>();
        h.add("1hash");
        h.add("2hash");
        h.add("3hash");
        i.add("1isin");
        i.add("2isin");
        i.add("3isin");
        Database.getRecords(Optional.of(h), Optional.of(i), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
//fetchEuroNext();
        //Fints f=Database.getFintsQuotes(Optional.of("XMIB"),Optional.of("MLSE") , Optional.empty());
        //f.getSerieCopy(3).plot("xmib", "price");
        //LOG.debug(Database.getGoogleQuotes("BIT:ENEL"));
        // LOG.debug(com.ettoremastrogiacomo.utils.Encoding.base64encode(com.ettoremastrogiacomo.utils.Encoding.getSHA1("prova".getBytes())));;

    }
}
