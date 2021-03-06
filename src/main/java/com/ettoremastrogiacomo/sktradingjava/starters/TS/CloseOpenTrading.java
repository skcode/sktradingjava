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
import java.util.TreeSet;
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
//leggo dati mercati euro
        ArrayList<String> markets = Database.getMarkets();
        ArrayList<String> marketMLSE = new ArrayList<> (Arrays.asList("MLSE"));
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(marketMLSE), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.empty());
        Portfolio ptf=new Portfolio(list, Optional.of(Fints.frequency.DAILY), Optional.empty(), Optional.empty(), Optional.empty());
        TreeMap<UDate,ArrayList<Double>> equity= new TreeMap<>();                        
        LOG.debug(ptf.dates.size());
        ArrayList<Double> profit= new ArrayList<>();
        //costruisco matrice differenza % tra chiusura e apertura
        Fints close, open;
        close=ptf.getClose();
        open=ptf.getOpen();        
        //Fints d1=Fints.Diff(close, open);
        final UDate []darr=open.getDate().toArray(new UDate[open.getDate().size()]);        
        //double[][] mat=Fints.DIV(d1, open).getMatrixCopy();
        double[][] mat= new double[close.getLength()][close.getNoSeries()];
        double[][] openmat= new double[close.getLength()][close.getNoSeries()];
        double[][] closemat= new double[close.getLength()][close.getNoSeries()];
        for (int i=0;i<mat.length;i++)
            for (int j=0;j<mat[i].length;j++){
                mat[i][j]=(close.get(i, j)-open.get(i, j))/open.get(i, j);
                openmat[i][j]=open.get(i, j);
                closemat[i][j]=close.get(i, j);
            }
        if ((POOLSIZE*2)>=ptf.securities.size()) throw new Exception("POOLSIZE TOO BIG "+POOLSIZE);        
        //inizializzo equity
        equity.put(darr[TRAIN_WINDOW-1], new ArrayList<Double>() {{ add(INITEQ);}});
        
        for (int i=0;i<(darr.length-TRAIN_WINDOW-TEST_WINDOW);i=i+TEST_WINDOW) {
            LOG.debug("********************************************");
            UDate[] traindates=open.getDate().subList(i, i+TRAIN_WINDOW).toArray(new UDate[TRAIN_WINDOW]);//    subSet(darr[i], darr[i+TRAIN_WINDOW]).toArray(new UDate[TRAIN_WINDOW]);
            LOG.debug("train from "+traindates[0] +" to "+traindates[traindates.length-1]+"\t len="+traindates.length);
            UDate[] testdates=open.getDate().subList(i+TRAIN_WINDOW, i+TRAIN_WINDOW+TEST_WINDOW).toArray(new UDate[TEST_WINDOW]);
            LOG.debug("test from "+testdates[0] +" to "+testdates[testdates.length-1]+"\t len="+testdates.length);            
            double[][] trainmat=DoubleDoubleArray.sub(mat, i, 0, i+TRAIN_WINDOW-1, mat[0].length-1);
            double[][] testmat=DoubleDoubleArray.sub(mat, i+TRAIN_WINDOW, 0, i+TRAIN_WINDOW+TEST_WINDOW-1, mat[0].length-1);
            double[][] testopenmat=DoubleDoubleArray.sub(openmat, i+TRAIN_WINDOW, 0, i+TRAIN_WINDOW+TEST_WINDOW-1, openmat[0].length-1);
            double[][] testclosemat=DoubleDoubleArray.sub(closemat, i+TRAIN_WINDOW, 0, i+TRAIN_WINDOW+TEST_WINDOW-1, closemat[0].length-1);
            
            
            LOG.debug("testmat shape "+DoubleDoubleArray.nRows(testmat)+"\t"+DoubleDoubleArray.nCols(testmat));
            double[] sumtrain=DoubleDoubleArray.sum(trainmat);
            double[] sumtest=DoubleDoubleArray.sum(testmat);
            Function<ArrayList<Integer>, Double> fitness = (arr) -> {
                int div=arr.size()/2;
                double d = 0;                
                Integer[] iar=arr.toArray(new Integer[arr.size()]);                    
                TreeSet<Integer> iarts= new TreeSet<Integer> ();
                iarts.addAll(arr);
                if (iarts.size()!=arr.size()) return d;//check duplicates
                for (int k=0;k<div;k++) {
                    d+=sumtrain[iar[k]];
                }            
                for (int k=div;k<iar.length;k++) {
                    d-=sumtrain[iar[k]];
                }            
                return d/arr.size();
            };
            
            Function<ArrayList<Integer>, Double> fitnesstest = (arr) -> {
                int div=arr.size()/2;
                double d = 0;                
                Integer[] iar=arr.toArray(new Integer[arr.size()]);                                    
                for (int k=0;k<div;k++) {                    
                    LOG.debug("LONG "+ptf.realnames.get(iar[k]));
                    for (int j=0;j<testmat.length;j++) {
                        d+=testmat[j][iar[k]];
                        LOG.debug(testdates[j].toYYYYMMDD()+"\t"+testmat[j][iar[k]]+"\topen:"+testopenmat[j][iar[k]]+"\tclose:"+testclosemat[j][iar[k]]);
                    }
                    
                }            
                
                
                for (int k=div;k<iar.length;k++) {
                    LOG.debug("SHORT "+ptf.realnames.get(iar[k]));
                    for (int j=0;j<testmat.length;j++) {
                        d-=testmat[j][iar[k]];
                        LOG.debug(testdates[j].toYYYYMMDD()+"\t"+(-testmat[j][iar[k]])+"\topen:"+testopenmat[j][iar[k]]+"\tclose:"+testclosemat[j][iar[k]]);
                    }                                        
                }            
                d/=iar.length;
                d=d-spreadPEN*testmat.length*2;
                ArrayList<Double> ad1=new ArrayList<>();
                ad1.add(equity.lastEntry().getValue().get(0)+INITEQ*d-FEE*iar.length*2*testmat.length);
                equity.put(testdates[testmat.length-1],ad1 );
                
                /*for (int j=0;j<testmat.length;j++){
                    double v=0;
                    for (int k=0;k<div;k++) v+=testmat[j][iar[k]];
                    for (int k=div;k<iar.length;k++) v-=testmat[j][iar[k]];
                    v/=iar.length;
                    LOG.debug("check "+v+"\t"+d);
                    ArrayList<Double> ad1=new ArrayList<>();
                    ad1.add(equity.lastEntry().getValue().get(0)*(1+d-spreadPEN*iar.length)-FEE*iar.length*2);
                    equity.put(testdates[j],ad1 );
                }*/
                return d;
            };  
            
            Phenotype<IntegerGene, Double> fen=Integer2Double(IntRange.of(0, ptf.securities.size()-1), POOLSIZE*2, 20000, fitness);

            ArrayList<Integer> bestarr=new ArrayList<>();
            fen.getGenotype().forEach((x)->bestarr.add(x.getGene().intValue()));
            LOG.info("test ="+fitnesstest.apply(bestarr));;
            profit.add(fitnesstest.apply(bestarr));
            LOG.info("cum sum profits "+profit.stream().mapToDouble((f) -> f).summaryStatistics().getSum());
            LOG.info("after "+profit.size()*TEST_WINDOW+" days");
            LOG.info(fen);
            LOG.debug("********************************************");
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
