/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import java.util.Optional;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.*;
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
public class BestCov2 {
    
    
    static Logger logger = Logger.getLogger(BestCov2.class);
    
    public static void main(String[] args) throws Exception {
        int minsamples=600,maxdaygap=6,maxold=30,minvol=10,setmin=15,setmax=35,popsize=5000,ngen=500;
        double maxpcgap=.15;        
        boolean plot=false;
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createStockEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFSTOCKEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        logger.debug("no sec "+ptf.getNoSecurities());
        logger.debug("len "+ptf.getLength());
        UDate train_enddate=ptf.dates.get(ptf.dates.size()-1);
        UDate train_startdate=ptf.dates.get(ptf.dates.size()-500);
        double[] w=ptf.optimizeMinVarQP(Optional.of(500), Optional.of(0), Optional.of(.05));
        Entry<Double,ArrayList<Integer>> winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, Portfolio.optMethod.MINVAR, plot, popsize, ngen);
        logger.info("************************ optimization GA ************************ ");
        logger.info("BEST "+1.0/winner.getKey());
        logger.info("BEST "+winner.getValue());
        for (Integer x : winner.getValue()) {
            logger.debug( ptf.getName(ptf.hashcodes.get(x)));
        }
            logger.debug("\n\n");
        logger.info("************************ optimization QP ************************ ");            
        for (int i=0;i<w.length;i++) {
            if (w[i]>0.001)
            logger.debug( ptf.getName(ptf.hashcodes.get(i))+"\t"+w[i]);
        }
        Fints rif=Fints.ER(Portfolio.createFintsFromPortfolio(ptf, "campione"), 100, true);
        Fints all=Fints.merge(rif, ptf.closeERlog);
        double [][] cov=all.getCorrelation();
        StringBuilder sb= new StringBuilder();
        TreeMap<Double,String> covmap=new TreeMap<>();
        String del=";";
        sb.append("CORR-MATRIX");
        all.getName().forEach((s) -> {
            sb.append(del).append(s);
        });
        sb.append("\n");
        for (int i=0;i<all.getNoSeries();i++) {
            sb.append(all.getName(i));
            for (int j=0;j<all.getNoSeries();j++){
                sb.append(del).append(cov[i][j]);
                if (i==0) {
                    covmap.put(cov[i][j], all.getName(j)+"\t"+ (j>0? ptf.realnames.get(j-1):""));
                }
                
            }        
            sb.append("\n");
        }
        File file = new File("./covanalisys.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        }                
        logger.info("************************ CORRELATION ranking ************************ ");
        covmap.keySet().forEach((x)->logger.debug(x+"\t"+covmap.get(x)));
    }
    
    
}
