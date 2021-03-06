package com.ettoremastrogiacomo.sktradingjava.data;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.utils.UDate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.JSONArray;
import org.json.JSONObject;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * Ultimo prezzo: Prezzo dell'ultimo contratto. Volume ultimo: quantità
 * scambiata dell'ultimo contratto. Var%: Variazione percentuale calcolata come
 * differenza tra l'Ultimo Prezzo e il Prezzo di Riferimento del giorno
 * precedente Variazione Assoluta: Variazione calcolata come differenza tra
 * l'Ultimo Prezzo e il Prezzo di Riferimento del giorno precedente Ora: Orario
 * dell'ultimo contratto. Prezzo di apertura: Prezzo d'asta di apertura del
 * titolo. FTSE/MIB Apertura Odierna : valore dell'indice calcolato sui prezzi
 * del primo contratto concluso dalle azioni componenti Prezzo di riferimento:
 * Prezzo che viene utilizzato per il calcolo della variazione percentuale; è
 * pari alla media ponderata dell'ultimo 10% delle quantità negoziate. Prezzo
 * ufficiale: prezzo medio, ponderato per le relative quantità, di tutti i
 * contratti conclusi durante la giornata. Prezzo di chiusura: prezzo al quale
 * vengono conclusi i contratti in asta di chiusura. Prezzo Medio Progressivo:
 * calcolato come rapporto Controvalore totale/Volume scambiato Prezzo di
 * controllo: in apertura coincide con il riferimento del giorno precedente,
 * durante la negoziazione è il prezzo di apertura, a fine seduta coincide con
 * il riferimento del giorno stesso. Min/Max oggi: Prezzo minimo e massimo
 * registrati dal titolo nella giornata di negoziazione. Min/Max anno: Prezzo
 * minimo e massimo registrati dal titolo dal primo giorno di negoziazione
 * dell'anno. Volume totale: Quantità complessiva di titoli scambiati nella
 * giornata Controvalore: Controvalore totale del titolo, calcolato come
 * prodotto dei prezzi per le relative quantità. N. Contratti: Numero dei
 * contratti conclusi nella giornata borsistica Fase di mercato: Fase di mercato
 * in cui si trova il titolo nel corso della giornata. Simbolo di tendenza
 * Indicatore che rileva la tendenza tra ultimo e penultimo prezzo della
 * giornata (in aumento, in diminuzione e invariato) Capitalizzazione: valore di
 * una società quotata, pari al prodotto tra il numero delle sue azioni e il
 * loro prezzo ufficiale. Capitale sociale: prodotto tra il numero delle azioni
 * quotate ed il corrispondente valore nominale.
 *
 * Legenda Scheda Societaria Ultimo aggiornamento: 7 Ottobre 2014 - 17:53
 *
 */
/**
 *
 * @author a241448
 */
public class Database {

    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(Database.class);
    static final java.util.List<String> MONTHS = Arrays.asList("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

    public static enum Markets {
        MLSE(1), EURONEXT(2), XETRA(3), NYSE(4), NASDAQ(5);
        private final int id;
        private static final ArrayList<String> list = new ArrayList<>();

        static {
            for (Markets m : Markets.values()) {
                list.add(m.name());
            }
        }

        public static boolean contains(String market) {
            return list.contains(market);
        }

        private Markets(int id) {
            this.id = id;
        }

        public int getID() {
            return this.id;
        }

    };

    public static enum Providers {
        BORSAITALIANA(1), EURONEXT(2), XETRA(3), NYSE(4), INVESTING(5), YAHOO(6), GOOGLE(7), SOLE24ORE(8);
        private final int priority;

        Providers(int priority) {
            this.priority = priority;
        }

        int getPriority() {
            return priority;
        }

        public String getNote() {
            if (this.name().equals("BORSAITALIANA")) {
                return "borsa italiana quotes";
            }
            if (this.name().equals("INVESTING")) {
                return "investing quotes";
            }
            if (this.name().equals("YAHOO")) {
                return "yahoo quotes";
            }
            if (this.name().equals("GOOGLE")) {
                return "google quotes";
            }
            if (this.name().equals("EURONEXT")) {
                return "euronext quotes";
            }
            if (this.name().equals("XETRA")) {
                return "xetra quotes";
            }
            if (this.name().equals("NYSE")) {
                return "nyse quotes";
            }
            if (this.name().equals("SOLE24ORE")) {
                return "sole24ore quotes";
            }

            return "";
        }
    };

    public static void deleteSharesTable() {
        String url = Init.db_url;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            stmt.execute("drop table  if exists " + Init.db_sharestable);
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
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    public static void clearEODTable() {
        String url = Init.db_url;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            stmt.execute("delete from " + Init.db_eoddatatable);
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
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    public static void clearSharesTable(Markets market) {
        String url = Init.db_url;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            stmt.execute("delete from " + Init.db_sharestable + " where market='" + market.name() + "'");
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
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    /**
     * create db with table securities
     * (isin,name,code,type,market,currency,sector,yahooquotes,bitquotes)
     */
    public static void createSecTable() {
        // SQLite connection string
        String url = Init.db_url;//"jdbc:sqlite:C://sqlite/db/tests.db";        
        // SQL statement for creating a new table

        String sql_intraday = "CREATE TABLE IF NOT EXISTS " + Init.db_intradaytable + " (\n"
                + "	hashcode text not null,\n"
                + "	date text not null,\n"
                + "	quotes text not null,\n"
                + "     PRIMARY KEY (hashcode,date));";

        String sql_shares = "CREATE TABLE IF NOT EXISTS " + Init.db_sharestable + " (\n"
                + "	hashcode text not null,\n"
                + "	isin text NOT NULL,\n"
                + "	name text not null,\n"
                + "	code text NOT NULL,\n"
                + "	type text not null,\n"
                + "	market text not null,\n"
                + "	currency text not null,\n"
                + "	sector text not null,\n"
                + "	primary key (hashcode) ,\n"
                + "     unique (isin,market));";//,\n" 

        String sql_eod = "CREATE TABLE IF NOT EXISTS " + Init.db_eoddatatable + " (\n"
                + "     hashcode not null,\n"
                + "     data text not null,\n"
                + "     provider text not null,\n"
                + "     PRIMARY KEY (hashcode,provider),\n"
                + "     FOREIGN KEY(provider) REFERENCES " + Init.db_providerstable + "(name));";

        String sql_providers = "CREATE TABLE IF NOT EXISTS " + Init.db_providerstable + " (\n"
                + "     name not null,\n"
                + "     priority integer not null,\n"
                + "     notes text,\n"
                + "     PRIMARY KEY (name));";

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(url);
            stmt = conn.createStatement();
            stmt.execute(sql_providers);
            stmt.execute(sql_eod);
            stmt.execute(sql_shares);
            stmt.execute(sql_intraday);
            Providers[] p = Providers.values();
            for (Providers p1 : p) {
                String s1 = "insert or replace into " + Init.db_providerstable + "(name,priority,notes) values('" + p1.name() + "'," + p1.getPriority() + ",'" + p1.getNote() + "')";
                stmt.execute(s1);
            }
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
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }

    }

    public static java.util.ArrayList<String> getMarkets() {
        java.util.ArrayList<String> mkts = new java.util.ArrayList<>();
        List<HashMap<String, String>> list = getRecords(Optional.empty());
        list.forEach((x) -> {
            if (!mkts.contains(x.get("market"))) {
                mkts.add(x.get("market"));
            }
        });
        return mkts;
    }

    public static java.util.HashMap<String, TreeSet<UDate>> getIntradayDatesMap() throws Exception {
        String sql = "select hashcode,date from " + Init.db_intradaytable;
        Connection conn = null;
        Statement stmt = null;
        java.sql.ResultSet res = null;
        java.util.HashMap<String, TreeSet<UDate>> map = new java.util.HashMap<>();
        try {
            conn = DriverManager.getConnection(Init.db_url);
            stmt = conn.createStatement();
            res = stmt.executeQuery(sql);

            while (res.next()) {
                String hash = res.getString(1);
                UDate d = UDate.parseYYYYMMDD(res.getString(2));

                if (map.containsKey(hash)) {
                    map.get(hash).add(d);
                } else {
                    TreeSet<UDate> ts = new TreeSet<>();
                    ts.add(d);
                    map.put(hash, ts);
                }
            }

        } catch (SQLException e) {
            LOG.error("cannot fetch list", e);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
        return map;
    }

    public static java.util.TreeMap<UDate, ArrayList<String>> getIntradayDatesReverseMap() throws Exception {
        String sql = "select hashcode,date from " + Init.db_intradaytable;
        Connection conn = null;
        Statement stmt = null;
        java.sql.ResultSet res = null;
        //java.util.HashMap<String, TreeSet<UDate>> map = new java.util.HashMap<>();
        java.util.TreeMap<UDate, ArrayList<String>> map = new TreeMap<>();
        try {
            conn = DriverManager.getConnection(Init.db_url);
            stmt = conn.createStatement();

            res = stmt.executeQuery(sql);

            while (res.next()) {
                String hash = res.getString(1);
                UDate d = UDate.parseYYYYMMDD(res.getString(2));
                if (map.containsKey(d)) {
                    map.get(d).add(hash);
                } else {
                    ArrayList<String> ts = new ArrayList<>();
                    ts.add(hash);
                    map.put(d, ts);
                }
            }
        } catch (SQLException e) {
            LOG.error("cannot fetch list", e);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
        return map;
    }

    /**
     *
     * @param wheresql filter sql with where , e.g. "where market='MLSE' and
     * type='STOCK'"
     * @return array of hashmap
     * (hashcode,isin,name,code,market,type,currency,sector)
     */
    public static java.util.ArrayList<java.util.HashMap<String, String>> getRecords(Optional<String> wheresql) {
        String wsql = wheresql.orElse("").trim();
        if (!wsql.toLowerCase().startsWith("where") && wsql.length() > 0) {
            wsql = "where " + wsql;
        }
        String sql = "select hashcode,isin,name,code,market,type,currency,sector from shares " + wsql;
        java.util.ArrayList<java.util.HashMap<String, String>> list = new java.util.ArrayList<>();
        //java.util.HashSet<String> set=java.util.HashSet<String>();
        Connection conn = null;
        Statement stmt = null;
        java.sql.ResultSet res = null;
        try {
            conn = DriverManager.getConnection(Init.db_url);
            stmt = conn.createStatement();
            // LOG.debug(sql);
            res = stmt.executeQuery(sql);
            int ncol = res.getMetaData().getColumnCount();

            while (res.next()) {
                java.util.HashMap<String, String> t = new java.util.HashMap<>();
                for (int i = 0; i < ncol; i++) {
                    t.put(res.getMetaData().getColumnName(i + 1), res.getString(i + 1));
                }
                list.add(t);
            }

        } catch (SQLException e) {
            LOG.error("cannot fetch securities list", e);
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
        return list;
    }

    /**
     *
     * @param code
     * @param market
     * @return hashcode from market+code
     * @throws Exception
     */
    public static String getHashcode(String code, String market) throws Exception {
        List<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList(code)), Optional.empty(), Optional.of(Arrays.asList(market)), Optional.empty(), Optional.empty());
        if (map.size() < 1) {
            throw new Exception(code + "." + market + " not found");
        }
        return map.get(0).get("hashcode");
    }

    /**
     *
     * @param isin
     * @param market
     * @return hashcode from market+code
     * @throws Exception
     */
    public static String getHashcodefromIsin(String isin, String market) throws Exception {
        List<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.of(Arrays.asList(isin)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList(market)), Optional.empty(), Optional.empty());
        if (map.size() < 1) {
            throw new Exception(isin + "." + market + " not found");
        }
        return map.get(0).get("hashcode");
    }

    /**
     *
     * @param hashcodes list of hashcodes
     * @return hashmap : hashcode, code.market.type.currency.isin.name
     * @throws Exception
     */
    public static HashMap<String, String> getCodeMarketName(List<String> hashcodes) throws Exception {
        List<HashMap<String, String>> map = Database.getRecords(Optional.of(hashcodes), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        if (map.isEmpty()) {
            throw new Exception("hashcodes not found");
        }
        HashMap<String, String> ret = new HashMap<>();
        map.forEach((x) -> {
            ret.put(x.get("hashcode"), x.get("code") + "." + x.get("market") + "." + x.get("type") + "." + x.get("currency") + "." + x.get("isin") + "." + x.get("name"));
        });
        return ret;
        //return map.get(0).get("hashcode");
    }

    public static TreeSet<UDate> getIntradayDates(String hashcode) throws Exception {
        HashMap<String, TreeSet<UDate>> m = getIntradayDatesMap();
        return m.get(hashcode);
    }

    public static TreeSet<UDate> getIntradayDates() throws Exception {
        HashMap<String, TreeSet<UDate>> m = getIntradayDatesMap();
        TreeSet<UDate> s = new TreeSet<>();
        m.keySet().forEach((x) -> {
            s.addAll(m.get(x));
        });
        return s;
    }

    public static Set<String> getIntradayHashCodes(Optional<UDate> d) throws Exception {
        HashMap<String, TreeSet<UDate>> m = getIntradayDatesMap();
        if (d.isEmpty()) {
            return m.keySet();
        } else {
            java.util.HashSet<String> s = new HashSet<>();
            m.keySet().forEach((x) -> {
                if (m.get(x).contains(d.get())) {
                    s.add(x);
                }
            });
            return s;
        }

        //return m.get(hashcode);
    }

    public static java.util.ArrayList< java.util.HashMap<String, String>> getRecords(Optional<List<String>> hashcode, Optional<List<String>> isin, Optional<List<String>> name, Optional<List<String>> code, Optional<List<String>> type, Optional<List<String>> market, Optional<List<String>> currency, Optional<List<String>> sector) throws Exception {

        StringBuilder hashcodesql = new StringBuilder();
        if (hashcode.isPresent()) {
            List<String> l = hashcode.get();
            if (l.size() > 0) {
                l.forEach((x) -> {
                    hashcodesql.append(",").append("'").append(x).append("'");
                });
                hashcodesql.replace(0, 1, "(");
                hashcodesql.append(")");
                hashcodesql.replace(0, 0, "hashcode in ");
            }
        }

        StringBuilder isinsql = new StringBuilder();
        if (isin.isPresent()) {
            List<String> l = isin.get();
            if (l.size() > 0) {

                l.forEach((x) -> {
                    isinsql.append(",").append("'").append(x).append("'");
                });
                //where = where.equals("") ? " where hashcode='" + hashcode.get() + "' " : where + " and hashcode='" + hashcode.get() + "' ";            
                isinsql.replace(0, 1, "(");
                isinsql.append(")");
                isinsql.replace(0, 0, "isin in ");
            }
        }

        StringBuilder namesql = new StringBuilder();
        if (name.isPresent()) {
            List<String> l = name.get();
            if (l.size() > 0) {

                l.forEach((x) -> {
                    namesql.append(",").append("'").append(x).append("'");
                });
                //where = where.equals("") ? " where hashcode='" + hashcode.get() + "' " : where + " and hashcode='" + hashcode.get() + "' ";            
                namesql.replace(0, 1, "(");
                namesql.append(")");
                namesql.replace(0, 0, "name in ");
            }
        }

        StringBuilder codesql = new StringBuilder();
        if (code.isPresent()) {
            List<String> l = code.get();
            if (l.size() > 0) {

                l.forEach((x) -> {
                    codesql.append(",").append("'").append(x).append("'");
                });
                //where = where.equals("") ? " where hashcode='" + hashcode.get() + "' " : where + " and hashcode='" + hashcode.get() + "' ";            
                codesql.replace(0, 1, "(");
                codesql.append(")");
                codesql.replace(0, 0, "code in ");
            }
        }

        StringBuilder typesql = new StringBuilder();
        if (type.isPresent()) {
            List<String> l = type.get();
            if (l.size() > 0) {
                l.forEach((x) -> {
                    typesql.append(",").append("'").append(x).append("'");
                });
                //where = where.equals("") ? " where hashcode='" + hashcode.get() + "' " : where + " and hashcode='" + hashcode.get() + "' ";            
                typesql.replace(0, 1, "(");
                typesql.append(")");
                typesql.replace(0, 0, "type in ");
            }
        }

        StringBuilder marketsql = new StringBuilder();
        if (market.isPresent()) {
            List<String> l = market.get();
            if (l.size() > 0) {
                l.forEach((x) -> {
                    marketsql.append(",").append("'").append(x).append("'");
                });
                //where = where.equals("") ? " where hashcode='" + hashcode.get() + "' " : where + " and hashcode='" + hashcode.get() + "' ";            
                marketsql.replace(0, 1, "(");
                marketsql.append(")");
                marketsql.replace(0, 0, "market in ");
            }
        }

        StringBuilder currencysql = new StringBuilder();
        if (currency.isPresent()) {
            List<String> l = currency.get();
            if (l.size() > 0) {
                l.forEach((x) -> {
                    currencysql.append(",").append("'").append(x).append("'");
                });
                //where = where.equals("") ? " where hashcode='" + hashcode.get() + "' " : where + " and hashcode='" + hashcode.get() + "' ";            
                currencysql.replace(0, 1, "(");
                currencysql.append(")");
                currencysql.replace(0, 0, "currency in ");
            }
        }
        StringBuilder sectorsql = new StringBuilder();
        if (sector.isPresent()) {
            List<String> l = sector.get();
            if (l.size() > 0) {
                l.forEach((x) -> {
                    sectorsql.append(",").append("'").append(x).append("'");
                });
                //where = where.equals("") ? " where hashcode='" + hashcode.get() + "' " : where + " and hashcode='" + hashcode.get() + "' ";            
                sectorsql.replace(0, 1, "(");
                sectorsql.append(")");
                sectorsql.replace(0, 0, "sector in ");
            }
        }

        String and = " and ";
        String where = " where ";//+hashcodesql+ " and " +isinsql;
        if (hashcodesql.length() > 0) {
            where += hashcodesql + and;
        }
        if (isinsql.length() > 0) {
            where += isinsql + and;
        }
        if (namesql.length() > 0) {
            where += namesql + and;
        }
        if (codesql.length() > 0) {
            where += codesql + and;
        }
        if (marketsql.length() > 0) {
            where += marketsql + and;
        }
        if (typesql.length() > 0) {
            where += typesql + and;
        }
        if (currencysql.length() > 0) {
            where += currencysql + and;
        }
        if (sectorsql.length() > 0) {
            where += sectorsql + and;
        }
        int k1 = where.lastIndexOf(and);
        if (k1 > 0) {
            where = where.substring(0, k1);
        }
        if (where.equalsIgnoreCase(" where ")) {
            where = "";
        }
        String sql = "select hashcode,isin,name,code,type,market,currency,sector from shares" + where;
        //LOG.debug(sql);
        return getRecords(Optional.of(where));
    }

    public static Fints getFintsQuotes(String hashcode) throws Exception {
        List<HashMap<String, String>> list = Database.getRecords(Optional.of(Arrays.asList(hashcode)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        if (list.isEmpty()) {
            throw new Exception("hashcode " + hashcode + " not found");
        }
        return getFintsQuotes(Optional.of(list.get(0).get("code")), Optional.of(list.get(0).get("market")), Optional.of(list.get(0).get("isin")));

    }

    /**
     *
     * @param code codice titolo
     * @param market mercato
     * @param isin
     * @return Fints con campi (open,high,low,close,volume,oi) se o,h,l non sono
     * positivi, pongo uguale a close
     * @throws Exception
     */
    public static Fints getFintsQuotes(Optional<String> code, Optional<String> market, Optional<String> isin) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        java.sql.ResultSet res = null;
        ArrayList<UDate> dates = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        double[][] matrix;
        Fints ret = new Fints();
        String codev, marketv;
        try {
            conn = DriverManager.getConnection(Init.db_url);
            stmt = conn.createStatement();
            String hashcode;
            if (isin.isPresent() && market.isPresent()) {//str.replaceAll(“\””, “\\\\\””);
                res = stmt.executeQuery("select hashcode,code,market from " + Init.db_sharestable + "where isin='" + isin.get() + "' and market='" + market.get().replaceAll("\"", "\\\\\"") + "'");

                if (res.next()) {
                    hashcode = res.getString("hashcode");
                    codev = res.getString("code");
                    marketv = res.getString("market");
                } else {
                    throw new Exception("isin " + isin.get() + " not found");
                }
                res = stmt.executeQuery("select data,provider from " + Init.db_eoddatatable + " where hashcode='" + hashcode + "'");
            } else if (code.isPresent() && market.isPresent()) {
                String q = "select hashcode,code,market from " + Init.db_sharestable + " where code='" + code.get() + "' and market='" + market.get().replaceAll("\"", "\\\"") + "'";
                res = stmt.executeQuery(q);
                if (res.next()) {
                    hashcode = res.getString("hashcode");
                    codev = res.getString("code");
                    marketv = res.getString("market");
                } else {
                    throw new Exception("code " + code.get() + " not found");
                }

                res = stmt.executeQuery("select data,provider from " + Init.db_eoddatatable + " where hashcode='" + hashcode + "'");

            } else {
                throw new Exception("isin+market or code+market must be present");
            }

            //TreeMap<Integer,JSONArray> jsondata= new TreeMap<>();//scelgo il provider con più dati se ultima data è uguale, altrimenti quello con ultima data
            UDate bestdate = new UDate(0);
            int bestlen = 0;
            JSONArray bestarr = new JSONArray();
            while (res.next()) {
                JSONArray ja1 = new JSONArray(res.getString("data"));
                if (ja1.isEmpty()) {
                    continue;
                }
                /*try {
                JSONObject jo1= ja1.getJSONObject(ja1.length() - 1);
                }catch (Exception e) {LOG.error(e, e);LOG.debug(ja1);System.exit(1);}
                 */
                UDate d1 = UDate.parseYYYYMMDD(ja1.getJSONObject(ja1.length() - 1).getString("date"));
                if (d1.after(bestdate)) {
                    bestarr = ja1;
                    bestlen = ja1.length();
                    bestdate = d1;
                } else if (d1.compareTo(bestdate) == 0) {
                    if (ja1.length() > bestlen) {
                        bestarr = ja1;
                        bestlen = ja1.length();
                        bestdate = d1;
                    }
                }

            }

            if (!bestarr.isEmpty()) {
                //LOG.debug("LOADING data FOR " + codev + "." + marketv);
                JSONArray arr = bestarr;
                java.util.TreeMap<UDate, java.util.ArrayList<Double>> map = new java.util.TreeMap<>();
                matrix = new double[arr.length()][6];
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String date = o.getString("date");
                    Double open = o.getDouble("open");
                    Double high = o.getDouble("high");
                    Double low = o.getDouble("low");
                    Double close = o.getDouble("close");
                    if (open <= 0) {
                        open = close;
                    }
                    if (high <= 0) {
                        high = close;
                    }
                    if (low <= 0) {
                        low = close;
                    }
                    Double volume = o.getDouble("volume");
                    Double oi = o.getDouble("oi");
                    if (volume < 0) {
                        volume = 0.;
                    }
                    if (oi < 0) {
                        oi = 0.;
                    }
                    map.put(UDate.parseYYYYMMDD(date), new ArrayList<>(Arrays.asList(open, high, low, close, volume, oi)));
                }
                int j = 0;
                for (UDate x : map.keySet()) {
                    dates.add(x);
                    for (int i = 0; i < 6; i++) {
                        matrix[j][i] = map.get(x).get(i);
                    }
                    j++;
                }
                names.add("OPEN(" + codev + "." + marketv + ")");
                names.add("HIGH(" + codev + "." + marketv + ")");
                names.add("LOW(" + codev + "." + marketv + ")");
                names.add("CLOSE(" + codev + "." + marketv + ")");
                names.add("VOLUME(" + codev + "." + marketv + ")");
                names.add("OI(" + codev + "." + marketv + ")");
                ret = new Fints(dates, names, Fints.frequency.DAILY, matrix);
            } else {
                throw new SQLException(code + "." + market + " not found or empty");
            }
        } catch (SQLException e) {
            LOG.error("cannot fetch " + code + "." + market, e);
            throw e;

        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
        return ret;
    }
    /**
     * @param hashcodes list
     *
     * @param length e.g. 200 samples
     * @param maxpcgap e.g. .2 gap in stock prices
     * @param maxdaygap e.g. 4 gap in ydata days
     * @param maxold e.g. 7 gap from current date
     * @param minvol e.g. 10000 mean volumes in length samples
     * @param sharpe_treshold e.g. 0
     * @return ArrayList hashcodes list matching criteria
     * @throws Exception
     */
    public static java.util.ArrayList<String> getFilteredPortfolio(Optional<List<String>> hashcodes, Optional<Integer> length, Optional<Double> maxpcgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol, Optional<Double> sharpe_treshold) throws Exception {
        java.util.ArrayList<String> retlist = new java.util.ArrayList<>();
        java.util.ArrayList<java.util.HashMap<String, String>> list = Database.getRecords(Optional.empty());

        //int win = length.orElse(0);
        long oneday = 1000 * 60 * 60 * 24;
        for (java.util.HashMap<String, String> map : list) {
            // if (map.get("type").equalsIgnoreCase(type.orElse("STOCK")) && map.get("market").equalsIgnoreCase(market.orElse("MTA"))) {
            //LOG.debug(map.get("name") + ";" + map.get("code") + ";" + map.get("type") + ";" + map.get("market") + ";" + map.get("sector"));
            if (hashcodes.isPresent()) {

                if (hashcodes.get().indexOf(map.get("hashcode")) < 0) {
                    // LOG.debug("hashcode " + map.get("hashcode") + " not in list");
                    continue;
                }
            }

            try {

                Fints t = Database.getFintsQuotes(Optional.of(map.get("code")), Optional.of(map.get("market")), Optional.empty());//.head(win * 2);//.getSerieCopy(3);                   
                //check length
                if (length.isPresent()) {
                    if (t.getLength() < length.get()) {
                        //   LOG.debug("length too short:" + t.getLength() + "<" + length.get());
                        continue;
                    }
                }
                //LOG.debug("length=" + t.getLength());
                //check sharpe
                if (sharpe_treshold.isPresent()) {
                    if (t.getLength() >= 250) {
                        Fints er = Fints.ER(t.getSerieCopy(3).head(250), 100, true);
                        Fints sma = Fints.SMA(Fints.Sharpe(er, 20), 200);
                        if (sma.get(sma.getLength() - 1, 0) <= sharpe_treshold.get()) {
                            //     LOG.debug("sharpe below treshold: " + sma.get(sma.getLength() - 1, 0) + "<=" + sharpe_treshold.get());
                            continue;
                        }
                        // LOG.debug("sharpe=" + sma.get(sma.getLength() - 1, 0));
                    } else {
                        // LOG.warn("cannot check for sharpe (len<250)");
                        continue;
                    }
                }

                int win = length.isPresent() ? length.get() : t.getLength();
                //check volumes

                double volmean = t.getSerieCopy(4).head(win).getMeans()[0];
                if (minvol.isPresent()) {
                    if (volmean < minvol.get()) {
                        // LOG.debug("too few volumes: " + volmean + "<" + minvol.get());
                        continue;
                    }
                }
                //LOG.debug("volmean=" + volmean);
                //check lastdate
                double dfn = t.getDaysFromNow();
                if (maxold.isPresent()) {
                    if (dfn > maxold.get()) {
                        //      LOG.debug("lasdate older " + dfn + " days from now");
                        continue;
                    }
                }
                //LOG.debug("lastdate=" + t.getLastDate());
                //check maxvaluegap
                t = t.getSerieCopy(3).head(win);//set to close
                double mapvg = t.getMaxAbsPercentValueGap(0);
                if (maxpcgap.isPresent()) {
                    if (mapvg > maxpcgap.get()) {
                        //      LOG.debug("max gap " + mapvg + ">" + maxpcgap.get());
                        continue;
                    }
                }
                //LOG.debug("maxpcvaluegap=" + t.getMaxAbsPercentValueGap(0));
                //dategap
                double mdg = t.getMaxDaysDateGap();
                if (maxdaygap.isPresent()) {
                    if (mdg > maxdaygap.get()) {
                        //      LOG.debug("dates gap " + mdg + ">" + maxdaygap.get());
                        continue;
                    }
                }
                //LOG.debug("maxdategap=" + t.getMaxDateGap() / oneday);
                retlist.add(map.get("hashcode"));
                //f = f == null ? t : f.merge(t);
                LOG.info("added " + map.get("isin") + "\t" + map.get("name") + "." + map.get("market"));
            } catch (Exception e) {
                LOG.warn(e, e);
            }
        }

        return retlist;
    }

    public static Fints getIntradayFintsQuotes(String code, String market, UDate date) throws Exception {
        String hash = Database.getHashcode(code, market);
        return getIntradayFintsQuotes(hash, date);
    }

    public static Fints getIntradayFintsQuotes(String hashcode, UDate date) throws Exception {
        String sql = "select quotes from " + Init.db_intradaytable + " where hashcode='" + hashcode + "' and date='" + date.toYYYYMMDD() + "'";
        String sql2 = "select code, market from " + Init.db_sharestable + " where hashcode='" + hashcode + "'";
        Fints ret = new Fints();
        try (Connection conn = DriverManager.getConnection(Init.db_url); Statement stmt = conn.createStatement(); Statement stmt2 = conn.createStatement()) {
            ResultSet res = stmt.executeQuery(sql);
            ResultSet res2 = stmt2.executeQuery(sql2);
            if (res.next() == false || res2.next() == false) {
                throw new Exception("not found : " + hashcode + "\t" + date);
            } else {
                String market = res2.getString("market");
                String code = res2.getString("code");
                JSONArray arr = new JSONArray(res.getString("quotes"));
                TreeMap<UDate, ArrayList<Double>> map = new TreeMap<>();
                //{"date":"2020-06-16 09:06:00.00","volume":20,"high":14.336,"low":14.336,"oi":0,"close":14.336,"open":14.336}
                for (int i = 0; i < arr.length(); i++) {
                    ArrayList<Double> l = new ArrayList<>();
                    JSONObject o = arr.getJSONObject(i);
                    UDate d = UDate.parse(o.getString("date"), "yyyy-MM-dd HH:mm:ss.SS");
                    l.add(o.getDouble("open"));
                    l.add(o.getDouble("high"));
                    l.add(o.getDouble("low"));
                    l.add(o.getDouble("close"));
                    l.add(o.getDouble("volume"));
                    l.add(o.getDouble("oi"));
                    map.put(d, l);
                }
                double[][] matrix = new double[map.size()][6];
                int i = 0;
                for (UDate d : map.keySet()) {
                    for (int j = 0; j < matrix[i].length; j++) {
                        matrix[i][j] = map.get(d).get(j);
                    }
                    i++;
                }
                ArrayList<String> names = new ArrayList<>();
                names.add("OPEN(" + code + "." + market + ")");
                names.add("HIGH(" + code + "." + market + ")");
                names.add("LOW(" + code + "." + market + ")");
                names.add("CLOSE(" + code + "." + market + ")");
                names.add("VOLUME(" + code + "." + market + ")");
                names.add("OI(" + code + "." + market + ")");
                ret = new Fints(new ArrayList<>(map.keySet()), names, Fints.frequency.MINUTE, matrix);

            }
        } catch (Exception e) {
            LOG.error("cannot fetch " + hashcode, e);
        }
        return ret;
    }

    /*public static Fints getIntradayFintsQuotes(String hashcode, UDate date) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        java.sql.ResultSet res = null;
        ArrayList<UDate> dates = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        double[][] matrix;
        java.util.TreeMap<UDate, Double> mapvolume = new java.util.TreeMap<>();
        java.util.TreeMap<UDate, Double> mapopen = new java.util.TreeMap<>();
        java.util.TreeMap<UDate, Double> maphigh = new java.util.TreeMap<>();
        java.util.TreeMap<UDate, Double> maplow = new java.util.TreeMap<>();
        java.util.TreeMap<UDate, Double> mapclose = new java.util.TreeMap<>();
        try {
            String month = (date.getMonth() + 1) < 10 ? "0" + Integer.toString(date.getMonth() + 1) : Integer.toString(date.getMonth() + 1);
            String day = (date.getDayofMonth()) < 10 ? "0" + Integer.toString(date.getDayofMonth()) : Integer.toString(date.getDayofMonth());
            String strdate = day + "/" + month + "/" + (date.getYear() - 2000);
            conn = DriverManager.getConnection(Init.db_url);
            stmt = conn.createStatement();
            String sql = "select quotes from intradayquotes where hashcode='" + hashcode + "' and date='" + strdate + "'";
            NumberFormat nf = NumberFormat.getInstance(Locale.ITALY);
            res = stmt.executeQuery("select * from shares where hashcode='" + hashcode + "'");
            String code, market;
            if (res.next()) {
                code = res.getString("code");
                market = res.getString("market");
            } else {
                throw new Exception("hashcode " + hashcode + " not found");
            }
            res = stmt.executeQuery(sql);
            if (res.next()) {
                String data = res.getString("quotes");
                String b1 = "{", b2 = "}";
                int k1 = 0, k2;
                while (true) {
                    k1 = data.indexOf(b1, k1);
                    k2 = data.indexOf(b2, k1);
                    if (k1 < 0 || k2 < 0) {
                        break;
                    }
                    String s = data.substring(k1 + 1, k2);
                    //LOG.debug(s);
                    int k3 = s.indexOf("PREZZO"), k4 = s.indexOf("VARIAZIONE");
                    String prezzo = s.substring(k3 + 7, k4 - 2).trim();
                    k3 = s.indexOf("VOLUME");
                    k4 = s.indexOf("PREZZO");
                    String volume = s.substring(k3 + 7, k4 - 2).trim();
                    String ora = s.substring(s.indexOf("ORA") + 4);
                    LocalDateTime datetime = LocalDateTime.parse(ora, DateTimeFormatter.ofPattern("dd/MM/yy H.m.s"));
                    Calendar c = Calendar.getInstance();
                    c.set(datetime.getYear(), datetime.getMonthValue() - 1, datetime.getDayOfMonth(), datetime.getHour(), datetime.getMinute(), datetime.getSecond());
                    c.set(Calendar.MILLISECOND, 0);
                    UDate d = new UDate(c.getTimeInMillis());
                    if (mapvolume.containsKey(d)) {
                        mapvolume.replace(d, mapvolume.get(d) + nf.parse(volume).doubleValue());
                    } else {
                        mapvolume.put(d, nf.parse(volume).doubleValue());
                    }
                    double t1 = nf.parse(prezzo).doubleValue();
                    if (maphigh.containsKey(d)) {
                        if (t1 > maphigh.get(d)) {
                            maphigh.replace(d, t1);
                        }
                    } else {
                        maphigh.put(d, t1);
                    }
                    if (maplow.containsKey(d)) {
                        if (t1 < maplow.get(d)) {
                            maplow.replace(d, t1);
                        }
                    } else {
                        maplow.put(d, t1);
                    }
                    if (!mapclose.containsKey(d)) {
                        mapclose.put(d, t1);
                    }
                    if (mapopen.containsKey(d)) {
                        mapopen.replace(d, t1);
                    } else {
                        mapopen.put(d, t1);
                    }
                    k1 = k2;
                }

                dates.addAll(mapvolume.keySet());
                names.add("OPEN(" + code + "." + market + ")");
                names.add("HIGH(" + code + "." + market + ")");
                names.add("LOW(" + code + "." + market + ")");
                names.add("CLOSE(" + code + "." + market + ")");
                names.add("VOLUME(" + code + "." + market + ")");
                names.add("OI(" + code + "." + market + ")");
                matrix = new double[dates.size()][6];
                for (int i = 0; i < matrix.length; i++) {
                    matrix[i][0] = mapopen.get(dates.get(i));
                    matrix[i][1] = maphigh.get(dates.get(i));
                    matrix[i][2] = maplow.get(dates.get(i));
                    matrix[i][3] = mapclose.get(dates.get(i));
                    matrix[i][4] = mapvolume.get(dates.get(i));
                    matrix[i][5] = 0;
                }
            } else {
                throw new Exception("cannot fetch " + hashcode);
            }
        } catch (Exception e) {
            //LOG.error("cannot fetch "+isin, e);
            throw e;
        } finally {
            try {
                if (res != null) {
                    res.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }
        return new Fints(dates, names, Fints.frequency.SECOND, matrix);
    }
     */
    public static void fetchEODquotesST() throws Exception {
        /*
                /**
         * .PA - Paris
            .MI - Milano
            .DE - XETRA
            .F - Frankfurt
            .AS - Amsterdam
            .LS - Lisbon
            .MA - Madrid
            * 
            * 
            * EPA: - Paris
            BIT: - Milan
            ETR: - XETRA
            FRA: - Frankfurt
            AMS: - Amsterdam
            ELI: - Lisbon
            BME: - Madrid
            * EBR - brussels
         */
        //NB GOOGLEQUOTES DOESN'T WORK!!!!!!!!!!!!!!!!!!!!!!!!!!!

        String sql = "insert or replace into eoddata values(?,?,?);";
        Connection conn = DriverManager.getConnection(Init.db_url);
        java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
        List<HashMap<String, String>> list = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        try {

            for (java.util.HashMap<String, String> s : list) {
                String market = s.get("market");
                String code = s.get("code");
                String type = s.get("type");
                String hashcode = s.get("hashcode");
                java.util.HashMap<String, String> map = new java.util.HashMap<>();
                //  Teoddata t1 = new Teoddata(stmt,s.get("hashcode"),s.get("code"), s.get("market"), s.get("type"));
                try {
                    if (market.toUpperCase().contains("MLSE") && type.toUpperCase().contains("STOCK")) {
                        //   map.put("googlequotes", Database.getGoogleQuotes("BIT:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".MI"));
                    } else if (market.toUpperCase().contains("MLSE")) {
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".MI"));
                    } else if (market.toUpperCase().contains("NYSE")) {
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code));
                    } else if (market.toUpperCase().contains("XETRA") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("ETR:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".DE"));
                    } else if (market.toUpperCase().contains("EURONEXT-XMLI") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("EPA:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".PA"));
                    } else if (market.toUpperCase().contains("EURONEXT-XBRU") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("EBR:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".BR"));
                    } else if (market.toUpperCase().contains("EURONEXT-XPAR") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("EPA:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".PA"));
                    } else if (market.toUpperCase().contains("EURONEXT-XLIS") && type.toUpperCase().contains("STOCK")) {
                        //  map.put("googlequotes", Database.getGoogleQuotes("ELI:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".LS"));
                    } else if (market.toUpperCase().contains("EURONEXT-ALXP") && type.toUpperCase().contains("STOCK")) {
                        //  map.put("googlequotes", Database.getGoogleQuotes("EPA:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".PA"));
                    } else if (market.toUpperCase().contains("EURONEXT-VPXB") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("AMS:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".AS"));
                    } else if (market.toUpperCase().contains("EURONEXT-XAMS") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("AMS:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".AS"));
                    } else if (market.toUpperCase().contains("EURONEXT-ENXL") && type.toUpperCase().contains("STOCK")) {
                        //  map.put("googlequotes", Database.getGoogleQuotes("ELI:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".LS"));
                    } else if (market.toUpperCase().contains("EURONEXT-MLXB") && type.toUpperCase().contains("STOCK")) {
                        //  map.put("googlequotes", Database.getGoogleQuotes("EBR:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".BR"));
                    } else if (market.toUpperCase().contains("EURONEXT-ALXL") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("ELI:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".LS"));
                    } else if (market.toUpperCase().contains("EURONEXT-TNLB") && type.toUpperCase().contains("STOCK")) {
                        //  map.put("googlequotes", Database.getGoogleQuotes("EBR:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".BR"));
                    } else if (market.toUpperCase().contains("EURONEXT-ALXB") && type.toUpperCase().contains("STOCK")) {
                        // map.put("googlequotes", Database.getGoogleQuotes("EBR:"+code ));
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".BR"));
                    } else if (market.toUpperCase().contains("EURONEXT-AYP") && type.toUpperCase().contains("STOCK")) {
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".IR"));
                    } else if (market.toUpperCase().contains("EURONEXT-A5G") && type.toUpperCase().contains("STOCK")) {
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".IR"));
                    } else if (market.toUpperCase().contains("EURONEXT-ALXP") && type.toUpperCase().contains("STOCK")) {
                        map.put("yahooquotes", FetchData.fetchYahooQuotes(code + ".PA"));
                    } else {
                        throw new Exception("unknown market/type " + market + "\t" + type);
                    }

                    if (map.containsKey("yahooquotes")) {
                        if (map.get("yahooquotes").isEmpty()) {
                            stmt.setNull(2, java.sql.Types.VARCHAR);
                        } else {
                            stmt.setString(2, map.get("yahooquotes"));
                        }
                    } else {
                        stmt.setNull(2, java.sql.Types.VARCHAR);
                    }

                    if (map.containsKey("googlequotes")) {
                        if (map.get("googlequotes").isEmpty()) {
                            stmt.setNull(3, java.sql.Types.VARCHAR);
                        } else {
                            stmt.setString(3, map.get("googlequotes"));
                        }
                    }
                    stmt.setNull(3, java.sql.Types.VARCHAR);

                    stmt.setString(1, hashcode);
                    stmt.executeUpdate();

                } catch (Exception e) {
                    LOG.warn(e);
                }

            }
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
            }
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
            }
        }

    }

}
