/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import static com.ettoremastrogiacomo.sktradingjava.data.Database.getIntradayFintsQuotes;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;

import com.ettoremastrogiacomo.utils.UDate;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sk
 */
public class IntradayCorrelation {

    static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IntradayCorrelation.class);// Logger.getLogger(IntradayCorrelation.class.getName());

    public static void main(String[] args) throws Exception {

        // UDate checkDate=UDate.genDate(2018, 4, 9, 0, 0, 0);
        java.util.HashMap<String, TreeSet<UDate>> map = Database.intradayDates();
        //    Database.getIntradayFintsQuotes(hashcode, date)
        UDate ld = new UDate(0);
        for (String s : map.keySet()) {
            if (map.get(s).last().after(ld)) {
                ld = map.get(s).last();
            }
        }
        NumberFormat formatter = new DecimalFormat("#0.0000"); 
        logger.debug("last date = " +ld);
        final int MINLEN = 400;
        final int MINMERGELEN = 300;
        java.util.ArrayList<Fints> all = new java.util.ArrayList<>();
        java.util.ArrayList<Fints> lagall = new java.util.ArrayList<>();
        TreeMap<Double, String> vgap = new java.util.TreeMap<>();
        TreeMap<Double, String> vol = new java.util.TreeMap<>();
        TreeMap<Double, String> volex = new java.util.TreeMap<>();
        HashMap<String,Double> dsharpe=new java.util.HashMap<>();
        HashMap<String,Double> dsharpe2=new java.util.HashMap<>();
        for (String x : map.keySet()) {
            try {
                if (!map.get(x).contains(ld)) {
                    continue;
                }
                Fints f0=getIntradayFintsQuotes(x, ld);
                Fints f = f0.getSerieCopy(3);
                if (f.getLength() < MINLEN) continue;
                
                Fints dailyC=Database.getFintsQuotes(x).getSerieCopy(3);
                if (dailyC.getLength()<350) continue;                                
                if (!dailyC.getLastDate().equals(ld)) {
                    logger.warn(dailyC.getLastDate() + "<>"+ld);
                    continue;
                }                       
                dailyC=dailyC.head(300);
                if (dailyC.getMaxDaysDateGap()>7) continue;
                
                //calculus
                
                Fints mSharpe=Fints.SMA(Fints.Sharpe(Fints.ER(dailyC, 100, true), 20), 200) ;
                Fints dmsharpe=Fints.SMA(Fints.Diff(mSharpe), 20);
                dsharpe.put( x,mSharpe.get( mSharpe.getLength()-1,0));
                dsharpe2.put(x, dmsharpe.get( dmsharpe.getLength()-1,0));
                //f=Fints.changeFrequency(f.getSerieCopy(0), Fints.frequency.MINUTE);  
                

                double volume= f0.getSerieCopy(4).getSums()[0];
                double minmax=(f.getMax()[0] - f.getMin()[0])/f.getMin()[0];
                vol.put(volume, x);
                vgap.put(minmax, x);
                volex.put(minmax*volume, x);
                
                f = Fints.ER(f, 100, true);
                if (f.getLength() > MINLEN) {
                    all.add(f);
                    //Fints t=Fints.Lag(Fints.Sign(f, 0,-1,1),1);
                    Fints t = Fints.Lag(f, 1);
                    //t=Fints.Kron(t, t);
                    lagall.add(t);
                    logger.debug("loaded " + f.getName(0) + " at " + ld);
                }
            } catch (Exception e) {
                logger.warn(e);
            }
        }
        logger.debug("size " + all.size());
        logger.debug("start");
        java.util.ArrayList<String> t1=new java.util.ArrayList<>();
        t1.addAll(dsharpe.keySet());
        java.util.HashMap<String,String> namemap=new java.util.HashMap<>();
        List<HashMap<String,String>> names=Database.getRecords(Optional.of(t1), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        names.forEach((x) -> {
            namemap.put(x.get("hashcode"), x.get("code")+"."+x.get("market")+"."+x.get("name"));
        });
        java.util.TreeMap<Double, String> ranking = new java.util.TreeMap<>();
        for (Fints x : all) {
            for (Fints y : lagall) {
                try {
                    Fints mf = x.merge(y);
                    if (mf.getLength() < MINMERGELEN) {
                        continue;
                    }
                    double d = mf.getCorrelation()[1][0];
                    ranking.put(d, x.getName(0) + "\t" + y.getName(0));
                    /*if (Math.abs(d)>Math.abs(bestcorr)){
                        bestcorr=d;
                        bestcouple=x.getName(0)+"\t"+y.getName(0);
                    }*/
                   // logger.info(formatter.format(d) + "\tcorr\t" + x.getName(0) + "\t" + y.getName(0));
                } catch (Exception e) {
                    logger.warn(e);
                }
            }
        }
        logger.debug("\n******* correlation *********");
        for (Double d : ranking.keySet()) {
            logger.debug(formatter.format(d) + "\t\t" + ranking.get(d));
        }
        logger.debug("\n******* daily excursion *********");
        for (Double d : vgap.keySet()) {
            String h=vgap.get(d);
            logger.debug(formatter.format(d) + "\t" + formatter.format(dsharpe.get(h))+ "\t" + formatter.format(dsharpe2.get(h))+ "\t" + namemap.get(h));
        }
        logger.debug("\n******* volume *********");
        for (Double d : vol.keySet()) {
            String h=vol.get(d);
            logger.debug(formatter.format(d) + "\t" + formatter.format(dsharpe.get(h))+ "\t" + formatter.format(dsharpe2.get(h))+ "\t" + namemap.get(h));
        }
        logger.debug("\n******* volume*excursion *********");
        for (Double d : volex.keySet()) {
            String h=volex.get(d);
            logger.debug(formatter.format(d) + "\t" + formatter.format(dsharpe.get(h))+ "\t" + formatter.format(dsharpe2.get(h))+ "\t" + namemap.get(h));
        }

        
        
    }

}
