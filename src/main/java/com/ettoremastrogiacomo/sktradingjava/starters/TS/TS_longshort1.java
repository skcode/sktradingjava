/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.TS;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Sensivity;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 *
 * @author sk
 */
public class TS_longshort1 {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(TS_longshort1.class);
    
    static final double INITEQ=10000,FEE=7,spreadPEN=.002,maxpcgap=.2;
    static AbstractMap.SimpleEntry<ArrayList<Double>,Double > run(Fints ER, int TRAIN_WINDOW,int TEST_WINDOW, int idxFTSEMIB,int POOLSIZE,boolean plot) throws Exception {
                TreeMap<UDate,ArrayList<Double>> equity= new TreeMap<>();                                
                equity.clear();
                double[][] ermat=ER.getMatrixCopy();
                equity.put(ER.getFirstDate(), new ArrayList<Double>() {{ add(INITEQ);}});
                for (int i=0;i<ER.getLength();i=i+TEST_WINDOW){
                    if ((i+TRAIN_WINDOW+TEST_WINDOW)>=ER.getLength()) continue;                    
                    //ArrayList<Integer> sup=new ArrayList<>();
                    double[] train=new double[ermat[0].length];
                    double[] trainv=new double[ermat[0].length];            
                    double[] trainsharpe=new double[ermat[0].length];            
                    double[] test=new double[ermat[0].length];

                    for (int k=0;k<ermat[0].length;k++){
                        for (int j=i;j<i+TRAIN_WINDOW;j++){
                            train[k]+=ermat[j][k];                     
                        }
                        train[k]/=TRAIN_WINDOW;
                    }
                    for (int k=0;k<ermat[0].length;k++){
                        for (int j=i;j<i+TRAIN_WINDOW;j++){
                            trainv[k]+=Math.pow(train[k]-ermat[j][k], 2.0) ;                     
                        }
                        trainv[k]=Math.sqrt(trainv[k]/TRAIN_WINDOW);
                    }
                    TreeMap<Double,Integer> map= new TreeMap<>();
                    for (int k=0;k<ermat[0].length;k++) {
                        trainsharpe[k]=train[k]/trainv[k];
                        double fitness=trainsharpe[k];
                        if (idxFTSEMIB!=k) map.put(fitness, k);
                    }
                    
                    
                    //int maxpos=DoubleArray.maxpos(trainsharpe);
                    double erv=0;
                    for (int k=0;k<POOLSIZE;k++){
                        //if (map.isEmpty()) continue;
                        int maxpos= map.pollLastEntry().getValue();                    
                        for (int j=i+TRAIN_WINDOW;j<i+TRAIN_WINDOW+TEST_WINDOW;j++){
                            erv+=(ermat[j][maxpos]-ermat[j][idxFTSEMIB]);                     
                        }
                    }
                    erv/=POOLSIZE;
                    

                    //if (maxpos==idxFTSEMIB) continue;//flat
                    ArrayList<Double> d= new ArrayList<>();     
                    d.add(equity.lastEntry().getValue().get(0)*(1+erv)*(1-spreadPEN));
                    equity.put(ER.getDate(i+TRAIN_WINDOW+TEST_WINDOW-1), d);
                }
                Fints f= new Fints(equity,Arrays.asList("equity"), Fints.frequency.DAILY);
                if (plot) f.plot("equity "+TRAIN_WINDOW+"\t:\t"+TEST_WINDOW, "val");
                LOG.info("train "+TRAIN_WINDOW+"\ttest "+TEST_WINDOW);
                LOG.info("mindd "+f.getMaxDD(0));
                LOG.info("profit "+f.getLastValueInCol(0));
                ArrayList<Double> d=new ArrayList<> ();d.add((double)TRAIN_WINDOW);d.add((double)TEST_WINDOW);                
                
                
                return new AbstractMap.SimpleEntry<>(d,f.getLastValueInCol(0));    
                //return new AbstractMap.SimpleEntry<>(d,f.getMaxDD(0));    
    }
    
    
    public static void main(String[] args) throws Exception {
        final int POOLSIZE=2;
         int TRAIN_WINDOW,TEST_WINDOW;        
         int TRAIN_WINDOW_MAX=60,WINDOW_MIN=5,SENSIVITY=5;
        final int samples=2000,maxdaygap=7,maxold=30,minvol=100000;        
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.empty());
        String hashFTSEMIB=Database.getHashcode("ETFMIB", "MLSE");
        list.add(hashFTSEMIB);
        Portfolio ptf=new Portfolio(list, Optional.of(Fints.frequency.DAILY), Optional.empty(), Optional.empty(), Optional.empty());        
        int idxFTSEMIB=ptf.indexOf(hashFTSEMIB);
        TreeMap<UDate,ArrayList<Double>> equity= new TreeMap<>();                        
        LOG.debug(ptf);        
        Fints close=ptf.getClose();
        if ((POOLSIZE*2)>=ptf.securities.size()) throw new Exception("POOLSIZE TOO BIG "+POOLSIZE);        
        //equity.put(darr[TRAIN_WINDOW-1], new ArrayList<Double>() {{ add(INITEQ);}});
        
        Fints ER=Fints.ER(close, 1, false);
        
        double[][] ermat=ER.getMatrixCopy();
        HashMap<ArrayList<Double>,Double> results= new HashMap<>();
        
        
        for (int trw=WINDOW_MIN;trw<=TRAIN_WINDOW_MAX;trw=trw+1){
            for (int tew=WINDOW_MIN;tew<=trw;tew=tew+1) {
                TRAIN_WINDOW=trw;TEST_WINDOW=tew;
                AbstractMap.SimpleEntry<ArrayList<Double>,Double >  e=run(ER,TRAIN_WINDOW,TEST_WINDOW,idxFTSEMIB,POOLSIZE,false);
                results.put(e.getKey(), e.getValue());
            }
        }
        TreeMap<Double,ArrayList<Double>> ranking= new TreeMap<>();
        Sensivity s= new Sensivity(results,Optional.of(SENSIVITY)); 
        TreeMap<Double,ArrayList<Double>> rank=s.getRanking();
        
        results.keySet().forEach((x)->ranking.put(results.get(x), x));
        ranking.keySet().forEach((x)->{LOG.info(x+"\t"+ranking.get(x));});        
        LOG.info("best entry "+ranking.lastEntry().getValue()+"->"+ranking.lastEntry().getKey());
        TRAIN_WINDOW=(int)Math.round(ranking.lastEntry().getValue().get(0));
        TEST_WINDOW=(int)Math.round(ranking.lastEntry().getValue().get(1));
        LOG.info("winner : train "+TRAIN_WINDOW+" test: "+TEST_WINDOW);
        run(ER,TRAIN_WINDOW ,TEST_WINDOW,idxFTSEMIB,POOLSIZE,true);
        TRAIN_WINDOW=(int)Math.round(rank.lastEntry().getValue().get(0));
        TEST_WINDOW=(int)Math.round(rank.lastEntry().getValue().get(1));
        run(ER,TRAIN_WINDOW ,TEST_WINDOW,idxFTSEMIB,POOLSIZE,true);
        rank.keySet().forEach((x)->{LOG.info(x+"\t"+rank.get(x));});
    }
}
