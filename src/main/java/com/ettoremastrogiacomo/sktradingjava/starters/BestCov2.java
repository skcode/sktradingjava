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
import java.util.ArrayList;
import java.util.Map.Entry;
/**
 *
 * @author a241448
 */
public class BestCov2 {
    
    
    static Logger logger = Logger.getLogger(BestCov2.class);
    
    public static void main(String[] args) throws Exception {
        int minsamples=1000;
        double maxpcgap=.2;
        
        Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createStockEURPortfolio(Optional.of(1000), Optional.of(.2), Optional.of(6), Optional.of(10), Optional.of(10000));
        logger.debug("no sec "+ptf.getNoSecurities());
        logger.debug("len "+ptf.getLength());
        UDate train_enddate=ptf.dates.get(ptf.dates.size()-1);
        UDate train_startdate=ptf.dates.get(ptf.dates.size()-500);
        double[] w=ptf.optimizeMinVarQP(Optional.of(500), Optional.of(0), Optional.of(.05));
        Entry<Double,ArrayList<Integer>> winner=ptf.opttrain(train_startdate, train_enddate, 10, 30, Portfolio.optMethod.MINVAR, false, 5000, 500);
        logger.debug("BEST "+1.0/winner.getKey());
        logger.debug("BEST "+winner.getValue());
        for (Integer x : winner.getValue()) {
            logger.debug( ptf.getName(ptf.unmodifiable_hashcodes.get(x)));
        }
            logger.debug("\n\n");
        for (int i=0;i<w.length;i++) {
            if (w[i]>0.001)
            logger.debug( ptf.getName(ptf.unmodifiable_hashcodes.get(i))+"\t"+w[i]);
        }
        
        
    }
    
    
}
