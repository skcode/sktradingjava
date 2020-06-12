/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.utils.Encoding;
import static com.ettoremastrogiacomo.utils.Encoding.String2Byte;
import static com.ettoremastrogiacomo.utils.Encoding.getSHA1;
import com.ettoremastrogiacomo.utils.HttpFetch;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jsoup.nodes.Element;
import com.ettoremastrogiacomo.sktradingjava.Security.secType;
import static com.ettoremastrogiacomo.sktradingjava.data.EURONEXT_DataFetch.fetchEURONEXTEOD;
import static com.ettoremastrogiacomo.sktradingjava.data.EURONEXT_DataFetch.fetchEuroNext;
import static com.ettoremastrogiacomo.sktradingjava.data.MLSE_DataFetch.fetchMLSEEOD;
import static com.ettoremastrogiacomo.sktradingjava.data.MLSE_DataFetch.fetchMLSEList;
import static com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch.fetchListDE;
import static com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch.fetchXETRAEOD;
import com.ettoremastrogiacomo.utils.UDate;
import java.sql.PreparedStatement;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.TreeMap;


class Tintradaydata implements Runnable {

    final secType st;
    // final com.ettoremastrogiacomo.utils.HttpFetch http;
    final String url;
    final String hashcode, isin;
    static final String ALLSHARE_URL_DETAILS = "https://www.borsaitaliana.it/borsa/azioni/contratti.html?isin=#&lang=it&page=";
    static final String ETF_DETAILS = "https://www.borsaitaliana.it/borsa/etf/contratti.html?isin=#&lang=it&page=";
    static final String ETCETN_DETAILS = "https://www.borsaitaliana.it/borsa/etc-etn/contratti.html?isin=#&lang=it&page=";
    static final String FUTURES_URL_DETAILS = "https://www.borsaitaliana.it/borsa/derivati/mini-ftse-mib/contratti.html?isin=#&lang=it&page=";
    final int ROW_SIZE;
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(Tintradaydata.class);

    public String data, fase;
    private final String sql = "insert or replace into intradayquotes (hashcode,date,quotes) values(?,?,?);";

    Tintradaydata(String isin, String hashcode, secType st) throws Exception {
        this.st = st;
        switch (st) {
            case STOCK:
                url = ALLSHARE_URL_DETAILS;
                ROW_SIZE = 5;
                break;
            case ETF:
                url = ETF_DETAILS;
                ROW_SIZE = 5;
                break;
            case ETCETN:
                url = ETCETN_DETAILS;
                ROW_SIZE = 5;
                break;
            case FUTURE:
                url = FUTURES_URL_DETAILS;
                ROW_SIZE = 4;
                break;
            default:
                throw new Exception(st + " not yet implemented");
        }
        this.hashcode = hashcode;
        this.isin = isin;//String sd = url.replace("#", isin);
    }

    @Override
    public void run() {
        int cnt = 0;
        java.util.ArrayList<java.util.HashMap<String, String>> list = new java.util.ArrayList<>();

        //https://www.borsaitaliana.it/borsa/etf/contratti.html?isin=LU1681046931&lang=it&page=0
        //https://www.borsaitaliana.it/borsa/etf/contratti.html?isin=FR0011807015&lang=it&page=0
        try {
            HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
            if (Init.use_http_proxy.equals("true")) {
                http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port),Init.http_proxy_type, Init.http_proxy_user, Init.http_proxy_password);
            }
            while (true) {

                String s = url.replace("#", isin) + Integer.toString(cnt);
                String fase = "", data = "";
                // String url = "http://www.something.com";

                //s="https://www.borsaitaliana.it/borsa/etf/contratti.html?isin=LU1681046931&lang=it&page=0";
                String ss = new String(http.HttpGetUrl(s, Optional.of(30), Optional.empty()));
                //if (ss.contains("Page Not Found")) {LOG.debug(isin+" not found\n"+url.replace("#", isin) + Integer.toString(cnt)); throw new Exception("cannot grab "+isin);}
                Document doc = Jsoup.parse(ss);//"m-table -responsive -clear-m"            
                Elements b = doc.select("div[class='w-999__bcol | l-box | l-screen -sm-9 -md-9'] strong");
                if (!b.isEmpty()) {
                    fase = b.get(0).text();
                    if (b.get(1).text().length() >= 8) {
                        data = b.get(1).text().substring(0, 8);
                    } else {
                        throw new Exception("cannot grab date, maybe no contracts for  ISIN " + isin + "\thash " + hashcode);
                    }
                    if (st != secType.FUTURE && !fase.equalsIgnoreCase("CHIUSURA")) {
                        throw new Exception("market not closed");
                    }

                    if (st == secType.FUTURE && !fase.equalsIgnoreCase("Fine Servizio")) {
                        throw new Exception("future market not closed");
                    }

                    this.fase = fase;
                    this.data = data;
                    Elements t = doc.select("table[class='m-table -responsive -clear-m'] tr td");
                    Elements f = doc.select("span[class='m-icon -pagination-right']");

                    if (t.isEmpty() || (t.size() % ROW_SIZE) != 0) {
                        throw new Exception("wrong string : " + t);
                    }
                    for (int i = 0; i < t.size(); i = i + ROW_SIZE) {
                        java.util.HashMap<String, String> m = new java.util.HashMap<>();
                        for (int j = i; j < (i + ROW_SIZE); j++) {
                            if (j == i) {
                                m.put("ORA", data + " " + t.get(j).text().trim());
                            } else if (j == (i + 1)) {
                                m.put("PREZZO", t.get(j).text().trim());
                            } else if (j == (i + 2)) {
                                m.put("VARIAZIONE_PERCENTUALE", t.get(j).text().trim());
                            } else if (j == (i + 3)) {
                                m.put("VOLUME", t.get(j).text().trim());
                            } else if (j == (i + 4)) {
                                m.put("TIPO", t.get(j).text().trim());
                            }
                        }

                        list.add(m);

                    }
                    //<span class="m-icon -pagination-right"></span>
                    //t.forEach((x)->{System.out.println(x.text());});
                    cnt++;
                    if (cnt > 1000) {
                        LOG.warn("reached 1000 pages, stop for isin " + isin);
                        break;
                    }
                    if (f.isEmpty()) {
                        break;
                    }
                }
            }
            LOG.debug("intraday data fetched for isin " + isin + "\thash " + hashcode + "\t" + data);
            synchronized (this) {
                try ( Connection conn = DriverManager.getConnection(Init.db_url)) {
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                        if (!data.equals("") && !list.isEmpty()) {
                            try {
                                stmt.setString(1, hashcode);
                                stmt.setString(2, data);
                                stmt.setString(3, list.toString());
                                stmt.executeUpdate();
                            } catch (SQLException e) {
                                LOG.warn(e);
                            }
                        }
                    }                    
                } 
            }
        } catch (Exception e) {
            this.data = "";
            //ExceptionUtils.getRootCause(e).getClass().getSimpleName();
            Throwable s = e;
            while (s.getCause() != null) {
                s = s.getCause();
            }

            LOG.warn("error loading isin " + isin + "\thash " + hashcode + "\t" + s.getClass() + "\t" + s.getMessage());
        }
    }
}

/**
 *
 * @author a241448
 */
public final class FetchData {

    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FetchData.class);
    static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

    //final com.ettoremastrogiacomo.utils.HttpFetch http;
    static public String computeHashcode(String isin, String market) throws Exception{
        return Encoding.base64encode(getSHA1(String2Byte((isin + market))));
    }

    public static void fetchIntraday() throws Exception {
        int pcount = Runtime.getRuntime().availableProcessors();
        java.util.HashMap<String, Tintradaydata> dataarr = new java.util.HashMap<>();
        java.util.HashMap<String, java.util.ArrayList<java.util.HashMap<String, String>>> map = new java.util.HashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(pcount);
        List<HashMap<String, String>> records = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("MLSE")), Optional.empty(), Optional.empty());
        for (HashMap<String, String> s : records) {
            // LOG.debug("starting thread for "+s.get("hashcode")+"\t"+s.get("name"));
            Tintradaydata t1 = new Tintradaydata(s.get("isin"), s.get("hashcode"), secType.getEnum(s.get("type")) );
            //dataarr.put(s.get("hashcode"), t1);
            executor.execute(t1);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }
    }

    static java.util.ArrayList<java.util.HashMap<String, String>> dividendiBIT(String hashcode, String type) throws Exception {
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
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
                s = new String(http.HttpGetUrl(s,Optional.empty(),Optional.empty()));                
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
    
    public static java.util.HashMap<String, java.util.HashMap<String, String>> fetchNYSE() throws Exception {
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
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
                long hc=Math.abs(9999999999L-Math.abs(name.hashCode()));//NON HO ISIN CORRETTO
                map.put("isin", "US" + Long.toString(hc));
                map.put("name", name);
                map.put("code", s[0]);
                map.put("type", "STOCK");
                map.put("currency", "USD");
                map.put("market", "NYSE");
                map.put("sector", "NA");
                all.put(map.get("isin"), map);
            } else
            if (s[4].contains("Y")) {//ETF
                String name = s[1];//s[1].indexOf(" -") > 0 ? s[1].substring(0, s[1].indexOf(" -")) : "";
                if (name.equals("")) {
                    continue;
                }
                LOG.info(s[0] + "\t" + name);
                java.util.HashMap<String, String> map = new java.util.HashMap<>();
                long hc=Math.abs(9999999999L-Math.abs(name.hashCode()));//NON HO ISIN CORRETTO
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
    
    public static void fetchNYSESharesDetails() throws Exception {
//        String sql = "insert or replace into securities (hashcode,name,code,type,market,currency,sector,yahooquotes,bitquotes,googlequotes) values"
        //              + "(?,?,?,?,?,?,?,(select yahooquotes from securities where hashcode = ?),(select bitquotes from securities where hashcode = ?),(select googlequotes from securities where hashcode = ?));";

        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
       LOG.info("fetching NYSE");
        try {
            all.putAll(fetchNYSE());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }        
 
        String sql = "insert or replace into shares values (?,?,?,?,?,?,?,?);";
        /*String sqlnew = "CREATE TABLE IF NOT EXISTS shares (\n"
                + "	hashcode text not null,\n"
                + "	hashcode text NOT NULL,\n"                
                + "	name text not null,\n"
                + "	code text NOT NULL,\n"
                + "	type text not null,\n"
                + "	market text not null,\n"
                + "	currency text not null,\n"
                + "	sector text not null,\n"
                + "	primary key (hashcode) ,\n"
                + "     unique (hashcode,market));";//,\n" */

        Connection conn = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.PreparedStatement ustmt = null;
        //java.sql.Statement qstmt = null;        
        try {
            conn = DriverManager.getConnection(Init.db_url);
            stmt = conn.prepareStatement(sql);
            conn.setAutoCommit(false);
            //  qstmt = conn.createStatement();
            for (String hashcode : all.keySet()) {
                try {
                    stmt.setString(1, hashcode);
                    stmt.setString(2, all.get(hashcode).get("isin"));
                    stmt.setString(3, all.get(hashcode).get("name"));
                    stmt.setString(4, all.get(hashcode).get("code"));
                    stmt.setString(5, all.get(hashcode).get("type"));
                    stmt.setString(6, all.get(hashcode).get("market"));
                    stmt.setString(7, all.get(hashcode).get("currency"));
                    stmt.setString(8, all.get(hashcode).get("sector"));
                    stmt.addBatch();

                    LOG.debug(hashcode + "\t" + all.get(hashcode).get("name") + "\t loaded");

                } catch (Exception e) {
                    LOG.warn("error loading " + hashcode + "\t" + all.get(hashcode).get("name"), e);
                }
            }
            stmt.executeBatch();
            conn.commit();
            //stmt.execute();
            //conn.commit();
        } catch (SQLException e) {
            LOG.error(e, e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (ustmt != null) {
                    ustmt.close();
                }
            } catch (SQLException e) {
            }

            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }

    }

    
        public static void loadEODdata() throws Exception {        
        java.util.HashMap<String, java.util.HashMap<String, String>> allm=new HashMap<>();
        java.util.HashMap<String, java.util.HashMap<String, String>> m=new HashMap<>();
        m.putAll(fetchMLSEList(secType.ETCETN));  
        m.putAll(fetchMLSEList(secType.STOCK));
        m.putAll(fetchMLSEList(secType.ETF));
        allm.putAll(m);
        HashMap<String, TreeMap<UDate, ArrayList<Double>>> datamap = new HashMap<>();        
        
        for (String x : m.keySet()) {
            try {
            String isin = m.get(x).get("isin");
            String code = m.get(x).get("code");
            String type = m.get(x).get("type");            
            TreeMap<UDate, ArrayList<Double>> data = new TreeMap<>();
            if (type.equalsIgnoreCase("STOCK")) data=fetchMLSEEOD(code, secType.STOCK);
            else if (type.equalsIgnoreCase("ETF")) data=fetchMLSEEOD(code, secType.ETF);
            else if (type.equalsIgnoreCase("ETCETN")) data=fetchMLSEEOD(code, secType.ETCETN);
            else throw new Exception(type +" not allowed");
            datamap.put(x, data);
            LOG.debug("fetched data from BORSAITALIANA " + m.get(x).get("name"));
            } catch (Exception e) {
                LOG.warn("cannot fetch BORSAITALIANA data for "+m.get(x).get("name")+"\t"+m.get(x).get("isin")+"\t"+m.get(x).get("code")+"\t"+e);
            }
        }
         m=fetchEuroNext();
         allm.putAll(m);
         for (String x : m.keySet()) {
             try {
            String isin = m.get(x).get("isin");            
            TreeMap<UDate, ArrayList<Double>> data=fetchEURONEXTEOD(isin, m.get(x).get("market"));            
            datamap.put(x, data);
            LOG.debug("fetched data from EURONEXT " + m.get(x).get("name"));
             }catch (Exception e) {
                LOG.warn("cannot fetch EURONEXT data for "+m.get(x).get("name")+"\t"+m.get(x).get("isin")+"\t"+m.get(x).get("code")+"\t"+e);
            }
        }
        m = fetchListDE();
        allm.putAll(m);
        for (String x : m.keySet()) {
            try {
            String isin = m.get(x).get("isin");
            TreeMap<UDate, ArrayList<Double>> data = fetchXETRAEOD(isin, true);
            datamap.put(x, data);
            LOG.debug("fetched data from XETRA " + m.get(x).get("name"));
             }catch (Exception e) {
                LOG.warn("cannot fetch XETRA data for "+m.get(x).get("name")+"\t"+m.get(x).get("isin")+"\t"+m.get(x).get("code")+"\t"+e);
            }
        }
        
         
        String sql1="insert or replace into eoddatav2(hashcode,date,open,high,low,close,volume,oi,provider) values(?,?,?,?,?,?,?,?,?)";        
        String sql2="insert or replace into shares(hashcode,isin,name,code,type,market,currency,sector) values(?,?,?,?,?,?,?,?)";
        
        try (Connection conn = DriverManager.getConnection(Init.db_url);                
            PreparedStatement ps = conn.prepareStatement(sql1);PreparedStatement ps2 = conn.prepareStatement(sql2);                
                ) {
            conn.setAutoCommit(false);
            for (String s:allm.keySet()){                                
                try {
                ps2.setString(1, s);
                ps2.setString(2, allm.get(s).get("isin"));
                ps2.setString(3, allm.get(s).get("name"));
                ps2.setString(4, allm.get(s).get("code"));
                ps2.setString(5, allm.get(s).get("type"));
                ps2.setString(6, allm.get(s).get("market"));
                ps2.setString(7, allm.get(s).get("currency"));
                ps2.setString(8, allm.get(s).get("sector"));
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
                    ps.setString(9,"BORSAITALIANA");                    
                    ps.addBatch();                    
                }
                LOG.debug(Arrays.toString(ps.executeBatch()) );            
                LOG.debug(Arrays.toString(ps2.executeBatch()));                        
                conn.commit();                
                } catch (Exception e) {
                    LOG.warn("cannot load "+ allm.get(s).get("name"));
                }
            }

           } catch (SQLException e) {
               LOG.warn(e);
           }        

        //fetchDatiCompletiMLSE("NL0010877643", secType.STOCK);
        //};
    }

    public static void main(String[] args) throws Exception {

        //fetchMLSEList(secType.FUTURE);
       // fetchDatiCompletiMLSE("NL0010877643", secType.STOCK);
        //};
    }

}
