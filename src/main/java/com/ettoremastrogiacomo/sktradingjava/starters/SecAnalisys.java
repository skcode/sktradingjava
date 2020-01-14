/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ettoremastrogiacomo.sktradingjava.starters;
//import java.util.ArrayList;
//import speedking.utils.Init;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import com.ettoremastrogiacomo.sktradingjava.Charts;
import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Security;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
//import speedking.trading.data.Datasource;
/**
 *
 * @author a241448
 */
public class SecAnalisys {
static public org.apache.log4j.Logger LOG= Logger.getLogger(SecAnalisys.class);

    public static void main(String[] args) throws Exception{
            String symbol="ISP";//INA.EURONEXT-XLIS
            String market="MLSE";
            String hashcode=Database.getHashcode(symbol, market);
            Fints f=Database.getFintsQuotes(Optional.of(symbol), Optional.of(market),Optional.empty());
            HashMap<String,TreeSet<UDate>> map=Database.getIntradayDatesMap();
            TreeSet<UDate> dates=Misc.mostRecentTimeSegment(map.get(hashcode), 1000*60*60*24*5);
            ArrayList<Double> closeopen=new ArrayList<>();
            ArrayList<Double> meanv=new ArrayList<>();
            ArrayList<Double> stdv=new ArrayList<>();
            ArrayList<Integer> samples= new ArrayList<>();
            ArrayList<Double> runs= new ArrayList<>();
            
            for (UDate d: dates){
                Fints f1=Database.getIntradayFintsQuotes(hashcode, d);
                closeopen.add((f1.getLastValueInCol(0)-f1.getFirstValueInCol(0))/f1.getFirstValueInCol(0));
                Fints er1=Fints.ER(f1, 1, false);
                meanv.add(er1.getMeans()[0]);
                stdv.add(er1.getStd()[0]);
                runs.add(f1.runTestZscore(Security.SERIE.CLOSE.getValue()));
                samples.add(f1.getLength());                
            }
            LOG.debug("intraday samples "+dates.size());
            LOG.debug("closeopen "+closeopen.stream().collect(Collectors.averagingDouble(a->a)));
            LOG.debug("meaner "+meanv.stream().collect(Collectors.averagingDouble(a->a)));
            LOG.debug("stder "+stdv.stream().collect(Collectors.averagingDouble(a->a)));
            LOG.debug("runs "+runs.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN));
            LOG.debug("samples "+samples.stream().mapToDouble(a->a).average().orElse(Double.NaN));
            
                System.out.println(f);
                System.out.println(f.getSerieCopy(3));
                Fints ER=Fints.ER(f.getSerieCopy(3), 1, false);
                Fints msharpe=Fints.SMA(Fints.Sharpe(ER, 20), 200);
                Fints dmsharpe=Fints.SMA(Fints.Diff(msharpe), 20);
                Fints dmsharpe2=Fints.SMA(Fints.Diff(dmsharpe), 20);
                //Fints dmsharpe2=Fints.Diff(dmsharpe);
                
                double []v_er=ER.getSerieVector(0);
                double std=com.ettoremastrogiacomo.utils.DoubleArray.std(v_er);
                double mean=com.ettoremastrogiacomo.utils.DoubleArray.mean(v_er);
                double min=com.ettoremastrogiacomo.utils.DoubleArray.min(v_er);
                double max=com.ettoremastrogiacomo.utils.DoubleArray.max(v_er);
                
                System.out.println("symbol="+symbol);
                System.out.println("freq="+f.getFrequency());
                System.out.println("samples="+f.getLength());
                System.out.println("first date="+f.getFirstDate());
                System.out.println("last date="+f.getLastDate());
                System.out.println("date gap="+f.getMaxDateGap()/(1000*60*60*24)+" day(s)");
                System.out.println("er std="+std+"\nmean="+mean+"\nmin="+min+"\nmax="+max);                
                System.out.println("date:"+msharpe.getDate(msharpe.getLength()-1));
                System.out.println("sh:"+msharpe.get(msharpe.getLength()-1,0 ));
                System.out.println("dsh:"+dmsharpe.get(dmsharpe.getLength()-1,0 ));
                System.out.println("ddsh:"+dmsharpe2.get(dmsharpe2.getLength()-1,0 ));                               
                System.out.println("ACF last 100 samples:\n"+f.getSerieCopy(3).head(100).getACF(10));
                System.out.println("ACF last 200 samples:\n"+f.getSerieCopy(3).head(200).getACF(10));
                System.out.println("ACF last 300 samples:\n"+f.getSerieCopy(3).head(300).getACF(10));
                System.out.println("ACF "+f.getLength()+" samples:\n"+f.getSerieCopy(3).getACF(10));
                Fints.Kron(Fints.multiLag(ER, 0), Fints.multiLag(ER, 0));
        Charts c1=new Charts("Analisys");
        Fints all=f.getSerieCopy(3);
        all=Fints.merge(all, msharpe);
        all=Fints.merge(all, dmsharpe);
        all=Fints.merge(all, dmsharpe2);
        XYPlot p1=c1.createXYPlot(all.getName(0),all.getSerieCopy(0));
        XYPlot p2=c1.createXYPlot(all.getName(1),all.getSerieCopy(1));
        XYPlot p3=c1.createXYPlot(all.getName(2),all.getSerieCopy(2));
        XYPlot p4=c1.createXYPlot(all.getName(3),all.getSerieCopy(3));
        XYPlot []arr={p1,p2,p3,p4};
        CombinedDomainXYPlot  cp=  c1.createCombinedDomainXYPlot("dom", arr, false);
        Database.getIntradayFintsQuotes(hashcode, dates.last()).toCSV("/tmp/intraday.csv");
        
        c1.plotCombined(cp,640,480);
        Fints iday=Security.createContinuity(Database.getIntradayFintsQuotes(hashcode, dates.last())).getSerieCopy(3);
        Fints totrade=Fints.KAMA(iday, 10, 20, 60).merge(Fints.KAMA(iday, 10, 60, 120)).merge(iday);
        totrade.plot(symbol, "price");        
        TreeMap<UDate,Double> equity= new TreeMap<>();
        equity.put(totrade.getFirstDate(), 1.);
        boolean longpos=false,shortpos=false;
        double fee=7,spreadfee=0.001;
        
        for (int i=1;i<totrade.getLength();i++){
            if (totrade.get(i, 2)>=totrade.get(i, 0)){
                if (shortpos || (!shortpos && ! longpos)){
                    shortpos=false;longpos=true;
                    equity.put(totrade.getDate(i), equity.lastEntry().getValue() *(1+(totrade.get(i, 2)-totrade.get(i-1, 2))/totrade.get(i-1, 2)-spreadfee)-fee*2);                                
                    LOG.debug(" buy at "+totrade.getDate(i)+"\t"+totrade.get(i, 2));
                }else{                    
                    equity.put(totrade.getDate(i), equity.lastEntry().getValue() *(1+(totrade.get(i, 2)-totrade.get(i-1, 2))/totrade.get(i-1, 2)));
                
                }
            }else {
                if (longpos || (!shortpos && ! longpos)){
                    shortpos=true;longpos=false;
                    equity.put(totrade.getDate(i), equity.lastEntry().getValue() *(1-(totrade.get(i, 2)-totrade.get(i-1, 2))/totrade.get(i-1, 2)-spreadfee)-fee*2);                            
                    LOG.debug(" sell at "+totrade.getDate(i)+"\t"+totrade.get(i, 2));
                }
                equity.put(totrade.getDate(i),equity.lastEntry().getValue()*(1-(totrade.get(i, 2)-totrade.get(i-1, 2))/totrade.get(i-1, 2)));
            }
            
        }
          (new Fints(equity, Arrays.asList("ieq"), Fints.frequency.SECOND)).plot("equity", "val");
        
        //Database.getIntradayFintsQuotes(hashcode, dates.last()).getSerieCopy(0).plot(symbol, "price");
        //Database.getIntradayFintsQuotes(hashcode, dates.last()).getSerieCopy(3).plot(symbol, "price");
        //c1.plot(p1,640,480);
    }
    
}
