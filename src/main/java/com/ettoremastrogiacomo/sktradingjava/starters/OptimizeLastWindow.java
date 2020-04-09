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
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class OptimizeLastWindow {

    static Logger logger = Logger.getLogger(OptimizeLastWindow.class);

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
        Fints eq2=alleq.SubSeries(new ArrayList<>(Arrays.asList(0,1)));
        double[][] mdiff= new double[eq2.getLength()][1];
        mdiff[0][0]=1;
        for (int i=1;i<eq2.getLength();i++){
            mdiff[i][0]=mdiff[i-1][0]*(1+eq2.get(i, 0)/eq2.get(i-1, 0)-eq2.get(i, 1)/eq2.get(i-1, 1));
        }
        Fints fdiff= new Fints(eq2.getDate(), Arrays.asList("diffeq"), eq2.getFrequency(), mdiff);
        if (plot.orElse(false)) {
            fdiff.plot("difference", "return");
        }        
        Fints fcorr=Fints.ER(eq2, 100, true);        
        
        logger.debug("eq correlation with BH\n"+fcorr.getCorrelation()[0][1]);
        logger.debug("eq covariance \n"+fcorr.getCovariance()[0][0]);
        logger.debug("eq covariance BH\n"+fcorr.getCovariance()[1][1]);
        return results;
    }
    static boolean checkval(Double d){
        if (d.isInfinite() || d.isNaN()) return false;
        return true;
    }
    public static void sensivitytest() {
        ArrayList<ArrayList<String>> list=Misc.CSVreader("test.txt", ';', 13);
        HashMap<ArrayList<Double>,Double> map= new HashMap<>();
        for (ArrayList<String> l: list) {
            if (l.get(8).equals("MAXSLOPE")){
                ArrayList<Double> t1= new  ArrayList<>();
                Double v1=Double.valueOf(l.get(2)),v2=Double.valueOf(l.get(9)),v3=Double.valueOf(l.get(1));
                if (!checkval(v1) || !checkval(v2) || !checkval(v3)) continue;
                t1.add(v1);
                t1.add(v2);                
                map.put(t1, v3);            
            }
        }
        com.ettoremastrogiacomo.sktradingjava.backtesting.Sensivity s = new com.ettoremastrogiacomo.sktradingjava.backtesting.Sensivity(map,Optional.of(10));
        s.getRanking();        
    }
    
    
    
    public static void main(String[] args) throws Exception {
    //140-90-maxslope best 
        int minvol = 100000, minvolETF = 1000;
        int maxold = 30;
        int maxdaygap = 10;
        double maxgap = .2;
        int minlen = 1500, minlenETF = 1500;
        boolean duplicates = false;
        int minoptset = 10, maxoptset = 25;
        int popsize = 10000;
        int ngens = 500;
        int trainwin=1000;
        
        optMethod opt = optMethod.MAXSLOPE;
        ArrayList<HashMap<String, String>> l = new ArrayList<>();
        Portfolio ptf = Portfolio.createStockEURPortfolio(Optional.of(minlen), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf = Portfolio.createETFSTOCKEURPortfolio(Optional.of(minlenETF), Optional.of(maxgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvolETF));
        UDate startDate=ptf.getDate(ptf.getLength()-trainwin);
        UDate endDate=ptf.getDate(ptf.getLength()-1);
        logger.info("start at "+startDate);
        logger.info("end at "+endDate);
        logger.info("samples "+trainwin);
        Entry<Double,ArrayList<Integer>> res=ptf.opttrain(startDate, endDate, minoptset, maxoptset, opt, duplicates, popsize, ngens);
        logger.info("BEST VAL = "+res.getKey());
        ptf.list2names(res.getValue()).forEach((x)->logger.info(x));                
    }
}
