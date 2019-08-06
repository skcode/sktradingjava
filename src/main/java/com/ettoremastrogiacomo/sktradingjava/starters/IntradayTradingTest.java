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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
        
        TreeSet<UDate> dates=new TreeSet<>(Misc.longestSet(Misc.timesegments(Database.getIntradayDates(), MAXGAP*24*60*60*1000)));
        ArrayList<String> list= new java.util.ArrayList<>();
        map.keySet().stream().filter((x) -> (map.get(x).containsAll(dates))).forEachOrdered((x) -> {
            if (isinok.contains(x))
            list.add(x);
        });
        LOG.debug(list.size());
        HashMap<String,TreeMap<UDate,Fints>> fmap= new HashMap<>();
        for (String x : list ){
            TreeMap<UDate,Fints> tm1= new TreeMap<>();
            dates.forEach(d-> { 
                try {
                    tm1.put(d, Database.getIntradayFintsQuotes(x, d));
                } catch (Exception e){}
            });
            double samples=0;
            for (UDate d: tm1.keySet()){
                samples+=tm1.get(d).getLength();
            }
            if ((samples/tm1.size())<10) continue;
            fmap.put(x, tm1);
        }
        LOG.debug("stocks size="+fmap.size()+"\tdatearraysize="+dates.size());
        HashMap<String,String> names=Database.getCodeMarketName(list);
        StringBuilder sb= new StringBuilder();
        sb.append("name;");
        dates.forEach(d->{sb.append(d.toYYYYMMDD()+";");});
        LOG.debug(fmap.size());
        for (String x: fmap.keySet()){
            sb.append("\n"+names.get(x)+";");
            fmap.get(x).keySet().forEach(d->{
                Fints t1=fmap.get(x).get(d);
                double dv=100*(t1.get(t1.getLength()-1, 3)-t1.get(0, 3))/t1.get(0, 3);
                sb.append(Double.toString(dv)+";");
            });
        }
        File file = new File("./closeopenpct.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        }                
    }
}
