/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.UDate;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author ettore
 */
public class IntradayStats {
        static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(IntradayStats.class);

    public static void main(String[] args) throws Exception {
        TreeSet<UDate> dates=Database.getIntradayDates();
        
        HashMap<String,TreeSet<UDate>> map=Database.getIntradayDatesMap();
        TreeMap<UDate,ArrayList<String>> rmap=Database.getIntradayDatesReverseMap();
        StringBuilder sb= new StringBuilder();        
        String del=";";
        sb.append("CHECK").append(del);
        for (UDate d: dates) {
            if (rmap.get(d).size()<200) continue;
            sb.append(d.toYYYYMMDD()).append(del);
        }
        sb.append("\n");
        HashMap<String,String> m=Database.getCodeMarketName(new ArrayList<> (map.keySet()));
        for (String x: map.keySet()) {
            if  (!m.get(x).contains("STOCK") || !m.get(x).contains("MLSE") ) continue;            
            sb.append(m.get(x)).append(del);
            for (UDate d: dates) {
                if (rmap.get(d).size()<200) continue;
                if (map.get(x).contains(d)) sb.append("1").append(del);
                else sb.append("0").append(del);            
            }
            sb.append("\n");
        }            
        File file = new File("./intradayfreq.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(sb.toString());
        }          
    }
}
