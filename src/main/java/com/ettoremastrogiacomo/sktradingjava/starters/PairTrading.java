/*
 * To change this license header, choose License Headers in Project Properties.
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
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 *
 * @author a241448
 */
public class PairTrading {

    static Logger logger = Logger.getLogger(PairTrading.class);

    public static void main(String[] args) throws Exception {
        int limitsamples = 300;
        double limitpct = .90;

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

        logger.debug("dates " + dates.size());
        logger.debug("stocks " + fintsmap.size());
        
        UDate[] datesarr=dates.stream().toArray(UDate[]::new);
        String[] hasharr=fintsmap.keySet().stream().toArray(String[]::new);
        int PAIR=3,EPOCHS=10000;
        double bestfitness=Double.NEGATIVE_INFINITY;
        for (int i=0;i<EPOCHS;i++){
            Set<String> posdicestring= new HashSet<>();
            Set<String> negdicestring= new HashSet<>();
            Integer[] dice=Misc.getDistinctRandom(PAIR*2, fintsmap.size()).toArray(Integer[]::new);
            for (int j=0;j<PAIR;j++) posdicestring.add(hasharr[dice[j]]);
            for (int j=PAIR;j<(PAIR*2);j++) negdicestring.add(hasharr[dice[j]]);            
            Fints meanF=new Fints();            
            double stepfitness=0;
            for (int j=0;j<datesarr.length;j++){
                Fints eqall=new Fints();
                for (String x: posdicestring) {eqall=eqall.isEmpty()? fintsmap.get(x).get(datesarr[j]).getEquity():Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquity()); }
                for (String x: negdicestring) {eqall=eqall.isEmpty()? fintsmap.get(x).get(datesarr[j]).getEquityShort():Fints.merge(eqall, fintsmap.get(x).get(datesarr[j]).getEquityShort()); }                
                eqall=Fints.MEANCOLS(eqall);
                HashMap<String,Double> stats=DoubleArray.LinearRegression(eqall.getCol(0));
                stepfitness+=eqall.getLastRow()[0];//1.0/Math.abs(eqall.getFirstValueInCol(0)-eqall.getLastRow()[0]);
                /*logger.debug("step "+(j+1)+" of "+datesarr.length);
                posdicestring.forEach((x)->{logger.debug("pos :"+nmap.get(x));});
                negdicestring.forEach((x)->{logger.debug("neg :"+nmap.get(x));});
                stats.keySet().forEach((x)-> {logger.debug(x+"\t"+stats.get(x));});*/
                //eqall.merge(eqall.getLinReg(0)).plot("m", "d");

            }        
            stepfitness/=datesarr.length;
            if (stepfitness>bestfitness){
                bestfitness=stepfitness;
                posdicestring.forEach((x)->{logger.debug("pos :"+x+"."+nmap.get(x));});
                negdicestring.forEach((x)->{logger.debug("neg :"+x+"."+nmap.get(x));});
                logger.debug("best fitness "+bestfitness);
            }
        }
        if (true) {
            return;
        }
    }
}
