/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.sktradingjava.Fints;

import static com.ettoremastrogiacomo.sktradingjava.starters.BestCov.logger;
import static com.ettoremastrogiacomo.utils.Misc.getDistinctRandom;
import static com.ettoremastrogiacomo.utils.Misc.getTempDir;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.apache.log4j.Logger;
//import org.jblas.util.Random;

/**
 *
 * @author sk
 */
class Tclass implements Runnable {
    
    final double[][] cov;
    final Fints f;
    final static long ITMAX = 50000000L;
    static double bestv = 1000;
    static Integer[] best = null;
    final double w2;
    final int size;
    final HashMap<Integer,String> namemap;
    
    public Tclass(final Fints f, int size,HashMap<Integer,String> names) throws Exception {
        this.f = f;
        this.cov = f.getCovariance();
        this.size = size;
        w2 = Math.pow(1.0 / size, 2);
        this.namemap=names;
        /*namemap=new HashMap<>();//name to hash
        for (int i=0;i<f.getNoSeries();i++) {
            String s1=f.getInnerName(i);
            String[] v=s1.split("\\.");
            String s2=Database.getHashcode(v[0],v[1]);            
            namemap.put(f.getName(i), s2);     
        }        
        namemap2=Database.getCodeMarketName(new ArrayList<>(namemap.values()));//hash to full        */
    }
    
    static double getBestVariance() {
        return bestv;
    }
    
    static Optional<Integer[]> getBestArray() {
        return Optional.ofNullable(best);
    }
    
    @Override
    public void run() {
        java.util.Set<Integer> s = null;
        long k = 1;
        while (true) {
            try {
                s = getDistinctRandom(size, f.getNoSeries());
            } catch (Exception e) {
            }
            if (s == null) {
                continue;
            }
            Integer[] idx = s.toArray(new Integer[s.size()]);
            double curv = 0;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    curv += w2 * cov[idx[i]][idx[j]];
                }
            }
            synchronized (this) {
                if (curv < bestv) {
                    bestv = curv;
                    best = idx;
                    logger.debug("thread id=" + Thread.currentThread().getId() + "\tit=" + k + "\tnew best : " + bestv);
                    for (Integer best1 : best) {
                   
                        logger.debug("\t" + f.getName(best1) +"\t"+ namemap.get(best1));
                    }
                }
            }
            k++;
            if (k > ITMAX) {
                break;
            }
            if ((k % 5000000)==0) logger.debug("Iteration "+k+" of "+ITMAX +"\tthread id "+Thread.currentThread().getId());
        }
        
    }
}

public class BestCov {
    
    static Logger logger = Logger.getLogger(BestCov.class);
    

    
    public static void main(String[] args) throws Exception {
        
        int setsize = 15;
        int win = 250;
        int minvol=100000;
        double maxpcgap=0.2;
        int maxdaygap=6;
        int maxold=10;
        double sharpe_treshold=Double.NEGATIVE_INFINITY;
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE","XETRA","EURONEXT")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes= new ArrayList<>();
        for (HashMap<String,String> x : map) {hashcodes.add(x.get("hashcode"));}
        hashcodes=Database.getFilteredPortfolio(Optional.of(hashcodes), Optional.of(win*2), Optional.of(maxpcgap), Optional.of(maxdaygap), Optional.of(maxold), Optional.of(minvol), Optional.of(sharpe_treshold));
        List<String> markets=Database.getMarkets();
        markets.forEach((x)->{logger.debug(x);});
        Fints f=new Fints();
        HashMap<Integer,String> hashcodes_ok= new HashMap<>();
        HashMap<String,String> codemarketname_ok= new HashMap<>();
        for ( String h: hashcodes) {
            Fints t1= Database.getFintsQuotes(h).getSerieCopy(3);
            if (t1==null) continue;
            f= f.isEmpty()? t1 : f.merge(t1);
            hashcodes_ok.put(f.getNoSeries()-1,h);            
        }
        codemarketname_ok=Database.getCodeMarketName(new ArrayList<>(hashcodes_ok.values()) );
        for (Integer i: hashcodes_ok.keySet()) {
            hashcodes_ok.replace(i, codemarketname_ok.get(hashcodes_ok.get(i)));
        }
        if ( f.isEmpty()) throw new Exception("empty series");
        logger.debug("noseries="+f.getNoSeries() + "\tlength=" + f.getLength() + "\tmaxdategap=" + f.getMaxDateGap() / (1000 * 60 * 60 * 24) +"\tlastdate=" + f.getLastDate());
        f = Fints.ER(f, 100, true).head(win);
        // Random.seed(System.currentTimeMillis());
        

        
        
        int processors = Runtime.getRuntime().availableProcessors()-1;
        if (f.getNoSeries() < setsize) {
            throw new Exception("too few series");
        }
        java.util.HashSet<Thread> tset = new java.util.HashSet<>();
        for (int i = 0; i < processors; i++) {
            Thread t1 = new Thread(new Tclass(f, setsize,hashcodes_ok));
            t1.start();
            tset.add(t1);
        }
        tset.forEach((x) -> {
            try {
                x.join();
            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(BestCov.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        //Database.g
        
        if (Tclass.getBestArray().isPresent()) {
            logger.info(Tclass.getBestVariance());
            Integer[] v = Tclass.getBestArray().get();
            Fints res = new Fints();
            for (int i = 0; i < v.length; i++) {
                res= i==0 ? f.getSerieCopy(v[i]) : res.merge(f.getSerieCopy(v[i]));
            }
            if (res != null) {
                res.toCSV(getTempDir()+"/res.csv");
            }
            logger.info("done, saved file "+getTempDir()+"/res.csv");
        } else {
            logger.info("simulation not started");
        }
        
    }
    
}
