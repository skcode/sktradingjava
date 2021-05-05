/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

/**
 *
 * @author sk
 */


import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import static com.ettoremastrogiacomo.sktradingjava.starters.SmartPortfolio.LOG;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.json.JSONObject;


public class CompareBETA {
    static Logger logger = Logger.getLogger(CompareBETA.class );
    public static void main(String[] args) throws Exception {
        int minsamples=1000,maxdaygap=7,maxold=10,minvol=100000,minvoletf=1000,setsize=20;
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
        TreeMap<Double,String> betamap2msciworldNames= new TreeMap<>();
        for (String h: ptf.hashcodes){
            Fints f=Database.getFintsQuotes(h).getSerieCopy(Security.SERIE.CLOSE.getValue());
            Fints fm=Fints.ER(Fints.merge(f, msciworld), 100, true);
            if (fm.getLength()<SIZE) continue;            
            double[][] c = fm.head(SIZE).getCovariance();
            double beta= c[0][1] / c[0][0];
            betamap2msciworld.put(beta, h);
            
        }
        betamap2msciworld.keySet().forEach((x)->betamap2msciworldNames.put(x, ptf.getName(betamap2msciworld.get(x))));
        ArrayList<String> newhashes=new ArrayList<>();
        JSONObject json= new JSONObject();
        json.put("samples", SIZE);
        json.put("minvol", minvol);
        json.put("size", betamap2msciworld.size());
        json.put("betamaphash", betamap2msciworld);
        json.put("betamapnames", betamap2msciworldNames);
        Misc.writeStringToFile(json.toString(), UDate.now().toYYYYMMDDHHmmss()+".beta.json");
        betamap2msciworld.headMap(0.3,true).keySet().forEach((x)->{
            logger.info(ptf.getName(betamap2msciworld.get(x))+"\t"+x);               
            newhashes.add(betamap2msciworld.get(x));
        });
        Portfolio newptf= new Portfolio(newhashes,Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty());
        Fints eq=Fints.merge(newptf.closeCampione, msciworld).getEquity();
        eq.plot("confronto con msciwrld", "val");        
        //betamap2msciworld.keySet().forEach((x)->{logger.debug(betamap2msciworld.get(x)+"\t"+x);});
        Fints fm=Fints.ER(Fints.merge(newptf.closeCampione, msciworld), 100, true);
        double[][] c = fm.head(SIZE).getCovariance();
        double beta= c[0][1] / c[0][0];
        logger.info("beta ="+beta);
        logger.info("mindd ptf="+eq.getMaxDD(0));
        logger.info("mindd msci="+eq.getMaxDD(1));
        logger.info("sharpe ptf="+fm.getSharpe()[0]);
        logger.info("sharpe msci="+fm.getSharpe()[1]);            
    }
}
