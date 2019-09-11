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
         
         for (UDate d: dates){
            if (!v.containsKey(UDate.roundMinute(d))) v.put(UDate.roundMinute(d), f.get(f.getIndex(d), i));            
         }
         Fints ret=new Fints(v, Arrays.asList(f.getName(0)), Fints.frequency.MINUTE);
         logger.debug(ret);
         return  ret;
     }
     
     public static void main(String [] args) throws Exception {                  
         int limitsamples=500;
         TreeMap<UDate,ArrayList<String>> map=Database.getIntradayDatesReverseMap();
         UDate last=map.lastEntry().getKey();
         Fints all= new Fints();
         for ( String x: map.lastEntry().getValue()){             
             try{
             Fints f=sec2min(Database.getIntradayFintsQuotes(x, last), 0);             
             if (f.getLength()>=limitsamples) all= all.isEmpty()? all=f:Fints.merge(all, f);
             } catch (Exception e){}
         }
         logger.debug(all);
         Fints er=Fints.ER(all, 100, true);
         Fints erlag=Fints.Lag(er, 1);
         
         logger.debug("\n"+DoubleDoubleArray.toString(er.getCovariance()) );
         logger.debug("\n"+DoubleDoubleArray.toString(er.getCorrelation()) );
         logger.debug("\n"+DoubleDoubleArray.toString(er.merge(erlag).getCorrelation()) );
     }
}
