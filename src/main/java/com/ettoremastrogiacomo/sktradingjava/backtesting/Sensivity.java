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
import org.apache.log4j.Logger;

/**
 *
 * @author sk
 */
public class Sensivity {
    final HashMap < ArrayList<Double>,Double> params;
    HashMap < AbstractMap.SimpleEntry< Integer,Integer> ,Double> distance= new HashMap<>();
    double stddist=0,meandist=0,maxdist=Double.MIN_VALUE,mindist=Double.MAX_VALUE;    
    double[][] mat;
    static Logger LOG = Logger.getLogger(Sensivity.class);
    public Sensivity(HashMap <ArrayList<Double>,Double> params){
        this.params=params;
        mat=new double[params.size()][params.keySet().stream().findFirst().get().size()+1];
        int rcnt=0;
        for (ArrayList<Double> v1: this.params.keySet()) {
            int ccnt=0;
            for (Double v2: v1) {
                mat[rcnt][ccnt]=v2;ccnt++;
            }
            mat[rcnt][ccnt]=params.get(v1);
        }
        
        for (int i=0;i<mat.length;i++) {
            for (int j=0;j<mat.length;j++) {
                double d=0;
                for (int k=0;k<(mat[i].length-1);k++)                    
                        d+=Math.pow(mat[i][k]-mat[j][k], 2) ;
                distance.put(new AbstractMap.SimpleEntry<>(i,j), Math.sqrt(d));
            }            
            LOG.debug("step "+i+" of "+mat.length);
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
