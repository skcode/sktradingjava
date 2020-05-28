/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.sktradingjava.Security.secType;
import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.LOG;
import com.ettoremastrogiacomo.utils.UDate;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author sk
 */
public class MLSE_DataFech {

    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FetchData.class);
    static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

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
        ArrayList<String> urls= new ArrayList<>();
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
                        if (urls.contains(sd)) return;else urls.add(sd);
                        try {
                            sd = new String(http.HttpGetUrl(sd, Optional.of(30), Optional.empty()));
                            Document docd = Jsoup.parse(sd);
                            //ArrayList<Element> tables=new ArrayList<>();
                            Elements tables=docd.select("table[class=\"m-table -clear-m\"]");
/*                            Element table1= docd.select("table[class=\"m-table -clear-m\"]").get(0);
                            Element table2= docd.select("table[class=\"m-table -clear-m\"]").get(1);
                            Element table3= docd.select("table[class=\"m-table -clear-m\"]").get(2);*/
                            HashMap<String,String> m= new HashMap<>();                            
                            for (Element table1: tables) {
                                Elements rows = table1.select("tr");
                                                
                                for (int i = 0; i < rows.size(); i++) { //first row is the col names so skip it.
                                    Element row = rows.get(i);
                                    Elements cols = row.select("td");
                                    if (cols.size()==2){
                                    m.put(cols.get(0).text(), cols.get(1).text());
                                    }
                                }                                     
                            }
                            UDate d;
                            if (type.equalsIgnoreCase("FUTURE")){
                                d=new UDate();
                                d= new UDate(d.time-1000*60*60*24);
                                map.put("code", "FTSEMIB");
                                map.put("refprice", m.get("Prezzo di Regolamento Precedente:"));
                                map.put("high", m.get("Max oggi"));
                                map.put("low", m.get("Min oggi"));
                                map.put("open", m.get("Prezzo Apertura:"));
                                map.put("volume", m.get("Volume Totale Contratti"));
                                map.put("refdate", d.toYYYYMMDD());
                                map.put("close", m.get("Prezzo Ultimo Contratto"));                                
                            } else if (type.equalsIgnoreCase("STOCK")) {
                                String pdr=m.get("Prezzo di riferimento");                            
                                int idxd=pdr.indexOf("-");
                                String price=pdr.substring(0,idxd-1);
                                map.put("refprice", price);
                                pdr=pdr.substring(idxd+2);                
                                int y=Integer.parseInt(pdr.substring(6, 8));
                                int M=Integer.parseInt(pdr.substring(3, 5));
                                int day=Integer.parseInt(pdr.substring(0, 2));                            
                                if (pdr.length()>8){
                                    int h=Integer.parseInt(pdr.substring(9, 11));
                                    int min=Integer.parseInt(pdr.substring(12, 14));
                                    int sec=Integer.parseInt(pdr.substring(15));                                            
                                    d=UDate.genDate(y+2000 , M-1, day, h, min, sec);
                                } else {
                                    d=UDate.genDate(y+2000 , M-1, day, 22, 0, 0);
                                }                                             
                                map.put("code", m.get("Codice Alfanumerico"));
                                map.put("high", m.get("Max Oggi"));
                                map.put("low", m.get("Min Oggi"));
                                map.put("open", m.get("Apertura Odierna:"));
                                map.put("volume", m.get("Quantità Totale"));
                                map.put("close", m.get("Chiusura:"));
                                map.put("refdate", d.toYYYYMMDD());                                
                            }else if (type.equalsIgnoreCase("ETF") ||type.equalsIgnoreCase("ETCETN")) {
                                String pdr=m.get("Prezzo di riferimento");                            
                                int idxd=pdr.indexOf("-");
                                String price=pdr.substring(0,idxd-1);
                                map.put("refprice", price);
                                pdr=pdr.substring(idxd+2);                
                                int y=Integer.parseInt(pdr.substring(6, 8));
                                int M=Integer.parseInt(pdr.substring(3, 5));
                                int day=Integer.parseInt(pdr.substring(0, 2));                            
                                if (pdr.length()>8){
                                    int h=Integer.parseInt(pdr.substring(9, 11));
                                    int min=Integer.parseInt(pdr.substring(12, 14));
                                    int sec=Integer.parseInt(pdr.substring(15));                                            
                                    d=UDate.genDate(y+2000 , M-1, day, h, min, sec);
                                } else {
                                    d=UDate.genDate(y+2000 , M-1, day, 22, 0, 0);
                                }                                             
                                map.put("code", m.get("Codice Alfanumerico"));
                                map.put("high", m.get("Max oggi"));
                                map.put("low", m.get("Min oggi"));
                                map.put("open", m.get("Apertura"));
                                map.put("volume", m.get("Volume totale"));
                                map.put("close", m.get("Prezzo asta di chiusura odierna"));
                                map.put("refdate", d.toYYYYMMDD());                                
                            }
                            
                            

                            map.put("fase",m.get("Fase di Mercato"));
                            if (!(m.get("Fase di Mercato").equalsIgnoreCase("chiusura")|| m.get("Fase di Mercato").equalsIgnoreCase("fine servizio"))) {
                                LOG.warn("mercato non chiuso per " +map.get("isin"));
                                return;
                            }
                            map.put("sector", m.toString());
                            map.keySet().forEach((xx)->{LOG.debug(xx+"\t"+map.get(xx));});
                            LOG.debug("********************");
                            String hash = FetchData.computeHashcode(map.get("isin"), market);//Encoding.base64encode(getSHA1(String2Byte((map.get("isin") + market))));
                            if (st==secType.FUTURE) {
                                if (all.size()==1) return;
                                else all.put(hash, map);
                            } else 
                            if (!all.containsKey(hash)) {
                                all.put(hash, map);
                            }
                        } catch (Exception e) {
                            LOG.warn("error fetching " +urldet.replace("#", isin)+"\n"+e);
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
 public static void main(String[] args) throws Exception {

        fetchMLSEList(secType.FUTURE);
        //fetchDatiCompletiMLSE("NL0010877643", secType.STOCK);
        //};
    }
}
