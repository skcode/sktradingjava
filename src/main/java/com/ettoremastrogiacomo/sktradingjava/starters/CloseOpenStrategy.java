/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 *
 * @author ettore
 */
public class CloseOpenStrategy {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CloseOpenStrategy.class);   
    static final int MINLEN=500,MAXOLD=5,MINVOL=10000,MAXDAYGAP=6;
    static final double MAXGAP=.1;
    public static void main(String[] args) throws Exception {
        Portfolio ptf=Portfolio.createMLSEStockEURPortfolio(Optional.of(MINLEN), Optional.of(MAXGAP), Optional.of(MAXDAYGAP), Optional.of(MAXOLD), Optional.of(MINVOL));
        LOG.debug(ptf);
        LOG.debug(ptf.closeER.getMaxDaysDateGap());
        Fints close=ptf.getClose(),open=ptf.getOpen();
        Fints f1=Fints.Diff(close, open);
        Fints closeOpen=Fints.DIV(f1, open);
        closeOpen.toCSV("/tmp/t.csv");
        UDate []darr=closeOpen.getDate().toArray(new UDate[closeOpen.getLength()]);
        double[] profit=new double[darr.length-1];
        int POOLSIZE=5;
        double LASTEQ=100000,FEE=7,spreadPEN=0.001;
        TreeMap<UDate,Double> equity= new TreeMap<>();
        for (int i=0;i<(darr.length-1);i++) {
            TreeMap<Double,Integer> comap=new TreeMap<>();
            for (int j=0;j<closeOpen.getNoSeries();j++) {
                comap.put(closeOpen.get(i, j), j);
            }
            
            TreeMap<Double, Integer> headmap=comap.entrySet().stream().limit(POOLSIZE).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            TreeMap<Double, Integer> tailmap=comap.descendingMap().entrySet().stream().limit(POOLSIZE).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);

            LOG.debug("********* DAY "+darr[i].toYYYYMMDD()+" *********");
            headmap.keySet().forEach((x)->{LOG.debug("head map: "+ptf.getName(ptf.unmodifiable_hashcodes.get(headmap.get(x))) +"\t"+x);});
            tailmap.keySet().forEach((x)->{LOG.debug("tail map: "+ptf.getName(ptf.unmodifiable_hashcodes.get(tailmap.get(x)))+"\t"+x);});
            double mh=0,mt=0;
            for (Double x:headmap.keySet()) mh+=x;mh/=headmap.size();
            for (Double x:tailmap.keySet()) mt+=x;mt/=tailmap.size();
            LOG.debug("mean train head: "+mh);
            LOG.debug("mean train tail: "+mt);
            double mh_forw=0;            
            for (Integer x: headmap.values()) {  
                mh_forw+=closeOpen.get(i+1, x);
            }
            mh_forw/=headmap.size();
            double mt_forw=0;
            for (Integer x: tailmap.values()) {                
                mt_forw+=closeOpen.get(i+1, x);
            }
            mt_forw/=tailmap.size();
            LOG.debug(darr[i]+"\tmh="+mh+"\tmt="+mt);
            LOG.debug("mh_forw="+mh_forw+"\tmt_forw="+mt_forw+"\tDIFF="+(mt_forw-mh_forw));           
            profit[i]=mt_forw-mh_forw;            
        }
        
        for (int i=0;i<profit.length;i++) {
                LASTEQ=LASTEQ*(1+profit[i])-FEE*POOLSIZE*4;
                LASTEQ=LASTEQ*(1-spreadPEN);
                equity.put(darr[i], LASTEQ);        
        }
        Fints eq= new Fints(equity,Arrays.asList("equity"),Fints.frequency.DAILY);
        eq=eq.merge(eq.getLinReg(0));
        HashMap<String,Double> stats=DoubleArray.LinearRegression(eq.getCol(0));
        LOG.debug("*******");
        LOG.debug("MAXGAP="+MAXGAP+"\tpoolsize="+POOLSIZE);
        LOG.debug("stocks size="+closeOpen.getNoSeries()+"\tdatearraysize="+closeOpen.getLength());   
        LOG.debug("TOT TRADES PROFIT="+DoubleArray.sum(profit));
        LOG.debug("MEAN TRADES PROFIT="+DoubleArray.mean(profit));
        LOG.debug("STD TRADES PROFIT="+DoubleArray.std(profit));
        LOG.debug("SHARPE TRADES PROFIT="+DoubleArray.mean(profit)/DoubleArray.std(profit));
        LOG.debug("MINDD EQUITY="+eq.getMaxDD(0));
        LOG.debug("SLOPE REG EQUITY="+stats.get("slope"));
        LOG.debug("STDERR REG EQUITY="+stats.get("stderr"));
        LOG.debug("NET PROFIT EQUITY="+eq.getLastValueInCol(0));        
        eq.plot("equity", "profit");

        //ptf.securities.forEach((x)-> LOG.debug(x.getName())); 
    }
}
