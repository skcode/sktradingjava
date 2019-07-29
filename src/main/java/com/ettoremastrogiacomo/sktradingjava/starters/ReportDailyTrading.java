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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class ReportDailyTrading {

    static Logger logger = Logger.getLogger(ReportDailyTrading.class);


    
    static HashMap<String, String> runWF(Portfolio ptf,int trainwin, int testwin, optMethod opt, Optional<Boolean> duplicates, Optional<Integer> optSetMin, Optional<Integer> optSetMax,Optional<Integer> populationSize,Optional<Integer> generations) throws Exception {
        HashMap<String, String> results = new HashMap<>();
        results.put("trainwin", String.valueOf(trainwin));
        results.put("testwin", String.valueOf(testwin));
        results.put("optmethod", opt.toString());
        int popsize = populationSize.orElse(10000);
        int ngen = generations.orElse(1000);
        boolean dup = duplicates.orElse(Boolean.FALSE);
        results.put("duplicate", String.valueOf(dup));
        int secmin = optSetMin.orElse(ptf.getNoSecurities() / 10);
        int secmax = optSetMax.orElse(ptf.getNoSecurities() / 10);
        results.put("suboptsetmin", String.valueOf(secmin));
        results.put("suboptsetmax", String.valueOf(secmax));
        results.put("totalset", String.valueOf(ptf.getNoSecurities()));
        results.put("total_samples", String.valueOf(ptf.getLength()));
        Fints alleq = ptf.walkForwardTest(Optional.of(trainwin), Optional.of(testwin), Optional.of(popsize), Optional.of(ngen), Optional.of(secmin), Optional.of(secmax), Optional.of(dup), Optional.of(opt));
        double efficiency = Portfolio.equityEfficiency(alleq, 0, 1); //((alleq.getLastValueInCol(0) - alleq.getLastValueInCol(1)) / alleq.getLastValueInCol(1)) * (alleq.getMaxDD(1) / alleq.getMaxDD(0)) / Math.log(alleq.getLength());
        results.put("profit", String.valueOf(alleq.getLastValueInCol(0)));
        results.put("maxdd", String.valueOf(alleq.getMaxDD(0)));
        results.put("profitBH", String.valueOf(alleq.getLastValueInCol(1)));
        results.put("maxddBH", String.valueOf(alleq.getMaxDD(1)));
        results.put("efficiency", String.valueOf(efficiency));
        alleq.plot("equity train="+trainwin+"  test="+testwin+"  fitness="+opt.toString()+"  efficiency="+String.valueOf(efficiency), "profit");
        return results;
    }


    public static void main(String[] args) throws Exception {

        int minvol = 5000,minvolETF=100;
        int maxold = 10;
        int maxdaygap = 10;
        double maxgap = .15;
        int minlen = 3000,minlenETF=2000; 
        boolean duplicates=false;
        int minoptset=7,maxoptset=25;
        int popsize=5000;
        int ngens=500;
        int trainfrom=65,trainto=65;
        int testfrom=50 ,testto=70;
        optMethod opt=optMethod.MAXSLOPE;
        //suboptsetmax;efficiency;trainwin;profitBH;totalset;maxdd;duplicate;suboptsetmin;optmethod;testwin;profit;maxddBH;total_samples;
        //best4stock 25;0.19039180728796914;65;2.4501394052044057;146;-0.27898161753310985;false;7;MAXSLOPE;60;5.015622075926776;-0.40446019283299295;2968;
        ArrayList<HashMap<String, String>> l = new ArrayList<>();
        Portfolio ptfSTOCK=Portfolio.createStockEURPortfolio(Optional.of(minlen),Optional.of(maxgap) , Optional.of(maxdaygap),Optional.of(maxold) ,Optional.of(minvol) );
        Portfolio ptfETF=Portfolio.createETFSTOCKEURPortfolio(Optional.of(minlenETF),Optional.of(maxgap) , Optional.of(maxdaygap),Optional.of(maxold) ,Optional.of(minvolETF) );
        try (BufferedWriter bwr = new BufferedWriter(new FileWriter(new File("./test.txt"),true))) {//append mode            
            for (int i =trainfrom; i <= trainto; i = i + 1) {//train win
                for (int j = testfrom; j <= testto; j = j + 1) {//test win
                    HashMap<String, String> m = runWF(ptfSTOCK,i, j, opt, Optional.of(duplicates), Optional.of(minoptset), Optional.of(maxoptset),Optional.of(popsize),Optional.of(ngens) );                    
                    if (l.isEmpty()) {
                        bwr.write("\n");
                        for (String x : m.keySet()) {
                            bwr.write(x + ";");
                        }
                    }
                    bwr.write("\n");
                    for (String x : m.keySet()) {
                        bwr.write(m.get(x) + ";");
                    }
                    l.add(m);
                    bwr.flush();
                }
            }
        }
        //bwr.close();
        l.forEach((x) -> System.out.println(x));
    }
}
