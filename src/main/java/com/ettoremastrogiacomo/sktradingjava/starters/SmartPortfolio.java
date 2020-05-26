/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import static com.ettoremastrogiacomo.sktradingjava.starters.Rankings.logger;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.log4j.Logger;

/**
 *
 * @author sk
 */
public class SmartPortfolio {
    static public org.apache.log4j.Logger LOG= Logger.getLogger(SmartPortfolio.class);
    
    public static void main(String[] args) throws Exception{        
        int minsamples=250,maxsamples=1500,stepsamples=250,maxdaygap=10,maxold=10,minvol=100000,minvoletf=1000,setmin=6,setmax=50,popsize=20000,ngen=1000;
        double maxpcgap=.15;      
        Portfolio.optMethod optm=Portfolio.optMethod.MAXSHARPE;
        boolean plot=false,plotlist=true;
        HashMap<String,Integer> list= new HashMap<>();
        HashMap<Integer,Double> meaneq= new HashMap<>();
        HashMap<Integer,Double> meanmaxdd= new HashMap<>();
        for (int samples=minsamples;samples<=maxsamples;samples=samples+stepsamples){
            Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_NYSE_Portfolio(Optional.of(samples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
            int SIZE=samples<ptf.getLength()?samples:ptf.getLength()-1;
            logger.info("************************ optimization GA "+optm.toString()+" ************************ ");
            logger.info("no sec "+ptf.getNoSecurities());
            logger.info("len "+ptf.getLength());
            logger.info("minvol "+minvol);
            logger.info("minvoletf "+minvoletf);
            logger.info("start date "+ptf.getDate(0)+"\tend date "+ptf.getDate(ptf.getLength()-1));        
            UDate train_enddate=ptf.dates.get(ptf.dates.size()-1);
            UDate train_startdate=ptf.dates.get(ptf.dates.size()-SIZE);
            setmax=setmax>ptf.getNoSecurities()?ptf.getNoSecurities():setmax;
            
            Map.Entry<Double,ArrayList<Integer>>winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, optm, plot, popsize, ngen);
            
            logger.info(train_startdate+"\tto\t"+train_enddate+"\tsamples "+ptf.closeER.Sub(train_startdate, train_enddate).getLength());
            logger.info("setmin "+setmin+"\tsetmax "+setmax);
            logger.info("BEST "+1.0/winner.getKey());
            logger.info("BEST "+winner.getValue());
            double[]w=new double[winner.getValue().size()];
            DoubleArray.fill(w, 1.0/winner.getValue().size());                    
            logger.info("BEST LOG VAR "+ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
            logger.info("BEST VAR check"+ptf.closeER.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
            logger.info("LOG VAR CAMPIONE "+Fints.ER(ptf.closeCampione, 100, true).head(SIZE).getCovariance()[0][0]);
            logger.info("BEST LEN "+winner.getValue().size());
            winner.getValue().forEach((x) -> {
                String t1=ptf.getName(ptf.hashcodes.get(x));
                logger.info( t1);
                if (list.containsKey(t1)) list.replace(t1, list.get(t1)+1);else list.put(t1, 1);                
            });
            Fints f=ptf.opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
            logger.info("MAXDD: "+f.getName(0)+"\t"+f.getMaxDD(0));
            logger.info("MAXDD: "+f.getName(1)+"\t"+f.getMaxDD(1));
            logger.info("Final EQ: "+f.getName(0)+"\t"+f.getLastValueInCol(0));
            logger.info("Final EQ: "+f.getName(1)+"\t"+f.getLastValueInCol(1));
            logger.debug("\n\n");   
            meaneq.put(samples, f.getLastValueInCol(0));
            meanmaxdd.put(samples, f.getMaxDD(0));
            f.plot(optm.toString()+" size="+SIZE+" maxdd="+f.getMaxDD(0)+" finaleq="+f.getLastValueInCol(0), "price");
            if (plotlist){
                for (Integer x : winner.getValue()){
                    ptf.getClose().getSerieCopy(x).plot(ptf.getName(ptf.hashcodes.get(x)), "price");
                }
                
            }
        
        }
        logger.info("LIST");
        list.keySet().forEach((x)->logger.info( x+"\t"+list.get(x)));        
        logger.info("MEAN EQUITY: "+meaneq.values().stream().mapToDouble(i-> i).average().orElse(Double.NaN));
        logger.info("MEAN MAXDD: "+meanmaxdd.values().stream().mapToDouble(i-> i).average().orElse(Double.NaN));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createStockEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFSTOCKEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
    
    
    }
}
