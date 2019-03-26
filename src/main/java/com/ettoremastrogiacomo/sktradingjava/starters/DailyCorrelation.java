/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import com.ettoremastrogiacomo.utils.Misc;
//import com.sun.javafx.image.impl.ByteArgb;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class DailyCorrelation {
    static final org.apache.log4j.Logger LOG= org.apache.log4j.Logger.getLogger(DailyCorrelation.class);
    static final int WINDOW = 1000;
    static final int MAXOLD=10;
    static final int MINVOL=1000;
    static final int MAXDAYGAP=6;
    static final double MAXPCGAP=.2;
    public static void main(String[] args) throws Exception {
        List<HashMap<String,String>> map=Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and sector like '%CLASSE 2 IND AZIONARIO%' and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        final ArrayList<String> hashes=new java.util.ArrayList<>();
        map.forEach((x)->{
            LOG.debug(x.get("name"));
            hashes.add(x.get("hashcode"));
        });
        ArrayList<String> newhashes=Database.getFilteredPortfolio(Optional.of(hashes), Optional.of(WINDOW), Optional.of(MAXPCGAP), Optional.of(MAXDAYGAP), Optional.of(MAXOLD), Optional.of(MINVOL), Optional.empty());
       
        Portfolio ptf= new Portfolio(newhashes, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        Fints all=ptf.closeERlog;
        /*for (String x : newhashes)
            all=all.isEmpty()?Database.getFintsQuotes(x).getSerieCopy(3):all.merge(Database.getFintsQuotes(x).getSerieCopy(3));
        LOG.debug(all.toString());
        all=Fints.ER(all, 100, true);*/
        double[][] c=all.getCorrelation();
        double min=DoubleArray.min ( com.ettoremastrogiacomo.utils.DoubleDoubleArray.min(c));
        LOG.debug(min);
        StringBuilder csvstring=new StringBuilder();
        csvstring.append("CORR");
        for (int i=0;i<all.getNoSeries();i++) csvstring.append(";").append(ptf.securities.get(i).getName() );
        for (int i=0;i<all.getNoSeries();i++) {
            csvstring.append("\n").append(ptf.securities.get(i).getName());
            for (int j=0;j<c.length;j++) csvstring.append(";").append(c[i][j]);
            
        }
        File file= new File(Misc.getTempDir()+"/corr.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {writer.write(csvstring.toString());}
        //java.util.HashMap<String,HashMap<String,String>> hmap=Misc.list2map(map, "hashmap");
        
        double[] w=ptf.optimizeMinVarQP(Optional.of(200), Optional.empty(),Optional.of(.5));
        for (int i=0;i<w.length;i++) {if (w[i]>.005) LOG.debug(ptf.securities.get(i).getName()+"\t"+w[i]*100+"%");}
        w=ptf.optimizeSharpeBH(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(15));
        for (int i=0;i<w.length;i++) {if (w[i]>.005) LOG.debug(ptf.securities.get(i).getName()+"\t"+w[i]*100+"%");}
        
    }
}
