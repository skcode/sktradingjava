/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.TreeMap;
import java.util.TreeSet;





/**
 *
 * @author root
 */
public class IntradayTradingTest {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayTradingTest.class);   
    public static java.util.TreeMap<Integer,java.util.AbstractMap.Entry<UDate,UDate>> continuousDates(long maxgapmsec) {
        java.util.TreeMap<Integer,java.util.AbstractMap.Entry<UDate,UDate>> map=new TreeMap<>();
        
        return map;
    }
    public static void main(String[] args) throws Exception {
        int TRAINW=40,TESTW=10,MAXGAP=4;
        
        java.util.HashMap<String,TreeSet<UDate>> map=Database.intradayDates();
        TreeSet<UDate> alldates=Database.getIntradayDates();
        for (String x: map.keySet()){
            
        }
        String code="ENEL",market="MLSE";
        String hash=Database.getHashcode(code, market);
        TreeSet<UDate> dates=Database.getIntradayDates(hash);
        java.util.TreeMap<UDate,Double> closeopenpct=new TreeMap<>();
        for (UDate d: dates) {
            Fints f=Database.getIntradayFintsQuotes(hash, d);
            closeopenpct.put(d, 100*(f.getLastRow()[3]-f.get(0, 3))/f.get(0, 3));
        }
        Fints f= new Fints(closeopenpct, java.util.Arrays.asList("closeopenpct"), Fints.frequency.DAILY);
        f.plot(code, "%");
    }
}
