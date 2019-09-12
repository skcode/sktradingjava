/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 *
 * @author a241448
 */
public class PairTrading {
     static Logger logger = Logger.getLogger(PairTrading.class);
     static Fints sec2min(Fints f,int i) throws Exception{         
         List<UDate> dates=f.getDate();
         TreeMap<UDate,Double> v=new TreeMap<>();
         if (f.getFrequency()!=Fints.frequency.SECOND) throw new Exception("must be sec freq");
         if (f.getFrequency()==Fints.frequency.MINUTE) return f;
         int span=10;
         for (UDate d: dates){
            if (!v.containsKey(UDate.roundXMinutes(d,span))) v.put(UDate.roundXMinutes(d,span), f.get(f.getIndex(d), i));            
         }
         Fints ret=new Fints(v, Arrays.asList(f.getName(0)), Fints.frequency.MINUTE);
         logger.debug(ret);
         return  ret;
     }
     
     public static void main(String [] args) throws Exception {                  
         int limitsamples=400;
         TreeMap<UDate,ArrayList<String>> map=Database.getIntradayDatesReverseMap();
         map.remove(map.lastEntry().getKey(), map.lastEntry().getValue());
         UDate last=map.lastEntry().getKey();
         ArrayList<Fints> all= new ArrayList<>();
         Fints ftsemibfut=new Fints();
         double tcorr=0.3;
         for ( String x: map.lastEntry().getValue()){             
             try{
                 Fints t1=Database.getIntradayFintsQuotes(x, last);
                 if (t1.getLength()<limitsamples) continue;
                Fints f=sec2min(t1, 0);             
             
             if (f.getName(0).contains("MINIFTSEMIB")) ftsemibfut=f;
             else all.add(f);             
             } catch (Exception e){}             
         }
         Fints ftsemibfuter=Fints.ER(ftsemibfut, 100, true);
         for ( Fints f : all){             
             Fints er=Fints.merge(Fints.ER(f, 100, true), ftsemibfuter);         
             er=er.merge(Fints.Lag(er, 1));             
             double[][] corr=er.getCorrelation();
             if (!(corr[0][2]>tcorr || corr[0][3]>tcorr || corr[1][2]>tcorr|| corr[0][3]>tcorr)) continue;
             logger.info(er);
             DoubleDoubleArray.show(er.getCorrelation()) ;
         }
         
     }
}
