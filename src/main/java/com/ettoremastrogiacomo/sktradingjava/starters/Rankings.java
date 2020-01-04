/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import java.util.Optional;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.*;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
/**
 *
 * @author a241448
 */
public class Rankings {
    
    
    static Logger logger = Logger.getLogger(Rankings.class);
    
    public static void main(String[] args) throws Exception {
        int minsamples=300,maxdaygap=6,maxold=30,minvol=10000,setmin=15,setmax=50,popsize=10000,ngen=500;
        double maxpcgap=.15;        
        boolean plot=false;
        Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createStockEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFSTOCKEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        int SIZE=minsamples<ptf.getLength()?minsamples:ptf.getLength()-1;
        logger.debug("no sec "+ptf.getNoSecurities());
        logger.debug("len "+ptf.getLength());
        UDate train_enddate=ptf.dates.get(ptf.dates.size()-1);
        UDate train_startdate=ptf.dates.get(ptf.dates.size()-SIZE);
        //double[] w=ptf.optimizeMinVarQP(Optional.of(minsamples<ptf.getLength()?minsamples:ptf.getLength()-1), Optional.of(0), Optional.of(.05));
        Entry<Double,ArrayList<Integer>> winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, Portfolio.optMethod.MINVAR, plot, popsize, ngen);
        logger.info("************************ optimization GA ************************ ");
        logger.info(train_startdate+"\tto\t"+train_enddate+"\tsamples "+ptf.closeER.Sub(train_startdate, train_enddate).getLength());
        logger.info("setmin "+setmin+"\tsetmax "+setmax);
        logger.info("BEST "+1.0/winner.getKey());
        logger.info("BEST "+winner.getValue());
        double[]w=new double[winner.getValue().size()];
        DoubleArray.fill(w, 1.0/winner.getValue().size());        
        logger.info("BEST LOG VAR "+ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("LOG VAR CAMPIONE "+Fints.ER(ptf.closeCampione, 100, true).head(SIZE).getCovariance()[0][0]);
        logger.info("BEST LEN "+winner.getValue().size());
        for (Integer x : winner.getValue()) {
            logger.info( ptf.getName(ptf.hashcodes.get(x)));
        }
        logger.debug("\n\n");
        
        
        TreeMap<Double,String> corrmap= new TreeMap<>();
        TreeMap<Double,String> betamap= new TreeMap<>();
        TreeMap<Double,String> varmap=new TreeMap<>();
        for (int i=0;i<ptf.getNoSecurities();i++) {
            betamap.put(ptf.getBeta(i, SIZE), ptf.realnames.get(i));
            corrmap.put(ptf.getCorrelation(i, SIZE), ptf.realnames.get(i));
            varmap.put(Math.pow(ptf.closeERlog.getSerieCopy(i).head(SIZE).getStd()[0], 2), ptf.realnames.get(i));
        }
        logger.info("************************ VAR ranking ************************ ");
        varmap.keySet().forEach((x)-> logger.info(x+"\t"+varmap.get(x)));   
        Misc.map2csv(varmap, "./varmap.csv");
        logger.info("************************ CORRELATION ranking ************************ ");
        corrmap.keySet().forEach((x)-> logger.info(x+"\t"+corrmap.get(x)));
        Misc.map2csv(corrmap, "./corrmap.csv");
        logger.info("************************ BETA ranking ************************ ");
        betamap.keySet().forEach((x)-> logger.info(x+"\t"+betamap.get(x)));
        Misc.map2csv(betamap, "./betamap.csv");
    }
    
    
}
