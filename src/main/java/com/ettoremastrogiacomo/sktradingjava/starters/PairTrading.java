/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
class Results {

    public double fitness, grossprofit;
    public Set<String> posdicestring, negdicestring;
    public UDate[] datesarr;

}

class ThreadClass implements Callable<Results> {

    HashMap<String, TreeMap<UDate, Fints>> fintsmap;
    UDate[] datesarr;
    Set<String> posdicestring, negdicestring;

    public ThreadClass(HashMap<String, TreeMap<UDate, Fints>> fintsmap, UDate[] datesarr, Set<String> posdicestring, Set<String> negdicestring) {
        this.fintsmap = fintsmap;
        this.datesarr = datesarr;
        this.posdicestring = posdicestring;
        this.negdicestring = negdicestring;
    }

    static Results test(HashMap<String, TreeMap<UDate, Fints>> fintsmap, UDate[] datesarr, Set<String> posdicestring, Set<String> negdicestring) throws Exception {
        ThreadClass tc = new ThreadClass(fintsmap, datesarr, posdicestring, negdicestring);
        for (int j = 0; j < datesarr.length; j++) {
            Fints eqall = new Fints();
            for (String x : posdicestring) {
                eqall = eqall.isEmpty() ? fintsmap.get(x).get(datesarr[j]).getEquity() : Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquity());
            }
            for (String x : negdicestring) {
                eqall = eqall.isEmpty() ? fintsmap.get(x).get(datesarr[j]).getEquityShort() : Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquityShort());
            }
            eqall = Fints.MEANCOLS(eqall);
            eqall.merge(eqall.getLinReg(0)).plot("eq " + datesarr[j].toYYYYMMDD(), "y");
        }
        return tc.call();
    }

    @Override
    public Results call() throws Exception {
        double stepfitness = 0;
        double grossprofit = 0;
        
        for (int j=0;j<datesarr.length;j++){
            for (String x : posdicestring) {
                Fints f1=fintsmap.get(x).get(datesarr[j]);
                grossprofit+=(f1.get(f1.getLength()-1, 0)-f1.get(0, 0))/f1.get(0, 0);
            }
            for (String x : negdicestring) {
                Fints f1=fintsmap.get(x).get(datesarr[j]);
                grossprofit-=(f1.get(f1.getLength()-1, 0)-f1.get(0, 0))/f1.get(0, 0);
            }        
            grossprofit/=(posdicestring.size()+negdicestring.size());
        }
        grossprofit/=datesarr.length;
        /*
        for (int j = 0; j < datesarr.length; j++) {
            Fints eqall = new Fints();
            for (String x : posdicestring) {
                eqall = eqall.isEmpty() ? fintsmap.get(x).get(datesarr[j]).getEquity() : Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquity());
            }
            for (String x : negdicestring) {
                eqall = eqall.isEmpty() ? fintsmap.get(x).get(datesarr[j]).getEquityShort() : Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquityShort());
            }
            eqall = Fints.MEANCOLS(eqall);
            HashMap<String, Double> stats = DoubleArray.LinearRegression(eqall.getCol(0));
            grossprofit += eqall.getLastRow()[0] - eqall.getFirstValueInCol(0);//gross profit
            stepfitness+=stats.get("slope")/stats.get("stderr");//sharpe
            //stepfitness += eqall.getLastRow()[0] - eqall.getFirstValueInCol(0);//gross profit
            //1.0/Math.abs(eqall.getFirstValueInCol(0)-eqall.getLastRow()[0]);
        }*/
        Results res = new Results();
        res.fitness = grossprofit;//stepfitness / datesarr.length;
        res.negdicestring = negdicestring;
        res.posdicestring = posdicestring;
        res.datesarr = this.datesarr;
        res.grossprofit = grossprofit / datesarr.length;
        return res;
    }
}

public class PairTrading {

    static Logger logger = Logger.getLogger(PairTrading.class);

    public static void main(String[] args) throws Exception {

        int limitsamples = 100;
        int PAIR = 2, EPOCHS = 10000, TESTSET = 1, TRAINSET = 35;
        final double VARFEE = .001, FIXEDFEE = 7, INITCAP = PAIR*60000;
        HashMap<String, TreeMap<UDate, Fints>> fintsmap = new HashMap<>();
        TreeSet<UDate> dates = Database.getIntradayDates();
        TreeSet<UDate> mio = Misc.mostRecentTimeSegment(dates, 1000 * 60 * 60 * 24 * 5);
        mio.forEach((x) -> {
            logger.debug(x.toString());
        });
        logger.debug(mio.size());
        HashMap<String, TreeSet<UDate>> map = Database.getIntradayDatesMap();
        HashMap<String, String> nmap = Database.getCodeMarketName(new ArrayList<>(map.keySet()));



        for (String x : map.keySet()) {
            if (map.get(x).containsAll(mio) && nmap.get(x).contains("MLSE.STOCK")) {

                TreeMap<UDate, Fints> t1 = new TreeMap<>();
                boolean toadd = true;
                for (UDate d : mio) {
                    Fints f1 = Database.getIntradayFintsQuotes(x, d);
                    if (f1.getLength() < limitsamples) {
                        toadd = false;
                        break;
                    }
                    // tmap1.put(d, Fints.createContinuity(Security.changeFreq(Database.getIntradayFintsQuotes(x, d), Fints.frequency.MINUTE).getSerieCopy(3)));
                    t1.put(d, Fints.createContinuity(Security.changeFreq(f1, Fints.frequency.MINUTE).getSerieCopy(3)));
                }
                if (toadd) {
                    logger.info("added " + nmap.get(x));
                    fintsmap.put(x, t1);
                }
            }
        }

        logger.info(fintsmap.size() + "\tof\t" + map.size());

        UDate[] datesall = mio.stream().toArray(UDate[]::new);
        UDate[] datesarr = new UDate[TRAINSET];
        UDate[] testdates = new UDate[TESTSET];
        ArrayList<Double> netprofit=new ArrayList<>();
        for (int i = 0; i < (datesall.length - TESTSET); i++) {
            for (int j = 0; j < datesarr.length; j++) {
                datesarr[j] = datesall[i + j];
            }
            for (int j = 0; j < testdates.length; j++) {
                testdates[j] = datesall[i + j + TRAINSET];
            }
            logger.debug("dates " + datesarr.length + "\t" + datesarr[0] + "\t" + datesarr[datesarr.length - 1]);
            logger.debug("testdates " + testdates.length + "\t" + testdates[0] + "\t" + testdates[testdates.length - 1]);
            logger.debug("stocks " + fintsmap.size());
            String[] hasharr = fintsmap.keySet().stream().toArray(String[]::new);
            Results bestresult = new Results();
            bestresult.fitness = Double.NEGATIVE_INFINITY;
            int POOL = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(POOL);
            List<Future<Results>> list = new ArrayList<>();
            for (int k = 0; k < EPOCHS; k++) {
                Set<String> posdicestring = new HashSet<>();
                Set<String> negdicestring = new HashSet<>();
                Integer[] dice = Misc.getDistinctRandom(PAIR * 2, fintsmap.size()).toArray(Integer[]::new);
                for (int j = 0; j < PAIR; j++) {
                    posdicestring.add(hasharr[dice[j]]);
                }
                for (int j = PAIR; j < (PAIR * 2); j++) {
                    negdicestring.add(hasharr[dice[j]]);
                }
                list.add(executor.submit(new ThreadClass(fintsmap, datesarr, posdicestring, negdicestring)));
                if (list.size() >= POOL) {
                    for (Future<Results> x : list) {
                        Results resfuture = x.get();
                        if (bestresult.fitness < resfuture.fitness) {
                            bestresult = resfuture;
                            logger.debug("best fit : " + bestresult.fitness);
                            logger.debug("from " + bestresult.datesarr[0] + " to " + bestresult.datesarr[bestresult.datesarr.length - 1] + "\tsamples=" + bestresult.datesarr.length);
                            bestresult.posdicestring.forEach((y) -> {
                                logger.debug("pos :" + y + "." + nmap.get(y));
                            });
                            bestresult.negdicestring.forEach((y) -> {
                                logger.debug("neg :" + y + "." + nmap.get(y));
                            });
                        }
                    }
                    list.clear();
                }
            }
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
            Results fres = ThreadClass.test(fintsmap, testdates, bestresult.posdicestring, bestresult.negdicestring);
            logger.info("check best fit : " + fres.fitness);
            logger.info("from " + fres.datesarr[0] + " to " + fres.datesarr[fres.datesarr.length - 1] + "\tsamples=" + fres.datesarr.length);
            fres.posdicestring.forEach((y) -> {
                logger.info("pos :" + y + "." + nmap.get(y));
            });
            fres.negdicestring.forEach((y) -> {
                logger.info("neg :" + y + "." + nmap.get(y));
            });
            
            //fres.grossprofit = fres.grossprofit > 0 ? fres.grossprofit *.74 : fres.grossprofit;
            //double finalcap = INITCAP * (1 + fres.grossprofit) * (1 - VARFEE) - FIXEDFEE * PAIR * 4;
            double finalcap = INITCAP * (1 + fres.grossprofit);
            netprofit.add((finalcap - INITCAP));
            
            logger.info("net profit=" + (finalcap - INITCAP));
            
            
            double[] np=  netprofit.stream().mapToDouble(Double::doubleValue).toArray();
            logger.info("net profit stats:");
            logger.info("initcap="+INITCAP+"\tpair="+PAIR+"\tvarfee="+VARFEE+"\tfixedfee="+FIXEDFEE+"\tsamples="+np.length);
            logger.info("sum ="+DoubleArray.sum(np));
            logger.info("mean ="+DoubleArray.mean(np));
            logger.info("max ="+DoubleArray.max(np));
            logger.info("min ="+DoubleArray.min(np));
            logger.info("std ="+DoubleArray.std(np));            
        }
        //for (int i=0;i<20;i++) testdates[i]=datesarr[datesarr.length-20+i];
    }
}