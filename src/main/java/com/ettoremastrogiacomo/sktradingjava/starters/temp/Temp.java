

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.temp;

import com.ettoremastrogiacomo.sktradingjava.data.Database;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.utils.UDate;

import java.util.ArrayList;
import java.util.TreeMap;
import static com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch.fetchXETRAEOD;

/**
 *
 * @author a241448
 */
public class Temp {

    static Logger LOG = Logger.getLogger(Temp.class);

  

    public static void main(String[] args) throws Exception {
        TreeMap<UDate,ArrayList<Double>> m=fetchXETRAEOD("DE0007551509",true);
        LOG.debug(Database.getYahooQuotes("MSFT"));
    }
}
