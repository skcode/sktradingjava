package com.ettoremastrogiacomo.sktradingjava;

import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import com.ettoremastrogiacomo.utils.Misc;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import com.sun.org.apache.bcel.internal.generic.AALOAD;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
//import javafx.util.Pair;
import org.apache.log4j.Logger;
import java.util.Set;
import java.util.TreeSet;

public class Portfolio {

    private Fints allfints, open, high, low, close, volume, oi;
    public final java.util.ArrayList<Security> securities;
    private final java.util.ArrayList<String> hashcodes;
    private final java.util.HashMap<String, String> names;
    private final int nosecurities;
    private final int length;
    public final java.util.List<UDate> dates;
    private final Fints.frequency freq;
    public final Fints closeERlog;
    public final Fints closeER;
    static final Logger LOG = Logger.getLogger(Portfolio.class);

    public enum optMethod {
        MINVAR,MINVARBARRIER, MAXSHARPE, MAXPROFIT, MINDD,MAXSLOPE,MINSTDERR,PROFITMINDDRATIO,SMASHARPE
    };

    public Portfolio(java.util.ArrayList<String> hashcodes, Optional<Fints.frequency> freq, Optional<UDate> iday, Optional<UDate> from, Optional<UDate> to) throws Exception {
        this.freq = freq.orElse(Fints.frequency.DAILY);
        if (this.freq.compareTo(Fints.frequency.DAILY) < 0 && !iday.isPresent()) {
            throw new Exception("day must be specified if intraday freq :" + freq);
        }
        this.securities = new java.util.ArrayList<>();
        this.hashcodes = new java.util.ArrayList<>();
        for (String s : hashcodes) {
            if (this.hashcodes.contains(s)) {
                LOG.warn("symbol " + s + " already inserted, skipping");
                continue;
            }
            this.hashcodes.add(s);
            this.securities.add(new com.ettoremastrogiacomo.sktradingjava.Security(s));
        }
        names = Database.getCodeMarketName(this.hashcodes);
        nosecurities = securities.size();
        allfints = new Fints();
        for (Security s : securities) {
            Fints f;
            switch (this.freq) {
                case MONTHLY:
                    f = s.getMonthly();
                    break;
                case WEEKLY:
                    f = s.getWeekly();
                    break;
                case DAILY:
                    f = s.getDaily();
                    break;
                case SECOND:
                    f = s.getIntradaySecond(iday.get());
                    break;
                case MINUTE:
                    f = s.getIntradayMinute(iday.get());
                    break;
                case HOUR:
                    f = s.getIntradayHour(iday.get());
                    break;
                default:
                    throw new Exception("not yet implemented " + this.freq);
                //break;
                }
            if (from.isPresent() && to.isPresent()) {
                allfints = allfints.isEmpty() ? f : allfints.merge(f.Sub(from.get(), to.get()));
            } else if (from.isPresent() && !to.isPresent()) {
                allfints = allfints.isEmpty() ? f : allfints.merge(f.Sub(from.get(), f.getLastDate()));
            } else if (!from.isPresent() && to.isPresent()) {
                allfints = allfints.isEmpty() ? f : allfints.merge(f.Sub(f.getFirstDate(), to.get()));
            } else {
                allfints = allfints.isEmpty() ? f : allfints.merge(f);
            }
        }
        length = allfints.getLength();
        dates = Collections.unmodifiableList(allfints.getDate());
        for (int i = 0; i < this.nosecurities; i++) {
            if (i == 0) {
                open = allfints.getSerieCopy(i * 6);
                high = allfints.getSerieCopy(i * 6 + 1);
                low = allfints.getSerieCopy(i * 6 + 2);
                close = allfints.getSerieCopy(i * 6 + 3);
                volume = allfints.getSerieCopy(i * 6 + 4);
                oi = allfints.getSerieCopy(i * 6 + 5);
            } else {
                open = open.merge(allfints.getSerieCopy(i * 6));
                high = high.merge(allfints.getSerieCopy(i * 6 + 1));
                low = low.merge(allfints.getSerieCopy(i * 6 + 2));
                close = close.merge(allfints.getSerieCopy(i * 6 + 3));
                volume = volume.merge(allfints.getSerieCopy(i * 6 + 4));
                oi = oi.merge(allfints.getSerieCopy(i * 6 + 5));
            }
        }
        closeERlog = Fints.ER(close, 100, true);
        closeER = Fints.ER(close, 1, false);
    }

    public ArrayList<String> set2names(Set<Integer> set) {
        ArrayList<String> list=new java.util.ArrayList<>();
        TreeSet<Integer> hashSetToTreeSet = new TreeSet<>(set); 
        hashSetToTreeSet.forEach((x) -> {list.add(this.names.get(this.hashcodes.get(x)));});
        return list;
    }
    
    
    public Fints opttest(Set<Integer> set, UDate startdate, UDate enddate, Optional<Double> lastequity, Optional<Double> lastequitybh) throws Exception {
        Fints subf = closeER.Sub(startdate, enddate);
        double[][] m = subf.getMatrixCopy();
        LOG.debug("\nTEST " + subf.toString()+"\n"+set+"\n"+set2names(set));
        int setsize = set.size();
        int poolsize = subf.getNoSeries();
        int len = subf.getLength();
        double[][] eqm = new double[len][2];
        for (int i = 0; i < len; i++) {
            double dm = 0;
            double dmbh = 0;
            for (int j : set) { //IF SET is empty, then flat!!! dm=0
                dm += m[i][j];
            }
            for (int j = 0; j < poolsize; j++) {
                dmbh += m[i][j];
            }
            dm = setsize>0 ?dm / setsize:0;
            dmbh = dmbh / poolsize;
            if (i == 0) {
                eqm[i][0] = lastequity.orElse(1.0) * (1 + dm);
                eqm[i][1] = lastequitybh.orElse(1.0) * (1 + dmbh);
            } else {
                eqm[i][0] = eqm[i - 1][0] * (1 + dm);
                eqm[i][1] = eqm[i - 1][1] * (1 + dmbh);
            }
        }
        return new Fints(subf.getDate(), Arrays.asList("equity", "equityBH"), subf.getFrequency(), eqm);
    }

    public Entry opttrain(int setsize, UDate startdate, UDate enddate, optMethod met, Optional<Long> epoch) throws Exception {
        Fints subf = closeER.Sub(startdate, enddate);
        Fints subflog = closeERlog.Sub(startdate, enddate);
        LOG.debug("\nTRAIN " + subf.toString());
        int poolsize = subf.getNoSeries();
        int samplelen = subf.getLength();
        if (setsize > poolsize) {
            throw new Exception("optimal set greather than available set " + setsize + ">" + poolsize);
        }
        double[][] m = subf.getMatrixCopy();
        double[][] mlog = subflog.getMatrixCopy();
        long DEFEPOCHS = 1000000L;
        int avproc=Runtime.getRuntime().availableProcessors();
        long effepochs=epoch.orElse(DEFEPOCHS)/avproc;
        ExecutorService pool = Executors.newFixedThreadPool(avproc);
        java.util.ArrayList<Future> futures = new java.util.ArrayList<>();
        switch (met) {
            case MINVAR: {
                double[][] c = DoubleDoubleArray.cov(m);
                double w = 1.0 / setsize;
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            double var = 0;
                            for (Integer sa1 : set) {
                                for (Integer sa2 : set) {
                                    var += w * w * c[sa1][sa2];
                                }
                            }
                            double fitness = 1.0 / var;
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;
            case MINVARBARRIER: {
                double[][] c = DoubleDoubleArray.cov(mlog);
                double[] mm=DoubleDoubleArray.mean(mlog);
                double w = 1.0 / setsize;
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            boolean notgood=false;
                            for (Integer sa1 : set) {
                                if (mm[sa1]<=0) notgood=true;//mean barrier
                            }
                            if (notgood) continue;
                            
                            double var = 0;
                            for (Integer sa1 : set) {
                                for (Integer sa2 : set) {
                                    var += w * w * c[sa1][sa2];
                                }
                            }
                            
                            
                            double fitness = 1.0 / var;
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                               
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;
            case SMASHARPE: {
                if (mlog.length<250) throw new Exception("SMASHARPE , trainingsamples<250:"+mlog.length);
                double[][] c = DoubleDoubleArray.cov(mlog);
                Fints smasharpe=Fints.SMA(Fints.Sharpe(subflog,200), 20);
                double[] lr=smasharpe.getLastRow();
                //double[] mm=DoubleDoubleArray.mean(mlog);
                double w = 1.0 / setsize;
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            boolean notgood=false;
                            for (Integer sa1 : set) {
                                if (lr[sa1]<=0) notgood=true;//mean barrier
                            }
                            if (notgood) continue;
                            
                            double var = 0;
                            for (Integer sa1 : set) {
                                for (Integer sa2 : set) {
                                    var += w * w * c[sa1][sa2];
                                }
                            }
                            double fitness = 1.0 / var;
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                               
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;

            case MAXSHARPE: {
                double[] eqt = new double[samplelen];
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            for (int i = 0; i < samplelen; i++) {
                                double mean = 0;
                                for (Integer sa1 : set) {
                                    mean += m[i][sa1];
                                }
                                mean = mean / setsize;
                                eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
                            }                            
                            HashMap<String,Double> map=DoubleArray.LinearRegression(eqt);
                            double fitness = map.get("slope")/map.get("stderr");
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;
            case MAXSLOPE: {
                double[] eqt = new double[samplelen];
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            for (int i = 0; i < samplelen; i++) {
                                double mean = 0;
                                for (Integer sa1 : set) {
                                    mean += m[i][sa1];
                                }
                                mean = mean / setsize;
                                eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
                            }                            
                            HashMap<String,Double> map=DoubleArray.LinearRegression(eqt);
                            double fitness = map.get("slope");
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;
            case MINSTDERR: {
                double[] eqt = new double[samplelen];
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            for (int i = 0; i < samplelen; i++) {
                                double mean = 0;
                                for (Integer sa1 : set) {
                                    mean += m[i][sa1];
                                }
                                mean = mean / setsize;
                                eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
                            }                            
                            HashMap<String,Double> map=DoubleArray.LinearRegression(eqt);
                            double fitness = 1.0/map.get("stderr");
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;
            case PROFITMINDDRATIO: {
                double[] eqt = new double[samplelen];
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            for (int i = 0; i < samplelen; i++) {
                                double mean = 0;
                                for (Integer sa1 : set) {
                                    mean += m[i][sa1];
                                }
                                mean = mean / setsize;
                                eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
                            }                            
                            //HashMap<String,Double> map=DoubleArray.LinearRegression(eqt);
                            double fitness = eqt[eqt.length-1]/Math.abs(DoubleArray.maxDrowDownPerc(eqt));
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;
            
            case MAXPROFIT: {
                //double [][] c=DoubleDoubleArray.cov(m);
                double[] eqt = new double[samplelen];
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);
                            for (int i = 0; i < samplelen; i++) {
                                double mean = 0;
                                for (Integer sa1 : set) {
                                    mean += m[i][sa1];
                                }
                                mean = mean / setsize;
                                eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
                            }
                            double fitness = eqt[eqt.length-1];
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }
            break;
            case MINDD: {
                double[] eqt = new double[samplelen];
                for (int k = 0; k < avproc; k++) {
                    futures.add(pool.submit(() -> {
                        double localbest = Double.NEGATIVE_INFINITY;
                        Set<Integer> localbestset = new TreeSet<>();
                        for (long t = 0; t < effepochs; t++) {
                            Set<Integer> set = Misc.getDistinctRandom(setsize, poolsize);

                            for (int i = 0; i < samplelen; i++) {
                                double mean = 0;
                                for (Integer sa1 : set) {
                                    mean += m[i][sa1];
                                }
                                mean = mean / setsize;
                                eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
                            }
                            //                            
                            double fitness = DoubleArray.maxDrowDownPerc(eqt);
                            if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
                                fitness = Double.NEGATIVE_INFINITY;
                            }
                            if (t == 0) {
                                localbest = fitness;
                                localbestset = set;
                            } else {
                                if (fitness > localbest) {
                                    localbest = fitness;
                                    localbestset = set;
                                }
                            }
                        }
                        return new AbstractMap.SimpleEntry<Double, Set>(localbest, localbestset);
                    }));
                }
            }

            break;

            default:
                throw new Exception("not yet implemented");
        }
        pool.shutdown();
        try {
            pool.awaitTermination(1, TimeUnit.HOURS);//wait 1 hour
        } catch (InterruptedException e) {
            LOG.error(e);
        }

        Entry<Double, Set> bestentry = new AbstractMap.SimpleEntry<>(Double.NEGATIVE_INFINITY, new TreeSet<Integer>());
        for (Future f : futures) {
            Entry<Double, Set> entry = (Entry) f.get();
            if (entry.getKey() > bestentry.getKey()) {
                bestentry = entry;
            }
        }
        return bestentry;
    }

    /**
     *
     * @param window optimization window length, default all porfolio length
     * @param window_offset offset from most recent trading day, default 0
     * @param limit_upper_bound
     * @return optimal weights
     * @throws Exception
     */
    public double[] optimizeMinVarQP(Optional<Integer> window, Optional<Integer> window_offset, Optional<Double> limit_upper_bound) throws Exception {
        LOG.debug("first serie" + closeERlog);
        if (window.isPresent()) {
            if (window.get() > closeERlog.getLength()) {
                throw new Exception("max windows = " + closeERlog.getLength());
            }
        }
        Fints sub = closeERlog.SubRows(closeERlog.getLength() - window_offset.orElse(0) - window.orElse(closeERlog.getLength()), closeERlog.getLength() - window_offset.orElse(0) - 1);
        LOG.debug("sub serie" + sub);
        LOG.debug("length " + sub.getLength() + "\tno series" + sub.getNoSeries());
        LOG.debug("max date gap " + sub.getMaxDaysDateGap());
        double[][] cov = sub.getCovariance();
        /*        A minimization problem in the form of:
        minimizex (1/2)xTPx+qTx+r  s.t.
          Gx ≤ h
          Ax = b,  
      where P ∈ S+n, G ∈ RmXn and A ∈ RpXn 
         */
        PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(cov, null, 0);
        double[][] A = new double[1][cov.length];
        com.ettoremastrogiacomo.utils.DoubleDoubleArray.fill(A, 1.0);
        double[] b = new double[]{1};
        //inequalities
        /*
        double[][] default_ineq = new double[cov.length][cov.length];
        double[] default_ineq_vector = new double[cov.length];
        for (int i = 0; i < cov.length; i++) {
            default_ineq[i][i] = -1.0;
        }*/
        //upper bound ineq e.g. 10%
        double min_upperb = 1.0 / (double) cov.length;
        if (limit_upper_bound.isPresent()) {
            if (limit_upper_bound.get() <= min_upperb || limit_upper_bound.get() > 1) {
                throw new Exception("wrong limit " + limit_upper_bound.get());
            }
        }
        double upper_bound = limit_upper_bound.orElse(1.0) > min_upperb ? limit_upper_bound.orElse(1.0) : min_upperb;
        LOG.debug("upper bound " + upper_bound);
        /*double[][] ub_ineq = new double[cov.length][cov.length];
        double[] ub_ineq_vector = new double[cov.length];
        for (int i = 0; i < cov.length; i++) {
            ub_ineq[i][i] = 1.0;
            ub_ineq_vector[i]=upper_bound;
        }*/
        //

        //double[][] td = ineq_constraints.orElse(default_ineq);
        //double[] tdv = ineq_contraints_vector.orElse(default_ineq_vector);
        //ineq_constraints.orElse(A)
        ConvexMultivariateRealFunction[] inequalities = new ConvexMultivariateRealFunction[cov.length * 2];
        for (int i = 0; i < cov.length; i++) {
            double[] td = new double[cov.length];
            td[i] = -1.0;
            inequalities[i] = new LinearMultivariateRealFunction(td, 0);
        }
        for (int i = cov.length; i < inequalities.length; i++) {
            double[] td = new double[cov.length];
            td[i - cov.length] = 1.0;
            inequalities[i] = new LinearMultivariateRealFunction(td, -upper_bound);
        }

        //optimization problem
        OptimizationRequest or = new OptimizationRequest();
        or.setF0(objectiveFunction);
        or.setFi(inequalities); //if you want x>=0 and x<=ub
        or.setA(A);
        or.setB(b);
        or.setToleranceFeas(1.E-12);
        or.setTolerance(1.E-12);
        //optimization
        JOptimizer opt = new JOptimizer();
        opt.setOptimizationRequest(or);
        opt.optimize();
        double[] sol = opt.getOptimizationResponse().getSolution();
        double bestvar = 0;
        for (int i = 0; i < cov.length; i++) {
            for (int j = 0; j < cov.length; j++) {
                bestvar += sol[i] * sol[j] * cov[i][j];
            }
        }
        LOG.debug("\n\n\n\n\nnew best=" + bestvar);
        for (int i = 0; i < sol.length; i++) {
            if (sol[i] > 0.001) {
                LOG.debug(this.names.get(securities.get(i).getHashcode()) + "\t" + sol[i]);
                //LOG.debug(this.securities.get(i).getName() + "\t" + sol[i]);
            }
        }

        return sol;
    }

    public void walkForwardTest(Optional<Integer> train_window, Optional<Integer> test_window, Optional<Long> epochs, Optional<Integer> equalWeightSec, Optional<Portfolio.optMethod> optmet) throws Exception {
        int testWin = test_window.orElse(60);//default 60 samples for test window
        int trainWin = train_window.orElse(250);//default 250 samples for train window
        int sizeOptimalSet = equalWeightSec.orElse(10);//default 10 stock to pick each time        
        Portfolio.optMethod optype = optmet.orElse(Portfolio.optMethod.MAXSHARPE);
        //Fints exret = Fints.ER(this.close, 1, false);
        //double[][] exretmat = exret.getMatrixCopy();
        int step = 0;
        //int stockPoolSize = exret.getNoSeries();

        double lastequity = 1;
        double lastequitybh = 1;
        LOG.debug("trainWin " + trainWin);
        LOG.debug("testWin " + testWin);
        LOG.debug("opt method " + optype);
        LOG.debug("runtime processors " + Runtime.getRuntime().availableProcessors());
        LOG.debug("epochs " + epochs);
        LOG.debug("date range " + closeER.getFirstDate()+"\t->\t"+closeER.getLastDate());
        LOG.debug("START");
        //LOG.debug("pool " + exret);
        Fints alleq = new Fints();
        while (true) {

            int offset = step * testWin;
            //LOG.debug("offset " + offset);

            if ((offset + trainWin + testWin) >= closeER.getLength()) {
                break;
            }
            //LOG.debug("date range  " + closeER.getDate(offset) + "\t" + closeER.getDate(offset + trainWin + testWin));
            Entry<Double, Set<Integer>> winner = this.opttrain(sizeOptimalSet, closeER.getDate(offset), closeER.getDate(offset + trainWin - 1), optype, epochs);
            LOG.debug("overall best : " + winner.getKey() + "\t"+winner.getValue()+"\n"+set2names(winner.getValue()));
            Fints eq = this.opttest(winner.getValue(), closeER.getDate(offset + trainWin), closeER.getDate(offset + trainWin + testWin - 1), Optional.of(lastequity), Optional.of(lastequitybh));
            if (alleq.isEmpty()) {
                alleq = eq;
            } else {
                alleq = Fints.append(alleq, eq);
            }
            lastequity = alleq.getLastValueInCol(0);// getLastRow()[0];
            lastequitybh = alleq.getLastValueInCol(1);
            LOG.debug("equity optimized " + lastequity+"\tmdd="+alleq.getMaxDD(0));
            LOG.debug("equity bh " + lastequitybh+"\tmdd="+alleq.getMaxDD(1));
            LOG.debug("equity info "+alleq);
            step++;
        }
        
        alleq = alleq.merge(Fints.merge(alleq.getLinReg(0), alleq.getLinReg(1)));        
        LOG.debug("equity mdd " + alleq.getMaxDD(0));
        LOG.debug("equity bh mdd " + alleq.getMaxDD(1));
        HashMap<String,Double> st1=DoubleArray.LinearRegression(alleq.getCol(0));
        HashMap<String,Double> st2=DoubleArray.LinearRegression(alleq.getCol(1));
        LOG.debug("linreg slope "+st1.get("slope"));
        LOG.debug("linreg slope bh "+st2.get("slope"));
        LOG.debug("linreg stderr "+st1.get("stderr"));
        LOG.debug("linreg stderr bh "+st2.get("stderr"));
        LOG.debug("linreg intercept "+st1.get("intercept"));
        LOG.debug("linreg intercept bh "+st2.get("intercept"));
        
        
        alleq.plot("equity", "val");

    }

    public Fints.frequency getFrequency() {
        return this.freq;
    }

    public int getLength() {
        return this.length;
    }

    public int getNoSecurities() {
        return this.nosecurities;
    }

    public java.util.List<UDate> getDate() {
        return allfints.getDate();
    }

    public int getOffset(String symbol) throws Exception {
        int ret = this.hashcodes.indexOf(symbol);
        if (ret < 0) {
            throw new Exception("symbol " + symbol + " not found");
        }
        return ret;
    }

    public int getOffset(UDate date) throws Exception {
        int ret = java.util.Arrays.binarySearch(dates.toArray(), date);
        if (ret < 0) {
            throw new Exception("Date " + date + " not found");
        }
        return ret;
    }

    public UDate getDate(int idx) throws Exception {
        return dates.get(idx);
    }

    public Fints getOpen() {
        return this.open;
    }

    public Fints getHigh() {
        return this.high;
    }

    public Fints getLow() {
        return this.low;
    }

    public Fints getClose() {
        return this.close;
    }

    public Fints getVolume() {
        return this.volume;
    }

    public Fints getOI() {
        return this.oi;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("\nPortfolio info\n");
        s.append("size: ").append(this.getLength()).append("\n");
        try {
            s.append("first date: ").append(this.getDate(0)).append("\n");
            s.append("last date: ").append(this.getDate(this.getLength() - 1)).append("\n");
            s.append("length: ").append(this.getLength()).append("\n");
        } catch (Exception e) {
        }
        s.append("num securities: ").append(this.getNoSecurities()).append("\n");
        s.append("date gap in days: ").append(this.allfints.getMaxDaysDateGap()).append("\n");
        s.append("days from now: ").append(this.allfints.getDaysFromNow()).append("\n");
        names.keySet().forEach((x) -> {
            s.append(names.get(x)).append("\n");
        });
        /*this.securities.forEach((x) -> {
            
            s.append(x.getIsin()).append("\t").append(x.getCode()).append(".").append(x.getMarket()).append("\t").append(x.getName()).append("\t").append(x.getSector()).append("\n");
        });*/
        return s.toString();
    }

}
