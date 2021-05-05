/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.TS;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 *
 * @author sk
 */
public class TS_longshort1 {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(TS_longshort1.class);
    public static void main(String[] args) throws Exception {
        final int POOLSIZE=1;
        final int TRAIN_WINDOW=60,TEST_WINDOW=20;
        final double INITEQ=10000,FEE=7,spreadPEN=.001,maxpcgap=.2;
        final int samples=1500,maxdaygap=7,maxold=30,minvol=1000000;        
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
        ArrayList<Double> profit= new ArrayList<>();
        Fints close, open;
        close=ptf.getClose();
        open=ptf.getOpen();        
        Fints d1=Fints.Diff(close, open);
        final UDate []darr=d1.getDate().toArray(new UDate[d1.getDate().size()]);        
        double[][] mat=Fints.DIV(d1, open).getMatrixCopy();
        if ((POOLSIZE*2)>=ptf.securities.size()) throw new Exception("POOLSIZE TOO BIG "+POOLSIZE);        
        //equity.put(darr[TRAIN_WINDOW-1], new ArrayList<Double>() {{ add(INITEQ);}});
        
        Fints ER=Fints.ER(close, 1, false);
        equity.put(ER.getFirstDate(), new ArrayList<Double>() {{ add(INITEQ);}});
        double[][] ermat=ER.getMatrixCopy();
        
        
        for (int i=0;i<ER.getLength();i=i+TEST_WINDOW){
            if ((i+TRAIN_WINDOW+TEST_WINDOW)>=ER.getLength()) break;
            LOG.info("------ day "+ER.getDate(i)+" --------");
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
            for (int k=0;k<ermat[0].length;k++) trainsharpe[k]=train[k]/trainv[k];
            int maxpos=DoubleArray.maxpos(trainsharpe);
            DoubleArray.fill(test, 1.0);

            for (int j=i+TRAIN_WINDOW;j<i+TRAIN_WINDOW+TEST_WINDOW;j++){
                test[maxpos]*=(1+ermat[j][maxpos]-ermat[j][idxFTSEMIB]);                     
            }

            //if (maxpos==idxFTSEMIB) continue;//flat
            ArrayList<Double> d= new ArrayList<>();     
            d.add(equity.lastEntry().getValue().get(0)*test[maxpos]);
            equity.put(ER.getDate(i+TRAIN_WINDOW+TEST_WINDOW-1), d);
        }
        Fints f= new Fints(equity,Arrays.asList("equity"), Fints.frequency.DAILY);
        f.plot("equity", "val");
        
    }
}
