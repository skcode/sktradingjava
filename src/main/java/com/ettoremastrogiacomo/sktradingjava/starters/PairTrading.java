


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
    public double fitness;
    public Set<String> posdicestring,negdicestring;   
}
class ThreadClass implements Callable<Results> {
    HashMap<String, TreeMap<UDate, Fints>> fintsmap;
    UDate[] datesarr;
    Set<String> posdicestring,negdicestring;
    public ThreadClass(HashMap<String, TreeMap<UDate, Fints>> fintsmap,UDate[] datesarr,Set<String> posdicestring,Set<String> negdicestring) {
        this.fintsmap=fintsmap;
        this.datesarr=datesarr;
        this.posdicestring=posdicestring;
        this.negdicestring=negdicestring;
    }
    
    static Results test(HashMap<String, TreeMap<UDate, Fints>> fintsmap,UDate[] datesarr,Set<String> posdicestring,Set<String> negdicestring)throws Exception {
        ThreadClass tc= new ThreadClass(fintsmap, datesarr, posdicestring, negdicestring);
        for (int j=0;j<datesarr.length;j++){
            Fints eqall=new Fints();
            for (String x: posdicestring) {eqall=eqall.isEmpty()? fintsmap.get(x).get(datesarr[j]).getEquity():Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquity()); }
            for (String x: negdicestring) {eqall=eqall.isEmpty()? fintsmap.get(x).get(datesarr[j]).getEquityShort():Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquityShort()); }                
            eqall=Fints.MEANCOLS(eqall);
            eqall.merge(eqall.getLinReg(0)).plot("eq", "y");
        }             
        return tc.call();
    }
    @Override
    public Results call() throws Exception {
        double stepfitness=0;
        for (int j=0;j<datesarr.length;j++){
            Fints eqall=new Fints();
            for (String x: posdicestring) {eqall=eqall.isEmpty()? fintsmap.get(x).get(datesarr[j]).getEquity():Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquity()); }
            for (String x: negdicestring) {eqall=eqall.isEmpty()? fintsmap.get(x).get(datesarr[j]).getEquityShort():Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquityShort()); }                
            eqall=Fints.MEANCOLS(eqall);
            HashMap<String,Double> stats=DoubleArray.LinearRegression(eqall.getCol(0));
            stepfitness+=Math.abs(eqall.getMax()[0]-eqall.getMin()[0]);// 1/Math.abs(eqall.getFirstValueInCol(0)-eqall.getLastRow()[0]);//1.0/Math.abs(eqall.getFirstValueInCol(0)-eqall.getLastRow()[0]);
        }     
        Results res= new Results();
        res.fitness=stepfitness/datesarr.length;
        res.negdicestring=negdicestring;
        res.posdicestring=posdicestring;        
        return res;
    }

}
public class PairTrading {

    static Logger logger = Logger.getLogger(PairTrading.class);
    
    

    public static void main(String[] args) throws Exception {
        int limitsamples = 300;
        double limitpct = .50;
        int PAIR=1,EPOCHS=10000,TESTSET=10;
        
        
        HashMap<String, TreeMap<UDate, Fints>> fintsmap = new HashMap<>();
        String datafileName = "./pairtrading.dat";
        TreeSet<UDate> dates = Database.getIntradayDates();
        TreeMap<UDate, ArrayList<String>> rmap = Database.getIntradayDatesReverseMap();
        HashMap<String, TreeSet<UDate>> map = Database.getIntradayDatesMap();
        HashMap<String, String> nmap = Database.getCodeMarketName(new ArrayList<String>(map.keySet()));

        if (new File(datafileName).exists()) {
            logger.debug("caricamento file " + datafileName);
            fintsmap = (HashMap<String, TreeMap<UDate, Fints>>) Misc.readObjFromFile(datafileName);
        } else {


            HashMap<String, Integer> timesmap = new HashMap<>();
            int maxt = 0;
            for (String x : map.keySet()) {

                if (nmap.get(x).contains("STOCK")) {
                    if (maxt < map.get(x).size()) {
                        maxt = map.get(x).size();
                    }
                    timesmap.put(x, map.get(x).size());
                }
            }
            logger.debug("MAX TIMES " + maxt);

            HashSet<String> toremove = new HashSet<>();
            for (String i : timesmap.keySet()) {
                if (timesmap.get(i) > (maxt * limitpct)) {
                    logger.debug(nmap.get(i) + "\t" + timesmap.get(i));
                } else {
                    toremove.add(i);
                }
                if (!map.get(i).contains(dates.last())) toremove.add(i);
            }
            toremove.forEach((x) -> {
                timesmap.remove(x);
            });

            for (String x : timesmap.keySet()) {
                TreeSet<UDate> td1 = map.get(x);
                TreeMap<UDate, Fints> tmap1 = new TreeMap<>();
                for (UDate d : td1) {
                    tmap1.put(d, Database.getIntradayFintsQuotes(x, d));
                }
                double samples1 = 0;
                for (Fints f1 : tmap1.values()) {
                    samples1 += f1.getLength();
                }
                samples1 = (double) samples1 / tmap1.size();
                if (samples1 > limitsamples) {
                    tmap1.clear();
                    for (UDate d : td1) {
                        tmap1.put(d, Fints.createContinuity(Security.changeFreq(Database.getIntradayFintsQuotes(x, d), Fints.frequency.MINUTE).getSerieCopy(3)));
                    }
                    //for (UDate d: td1) tmap1.replace(d, Fints.createContinuity(tmap1.get(d))) ;
                    logger.debug("loading fints from " + nmap.get(x));
                    fintsmap.put(x, tmap1);
                }

            }
            logger.debug("*******************");
            for (String x : fintsmap.keySet()) {
                logger.debug(nmap.get(x));
            }
            Misc.writeObjToFile(fintsmap, datafileName);
        }
        for (TreeMap<UDate, Fints> x : fintsmap.values()) {
            dates.retainAll(x.keySet());
        }

        
        
        UDate[] datesarr=dates.stream().limit(dates.size()-TESTSET).toArray(UDate[]::new);
        UDate[] testdates=dates.stream().skip(dates.size()-TESTSET).toArray(UDate[]::new);;
        //for (int i=0;i<20;i++) testdates[i]=datesarr[datesarr.length-20+i];
        
        logger.debug("dates " + datesarr.length+"\t"+datesarr[0]+"\t"+datesarr[datesarr.length-1]);
        logger.debug("testdates " + testdates.length+"\t"+testdates[0]+"\t"+testdates[testdates.length-1]);
        logger.debug("stocks " + fintsmap.size());
        String[] hasharr=fintsmap.keySet().stream().toArray(String[]::new);        
        Results bestresult=new Results();
        bestresult.fitness=Double.NEGATIVE_INFINITY;        
        int POOL=Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(POOL);
        List<Future<Results>> list = new ArrayList<>();  
        for (int i=0;i<EPOCHS;i++){
            Set<String> posdicestring= new HashSet<>();
            Set<String> negdicestring= new HashSet<>();
            Integer[] dice=Misc.getDistinctRandom(PAIR*2, fintsmap.size()).toArray(Integer[]::new);
            for (int j=0;j<PAIR;j++) posdicestring.add(hasharr[dice[j]]);
            for (int j=PAIR;j<(PAIR*2);j++) negdicestring.add(hasharr[dice[j]]);                        
            list.add(executor.submit(new ThreadClass(fintsmap, datesarr, posdicestring, negdicestring)));
            if (list.size()>=POOL){
                for (Future<Results> x: list){
                    Results resfuture=x.get();
                    if (bestresult.fitness<resfuture.fitness) {
                        bestresult=resfuture;
                        logger.debug("best fit : "+bestresult.fitness);
                        bestresult.posdicestring.forEach((y)->{logger.debug("pos :"+y+"."+nmap.get(y));});
                        bestresult.negdicestring.forEach((y)->{logger.debug("neg :"+y+"."+nmap.get(y));});
                    }
                }                
                list.clear();
            }
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);
        Results fres=ThreadClass.test(fintsmap, testdates, bestresult.posdicestring, bestresult.negdicestring);
        logger.debug("check best fit : "+fres.fitness);
        fres.posdicestring.forEach((y)->{logger.debug("pos :"+y+"."+nmap.get(y));});
        fres.negdicestring.forEach((y)->{logger.debug("neg :"+y+"."+nmap.get(y));});        
    }
}
