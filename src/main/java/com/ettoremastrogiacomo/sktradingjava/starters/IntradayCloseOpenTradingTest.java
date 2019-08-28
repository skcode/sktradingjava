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
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;





/**
 *
 * @author root
 */
public class IntradayCloseOpenTradingTest {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayCloseOpenTradingTest.class);   
    public static java.util.TreeMap<Integer,java.util.AbstractMap.Entry<UDate,UDate>> continuousDates(long maxgapmsec) {
        java.util.TreeMap<Integer,java.util.AbstractMap.Entry<UDate,UDate>> map=new TreeMap<>();        
        return map;
    }
    public static void main(String[] args) throws Exception {
        int MAXGAP=7,MINSAMPLE=500;        
        int POOLSIZE=1;
        double LASTEQ=20000,FEE=7,spreadPEN=.001;
        double INITEQ=LASTEQ;
        
        TreeMap<UDate,Double> equity= new TreeMap<>();
        TreeSet<UDate> allIntradayDates=Database.getIntradayDates();
        java.util.TreeMap<UDate,ArrayList<String>> revmap=Database.getIntradayDatesReverseMap();
        java.util.HashMap<String,TreeSet<UDate>> map=Database.getIntradayDatesMap();        
        ArrayList<HashMap<String,String>> check=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.empty(), Optional.empty());
        HashSet<String> isinok=new HashSet<>();
        UDate lastdate=allIntradayDates.last();
        
        check.forEach((x)->{
            try {
            Fints f1=Database.getIntradayFintsQuotes(x.get("hashcode"), lastdate);
            if (f1.getLength()<MINSAMPLE) return;
            if (revmap.get(lastdate).contains(x.get("hashcode")))
                isinok.add(x.get("hashcode"));
            } catch (Exception e){}     });
        
        LOG.debug(isinok.size());
        ArrayList<String> list= new java.util.ArrayList<>(isinok);
        TreeSet<UDate> dates = new TreeSet<>();
        for (UDate d: allIntradayDates) {
            if (revmap.get(d).containsAll(list)) dates.add(d);
        }
        
        
        //Database.getIntradayDates().forEach((x)->{LOG.debug(x);});
        //TreeSet<UDate> dates=new TreeSet<>(Misc.longestSet(Misc.timesegments(Database.getIntradayDates(), MAXGAP*24*60*60*1000)));
         dates=new TreeSet<>(Misc.mostRecentTimeSegment(Database.getIntradayDates(), MAXGAP*24*60*60*1000));
        //datesl.forEach((x)->{LOG.debug(x);});
        dates.forEach((x)->{LOG.debug(x);});
        
        //map.keySet().stream().filter((x) -> (map.get(x).containsAll(dates))).forEachOrdered((x) -> {
          //  if (isinok.contains(x))
           // list.add(x);
        //});
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

        LOG.debug("MAXGAP="+MAXGAP+"\tMINSAMPLE="+MINSAMPLE+"\tpoolsize="+POOLSIZE);
        LOG.debug("stocks size="+fmap.size()+"\tdatearraysize="+dates.size());   
        double[] profit=new double[darr.length-1];
        double[] profit_train=new double[darr.length-1];
        for (int i=0;i<(darr.length-1);i++) {
            TreeMap<Double,String> comap=new TreeMap<>();            
            for (String x: fmap.keySet()) {
                Fints f1=fmap.get(x).get(darr[i]);
                comap.put(100.0*(f1.getLastValueInCol(3)-f1.get(0, 3))/f1.get(0, 3), x);
                //open.put(x,new AbstractMap.SimpleEntry<>(f1.getFirstDate(),f1.get(0, 3)) );
                //close.put(x,new AbstractMap.SimpleEntry<>(f1.getLastDate(),f1.getLastValueInCol(3)));
            }
            TreeMap<Double, String> headmap=comap.entrySet().stream().limit(POOLSIZE).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            TreeMap<Double, String> tailmap=comap.descendingMap().entrySet().stream().limit(POOLSIZE).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);

            LOG.debug("********* DAY "+darr[i].toYYYYMMDD()+" *********");
            double mh=0,mt=0;
            for (String x: headmap.values()){
                Fints f1=fmap.get(x).get(darr[i]);
                double pct=100.0*(f1.getLastValueInCol(3)-f1.getFirstValueInCol(3))/f1.getFirstValueInCol(3);
                mh+=pct;
                LOG.debug("head map: "+names.get(x)+"\topen="+f1.getFirstDate()+";"+ f1.getFirstValueInCol(3)+"\tclose="+f1.getLastDate()+";"+f1.getLastValueInCol(3)+"\tchange%="+pct+"\tvolume="+f1.getSums()[4]);
            }

            for (String x: tailmap.values()){
                Fints f1=fmap.get(x).get(darr[i]);
                double pct=100.0*(f1.getLastValueInCol(3)-f1.getFirstValueInCol(3))/f1.getFirstValueInCol(3);
                mt+=pct;
                LOG.debug("tail map: "+names.get(x)+"\topen="+f1.getFirstDate()+";"+ f1.getFirstValueInCol(3)+"\tclose="+f1.getLastDate()+";"+f1.getLastValueInCol(3)+"\tchange%="+pct+"\tvolume="+f1.getSums()[4]);
            }                        
            LOG.debug("tot train head: "+mh+"\t"+mh/(POOLSIZE));
            LOG.debug("tot train tail: "+mt+"\t"+mt/(POOLSIZE));
            LOG.debug("mean train : "+(mt-mh)/(POOLSIZE*2));
            LOG.debug("TEST at date "+darr[i+1]);
            double mh_forw=0;                        
            double mt_forw=0;
            for (String x: headmap.values()) {                
                Fints f1=fmap.get(x).get(darr[i+1]);                
                double pct=100.0*(f1.getLastValueInCol(3)-f1.getFirstValueInCol(3))/f1.getFirstValueInCol(3);
                mh_forw+=pct;
                LOG.debug("head map FWD: "+names.get(x)+"\topen="+f1.getFirstDate()+";"+ f1.getFirstValueInCol(3)+"\tclose="+f1.getLastDate()+";"+f1.getLastValueInCol(3)+"\tchange%="+pct+"\tvolume="+f1.getSums()[4]);                
            }
            for (String x: tailmap.values()) {                
                Fints f1=fmap.get(x).get(darr[i+1]);                
                double pct=100.0*(f1.getLastValueInCol(3)-f1.getFirstValueInCol(3))/f1.getFirstValueInCol(3);
                mt_forw+=pct;
                LOG.debug("tail map FWD: "+names.get(x)+"\topen="+f1.getFirstDate()+";"+ f1.getFirstValueInCol(3)+"\tclose="+f1.getLastDate()+";"+f1.getLastValueInCol(3)+"\tchange%="+pct+"\tvolume="+f1.getSums()[4]);                
            }
            

            
            profit[i]=(mt_forw-mh_forw)/(POOLSIZE*2); 
            profit_train[i]=(mt-mh)/(POOLSIZE*2); 
            LOG.debug("tot test head: "+mh_forw+"\t"+mh_forw/(POOLSIZE));
            LOG.debug("tot tes tail: "+mt_forw+"\t"+mt_forw/(POOLSIZE));
            LOG.debug("mean test : "+profit[i]);

            
        }
        equity.put(darr[0], LASTEQ);        
        for (int i=0;i<profit.length;i++) {
                LASTEQ=LASTEQ*(1+profit[i]/100)-FEE*POOLSIZE*4;
                LASTEQ=LASTEQ*(1-spreadPEN);
                equity.put(darr[i+1], LASTEQ);        
        }
        Fints eq= new Fints(equity,Arrays.asList("equity"),Fints.frequency.DAILY);
        eq=eq.merge(eq.getLinReg(0));
        HashMap<String,Double> stats=DoubleArray.LinearRegression(eq.getCol(0));
        LOG.debug("*******");
        LOG.debug("MAXGAP="+MAXGAP+"\tMINSAMPLE="+MINSAMPLE+"\tPOOLSIZE="+POOLSIZE);
        LOG.debug("stocks size="+fmap.size()+"\tdatearraysize="+dates.size());   
        LOG.debug("TOT TRADES PROFIT="+DoubleArray.sum(profit));
        LOG.debug("MEAN TRADES PROFIT="+DoubleArray.mean(profit));
        LOG.debug("STD TRADES PROFIT="+DoubleArray.std(profit));
        LOG.debug("SHARPE TRADES PROFIT="+DoubleArray.mean(profit)/DoubleArray.std(profit));
        LOG.debug("MINDD EQUITY="+eq.getMaxDD(0));
        LOG.debug("SLOPE REG EQUITY="+stats.get("slope"));
        LOG.debug("STDERR REG EQUITY="+stats.get("stderr"));
        LOG.debug("NET PROFIT EQUITY="+eq.getLastValueInCol(0));        
        LOG.debug("CORR TRAIN TEST="+DoubleArray.corr(profit, profit_train));
        LOG.debug("NET PROFIT%="+(eq.getLastValueInCol(0)/INITEQ-1)*100+"%");
        LOG.debug("NET PROFIT% x day="+(eq.getLastValueInCol(0)/INITEQ-1)*100/dates.size()+"%");
        eq.plot("equity", "profit");
    }
}
