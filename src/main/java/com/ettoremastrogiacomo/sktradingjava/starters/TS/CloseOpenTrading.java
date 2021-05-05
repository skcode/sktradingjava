/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.TS;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import static com.ettoremastrogiacomo.utils.JeneticsTemplates.Integer2Double;
import com.ettoremastrogiacomo.utils.UDate;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.util.IntRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;





/**
 *
 * @author root
 */
public class CloseOpenTrading {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CloseOpenTrading.class);   
    
    public static void main(String[] args) throws Exception {
        //final int MAXGAP=5,MINSAMPLE=100,MINDAYS=200;  
        final int POOLSIZE=1;
        final int TRAIN_WINDOW=120,TEST_WINDOW=5;
        final double INITEQ=40000,FEE=7,spreadPEN=.001,maxpcgap=.2;
        final int samples=2000,maxdaygap=7,maxold=30,minvol=50000;

        ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.empty());
        Portfolio ptf=new Portfolio(list, Optional.of(Fints.frequency.DAILY), Optional.empty(), Optional.empty(), Optional.empty());
        TreeMap<UDate,ArrayList<Double>> equity= new TreeMap<>();                        
        LOG.debug(ptf.dates.size());
        ArrayList<Double> profit= new ArrayList<>();
        Fints close, open;
        close=ptf.getClose();
        open=ptf.getOpen();        
        Fints d1=Fints.Diff(close, open);
        final UDate []darr=d1.getDate().toArray(new UDate[d1.getDate().size()]);        
        double[][] mat=Fints.DIV(d1, open).getMatrixCopy();
        if ((POOLSIZE*2)>=ptf.securities.size()) throw new Exception("POOLSIZE TOO BIG "+POOLSIZE);        
        equity.put(darr[TRAIN_WINDOW-1], new ArrayList<Double>() {{ add(INITEQ);}});
        
        for (int i=0;i<(darr.length-TRAIN_WINDOW-TEST_WINDOW);i=i+TEST_WINDOW) {
            UDate[] traindates=d1.getDate().subList(i, i+TRAIN_WINDOW).toArray(new UDate[TRAIN_WINDOW]);//    subSet(darr[i], darr[i+TRAIN_WINDOW]).toArray(new UDate[TRAIN_WINDOW]);
            LOG.debug(traindates.length);
            UDate[] testdates=d1.getDate().subList(i+TRAIN_WINDOW, i+TRAIN_WINDOW+TEST_WINDOW).toArray(new UDate[TEST_WINDOW]);
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
                for (int k=0;k<div;k++) LOG.debug("LONG "+ptf.realnames.get(iar[k]));
                for (int k=0;k<div;k++) {                    
                    for (int j=0;j<testmat.length;j++) {
                        d+=testmat[j][iar[k]];
                        //LOG.debug(testdates[j].toYYYYMMDD()+"\t"+testmat[j][iar[k]]);
                    }
                    
                }            
                for (int k=div;k<iar.length;k++) LOG.debug("SHORT "+ptf.realnames.get(iar[k]));
                
                for (int k=div;k<iar.length;k++) {
                    for (int j=0;j<testmat.length;j++) {
                        d-=testmat[j][iar[k]];
                       // LOG.debug(testdates[j].toYYYYMMDD()+"\t"+(-testmat[j][iar[k]]));
                    }                                        
                }            
                
                for (int j=0;j<testmat.length;j++){
                    double v=0;
                    for (int k=0;k<div;k++) v+=testmat[j][iar[k]];
                    for (int k=div;k<iar.length;k++) v-=testmat[j][iar[k]];
                    v/=iar.length;
                    ArrayList<Double> ad1=new ArrayList<>();
                    ad1.add(equity.lastEntry().getValue().get(0)*(1+v-spreadPEN*iar.length)-FEE*iar.length*2);
                    equity.put(testdates[j],ad1 );
                }
                return d;
            };  
            
            Phenotype<IntegerGene, Double> fen=Integer2Double(IntRange.of(0, ptf.securities.size()-1), POOLSIZE*2, 20000, fitness);

            LOG.info(fen);
            ArrayList<Integer> bestarr=new ArrayList<>();
            fen.getGenotype().forEach((x)->bestarr.add(x.getGene().intValue()));
            LOG.info("test ="+fitnesstest.apply(bestarr));;
            profit.add(fitnesstest.apply(bestarr));
            LOG.info("cum sum profits "+profit.stream().mapToDouble((f) -> f).summaryStatistics().getSum());
            LOG.info(fen);
            
        }
        LOG.info("media "+profit.stream().mapToDouble((f) -> f).average().getAsDouble());
        LOG.info("max "+profit.stream().mapToDouble((f) -> f).max().getAsDouble());
        LOG.info("min "+profit.stream().mapToDouble((f) -> f).min().getAsDouble());
        LOG.info("prof ratio "+profit.stream().mapToDouble((f) -> f).average().getAsDouble()/Math.abs(profit.stream().mapToDouble((f) -> f).min().getAsDouble()));
        LOG.info("num tests "+profit.size());
        Fints f= new Fints(equity,Arrays.asList("equity"), Fints.frequency.DAILY);
        f.plot("equity", "val");
        
        

        
        
    }
}
