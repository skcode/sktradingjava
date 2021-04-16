/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.intraday;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import static com.ettoremastrogiacomo.utils.JeneticsTemplates.Integer2Double;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import io.jenetics.Chromosome;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.util.IntRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;





/**
 *
 * @author root
 */
public class IntradayCloseOpenTradingTest3 {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayCloseOpenTradingTest3.class);   
    
    public static void main(String[] args) throws Exception {
        //final int MAXGAP=5,MINSAMPLE=100,MINDAYS=200;  
        final int POOLSIZE=1;
        final int TRAIN_WINDOW=100,TEST_WINDOW=5;
        final double LASTEQ=120000,FEE=7,spreadPEN=.001,maxpcgap=.2;
        final double INITEQ=LASTEQ;
        final int samples=250,maxdaygap=7,maxold=30,minvol=50000;

        Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_STOCK_EUR_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        TreeMap<UDate,Double> equity= new TreeMap<>();
        
        
        
        LOG.debug(ptf.dates.size());

        ArrayList<Double> profit= new ArrayList<>();
        ArrayList<Double> profit_train= new ArrayList<>();
        final UDate []darr=ptf.dates.toArray(new UDate[ptf.dates.size()]);        
        Fints d1=Fints.Diff(ptf.getClose(), ptf.getOpen());
        double[][] mat=Fints.DIV(d1, ptf.getOpen()).getMatrixCopy();
        if ((POOLSIZE*2)>=ptf.securities.size()) throw new Exception("POOLSIZE TOO BIG "+POOLSIZE);
        

        
        for (int i=0;i<(darr.length-TRAIN_WINDOW-TEST_WINDOW);i=i+TEST_WINDOW) {
            UDate[] traindates=ptf.dates.subList(i, i+TRAIN_WINDOW).toArray(new UDate[TRAIN_WINDOW]);//    subSet(darr[i], darr[i+TRAIN_WINDOW]).toArray(new UDate[TRAIN_WINDOW]);
            LOG.debug(traindates.length);
            UDate[] testdates=ptf.dates.subList(i+TRAIN_WINDOW, i+TRAIN_WINDOW+TEST_WINDOW).toArray(new UDate[TEST_WINDOW]);
            LOG.debug(testdates.length);
            double[][] trainmat=DoubleDoubleArray.sub(mat, i, 0, i+TRAIN_WINDOW-1, mat[0].length-1);
            LOG.debug(DoubleDoubleArray.nRows(trainmat)+"\t"+DoubleDoubleArray.nCols(trainmat));
            
            double[][] testmat=DoubleDoubleArray.sub(mat, i+TRAIN_WINDOW, 0, i+TRAIN_WINDOW+TEST_WINDOW-1, mat[0].length-1);
            LOG.debug(DoubleDoubleArray.nRows(testmat)+"\t"+DoubleDoubleArray.nCols(testmat));
            double[] sumtrain=DoubleDoubleArray.sum(trainmat);
            double[] sumtest=DoubleDoubleArray.sum(testmat);
            Function<ArrayList<Integer>, Double> fitness = (arr) -> {
                int div=arr.size()/2;
                double d = 0;
                Integer[] iar=arr.toArray(new Integer[arr.size()]);                    
                for (int k=0;k<div;k++) {
                    d+=sumtrain[iar[k]];
                }            
                for (int k=div;k<iar.length;k++) {
                    d-=sumtrain[iar[k]];
                }            
                return d;
            };
            Function<ArrayList<Integer>, Double> fitnesstest = (arr) -> {
                int div=arr.size()/2;
                double d = 0;
                Integer[] iar=arr.toArray(new Integer[arr.size()]);                    
                for (int k=0;k<div;k++) {
                    d+=sumtest[iar[k]];
                }            
                for (int k=div;k<iar.length;k++) {
                    d-=sumtest[iar[k]];
                }            
                return d;
            };            
            Phenotype<IntegerGene, Double> fen=Integer2Double(IntRange.of(0, ptf.securities.size()-1), POOLSIZE*2, 20000, fitness);

            LOG.info(fen);
            ArrayList<Integer> bestarr=new ArrayList<>();
            fen.getGenotype().forEach((x)->bestarr.add(x.getGene().intValue()));
            LOG.info("test ="+fitnesstest.apply(bestarr));;
            profit.add(fitnesstest.apply(bestarr));
            System.out.println(fen);
            
        }
        LOG.info("media "+profit.stream().mapToDouble((f) -> f).average().getAsDouble());
        LOG.info("max "+profit.stream().mapToDouble((f) -> f).max().getAsDouble());
        LOG.info("min "+profit.stream().mapToDouble((f) -> f).min().getAsDouble());
        LOG.info("num tests "+profit.size());
        System.exit(0);
        
        

        
        
    }
}
