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
    final static long ITMAX = 500000000L;
    static double bestv = 1000;
    static Integer[] best = null;
    final double w2;
    final int size;
    final HashMap<String,String> namemap,namemap2;
    
    public Tclass(final Fints f, int size) throws Exception {
        this.f = f;
        this.cov = f.getCovariance();
        this.size = size;
        w2 = Math.pow(1.0 / size, 2);
        namemap=new HashMap<>();//name to hash
        for (int i=0;i<f.getLength();i++) {
            String s1=f.getInnerName(i);
            String[] v=s1.split("\\.");
            String s2=Database.getHashcode(v[0],v[1]);            
            namemap.put(f.getName(i), s2);     
        }        
        namemap2=Database.getCodeMarketName(new ArrayList<String>(namemap.values()));//hash to full        
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
                        logger.debug("\t" + f.getName(best1) +"\t"+ namemap2.get(namemap.get(f.getName(best1))));
                    }
                }
            }
            k++;
            if (k > ITMAX) {
                break;
            }
            if ((k % 5000000)==0) logger.debug("Iteration "+k+" of "+ITMAX);
        }
        
    }
}

public class BestCov {
    
    static Logger logger = Logger.getLogger(BestCov.class);
    

    
    public static void main(String[] args) throws Exception {
        
        int setsize = 15;
        int win = 90;
        int minvol=50000;
        List<String> markets=Database.getMarkets();
        markets.forEach((x)->{logger.debug(x);});
        Fints f=new Fints();
        for (String m : markets){
          if (m.contains("MLSE")||m.contains("XETRA")||m.contains("EURONEXT")){
          //  if (m.contains("EURONEXT")){
                Fints t1=Database.getFilteredPortfolioOfClose(Optional.of(win*2), Optional.of("STOCK"), Optional.of(m), Optional.empty(),Optional.of(10), Optional.of(1000), Optional.of(minvol), Optional.of(0.0));
                if (t1==null) continue;                
                f= f.isEmpty()? t1 : f.merge(t1);
                
            }
        
        }
        
        if (f==null || f.isEmpty()) throw new Exception("empty series");
        logger.debug("noseries="+f.getNoSeries() + "\tlength=" + f.getLength() + "\tmaxdategap=" + f.getMaxDateGap() / (1000 * 60 * 60 * 24) +"\tlastdate=" + f.getLastDate());
        f = Fints.ER(f, 100, true).head(win);
        // Random.seed(System.currentTimeMillis());
        

        
        
        int processors = Runtime.getRuntime().availableProcessors();
        if (f.getNoSeries() < setsize) {
            throw new Exception("too few series");
        }
        java.util.HashSet<Thread> tset = new java.util.HashSet<>();
        for (int i = 0; i < processors; i++) {
            Thread t1 = new Thread(new Tclass(f, setsize));
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
