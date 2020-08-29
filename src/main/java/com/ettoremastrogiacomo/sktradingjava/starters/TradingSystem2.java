/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.backtesting.Orders;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.Charts;
import com.ettoremastrogiacomo.sktradingjava.backtesting.MT_Optimizer;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.system.BUYANDHOLD;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.jfree.chart.plot.*;

/**
 *
 * @author a241448
 */
public class TradingSystem2 {

    static Logger logger = Logger.getLogger(TradingSystem2.class);

    public static void main(String[] args) throws Exception {

        com.ettoremastrogiacomo.sktradingjava.backtesting.MT_Optimizer tsopt;
        Orders orders;
        com.ettoremastrogiacomo.sktradingjava.backtesting.Statistics stats;
        Charts c;
        XYPlot p;
        try {
            final java.util.ArrayList<String> list1 = new java.util.ArrayList<>();
            final java.util.ArrayList<String> alist = new java.util.ArrayList<>();

            List<HashMap<String, String>> records = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("XMEM")), Optional.empty(), Optional.of(Arrays.asList("MLSE")), Optional.empty(), Optional.empty());

            records.forEach((x) -> {
                list1.add(x.get("hashcode"));
            });

            records = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and sector like '%CLASSE 2 IND AZIONARIO%'"));

            records.forEach((x) -> {
                alist.add(x.get("hashcode"));
            });
            //list1.add(Database.getIsins(Optional.empty(), Optional.empty(), Optional.of("XMEM"), Optional.empty(), Optional.of("MLSE"), Optional.empty(), Optional.empty()).get(0));                    

            //String []itaetf={"IBCX.MI","LQDE.MI","X25E.MI","XEMB.MI","XGIN.MI","EQQQ.MI","XMWO.MI","XMEM.MI","EXXY.MI","RICI.MI","XSFR.MI","IPRP.MI"};
            //String []itaetf={"XAXJ.MI","XMWO.MI","XMEM.MI","XCBL.MI","XSFR.MI"};
            //alist.addAll(Arrays.asList(itaetf));
            //alist.add(Database.getIsins(Optional.empty(), Optional.empty(), Optional.of("XAXJ"), Optional.empty(), Optional.of("MLSE"), Optional.empty(), Optional.empty()).get(0));                    
            //alist.add(Database.getIsins(Optional.empty(), Optional.empty(), Optional.of("XMWO"), Optional.empty(), Optional.of("MLSE"), Optional.empty(), Optional.empty()).get(0));                    
            //alist.add(Database.getIsins(Optional.empty(), Optional.empty(), Optional.of("XMEM"), Optional.empty(), Optional.of("MLSE"), Optional.empty(), Optional.empty()).get(0));                    
            //alist.add(Database.getIsins(Optional.empty(), Optional.empty(), Optional.of("XSFR"), Optional.empty(), Optional.of("MLSE"), Optional.empty(), Optional.empty()).get(0));                                        
            java.util.ArrayList<String> alist2 = Database.getFilteredPortfolio(Optional.of(alist), Optional.of(2000), Optional.of(.2), Optional.of(7), Optional.of(30), Optional.of(100), Optional.empty());

            logger.info("building portfolio");
            com.ettoremastrogiacomo.sktradingjava.Portfolio portfolio = new Portfolio(list1, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
            com.ettoremastrogiacomo.sktradingjava.Portfolio portfolio2 = new Portfolio(alist2, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
            com.ettoremastrogiacomo.sktradingjava.backtesting.Backtest bk;
            //test ts

            logger.info("TS building");
            //speedking.trading.system.MA_CROSSOVER max=new speedking.trading.system.MA_CROSSOVER(portfolio);

            java.util.HashMap<int[], Double> m = new java.util.HashMap<>();
            int[] best;

            BUYANDHOLD BH = new BUYANDHOLD(portfolio2);
            tsopt = new com.ettoremastrogiacomo.sktradingjava.backtesting.MT_Optimizer(BH, BH.getDates().get(0), BH.getDates().get(BH.getDates().size() - 1), Optional.of(2000000), Optional.empty(), Optional.empty(), Optional.empty());
            tsopt.setFitnessObjective(MT_Optimizer.fitobjective.SHARPE);
            m.clear();
            best = tsopt.run(m);
            //BH.setParams(cr);
            java.util.HashMap<String, Double> bestpar = BH.getRealParams(best);
            bestpar.keySet().forEach((s) -> {
                System.out.println("Best par " + s + " : " + bestpar.get(s));
            });
            orders = BH.apply(BH.getDates().get(0), BH.getDates().get(BH.getDates().size() - 1), best);
            bk = new com.ettoremastrogiacomo.sktradingjava.backtesting.Backtest(portfolio2, Optional.empty(), Optional.empty(), Optional.empty());
            stats = bk.apply(orders, portfolio2.getDate().get(0), portfolio2.getDate().get(portfolio2.getLength() - 1));
            System.out.println(stats.toString());
            c = new Charts("title");
            p = c.createXYPlot("range", stats.equity);
            c.plot(p, 640, 480);

            com.ettoremastrogiacomo.sktradingjava.system.SHARPE_CROSSOVER max = new com.ettoremastrogiacomo.sktradingjava.system.SHARPE_CROSSOVER(portfolio);
            MT_Optimizer mtopt = new MT_Optimizer(max, max.getDates().get(0), max.getDates().get(max.getDates().size() - 1), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
            mtopt.setFitnessObjective(MT_Optimizer.fitobjective.NETPROFIT);

            best = mtopt.run(m);
            stats = mtopt.getStats(best);
            logger.debug(best[0] + "\t" + best[1] + "\n" + stats);
            stats.equity.plot("equity", "cap");

        } catch (Exception e) {
            logger.error("holy shit", e);
        }

    }

}
