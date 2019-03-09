package com.ettoremastrogiacomo.sktradingjava;

import com.ettoremastrogiacomo.sktradingjava.backtesting.Backtest;
import com.ettoremastrogiacomo.sktradingjava.backtesting.MT_Optimizer;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Statistics;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.sktradingjava.system.BUYANDHOLD;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import com.ettoremastrogiacomo.utils.Misc;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
//import javafx.util.Pair;
import org.apache.log4j.Logger;
import com.ettoremastrogiacomo.utils.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class Portfolio {

    private Fints allfints, open, high, low, close, volume, oi;
    public final java.util.ArrayList<Security> securities;
    private final java.util.ArrayList<String> hashcodes;
    private final java.util.HashMap<String, String> names;
    private final int nosecurities;
    private final int length;
    public final java.util.List<UDate> dates;
    private final Fints.frequency freq;
    public final Fints closeER;
    static final Logger LOG = Logger.getLogger(Portfolio.class);

    public enum optMethod {
        MINVAR, MAXSHARPE, MAXPROFIT, MINDD
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

        //if (this.freq.ordinal()>=Fints.frequency.DAILY.ordinal()) {
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

        closeER = Fints.ER(close, 100, true);
        //cov=closeER.getCovariance();
        //corr=closeER.getCorrelation();
        //average=closeER.getMeans();
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
        LOG.debug("first serie" + closeER);
        if (window.isPresent()) {
            if (window.get() > closeER.getLength()) {
                throw new Exception("max windows = " + closeER.getLength());
            }
        }
        Fints sub = closeER.SubRows(closeER.getLength() - window_offset.orElse(0) - window.orElse(closeER.getLength()), closeER.getLength() - window_offset.orElse(0) - 1);
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

    public void walkForwardTest2(Optional<Integer> train_window, Optional<Integer> test_window, Optional<Long> epochs, Optional<Integer> equalWeightSec, Optional<Portfolio.optMethod> optmet) throws Exception {
        int testWin = test_window.orElse(60);//default 60 samples for test window
        int trainWin = train_window.orElse(250);//default 250 samples for train window
        int sizeOptimalSet = equalWeightSec.orElse(10);//default 10 stock to pick each time        
        Portfolio.optMethod optype = optmet.orElse(Portfolio.optMethod.MAXSHARPE);
        Fints exret = Fints.ER(this.close, 1, false);
        double[][] exretmat = exret.getMatrixCopy();
        int step = 0;
        int stockPoolSize = exret.getNoSeries();

        double eqopt = 1;
        double eqbh = 1;
        LOG.debug("trainWin " + trainWin);
        LOG.debug("testWin " + testWin);
        LOG.debug("opt method " + optype);
        LOG.debug("pool " + exret);

        while (true) {

            int offset = step * testWin;
            LOG.debug("offset " + offset);
            if ((offset + trainWin + testWin) >= exretmat.length) {
                break;
            }
            final java.util.TreeMap<Double, java.util.Set<Integer>> winnerSetSharpe = new java.util.TreeMap<>();

            ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (int p = 0; p < Runtime.getRuntime().availableProcessors(); p++) {
                pool.execute(() -> {

                    for (int k = 0; k < epochs.orElse(500000L); k++) {
                        try {
                        java.util.Set<Integer> tempSet = Misc.getDistinctRandom(sizeOptimalSet, stockPoolSize);
                        double[] vtrain = new double[trainWin];
                        for (int i = 0; i < trainWin; i++) {
                            vtrain[i] = 0;
                            for (int j : tempSet) {
                                vtrain[i] = vtrain[i] + exretmat[offset + i][j];
                            }
                            vtrain[i] = vtrain[i] / (double) sizeOptimalSet;
                        }
                        double meantrain = DoubleArray.mean(vtrain);
                        double stdtrain = DoubleArray.std(vtrain);
                        double sharpetrain = meantrain / stdtrain;
                        double res;
                        switch (optype) {
                            case MAXPROFIT:
                                res = meantrain;
                                break;
                            case MAXSHARPE:
                                res = sharpetrain;
                                break;
                            case MINVAR:
                                res = 1.0 / stdtrain;
                                break;
                            case MINDD:
                                throw new Exception("mindd not implemented " + optype);
                            default:
                                throw new Exception("unknow optmethod " + optype);
                        }
                        if (Double.isFinite(res)) {
                            if (winnerSetSharpe.isEmpty()) {
                                winnerSetSharpe.put(res, tempSet);
                                //LOG.debug("new best " + res + " at " + k);
                            } else {
                                if (res > winnerSetSharpe.lastKey()) {
                                    winnerSetSharpe.put(res, tempSet);
                                  //  LOG.debug("new best " + res + " at " + k);
                                }
                            }
                        }
                        } catch (Exception e) {LOG.warn(e);}
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.HOURS);            
            LOG.debug("overall best : " + winnerSetSharpe.lastEntry().getKey() + winnerSetSharpe.lastEntry().getValue());
            double[] vtest = new double[testWin];
            double[] vtestbh = new double[testWin];
            for (int i = 0; i < testWin; i++) {
                vtest[i] = 0;
                vtestbh[i] = 0;
                for (int j : winnerSetSharpe.lastEntry().getValue()) {
                    vtest[i] = vtest[i] + exretmat[offset + trainWin + i][j];
                }
                for (int j = 0; j < stockPoolSize; j++) {
                    vtestbh[i] = vtestbh[i] + exretmat[offset + trainWin + i][j];
                }
                vtest[i] = vtest[i] / (double) sizeOptimalSet;
                vtestbh[i] = vtestbh[i] / (double) stockPoolSize;
            }
            double meantest = DoubleArray.mean(vtest);
            double stdtest = DoubleArray.std(vtest);
            double sharpetest = meantest / stdtest;
            double meantestbh = DoubleArray.mean(vtestbh);
            double stdtestbh = DoubleArray.std(vtestbh);
            double sharpetestbh = meantest / stdtestbh;
            for (int i = 0; i < testWin; i++) {
                eqbh = eqbh * (1 + vtestbh[i]);
                eqopt = eqopt * (1 + vtest[i]);
            }
            LOG.debug("equity optimized " + eqopt);
            LOG.debug("equity bh " + eqbh);
            step++;
        }
    }

    public void walkForwardTest(Optional<Integer> train_window, Optional<Integer> test_window, Optional<Long> epochs, Optional<Integer> equalWeightSec, Optional<Portfolio.optMethod> optmet) throws Exception {
        int testWin = test_window.orElse(60);//default 60 samples for test window
        int trainWin = train_window.orElse(250);//default 250 samples for train window
        int sizeOptimalSet = equalWeightSec.orElse(10);//default 10 stock to pick each time        
        Portfolio.optMethod optype = optmet.orElse(Portfolio.optMethod.MAXSHARPE);
        UDate startDate = closeER.getFirstDate();

        LOG.debug("OPTIMIZE FOR " + optype);
        LOG.debug("Train window size = " + trainWin + "\tTest window size = " + testWin);
        LOG.debug("start training from " + startDate);
        LOG.debug("optimal set size " + sizeOptimalSet);
        LOG.debug("all samples Fints " + closeER.toString());
        LOG.debug("runtime processors " + Runtime.getRuntime().availableProcessors());
        if ((closeER.getLength() - testWin - trainWin) <= 0) {
            throw new Exception("size too short, try to change parameters: " + closeER.getLength() + "<=" + (testWin + trainWin));
        }
        if (sizeOptimalSet > closeER.getNoSeries()) {
            throw new Exception("optimal set must be less than pool size: " + sizeOptimalSet + ">" + closeER.getNoSeries());
        }

        int step = 0;
        Fints allequitySharpe = new Fints();
        while (true) {
            int ubound = testWin * step + trainWin + testWin - 1;
            if (closeER.getLength() < (ubound + 1)) {
                LOG.debug("terminato ubound=" + ubound + "\tlast=" + (closeER.getLength() - 1));
                break;
            }
            LOG.info("Step " + (step + 1));
            final Fints subTrain = closeER.SubRows(testWin * step, testWin * step + trainWin - 1);
            final Fints subTest = closeER.SubRows(testWin * step + trainWin, testWin * step + trainWin + testWin - 1);
            LOG.debug("trainset" + subTrain.toString());
            LOG.debug("testset" + subTest.toString());
            final java.util.TreeMap<Double, java.util.Set<Integer>> winnerSetSharpe = new java.util.TreeMap<>();
            final double[][] m_subTrain = subTrain.getMatrixCopy();
            final double[][] m_subTest = subTest.getMatrixCopy();
            ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            for (int k = 0; k < Runtime.getRuntime().availableProcessors(); k++) {
                pool.execute(() -> {
                    int stockPoolSize = subTrain.getNoSeries();
                    java.util.Set<Integer> bestsetsharpe = new java.util.TreeSet<>();
                    double bestsharpe = Double.NEGATIVE_INFINITY;// Double.MIN_VALUE;
                    for (long l = 0; l < epochs.orElse(1000000L); l++) {
                        try {
                            java.util.Set<Integer> tempSet = Misc.getDistinctRandom(sizeOptimalSet, stockPoolSize);//getRandom(P, len);                        
                            double[] vec = new double[m_subTrain.length];
                            for (int i = 0; i < vec.length; i++) {
                                vec[i] = 0;
                                for (int j : tempSet) {
                                    vec[i] += m_subTrain[i][j];
                                }
                                vec[i] = vec[i] / sizeOptimalSet;
                            }
                            double sharpe = 0.0;
                            switch (optype) {
                                case MAXPROFIT:
                                    sharpe = DoubleArray.mean(vec);
                                    break;
                                case MAXSHARPE:
                                    sharpe = DoubleArray.mean(vec) / DoubleArray.std(vec);
                                    break;
                                case MINVAR:
                                    sharpe = 1.0 / DoubleArray.std(vec);
                                    break;
                                case MINDD:
                                    double[][] tmat = new double[vec.length][1];
                                    for (int z = 0; z < tmat.length; z++) {
                                        tmat[z][0] = z == 0 ? (Math.pow(10.0, vec[z] / 100.0)) : (Math.pow(10.0, vec[z] / 100.0)) * tmat[z - 1][0];
                                    }
                                    Fints tt2 = new Fints(subTrain.getDate(), Arrays.asList("temp"), subTrain.getFrequency(), tmat);
                                    sharpe = tt2.getMaxDD(0);
                                    break;
                                default:
                                    throw new Exception("unknow optmethod " + optype);
                            }

                            // double sharpe=DoubleArray.mean(vec);//DA CAMBIARE
                            if (Double.isFinite(sharpe)) {
                                if (sharpe > bestsharpe) {
                                    bestsharpe = sharpe;
                                    bestsetsharpe = tempSet;
                                }
                            }
                        } catch (Exception e) {
                            LOG.warn(e);
                        }
                    }
                    if (Double.isFinite(bestsharpe)) {
                        synchronized (Portfolio.class) {
                            winnerSetSharpe.put(bestsharpe, bestsetsharpe);
                            LOG.debug("new best " + bestsharpe + "\t" + bestsetsharpe);
                        }
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.HOURS);
            double lastEquityVal = allequitySharpe.isEmpty() ? 1.0 : allequitySharpe.getLastRow()[0];
            double lastEquityBH = allequitySharpe.isEmpty() ? 1.0 : allequitySharpe.getLastRow()[1];
            double[][] eqmat = new double[subTest.getLength()][2];
            if (winnerSetSharpe.isEmpty()) {//no winner found (e.g. sharpe > 0 )
                LOG.debug("no winner found");
                for (int i = 0; i < eqmat.length; i++) {
                    eqmat[i][0] = lastEquityVal;
                }
            } else {
                Fints temp_train = subTrain.SubSeries(new ArrayList<>(winnerSetSharpe.lastEntry().getValue()));
                Fints temp_test = subTest.SubSeries(new ArrayList<>(winnerSetSharpe.lastEntry().getValue()));
                LOG.info("trainopt" + temp_train);
                LOG.info("testopt" + temp_test);
                LOG.info("best value : " + winnerSetSharpe.lastEntry().getKey());
                LOG.info("train variance " + temp_train.getEqualWeightedCovariance());
                LOG.info("train mean " + DoubleArray.mean(temp_train.getMeans()));
                LOG.info("test variance " + temp_test.getEqualWeightedCovariance());
                LOG.info("test mean " + DoubleArray.mean(temp_test.getMeans()));

                for (int i = 0; i < eqmat.length; i++) {
                    double t1 = 0;
                    for (int j : winnerSetSharpe.lastEntry().getValue()) {
                        t1 += (Math.pow(10.0, m_subTest[i][j] / 100.0) - 1.0);
                    }
                    t1 = t1 / sizeOptimalSet;
                    eqmat[i][0] = i == 0 ? lastEquityVal * (1 + t1) : eqmat[i - 1][0] * (1 + t1);
                }
            }
            //calculate B&H
            for (int i = 0; i < eqmat.length; i++) {
                double t1 = 0;
                for (int j = 0; j < m_subTest[i].length; j++) {
                    t1 += (Math.pow(10.0, m_subTest[i][j] / 100.0) - 1.0);
                }
                t1 = t1 / m_subTest[i].length;
                eqmat[i][1] = i == 0 ? lastEquityBH * (1 + t1) : eqmat[i - 1][1] * (1 + t1);
            }
            LOG.info("final equity optimized " + eqmat[eqmat.length - 1][0]);
            LOG.info("final equity BH " + eqmat[eqmat.length - 1][1]);
            Fints equity = new Fints(subTest.getDate(), Arrays.asList("optimized", "b&h"), Fints.frequency.DAILY, eqmat);
            LOG.info("equity maxdd " + equity.getMaxDD(0));
            LOG.info("equity maxdd bh " + equity.getMaxDD(1));
            allequitySharpe = allequitySharpe.isEmpty() ? equity : Fints.append(allequitySharpe, equity);
            winnerSetSharpe.clear();
            step++;
        }
        allequitySharpe.merge(allequitySharpe.getLinReg(0)).merge(allequitySharpe.getLinReg(1)).plot("equity", "exret");
        LOG.info("allequity maxdd " + allequitySharpe.getMaxDD(0));
        LOG.info("allequity maxdd bh " + allequitySharpe.getMaxDD(1));

    }

    /*   public void walkForwardTest(Optional<Integer> train_window, Optional<Integer> test_window, Optional<Long> epochs, Optional<Integer> equalWeightSec) throws Exception {
        LOG.debug("Start walkforward test");
        int testWin = test_window.orElse(60);//default 60 samples for test window
        int trainWin = train_window.orElse(250);//default 250 samples for train window
        int sizeOptimalSet = equalWeightSec.orElse(10);//default 10 stock to pick each time
        LOG.debug("Train window size = " + trainWin + "\tTest window size = " + testWin);
        Fints smaSharpe = Fints.SMA(Fints.Sharpe(closeER, 20), 200);//begins 
        UDate startDate = smaSharpe.getDate(1);
        LOG.debug("start training from " + startDate);
        int offset_closeER = closeER.getIndex(startDate);
        if ((closeER.getLength() - testWin - trainWin - offset_closeER) <= 0) {
            throw new Exception("size too short, try to change parameters");
        }
        //boolean stopCond = false;
        int step = 0;
        Fints allequityMinVar = new Fints();
        Fints allequitySharpe = new Fints();
        while (true) {
            int ubound = offset_closeER + testWin * step + trainWin + testWin - 1;
            if (closeER.getLength() < ubound) {
                LOG.debug("terminato ubound=" + ubound + "\tlast=" + (closeER.getLength() - 1));
                break;
            }
            LOG.info("all " + closeER.getLength() + "\t" + closeER.getNoSeries());
            Fints subTrain = closeER.SubRows(offset_closeER + testWin * step, offset_closeER + testWin * step + trainWin - 1);
            Fints subTest = closeER.SubRows(offset_closeER + testWin * step + trainWin, offset_closeER + testWin * step + trainWin + testWin - 1);
            Fints trainMinVar = new Fints(), testMinVar = new Fints();
            double[] sharpeVal = smaSharpe.getRow(smaSharpe.getIndex(subTrain.getFirstDate()) - 1);
            ArrayList<String> trainSetMinVar = new java.util.ArrayList<>();
            ArrayList<String> trainSetSharpe = new java.util.ArrayList<>();
            for (int i = 0; i < sharpeVal.length; i++) {
                if (sharpeVal[i] > 0) {//chek sharpe>0
                    trainSetMinVar.add(this.securities.get(i).getHashcode());
                    trainMinVar = trainMinVar.isEmpty() ? subTrain.getSerieCopy(i) : trainMinVar.merge(subTrain.getSerieCopy(i));
                    testMinVar = testMinVar.isEmpty() ? subTest.getSerieCopy(i) : testMinVar.merge(subTest.getSerieCopy(i));
                }
                trainSetSharpe.add(this.securities.get(i).getHashcode());
            }
            
            if (sizeOptimalSet > trainMinVar.getNoSeries()) {
                throw new Exception("too few series " + trainMinVar.getNoSeries()+" , needed almost "+sizeOptimalSet);
            }
            
            //final double[][] covTest = subTestOK.getCovariance();
            ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            final Fints trainMinVarFinal = trainMinVar;
            final Fints testMinVarFinal = testMinVar;
            final Fints trainSharpeFinal=subTrain;
            final Fints testSharpeFinal=subTest;
            LOG.info("trainMinVar:"+trainMinVarFinal.toString());
            LOG.info("testMinVar:"+testMinVarFinal.toString());
            LOG.info("trainSharpe:"+trainSharpeFinal.toString());
            LOG.info("testSharpe:"+testSharpeFinal.toString());
            final double[][] covTrain = trainMinVar.getCovariance();
            
            LOG.info("train from " + trainMinVarFinal.getFirstDate() + " to " + trainMinVarFinal.getLastDate() + " samples " + trainMinVarFinal.getLength());
            LOG.info("test from " + testMinVarFinal.getFirstDate() + " to " + testMinVarFinal.getLastDate() + " samples " + testMinVarFinal.getLength());
            //final com.ettoremastrogiacomo.utils.Pair<Double,java.util.Set<Integer>> winnerPair;//=new com.ettoremastrogiacomo.utils.Pair<Double,java.util.Set<Integer>>();
            final java.util.TreeMap<Double, java.util.Set<Integer>> winnerSetMinVar = new java.util.TreeMap<>();
            final java.util.TreeMap<Double, java.util.Set<Integer>> winnerSetSharpe = new java.util.TreeMap<>();
            for (int k = 0; k < Runtime.getRuntime().availableProcessors(); k++) {
                pool.execute(() -> {
                    int sizeTrainMinVar = trainMinVarFinal.getNoSeries();
                    int sizeTrainSharpe = trainSharpeFinal.getNoSeries();
                    java.util.Set<Integer> bestsetMinVar = new java.util.TreeSet<>();
                    java.util.Set<Integer> bestsetsharpe = new java.util.TreeSet<>();
                    double bestMinVar = Double.MAX_VALUE;
                    double bestsharpe = Double.MIN_VALUE;
                    for (long l = 0; l < epochs.orElse(1000000L); l++) {
                        try {
                            java.util.Set<Integer> setMinVar = Misc.getDistinctRandom(sizeOptimalSet, sizeTrainMinVar);//getRandom(P, len);                        
                            double[] v = new double[sizeTrainMinVar];
                            setMinVar.forEach((i) -> {
                                v[i] = 1.0 / sizeOptimalSet;
                            });
                            double var = 0;
                            for (int i = 0; i < sizeTrainMinVar; i++) {
                                for (int j = 0; j < sizeTrainMinVar; j++) {
                                    var += v[i] * v[j] * covTrain[i][j];
                                }
                            }
                            if (var < bestMinVar) {
                                LOG.debug("best var at=" + l + "\ttid=" + Thread.currentThread().getId() + " : " + var);
                                bestsetMinVar = setMinVar;
                                bestMinVar = var;
                            }
                            java.util.Set<Integer> setsharpe = Misc.getDistinctRandom(sizeOptimalSet, sizeTrainSharpe);//getRandom(P, len);
                            double[] meanv = new double[trainWin];
                            for (int i = 0; i < trainSharpeFinal.getLength(); i++) {
                                double t1 = 0;
                                for (int j : setsharpe) {
                                    t1 += trainSharpeFinal.get(i, j);
                                }
                                meanv[i] = t1 / sizeTrainSharpe;
                            }
                            double shrp = DoubleArray.mean(meanv) / DoubleArray.std(meanv);
                            if (Double.isFinite(shrp))
                            if (shrp > bestsharpe) {
                                LOG.debug("best sharpe at=" + l + "\ttid=" + Thread.currentThread().getId() + " : " + shrp);
                                bestsetsharpe = setsharpe;
                                bestsharpe = shrp;
                            }
                            //results.put(set, var);
                        } catch (Exception e) {
                            LOG.error("error at thread " + Thread.currentThread().getName() + "\tmsg:" + e.getMessage());
                        }
                    }
                    if (Double.isFinite(bestMinVar)) {
                        synchronized (Portfolio.this) {
                            winnerSetMinVar.put(bestMinVar, bestsetMinVar);
                        }
                    }
                    if (Double.isFinite(bestsharpe)) {
                        synchronized (Portfolio.this) {
                            winnerSetSharpe.put(bestsharpe, bestsetsharpe);
                        }
                    }

                }
                );
            }
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.HOURS);
            LOG.info("bestvar " + winnerSetMinVar.firstEntry().getKey());
            LOG.info("size train minvar "+trainMinVarFinal.getNoSeries());
            LOG.debug("bestset");
            Fints nf = new Fints();
            winnerSetMinVar.firstEntry().getValue().forEach((x) -> {
                LOG.debug(names.get(trainSetMinVar.get(x)));
            });
            double[] w = new double[winnerSetMinVar.firstEntry().getValue().size()];
            for (int i = 0; i < w.length; i++) {
                w[i] = 1.0 / sizeOptimalSet;
            }
            for (int i : winnerSetMinVar.firstEntry().getValue()) {
                nf = nf.isEmpty() ? testMinVarFinal.getSerieCopy(i) : nf.merge(testMinVarFinal.getSerieCopy(i));
            }
            LOG.info("varianza test: " + nf.getWeightedCovariance(w));
            double[][] equitymat = new double[nf.getLength()][2];
            double[][] testptfmat = nf.getMatrixCopy();
            double[][] testptfallmat = testMinVarFinal.getMatrixCopy();
            double oldlasteq=allequityMinVar.isEmpty()?1:allequityMinVar.getLastRow()[0];
            double oldlasteqbh=allequityMinVar.isEmpty()?1:allequityMinVar.getLastRow()[1];
            for (int i = 0; i < testptfmat.length; i++) {
                double t1 = 0, t2 = 0;
                for (int j = 0; j < testptfmat[i].length; j++) {
                    t1 += (Math.pow(10.0, testptfmat[i][j] / 100.0) - 1.0);
                }
                for (int j = 0; j < testptfallmat[i].length; j++) {
                    t2 += (Math.pow(10.0, testptfallmat[i][j] / 100.0) - 1.0);
                }
                t1 = t1 / testptfmat[i].length;
                t2 = t2 / testptfallmat[i].length;
                equitymat[i][0] = i == 0 ? oldlasteq * (1 + t1) : equitymat[i - 1][0] * (1 + t1);
                equitymat[i][1] = i == 0 ? oldlasteqbh * (1 + t2) : equitymat[i - 1][1] * (1 + t2);
            }
            LOG.info("final equity minvar " + equitymat[equitymat.length - 1][0]);
            Fints equity = new Fints(nf.getDate(), Arrays.asList("equity", "b&h"), Fints.frequency.DAILY, equitymat);
            allequityMinVar = allequityMinVar.isEmpty() ? equity : Fints.append(allequityMinVar, equity);


            LOG.info("bestsharpe " + winnerSetSharpe.lastEntry().getKey());
            LOG.info("bestset");
            nf = new Fints();
            LOG.info("size winner sharpe"+winnerSetSharpe.lastEntry().getValue().size());
            LOG.info("size train sharpe"+trainSetSharpe.size());
            
            winnerSetSharpe.lastEntry().getValue().forEach((x) -> {
                LOG.debug(names.get( trainSetSharpe.get(x)));
            });            
            for (int i : winnerSetSharpe.lastEntry().getValue()) {
                nf = nf.isEmpty() ? testSharpeFinal.getSerieCopy(i) : nf.merge(testSharpeFinal.getSerieCopy(i));
            }
            equitymat = new double[nf.getLength()][2];
            testptfmat = nf.getMatrixCopy();
            testptfallmat = subTest.getMatrixCopy();
            double oldlasteqSharpe=allequitySharpe.isEmpty()?1:allequitySharpe.getLastRow()[0];
            double oldlasteqbhSharpe=allequitySharpe.isEmpty()?1:allequitySharpe.getLastRow()[1];            
            for (int i = 0; i < testptfmat.length; i++) {
                double t1 = 0, t2 = 0;
                for (int j = 0; j < testptfmat[i].length; j++) {
                    t1 += (Math.pow(10.0, testptfmat[i][j] / 100.0) - 1.0);
                }
                for (int j = 0; j < testptfallmat[i].length; j++) {
                    t2 += (Math.pow(10.0, testptfallmat[i][j] / 100.0) - 1.0);
                }
                t1 = t1 / testptfmat[i].length;
                t2 = t2 / testptfallmat[i].length;
                equitymat[i][0] = i == 0 ? oldlasteqSharpe * (1 + t1) : equitymat[i - 1][0] * (1 + t1);
                equitymat[i][1] = i == 0 ? oldlasteqbhSharpe * (1 + t2) : equitymat[i - 1][1] * (1 + t2);
            }
            LOG.info("final equity sharpe " + equitymat[equitymat.length - 1][0]);
            equity = new Fints(nf.getDate(), Arrays.asList("equity", "b&h"), Fints.frequency.DAILY, equitymat);
            allequitySharpe = allequitySharpe.isEmpty() ? equity : Fints.append(allequitySharpe, equity);
            step++;
            //stopCond=true;
        }
        allequityMinVar.plot("equityminvaropt", "exret");
        allequitySharpe.plot("equitysharpeopt", "exret");
    }
     */
    public double[] optimizeMinVar(Optional<Integer> window, Optional<Integer> window_offset, Optional<Long> epochs, Optional<Integer> equalWeightSec) throws Exception {
        LOG.debug("first serie" + closeER);
        if (window.isPresent()) {
            if (window.get() > closeER.getLength()) {
                throw new Exception("max windows = " + closeER.getLength());
            }
        }
        int eqSec = equalWeightSec.orElse(10);
        if (eqSec > closeER.getNoSeries()) {
            throw new Exception("no securities out of range: " + eqSec + ">" + closeER.getNoSeries());
        }
        Fints sub = closeER.SubRows(closeER.getLength() - window_offset.orElse(0) - window.orElse(closeER.getLength()), closeER.getLength() - window_offset.orElse(0) - 1);
        LOG.debug("sub serie" + sub);
        LOG.debug("length " + sub.getLength() + "\tno series " + sub.getNoSeries());
        LOG.debug("max date gap " + sub.getMaxDaysDateGap());
        double[][] cov = sub.getCovariance();

        double w = 1.0 / eqSec;
        //javafx.util.Pair<java.util.List<Integer>,Double> results=new Pair<java.util.List<Integer>,Double>();//=new javafx.util.Pair<java.util.List<Integer>,Double>();

        //double[] bestselection=new double[closeER.getNoSeries()];
        //final double bestvar=Double.MAX_VALUE;   
        //final java.util.HashMap<java.util.List<Integer>,Double> results=new java.util.HashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        //com.ettoremastrogiacomo.utils.DoubleDoubleArray.show(cov);
        java.util.ArrayList<Future> flist = new java.util.ArrayList<>();
        for (int k = 0; k < Runtime.getRuntime().availableProcessors(); k++) {
            flist.add(pool.submit(() -> {
                int len = closeER.getNoSeries();
                java.util.Set<Integer> bestset = new java.util.TreeSet<>();
                double bestvar = Double.MAX_VALUE;
                for (long l = 0; l < epochs.orElse(1000000L); l++) {
                    try {
                        //LOG.debug(Thread.currentThread().getName()+"\t"+l);

                        java.util.Set<Integer> set = Misc.getDistinctRandom(eqSec, len);//getRandom(P, len);
                        double[] v = new double[len];
                        set.forEach((i) -> {
                            v[i] = w;
                            // if (v[i]<0) throw new Exception("CAZ"+v[i]);
                        });
                        double var = 0;
                        for (int i = 0; i < len; i++) {
                            for (int j = 0; j < len; j++) {
                                var += v[i] * v[j] * cov[i][j];
                            }
                        }
                        if (var < bestvar) {
                            LOG.debug("best at=" + l + "\ttid=" + Thread.currentThread().getId() + " : " + var);
                            bestset = set;
                            bestvar = var;

                        }
                        //results.put(set, var);
                    } catch (Exception e) {
                        LOG.error("error at thread " + Thread.currentThread().getName() + "\tmsg:" + e.getMessage());
                    }
                }

                return Pair.of(bestset, bestvar);//new Pair(bestset, bestvar);
            }));
        }
        pool.shutdown();

        double bestvar = Double.MAX_VALUE;
        java.util.Set<Integer> bestset = new java.util.TreeSet<>();
        double[] v = new double[closeER.getNoSeries()];
        for (Future f : flist) {
            Pair p = (Pair) f.get();
            if ((Double) p.second < bestvar) {
                bestvar = (Double) p.second;
                bestset = (java.util.Set) p.first;
            }
        }

        bestset.forEach((i) -> {
            v[i] = w;
        });
        LOG.debug("\n\n\n\n\nnew best=" + bestvar);
        for (int i = 0; i < v.length; i++) {
            if (v[i] != 0.0) {
                LOG.debug(this.names.get(securities.get(i).getHashcode()) + "\t" + v[i]);
            }
        }

        return v;
    }

    public double[] optimizeSharpeBH(Optional<Integer> window, Optional<Integer> window_offset, Optional<Long> epochs, Optional<Integer> equalWeightSec) throws Exception {
        LOG.debug("first serie" + closeER);
        if (window.isPresent()) {
            if (window.get() > closeER.getLength()) {
                throw new Exception("max windows = " + closeER.getLength());
            }
        }

        int eqSec = equalWeightSec.orElse(10);
        if (eqSec > closeER.getNoSeries()) {
            throw new Exception("no securities out of range: " + eqSec + ">" + closeER.getNoSeries());
        }
        Fints sub = closeER.SubRows(closeER.getLength() - window_offset.orElse(0) - window.orElse(closeER.getLength()), closeER.getLength() - window_offset.orElse(0) - 1);
        LOG.debug("sub serie" + sub);
        LOG.debug("length " + sub.getLength() + "\tno series" + sub.getNoSeries());
        LOG.debug("max date gap " + sub.getMaxDaysDateGap());
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        //com.ettoremastrogiacomo.utils.DoubleDoubleArray.show(cov);
        java.util.TreeMap<Double, java.util.ArrayList<Object>> results = new java.util.TreeMap<>();
        java.util.ArrayList<Future> flist = new java.util.ArrayList<>();
        Fints besteq = new Fints();
        BUYANDHOLD bh = new BUYANDHOLD(Portfolio.this);
        com.ettoremastrogiacomo.sktradingjava.backtesting.Backtest bt = new com.ettoremastrogiacomo.sktradingjava.backtesting.Backtest(Portfolio.this, Optional.of(1000.0), Optional.empty(), Optional.empty());
        for (int k = 0; k < Runtime.getRuntime().availableProcessors(); k++) {
            flist.add(pool.submit(() -> {
                int len = closeER.getNoSeries();
                java.util.Set<Integer> bestset = new java.util.TreeSet<>();
                double bestvar = Double.MIN_VALUE;
                for (long l = 0; l < epochs.orElse(1000000L); l++) {
                    try {
                        java.util.Set<Integer> set = Misc.getDistinctRandom(eqSec, len);//getRandom(P, len);
                        double[] v;
                        v = new double[len];
                        set.forEach((i) -> {
                            v[i] = 1.0 / eqSec;
                        });
                        Statistics stats = bt.apply(bh.apply(sub.getFirstDate(), sub.getLastDate(), v), sub.getFirstDate(), sub.getLastDate());
                        double var = stats.linregsharpe;
                        if (var > bestvar) {
                            bestset = set;
                            bestvar = var;
                            LOG.debug("best at=" + l + "\ttid=" + Thread.currentThread().getId() + " : " + stats.linregsharpe);
                            java.util.ArrayList<Object> tlist = new java.util.ArrayList<>();
                            tlist.add(set);
                            tlist.add(stats.equity);
                            synchronized (Portfolio.this) {
                                results.put(var, tlist);
                            }
                        }
                    } catch (Exception e) {
                        LOG.error("error at thread " + Thread.currentThread().getName() + "\tmsg:" + e.getMessage());
                    }
                }
                //java.util.HashMap<Set,Double> retval=
                return Pair.of(bestset, bestvar);
            }));
        }
        for (Future f : flist) {
            Pair p = (Pair) f.get();
        }

        pool.shutdown();

        double bestvar = results.lastKey();// Double.MIN_VALUE;
        java.util.Set<Integer> bestset = new java.util.TreeSet<>();
        double[] v = new double[closeER.getNoSeries()];
        for (Future f : flist) {
            Pair p = (Pair) f.get();
            /*if ((Double) p.second > bestvar) {
                bestvar = (Double) p.second;
                bestset = (java.util.Set) p.first;
            }*/
        }
        java.util.ArrayList<Object> last = results.lastEntry().getValue();
        bestset = (Set<Integer>) last.get(0);
        besteq = (Fints) last.get(1);
        bestset.forEach((i) -> {
            v[i] = 1.0 / eqSec;
        });
        //LOG.debug("\n\n\n\n\nnew best=" + bestvar);
        LOG.debug("\n\n\n\n\nnew best=" + bestvar);

        for (int i = 0; i < v.length; i++) {
            if (v[i] != 0.0) {
                LOG.debug(this.names.get(securities.get(i).getHashcode()) + "\t" + v[i]);
                //LOG.debug(this.securities.get(i).getName() + "\t" + v[i]);
            }
        }
        besteq.plot("equity", "val");
        return v;
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

    public static void main(String[] args) throws Exception {
        java.util.ArrayList<String> isins = new java.util.ArrayList<>();

        //List<HashMap<String,String>> records=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.empty(), Optional.of(Arrays.asList("EUR")), Optional.empty());
        List<HashMap<String, String>> records = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.empty(), Optional.of(Arrays.asList("EUR")), Optional.empty());
        List<String> hashes = new java.util.ArrayList<>();
        records.forEach((x) -> {
            hashes.add(x.get("hashcode"));
        });
        java.util.ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashes), Optional.of(250), Optional.of(.2), Optional.of(7), Optional.of(20), Optional.of(100000), Optional.empty());
        Portfolio ptf = new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        double[] d = ptf.optimizeMinVarQP(Optional.empty(), Optional.empty(), Optional.of(0.05));
        double[] d2 = ptf.optimizeMinVar(Optional.empty(), Optional.empty(), Optional.of(5000000L), Optional.of(50));
        //double[] d3 = ptf.optimizeSharpeBH(Optional.empty(), Optional.empty(), Optional.of(1000000L), Optional.of(20));
        //com.ettoremastrogiacomo.utils.DoubleArray.show(d);
    }
}
