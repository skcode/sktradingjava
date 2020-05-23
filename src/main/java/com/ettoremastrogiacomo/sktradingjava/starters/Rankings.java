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
        int minsamples=1500,maxdaygap=10,maxold=10,minvol=50000,minvoletf=10,setmin=10,setmax=50,popsize=20000,ngen=1000;
        double maxpcgap=.15;        
        boolean plot=false;
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createStockEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFSTOCKEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.create_ETF_INDICIZZATI_AZIONARIO_MLSE_Portfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        //Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createETFEURPortfolio(Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvoletf));
        Portfolio ptf=com.ettoremastrogiacomo.sktradingjava.Portfolio.createNYSEStockUSDPortfolio (Optional.of(minsamples), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol));
        int SIZE=minsamples<ptf.getLength()?minsamples:ptf.getLength()-1;
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
            betamap.put(ptf.getBeta(i, SIZE), ptf.realnames.get(i));
            corrmap.put(ptf.getCorrelation(i, SIZE), ptf.realnames.get(i));
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
        //betamap.keySet().forEach((x)-> logger.info(x+"\t"+betamap.get(x)));
        //Misc.map2csv(betamap, "./betamap.csv",Optional.empty());
        
        UDate train_enddate=ptf.dates.get(ptf.dates.size()-1);
        UDate train_startdate=ptf.dates.get(ptf.dates.size()-SIZE);
        //double[] w=ptf.optimizeMinVarQP(Optional.of(minsamples<ptf.getLength()?minsamples:ptf.getLength()-1), Optional.of(0), Optional.of(.05));
        setmax=setmax>ptf.getNoSecurities()?ptf.getNoSecurities():setmax;
        
        Entry<Double,ArrayList<Integer>> winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, Portfolio.optMethod.MINVAR, plot, popsize, ngen);
        logger.info("************************ optimization GA MINVAR ************************ ");
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
            logger.info( ptf.getName(ptf.hashcodes.get(x)));
        });
        Fints f=ptf.opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
        f.plot("minvar", "price");        
        logger.info("MAXDD: "+f.getName(0)+"\t"+f.getMaxDD(0));
        logger.info("MAXDD: "+f.getName(1)+"\t"+f.getMaxDD(1));
        logger.info("Final EQ: "+f.getName(0)+"\t"+f.getLastValueInCol(0));
        logger.info("Final EQ: "+f.getName(1)+"\t"+f.getLastValueInCol(1));        
        logger.debug("\n\n");


        winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, Portfolio.optMethod.MINDD, plot, popsize, ngen);
        logger.info("************************ optimization GA MINDD ************************ ");
        logger.info(train_startdate+"\tto\t"+train_enddate+"\tsamples "+ptf.closeER.Sub(train_startdate, train_enddate).getLength());
        logger.info("setmin "+setmin+"\tsetmax "+setmax);
        logger.info("BEST "+1.0/winner.getKey());
        logger.info("BEST "+winner.getValue());
        w=new double[winner.getValue().size()];
        DoubleArray.fill(w, 1.0/winner.getValue().size());        
        logger.info("BEST LOG VAR "+ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("BEST VAR check"+ptf.closeER.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("LOG VAR CAMPIONE "+Fints.ER(ptf.closeCampione, 100, true).head(SIZE).getCovariance()[0][0]);
        logger.info("BEST LEN "+winner.getValue().size());
        winner.getValue().forEach((x) -> {
            logger.info( ptf.getName(ptf.hashcodes.get(x)));
        });
        f=ptf.opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
        f.plot("mindd", "price");
        logger.info("MAXDD: "+f.getName(0)+"\t"+f.getMaxDD(0));
        logger.info("MAXDD: "+f.getName(1)+"\t"+f.getMaxDD(1));
        logger.info("Final EQ: "+f.getName(0)+"\t"+f.getLastValueInCol(0));
        logger.info("Final EQ: "+f.getName(1)+"\t"+f.getLastValueInCol(1));
        logger.debug("\n\n");        
        
        winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, Portfolio.optMethod.MAXSLOPE, plot, popsize, ngen);
        logger.info("************************ optimization GA MAXSLOPE ************************ ");
        logger.info(train_startdate+"\tto\t"+train_enddate+"\tsamples "+ptf.closeER.Sub(train_startdate, train_enddate).getLength());
        logger.info("setmin "+setmin+"\tsetmax "+setmax);
        logger.info("BEST "+1.0/winner.getKey());
        logger.info("BEST "+winner.getValue());
        w=new double[winner.getValue().size()];
        DoubleArray.fill(w, 1.0/winner.getValue().size());        
        logger.info("BEST LOG VAR "+ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("BEST VAR check"+ptf.closeER.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("LOG VAR CAMPIONE "+Fints.ER(ptf.closeCampione, 100, true).head(SIZE).getCovariance()[0][0]);
        logger.info("BEST LEN "+winner.getValue().size());
        winner.getValue().forEach((x) -> {
            logger.info( ptf.getName(ptf.hashcodes.get(x)));
        });
        f=ptf.opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
        f.plot("maxslope", "price");
        logger.info("MAXDD: "+f.getName(0)+"\t"+f.getMaxDD(0));
        logger.info("MAXDD: "+f.getName(1)+"\t"+f.getMaxDD(1));
        logger.info("Final EQ: "+f.getName(0)+"\t"+f.getLastValueInCol(0));
        logger.info("Final EQ: "+f.getName(1)+"\t"+f.getLastValueInCol(1));        
        logger.debug("\n\n");        


        winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, Portfolio.optMethod.MINCORR, plot, popsize, ngen);
        logger.info("************************ optimization GA MINCORR ************************ ");
        logger.info(train_startdate+"\tto\t"+train_enddate+"\tsamples "+ptf.closeER.Sub(train_startdate, train_enddate).getLength());
        logger.info("setmin "+setmin+"\tsetmax "+setmax);
        logger.info("BEST "+1.0/winner.getKey());
        logger.info("BEST "+winner.getValue());
        w=new double[winner.getValue().size()];
        DoubleArray.fill(w, 1.0/winner.getValue().size());        
        logger.info("BEST LOG VAR "+ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("BEST VAR check"+ptf.closeER.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("LOG VAR CAMPIONE "+Fints.ER(ptf.closeCampione, 100, true).head(SIZE).getCovariance()[0][0]);
        logger.info("BEST LEN "+winner.getValue().size());
        winner.getValue().forEach((x) -> {
            logger.info( ptf.getName(ptf.hashcodes.get(x)));
        });
        f=ptf.opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
        f.plot("mincorr", "price");
        logger.info("MAXDD: "+f.getName(0)+"\t"+f.getMaxDD(0));
        logger.info("MAXDD: "+f.getName(1)+"\t"+f.getMaxDD(1));
        logger.info("Final EQ: "+f.getName(0)+"\t"+f.getLastValueInCol(0));
        logger.info("Final EQ: "+f.getName(1)+"\t"+f.getLastValueInCol(1));        
        logger.debug("\n\n");        


        winner=ptf.opttrain(train_startdate, train_enddate, setmin, setmax, Portfolio.optMethod.MAXSHARPE, plot, popsize, ngen);
        logger.info("************************ optimization GA MAXSHARPE ************************ ");
        logger.info(train_startdate+"\tto\t"+train_enddate+"\tsamples "+ptf.closeER.Sub(train_startdate, train_enddate).getLength());
        logger.info("setmin "+setmin+"\tsetmax "+setmax);
        logger.info("BEST "+1.0/winner.getKey());
        logger.info("BEST "+winner.getValue());
        w=new double[winner.getValue().size()];
        DoubleArray.fill(w, 1.0/winner.getValue().size());        
        logger.info("BEST LOG VAR "+ptf.closeERlog.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("BEST VAR check"+ptf.closeER.SubSeries(winner.getValue()).head(SIZE).getWeightedCovariance(w));
        logger.info("LOG VAR CAMPIONE "+Fints.ER(ptf.closeCampione, 100, true).head(SIZE).getCovariance()[0][0]);
        logger.info("BEST LEN "+winner.getValue().size());
        winner.getValue().forEach((x) -> {
            logger.info( ptf.getName(ptf.hashcodes.get(x)));
        });
        f=ptf.opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
        f.plot("maxsharpe", "price");
        logger.info("MAXDD: "+f.getName(0)+"\t"+f.getMaxDD(0));
        logger.info("MAXDD: "+f.getName(1)+"\t"+f.getMaxDD(1));
        logger.info("Final EQ: "+f.getName(0)+"\t"+f.getLastValueInCol(0));
        logger.info("Final EQ: "+f.getName(1)+"\t"+f.getLastValueInCol(1));        
        logger.debug("\n\n");        

        
    }
    
    
}
