/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Init;

import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.computeHashcode;

import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author sk
 */
public class EURONEXT_DataFetch {

    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(EURONEXT_DataFetch.class);

    public static java.util.HashMap<String, java.util.HashMap<String, String>> fetchEuroNext() throws Exception {
        /**
         *
         * Euronext Access Brussels Euronext Access Lisbon
         */
        java.util.HashMap<String, String> marketMap = new java.util.HashMap<String, String>() {
            {
                put("Traded not listed Brussels", "TNLB");
                put("Euronext Paris, London", "XPAR");
                put("Euronext Paris, Brussels", "XPAR");
                put("Euronext Paris, Amsterdam, Brussels", "XPAR");
                put("Euronext Paris, Amsterdam", "XPAR");
                put("Euronext Paris", "XPAR");
                put("Euronext Lisbon", "XLIS");
                put("Euronext Growth Paris", "ALXP");
                put("Euronext Growth Lisbon", "ALXL");
                put("Euronext Growth Brussels, Paris", "ALXB");
                put("Euronext Growth Brussels", "ALXB");
                put("Euronext Brussels, Paris", "XBRU");
                put("Euronext Brussels", "XBRU");
                put("Euronext Brussels, Amsterdam", "XBRU");
                put("Euronext Expert Market", "VPXB");
                put("Euronext Amsterdam, Paris", "XAMS");
                put("Euronext Amsterdam, London", "XAMS");
                put("Euronext Amsterdam, Brussels, Paris", "XAMS");
                put("Euronext Amsterdam, Brussels", "XAMS");
                put("Euronext Amsterdam", "XAMS");
                put("Euronext Growth Dublin", "AYP");
                put("Euronext Dublin", "A5G");
                put("Euronext Growth Paris, Brussels", "ALXP");
                put("Euronext Access Paris", "XMLI");
                put("Euronext Access Lisbon", "ENXL");
                put("Euronext Access Brussels", "MLXB");
                put("Euronext Growth Oslo","MERK");
            }
        };
        String u0 = "https://live.euronext.com/en/products/equities/list";
        //https://live.euronext.com/en/product/equities/FR0013341781-XPAR/2crsi/2crsi/quotes#historical-price
        //String u0 = "https://www.euronext.com/en/equities/directory";
        //String det = "https://www.euronext.com/en/products/equities/BE0003849669-MLXB/market-information";
        //String det = "https://www.euronext.com/en/products/equities/#/market-information";
        //https://live.euronext.com/en/product/equities/BE0003849669-MLXB#historical-price
        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
        com.ettoremastrogiacomo.utils.HttpFetch httpf = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            httpf.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        String s = new String(httpf.HttpGetUrl(u0, Optional.empty(), Optional.empty()));
        List<HttpCookie> ck = httpf.getCookies();
        //\/pd\/data\/stocks\/download
        //int k1 = s.indexOf("\\/en\\/popup\\/data\\/download?");
        int k1 = s.indexOf("\\/pd\\/data\\/stocks\\/download?");
        int k2 = s.indexOf("\"", k1);
        String u1 = s.substring(k1, k2 - 1);
        //LOG.debug(u1);
        u1 = u1.replace("\\u0026", "&");

        u1 = "https://live.euronext.com" + u1.replace("/", "");
        u1 = u1.replace("\\", "/");//"+"&display_datapoints=dp_stocks&display_filters=df_stocks";

        LOG.debug(u1);
        java.util.HashMap<String, String> vmap = new java.util.HashMap<>();
        vmap.put("args[fe_date_format]", "d/m/y");
        vmap.put("args[fe_decimal_separator]", ".");
        vmap.put("decimal_separator", "1");
        vmap.put("args[fe_layout]", "ver");
        vmap.put("args[fe_type]", "excel");
        vmap.put("args[initialLetter]", "");
        vmap.put("iDisplayLength", "20");
        vmap.put("iDisplayStart", "0");
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
            if (row.length != 13) {
                continue;
            }
            if (row[0].equalsIgnoreCase("Name")) {
                LOG.debug(line);
                continue;//first row
            }
            if (row[0].isEmpty()) {
                continue;
            }
            String mkt = row[3].replace("\"", "");
            if (!marketMap.keySet().contains(mkt)) {
                LOG.warn("market not found : " + row[3] + "\t" + line);
                continue;
            }
            LOG.debug(line);
            String isin = row[1].replace("\"", "");
            String market = "EURONEXT-" + marketMap.get(mkt);//+"\t"+row[3].toUpperCase();
            String sector = "NA";
            if (sector.isEmpty() || market.isEmpty()) {
                continue;
            }
            java.util.HashMap<String, String> map = new java.util.HashMap<>();
            map.put("type", "STOCK");
            map.put("market", market.toUpperCase());
            map.put("currency", row[4].replace("\"", "").toUpperCase());
            map.put("sector", sector);
            map.put("isin", isin);
            map.put("code", row[2].replace("\"", ""));
            if (row[2].replace("\"", "").equalsIgnoreCase("-")) {
                LOG.warn(row[2] + "\t" + line);
                continue;
            }
            map.put("name", row[0].replace("\"", ""));
            map.keySet().forEach((x) -> {
                LOG.debug(x + "\t" + map.get(x));
            });
            String hash = computeHashcode(map.get("isin"), map.get("market"));// Encoding.base64encode(getSHA1(String2Byte((map.get("isin") + map.get("market")))));
            if (!all.containsKey(hash)) {
                all.put(hash, map);
            }

        }
        return all;
    }

    static public JSONArray fetchEURONEXTEOD(String isin, String market) throws Exception {
        String val = market.contains("EURONEXT") ? isin + market.substring(market.indexOf("-")) : isin + "-" + market;
        //https://live.euronext.com/intraday_chart/getChartData/FR0010242511-XPAR/max
        String url = "https://live.euronext.com/intraday_chart/getChartData/" + val + "/max";
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        //TreeMap<UDate,ArrayList<Double>> values= new TreeMap<>();
        String res = new String(http.HttpGetUrl(url, Optional.empty(), Optional.empty()));
        JSONArray arr = new JSONArray(res);
        JSONArray totalarr = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject sv = new JSONObject();
                JSONObject e = arr.getJSONObject(i);
                String[] dateel = e.getString("time").substring(0, 10).split("-");
                UDate datev = UDate.genDate(Integer.parseInt(dateel[0]), Integer.parseInt(dateel[1]) - 1, Integer.parseInt(dateel[2]), 0, 0, 0);
                double close = e.getDouble("price");
                double volume = e.getLong("volume");
                sv.put("date", datev.toYYYYMMDD());
                sv.put("close", close);
                sv.put("open", close);
                sv.put("high", close);
                sv.put("low", close);
                sv.put("volume", volume);
                sv.put("oi", 0);
                totalarr.put(sv);
            } catch (NumberFormatException | JSONException e) {
            } //skip row
            //values.put(datev, new ArrayList<>(Arrays.asList(close,close,close,close,volume)) );
        }
        LOG.debug("samples fetched for " + isin + " = " + totalarr.length());
        return totalarr;
    }

}
