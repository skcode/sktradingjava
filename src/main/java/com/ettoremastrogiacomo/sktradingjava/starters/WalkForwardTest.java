/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.backtesting.MT_Optimizer;
import com.ettoremastrogiacomo.sktradingjava.backtesting.WalkForward;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import java.util.Arrays;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class WalkForwardTest {
      public static void main(String[] args)throws Exception {
                    java.util.ArrayList<String> list1=new java.util.ArrayList<> ();
                    java.util.ArrayList<String> alist=new java.util.ArrayList<> ();
                    java.util.List<java.util.HashMap<String,String>> records=Database.getRecords(Optional.empty(),Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("XMEM")), Optional.empty(), Optional.of(Arrays.asList("MLSE")), Optional.empty(), Optional.empty());
                    list1.add(records.get(0).get("hashcode"));
                    
                    com.ettoremastrogiacomo.sktradingjava.Portfolio portfolio=new Portfolio(list1,Optional.empty(),Optional.empty(),Optional.empty(),Optional.empty());          
                    com.ettoremastrogiacomo.sktradingjava.system.SHARPE_CROSSOVER max=new com.ettoremastrogiacomo.sktradingjava.system.SHARPE_CROSSOVER(portfolio)     ;
                    com.ettoremastrogiacomo.sktradingjava.backtesting.WalkForward wf=new WalkForward(max,max.getDates().get(0),max.getDates().get(max.getDates().size()-1),1000,120,MT_Optimizer.fitobjective.NETPROFIT,Optional.empty(),Optional.empty(),Optional.empty());
                    Fints eq=wf.run(Optional.empty());
                    eq.plot("equity", "val");
                    
      }
}
