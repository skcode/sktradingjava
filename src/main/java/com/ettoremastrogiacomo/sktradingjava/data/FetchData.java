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
import com.ettoremastrogiacomo.utils.UDate;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;


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
                http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
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

    /*static public java.util.AbstractMap.SimpleEntry<UDate,HashMap<String,String>> fetchDatiCompletiMLSE(String isin, com.ettoremastrogiacomo.sktradingjava.Security.secType type) throws Exception{
        //https://www.borsaitaliana.it/borsa/azioni/dati-completi.html?isin=NL0010877643&lang=it        
        
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
        }        
        String urletf="https://www.borsaitaliana.it/borsa/etf/dettaglio.html?isin=#&lang=it";
        String urlstock="https://www.borsaitaliana.it/borsa/azioni/dati-completi.html?isin=#&lang=it";
        String urletcetn="https://www.borsaitaliana.it/borsa/etc-etn/dettaglio.html?isin=#&lang=it";
        //
        String url="";
        if (type==com.ettoremastrogiacomo.sktradingjava.Security.secType.STOCK) url = urlstock.replace("#", isin);
        else if (type==com.ettoremastrogiacomo.sktradingjava.Security.secType.ETF) url = urletf.replace("#", isin);
        else if (type==com.ettoremastrogiacomo.sktradingjava.Security.secType.ETCETN) url = urletcetn.replace("#", isin);
        else throw new RuntimeException("type not implemented :"+type.toString());
                LOG.debug(url);
                Document doc = Jsoup.parse(new String(http.HttpGetUrl(url, Optional.of(20), Optional.empty())));
                Element table1= doc.select("table[class=\"m-table -clear-m\"]").get(0);
                Element table2= doc.select("table[class=\"m-table -clear-m\"]").get(1);
                Elements rows = table1.select("tr");
                HashMap<String,String> m= new HashMap<>();                
                for (int i = 0; i < rows.size(); i++) { //first row is the col names so skip it.
                    Element row = rows.get(i);
                    Elements cols = row.select("td");
                    if (cols.size()==2){
                        for (int j=0;j<cols.size();j++){
                            LOG.debug("ROW "+i+"\t"+cols.get(j).text());                        
                        }
                    m.put(cols.get(0).text(), cols.get(1).text());
                    }
                }         
                rows = table2.select("tr");
                for (int i = 0; i < rows.size(); i++) { //first row is the col names so skip it.
                    Element row = rows.get(i);
                    Elements cols = row.select("td");
                    if (cols.size()==2){
                        for (int j=0;j<cols.size();j++){
                            LOG.debug("ROW "+i+"\t"+cols.get(j).text());
                        }
                        m.put(cols.get(0).text(), cols.get(1).text());                    
                    }
                }         
                String pdr=m.get("Prezzo di riferimento");
                int idx=pdr.indexOf("-");
                pdr=pdr.substring(idx+2);                
                int y=Integer.parseInt(pdr.substring(6, 8));
                int M=Integer.parseInt(pdr.substring(3, 5));
                int day=Integer.parseInt(pdr.substring(0, 2));
                int h=Integer.parseInt(pdr.substring(9, 11));
                int min=Integer.parseInt(pdr.substring(12, 14));
                int sec=Integer.parseInt(pdr.substring(15));                
                UDate d=UDate.genDate(y+2000 , M-1, day, h, min, sec);
                LOG.debug(d);
                return new java.util.AbstractMap.SimpleEntry<>(d,m);        
    }*/
    
    static java.util.HashMap<String, java.util.HashMap<String, String>> fetchMLSEList(secType st) throws Exception {
        int cnt = 1;
        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
        String url, urldet, type, currency, market;
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
        }
        final String FUTURES_URL = "https://www.borsaitaliana.it/borsa/derivati/mini-ftse-mib/lista.html";
        final String FUTURES_URL_DETAILS = "https://www.borsaitaliana.it/borsa/derivati/mini-ftse-mib/dati-completi.html?isin=#";
        final String ALLSHARE_URL_DETAILS = "https://www.borsaitaliana.it/borsa/azioni/dati-completi.html?&isin=#";
        final String ETF_DETAILS = "https://www.borsaitaliana.it/borsa/etf/dettaglio.html?isin=#";
        final String ETCETN_DETAILS = "https://www.borsaitaliana.it/borsa/etc-etn/dettaglio.html?isin=#";
        final String ALLSHARE_URL = "https://www.borsaitaliana.it/borsa/azioni/all-share/lista.html?&page=#";
        final String ETF_URL = "https://www.borsaitaliana.it/borsa/etf/search.html?comparto=ETF&idBenchmarkStyle=&idBenchmark=&indexBenchmark=&lang=it&page=#";
        final String ETCETN_URL = "https://www.borsaitaliana.it/borsa/etf/search.html?comparto=ETC&idBenchmarkStyle=&idBenchmark=&indexBenchmark=&lang=it&page=#";

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
                        //if (st==secType.FUTURE && !x.attr("href").contains("/mini-ftse-mib/")) return;
                        int idx = x.attr("href").indexOf("/scheda/");
                        String isin = x.attr("href").substring(idx + 8, idx + 8 + 12);
                        //LOG.debug(hashcode + "\t" + x.text().toUpperCase());
                        java.util.HashMap<String, String> map = new java.util.HashMap<>();
                        map.put("isin", isin);
                        if (st != secType.FUTURE) {
                            map.put("name", x.text().toUpperCase());
                        } else {
                            map.put("name", "MINIFTSEMIB-" + x.text().toUpperCase());
                        }
                        map.put("type", type);
                        map.put("currency", currency);
                        map.put("market", market);

                        String sd = urldet.replace("#", isin);
                        try {
                            sd = new String(http.HttpGetUrl(sd, Optional.of(30), Optional.empty()));
                            Document docd = Jsoup.parse(sd);
                            Elements elements = docd.select("span[class='t-text -size-xs'] > strong");
                            Elements elements2 = docd.select("span[class*='t-text -right ']");
                            int min = elements.size() < elements2.size() ? elements.size() : elements2.size();
                            LOG.debug("**********" + isin + "**********");
                            LOG.debug(isin + "\t" + x.text().toUpperCase() + "\t" + type + "\t" + currency + "\t" + market);
                            String sector = "";
                            for (int i = 0; i < min; i++) {
                                //map.put(elements.get(i).text(), elements2.get(i).text());
                                LOG.debug(elements.get(i).text() + "\t" + elements2.get(i).text());
                                if (elements.get(i).text().contains("Codice Alfanumerico")) {
                                    map.put("code", elements2.get(i).text());
                                }
                                if (st == secType.STOCK && elements.get(i).text().contains("Super Sector")) {
                                    sector = elements2.get(i).text();
                                }
                                //map.put("sector", elements2.get(i).text());
                                if (elements.get(i).text().contains("Capitalizzazione")) {
                                    map.put("capitalization", elements2.get(i).text().replace(".", ""));
                                }
                                if ((st == secType.ETF || st == secType.ETCETN) && (elements.get(i).text().contains("Benchmark")
                                        || elements.get(i).text().contains("Area Benchmark")
                                        || elements.get(i).text().contains("Emittente")
                                        || elements.get(i).text().contains("Segmento")
                                        || elements.get(i).text().contains("Classe")
                                        || elements.get(i).text().contains("Commissioni totali annue")
                                        || elements.get(i).text().contains("Tipo strumento")
                                        || elements.get(i).text().contains("Sottostante")
                                        || elements.get(i).text().contains("Dividendi")
                                        || elements.get(i).text().contains("Tipo sottostante")
                                        || elements.get(i).text().contains("Commissioni entrata uscita Performance"))) {
                                    sector = sector.length() == 0 ? sector = elements.get(i).text() + "=" + elements2.get(i).text() : sector + ";" + elements.get(i).text() + "=" + elements2.get(i).text();
                                }
                                if (st == secType.FUTURE) {
                                    sector = "NA";
                                    map.put("code", map.get("name"));
                                }

                            }
                            map.put("sector", sector);
                            map.keySet().forEach((y) -> {
                                LOG.debug(y + "\t" + map.get(y));
                            });
                            LOG.debug("********************");
                            String hash = computeHashcode(map.get("isin"), market);//Encoding.base64encode(getSHA1(String2Byte((map.get("isin") + market))));
                            if (!all.containsKey(hash)) {
                                all.put(hash, map);
                            }

                        } catch (Exception e) {
                            LOG.warn(e);
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

        /*
        *(all|financial|healthcare|services|utilities|industrial_goods|basic_materials|conglomerates|consumer_goods|technology)
        data.FetchData lambda$fetchMLSEList$3 - ALIMENTARI  consumer_goods
data.FetchData lambda$fetchMLSEList$3 - ASSICURAZIONI financial
data.FetchData lambda$fetchMLSEList$3 - AUTOMOBILI E COMPONENTISTICA industrial_goods
data.FetchData lambda$fetchMLSEList$3 - BANCHE financial
data.FetchData lambda$fetchMLSEList$3 - BENI IMMOBILI 
data.FetchData lambda$fetchMLSEList$3 - CHIMICA
data.FetchData lambda$fetchMLSEList$3 - COMMERCIO
data.FetchData lambda$fetchMLSEList$3 - EDILIZIA E MATERIALI
data.FetchData lambda$fetchMLSEList$3 - MATERIE PRIME
data.FetchData lambda$fetchMLSEList$3 - MEDIA
data.FetchData lambda$fetchMLSEList$3 - PETROLIO E GAS NATURALE
data.FetchData lambda$fetchMLSEList$3 - PRODOTTI E SERVIZI INDUSTRIALI
data.FetchData lambda$fetchMLSEList$3 - PRODOTTI PER LA CASA, PER LA PERSONA, MODA
data.FetchData lambda$fetchMLSEList$3 - SALUTE
data.FetchData lambda$fetchMLSEList$3 - SERVIZI FINANZIARI
data.FetchData lambda$fetchMLSEList$3 - SERVIZI PUBBLICI
data.FetchData lambda$fetchMLSEList$3 - TECNOLOGIA
data.FetchData lambda$fetchMLSEList$3 - TELECOMUNICAZIONI
data.FetchData lambda$fetchMLSEList$3 - VIAGGI E TEMPO LIBERO
         */
        return all;
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
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
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
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
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
   /* 
    public static void fetchMLSEEOD() throws Exception {
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("MLSE")), Optional.empty(), Optional.empty());
        for (HashMap<String,String> m: map) {
            String hash=m.get("hashcode");
            String isin=m.get("isin");
            String type=m.get("type");
            SimpleEntry<UDate, HashMap<String,String>> e;
            if (type.equalsIgnoreCase("STOCK")) e=fetchDatiCompletiMLSE(isin, secType.STOCK);
            else if (type.equalsIgnoreCase("ETF")) e=fetchDatiCompletiMLSE(isin, secType.ETF);
            else if (type.equalsIgnoreCase("ETCETN")) fetchDatiCompletiMLSE(isin, secType.ETCETN);
            
        }
        String sql = "insert or replace into shares values (?,?,?,?,?,?,?,?);";    
        Connection conn = null;
        java.sql.PreparedStatement stmt = null;
        java.sql.PreparedStatement ustmt = null;
        
    }
    */
    public static void fetchSharesDetails() throws Exception {
//        String sql = "insert or replace into securities (hashcode,name,code,type,market,currency,sector,yahooquotes,bitquotes,googlequotes) values"
        //              + "(?,?,?,?,?,?,?,(select yahooquotes from securities where hashcode = ?),(select bitquotes from securities where hashcode = ?),(select googlequotes from securities where hashcode = ?));";

        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
        LOG.info("fetching Euronext");
        try {
            all.putAll(fetchEuroNext());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        LOG.info("fetching XETRA");
        try {
            all.putAll(fetchListDE());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        LOG.info("fetching NYSE");
        try {
            all.putAll(fetchNYSE());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }        
        LOG.info("fetching MLSE");
        try {
            all.putAll(fetchMLSEList(secType.ETCETN));
            all.putAll(fetchMLSEList(secType.ETF));
            all.putAll(fetchMLSEList(secType.STOCK));
            all.putAll(fetchMLSEList(secType.FUTURE));
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

    /*static java.util.HashMap<String, java.util.HashMap<String, String>> fetchListSole24Ore() throws Exception {
        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();
        String allShare = "http://finanza-mercati.ilsole24ore.com/quotazioni.php?QUOTE=Mibtel&cstdet=IndItaPan";
        String euroTLX = "http://finanza-mercati.ilsole24ore.com/azioni/eurotlx/azioni-estere/main.php?TA_START=#";
        String details = "http://finanza-mercati.ilsole24ore.com/quotazioni.php?QUOTE=!#";
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
        }

//        <td class="tdDefualtS1 first"><a href="&#10;&#9;&#9;&#9;&#9;&#9;&#9;&#9;&#9;/quotazioni.php?QUOTE=!BET.MI">Be</a></td>
        String s = new String(http.HttpGetUrl(allShare, Optional.empty(), Optional.empty()));
        Document doc = Jsoup.parse(s);
        Elements links = doc.select("td[class=\"tdDefualtS1 first\"] a");
        for (Element x : links) {
            
            int k1 = x.outerHtml().indexOf("QUOTE");
            int k2 = x.outerHtml().indexOf("\"", k1);
            if (x.outerHtml().indexOf("quotazioni.php?QUOTE") > 0) {
                java.util.HashMap<String, String> map = new java.util.HashMap<>();
                map.put("name", x.text());
                map.put("code", x.outerHtml().substring(k1 + 7, k2));
                map.put("type", "STOCK");
                map.put("market", "MLSE");
                map.put("currency", "EUR");
                //String det=details.replace("#", map.get("code"));
                //map.put("sector", sector);map.put("hashcode", hashcode);                
                LOG.debug(map.get("name") + "\t" + map.get("code"));
            }
        }
        
        boolean succ = false;
        int ta_start = 0;
        do {
            s = new String(http.HttpGetUrl(euroTLX.replace("#", Integer.toString(ta_start)), Optional.empty(), Optional.empty()));
            doc = Jsoup.parse(s);            
            links = doc.select("td[class=\"tdDefualtS1\"] a");
            Elements btn = doc.select("div[class=\"btnDirezione\"] a");
            for (Element x : links) {
                int k1 = x.outerHtml().indexOf("QUOTE");
                int k2 = x.outerHtml().indexOf("\"", k1);
                if (x.outerHtml().indexOf("quotazioni.php?QUOTE") > 0) {
                    java.util.HashMap<String, String> map = new java.util.HashMap<>();
                    map.put("name", x.text());
                    map.put("code", x.outerHtml().substring(k1 + 7, k2));
                    map.put("type", "STOCK");
                    map.put("market", "TLX");
                    map.put("currency", "EUR");
                    //String det=details.replace("#", map.get("code"));
                    //map.put("sector", sector);map.put("hashcode", hashcode);                
                    LOG.debug(map.get("name") + "\t" + map.get("code"));
                }
            }            
            succ = false;
            for (Element x : btn) {                
                if (x.text().toLowerCase().contains("successivo")) {
                    succ = true;
                }
            }
            ta_start += 30;
            LOG.debug(succ);
        } while (succ);
        return all;
    }
     */
    static java.util.HashMap<String, java.util.HashMap<String, String>> fetchEuroNext() throws Exception {
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
                put("Euronext Expert Market","VPXB"); 
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
            httpf.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
        }
        String s = new String(httpf.HttpGetUrl(u0, Optional.empty(), Optional.empty()));
        List<HttpCookie> ck=httpf.getCookies();
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
        try ( BufferedReader in = new BufferedReader(
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
                LOG.warn("market not found : " + row[3]+"\t"+line);
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

    static java.util.HashMap<String, java.util.HashMap<String, String>> fetchListDE() throws Exception {
        //String XetraSuffix="ETR";
        String XetraURL = "http://www.xetra.com/xetra-en/instruments/shares/list-of-tradable-shares";

        String det = "https://www.boerse-berlin.com/index.php/Shares?isin=#";//   "http://www.boerse-berlin.com/index.php/Shares?isin=#";
        java.util.HashMap<String, java.util.HashMap<String, String>> all = new java.util.HashMap<>();

        String url, type = "STOCK", currency = "EUR", market = "XETRA";
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_user, Init.http_proxy_password);
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

    public static void main(String[] args) throws Exception {

        //fetchMLSEList(secType.FUTURE);
       // fetchDatiCompletiMLSE("NL0010877643", secType.STOCK);
        //};
    }

}
