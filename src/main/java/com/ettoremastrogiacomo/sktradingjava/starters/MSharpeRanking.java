
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package com.ettoremastrogiacomo.sktradingjava.starters;
 

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;
 
/**
*
* @author ettore
*/
public class MSharpeRanking {
 
    static Logger logger = Logger.getLogger(MSharpeRanking.class);
 
    public static void main(String[] args) throws Exception {

/*

        
        java.util.ArrayList<String> syms = com.ettoremastrogiacomo.sktradingjava.data.DBInterface.getSymbolListFilter(null, "MLSE", "EUR", null, null, null, null, "STOCK", null);
        java.util.ArrayList<String> newsyms = new java.util.ArrayList<>();
 
        Fints all_close =new Fints(),all_open=new Fints(),all_high=new Fints(),all_low=new Fints(),all_vol=new Fints();
        java.util.Calendar c = java.util.Calendar.getInstance();

        if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            c.add(Calendar.DATE, -2);
        } else {
            c.add(Calendar.DATE, -1);
        }

        UDate lastdate = new UDate(c.getTimeInMillis());
        for (int i = 0; i < syms.size(); i++) {
            Fints f = null;
            boolean err = false;
            try {
                f = DBInterface.getFints(syms.get(i), Fints.frequency.DAILY);
            } catch (Exception e) {
                err = true;
            }
            if (err) {
                continue;
            }
            if (f.getMaxDateGap()>(7*24*60*60*1000)) continue;
            if (f.getLastDate().before(lastdate)) {
                continue;
            }
            if (f.getLength() < 300) {
                continue;
            }
            newsyms.add(syms.get(i));
            if (newsyms.size() == 1) {
                all_close = f.getSerieCopy(3);
             
            } else {
                all_close = all_close.merge(f.getSerieCopy(3));

            }
        }
        java.util.HashMap<String,String> isinmap=new java.util.HashMap<>();
        java.util.HashMap<String,String> namemap=new java.util.HashMap<>();
        for (String s: newsyms) {
            java.util.HashMap<String, String> m=com.ettoremastrogiacomo.sktradingjava.data.DBInterface.getSymbolInfo(s);
            isinmap.put(s,m.get("isin") );
            namemap.put(s,m.get("name"));
        }
        
        
        Fints ER = Fints.ER(all_close, 1, false);
        Fints msharpe = Fints.SMA(Fints.Sharpe(ER, 20), 200);
        
        int len=msharpe.getLength();
        int seriescount=msharpe.getNoSeries();
        logger.info("Samples = "+msharpe.getLength() + "\tSeries= " + msharpe.getNoSeries());
        System.out.println("firstdate="+msharpe.getDate(0)+"\t"+"lastdate="+msharpe.getDate(len-1));

            java.util.SortedMap<Double, String> sortedMap = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
            java.util.TreeMap<Double, String> topmap = new java.util.TreeMap<>();
 
            for (int i = 0; i < seriescount; i++) {
                topmap.put(msharpe.get(len-1, i), msharpe.getName(i));
            }
            for (int i = 0; i < seriescount; i++) {
                sortedMap.put(msharpe.get(len-1, i), newsyms.get(i));
            }
            int k = 0;
            
            for (Double d : sortedMap.keySet()) {
                if (!d.isNaN() ) {
                    System.out.println("NO " + (k + 1) + " ; " + topmap.get( d) + " ; value ; " +  d + " ; code ; " + sortedMap.get(d) + " ; isin ; " + isinmap.get(sortedMap.get( d)));
                    k++;
                }
 
            }*/
    }
}
 