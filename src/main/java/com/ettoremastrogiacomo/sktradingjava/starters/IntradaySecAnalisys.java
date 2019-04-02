/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.HashMap;
import java.util.TreeSet;

/**
 *
 * @author sk
 */
public class IntradaySecAnalisys {
    static final org.apache.log4j.Logger LOG= org.apache.log4j.Logger.getLogger(IntradaySecAnalisys.class);
    public static void main(String[] args) throws Exception {
        String symbol="FCA",market="MLSE";
        
        String hashcode=Database.getHashcode(symbol, market);
        TreeSet<UDate> dates=Database.getIntradayDates(hashcode);
        Fints f=Database.getIntradayFintsQuotes(hashcode, dates.last());        
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.0000");
        //double [][] m=f.getMatrixCopy();
        int price=3,volume=4;
        Fints fprice=f.getSerieCopy(price);
        double max= f.getMax()[price],min=f.getMin()[price],maxdd=f.getMaxDD(price);
        LOG.debug("range "+min+"->"+max+"\trange%="+df.format(((max-min)/min)*100)+"\tmaxgap%="+df.format(f.getMaxAbsPercentValueGap(price)*100)+"\tmaxdd%="+df.format(maxdd*100));
        HashMap<String,Double> map=DoubleArray.LinearRegression(f.getSerieVector(price));
        LOG.debug("volume "+df.format(f.getSerieCopy(volume).getSums()[0]));
        LOG.debug("slope "+df.format(map.get("slope")));
        LOG.debug("intercept "+df.format(map.get("intercept")));
        LOG.debug("stderr "+df.format(map.get("stderr")));
        LOG.debug("runtest "+f.runsTest95(price));
        LOG.debug(f.toString());
        //fprice.merge(fprice.getLinReg(0)).plot(symbol, "price");
        
        
        
    }
}
