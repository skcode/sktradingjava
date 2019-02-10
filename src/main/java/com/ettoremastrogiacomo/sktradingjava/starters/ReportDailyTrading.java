/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.utils.UDate;


/**
 *
 * @author sk
 */
public class ReportDailyTrading {
    static Logger logger = Logger.getLogger(ReportDailyTrading.class);
 
      public static String padRight(String s) {
    return String.format("%1$-" + 15 + "s", s);
  }

    public static void main(String[] args) throws Exception {

/*

        
        java.util.ArrayList<String> syms = com.ettoremastrogiacomo.sktradingjava.data.DBInterface.getSymbolListFilter(null, "MLSE", "EUR", null, null, null, null, "STOCK", null);
        
        java.util.TreeMap<Double,String> best2tradehl=new java.util.TreeMap<> ();
        java.util.Calendar c = java.util.Calendar.getInstance();
        double volumetreshold=1000000;
        if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            c.add(Calendar.DATE, -2);
        } else {
            c.add(Calendar.DATE, -1);
        }

        UDate lastdate = new UDate(c.getTimeInMillis());
        for (String sym : syms) {
            Fints f = new Fints();
            boolean err = false;
            try {
                f = DBInterface.getFints(sym, Fints.frequency.DAILY);
            }catch (Exception e) {
                err = true;
            }
            if (err) {
                continue;
            }
            if (f.getMaxDateGap()>(7*24*60*60*1000)) continue;
            if (f.getLastDate().before(lastdate)) {
                continue;
            }
            if (f.getLength() < 300) {
                continue;
            }            
            java.util.HashMap<String,String> info=DBInterface.getSymbolInfo(sym);
            
            Fints ER=Fints.ER(f.getSerieCopy(3), 100, true);
            Fints volatility=Fints.Volatility(ER, 20);
            Fints sharpe200=Fints.Sharpe(ER, 200);
            Fints sharpe20=Fints.Sharpe(ER, 20);
            double hlchange=(f.get(f.getLength()-1, 1)-f.get(f.getLength()-1, 2))/f.get(f.getLength()-1, 2);
            double close=f.get(f.getLength()-1, 3);
            double volume=f.get(f.getLength()-1, 4);
            double lastchangeclose=(f.get(f.getLength()-1, 3)-f.get(f.getLength()-2, 3))/f.get(f.getLength()-2, 3);
            //best2tradehlv.put(hlchange*volume, sym);
            
            
            if (volume>volumetreshold) best2tradehl.put(hlchange, sym);
            String sep="";
            java.text.SimpleDateFormat dt1 = new java.text.SimpleDateFormat("yyyyMMdd");
            java.text.DecimalFormat df = new java.text.DecimalFormat("#0.0000"); 
            java.text.DecimalFormat df2= new java.text.DecimalFormat("#"); 
            String s=padRight(dt1.format(f.getLastDate())) +sep+padRight(sym)  +sep+padRight(df.format(close))+sep+sep+padRight(df.format(lastchangeclose))+sep+sep+padRight(df.format(hlchange))+sep+sep+padRight(df2.format(volume))+sep+sep+padRight(df.format(hlchange*volume))+sep+sep+padRight(df.format(volatility.get(volatility.getLength()-1, 0)))+sep+sep+padRight(df.format(sharpe20.get(sharpe20.getLength()-1, 0)))+sep+sep+padRight(df.format(sharpe200.get(sharpe200.getLength()-1, 0)));
            logger.info(s);

        }
        logger.info("best2trade hl (volume>"+String.valueOf(volumetreshold)+")");
        for (Double d: best2tradehl.keySet()) logger.info("HiLow change%="+d+"\t"+best2tradehl.get(d));
*/
    }
}
