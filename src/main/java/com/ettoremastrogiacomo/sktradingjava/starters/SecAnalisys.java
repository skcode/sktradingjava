/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.apache.log4j.Logger;

/**
 *
 * @author sk
 */
public class SecAnalisys {
static public org.apache.log4j.Logger LOG= Logger.getLogger(SecAnalisys.class);

    public static void main(String[] args) throws Exception{
            //String symbol="STAW";//INA.EURONEXT-XLIS
            String symbol="XSPX";
            String market="MLSE";
            int window=2500;
            String hashcode=Database.getHashcode(symbol, market);
            HashMap<String,String> info=Database.getRecords(Optional.of(Arrays.asList(hashcode)), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()).get(0);
            info.keySet().forEach((x)->LOG.info(x+"\t"+info.get(x)));
            Security s=new com.ettoremastrogiacomo.sktradingjava.Security(hashcode);
            Fints f=s.getDaily().getSerieCopy(Security.SERIE.CLOSE.getValue());
            f=f.head(f.getLength()>window?window:f.getLength());
            f.plot(symbol+"."+market, "price");
            Fints eq=f.getEquity();
            HashMap<String,Double> lr=DoubleArray.LinearRegression(eq.getCol(0));
            lr.keySet().forEach((x)->LOG.info(x+"\t"+lr.get(x)));
            Fints equity=f.getEquity();
            equity.plot("equity", symbol);
            NumberFormat nf=NumberFormat.getInstance();
            nf.setMaximumFractionDigits(4);
            nf.setMinimumFractionDigits(2);
            Fints er=Fints.ER(f, 1, false);
            LOG.info("samples : "+f.getLength());
            LOG.info("equity last "+equity.getLastRow()[0]);
            double dailyRet=(Math.exp(Math.log(equity.getLastRow()[0])/equity.getLength())-1.0);
            double annualRet=(Math.exp(Math.log(equity.getLastRow()[0])/(equity.getLength()/252.0))-1.0);            
            LOG.info("daily return % "+nf.format(dailyRet*100));
            LOG.info("annual return % "+nf.format(annualRet*100));
            LOG.info("from "+f.getFirstDate()+" to "+f.getLastDate());
            LOG.info("mean annualized excess return : "+er.getMeans()[0]*252);
            LOG.info("std excess return : "+er.getStd()[0]*Math.sqrt(252));
            LOG.info("max "+f.getMax()[0]);
            LOG.info("min "+f.getMin()[0]);
            LOG.info("max drowdown "+nf.format(f.getMaxDD(0)*100)+"%");
            
            
            
            //Fints f=Database.getFintsQuotes(Optional.of(symbol), Optional.of(market),Optional.empty());
            
        
    }

}
