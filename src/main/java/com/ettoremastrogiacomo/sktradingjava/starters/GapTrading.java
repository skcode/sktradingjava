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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 *
 * @author sk
 */
public class GapTrading {
    static Logger logger=org.apache.log4j.Logger.getLogger (GapTrading.class) ;
    public static void main(String[] args) throws Exception {
        String filename = "./pairtrading.dat";
        File file = new File(filename);
        int minvol = 100000;
        int PAIR = 5;
        int hourstart=9,minutestart=00,hourend=17,minuteend=29;
        final double VARFEE = .001, FIXEDFEE = 7, INITCAP = PAIR * 60000;
        
        HashMap<String, TreeMap<UDate, AbstractMap.SimpleEntry<Double,Double>> > fintsmap = new HashMap<>();
        TreeSet<UDate> dates = Database.getIntradayDates();
        TreeSet<UDate> mio = Misc.mostRecentTimeSegment(dates, 1000 * 60 * 60 * 24 * 5);

        HashMap<String, TreeSet<UDate>> map = Database.getIntradayDatesMap();
        HashMap<String, String> nmap = Database.getCodeMarketName(new ArrayList<>(map.keySet()));


        for (String x : map.keySet()) {
            if (map.get(x).containsAll(mio) && nmap.get(x).contains("MLSE.STOCK")) {

                TreeMap<UDate, AbstractMap.SimpleEntry<Double,Double>> t1 = new TreeMap<>();
                boolean toadd = true;
                for (UDate d : mio) {
                    Fints f1 = Database.getIntradayFintsQuotes(x, d);
                    UDate d1s=UDate.getNewDate(d, hourstart, minutestart, 0);
                    UDate d1e=UDate.getNewDate(d, hourend, minuteend, 0);

                    if (f1.getSums()[4] < minvol || f1.getIndex(d1e)<0 || f1.getIndex(d1s)<0) {
                        toadd = false;
                        break;
                    }
                    // tmap1.put(d, Fints.createContinuity(Security.changeFreq(Database.getIntradayFintsQuotes(x, d), Fints.frequency.MINUTE).getSerieCopy(3)));
                    t1.put(d, new AbstractMap.SimpleEntry<>(f1.get(d1s, 0),f1.get(d1e, 3)));
                }
                if (toadd) {
                    logger.info("added " + nmap.get(x));
                    fintsmap.put(x, t1);
                }
            }
        }
        logger.info(fintsmap.size() + "\tof\t" + map.size());
        String[] hasharr = fintsmap.keySet().stream().toArray(String[]::new);
        UDate[] datesall = mio.stream().toArray(UDate[]::new);
        ArrayList<Double> netprofit = new ArrayList<>();
        ArrayList<Double> cumprofit = new ArrayList<>();
        ArrayList<Double>grossprofit = new ArrayList<>();
        
        for (int i = 1; i < (datesall.length); i++) {
            
            logger.debug("dates " + datesall[i] + "\t" + datesall[i-1]);
            logger.debug("stocks " + fintsmap.size());
            TreeMap<Double,String> closeopengap=new  TreeMap<>();
            UDate prevdateend=UDate.getNewDate(datesall[i-1], hourend, minuteend, 0);
            UDate currdateend=UDate.getNewDate(datesall[i], hourend, minuteend, 0);
            UDate currdatestart=UDate.getNewDate(datesall[i], hourstart, minutestart, 0);
            logger.debug("check dates:"+prevdateend+"\t"+currdatestart+"\t"+currdateend);
            for (String s:hasharr) {
                try {
                    double delta=(fintsmap.get(s).get(datesall[i]).getKey()-fintsmap.get(s).get(datesall[i-1]).getValue())/fintsmap.get(s).get(datesall[i-1]).getValue();
                    fintsmap.get(s).get(datesall[i]).getValue();
                    closeopengap.put(delta, s);                
                }
                catch(Exception e) {
                    logger.warn(nmap.get(s) + " skipped");;
                }
            }
            Double[] neggapbest=closeopengap.keySet().stream().sorted().limit(PAIR).toArray(Double[]::new);
            Double[] posgapbest=closeopengap.keySet().stream().sorted(Comparator.reverseOrder()).limit(PAIR).toArray(Double[]::new);
            double gp=0;            
            
            for (Double neggapbest1 : neggapbest) {
                //Fints f1 = fintsmap.get(closeopengap.get(neggapbest1)).get(datesall[i]);
                String x=closeopengap.get(neggapbest1);
                logger.debug("buy "+nmap.get(x));                
                gp+=  (fintsmap.get(x).get(datesall[i]).getValue()-fintsmap.get(x).get(datesall[i]).getKey())/fintsmap.get(x).get(datesall[i]).getKey(); // f1.get(currdateend,0)-f1.get(currdatestart,0))/f1.get(currdatestart,0);                
                logger.debug("values open-close "+fintsmap.get(x).get(datesall[i]).getKey()+"\t"+fintsmap.get(x).get(datesall[i]).getValue());                
            }
            for (Double posgapbest1 : posgapbest) {
                String x=closeopengap.get(posgapbest1);
                logger.debug("sell "+nmap.get(x));
                gp-=  (fintsmap.get(x).get(datesall[i]).getValue()-fintsmap.get(x).get(datesall[i]).getKey())/fintsmap.get(x).get(datesall[i]).getKey(); // f1.get(currdateend,0)-f1.get(currdatestart,0))/f1.get(currdatestart,0);                
                logger.debug("values open-close "+fintsmap.get(x).get(datesall[i]).getKey()+"\t"+fintsmap.get(x).get(datesall[i]).getValue());                

            }
            gp=gp/(PAIR*2);
            double np=INITCAP*(gp-VARFEE)-FIXEDFEE*PAIR*2;
            logger.info("gross profit "+gp);
            logger.info("net profit "+np);
            grossprofit.add(gp);
            netprofit.add(np);
            if (cumprofit.isEmpty()) cumprofit.add(np);else cumprofit.add(cumprofit.get(cumprofit.size()-1)+np);
            logger.info("average : "+grossprofit.stream().mapToDouble(k->k).summaryStatistics().getAverage());
            logger.info("max : "+grossprofit.stream().mapToDouble(k->k).summaryStatistics().getMax());
            logger.info("min : "+grossprofit.stream().mapToDouble(k->k).summaryStatistics().getMin());
            logger.info("sum : "+grossprofit.stream().mapToDouble(k->k).summaryStatistics().getSum());

        }
        logger.info("******* statistics grosspr********");
        double[] gpa=grossprofit.stream().mapToDouble(i->i).toArray();
        double[] npa=netprofit.stream().mapToDouble(i->i).toArray();

        logger.info("sum "+DoubleArray.sum(gpa));
        logger.info("mean "+DoubleArray.mean(gpa));
        logger.info("std "+DoubleArray.std(gpa));
        logger.info("sharpe ="+DoubleArray.mean(gpa)/DoubleArray.std(gpa));
        logger.info("max "+DoubleArray.max(gpa));
        logger.info("min "+DoubleArray.min(gpa));
        logger.info("total "+gpa.length);
        logger.info("num pos "+grossprofit.stream().mapToInt(i->  (i>=0 ? 1 :0)).sum());
        logger.info("num neg "+grossprofit.stream().mapToInt(i->  (i<=0 ? 1 :0)).sum());
        
        logger.info("******* statistics netpr********");
        logger.info("sum "+DoubleArray.sum(npa));
        logger.info("initcap "+INITCAP);
        logger.info("pair "+PAIR);
        logger.info("yield%  "+100*DoubleArray.sum(npa)/INITCAP);
        logger.info("yield over y%  "+100*DoubleArray.sum(npa)*250/(INITCAP*npa.length));
        logger.info("mean "+DoubleArray.mean(npa));
        logger.info("std "+DoubleArray.std(npa));
        logger.info("sharpe ="+DoubleArray.mean(npa)/DoubleArray.std(npa));
        logger.info("max "+DoubleArray.max(npa));
        logger.info("min "+DoubleArray.min(npa));
        logger.info("total "+npa.length);
        logger.info("num pos "+netprofit.stream().mapToInt(i->  (i>=0 ? 1 :0)).sum());
        logger.info("num neg "+netprofit.stream().mapToInt(i->  (i<=0 ? 1 :0)).sum());
        logger.info("ALL STOCKS n."+fintsmap.keySet().size());
        logger.info("cumprofit min ="+cumprofit.stream().mapToDouble(i->i).summaryStatistics().getMin());
        logger.info("cumprofit max ="+cumprofit.stream().mapToDouble(i->i).summaryStatistics().getMax());
        logger.info("cumprofit end ="+cumprofit.get(cumprofit.size()-1));
        
        fintsmap.keySet().forEach((x)->logger.info(nmap.get(x)));
        
        //for (int i=0;i<20;i++) testdates[i]=datesarr[datesarr.length-20+i];
    }
    
}
