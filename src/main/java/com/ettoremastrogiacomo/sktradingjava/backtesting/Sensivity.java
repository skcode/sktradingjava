/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.backtesting;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * @author sk
 */
public class Sensivity {
    final HashMap < ArrayList<Double>,Double> params;
    HashMap < AbstractMap.SimpleEntry< ArrayList<Double>,ArrayList<Double>> ,Double> distance;
    double stddist=0,meandist=0,maxdist=Double.MIN_VALUE,mindist=Double.MAX_VALUE;    
        static Double distance(ArrayList<Double> v1,ArrayList<Double> v2) {
            double d=0;
            for (Double d1: v1)
                for (Double d2: v2)
                    d+=(d1-d2)*(d1-d2);
            return Math.sqrt(d);
        }
        
    public Sensivity(HashMap < ArrayList<Double>,Double> params){
        this.params=params;
        for (ArrayList<Double> v1: this.params.keySet()) {
            for (ArrayList<Double> v2: this.params.keySet()) {
                distance.put(new AbstractMap.SimpleEntry<>(v1,v2), distance(v1,v2));
            }            
        }        
        for (Double v: distance.values()){
            meandist+=v;
            if (v>maxdist) maxdist=v;
            if (v<mindist && v!=0) mindist=v;
        }
        meandist=meandist/distance.size();
        for (Double v: distance.values()){
            stddist+=(v-meandist)*(v-meandist);
        }
        stddist=Math.sqrt(stddist);
    }
    
    public double getMeanDist() {return this.meandist;}
    public double getMaxDist() {return this.maxdist;}
    public double getMinDist() {return this.mindist;}
    public double getStdDist() {return this.stddist;}
    
    public TreeMap<Double,ArrayList<Double>> getRanking(Double limit){
        TreeMap<Double,ArrayList<Double>> ranking=new TreeMap<>();
        for (ArrayList<Double> x:this.params.keySet()){
            double mean=0;int cnt=0;
            for ( AbstractMap.SimpleEntry e: distance.keySet()){
                if (e.getKey().equals(x) && distance.get(e)<=limit){
                    mean+=params.get(x);cnt++;
                }
            }
            ranking.put(mean/cnt, x);
        }
        return ranking;
    }    
}
