/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.Portfolio.optMethod;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class ReportDailyTrading {
    static Logger logger = Logger.getLogger(ReportDailyTrading.class);
 
    
    static HashMap<String,String> checkStock(int trainwin,int testwin, optMethod opt, Optional<Boolean> duplicates,Optional<Integer> optSet) throws Exception {        
        HashMap<String,String> results= new HashMap<>();
        results.put("trainwin", String.valueOf(trainwin)  );
        results.put("testwin", String.valueOf(testwin));    
        results.put("optmethod", opt.toString());    
        int minvol=5000;
        int maxold=10;
        int popsize=10000;
        int ngen=1000;
        int maxdaygap=10;
        double maxgap=.2;
        int minlen=3000;  
        boolean dup=duplicates.orElse(Boolean.FALSE);
        results.put("duplicate", String.valueOf(dup));   
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE","XETRA","EURONEXT")), Optional.empty(), Optional.empty());
        ArrayList<String> hashcodes= new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list=Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(minlen), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.empty());                       
        Portfolio ptf= new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        int sec=optSet.orElse(ptf.getNoSecurities()/10);
        results.put("suboptset",String.valueOf(sec));
        results.put("totalset",String.valueOf(ptf.getNoSecurities()));
        results.put("total_samples",String.valueOf(ptf.getLength()));
        Fints alleq=ptf.walkForwardTest(Optional.of(trainwin), Optional.of(testwin), Optional.of(popsize), Optional.of(ngen), Optional.of(sec),Optional.of(dup),Optional.of(opt));    
        double efficiency=((alleq.getLastValueInCol(0)-alleq.getLastValueInCol(1))/alleq.getLastValueInCol(1))*(alleq.getMaxDD(1)/alleq.getMaxDD(0))/Math.log(alleq.getLength());        
        results.put("profit", String.valueOf(alleq.getLastValueInCol(0)));
        results.put("maxdd", String.valueOf(alleq.getMaxDD(0)));
        results.put("profitBH", String.valueOf(alleq.getLastValueInCol(1)));
        results.put("maxddBH", String.valueOf(alleq.getMaxDD(1)));        
        results.put("efficiency", String.valueOf(efficiency));
        return results;
    }
    
    static HashMap<String,String> checkETF(int trainwin,int testwin, optMethod opt, Optional<Boolean> duplicates,Optional<Integer> optSet) throws Exception {
        
        HashMap<String,String> results= new HashMap<>();
        results.put("trainwin", String.valueOf(trainwin)  );
        results.put("testwin", String.valueOf(testwin));    
        results.put("optmethod", opt.toString());    
        int minvol=500;
        int maxold=10;
        int popsize=20000;
        int ngen=2000;
        int maxdaygap=10;
        double maxgap=.2;
        int minlen=2000;  
        boolean dup=duplicates.orElse(Boolean.FALSE);
        results.put("duplicate", String.valueOf(dup));   
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and sector like '%CLASSE 2 IND AZIONARIO%' and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        ArrayList<String> hashcodes= new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list=Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(minlen), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.empty());                       
        Portfolio ptf= new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        int sec=optSet.orElse(ptf.getNoSecurities()/10);
        results.put("suboptset",String.valueOf(sec));
        results.put("totalset",String.valueOf(ptf.getNoSecurities()));
        results.put("total_samples",String.valueOf(ptf.getLength()));
        Fints alleq=ptf.walkForwardTest(Optional.of(trainwin), Optional.of(testwin), Optional.of(popsize), Optional.of(ngen), Optional.of(sec),Optional.of(dup),Optional.of(opt));    
        double efficiency=((alleq.getLastValueInCol(0)-alleq.getLastValueInCol(1))/alleq.getLastValueInCol(1))*(alleq.getMaxDD(1)/alleq.getMaxDD(0))/Math.log(alleq.getLength());        
        results.put("profit", String.valueOf(alleq.getLastValueInCol(0)));
        results.put("maxdd", String.valueOf(alleq.getMaxDD(0)));
        results.put("profitBH", String.valueOf(alleq.getLastValueInCol(1)));
        results.put("maxddBH", String.valueOf(alleq.getMaxDD(1)));        
        results.put("efficiency", String.valueOf(efficiency));
        return results;
        
        
       //ptf.walkForwardTest(Optional.of(trainwin), Optional.of(testwin), Optional.of(epochs), Optional.of(sec),Optional.of(Portfolio.optMethod.MINDD));    
    }


    public static void main(String[] args) throws Exception {
        //checkETF();
    checkETF(2000,100,optMethod.MINDD,Optional.of(false),Optional.of(10));
    if (true) return;
        ArrayList<HashMap<String,String>> l= new ArrayList<>();
        
        
        try (BufferedWriter bwr = new BufferedWriter(new FileWriter(new File("./test.txt")))) {
            for (int i=150;i<=250;i=i+10){
                for (int j=20;j<=60;j=j+10) {
                    HashMap<String,String> m=checkStock(i,j,optMethod.MAXSLOPE,Optional.of(false),Optional.of(10));
                    if (l.isEmpty()) {
                        for (String x: m.keySet()){ bwr.write(x+";");}                                    
                    }
                    bwr.write("\n");
                    for (String x: m.keySet()){ bwr.write(m.get(x)+";");}                                    
                    l.add(m);                
                    bwr.flush();                
                }
            }
        }
        //bwr.close();
        l.forEach((x)->System.out.println(x));
    }
}
