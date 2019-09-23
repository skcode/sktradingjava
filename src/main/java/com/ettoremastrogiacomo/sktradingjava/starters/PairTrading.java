/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;
import io.jenetics.util.IntRange;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
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
     /*
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

     static Fints tick2minutesb(Fints f,int span) throws Exception{         
         List<UDate> dates=f.getDate();
         TreeMap<UDate,ArrayList<Double>> v=new TreeMap<>();
         if (f.getFrequency()!=Fints.frequency.SECOND) throw new Exception("must be sec freq");         
         double open,high,low,close,vol,oi;
         UDate current=new UDate(0);
         for (UDate d: dates){     
             if (current.before(UDate.roundXMinutes(d,span))){
                 current=UDate.roundXMinutes(d,span);
                 low=close=high=open=f.get(f.getIndex(d), 0);
                 vol=f.get(f.getIndex(d), 4);oi=vol=f.get(f.getIndex(d), 5);
             }
            if (!v.containsKey(UDate.roundXMinutes(d,span))) {
                high=low=close=open=f.get(f.getIndex(d), 0);                
                //ArrayList<Double> t1=new ArrayList<>();
                //for (int i=0;i<f.getNoSeries();i++) t1.add(f.get(f.getIndex(d), i));
                //v.put(UDate.roundXMinutes(d,span), t1);            
            } else {            
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

     
     */
     public static void main(String [] args) throws Exception {                  
         int limitsamples=300;
         Fints.frequency fq=Fints.frequency.MINUTE;
         TreeMap<UDate,ArrayList<String>> map=Database.getIntradayDatesReverseMap();         
         
         UDate[] datearray= (UDate[])map.keySet().stream().toArray(UDate[]::new);
         UDate last=datearray[datearray.length-6];
         ArrayList<Fints> all= new ArrayList<>();
         ArrayList<Fints> poseq= new ArrayList<>();
         ArrayList<Fints> negeq= new ArrayList<>();         
         ArrayList<String> names= new ArrayList<>();
         ArrayList<String> hash= new ArrayList<>();
         HashMap<String,String> nmap= Database.getCodeMarketName(map.lastEntry().getValue());
         for ( String x: map.lastEntry().getValue()){             
             try{
                 if (!nmap.get(x).contains("STOCK")) continue;
                 Fints t1=Database.getIntradayFintsQuotes(x, last);
                 if (t1.getLength()<limitsamples) continue;
                names.add(nmap.get(x));
                hash.add(x);
             t1=Fints.createContinuity(Security.changeFreq(t1,fq));                  
             all.add(t1.getSerieCopy(3));          
             poseq.add(t1.getSerieCopy(3).getEquity());
             negeq.add(t1.getSerieCopy(3).getShortEquity());
             logger.debug(t1);
             } catch (Exception e){}             
         }
         
         int epochs=1000000,pool=3;
         double best=Double.NEGATIVE_INFINITY;
         for (int k=0;k<epochs;k++){
             if ((k % 10000)==0) logger.info("epoch "+k);
             List<Integer> set=Misc.set2list(Misc.getDistinctRandom(pool*2, all.size())) ;
             //[49, 19, 38, 23, 25, 12]	
             //List<Integer> set= Arrays.asList(49, 19, 38, 23, 25, 12);
             Fints f=new Fints();
             for (int i=0;i<set.size();i++) {
                 if (i<pool ) {
                     if (f.isEmpty()) f=poseq.get(set.get(i));
                     else f=f.merge(poseq.get(set.get(i)));
                 }else{
                     f=f.merge(negeq.get(set.get(i)));
                 }
             }
             Fints mc=Fints.MEANCOLS(f);
             HashMap<String,Double> m=DoubleArray.LinearRegression(mc.getCol(0));
             double slope=Math.abs(m.get("slope"));
             double sharpe=Math.abs(m.get("sharpe"));             
             double max=mc.getMaxAbs()[0];
             double mean=mc.getMeans()[0];
             double std=mc.getStd()[0];
             double lastval=mc.getLastRow()[0];
             double fitness=Math.pow(10, max)/Math.abs(1-lastval);
             if (fitness>best) {
                 String bt="";
                 for (int i=0;i<set.size();i++)bt+=names.get(set.get(i))+";";
                 best=fitness;
                 logger.info("new best "+set+"\t"+best+"\t"+bt);
                mc.plot("eq "+best, "gain");
             }
             
         }
         
         logger.debug("size "+hash.size());

         if (true) return;
         
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
