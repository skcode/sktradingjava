/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class ReportDailyTrading {
    static Logger logger = Logger.getLogger(ReportDailyTrading.class);
 

    static void checkStock() throws Exception {
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE","XETRA","EURONEXT")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes= new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        int trainwin=1500,testwin=250,sec;
        int minvol=5000;
        int maxold=10;
        long epochs=2000000L;                                             
        int maxdaygap=10;
        double maxgap=.15;
        int minlen=2500;        
        ArrayList<String> list=Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(minlen), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.empty());               
        Portfolio ptf= new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        sec=ptf.getNoSecurities()/10;
        ptf.walkForwardTest(Optional.of(trainwin), Optional.of(testwin), Optional.of(epochs), Optional.of(sec),Optional.of(Portfolio.optMethod.MAXSHARPE));    
    }
    
    static void checkETF() throws Exception {
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and sector like '%CLASSE 2 IND AZIONARIO%' and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        //ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF-STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes= new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        int trainwin=1500,testwin=500,sec;
        int minvol=1000;
        int maxold=10;
        long epochs=1000000L;                                             
        int maxdaygap=10;
        double maxgap=.15;
        int minlen=2100;
        ArrayList<String> list=Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(minlen), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.empty());               
        Portfolio ptf= new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        sec=ptf.getNoSecurities()/10;
        //if (sec>10) sec=10;
        ptf.walkForwardTest(Optional.of(trainwin), Optional.of(testwin), Optional.of(epochs), Optional.of(sec),Optional.of(Portfolio.optMethod.MINDD));    
    }


    public static void main(String[] args) throws Exception {
        //checkETF();
        checkStock();
    }
}
