/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.utils.Encoding;
import static com.ettoremastrogiacomo.utils.Encoding.String2Byte;
import static com.ettoremastrogiacomo.utils.Encoding.getSHA1;
import com.ettoremastrogiacomo.utils.HttpFetch;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import com.ettoremastrogiacomo.sktradingjava.Security.secType;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;
import static com.ettoremastrogiacomo.sktradingjava.data.EURONEXT_DataFetch.fetchEURONEXTEOD;
import static com.ettoremastrogiacomo.sktradingjava.data.EURONEXT_DataFetch.fetchEuroNext;
import static com.ettoremastrogiacomo.sktradingjava.data.MLSE_DataFetch.fetchMLSEEOD;
import static com.ettoremastrogiacomo.sktradingjava.data.MLSE_DataFetch.fetchMLSEList;
import static com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch.fetchXETRAEOD2;
import static com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch.fetchXETRAEOD;
import static com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch.fetchListDE;

/**
 *
 * @author a241448
 */
public final class FetchData {

    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FetchData.class);
    static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    //final com.ettoremastrogiacomo.utils.HttpFetch http;
    static public String computeHashcode(String isin, String market) throws Exception {
        return Encoding.base64encode(getSHA1(String2Byte((isin + market))));
    }

    static java.util.ArrayList<java.util.HashMap<String, String>> dividendiBIT(String hashcode, String type) throws Exception {
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        int cnt = 1;
        //http://www.borsaitaliana.it/borsa/quotazioni/azioni/elenco-completo-dividendi.html?hashcode=IT0003128367&lang=it&page=1
        //Azioni 	Div. Cda 	Div. Ass. 	Divisa 	Stacco 	Pagamento 	Assemblea 	Avviso
        String s_url = "";
        if (type.equalsIgnoreCase("STOCK")) {
            s_url = "http://www.borsaitaliana.it/borsa/quotazioni/azioni/elenco-completo-dividendi.html?hashcode=" + hashcode + "&lang=it&page=";
        } else if (type.equalsIgnoreCase("ETF")) {
            s_url = "http://www.borsaitaliana.it/borsa/etf/dividendi.html?hashcode=" + hashcode + "&lang=it&page=";
        } else {
            throw new Exception("unknown type: " + type);
        }
        java.util.ArrayList<java.util.HashMap<String, String>> list = new java.util.ArrayList<>();
        int row_size = 8, row_size_etf = 5;
        try {
            while (true) {
                String s = s_url + Integer.toString(cnt);
                //String fase="",data="";
                s = new String(http.HttpGetUrl(s, Optional.empty(), Optional.empty()));
                Document doc = Jsoup.parse(s);//"m-table -responsive -clear-m"            
                Elements b = doc.select("table[class='table_dati']  tr td");
                Elements c = doc.select("a[title='Successiva']");
                if (type.equalsIgnoreCase("STOCK")) {
                    if (b.isEmpty() || (b.size() % row_size) != 0) {
                        throw new Exception("wrong string : " + b);
                    }
                }
                if (type.equalsIgnoreCase("ETF")) {
                    if (b.isEmpty() || (b.size() % row_size_etf) != 0) {
                        throw new Exception("wrong string : " + b);
                    }
                }
                if (type.equalsIgnoreCase("STOCK")) {
                    for (int i = 0; i < b.size(); i = i + row_size) {
                        java.util.HashMap<String, String> m = new java.util.HashMap<>();
                        for (int j = i; j < (i + row_size); j++) {
                            if (j == i) {
                                m.put("AZIONI", b.get(j).text().trim());
                            } else if (j == (i + 1)) {
                                m.put("DIV_CDA", b.get(j).text().trim());
                            } else if (j == (i + 2)) {
                                m.put("DIV_ASS", b.get(j).text().trim());
                            } else if (j == (i + 3)) {
                                m.put("DIVISA", b.get(j).text().trim());
                            } else if (j == (i + 4)) {
                                m.put("STACCO", b.get(j).text().trim());
                            } else if (j == (i + 5)) {
                                m.put("PAGAMENTO", b.get(j).text().trim());
                            } else if (j == (i + 6)) {
                                m.put("ASSEMBLEA", b.get(j).text().trim());
                            } else if (j == (i + 7)) {
                                m.put("AVVISO", b.get(j).text().trim());
                            }
                        }
                        list.add(m);
                    }

                } else {
                    for (int i = 0; i < b.size(); i = i + row_size_etf) {
                        java.util.HashMap<String, String> m = new java.util.HashMap<>();
                        for (int j = i; j < (i + row_size_etf); j++) {
                            if (j == i) {
                                m.put("STACCO", b.get(j).text().trim());
                            } else if (j == (i + 1)) {
                                m.put("PROVENTO", b.get(j).text().trim());
                            } else if (j == (i + 2)) {
                                m.put("VALUTA", b.get(j).text().trim());
                            } else if (j == (i + 3)) {
                                m.put("PAGAMENTO", b.get(j).text().trim());
                            } else if (j == (i + 4)) {
                                m.put("TIPO_PAGAMENTO", b.get(j).text().trim());
                            }
                        }
                        list.add(m);
                    }
                }
                //b.forEach((x)->{System.out.println(x.text());});
                //Azioni Div. Cda Div. Ass. Divisa Stacco Pagamento Assemblea Avviso 
                if (c.isEmpty()) {
                    break;
                }
                cnt++;

            }
        } catch (Exception e) {
            LOG.warn("no dividends for " + hashcode);
        }

        return list;

    }

    public static java.util.HashMap<String, java.util.HashMap<String, String>> fetchNYSEList() throws Exception {
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
        //Symbol|Security Name|Market Category|Test Issue|Financial Status|Round Lot Size
        //String details = "https://it.finance.yahoo.com/quote/MSFT/profile?p=#";
        String url_nasdaq = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/nasdaqlisted.txt";
        //ACT Symbol|Security Name|Exchange|CQS Symbol|ETF|Round Lot Size|Test Issue|NASDAQ Symbol
        String url_others = "ftp://ftp.nasdaqtrader.com/SymbolDirectory/otherlisted.txt";
        String nasdaq = new String(http.FTPGetUrl(url_nasdaq));
        String others = new String(http.FTPGetUrl(url_others)) + "\n" + nasdaq;
        String[] lines = others.split("\n");
        for (String x : lines) {
            String[] s = x.split("\\|");
            if (s.length < 5) {
                continue;
            }
            if (s[1].contains("Common Stock")) {
                String name = s[1];//s[1].indexOf(" -") > 0 ? s[1].substring(0, s[1].indexOf(" -")) : "";
                if (name.equals("")) {
                    continue;
                }
                LOG.info(s[0] + "\t" + name);
                java.util.HashMap<String, String> map = new java.util.HashMap<>();
                long hc = Math.abs(9999999999L - Math.abs(name.hashCode()));//NON HO ISIN CORRETTO
                map.put("isin", "US" + Long.toString(hc));
                map.put("name", name);
                map.put("code", s[0]);
                map.put("type", "STOCK");
                map.put("currency", "USD");
                map.put("market", "NYSE");
                map.put("sector", "NA");
                all.put(map.get("isin"), map);
            } else if (s[4].contains("Y")) {//ETF
                String name = s[1];//s[1].indexOf(" -") > 0 ? s[1].substring(0, s[1].indexOf(" -")) : "";
                if (name.equals("")) {
                    continue;
                }
                LOG.info(s[0] + "\t" + name);
                java.util.HashMap<String, String> map = new java.util.HashMap<>();
                long hc = Math.abs(9999999999L - Math.abs(name.hashCode()));//NON HO ISIN CORRETTO
                map.put("isin", "US" + Long.toString(hc));
                map.put("name", name);
                map.put("code", s[0]);
                map.put("type", "ETF");
                map.put("currency", "USD");
                map.put("market", "NYSE");
                map.put("sector", "NA");
                all.put(map.get("isin"), map);
            }

        }
        return all;
    }

    public static void loadEODdata() throws Exception {

        String sql1 = "insert or replace into " + Init.db_sharestable + "(hashcode,isin,name,code,type,market,currency,sector) values(?,?,?,?,?,?,?,?)";
        String sql2 = "insert or replace into " + Init.db_eoddatatable + "(hashcode,data,provider) values(?,?,?)";
        String sql3 = "insert or replace into " + Init.db_intradaytable + "(hashcode,date,quotes) values(?,?,?)";
        Connection conn = DriverManager.getConnection(Init.db_url);
        java.sql.PreparedStatement stmt1 = conn.prepareStatement(sql1);
        java.sql.PreparedStatement stmt2 = conn.prepareStatement(sql2);
        java.sql.PreparedStatement stmt3 = conn.prepareStatement(sql3);
        conn.setAutoCommit(false);

        java.util.HashMap<String, java.util.HashMap<String, String>> m = new HashMap<>();
////////////////BORSA FRANCESE
        LOG.debug("*** fetching EURONEXT shares ***");
        m = fetchEuroNext();

        for (String x : m.keySet()) {
            try {
                String isin = m.get(x).get("isin");
                LOG.debug(">>now fetching " + m.get(x));
                JSONArray data = fetchEURONEXTEOD(isin, m.get(x).get("market"));
                LOG.debug("data len " + data.length());
                stmt1.setString(1, x);
                stmt1.setString(2, m.get(x).get("isin"));
                stmt1.setString(3, m.get(x).get("name"));
                stmt1.setString(4, m.get(x).get("code"));
                stmt1.setString(5, m.get(x).get("type"));
                stmt1.setString(6, m.get(x).get("market"));
                stmt1.setString(7, m.get(x).get("currency"));
                stmt1.setString(8, m.get(x).get("sector"));
                LOG.debug("rows updated in " + Init.db_sharestable + " table " + stmt1.executeUpdate());
                stmt2.setString(1, x);
                stmt2.setString(2, data.toString());
                stmt2.setString(3, "EURONEXT");
                LOG.debug("rows updated in " + Init.db_eoddatatable + " table " + stmt2.executeUpdate());
                //stmt1.executeBatch();
                //stmt2.executeBatch();
                conn.commit();
                LOG.debug("fetched data from EURONEXT " + m.get(x).get("name"));
            } catch (Exception e) {
                LOG.warn("cannot fetch EURONEXT data for " + m.get(x).get("name") + "\t" + m.get(x).get("isin") + "\t" + m.get(x).get("code") + "\t" + e);
            }
        }

////////////////BORSA ITALIANA
        LOG.debug("*** fetching MLSE shares ***");
        m.putAll(fetchMLSEList(secType.ETCETN));
        m.putAll(fetchMLSEList(secType.STOCK));
        m.putAll(fetchMLSEList(secType.ETF));

        for (String x : m.keySet()) {
            try {
                String isin = m.get(x).get("isin");
                String code = m.get(x).get("code");
                String type = m.get(x).get("type");
                JSONArray data = new JSONArray();
                JSONArray data2 = MLSE_DataFetch.fetchMLSEEODsole24ore(code);
                JSONArray data3 = new JSONArray();
                if (type.equalsIgnoreCase("STOCK")) {
                    data = fetchMLSEEOD(code, secType.STOCK);
                    data3 = MLSE_DataFetch.fetchMLSEEODintraday(code, secType.STOCK);
                } else if (type.equalsIgnoreCase("ETF")) {
                    data = fetchMLSEEOD(code, secType.ETF);
                    data3 = MLSE_DataFetch.fetchMLSEEODintraday(code, secType.ETF);
                } else if (type.equalsIgnoreCase("ETCETN")) {
                    data = fetchMLSEEOD(code, secType.ETCETN);
                    data3 = MLSE_DataFetch.fetchMLSEEODintraday(code, secType.ETCETN);
                } else {
                    throw new Exception(type + " not allowed");
                }
                stmt1.setString(1, x);
                stmt1.setString(2, m.get(x).get("isin"));
                stmt1.setString(3, m.get(x).get("name"));
                stmt1.setString(4, m.get(x).get("code"));
                stmt1.setString(5, m.get(x).get("type"));
                stmt1.setString(6, m.get(x).get("market"));
                stmt1.setString(7, m.get(x).get("currency"));
                stmt1.setString(8, m.get(x).get("sector"));
                stmt1.addBatch();
                stmt2.setString(1, x);
                stmt2.setString(2, data.toString());
                stmt2.setString(3, "BORSAITALIANA");
                stmt2.addBatch();
                stmt2.setString(1, x);
                stmt2.setString(2, data2.toString());
                stmt2.setString(3, "SOLE24ORE");
                stmt2.addBatch();
                if (!data3.isEmpty()) {
                    stmt3.setString(1, x);
                    stmt3.setString(2, data3.getJSONObject(0).getString("date").substring(0, 10).trim().replace("-", ""));
                    stmt3.setString(3, data3.toString());
                    stmt3.addBatch();
                    stmt3.executeBatch();
                }
                stmt1.executeBatch();
                stmt2.executeBatch();

                conn.commit();
                LOG.debug("fetched data from BORSAITALIANA " + m.get(x).get("name"));
            } catch (Exception e) {
                LOG.warn("cannot fetch BORSAITALIANA data for " + m.get(x).get("name") + "\t" + m.get(x).get("isin") + "\t" + m.get(x).get("code") + "\t" + e);
            }
        }
////////////////BORSA TEDESCA
        LOG.debug("*** fetching XETRA shares ***");
        m = fetchListDE();

        for (String x : m.keySet()) {
            try {
                String isin = m.get(x).get("isin");
                JSONArray data = fetchXETRAEOD(isin, true);
                stmt1.setString(1, x);
                stmt1.setString(2, m.get(x).get("isin"));
                stmt1.setString(3, m.get(x).get("name"));
                stmt1.setString(4, m.get(x).get("code"));
                stmt1.setString(5, m.get(x).get("type"));
                stmt1.setString(6, m.get(x).get("market"));
                stmt1.setString(7, m.get(x).get("currency"));
                stmt1.setString(8, m.get(x).get("sector"));
                stmt1.addBatch();
                stmt2.setString(1, x);
                stmt2.setString(2, data.toString());
                stmt2.setString(3, "XETRA");
                stmt2.addBatch();
                stmt1.executeBatch();
                stmt2.executeBatch();
                conn.commit();
                LOG.debug("fetched data from XETRA " + m.get(x).get("name"));
            } catch (Exception e) {
                LOG.warn("cannot fetch XETRA data for " + m.get(x).get("name") + "\t" + m.get(x).get("isin") + "\t" + m.get(x).get("code") + "\t" + e);
            }
        }
        LOG.debug("*** fetching NYSE shares ***");
        m = fetchNYSEList();

        for (String x : m.keySet()) {
            try {
                String code = m.get(x).get("code");
                String s = fetchYahooQuotes(code);
                String[] lines = s.split("\n");
                //TreeMap<UDate, ArrayList<Double>> data = new TreeMap<>();
                JSONArray data = new JSONArray();
                for (int i = 1; i < lines.length; i++) {//skip first header line
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
                    }
                }
                stmt1.setString(1, x);
                stmt1.setString(2, m.get(x).get("isin"));
                stmt1.setString(3, m.get(x).get("name"));
                stmt1.setString(4, m.get(x).get("code"));
                stmt1.setString(5, m.get(x).get("type"));
                stmt1.setString(6, m.get(x).get("market"));
                stmt1.setString(7, m.get(x).get("currency"));
                stmt1.setString(8, m.get(x).get("sector"));
                stmt1.addBatch();
                stmt2.setString(1, x);
                stmt2.setString(2, data.toString());
                stmt2.setString(3, "YAHOO");
                stmt2.addBatch();
                stmt1.executeBatch();
                stmt2.executeBatch();
                conn.commit();
                LOG.debug("fetched data from NYSE " + m.get(x).get("name"));
            } catch (Exception e) {
                LOG.warn("cannot fetch NYSE data for " + m.get(x).get("name") + "\t" + m.get(x).get("isin") + "\t" + m.get(x).get("code") + "\t" + e);
            }

        }

        try {
            if (stmt1 != null) {
                stmt1.close();
            }
        } catch (SQLException e) {
        }
        try {
            if (stmt2 != null) {
                stmt2.close();
            }
        } catch (SQLException e) {
        }
        try {
            if (stmt3 != null) {
                stmt3.close();
            }
        } catch (SQLException e) {
        }

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
        }

        //fetchDatiCompletiMLSE("NL0010877643", secType.STOCK);
        //};
    }

    /*public static String fetchYahooQuotes(String symbol) throws Exception {
        URL url = new URL("https://finance.yahoo.com/quote/" + symbol + "/history?p=" + symbol);
        HttpFetch http = new HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        String res = new String(http.HttpGetUrl(url.toString(), Optional.empty(), Optional.empty()));
        int k0 = res.indexOf("action=\"/consent\"");
        if (k0 > 0) {
            HashMap<String, String> pmap = new HashMap<>();
            Document dy = Jsoup.parse(res);
            Elements els = dy.select("form[class='consent-form'] input[type='hidden']");
            els.forEach((x) -> {
                pmap.put(x.attr("name"), x.attr("value"));
            });
            HttpURLConnection huc = http.sendPostRequest("https://guce.oath.com/consent", pmap);
            BufferedReader in = new BufferedReader(new InputStreamReader(huc.getInputStream()));
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
        // LOG.info("crumb=" + crumb);
        //https://query1.finance.yahoo.com/v7/finance/download/HFRN.MI?period1=0&period2=1578265200&interval=1d&events=history&crumb=hCd0SUv4Zf2
        String u2 = "https://query1.finance.yahoo.com/v7/finance/download/" + symbol + "?period1=0&period2=" + System.currentTimeMillis() + "&interval=1d&events=history&crumb=" + crumb;
        res = new String(http.HttpGetUrl(u2, Optional.empty(), Optional.of(http.getCookies())));
        Database.LOG.debug("getting " + symbol + "\tURL=" + u2);
        return res;
    }*/
    public static String fetchYahooQuotes(String symbol) throws Exception {
        //URL url = new URL("https://finance.yahoo.com/quote/" + symbol + "/history?p=" + symbol);
        HttpFetch http = new HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
        }
        //https://query1.finance.yahoo.com/v7/finance/download/HFRN.MI?period1=0&period2=1578265200&interval=1d&events=history&crumb=hCd0SUv4Zf2
        String u2 = "https://query1.finance.yahoo.com/v7/finance/download/" + symbol + "?period1=0&period2=" + System.currentTimeMillis() + "&interval=1d&events=history";
        String res = new String(http.HttpGetUrl(u2, Optional.empty(), Optional.empty()));
        Database.LOG.debug("getting " + symbol + "\tURL=" + u2);
        return res;
    }

    static void loadintoDB(String x, java.util.HashMap<String, java.util.HashMap<String, String>> m, Database.Providers provider, Optional<Boolean> intraday) throws Exception {
        String sql1 = "insert or replace into " + Init.db_sharestable + "(hashcode,isin,name,code,type,market,currency,sector) values(?,?,?,?,?,?,?,?)";
        String sql2 = "insert or replace into " + Init.db_eoddatatable + "(hashcode,data,provider) values(?,?,?)";
        String sql3 = "insert or replace into " + Init.db_intradaytable + "(hashcode,date,quotes) values(?,?,?)";
        boolean iday = intraday.orElse(false);
        try (Connection conn = DriverManager.getConnection(Init.db_url)) {
            java.sql.PreparedStatement stmt1 = conn.prepareStatement(sql1);
            java.sql.PreparedStatement stmt2 = conn.prepareStatement(sql2);
            java.sql.PreparedStatement stmt3 = conn.prepareStatement(sql3);
            java.sql.Statement stmt = conn.createStatement();
            if (stmt.executeQuery("select * from " + Init.db_providerstable + " where name='" + provider + "'").next() == false) {
                throw new Exception("provider " + provider + " do not exists");
            }
            String isin = m.get(x).get("isin");
            String code = m.get(x).get("code");
            String type = m.get(x).get("type");
            String name = m.get(x).get("name");
            String market = m.get(x).get("market");
            LOG.debug(">>now fetching " + m.get(x));
            JSONArray data = new JSONArray(), dataiday = new JSONArray();
            String idaydate = "";
            //BORSAITALIANA(1), EURONEXT(2), XETRA(3), NYSE(4), INVESTING(5), YAHOO(6), GOOGLE(7);
            switch (provider) {
                case EURONEXT:
                    data = fetchEURONEXTEOD(isin, market);
                    break;
                case BORSAITALIANA:
                    data = MLSE_DataFetch.fetchMLSEEOD(code, Security.secType.valueOf(type));
                    if (iday) {
                        dataiday = MLSE_DataFetch.fetchMLSEEODintraday(code, Security.secType.valueOf(type));
                        if (!dataiday.isEmpty()) {
                            idaydate = dataiday.getJSONObject(0).getString("date").substring(0, 10).trim().replace("-", "");
                        }
                    }
                    break;
                case SOLE24ORE:
                    data = MLSE_DataFetch.fetchMLSEEODsole24ore(code);
                    break;
                case XETRA:
                    data = fetchXETRAEOD2(isin, false);
                    break;
                case YAHOO:
                    String s = fetchYahooQuotes(code); 
                    String[] lines = s.split("\n");
                    data = new JSONArray();
                    for (int i = 1; i < lines.length; i++) {//skip first header line
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
                        }
                    }                    
                    break;                    
                default:
                    throw new Exception("provider " + provider + " not implemented");
            }
            LOG.debug("data len " + data.length());
            if (data.length() == 0) {
                //stmt.executeUpdate("delete from "+Init.db_eoddatatable +" where hashcode='"+x+"' and provider='"+provider+"'");                
                LOG.warn("no data for " + isin + "."+code+ "."+name+" in " + provider);
            } else {
                stmt1.setString(1, x);
                stmt1.setString(2, m.get(x).get("isin"));
                stmt1.setString(3, m.get(x).get("name"));
                stmt1.setString(4, m.get(x).get("code"));
                stmt1.setString(5, m.get(x).get("type"));
                stmt1.setString(6, m.get(x).get("market"));
                stmt1.setString(7, m.get(x).get("currency"));
                stmt1.setString(8, m.get(x).get("sector"));
                LOG.debug("rows updated in " + Init.db_sharestable + " table " + stmt1.executeUpdate());
                stmt2.setString(1, x);
                stmt2.setString(2, data.toString());
                stmt2.setString(3, provider.toString());
                LOG.debug("rows updated in " + Init.db_eoddatatable + " table " + stmt2.executeUpdate());
                if (iday && !dataiday.isEmpty()) {
                    stmt3.setString(1, x);
                    stmt3.setString(2, idaydate);
                    stmt3.setString(3, dataiday.toString());
                    LOG.debug("rows updated in " + Init.db_intradaytable + " table " + stmt3.executeUpdate());
                }
            }
        }

    }

    static public void loadEODdatanew() throws Exception {
        //fetchEURONEXTEOD("FR0010208488", "EURONEXT-XPAR");

        //fetchMLSEList(Security.secType.ETCETN);        
        //fetchMLSEList(Security.secType.ETF);    
        try {
            LOG.debug("*** fetching XETRA shares ***");
            java.util.HashMap<String, java.util.HashMap<String, String>> mapXETRA = fetchListDE();
            mapXETRA.keySet().forEach((x) -> {
                try {
                    loadintoDB(x, mapXETRA, Database.Providers.XETRA , Optional.of(false));
                } catch (Exception e) {
                    LOG.warn(e);
                }
            });
        } catch (Exception e) {
            LOG.error("ERROR FETCHING XETRA");
        }
        
        try {
            LOG.debug("*** fetching BORSAITALIANA shares ***");
            java.util.HashMap<String, java.util.HashMap<String, String>> mapMLSEBIT = fetchMLSEList(Security.secType.STOCK);
            mapMLSEBIT.putAll(fetchMLSEList(Security.secType.ETCETN));
            mapMLSEBIT.putAll(fetchMLSEList(Security.secType.ETF));
            //map=fetchMLSEList(Security.secType.STOCK); 

            mapMLSEBIT.keySet().forEach((x) -> {
                try {
                    loadintoDB(x, mapMLSEBIT, Database.Providers.BORSAITALIANA, Optional.of(true));
                    loadintoDB(x, mapMLSEBIT, Database.Providers.SOLE24ORE, Optional.of(false));
                } catch (Exception e) {
                    LOG.warn(e);
                }
            });

        } catch (Exception e) {
            LOG.error("ERROR FETCHING BORSAITALIANA");
        }

        try {
            LOG.debug("*** fetching EURONEXT shares ***");
            java.util.HashMap<String, java.util.HashMap<String, String>> mapEURONEXT = fetchEuroNext();
            mapEURONEXT.keySet().forEach((x) -> {
                try {
                    loadintoDB(x, mapEURONEXT, Database.Providers.EURONEXT, Optional.of(false));
                } catch (Exception e) {
                    LOG.warn(e);
                }
            });

        } catch (Exception e) {
            LOG.error("ERROR FETCHING EURONEXT");
        }

        try {
            LOG.debug("*** fetching NYSE shares ***");
            java.util.HashMap<String, java.util.HashMap<String, String>> mapNYSE = fetchNYSEList();
            mapNYSE.keySet().forEach((x) -> {
                try {
                    loadintoDB(x, mapNYSE, Database.Providers.YAHOO, Optional.of(false));
                } catch (Exception e) {
                    LOG.warn(e);
                }
            });
        } catch (Exception e) {
            LOG.error("ERROR FETCHING NYSE");
        }

    }

}
