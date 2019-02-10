/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.TreeSet;

/**
 *
 * @author sk
 */
public class IntradaySecAnalisys {
    static final org.apache.log4j.Logger LOG= org.apache.log4j.Logger.getLogger(IntradaySecAnalisys.class);
    public static void main(String[] args) throws Exception {
        String symbol="FCT",market="MLSE";
        
        String hashcode=Database.getHashcode(symbol, market);
        TreeSet<UDate> dates=Database.getIntradayDates(hashcode);
        Fints f=Database.getIntradayFintsQuotes(hashcode, dates.last());        
        LOG.debug(f.toStringL());
        f.getSerieCopy(3).plot(symbol, "price");
        LOG.debug("volume "+f.getSerieCopy(4).getSums()[0]);
        
        
    }
}
