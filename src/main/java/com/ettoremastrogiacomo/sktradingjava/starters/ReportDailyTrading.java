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
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

    static HashMap<String, String> runWF(Portfolio ptf, int trainwin, int testwin, optMethod opt, Optional<Boolean> duplicates, Optional<Integer> optSetMin, Optional<Integer> optSetMax, Optional<Integer> populationSize, Optional<Integer> generations, Optional<Boolean> plot) throws Exception {
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
        if (plot.orElse(false)) {
            alleq.plot("equity train=" + trainwin + "  test=" + testwin + "  fitness=" + opt.toString() + "  efficiency=" + String.valueOf(efficiency), "profit");
        }
        Fints fcorr=Fints.ER(alleq.SubSeries(new ArrayList<>(Arrays.asList(0,1))), 100, true);        
        logger.debug("eq correlation with BH\n"+fcorr.getCorrelation()[0][1]);
        logger.debug("eq covariance \n"+fcorr.getCovariance()[0][0]);
        logger.debug("eq covariance BH\n"+fcorr.getCovariance()[1][1]);
        return results;
    }

    public static void main(String[] args) throws Exception {

        int minvol = 1000, minvolETF = 100;
        int maxold = 30;
        int maxdaygap = 10;
        double maxgap = .2;
        int minlen = 2000, minlenETF = 2000;
        boolean duplicates = false;
        int minoptset = 10, maxoptset = 25;
        int popsize = 10000;
        int ngens = 500;
        int trainfrom = 500, trainto = 500, trainstep = 1;
        int testfrom = 500, testto = 500, teststep = 1;
        optMethod opt = optMethod.MINDD;
        boolean plot = true;
        boolean appendtofile = true;
        //suboptsetmax;efficiency;trainwin;profitBH;totalset;maxdd;duplicate;suboptsetmin;optmethod;testwin;profit;maxddBH;total_samples;
        //best4stock 25;0.19039180728796914;65;2.4501394052044057;146;-0.27898161753310985;false;7;MAXSLOPE;60;5.015622075926776;-0.40446019283299295;2968;
        ArrayList<HashMap<String, String>> l = new ArrayList<>();
        Portfolio ptfSTOCK = Portfolio.createStockEURPortfolio(Optional.of(minlen), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        Portfolio ptfETF = Portfolio.createETFSTOCKEURPortfolio(Optional.of(minlenETF), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvolETF));
        try ( BufferedWriter bwr = new BufferedWriter(new FileWriter(new File("./test.txt"), true))) {//append mode            
            for (int i = trainfrom; i <= trainto; i = i + trainstep) {//train win
                for (int j = testfrom; j <= testto && j <= i; j = j + teststep) {//test win
                    HashMap<String, String> m = runWF(ptfSTOCK, i, j, opt, Optional.of(duplicates), Optional.of(minoptset), Optional.of(maxoptset), Optional.of(popsize), Optional.of(ngens), Optional.of(plot));
                    if (appendtofile) {
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
                        bwr.flush();
                    }
                    l.add(m);

                }
            }
        }
        //bwr.close();
        l.forEach((x) -> System.out.println(x));
    }
}
