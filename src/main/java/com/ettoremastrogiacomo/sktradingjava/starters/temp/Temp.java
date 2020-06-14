

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters.temp;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.utils.UDate;

import java.util.ArrayList;
import java.util.TreeMap;
import static com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch.fetchXETRAEOD;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author a241448
 */
public class Temp {

    static Logger LOG = Logger.getLogger(Temp.class);

    public static JSONArray tree2json(TreeMap<UDate,ArrayList<Double>> m){
        
       JSONArray array= new JSONArray();
       for (UDate d: m.keySet()) 
       {
            JSONObject jsonObject = new JSONObject();
            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("open", m.get(d).get(0));
            jsonObject2.put("high", m.get(d).get(1));
            jsonObject2.put("low", m.get(d).get(2));
            jsonObject2.put("close", m.get(d).get(3));
            jsonObject2.put("volume", m.get(d).get(4));
            if (m.get(d).size()<6) jsonObject2.put("oi", 0);
            else jsonObject2.put("oi", m.get(d).get(5));            
            jsonObject.put("date", d.toYYYYMMDD());            
            jsonObject.put("data", jsonObject2);
            array.put(jsonObject);
       }                
        return array;
    }

    public static void main(String[] args) throws Exception {
       Fints enel=Database.getFintsQuotes(Optional.of("ENEL"), Optional.of("MLSE"), Optional.empty());
       enel.getSerieCopy(3).plot("enel", "price");
       // LOG.debug(Database.getYahooQuotes("MSFT"));
       
       

       
       
 
       
    }
}
