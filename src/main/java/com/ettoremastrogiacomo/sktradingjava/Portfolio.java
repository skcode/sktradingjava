package com.ettoremastrogiacomo.sktradingjava;

import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import com.ettoremastrogiacomo.utils.UDate;
import com.joptimizer.functions.ConvexMultivariateRealFunction;
import com.joptimizer.functions.LinearMultivariateRealFunction;
import com.joptimizer.functions.PDQuadraticMultivariateRealFunction;
import com.joptimizer.optimizers.JOptimizer;
import com.joptimizer.optimizers.OptimizationRequest;
import com.sun.tools.javac.util.ArrayUtils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
//import javafx.util.Pair;
import org.apache.log4j.Logger;
import java.util.Set;
import java.util.TreeSet;


import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.Genotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;
import io.jenetics.util.IntRange;



class GeneticOpt {
     static  double [][]m;
     static  int setmin,setmax,poolsize,samplelen;
     static  Portfolio.optMethod met;
     static  double[][]cov;
     static  double[] meanbycols;
     static  boolean duplicates;
     static  int popsize,generations;
    GeneticOpt(double[][]m,int setmin,int setmax,Portfolio.optMethod met,Optional<Boolean> duplicates,Optional<Integer> popsize,Optional<Integer> generations)throws Exception {
        GeneticOpt.m=m;
        GeneticOpt.setmax=setmax;GeneticOpt.setmin=setmin;GeneticOpt.met=met;
        GeneticOpt.cov=DoubleDoubleArray.cov(m);
        GeneticOpt.poolsize=cov.length;
        GeneticOpt.samplelen=m.length;
        GeneticOpt.meanbycols=DoubleDoubleArray.mean(m);
        GeneticOpt.duplicates=duplicates.orElse(false);
        GeneticOpt.popsize=popsize.orElse(10000);
        GeneticOpt.generations=generations.orElse(1000);
        if (setmax > poolsize) {
            throw new Exception("optimal set greather than available set " + setmax + ">" + poolsize);
        }
        if (setmin>setmax) throw new Exception("wrong set size "+setmin+">"+setmax);
        if (setmin<2) throw new Exception("setmin too short "+setmin);
    }     
     private static ArrayList<Integer> toArray(Genotype<IntegerGene> gt) {
       
        ArrayList<Integer> arr= new ArrayList<>();
          
          if (duplicates) {
              gt.getChromosome().as(IntegerChromosome.class).stream().sorted().forEach((x)->{arr.add(x.intValue());});
          }else {
              gt.getChromosome().as(IntegerChromosome.class).stream().distinct().sorted().forEach((x)->{arr.add(x.intValue());});
          }
        return arr;
    }
     
    private  static double eval(Genotype<IntegerGene> gt) {
         ArrayList<Integer> set= toArray(gt);         
        double fitness=Double.NEGATIVE_INFINITY;
        if (set.size()<setmin) return fitness;
        double[] eqt=new double[samplelen];
        double w=1.0/set.size();        
        //build var
        double var = 0,meanret=0;
        for (Integer sa1 : set) {
            for (Integer sa2 : set) {
                var += w * w * cov[sa1][sa2];
            }
            meanret+=meanbycols[sa1];
        }                    
        meanret/=set.size();
        //build equity
        for (int i = 0; i < samplelen; i++) {
            double mean = 0;
            for (Integer  sa1 : set) {
                mean += m[i][sa1];
            }
            mean = mean / set.size();
            eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
        }                       
        HashMap<String,Double> lrmap=new HashMap<>();
        try {
            lrmap=DoubleArray.LinearRegression(eqt);
        } catch (Exception e) {System.err.print("errore "+e);}
        switch (met){
                case MAXPROFIT:{fitness=eqt[eqt.length-1];}
                break;
                case MAXPROFITNSHARES:{fitness=eqt[eqt.length-1]*(1+set.size()*.005);}
                break;
                case MINDD:{
                    try {
                    fitness=DoubleArray.maxDrowDownPerc(eqt);}catch (Exception e ){System.err.print("errore "+e);}                
                }
                break;
                case MAXSLOPE:{fitness=lrmap.get("slope");}
                break;
                case MAXSHARPE:{fitness=lrmap.get("slope")/lrmap.get("stderr");}
                break;
                case MINSTDERR:{fitness=1.0/lrmap.get("stderr");}
                break;
                case PROFITMINDDRATIO :{
                    try {
                    fitness=eqt[eqt.length-1]/Math.abs(DoubleArray.maxDrowDownPerc(eqt));}
                    catch (Exception e){System.err.print("errore "+e);}
                }                
                break;
                case MINVAR:{fitness=1.0/var;}
                break;           
                case SMASHARPE:{fitness=meanret/var;}
                break;
                default:
//                                throw new Exception("not yet implemented");
        }
        if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
            fitness = Double.NEGATIVE_INFINITY;
        }        
        return fitness;
    }
    

    public AbstractMap.SimpleEntry<Double,ArrayList<Integer>> run() {
        Factory<Genotype<IntegerGene>> gtf = setmin==setmax?
            Genotype.of(IntegerChromosome.of(0, poolsize-1, setmin))
                : Genotype.of(IntegerChromosome.of(IntRange.of(0, poolsize-1),IntRange.of(setmin, setmax)));// 
        
        Engine< IntegerGene, Double> engine = Engine
            .builder(GeneticOpt::eval, gtf).populationSize(GeneticOpt.popsize)
            .build();        
        Genotype<IntegerGene> result = engine.stream()
            .limit(GeneticOpt.generations)
            .collect(EvolutionResult.toBestGenotype());        
        //Genotype<IntegerGene> resultEff= duplicates ? result: result.stream().distinct().sorted();
        ArrayList<Integer> aresult=toArray(result);
       // System.out.println("risultato:  \t"+aresult);
        double mfit=GeneticOpt.eval(result);
        //System.out.println("\neval:  \t" + mfit);
        return new AbstractMap.SimpleEntry<>(mfit,aresult);        
    }

}

/**
 *
 * @author ettore
 */
public class Portfolio {

    private Fints allfints, open, high, low, close, volume, oi;

    /**
     *
     */
    public final java.util.ArrayList<Security> securities;
    public final List<String> unmodifiable_hashcodes;
    
    private final java.util.ArrayList<String> hashcodes;    
    private final java.util.HashMap<String, String> names;
    private final int nosecurities;
    private final int length;

    /**
     *
     */
    public final java.util.List<UDate> dates;
    private final Fints.frequency freq;

    /**
     *
     */
    public final Fints closeERlog;

    /**
     *
     */
    public final Fints closeER;
    static final Logger LOG = Logger.getLogger(Portfolio.class);

    /**
     *
     */
    public enum optMethod {

        /**
         *
         */
        MINVAR,

        /**
         *
         */
        MINVARBARRIER,

        /**
         *
         */
        MAXSHARPE,

        /**
         *
         */
        MAXPROFIT,
        MAXPROFITNSHARES,

        /**
         *
         */
        MINDD,

        /**
         *
         */
        MAXSLOPE,

        /**
         *
         */
        MINSTDERR,

        /**
         *
         */
        PROFITMINDDRATIO,

        /**
         *
         */
        SMASHARPE
    };

    /**
     *
     * @param hashcodes
     * @param freq
     * @param iday
     * @param from
     * @param to
     * @throws Exception
     */
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
        this.unmodifiable_hashcodes = Collections.unmodifiableList(this.hashcodes);
        //this.hashcodes=Collections.unmodifiableList(this.hashcodes);
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

    
    public String getName(String hashcode) {
        //if (!this.hashcodes.contains(hashcode)) throw new RuntimeException(hashcode+"\t not found");
        return this.names.getOrDefault(hashcode,"NOT FOUND");
    }
    /**
     *
     * @param set
     * @return
     */
    public ArrayList<String> set2names(Set<Integer> set) {
        ArrayList<String> list=new java.util.ArrayList<>();
        TreeSet<Integer> hashSetToTreeSet = new TreeSet<>(set); 
        hashSetToTreeSet.forEach((x) -> {list.add(this.names.get(this.hashcodes.get(x)));});
        return list;
    }

    public ArrayList<String> list2names(ArrayList<Integer> set) {
        ArrayList<String> list=new java.util.ArrayList<>();
        //TreeSet<Integer> hashSetToTreeSet = new TreeSet<>(set); 
        set.forEach((x) -> {list.add(this.names.get(this.hashcodes.get(x)));});
        return list;
    }

    public static Portfolio createStockEURPortfolio(Optional<Integer> minlen,Optional<Double> maxgap,Optional<Integer>maxdaygap, Optional<Integer>maxold, Optional<Integer>minvol)throws Exception {
        ArrayList<String> markets = Database.getMarkets();

        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());    
    }

    public static Portfolio createMLSEStockEURPortfolio(Optional<Integer> minlen,Optional<Double> maxgap,Optional<Integer>maxdaygap, Optional<Integer>maxold, Optional<Integer>minvol)throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());    
    }
    
    
    public static Portfolio createETFSTOCKEURPortfolio(Optional<Integer> minlen,Optional<Double> maxgap,Optional<Integer>maxdaygap, Optional<Integer>maxold, Optional<Integer>minvol)throws Exception {

        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and sector like '%CLASSE 2 IND AZIONARIO%' and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());    
    }
    
    public static Portfolio createETFEURPortfolio(Optional<Integer> minlen,Optional<Double> maxgap,Optional<Integer>maxdaygap, Optional<Integer>maxold, Optional<Integer>minvol)throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and (sector like '%CLASSE 2 IND AZIONARIO%' or sector like '%OBBLIGAZIONARIO%') and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());    
    }
    
    public static double equityEfficiency(Fints alleq,int idxeq,int idxbh)throws Exception {
        return ((alleq.getLastValueInCol(idxeq) - alleq.getLastValueInCol(idxbh)) / alleq.getLastValueInCol(idxbh)) * (alleq.getMaxDD(idxbh) / alleq.getMaxDD(idxeq)) / Math.log(alleq.getLength());
    }
    
    /**
     *
     * @param set
     * @param startdate
     * @param enddate
     * @param lastequity
     * @param lastequitybh
     * @return
     * @throws Exception
     */
    public Fints opttest(ArrayList<Integer> set, UDate startdate, UDate enddate, Optional<Double> lastequity, Optional<Double> lastequitybh) throws Exception {
        Fints subf = closeER.Sub(startdate, enddate);
        double[][] m = subf.getMatrixCopy();        
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

    /**
     * 
     * @param train_window default 250
     * @param test_window default 60
     * @param populationSize default 10000
     * @param generations default 1000
     * @param optsetmin default 8
     * @param optsetmax default 12
     * @param duplicates default false
     * @param optmet default MAXSHARPE
     * @return 
     * @throws Exception 
     */
    public Fints walkForwardTest(Optional<Integer> train_window, Optional<Integer> test_window, Optional<Integer> populationSize,Optional<Integer> generations, Optional<Integer> optsetmin,Optional<Integer> optsetmax, Optional<Boolean> duplicates,Optional<Portfolio.optMethod> optmet) throws Exception {
        int testWin = test_window.orElse(60);//default 60 samples for test window
        int trainWin = train_window.orElse(250);//default 250 samples for train window
        //int sizeOptimalSet = equalWeightSec.orElse(10);//default 10 stock to pick each time        
        Portfolio.optMethod optype = optmet.orElse(Portfolio.optMethod.MAXSHARPE);
        int popsize=populationSize.orElse(10000);
        int ngen=generations.orElse(1000);
        boolean dups=duplicates.orElse(Boolean.FALSE);
        //Fints exret = Fints.ER(this.close, 1, false);
        //double[][] exretmat = exret.getMatrixCopy();
        int step = 0;
        //int stockPoolSize = exret.getNoSeries();

        double lastequity = 1;
        double lastequitybh = 1;
        int setmin=optsetmin.orElse(8) ;//  sizeOptimalSet - (sizeOptimalSet/4);
        int setmax=optsetmax.orElse(12);//sizeOptimalSet + (sizeOptimalSet/4);        
        if (setmin<1 || setmin>setmax || setmax>closeER.getNoSeries()) throw new Exception("bad min max opt set size");
        LOG.debug("trainWin " + trainWin);
        LOG.debug("testWin " + testWin);
        LOG.debug("total samples " + closeER.getLength());
        LOG.debug("date range " + closeER.getFirstDate()+"\t->\t"+closeER.getLastDate());
        LOG.debug("opt method " + optype);
        LOG.debug("runtime processors " + Runtime.getRuntime().availableProcessors());
        LOG.debug("population size " + popsize);
        LOG.debug("generations " + ngen);
        LOG.debug("duplicates " + dups);
        LOG.debug("set size "+"\t["+setmin+","+setmax+"]");
        LOG.debug("START");
        //LOG.debug("pool " + exret);
        Fints alleq = new Fints();
        ArrayList<Double> efficiencies= new ArrayList<>();
        while (true) {

            int offset = step * testWin;
            //LOG.debug("offset " + offset);

            if ((offset + trainWin + 2) >= closeER.getLength()) {
                LOG.debug("too few samples to test, ending");
                break;
            }
            UDate train_startdate,train_enddate;
            UDate test_startdate,test_enddate;
            train_startdate=closeER.getDate(offset);
            train_enddate=closeER.getDate(offset + trainWin - 1);
            
            if ((offset + trainWin + testWin) >= closeER.getLength()) {
                test_startdate=closeER.getDate(offset + trainWin);
                test_enddate=closeER.getLastDate();
            } else {
                test_startdate=closeER.getDate(offset + trainWin);
                test_enddate=closeER.getDate(offset + trainWin+testWin-1);
            }
            //begin train
            LOG.debug("\nTRAIN");
            LOG.debug("date range  " + train_startdate + " -> " + train_enddate);
            LOG.debug("database "+closeER.Sub(train_startdate, train_enddate));

            GeneticOpt go = new GeneticOpt(closeER.Sub(train_startdate, train_enddate).getMatrixCopy(),setmin,setmax,optype,Optional.of(dups),Optional.of(popsize),Optional.of(ngen));
            Entry<Double, ArrayList<Integer>> winner = go.run();
            Fints eqtrain = opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
            LOG.debug("train profit "+eqtrain.getLastValueInCol(0));
            LOG.debug("train profit BH "+eqtrain.getLastValueInCol(1));
            LOG.debug("maxdd "+eqtrain.getMaxDD(0));            
            LOG.debug("maxdd BH "+eqtrain.getMaxDD(1));            
            LOG.debug("samples "+eqtrain.getLength());
            LOG.debug("series "+closeER.getNoSeries());
            LOG.debug("overall best : " + winner.getKey() + "\t"+winner.getValue());
            LOG.debug("optimal set size : "+winner.getValue().size());
            List<String> fullnames=list2names(winner.getValue());
            fullnames.forEach((x)->{LOG.debug(x);});                
        
            
            //
            //begin test
            LOG.debug("\nTEST");
            LOG.debug("date range  " + test_startdate + " -> " + test_enddate);
            Fints eq = this.opttest(winner.getValue(), test_startdate, test_enddate, Optional.of(1.0), Optional.of(1.0));
            efficiencies.add(((eq.getLastValueInCol(0)-eq.getLastValueInCol(1))/eq.getLastValueInCol(1))*(eq.getMaxDD(1)/eq.getMaxDD(0))/Math.log(eq.getLength()));
            LOG.debug("test profit "+eq.getLastValueInCol(0));
            LOG.debug("test profit BH "+eq.getLastValueInCol(1));
            LOG.debug("maxdd "+eq.getMaxDD(0));            
            LOG.debug("maxdd BH "+eq.getMaxDD(1));            
            LOG.debug("samples "+eq.getLength());
            LOG.debug("series "+winner.getValue().size());
            eq = this.opttest(winner.getValue(), test_startdate, test_enddate, Optional.of(lastequity), Optional.of(lastequitybh));
            if (alleq.isEmpty()) {
                alleq = eq;
            } else {
                alleq = Fints.append(alleq, eq);
            }
            lastequity = alleq.getLastValueInCol(0);// getLastRow()[0];
            lastequitybh = alleq.getLastValueInCol(1);
            LOG.debug("ALL test equity optimized " + lastequity+"\tmdd="+alleq.getMaxDD(0));
            LOG.debug("ALL test equity bh " + lastequitybh+"\tmdd="+alleq.getMaxDD(1));
            LOG.debug("ALL test equity info "+alleq);
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
        
        double efficiency=((alleq.getLastValueInCol(0)-alleq.getLastValueInCol(1))/alleq.getLastValueInCol(1))*(alleq.getMaxDD(1)/alleq.getMaxDD(0))/Math.log(alleq.getLength());
        LOG.debug("alleq total efficiency "+efficiency);
        double[] eff= efficiencies.stream().mapToDouble(Double::doubleValue).toArray();
        LOG.debug("mean test efficiency "+DoubleArray.mean(eff));
        LOG.debug("max test efficiency "+DoubleArray.max(eff));
        LOG.debug("min test efficiency "+DoubleArray.min(eff));
        LOG.debug("std test efficiency "+DoubleArray.std(eff));
        return alleq;
        //check BH eq
        /*
        double[][] check_BH=closeER.Sub(alleq.getFirstDate(), alleq.getLastDate()).getMatrixCopy();        
        double [] checkeq= new double[check_BH.length];
        for (int i=0;i<check_BH.length;i++){
            double t1=DoubleArray.sum(check_BH[i])/check_BH[i].length;
            if (i==0) checkeq[0]=1+t1; else checkeq[i]=checkeq[i-1]*(1+t1);
        }
        LOG.debug("check eq BH = "+checkeq[checkeq.length-1]);*/

    }

    /**
     *
     * @return
     */
    public Fints.frequency getFrequency() {
        return this.freq;
    }

    /**
     *
     * @return
     */
    public int getLength() {
        return this.length;
    }

    /**
     *
     * @return
     */
    public int getNoSecurities() {
        return this.nosecurities;
    }

    /**
     *
     * @return
     */
    public java.util.List<UDate> getDate() {
        return allfints.getDate();
    }

    /**
     *
     * @param symbol
     * @return
     * @throws Exception
     */
    public int getOffset(String symbol) throws Exception {
        int ret = this.hashcodes.indexOf(symbol);
        if (ret < 0) {
            throw new Exception("symbol " + symbol + " not found");
        }
        return ret;
    }

    /**
     *
     * @param date
     * @return
     * @throws Exception
     */
    public int getOffset(UDate date) throws Exception {
        int ret = java.util.Arrays.binarySearch(dates.toArray(), date);
        if (ret < 0) {
            throw new Exception("Date " + date + " not found");
        }
        return ret;
    }

    /**
     *
     * @param idx
     * @return
     * @throws Exception
     */
    public UDate getDate(int idx) throws Exception {
        return dates.get(idx);
    }

    /**
     *
     * @return
     */
    public Fints getOpen() {
        return this.open;
    }

    /**
     *
     * @return
     */
    public Fints getHigh() {
        return this.high;
    }

    /**
     *
     * @return
     */
    public Fints getLow() {
        return this.low;
    }

    /**
     *
     * @return
     */
    public Fints getClose() {
        return this.close;
    }

    /**
     *
     * @return
     */
    public Fints getVolume() {
        return this.volume;
    }

    /**
     *
     * @return
     */
    public Fints getOI() {
        return this.oi;
    }

    /**
     *
     * @return
     */
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
        
        securities.forEach((x)-> s.append(names.get(x.getHashcode())).append("\n"));
        
        //names.keySet().forEach((x) -> {
         //   s.append(names.get(x)).append("\n");
        //});
        /*this.securities.forEach((x) -> {
            
            s.append(x.getIsin()).append("\t").append(x.getCode()).append(".").append(x.getMarket()).append("\t").append(x.getName()).append("\t").append(x.getSector()).append("\n");
        });*/
        return s.toString();
    }

}
