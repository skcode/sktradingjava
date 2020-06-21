/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.intraday;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author sk
 */
public class IntradayReport {

    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayReport.class);

    public static void main(String[] args) throws Exception {
        StringBuilder sb=new StringBuilder();
        TreeSet<UDate> intradayDates= Database.getIntradayDates();
        UDate lastdate=intradayDates.last();
        
        Set<String> list=Database.getIntradayHashCodes(Optional.of(lastdate));
        HashMap<String,String> names=Database.getCodeMarketName(new ArrayList<>(list));
        String delimiter=";";
        
        sb.append("name").append(delimiter).append("daysamples").append(delimiter).append("lastday").append(delimiter);
        sb.append("range-mean").append(delimiter).append("range-min").append(delimiter).append("range-max").append(delimiter).append("range-last").append(delimiter);
        sb.append("volume-mean").append(delimiter).append("volume-min").append(delimiter).append("volume-max").append(delimiter).append("volume-last").append(delimiter);
        sb.append("samples-mean").append(delimiter).append("samples-min").append(delimiter).append("samples-max").append(delimiter).append("samples-last").append(delimiter);
        sb.append("zval-mean").append(delimiter).append("zval-min").append(delimiter).append("zval-max").append(delimiter).append("zval-last").append(delimiter);
        sb.append("corrlag-mean").append(delimiter).append("corrlag-min").append(delimiter).append("corrlag-max").append(delimiter).append("corrlag-last").append(delimiter);
        sb.append("corrlag-mean1").append(delimiter).append("corrlag-min1").append(delimiter).append("corrlag-max1").append(delimiter).append("corrlag-last1").append(delimiter);        
        sb.append("corrlag-mean3").append(delimiter).append("corrlag-min3").append(delimiter).append("corrlag-max3").append(delimiter).append("corrlag-last3").append(delimiter);        
        sb.append("corrlag-mean5").append(delimiter).append("corrlag-min5").append(delimiter).append("corrlag-max5").append(delimiter).append("corrlag-last5").append(delimiter);        
        sb.append("corrlag-mean10").append(delimiter).append("corrlag-min10").append(delimiter).append("corrlag-max10").append(delimiter).append("corrlag-last10").append(delimiter);        
        sb.append("corrlag-mean15").append(delimiter).append("corrlag-min15").append(delimiter).append("corrlag-max15").append(delimiter).append("corrlag-last15").append(delimiter);        
        sb.append("volat-mean").append(delimiter).append("volat-min").append(delimiter).append("volat-max").append(delimiter).append("volat-last").append(delimiter);
        sb.append("maxdd-mean").append(delimiter).append("maxdd-min").append(delimiter).append("maxdd-max").append(delimiter).append("maxdd-last").append(delimiter);
        sb.append("closeopen-mean").append(delimiter).append("closeopen-min").append(delimiter).append("closeopen-max").append(delimiter).append("closeopen-last").append("\n");
        
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.0000");        
        for (String x: list){
            String hashcode = x;//Database.getHashcode(symbol, market);            
            int price = 3, volume = 4;
            TreeSet<UDate> dates = Misc.mostRecentTimeSegment(Database.getIntradayDates(hashcode), 1000*60*60*24*5);
            if (dates.size()<100) continue;
            java.util.TreeMap<UDate,Double> rangemap=new java.util.TreeMap<>();
            java.util.TreeMap<UDate,Double> volumemap=new java.util.TreeMap<>();
            java.util.TreeMap<UDate,Double> samplesmap=new java.util.TreeMap<>();
            java.util.TreeMap<UDate,Double> zvalmap=new java.util.TreeMap<>();
            java.util.TreeMap<UDate,Double> corrlagmap=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> corrlagmap1=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> corrlagmap3=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> corrlagmap5=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> corrlagmap10=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> corrlagmap15=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> volatmap=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> maxddmap=new java.util.TreeMap<>();        
            java.util.TreeMap<UDate,Double> closeopen=new java.util.TreeMap<>(); 
            String name=names.get(hashcode);
            if (!name.contains("STOCK")) continue;
            for (UDate d : dates) {
                Fints f = Database.getIntradayFintsQuotes(hashcode, d);
                if (f.getLength()<100) continue;
                Fints f1=Security.changeFreq(f, Fints.frequency.MINUTE).getSerieCopy(price);
                Fints f3=Security.changeFreq(f, Fints.frequency.MINUTES3).getSerieCopy(price);
                Fints f5=Security.changeFreq(f, Fints.frequency.MINUTES5).getSerieCopy(price);
                Fints f10=Security.changeFreq(f, Fints.frequency.MINUTES10).getSerieCopy(price);
                Fints f15=Security.changeFreq(f, Fints.frequency.MINUTES15).getSerieCopy(price);                                
                Fints fprice = f.getSerieCopy(price);
                Fints logprice = Fints.ER(fprice, 100, true);
                Fints logprice1 = Fints.ER(f1, 100, true);
                Fints logprice3 = Fints.ER(f3, 100, true);
                Fints logprice5 = Fints.ER(f5, 100, true);
                Fints logprice10 = Fints.ER(f10, 100, true);
                Fints logprice15 = Fints.ER(f15, 100, true);                
                Fints lagged = logprice.merge(Fints.Lag(logprice, 1));
                Fints lagged1 = logprice1.merge(Fints.Lag(logprice1, 1));
                Fints lagged3 = logprice3.merge(Fints.Lag(logprice3, 1));
                Fints lagged5 = logprice5.merge(Fints.Lag(logprice5, 1));
                Fints lagged10 = logprice10.merge(Fints.Lag(logprice10, 1));
                Fints lagged15 = logprice15.merge(Fints.Lag(logprice15, 1));                
                LOG.debug("analizing "+name+" day "+d);
                double max = f.getMax()[price], min = f.getMin()[price], maxdd = f.getMaxDD(price);
                HashMap<String, Double> map = DoubleArray.LinearRegression(f.getSerieVector(price));
                rangemap.put(d, ((max - min) / min) * 100);
                volumemap.put(d, f.getSerieCopy(volume).getSums()[0]);
                samplesmap.put(d,(double)f.getLength());
                zvalmap.put(d, f.runTestZscore(price));
                corrlagmap.put(d, lagged.getCorrelation()[0][1]);
                corrlagmap1.put(d, lagged1.getCorrelation()[0][1]);
                corrlagmap3.put(d, lagged3.getCorrelation()[0][1]);
                corrlagmap5.put(d, lagged5.getCorrelation()[0][1]);
                corrlagmap10.put(d, lagged10.getCorrelation()[0][1]);
                corrlagmap15.put(d, lagged15.getCorrelation()[0][1]);
                volatmap.put(d, map.get("stderr"));
                maxddmap.put(d, maxdd*100);
                closeopen.put(d, 100*(fprice.getLastRow()[0]-fprice.get(0, 0))/fprice.getLastRow()[0]);
                //fprice.merge(fprice.getLinReg(0)).plot(symbol, "price");
            }
            if (rangemap.isEmpty()) continue;
            sb.append(name).append(delimiter).append(dates.size()).append(delimiter).append(lastdate.toYYYYMMDD()).append(delimiter);
            Fints f1=new Fints(Misc.mapConvert(rangemap), Arrays.asList("rangemap"), Fints.frequency.DAILY);
            sb.append(f1.getMeans()[0]).append(delimiter).append(f1.getMin()[0]).append(delimiter).append(f1.getMax()[0]).append(delimiter).append(f1.getLastRow()[0]).append(delimiter);
            Fints f2=new Fints(Misc.mapConvert(volumemap), Arrays.asList("volumemap"), Fints.frequency.DAILY);
            sb.append(f2.getMeans()[0]).append(delimiter).append(f2.getMin()[0]).append(delimiter).append(f2.getMax()[0]).append(delimiter).append(f2.getLastRow()[0]).append(delimiter);
            Fints f3=new Fints(Misc.mapConvert(samplesmap), Arrays.asList("samplesmap"), Fints.frequency.DAILY);
            sb.append(f3.getMeans()[0]).append(delimiter).append(f3.getMin()[0]).append(delimiter).append(f3.getMax()[0]).append(delimiter).append(f3.getLastRow()[0]).append(delimiter);            
            Fints f4=new Fints(Misc.mapConvert(zvalmap), Arrays.asList("zvalmap"), Fints.frequency.DAILY);
            sb.append(f4.getMeans()[0]).append(delimiter).append(f4.getMin()[0]).append(delimiter).append(f4.getMax()[0]).append(delimiter).append(f4.getLastRow()[0]).append(delimiter);            
            Fints f5=new Fints(Misc.mapConvert(corrlagmap), Arrays.asList("corrlagmap"), Fints.frequency.DAILY);
            sb.append(f5.getMeans()[0]).append(delimiter).append(f5.getMin()[0]).append(delimiter).append(f5.getMax()[0]).append(delimiter).append(f5.getLastRow()[0]).append(delimiter);            

            Fints f5_1=new Fints(Misc.mapConvert(corrlagmap1), Arrays.asList("corrlagmap"), Fints.frequency.DAILY);
            sb.append(f5_1.getMeans()[0]).append(delimiter).append(f5_1.getMin()[0]).append(delimiter).append(f5_1.getMax()[0]).append(delimiter).append(f5_1.getLastRow()[0]).append(delimiter);            
            Fints f5_3=new Fints(Misc.mapConvert(corrlagmap3), Arrays.asList("corrlagmap"), Fints.frequency.DAILY);
            sb.append(f5_3.getMeans()[0]).append(delimiter).append(f5_3.getMin()[0]).append(delimiter).append(f5_3.getMax()[0]).append(delimiter).append(f5_3.getLastRow()[0]).append(delimiter);            
            Fints f5_5=new Fints(Misc.mapConvert(corrlagmap5), Arrays.asList("corrlagmap"), Fints.frequency.DAILY);
            sb.append(f5_5.getMeans()[0]).append(delimiter).append(f5_5.getMin()[0]).append(delimiter).append(f5_5.getMax()[0]).append(delimiter).append(f5_5.getLastRow()[0]).append(delimiter);            
            Fints f5_10=new Fints(Misc.mapConvert(corrlagmap10), Arrays.asList("corrlagmap"), Fints.frequency.DAILY);
            sb.append(f5_10.getMeans()[0]).append(delimiter).append(f5_10.getMin()[0]).append(delimiter).append(f5_10.getMax()[0]).append(delimiter).append(f5_10.getLastRow()[0]).append(delimiter);            
            Fints f5_15=new Fints(Misc.mapConvert(corrlagmap15), Arrays.asList("corrlagmap"), Fints.frequency.DAILY);
            sb.append(f5_15.getMeans()[0]).append(delimiter).append(f5_15.getMin()[0]).append(delimiter).append(f5_15.getMax()[0]).append(delimiter).append(f5_15.getLastRow()[0]).append(delimiter);            

            
            Fints f6=new Fints(Misc.mapConvert(volatmap), Arrays.asList("volatmap"), Fints.frequency.DAILY);
            sb.append(f6.getMeans()[0]).append(delimiter).append(f6.getMin()[0]).append(delimiter).append(f6.getMax()[0]).append(delimiter).append(f6.getLastRow()[0]).append(delimiter);            
            Fints f7=new Fints(Misc.mapConvert(maxddmap), Arrays.asList("maxddmap"), Fints.frequency.DAILY);
            sb.append(f7.getMeans()[0]).append(delimiter).append(f7.getMin()[0]).append(delimiter).append(f7.getMax()[0]).append(delimiter).append(f7.getLastRow()[0]).append(delimiter);            
            Fints f8=new Fints(Misc.mapConvert(closeopen), Arrays.asList("maxddmap"), Fints.frequency.DAILY);
            sb.append(f8.getMeans()[0]).append(delimiter).append(f8.getMin()[0]).append(delimiter).append(f8.getMax()[0]).append(delimiter).append(f8.getLastRow()[0]).append("\n");            
            LOG.debug(name);
            LOG.debug("SAMPLES DAYS "+dates.size());
            LOG.debug("rangemap% mean "+f1.getMeans()[0]+"\tmin "+f1.getMin()[0]+"\tmax "+f1.getMax()[0]+"\tlast "+f1.getLastRow()[0]);
            LOG.debug("volumemap mean "+f2.getMeans()[0]+"\tmin "+f2.getMin()[0]+"\tmax "+f2.getMax()[0]+"\tlast "+f2.getLastRow()[0]);
            LOG.debug("samplesmap mean "+f3.getMeans()[0]+"\tmin "+f3.getMin()[0]+"\tmax "+f3.getMax()[0]+"\tlast "+f3.getLastRow()[0]);
            LOG.debug("zvalmap mean "+f4.getMeans()[0]+"\tmin "+f4.getMin()[0]+"\tmax "+f4.getMax()[0]+"\tlast "+f4.getLastRow()[0]);
            LOG.debug("corrlagmap mean "+f5.getMeans()[0]+"\tmin "+f5.getMin()[0]+"\tmax "+f5.getMax()[0]+"\tlast "+f5.getLastRow()[0]);
LOG.debug("corrlagmap1 mean "+f5_1.getMeans()[0]+"\tmin "+f5_1.getMin()[0]+"\tmax "+f5_1.getMax()[0]+"\tlast "+f5_1.getLastRow()[0]);
LOG.debug("corrlagmap3 mean "+f5_3.getMeans()[0]+"\tmin "+f5_3.getMin()[0]+"\tmax "+f5_3.getMax()[0]+"\tlast "+f5_3.getLastRow()[0]);
LOG.debug("corrlagmap5 mean "+f5_5.getMeans()[0]+"\tmin "+f5_5.getMin()[0]+"\tmax "+f5_5.getMax()[0]+"\tlast "+f5_5.getLastRow()[0]);
LOG.debug("corrlagmap10 mean "+f5_10.getMeans()[0]+"\tmin "+f5_10.getMin()[0]+"\tmax "+f5_10.getMax()[0]+"\tlast "+f5_10.getLastRow()[0]);
LOG.debug("corrlagmap15 mean "+f5_15.getMeans()[0]+"\tmin "+f5_15.getMin()[0]+"\tmax "+f5_15.getMax()[0]+"\tlast "+f5_15.getLastRow()[0]);
            LOG.debug("volatmap mean "+f6.getMeans()[0]+"\tmin "+f6.getMin()[0]+"\tmax "+f6.getMax()[0]+"\tlast "+f6.getLastRow()[0]);
            LOG.debug("maxddmap% mean "+f7.getMeans()[0]+"\tmin "+f7.getMin()[0]+"\tmax "+f7.getMax()[0]+"\tlast "+f7.getLastRow()[0]);
            LOG.debug("closeopen% mean "+f8.getMeans()[0]+"\tmin "+f8.getMin()[0]+"\tmax "+f8.getMax()[0]+"\tlast "+f8.getLastRow()[0]);
        }
        File file = new File("./intradaysecanalisys.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        }        
    }
}
