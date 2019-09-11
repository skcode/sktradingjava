/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;





/**
 *
 * @author root
 */
public class IntradayCloseOpenTradingTest2 {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayCloseOpenTradingTest2.class);   
    public static void main(String[] args) throws Exception {
        final int MAXGAP=5,MINSAMPLE=70;        
        int POOLSIZE=2;
        double LASTEQ=60000,FEE=7,spreadPEN=.001;
        double INITEQ=LASTEQ;
        
        TreeMap<UDate,Double> equity= new TreeMap<>();
        TreeSet<UDate> dates=new TreeSet<>();
        java.util.TreeMap<UDate,ArrayList<String>> revmap=Database.getIntradayDatesReverseMap();
        java.util.HashMap<String,TreeSet<UDate>> map=Database.getIntradayDatesMap();        
        revmap.keySet().forEach((x)->{if (revmap.get(x).size()>200) dates.add(x);});
        HashMap<String,String> names=Database.getCodeMarketName(new ArrayList<>(map.keySet()));        
        dates.forEach((x)->{LOG.debug(x);});

        
        final UDate []darr=dates.toArray(new UDate[dates.size()]);

        LOG.debug("MAXGAP="+MAXGAP+"\tMINSAMPLE="+MINSAMPLE+"\tpoolsize="+POOLSIZE);
        LOG.debug("datearraysize="+dates.size());   
        double[] profit=new double[darr.length-1];
        double[] profit_train=new double[darr.length-1];
        
        for (int i=0;i<(darr.length-1);i++) {            
            HashMap<String,Fints> fmap= new HashMap<>();
            HashMap<String,Fints> fmapfwd= new HashMap<>();
            for (String x : revmap.get(darr[i])) {
                
                if (names.get(x).contains("STOCK") && names.get(x).contains("MLSE")) {                    
                    if (!revmap.get(darr[i+1]).contains(x)) { LOG.warn("day ahed empty for stock "+names.get(x));continue;}
                    if (darr[i+1].diffdays(darr[i])>MAXGAP) {LOG.warn("day gap "+darr[i+1].diffdays(darr[i])); continue;}
                    Fints f1=Database.getIntradayFintsQuotes(x, darr[i]);
                    if (f1.getLength()>MINSAMPLE ) fmap.put(x, f1);else LOG.warn("too few samples for stock "+names.get(x));
                }                         
            }
            
            TreeMap<Double,String> comap=new TreeMap<>();   
            for (String x: fmap.keySet()) {
                Fints f1=fmap.get(x);
                comap.put(100.0*(f1.getLastValueInCol(3)-f1.get(0, 3))/f1.get(0, 3), x);
            }
            TreeMap<Double, String> headmap=comap.entrySet().stream().limit(POOLSIZE).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            TreeMap<Double, String> tailmap=comap.descendingMap().entrySet().stream().limit(POOLSIZE).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            LOG.debug("********* DAY "+darr[i].toYYYYMMDD()+" *********");
            double mh=0,mt=0;
            for (String x: headmap.values()){
                Fints f1=fmap.get(x);
                double pct=100.0*(f1.getLastValueInCol(3)-f1.getFirstValueInCol(3))/f1.getFirstValueInCol(3);
                mh+=pct;
                LOG.debug("head map: "+names.get(x)+"\topen="+f1.getFirstDate()+";"+ f1.getFirstValueInCol(3)+"\tclose="+f1.getLastDate()+";"+f1.getLastValueInCol(3)+"\tchange%="+pct+"\tvolume="+f1.getSums()[4]);
            }

            for (String x: tailmap.values()){
                Fints f1=fmap.get(x);
                double pct=100.0*(f1.getLastValueInCol(3)-f1.getFirstValueInCol(3))/f1.getFirstValueInCol(3);
                mt+=pct;
                LOG.debug("tail map: "+names.get(x)+"\topen="+f1.getFirstDate()+";"+ f1.getFirstValueInCol(3)+"\tclose="+f1.getLastDate()+";"+f1.getLastValueInCol(3)+"\tchange%="+pct+"\tvolume="+f1.getSums()[4]);
            }                        
            LOG.debug("tot train head: "+mh+"\t"+mh/(POOLSIZE));
            LOG.debug("tot train tail: "+mt+"\t"+mt/(POOLSIZE));
            LOG.debug("mean train : "+(mt-mh)/(POOLSIZE*2));
            LOG.debug("TEST at date "+darr[i+1]);
            for (String x : fmap.keySet()) {
                    Fints f1=Database.getIntradayFintsQuotes(x, darr[i+1]);
                    fmapfwd.put(x, f1);            
            }
            
            double mh_forw=0;                        
            double mt_forw=0;
            for (String x: headmap.values()) {                
                Fints f1=fmapfwd.get(x);                
                double pct=100.0*(f1.getLastValueInCol(3)-f1.getFirstValueInCol(3))/f1.getFirstValueInCol(3);
                mh_forw+=pct;
                LOG.debug("head map FWD: "+names.get(x)+"\topen="+f1.getFirstDate()+";"+ f1.getFirstValueInCol(3)+"\tclose="+f1.getLastDate()+";"+f1.getLastValueInCol(3)+"\tchange%="+pct+"\tvolume="+f1.getSums()[4]);                
            }
            for (String x: tailmap.values()) {                
                Fints f1=fmapfwd.get(x);                
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
        LOG.debug("datearraysize="+dates.size());   
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
