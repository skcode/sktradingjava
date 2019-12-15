/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 *
 * @author ettore
 */
public class IntradayStats {
        static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayStats.class);

    public static void checkp(String hash, UDate d,Fints.frequency freq) throws Exception{
        TreeMap<UDate,Double> eq=new TreeMap<>();
        double v=1;
        
        Fints f=Database.getIntradayFintsQuotes(hash, d);
        Fints fs=Security.changeFreq(f, freq).getSerieCopy(3);
        eq.put( fs.getDate(0),v);
        for (int i=1;i<(fs.getLength()-1);i++) {
            double d1=(fs.get(i,0)-fs.get(i-1,0))/fs.get(i-1,0);
            double d2=(fs.get(i+1,0)-fs.get(i,0))/fs.get(i,0);
            v=d1>0? v*(1-d2):v*(1+d2);//inverse corr
            eq.put( fs.getDate(i+1),v);        
        }
        Fints equity= new Fints(eq, Arrays.asList("equity"), freq);
        equity.plot("title", "val");
        fs.plot("stock", "price");
    }
    
    public static void main(String[] args) throws Exception {
        
        String hash="xo8HLPAdBEEx1oYDsnOFyUoC99c=";
        checkp(hash, UDate.genDate(2019, 11, 13, 0, 0, 0), Fints.frequency.MINUTES5);
        //if (true) return;
        
        TreeSet<UDate> dates=Misc.mostRecentTimeSegment(Database.getIntradayDates(), 1000*60*60*24*5);        
        
        HashMap<String,TreeSet<UDate>> map=Database.getIntradayDatesMap();
        TreeMap<UDate,ArrayList<String>> rmap=Database.getIntradayDatesReverseMap();        
        HashMap<String,String> m=Database.getCodeMarketName(new ArrayList<> (map.keySet()));
        TreeSet<UDate> dates2=dates.stream().sorted(Comparator.reverseOrder()).limit(5).collect(Collectors.toCollection(TreeSet::new));
        TreeMap<Double,String> c1= new TreeMap<> ();
        TreeMap<Double,String> c3= new TreeMap<> ();
        TreeMap<Double,String> c5= new TreeMap<> ();
        TreeMap<Double,String> c10= new TreeMap<> ();
        TreeMap<Double,String> c15= new TreeMap<> ();
        TreeMap<Double,String> c30= new TreeMap<> ();
        TreeMap<Double,String> c60= new TreeMap<> ();
        for (String x: map.keySet()) {
            if  (!m.get(x).contains("STOCK") || !m.get(x).contains("MLSE") ) continue;            
            if (!map.get(x).containsAll(dates)) continue;
            double d1=0,d3=0,d5=0,d10=0,d15=0,d30=0,d60=0;
            for (UDate d: dates2) {                
                //if (!d.equals((UDate)dates.last())) continue;
                Fints f=Database.getIntradayFintsQuotes(x, d);
                if (f.getLength()<300) continue;
                
                Fints f1=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTE);
                Fints f1er=Fints.ER(f1.getSerieCopy(3),100,true);
                double f1corr=Fints.merge(f1er, Fints.Lag(f1er, 1)).getCorrelation()[0][1];                
                d1+=f1corr;
                
                Fints f3=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES3);
                Fints f3er=Fints.ER(f3.getSerieCopy(3),100,true);
                double f3corr=Fints.merge(f3er, Fints.Lag(f3er, 1)).getCorrelation()[0][1];
                d3+=f3corr;
                
                Fints f5=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES5);
                Fints f5er=Fints.ER(f5.getSerieCopy(3),100,true);
                double f5corr=Fints.merge(f5er, Fints.Lag(f5er, 1)).getCorrelation()[0][1];                
                d5+=f5corr;
                
                Fints f10=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES10);
                Fints f10er=Fints.ER(f10.getSerieCopy(3),100,true);
                double f10corr=Fints.merge(f10er, Fints.Lag(f10er, 1)).getCorrelation()[0][1];
                d10+=f10corr;
                
                Fints f15=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES15);
                Fints f15er=Fints.ER(f15.getSerieCopy(3),100,true);
                double f15corr=Fints.merge(f15er, Fints.Lag(f15er, 1)).getCorrelation()[0][1];
                d15+=f15corr;

                Fints f30=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.MINUTES30);
                Fints f30er=Fints.ER(f30.getSerieCopy(3),100,true);
                double f30corr=Fints.merge(f30er, Fints.Lag(f30er, 1)).getCorrelation()[0][1];
                d30+=f30corr;

                Fints f60=com.ettoremastrogiacomo.sktradingjava.Security.changeFreq(f, Fints.frequency.HOUR);
                Fints f60er=Fints.ER(f60.getSerieCopy(3),100,true);
                double f60corr=Fints.merge(f60er, Fints.Lag(f60er, 1)).getCorrelation()[0][1];
                d60+=f60corr;

                
                double limit=.4;
                /*if (Math.abs(f1corr)>limit) LOG.info("1 min:"+f1corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());
                if (Math.abs(f3corr)>limit) LOG.info("3 min:"+f3corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());
                if (Math.abs(f5corr)>limit) LOG.info("5 min:"+f5corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());
                if (Math.abs(f10corr)>limit) {LOG.info("10 min:"+f10corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD()); f10.getSerieCopy(3).plot("price", "time");}
                if (Math.abs(f15corr)>limit) {LOG.info("15 min:"+f15corr+"\t"+m.get(x)+"\t"+d.toYYYYMMDD());f15.getSerieCopy(3).plot("price", "time");}*/
            }
            c1.put(d1/dates2.size(), x+"."+m.get(x));
            c3.put(d3/dates2.size(), x+"."+m.get(x));
            c5.put(d5/dates2.size(), x+"."+m.get(x));
            c10.put(d10/dates2.size(), x+"."+m.get(x));
            c15.put(d15/dates2.size(), x+"."+m.get(x));            
            c30.put(d30/dates2.size(), x+"."+m.get(x));            
            c60.put(d60/dates2.size(), x+"."+m.get(x));            
        }            
        LOG.info(dates2.first()+" to "+dates2.last()+"\t"+dates2.size());
        LOG.info("C1:\n"+c1);
        LOG.info("C3:\n"+c3);
        LOG.info("C5:\n"+c5);
        LOG.info("C10:\n"+c10);
        LOG.info("C15:\n"+c15);
        LOG.info("C30:\n"+c30);
        LOG.info("C60:\n"+c60);
    }
}
