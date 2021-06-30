

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.backtesting;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.log4j.Logger;

/**
 *
 * @author sk
 */
public class Sensivity {
    final HashMap < ArrayList<Double>,Double> params;
    TreeSet < Double> distance= new TreeSet<>();
  
    static Double computedist(ArrayList<Double> v1, ArrayList<Double> v2){
        double d=0;                
        Double [] d1=v1.toArray(new Double[0]);
        Double [] d2=v2.toArray(new Double[0]);
        for (int i=0;i<d1.length;i++)                                
                d+=Math.pow(d1[i]-d2[i], 2) ;
        return Math.sqrt(d);                        
    }
    static Logger LOG = Logger.getLogger(Sensivity.class);
    public Sensivity(HashMap <ArrayList<Double>,Double> params,Optional<Integer> multiplier){
        this.params=params;
        for (ArrayList<Double> v1: params.keySet()) {
            for (ArrayList<Double> v2: params.keySet()) {                
                distance.add(computedist(v1, v2)); 
                if (distance.size()>(v1.size()*multiplier.orElse(7))) distance.remove(distance.last());
            }            
        }        
    }
    

    
    public void show(ArrayList<Double> v,double limit) {
            for (ArrayList<Double> v2:this.params.keySet()) {
                if (computedist(v, v2)<limit) {
                    LOG.debug(v2+"\t"+params.get(v2));
                }                
            }   
    }
    public TreeMap<Double,ArrayList<Double>> getRanking(){
        double LIM=distance.last();
        TreeMap<Double,ArrayList<Double>> ranking=new TreeMap<>();        
        for (ArrayList<Double> v1:this.params.keySet()){
            double d=0;int cnt=0;
            for (ArrayList<Double> v2:this.params.keySet()) {
                if (computedist(v1, v2)<LIM) {
                    d+=params.get(v2);cnt++;
                }                
            }   
            d=d/cnt;
            if (Double.isFinite(d)) ranking.put(d, v1);
        }        
         /*for (Double d: ranking.keySet()){
             LOG.debug(d+"\t"+ranking.get(d));
         }
         LOG.debug("last dist = "+distance.last());
         //show(ranking.lastEntry().getValue(),LIM);*/
        return ranking;
    }    
}
