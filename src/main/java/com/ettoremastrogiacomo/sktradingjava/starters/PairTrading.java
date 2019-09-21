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

     static Fints tick2minutes(Fints f,int span) throws Exception{         
         List<UDate> dates=f.getDate();
         TreeMap<UDate,ArrayList<Double>> v=new TreeMap<>();
         if (f.getFrequency()!=Fints.frequency.SECOND) throw new Exception("must be sec freq");         
         for (UDate d: dates){
            if (!v.containsKey(UDate.roundXMinutes(d,span))) {
                ArrayList<Double> t1=new ArrayList<>();
                for (int i=0;i<f.getNoSeries();i++) t1.add(f.get(f.getIndex(d), i));
                v.put(UDate.roundXMinutes(d,span), t1);            
            }
         }
         double[][]m= new double[v.size()][f.getNoSeries()];
         int i=0;
         for (UDate d: v.keySet()){
             m[i]=v.get(d).stream().mapToDouble(Double::doubleValue).toArray();
             i++;
         }
         Fints ret=new Fints(new ArrayList(v.keySet()),  f.getName(), Fints.frequency.MINUTE,m);         
         return  ret;
     }


     
     public static void main(String [] args) throws Exception {                  
         int limitsamples=400,span=1;
         TreeMap<UDate,ArrayList<String>> map=Database.getIntradayDatesReverseMap();
         
         UDate last=map.lastEntry().getKey();
         ArrayList<Fints> all= new ArrayList<>();
         ArrayList<String> names= new ArrayList<>();
         ArrayList<String> hash= new ArrayList<>();
         HashMap<String,String> nmap= Database.getCodeMarketName(map.lastEntry().getValue());
         
         
         double tcorr=0.3;
         for ( String x: map.lastEntry().getValue()){             
             try{
                 Fints t1=Database.getIntradayFintsQuotes(x, last);
                 if (t1.getLength()<limitsamples) continue;
                Fints f=tick2minutes(t1, span);                          
                //Fints f=t1;                          
                all.add(f.getSerieCopy(3));
                names.add(nmap.get(x));
                hash.add(x);
             //if (f.getName(0).contains("MINIFTSEMIB")) ftsemibfut=f;
             //else all.add(f);             
             } catch (Exception e){}             
         }
         for (Fints f1: all) {
             for (Fints f2: all) {
                 int i1=all.indexOf(f1),i2=all.indexOf(f2);
                 Fints f1er=Fints.ER(f1, 100, true);
                 Fints f2er=Fints.ER(f2, 100, true);
                 Fints er=Fints.merge(f1er, f2er);
                 Fints erlag=Fints.Lag(er, 1);//1 sample lag
                 er=er.merge(erlag);
                 double [][] corr=er.getCorrelation();
                 //if (!(corr[0][2]>tcorr || corr[0][3]>tcorr || corr[1][2]>tcorr|| corr[0][3]>tcorr)) continue;
                 
                 //Fints.Diff(f1er, f2er).plot(names.get(i1)+"."+names.get(i2), "price");
                 Fints diff=f1.merge(f2).getEquity().getDiffCols(0,1 );
                 
                 HashMap<String,Double> lreg=DoubleArray.LinearRegression(diff.getCol(0));
                 //if (Math.abs(diff.getMeans()[0])<0.04) continue;
                 if (Math.abs(lreg.get("slope"))<.0001) continue;
                 logger.info(er);
                 diff.merge(diff.getLinReg(0)) .plot("eqdiff", "val");
                 //DoubleDoubleArray.show(er.getCorrelation()) ;
                 logger.info(diff.getMeans()[0]+";"+lreg.get("slope")+"\t"+names.get(i1)+"."+hash.get(i1)+"\t"+names.get(i2)+"."+hash.get(i2));
                 
             }
         }
         
       /*  String h1="OxxI3YPeCq0IbTkh+zgksZM/wc8=",h2="Q0dhtaXCK8QgycFLNlPUsjexxhA=";
         for (UDate d: map.keySet()) {
             if (map.get(d).contains(h1) && map.get(d).contains(h2)){
                 Fints f1=tick2minutes(Database.getIntradayFintsQuotes(h1, d).getSerieCopy(3),span);
                 Fints f2=tick2minutes(Database.getIntradayFintsQuotes(h2, d).getSerieCopy(3),span);
                 Fints er=Fints.merge(Fints.ER(f1, 100, true), Fints.ER(f2, 100, true));
                 Fints erlag=Fints.Lag(er, 1);//1 sample lag
                 er=er.merge(erlag);
                 double [][] corr=er.getCorrelation();
                 //if (!(corr[0][2]>tcorr || corr[0][3]>tcorr || corr[1][2]>tcorr|| corr[0][3]>tcorr)) continue;
                 logger.info(er);
                 DoubleDoubleArray.show(er.getCorrelation()) ;
                 
             }
         }*/
         //IG.MLSE.STOCK.EUR.IT0005211237.ITALGAS	ENEL.MLSE.STOCK.EUR.IT0003128367.ENEL
         
         //Fints ftsemibfuter=Fints.ER(ftsemibfut, 100, true);
     }
}
