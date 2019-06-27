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
 

    public static void main(String[] args) throws Exception {
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE","XETRA","EURONEXT")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes= new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        int trainwin=250,testwin=60,sec=10;
        long epochs=10000000L;
        ArrayList<String> list=Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(1500), Optional.of(.15), Optional.of(10), Optional.empty(), Optional.of(500000), Optional.empty());        
        Portfolio ptf= new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        ptf.walkForwardTest(Optional.of(trainwin), Optional.of(testwin), Optional.of(epochs), Optional.of(sec),Optional.of(Portfolio.optMethod.MAXPROFIT));
    }
}
