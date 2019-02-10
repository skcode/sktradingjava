
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;


import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Charts;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.jfree.chart.plot.XYPlot;

/**
 *
 * @author ettore
 */
public class BestRunnersLS {

    static Logger LOG = Logger.getLogger(BestRunnersLS.class);

    public static void main(String[] args) throws Exception {
        List<HashMap<String,String>> records=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.empty(), Optional.of(Arrays.asList("EUR")), Optional.empty());
        List<String> hashes=new java.util.ArrayList<>();
        records.forEach((x)->{hashes.add(x.get("hashcode"));});

        java.util.ArrayList<String> syms = Database.getFilteredPortfolio(Optional.of(hashes), Optional.of(1500), Optional.empty(), Optional.of(6), Optional.of(10), Optional.of(10000), Optional.empty());//com.ettoremastrogiacomo.sktradingjava.data.DBInterface.getSymbolListFilter(null, "MLSE", "EUR", null, null, null, null, "ETF-STOCK", null);
        //java.util.ArrayList<String> newsyms = new java.util.ArrayList<>();
        Fints all=new Fints();
        java.util.HashMap<String, String> isinmap = new java.util.HashMap<>();
        java.util.HashMap<String, String> namemap = new java.util.HashMap<>();
        java.util.HashMap<String, Integer> codeidx = new java.util.HashMap<>();
        int kk = 0;
        for (String x : syms){
            try {
                all=all.isEmpty()?Database.getFintsQuotes(x).getSerieCopy(3):all.merge(Database.getFintsQuotes(x).getSerieCopy(3));
                java.util.List<java.util.HashMap<String, String>> m = Database.getRecords(Optional.of("where hashcode='"+x+"'"));// com.ettoremastrogiacomo.sktradingjava.data.DBInterface.getSymbolInfo(s);
                if (m.isEmpty()) throw new Exception("hashcode"+x+" not found");
                isinmap.put(x, m.get(0).get("hashcode"));
                namemap.put(x, m.get(0).get("name"));
                codeidx.put(x, kk);
                kk++;
            } catch (Exception ex) {
                //java.util.logging.Logger.getLogger(BestRunnersLS.class.getName()).log(Level.SEVERE, null, ex);
                LOG.warn(ex);
            }
        }
        

        Fints ER = Fints.ER(all, 1, false);
        Fints msharpe = Fints.SMA(Fints.Sharpe(ER, 20), 200);

        int offset = all.getIndex(msharpe.getFirstDate());
        LOG.info("SYMS LEN="+syms.size()+"\tmsharpelen="+msharpe.getLength() + "\tmsharpeseries=" + msharpe.getNoSeries());
        int len = msharpe.getLength();
        int seriescount = msharpe.getNoSeries();
        LOG.info("firstdate=" + msharpe.getDate(0) + "\t" + "lastdate=" + msharpe.getDate(len - 1) + "\tnoseries=" + seriescount);
        int TOP_LONG = 10, TOP_SHORT = 0;
        double initialequity = 1000;
        double longfee = 0.005, shortfee = 0.01,treshold=0.07;
        
        java.util.HashMap<String, Double> longportfolio = new java.util.HashMap<>(), shortportfolio = new java.util.HashMap<>();
        java.util.TreeMap<UDate, Double> equity = new java.util.TreeMap<>();

        equity.put(msharpe.getFirstDate(), initialequity);//set initial equity
        double liquidity = initialequity;

        for (int j = 1; j < (len ); j++) {
            
            LOG.info("long portfolio at "+msharpe.getDate(j));
            for (String s : longportfolio.keySet()) {
                LOG.info(s+"\tvalue€ "+longportfolio.get(s) );
            }
            
            LOG.info("short portfolio at "+msharpe.getDate(j));
            for (String s : shortportfolio.keySet()) {
                LOG.info(s+"\tvalue€ "+shortportfolio.get(s));
            }
            
            //run portfolios 
            double sumlong = 0, sumshort = 0;
            for (String s : longportfolio.keySet()) {
                int idxs = codeidx.get(s);
                double var = (all.get(j + offset, idxs) - all.get(j + offset - 1, idxs)) / all.get(j + offset - 1, idxs);
                var = longportfolio.get(s) * (1 + var);
                sumlong += var;
                //longportfolio.remove(s);
                
                longportfolio.put(s, var);//update
                
                
            }
            for (String s : shortportfolio.keySet()) {
                int idxs = codeidx.get(s);
                double var = (all.get(j + offset, idxs) - all.get(j + offset - 1, idxs)) / all.get(j + offset - 1, idxs);
                var = shortportfolio.get(s) * (1 - var);
                sumshort += var;
                //shortportfolio.remove(s);
                
                shortportfolio.put(s, var);//update
            }
            LOG.debug(msharpe.getDate(j)+" l="+sumlong+" s="+sumshort+" liq="+liquidity);
            equity.put(msharpe.getDate(j), sumlong + sumshort + liquidity);

            //update portfolios            
            //select winners
            java.util.SortedMap<Double, String> long_sortedMap = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
            java.util.SortedMap<Double, String> short_sortedMap = new java.util.TreeMap<>();
            for (int i = 0; i < seriescount; i++) {
                if (!Double.isNaN(msharpe.get(j, i)) &&  msharpe.get(j, i) > treshold  ) {
                    long_sortedMap.put(msharpe.get(j, i), syms.get(i));
                    //logger.debug(msharpe.getDate(j)+" sym="+newsyms.get(i)+" long_fitness="+msharpe.get(j, i));
                }
                if (!Double.isNaN(msharpe.get(j, i)) && msharpe.get(j, i) < -treshold) {
                    short_sortedMap.put(msharpe.get(j, i), syms.get(i));
                    //logger.debug(msharpe.getDate(j)+" sym="+newsyms.get(i)+" short_fitness="+msharpe.get(j, i));
                }
            }
            if (TOP_LONG>0){
                if (long_sortedMap.size()>TOP_LONG) long_sortedMap=long_sortedMap.headMap( (Double) long_sortedMap.keySet().toArray()[TOP_LONG]);}
                else long_sortedMap.clear();
            if (TOP_SHORT>0){
                if (short_sortedMap.size()>TOP_SHORT) short_sortedMap=short_sortedMap.headMap( (Double) short_sortedMap.keySet().toArray()[TOP_SHORT]);}                
            else short_sortedMap.clear();
            
            
                //
            java.util.HashMap<String, Double> templ = new java.util.HashMap<>(longportfolio);
            
            for (String s : templ.keySet()) {
                if (!long_sortedMap.values().contains(s) || j == (len - 1)) { //close position
                    double var = longportfolio.get(s) * (1 - longfee);
                    longportfolio.remove(s);
                    LOG.debug("long removed "+s);
                    liquidity += var;
                }
            }
            
            java.util.HashMap<String, Double> temps = new java.util.HashMap<>(shortportfolio);
            for (String s : temps.keySet()) {
                if (!short_sortedMap.values().contains(s) || j == (len - 1)) { //close position
                    double var = shortportfolio.get(s) * (1 - shortfee);
                    shortportfolio.remove(s);
                    LOG.debug("short removed "+s);
                    liquidity += var;
                }
            }
            
            int stock2assign = long_sortedMap.size() - longportfolio.size() + short_sortedMap.size() - shortportfolio.size();
            //double liq2assign = stock2assign>0?liquidity / (double) stock2assign:0;
            double liq2assign =liquidity/(double)(TOP_LONG+TOP_SHORT);
            if (j != (len - 1)) {
                

                
                for (String s : long_sortedMap.values()) {
                    if (!longportfolio.containsKey(s)) {
                        longportfolio.put(s, liq2assign * (1 - longfee));//open position
                        LOG.debug("long added "+s);
                        liquidity -= liq2assign;
                    }
                }
                for (String s : short_sortedMap.values()) {
                    if (!shortportfolio.containsKey(s)) {
                        shortportfolio.put(s, liq2assign * (1 - shortfee));//open position
                        LOG.debug("short added "+s);
                        liquidity -= liq2assign;
                    }
                }

            }
            //reassign liquidity
            if (liquidity>(initialequity/10.0) && (longportfolio.size()==TOP_LONG || shortportfolio.size()==TOP_SHORT) ) { 
                liq2assign =liquidity/(double)(longportfolio.size()+shortportfolio.size());
                for (String s: longportfolio.keySet()) {
                    longportfolio.put(s,longportfolio.get(s)+  liq2assign * (1 - longfee));
                    liquidity -= liq2assign;
                }
                for (String s: shortportfolio.keySet()) {
                    shortportfolio.put(s,shortportfolio.get(s)+  liq2assign * (1 - shortfee));
                    liquidity -= liq2assign;
                }                                    
            }
            

        }
        
        //generate equity
        double[][] eq=new double[equity.size()][1];
        java.util.ArrayList<UDate> eqd=new java.util.ArrayList<>();
        java.util.ArrayList<String> eqs=new java.util.ArrayList<>();
        eqs.add("equity");
        int k=0;
        for (UDate d: equity.keySet()) {
            eqd.add(d);
            eq[k][0]=equity.get(d);k++;
        }
        Fints eqf=new Fints(eqd,eqs,Fints.frequency.DAILY,eq);
        Charts c1=new Charts("TS");
        
        XYPlot p1=c1.createXYPlot("equity",eqf);
        c1.plot(p1,640,480);

        
    }
}
