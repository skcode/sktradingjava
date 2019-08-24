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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
        int MAXGAP=5,MINSAMPLE=50;        
        int lookf=1,poolsize=1;
        
        java.util.HashMap<String,TreeSet<UDate>> map=Database.intradayDates();        
        ArrayList<HashMap<String,String>> check=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.empty(), Optional.empty());
        HashSet<String> isinok=new HashSet<>();
        check.forEach((x)->{
            isinok.add(x.get("hashcode"));
        });
        
        //TreeSet<UDate> dates=new TreeSet<>(Misc.longestSet(Misc.timesegments(Database.getIntradayDates(), MAXGAP*24*60*60*1000)));
        TreeSet<UDate> dates=new TreeSet<>(Misc.mostRecentTimeSegment(Database.getIntradayDates(), MAXGAP*24*60*60*1000));
        dates.forEach((x)->{LOG.debug(x);});
        ArrayList<String> list= new java.util.ArrayList<>();
        map.keySet().stream().filter((x) -> (map.get(x).containsAll(dates))).forEachOrdered((x) -> {
            if (isinok.contains(x))
            list.add(x);
        });
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
            if ((samples/tm1.size())<MINSAMPLE) continue;
            fmap.put(x, tm1);
        }
        
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
        
        UDate []darr=dates.toArray(new UDate[dates.size()]);
        double mean_ret=0,tot_ret=0;
        LOG.debug("MAXGAP="+MAXGAP+"\tMINSAMPLE="+MINSAMPLE+"\tlookforward="+lookf+"\tpoolsize="+poolsize);
        LOG.debug("stocks size="+fmap.size()+"\tdatearraysize="+dates.size());   
        double[] profit=new double[darr.length-lookf];
        for (int i=0;i<(darr.length-lookf);i++) {
            TreeMap<Double,String> comap=new TreeMap<>();
            for (String x: fmap.keySet()) {
                Fints f1=fmap.get(x).get(darr[i]);
                comap.put(100.0*(f1.getLastValueInCol(3)-f1.get(0, 3))/f1.get(0, 3), x);
            }
            TreeMap<Double, String> headmap=comap.entrySet().stream().limit(poolsize).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            TreeMap<Double, String> tailmap=comap.descendingMap().entrySet().stream().limit(poolsize).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);

            LOG.debug("********* DAY "+darr[i].toYYYYMMDD()+" *********");
            headmap.keySet().forEach((x)->{LOG.debug("head map: "+names.get(headmap.get(x))+"\t"+x);});
            tailmap.keySet().forEach((x)->{LOG.debug("tail map: "+names.get(tailmap.get(x))+"\t"+x);});
            double mh=0,mt=0;
            for (Double x:headmap.keySet()) mh+=x;mh/=headmap.size();
            for (Double x:tailmap.keySet()) mt+=x;mt/=tailmap.size();
            LOG.debug("mean train head: "+mh);
            LOG.debug("mean train tail: "+mt);
            double mh_forw=0;            
            for (String x: headmap.values()) {                
                for (int j=i+1;j<=(i+lookf);j++){
                    Fints f1=fmap.get(x).get(darr[j]);
                    mh_forw+=100.0*(f1.getLastValueInCol(3)-f1.get(0, 3))/f1.get(0, 3);
                }
                mh_forw/=lookf;                
            }
            mh_forw/=headmap.size();
            double mt_forw=0;
            for (String x: tailmap.values()) {                
                for (int j=i+1;j<=(i+lookf);j++){
                    Fints f1=fmap.get(x).get(darr[j]);
                    mt_forw+=100.0*(f1.getLastValueInCol(3)-f1.get(0, 3))/f1.get(0, 3);
                }
                mt_forw/=lookf;                
            }
            mt_forw/=tailmap.size();
            LOG.debug(darr[i]+"\tmh="+mh+"\tmt="+mt);
            LOG.debug("mh_forw="+mh_forw+"\tmt_forw="+mt_forw+"\tDIFF="+(mt_forw-mh_forw));           
            tot_ret+=mt_forw-mh_forw;
            profit[i]=mt_forw-mh_forw;
        }
        LOG.debug("*******");
        LOG.debug("MAXGAP="+MAXGAP+"\tMINSAMPLE="+MINSAMPLE+"\tlookforward="+lookf+"\tpoolsize="+poolsize);
        LOG.debug("stocks size="+fmap.size()+"\tdatearraysize="+dates.size());   
        LOG.debug("TOT PROFIT="+DoubleArray.sum(profit));
        LOG.debug("MEAN PROFIT="+DoubleArray.mean(profit));
        LOG.debug("STD PROFIT="+DoubleArray.std(profit));
        LOG.debug("SHARPE PROFIT="+DoubleArray.mean(profit)/DoubleArray.std(profit));
    }
}
