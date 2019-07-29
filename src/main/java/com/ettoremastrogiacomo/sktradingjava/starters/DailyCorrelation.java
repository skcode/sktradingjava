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
    static final int WINDOW = 2000;
    static final int MAXOLD=10;
    static final int MINVOL=10000;
    static final int MINVOLETF=100;
    static final int MAXDAYGAP=10;
    static final double MAXPCGAP=.2;
    static final double MAXWEIGHT=.05;
    static final double MAXWEIGHTETF=.1;
    static double eps=.005;
    public static void main(String[] args) throws Exception {
        List<HashMap<String,String>> map=Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and sector like '%CLASSE 2 IND AZIONARIO%' and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        List<HashMap<String,String>> smap=Database.getRecords(Optional.of(" where type= 'STOCK' and currency='EUR' and not isin like '%BE%' and not isin like '%IE%'"));
        final ArrayList<String> hashes=new java.util.ArrayList<>();
        final ArrayList<String> shashes=new java.util.ArrayList<>();
        map.forEach((x)->{
            LOG.debug(x.get("name"));
            hashes.add(x.get("hashcode"));
        });
        smap.forEach((x)->{
            LOG.debug(x.get("name"));
            shashes.add(x.get("hashcode"));
        });
        
        ArrayList<String> newhashes=Database.getFilteredPortfolio(Optional.of(hashes), Optional.of(WINDOW), Optional.of(MAXPCGAP), Optional.of(MAXDAYGAP), Optional.of(MAXOLD), Optional.of(MINVOLETF), Optional.empty());       
        ArrayList<String> snewhashes=Database.getFilteredPortfolio(Optional.of(shashes), Optional.of(WINDOW), Optional.of(MAXPCGAP), Optional.of(MAXDAYGAP), Optional.of(MAXOLD), Optional.of(MINVOL), Optional.empty());       
        Portfolio ptf= new Portfolio(newhashes, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        Portfolio sptf= new Portfolio(snewhashes, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        Fints all=ptf.closeERlog;
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
        
        double[] w=ptf.optimizeMinVarQP(Optional.of(WINDOW), Optional.empty(),Optional.of(MAXWEIGHTETF));
        for (int i=0;i<w.length;i++) {if (w[i]>eps) LOG.debug(ptf.securities.get(i).getName()+"\t"+w[i]*100+"%");}
        double[] sw=sptf.optimizeMinVarQP(Optional.of(WINDOW), Optional.empty(),Optional.of(MAXWEIGHT));
        for (int i=0;i<sw.length;i++) {if (sw[i]>eps) LOG.debug(sptf.securities.get(i).getName()+"\t"+sw[i]*100+"%");}
        
    }
}
