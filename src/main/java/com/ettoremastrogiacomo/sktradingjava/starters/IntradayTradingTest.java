/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;





/**
 *
 * @author root
 */
public class IntradayTradingTest {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayTradingTest.class);   
    public static java.util.TreeMap<Integer,java.util.AbstractMap.Entry<UDate,UDate>> continuousDates(long maxgapmsec) {
        java.util.TreeMap<Integer,java.util.AbstractMap.Entry<UDate,UDate>> map=new TreeMap<>();
        
        return map;
    }
    public static void main(String[] args) throws Exception {
        int TRAINW=20,TESTW=10,MAXGAP=5;        
        java.util.HashMap<String,TreeSet<UDate>> map=Database.intradayDates();        
        ArrayList<HashMap<String,String>> check=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.empty(), Optional.empty());
        HashSet<String> isinok=new HashSet<>();
        check.forEach((x)->{
            isinok.add(x.get("hashcode"));
        });
        LOG.debug(isinok.size());
        TreeSet<UDate> dates=new TreeSet<>(Misc.longestSet(Misc.timesegments(Database.getIntradayDates(), MAXGAP*24*60*60*1000)));
        ArrayList<String> list= new java.util.ArrayList<>();
        map.keySet().stream().filter((x) -> (map.get(x).containsAll(dates))).forEachOrdered((x) -> {
            if (isinok.contains(x))
            list.add(x);
        });
        LOG.debug("stocks size="+list.size()+"\tdatearraysize="+dates.size());
        HashMap<String,String> names=Database.getCodeMarketName(list);
        TreeMap<Double,String> corrmap= new TreeMap<>();
        TreeMap<Double,String> posmap= new TreeMap<>();
        TreeMap<Double,String> samplesmap= new TreeMap<>();
        
        
        Fints fall=new Fints();
        for (String x : list) {
            java.util.TreeMap<UDate,Double> m1=new TreeMap<>();
            double pos=0;
            double samples4d=0;
            for (UDate d: dates){
                Fints t1=Database.getIntradayFintsQuotes(x, d);
                double dv=100*(t1.get(t1.getLength()-1, 3)-t1.get(0, 3))/t1.get(0, 3);
                m1.put(d, dv);
                if (dv>=0) pos++;
                samples4d+=t1.getLength();
            }
            Fints f= new Fints(m1, Arrays.asList(names.get(x).substring(0, names.get(x).indexOf("."))), Fints.frequency.DAILY);
            fall=fall.isEmpty()?f:fall.merge(f);
            Double[] d1=m1.values().toArray(new Double[m1.size()]);
            double[] v1=new double[m1.size()-1],v2=new double[m1.size()-1];
            for (int i=1;i<d1.length;i++){
                v1[i-1]=d1[i];v2[i-1]=d1[i-1];
            }
            
            corrmap.put(DoubleArray.corr(v1, v2),names.get(x));            
            posmap.put(100*pos/(dates.size()),names.get(x));                 
            samplesmap.put(samples4d/dates.size(), names.get(x));
        }
        corrmap.keySet().forEach((x)->{
            LOG.debug("Corr="+x+"\t"+corrmap.get(x));        
        });
        posmap.keySet().forEach((x)->{
            LOG.debug("Positive ex%="+x+"\t"+posmap.get(x));        
        });
        samplesmap.keySet().forEach((x)->{
            LOG.debug("Mean samples%="+x+"\t"+samplesmap.get(x));        
        });

        
        fall.plot("ranges", "%");
    }
}
