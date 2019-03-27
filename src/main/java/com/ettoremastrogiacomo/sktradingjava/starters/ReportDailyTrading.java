/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import java.util.ArrayList;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class ReportDailyTrading {
    static Logger logger = Logger.getLogger(ReportDailyTrading.class);
 

    public static void main(String[] args) throws Exception {

        ArrayList<String> list=Database.getFilteredPortfolio(Optional.empty(), Optional.of(1500), Optional.of(.15), Optional.of(7), Optional.empty(), Optional.of(500000), Optional.empty());        
        Portfolio ptf= new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        ptf.walkForwardTest(Optional.of(60), Optional.of(60), Optional.of(1000000L), Optional.of(10),Optional.of(Portfolio.optMethod.MINDD));
    }
}
