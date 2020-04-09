/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.intraday;

import com.ettoremastrogiacomo.sktradingjava.Charts;
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
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;

/**
 *
 * @author a241448
 */
class CallableClass implements Callable<CallableClass> {

    private final Fints f;
    private final int h, k;
    private Fints eqres, tot;
    private int trades = 0;
    static double INITCAP = 10000, FIXEDFEE = 7, VARFEE = 0.001;

    public static void setParams(double initcap, double fixedfee, double varfee) {
        INITCAP = initcap;
        FIXEDFEE = fixedfee;
        VARFEE = varfee;
    }

    List<Integer> getParams() {
        return Arrays.asList(h, k);
    }

    Fints getEquity() {
        return eqres;
    }

    int getTrades() {
        return trades;
    }

    void plotTot() throws Exception {
        Charts c = new Charts("stock");
        XYPlot p2 = c.createXYPlot(tot.getName(2), tot.getSerieCopy(2));
        XYPlot p1 = c.createXYPlot(tot.getName(1), tot.getSerieCopy(1));
        XYPlot p0 = c.createXYPlot(tot.getName(0), tot.getSerieCopy(0));
        CombinedDomainXYPlot p = c.createCombinedDomainXYPlot("", new XYPlot[]{p2, p1, p0}, true);
        c.plotCombined(p, 640, 480);
    }

    public CallableClass(Fints f, int h, int k) throws Exception {
        if (f.isEmpty() || f.getNoSeries() > 1) {
            throw new RuntimeException("only one serie allowed");
        }
        this.f = f;
        this.k = k;
        this.h = h;
    }

    private CallableClass longshortts() {
        try {
            Fints i1 = Fints.SMA(Fints.Sharpe(Fints.ER(f, 100, true), 20), h);
            Fints i2 = Fints.SMA(Fints.Sharpe(Fints.ER(f, 100, true), 20), k);
            tot = i1.merge(i2).merge(f);
            TreeMap<UDate, Double> eq = new TreeMap<>();
            eq.put(tot.getFirstDate(), INITCAP);
            boolean flat = true, ls = false;
            for (int j = 0; j < (tot.getLength() - 1); j++) {
                boolean cond = tot.get(j, 1) > 0 && tot.get(j, 0) > 0;
                double v = (tot.get(j + 1, 2) - tot.get(j, 2)) / tot.get(j, 2);
                if (cond) {// k sma & h sma                
                    if (flat) {
                        eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v) - FIXEDFEE);
                        trades++;
                    } else if (!ls) {
                        eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v) - 2 * FIXEDFEE);
                        trades = trades + 2;
                    } else {
                        eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v));
                    }
                    flat = false;
                    ls = true;
                } else if (tot.get(j, 1) < 0 ^ tot.get(j, 0) < 0) {//xor
                    if (!flat) {
                        if (ls) {
                            eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 + v) - FIXEDFEE);
                        } else {
                            eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 - v) - FIXEDFEE);
                        }
                        trades++;
                        flat = true;
                    } else {
                        eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)));
                    }
                } else {
                    if (flat) {
                        eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 - v) - FIXEDFEE);
                        trades++;
                    } else if (ls) {
                        eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 - v) - 2 * FIXEDFEE);
                        trades = trades + 2;
                    } else {
                        eq.put(tot.getDate(j + 1), eq.get(tot.getDate(j)) * (1 - v));
                    }
                    flat = false;
                    ls = false;
                }
            }
            if (!flat) {
                trades++;
                eq.replace(eq.lastKey(), eq.lastEntry().getValue() - FIXEDFEE);//close trades        
            }
            eqres = new Fints(eq, Arrays.asList("equity-" + f.getName(0) + "-" + tot.getFirstDate().toYYYYMMDD()), tot.getFrequency());
        } catch (Exception e) {
            logger.warn(e);
        }
        return this;
    }

    @Override
    public CallableClass call() throws Exception {
        return longshortts();
    }
}

public class TestTrading {

    static Logger logger = org.apache.log4j.Logger.getLogger(TestTrading.class);

    public static ArrayList<Double> test(String hash, int h, int k, TreeSet<UDate> dates) throws Exception {
        ArrayList<Double> profit = new ArrayList<>();
        for (UDate d : dates) {
            Fints f = Security.changeFreq(Security.createContinuity(Database.getIntradayFintsQuotes(hash, d)), Fints.frequency.MINUTE).getSerieCopy(3);
            CallableClass c = new CallableClass(f, h, k).call();
            c.plotTot();
            profit.add(c.getEquity().getLastValueInCol(0));
        }
        return profit;
    }

    public static void main(String[] args) throws Exception {
        final double VARFEE = .001, FIXEDFEE = 7, INITCAP = 60000, MINSAMPLES = 300;
        final int TRAINWIN = 100, TESTWIN = 10;
        HashMap<String, TreeMap<UDate, Fints>> fintsmap = new HashMap<>();
        TreeSet<UDate> dates = Misc.mostRecentTimeSegment(Database.getIntradayDates(), 1000 * 60 * 60 * 24 * 5);
        HashMap<String, TreeSet<UDate>> map = Database.getIntradayDatesMap();
        HashMap<String, String> nmap = Database.getCodeMarketName(new ArrayList<>(map.keySet()));
        

        //TestTrading.test(Database.getHashcode("ENEL", "MLSE"), 2, 26, datest);
        for (String x : map.keySet()) {
            if (map.get(x).containsAll(dates) && nmap.get(x).contains("FBK.MLSE.STOCK")) {//best profit at k,h=[187, 202] FBK
                boolean toadd = true;
                TreeMap<UDate, Fints> t1 = new TreeMap<>();
                for (UDate d : dates) {
                    Fints f1 = Database.getIntradayFintsQuotes(x, d);
                    if (f1.getLength() < MINSAMPLES) {
                        toadd = false;
                        break;
                    } else {
                        t1.put(d, Security.changeFreq(Security.createContinuity(Database.getIntradayFintsQuotes(x, d)), Fints.frequency.MINUTE).getSerieCopy(3));

                    }
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
        ExecutorService executor = null;
        UDate[] datesarr = dates.stream().map(i -> i).toArray(UDate[]::new);
        
        for (String x : fintsmap.keySet()) {
            java.util.ArrayList<Double> testprofits= new ArrayList<>();
            for (int m = 0; m < (dates.size() - TESTWIN); m = m + TESTWIN) {
                TreeSet<UDate> traindates = new TreeSet<>();
                TreeSet<UDate> testdates = new TreeSet<>();
                if ((m+TRAINWIN)>=(datesarr.length-1)) break;
                
                for (int jj = m; jj < m + TRAINWIN; jj++) {
                    traindates.add(datesarr[jj]);
                }
                int limtest=m+TRAINWIN+TESTWIN;
                if (limtest>datesarr.length) limtest=datesarr.length;
                for (int jj = m + TRAINWIN; jj < limtest; jj++) {
                    testdates.add(datesarr[jj]);
                }
                logger.info("train dates " + traindates.first()+"->"+traindates.last()+"\t"+traindates.size());
                logger.info("test dates " + testdates.first()+"->"+testdates.last()+"\t"+testdates.size());
                AbstractMap.SimpleEntry<ArrayList<Integer>, ArrayList<Double>> bestprofit = new AbstractMap.SimpleEntry(new ArrayList<Integer>(), new ArrayList<Double>());
                try {
                    executor = Executors.newFixedThreadPool(POOL);
                    List<Future<CallableClass>> list = new ArrayList<>();
                    logger.info("analizing " + nmap.get(x));                    
                    for (int k = 2; k <= 240; k++) {
                        for (int h = k; h <= 240; h++) {
                            try {
                                ArrayList<Double> profit = new ArrayList<>();
                                ArrayList<Integer> trades = new ArrayList<>();
                                for (UDate d : traindates) {
                                    Fints f = fintsmap.get(x).get(d);
                                    list.add(executor.submit(new CallableClass(f, h, k)));
                                    if (list.size() >= POOL || (d.equals(dates.last()))) {
                                        for (Future<CallableClass> z : list) {
                                            Fints reseq = z.get().getEquity();
                                            profit.add(reseq.getLastValueInCol(0));
                                            trades.add(z.get().getTrades());
                                        }
                                        list.clear();
                                    }
                                }
                                double mean = profit.stream().mapToDouble(i -> i).summaryStatistics().getAverage();
                                if (bestprofit.getKey().isEmpty()) {
                                    bestprofit = new AbstractMap.SimpleEntry<>(new ArrayList<>(Arrays.asList(k, h)), profit);
                                    logger.info("best profit at k,h=" + bestprofit.getKey());
                                    logger.info("mean " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getAverage());
                                    logger.info("max " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMax());
                                    logger.info("min " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMin());
                                    logger.info("mean trades " + trades.stream().mapToInt(i -> i).summaryStatistics().getAverage());
                                } else {
                                    double bpm = bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getAverage();
                                    if (mean > bpm) {
                                        bestprofit = new AbstractMap.SimpleEntry<>(new ArrayList<>(Arrays.asList(k, h)), profit);
                                        if (mean > INITCAP) {
                                            logger.info("best profit at k,h=" + bestprofit.getKey());
                                            logger.info("mean " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getAverage());
                                            logger.info("max " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMax());
                                            logger.info("min " + bestprofit.getValue().stream().mapToDouble(i -> i).summaryStatistics().getMin());
                                            logger.info("mean trades " + trades.stream().mapToInt(i -> i).summaryStatistics().getAverage());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.warn(e);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("skip due to error");
                } finally {
                    if (executor!=null) {
                    executor.shutdown();
                    executor.awaitTermination(10, TimeUnit.MINUTES);                    
                    }
                }
                ArrayList<Double> tp=test(x, bestprofit.getKey().get(0),bestprofit.getKey().get(1), testdates);
                logger.info("average test profit :"+tp.stream().mapToDouble(i->i).summaryStatistics().getAverage());
                testprofits.addAll(tp);
                logger.info("overall average test profit :"+testprofits.stream().mapToDouble(i->i).summaryStatistics().getAverage());                
            }
        }
    }
}
