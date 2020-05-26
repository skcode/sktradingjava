/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.Misc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.log4j.Logger;

/**
 *
 * @author sk
 */
public class ListSecurities {
    static Logger logger = Logger.getLogger(ListSecurities.class);
    public static void main(String[] args) throws Exception {
        
        ArrayList<HashMap<String, String>> attivi_map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%TIPO STRUMENTO=ETF ATTIVI%'"));
        ArrayList<HashMap<String, String>> strutturati_map  = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%TIPO STRUMENTO=ETF STRUTTURATI%'"));
        ArrayList<HashMap<String, String>> indicizzati_map  = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%'"));
        ArrayList<HashMap<String, String>> indicizzati_azionario_map  = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI;CLASSE=CLASSE 2 IND AZIONARIO%'"));
        ArrayList<HashMap<String, String>> indicizzati_obbligazionario_map  =Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI;CLASSE=CLASSE 1 IND OBBLIGAZIONARIO%'"));
        ArrayList<HashMap<String,String>> tutti=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.empty(), Optional.empty(), Optional.empty());
            for ( HashMap<String,String> v : indicizzati_obbligazionario_map){
                logger.info("-------------");
                for (String k : v.keySet()) logger.info(k+"\t"+v.get(k));            
            }
            
    
    }
}
