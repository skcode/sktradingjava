/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import java.util.Optional;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.*;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
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
        int minsamples=1000,maxdaygap=7,maxold=10,minvol=10000,minvoletf=1000;
        double maxpcgap=.3;           
        Fints msciworld=Database.getFintsQuotes(Database.getHashcode("XMWO", "MLSE")).getSerieCopy(Security.SERIE.CLOSE.getValue());        
        //String sp500hash=Database.getHashcode("CSSPX", "MLSE");        
        boolean plot=false;
        Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_STOCK_EUR_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_STOCK_NYSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFSTOCKEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_exCOMMODITIES_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_NYSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_GLOBALI_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        
        int SIZE=minsamples<ptf.getLength()?minsamples:ptf.getLength()-1;
        TreeMap<Double,String> betamap2msciworld= new TreeMap<>();
        for (String h: ptf.hashcodes){
            Fints f=Database.getFintsQuotes(h).getSerieCopy(Security.SERIE.CLOSE.getValue());
            Fints fm=Fints.ER(Fints.merge(f, msciworld), 100, true);
            if (fm.getLength()<SIZE) continue;            
            double[][] c = fm.head(SIZE).getCovariance();
            double beta= c[0][1] / c[0][0];
            betamap2msciworld.put(beta, ptf.getName(h));
            logger.info(ptf.getName(h)+"\t"+beta);             
        }
        betamap2msciworld.keySet().forEach((x)->{logger.debug(betamap2msciworld.get(x)+"\t"+x);});
        System.exit(0);
        logger.info("no sec "+ptf.getNoSecurities());
        logger.info("len "+ptf.getLength());
        logger.info("minvol "+minvol);
        logger.info("minvoletf "+minvoletf);
        logger.info("start date "+ptf.getDate(0)+"\tend date "+ptf.getDate(ptf.getLength()-1));        
        TreeMap<Double,String> corrmap= new TreeMap<>();
        TreeMap<Double,String> betamap= new TreeMap<>();
        TreeMap<Double,String> varmap=new TreeMap<>();
        TreeMap<Double,String> minddmap=new TreeMap<>();
        for (int i=0;i<ptf.getNoSecurities();i++) {
            //betamap.put(ptf.getBeta (i, SIZE), ptf.realnames.get(i));
            //corrmap.put(ptf.getCorrelation(i, SIZE), ptf.realnames.get(i));
            betamap.put(ptf.getBeta(i,SIZE), ptf.realnames.get(i));
            corrmap.put(ptf.getCorrelation(i,SIZE), ptf.realnames.get(i));
            varmap.put(Math.pow(ptf.closeERlog.getSerieCopy(i).head(SIZE).getStd()[0], 2), ptf.realnames.get(i));
            minddmap.put(ptf.getClose().getSerieCopy(i).getMaxDD(0),ptf.realnames.get(i));
        }
        Misc.map2csv(minddmap, "./minddmap.csv",Optional.empty());
        //logger.info("************************ VAR ranking ************************ ");
        //varmap.keySet().forEach((x)-> logger.info(x+"\t"+varmap.get(x)));   
        Misc.map2csv(varmap, "./varmap.csv",Optional.empty());
        //logger.info("************************ CORRELATION ranking ************************ ");
        //corrmap.keySet().forEach((x)-> logger.info(x+"\t"+corrmap.get(x)));
        Misc.map2csv(corrmap, "./corrmap.csv",Optional.empty());
        //logger.info("************************ BETA ranking ************************ ");
        //betamap.keySet().forEach((x)-> logger.info(x+"\t"+betamap.get(x)));
        Misc.map2csv(betamap, "./betamap.csv",Optional.empty());
        //logger.info("************************ SHARPE ranking ************************ ");
        double [] smasharpev=Fints.SMA(Fints.Sharpe(ptf.closeERlog, 20), 200).getLastRow();
        TreeMap<Double,String> smasharpemap= new TreeMap<>();
        for (int i=0;i<smasharpev.length;i++) smasharpemap.put(smasharpev[i], ptf.realnames.get(i));
        //smasharpemap.keySet().forEach((x)-> logger.info(x+"\t"+smasharpemap.get(x)));
        Misc.map2csv(smasharpemap, "./sharpeamap.csv",Optional.empty());        

    }
    
    
}
