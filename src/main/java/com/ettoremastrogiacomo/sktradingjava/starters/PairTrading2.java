/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 *
 * @author a241448
 */



public class PairTrading2 {

    static Logger logger = Logger.getLogger(PairTrading.class);

    public static void main(String[] args) throws Exception {
        String filename = "./pairtrading.dat";
        File file = new File(filename);
        int limitsamples = 200;
        int PAIR = 1, EPOCHS = 10000, TESTSET = 1, TRAINSET = 80;
        final double VARFEE = .001, FIXEDFEE = 7, INITCAP = PAIR * 60000;
        HashMap<String, TreeMap<UDate, Fints>> fintsmap = new HashMap<>();
        TreeSet<UDate> dates = Database.getIntradayDates();
        TreeSet<UDate> mio = Misc.mostRecentTimeSegment(dates, 1000 * 60 * 60 * 24 * 5);
        mio.forEach((x) -> {
            logger.debug(x.toString());
        });
        logger.debug(mio.size());
        HashMap<String, TreeSet<UDate>> map = Database.getIntradayDatesMap();
        HashMap<String, String> nmap = Database.getCodeMarketName(new ArrayList<>(map.keySet()));

        if (file.exists()) {
            fintsmap = (HashMap<String, TreeMap<UDate, Fints>>) Misc.readObjFromFile(filename);
        } else {
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
            Misc.writeObjToFile(fintsmap, filename);
        }
        logger.info(fintsmap.size() + "\tof\t" + map.size());

        UDate[] datesall = mio.stream().toArray(UDate[]::new);
        UDate[] datesarr = new UDate[TRAINSET];
        UDate[] testdates = new UDate[TESTSET];
        ArrayList<Double> netprofit = new ArrayList<>();
        for (int i = 0; i < (datesall.length - TESTSET - TRAINSET + 1); i = i + TESTSET) {
            for (int j = 0; j < datesarr.length; j++) {
                datesarr[j] = datesall[i + j];
            }
            logger.debug("i=" + i);
            for (int j = 0; j < testdates.length; j++) {
                testdates[j] = datesall[i + j + TRAINSET];
            }
            logger.debug("dates " + datesarr.length + "\t" + datesarr[0] + "\t" + datesarr[datesarr.length - 1]);
            logger.debug("testdates " + testdates.length + "\t" + testdates[0] + "\t" + testdates[testdates.length - 1]);
            logger.debug("stocks " + fintsmap.size());
            TreeMap<Double,String> ranking=new TreeMap<>();
            
            for (String x:fintsmap.keySet()) {               
                double mx=0;
                for (UDate d:datesarr){
                    mx+=(fintsmap.get(x).get(d).getLastValueInCol(0)-fintsmap.get(x).get(d).getFirstValueInCol(0))/fintsmap.get(x).get(d).getFirstValueInCol(0);
                }
                mx=mx/datesarr.length;
                ranking.put(mx, x);
            }        
            TreeMap<Double, String> headmap=ranking .entrySet().stream().limit(PAIR).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
            TreeMap<Double, String> tailmap=ranking.descendingMap().entrySet().stream().limit(PAIR).collect(TreeMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);                        
            double hms=tailmap.keySet().stream().collect(Collectors.summingDouble(Double::doubleValue));
            double tms=-headmap.keySet().stream().collect(Collectors.summingDouble(Double::doubleValue));
            logger.debug("train head:");
            headmap.values().forEach((x)->logger.debug(nmap.get(x)));
            logger.debug(hms);
            logger.debug("train tail:");
            tailmap.values().forEach((x)->logger.debug(nmap.get(x)));
            logger.debug(-tms);
            double tottrain=(hms+tms)/(PAIR*2.0);
            TreeMap<Double,String> test=new TreeMap<>();
            for (String x:headmap.values()) {               
                double mx=0;
                for (UDate d:testdates){
                    mx+=(fintsmap.get(x).get(d).getLastValueInCol(0)-fintsmap.get(x).get(d).getFirstValueInCol(0))/fintsmap.get(x).get(d).getFirstValueInCol(0);
                }
                mx=mx/testdates.length;
                test.put(-mx, x);
            }                    
            for (String x:tailmap.values()) {               
                double mx=0;
                for (UDate d:testdates){
                    mx+=(fintsmap.get(x).get(d).getLastValueInCol(0)-fintsmap.get(x).get(d).getFirstValueInCol(0))/fintsmap.get(x).get(d).getFirstValueInCol(0);
                }
                mx=mx/testdates.length;
                test.put(mx, x);
            }                   
            double tottest=test.keySet().stream().collect(Collectors.summingDouble(Double::doubleValue))/(PAIR*2);
            logger.debug("tail:");
            tailmap.values().forEach((x)->logger.debug(nmap.get(x)));
            logger.debug("head:");
            headmap.values().forEach((x)->logger.debug(nmap.get(x)));
            logger.debug("train:" + tottrain);
            logger.debug("test:" + tottest);
            netprofit.add(tottest);
            logger.debug("progress: "+(netprofit.stream().collect(Collectors.summingDouble(Double::doubleValue))));;
        }
        //for (int i=0;i<20;i++) testdates[i]=datesarr[datesarr.length-20+i];
    }
}
