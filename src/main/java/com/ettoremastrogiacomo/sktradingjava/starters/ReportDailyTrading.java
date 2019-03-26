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
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.Misc;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class ReportDailyTrading {
    static Logger logger = Logger.getLogger(ReportDailyTrading.class);
 

    public static void main(String[] args) throws Exception {

        List<String> list=Database.getFilteredPortfolio(Optional.empty(), Optional.of(1000), Optional.of(.15), Optional.of(10), Optional.empty(), Optional.of(1000000), Optional.empty());
        java.util.HashMap<String,Fints> close= new HashMap<>();
        java.util.HashMap<String,Fints> sharpe= new HashMap<>();
        java.util.HashMap<String,Fints> dsharpe= new HashMap<>();
        java.util.HashMap<String,Fints> ddsharpe= new HashMap<>();
        java.util.HashMap<String,String> bestmap= new HashMap<>();
        java.util.HashMap<String,String> names= Database.getCodeMarketName(list);
        NumberFormat formatter = new DecimalFormat("#0.0000");
        StringBuilder bests=new StringBuilder();
        java.util.ArrayList<String> bestStock= new ArrayList<>();
        java.util.ArrayList<String> allStock= new ArrayList<>();
        for (String x : list){
            if (!names.get(x).contains("STOCK")) continue;
            if (names.get(x).contains("IE0")) continue;
            Fints t1=Database.getFintsQuotes(x).getSerieCopy(3).head(300);
            close.put(x, Fints.ER(t1, 100, true));
            sharpe.put(x, Fints.SMA(Fints.Sharpe(close.get(x), 20), 200));//Fints dmsharpe=Fints.SMA(Fints.Diff(msharpe), 20);            
            dsharpe.put(x, Fints.SMA(Fints.Diff(sharpe.get(x)), 20));
            ddsharpe.put(x, Fints.SMA(Fints.Diff(dsharpe.get(x)), 20));            
            StringBuilder s= new StringBuilder();
            s.append(t1.getLastDate()).append("\t");
            s.append(formatter.format(t1.getLastRow()[0])).append("\t");
            double s1=sharpe.get(x).getLastRow()[0];
            double s2=dsharpe.get(x).getLastRow()[0];
            double s3=ddsharpe.get(x).getLastRow()[0];
            s.append(formatter.format(s1)).append("\t");
            s.append(formatter.format(s2)).append("\t");
            s.append(formatter.format(s3)).append("\t");
            s.append(names.get(x));            
            logger.info(s);
            if (s1>0 /*&& s2>0 && s3>0*/) {
                bests.append(Misc.padRight(names.get(x), 50, ' ')).append("\t").append(formatter.format(s1)).append("\t").append(formatter.format(s2)).append("\t").append(formatter.format(s3)).append("\n");
                bestStock.add(x);
            }
            allStock.add(x);
        }
        logger.info("\nBESTS\n"+bests);
        Portfolio ptf= new Portfolio(allStock, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        ptf.walkForwardTest3(Optional.of(80), Optional.of(20), Optional.of(1000000L), Optional.of(10),Optional.of(Portfolio.optMethod.MAXSHARPE));
        //ptf.optimizeMinVar(Optional.of(120), Optional.empty(), Optional.of(2000000L), Optional.of(20));
      //  ptf.optimizeMinVarQP(Optional.of(120), Optional.empty(), Optional.empty());
        //ptf.optimizeSharpeBH(Optional.of(120), Optional.empty(), Optional.of(1000000L), Optional.of(20));
    }
}
