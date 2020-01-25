/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.intraday;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import static com.ettoremastrogiacomo.sktradingjava.starters.intraday.TestTrading.logger;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * @author a241448
 */

class CallableClass  implements Callable<CallableClass>{
    final Fints f;
    final int h,k;
    Fints eqres;
    static double INITCAP=10000,FIXEDFEE=7,VARFEE=0.001;
    
    public static void setParams(double initcap,double fixedfee,double varfee){
        INITCAP=initcap;FIXEDFEE=fixedfee;VARFEE=varfee;
    }
    List<Integer> getParams() {
        return Arrays.asList(h,k);
    }
    Fints getEquity() {
    return eqres;
    }
    public  CallableClass  (Fints f, int h,int k) throws Exception { 
        if (f.isEmpty() || f.getNoSeries()>1) throw new RuntimeException("only one serie allowed");
        this.f=f;
        this.k=k;
        this.h=h;
    }
    
    @Override
    public CallableClass call() throws Exception {
        Fints i1 = Fints.SMA(Fints.Sharpe(Fints.ER(f, 100, true), 10), h);
        Fints i2 = Fints.SMA(Fints.Sharpe(Fints.ER(f, 100, true), 10), k);
        Fints tot = i1.merge(i2).merge(f);
        TreeMap<UDate, Double> eq = new TreeMap<>();
        eq.put(tot.getFirstDate(), INITCAP);
        boolean flat=true;                    
        for (int j = 0; j < (tot.getLength() - 1); j++) {
            if (tot.get(j, 1) > tot.get(j, 0)) {
                double v = (tot.get(j + 1, 2) - tot.get(j, 2)) / tot.get(j, 2);
                if (flat) eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v)-FIXEDFEE);
                else eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v));
                flat=false;
            } else {
                if (!flat) eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j))-FIXEDFEE);
                else eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)));
                flat=true;
            }
        }                    
        eqres= new Fints(eq, Arrays.asList("equity-"+f.getName(0)+"-"+tot.getFirstDate().toYYYYMMDD()), tot.getFrequency());        
        return this;
        
    }

}
public class TestTrading {

    static Logger logger = org.apache.log4j.Logger.getLogger(TestTrading.class);

    public static void main(String[] args) throws Exception {
        final double VARFEE = .001, FIXEDFEE = 7, INITCAP = 60000, MINSAMPLES = 100;

        HashMap<String, TreeMap<UDate, Fints>> fintsmap = new HashMap<>();

        TreeSet<UDate> dates = Misc.mostRecentTimeSegment(Database.getIntradayDates(), 1000 * 60 * 60 * 24 * 5);

        HashMap<String, TreeSet<UDate>> map = Database.getIntradayDatesMap();
        HashMap<String, String> nmap = Database.getCodeMarketName(new ArrayList<>(map.keySet()));

        for (String x : map.keySet()) {
            if (map.get(x).containsAll(dates) && nmap.get(x).contains("ENEL.MLSE.STOCK")) {
                boolean toadd = true;
                TreeMap<UDate, Fints> t1 = new TreeMap<>();
                for (UDate d : dates) {
                    Fints f1 = Database.getIntradayFintsQuotes(x, d);
                    if (f1.getLength() < MINSAMPLES) {
                        toadd = false;
                        break;
                    } else {
                        t1.put(d, Security.changeFreq(Security.createContinuity(Database.getIntradayFintsQuotes(x, d)), Fints.frequency.MINUTE).getSerieCopy(3));
                        //t1.put(d, Security.createContinuity(Database.getIntradayFintsQuotes(x, d)).getSerieCopy(3));
                    }

                    // tmap1.put(d, Security.createContinuity(Security.changeFreq(Database.getIntradayFintsQuotes(x, d), Fints.frequency.MINUTE).getSerieCopy(3)));
                }
                if (toadd) {
                    logger.info("added " + nmap.get(x));
                    fintsmap.put(x, t1);
                }
            }
        }
        logger.info(fintsmap.size() + "\tof\t" + map.size());
        logger.info("time range: " + dates.first() + " -> " + dates.last() + "\tsamples=" + dates.size());
        CallableClass.setParams(INITCAP, FIXEDFEE, VARFEE);
        int POOL = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(POOL);
        List<Future<CallableClass>> list = new ArrayList<>();        
        for (String x : fintsmap.keySet()) {
            AbstractMap.SimpleEntry<ArrayList<Integer>, ArrayList<Double>> bestprofit = new AbstractMap.SimpleEntry(new ArrayList<Integer>(), new ArrayList<Double>());
            for (int k = 2; k <= 120; k++) 
                for (int h = 2; h <= 120; h++) 
            {
                ArrayList<Double> profit = new ArrayList<>();
                
                for (UDate d : dates) {
                    Fints f = fintsmap.get(x).get(d);
                    list.add(executor.submit(new CallableClass(f, h, k)));
                    
                    if (list.size()>=POOL || (d.equals(dates.last()) )) {
                        for (Future<CallableClass> z: list) {
                            Fints reseq=z.get().getEquity();                            
                                profit.add(reseq.getLastValueInCol(0));                          
                        }
                        list.clear();
                    }
/*                    
                    Fints i1 = Fints.SMA(Fints.Sharpe(Fints.ER(f, 100, true), 10), h);
                    Fints i2 = Fints.SMA(Fints.Sharpe(Fints.ER(f, 100, true), 10), k);
                    Fints tot = i1.merge(i2).merge(f);

                    TreeMap<UDate, Double> eq = new TreeMap<>();
                    eq.put(tot.getFirstDate(), INITCAP);
                    boolean flat=true;                    
                    for (int j = 0; j < (tot.getLength() - 1); j++) {
                        if (tot.get(j, 1) > tot.get(j, 0)) {
                            double v = (tot.get(j + 1, 2) - tot.get(j, 2)) / tot.get(j, 2);
                            if (flat) eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v)-FIXEDFEE);
                            else eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v));
                            flat=false;
                        } else {
                            if (!flat) eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j))-FIXEDFEE);
                            else eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)));
                            flat=true;
                        }
                    }                    
*/
                    // (new Fints(eq, Arrays.asList("equity"), tot.getFrequency())).plot(d.toYYYYMMDD(), "price");
                    // tot.plot("tot", "price");
                    
                    //logger.info("profit for day " + d + "\t" + eq.lastEntry().getValue());
                    //logger.info("mean " + profit.stream().mapToDouble(i -> i).summaryStatistics().getAverage());
                    //logger.info("max " + profit.stream().mapToDouble(i -> i).summaryStatistics().getMax());
                    //logger.info("min " + profit.stream().mapToDouble(i -> i).summaryStatistics().getMin());

                }
                
                double mean = profit.stream().mapToDouble(i -> i).summaryStatistics().getAverage();
                if (bestprofit.getKey().isEmpty()) {
                    bestprofit = new AbstractMap.SimpleEntry<>(new ArrayList<>(Arrays.asList(k,h)), profit);
                    logger.info("best profit at k,h="+bestprofit.getKey());
                    logger.info("mean " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getAverage());
                    logger.info("max " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMax());
                    logger.info("min " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMin());
                    
                } else {
                    double bpm = bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getAverage();
                    if (mean > bpm) {
                        bestprofit = new AbstractMap.SimpleEntry<>(new ArrayList<>(Arrays.asList(k,h)), profit);
                        logger.info("best profit at k,h="+bestprofit.getKey());
                        logger.info("mean " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getAverage());
                        logger.info("max " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMax());
                        logger.info("min " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMin());                        
                    }
                }
            }

        }
        executor.shutdown();  
        executor.awaitTermination(10, TimeUnit.MINUTES);
    }
}

