/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author ettore
 */
public class IntradayStats {
        static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayStats.class);

    public static void main(String[] args) throws Exception {
        TreeSet<UDate> dates=Misc.mostRecentTimeSegment(Database.getIntradayDates(), 1000*60*60*24*5);        
        
        HashMap<String,TreeSet<UDate>> map=Database.getIntradayDatesMap();
        TreeMap<UDate,ArrayList<String>> rmap=Database.getIntradayDatesReverseMap();
        StringBuilder sb= new StringBuilder();        
        HashMap<String,String> m=Database.getCodeMarketName(new ArrayList<> (map.keySet()));
        for (String x: map.keySet()) {
            if  (!m.get(x).contains("STOCK") || !m.get(x).contains("MLSE") ) continue;            
            if (!map.get(x).containsAll(dates)) continue;
            for (UDate d: dates) {                
                if (!d.equals((UDate)dates.last())) continue;
                Fints f=Database.getIntradayFintsQuotes(x, d);
                if (f.getLength()<200) continue;
                Fints f1=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTE);
                Fints f1er=Fints.ER(f1.getSerieCopy(3),100,true);
                double f1corr=Fints.merge(f1er, Fints.Lag(f1er, 1)).getCorrelation()[0][1];
                
                Fints f3=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES3);
                Fints f3er=Fints.ER(f3.getSerieCopy(3),100,true);
                double f3corr=Fints.merge(f3er, Fints.Lag(f3er, 1)).getCorrelation()[0][1];

                Fints f5=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES5);
                Fints f5er=Fints.ER(f5.getSerieCopy(3),100,true);
                double f5corr=Fints.merge(f5er, Fints.Lag(f5er, 1)).getCorrelation()[0][1];
                
                
                Fints f10=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES10);
                Fints f10er=Fints.ER(f10.getSerieCopy(3),100,true);
                double f10corr=Fints.merge(f10er, Fints.Lag(f10er, 1)).getCorrelation()[0][1];

                Fints f15=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES15);
                Fints f15er=Fints.ER(f15.getSerieCopy(3),100,true);
                double f15corr=Fints.merge(f15er, Fints.Lag(f15er, 1)).getCorrelation()[0][1];
                double limit=.5;
                if (Math.abs(f1corr)>limit) LOG.info("1 min:"+f1corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());
                if (Math.abs(f3corr)>limit) LOG.info("3 min:"+f3corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());
                if (Math.abs(f5corr)>limit) LOG.info("5 min:"+f5corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());
                if (Math.abs(f10corr)>limit) {LOG.info("10 min:"+f10corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD()); f10.getSerieCopy(3).plot("price", "time");}
                if (Math.abs(f15corr)>limit) LOG.info("15 min:"+f15corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());
            }
            
        }            
    }
}
