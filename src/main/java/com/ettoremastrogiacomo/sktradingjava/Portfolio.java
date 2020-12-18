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

    static double[][] m;
    static int setmin, setmax, poolsize, samplelen;
    static Portfolio.optMethod met;
    static double[][] cov;
    static double[][] extcorr;
    static double[] meanbycols;
    static boolean duplicates;
    static int popsize, generations;
    static final Logger LOG = Logger.getLogger(GeneticOpt.class);
    GeneticOpt(double[][] m, int setmin, int setmax, Portfolio.optMethod met, Optional<Boolean> duplicates, Optional<Integer> popsize, Optional<Integer> generations) throws Exception {
        GeneticOpt.m = m;
        if (!DoubleDoubleArray.isFinite(m)) throw new Exception("not finite er matrix");
        GeneticOpt.setmax = setmax;
        GeneticOpt.setmin = setmin;
        GeneticOpt.met = met;
        GeneticOpt.cov = DoubleDoubleArray.cov(m);
        if (!DoubleDoubleArray.isFinite(cov)) throw new Exception("not finite cov matrix");
        GeneticOpt.poolsize = cov.length;
        GeneticOpt.samplelen = m.length;
        GeneticOpt.meanbycols = DoubleDoubleArray.mean(m);
        if (!DoubleArray.isFinite(meanbycols)) throw new Exception("not finite means vector");
        GeneticOpt.duplicates = duplicates.orElse(false);
        GeneticOpt.popsize = popsize.orElse(10000);
        GeneticOpt.generations = generations.orElse(1000);
        double[][] extm = new double[m.length][poolsize + 1];
        for (int i = 0; i < extm.length; i++) {
            for (int j = 0; j <= poolsize; j++) {
                if (j < poolsize) {
                    extm[i][j] = m[i][j];
                } else {
                    double d = DoubleArray.sum(m[i]);
                    d /= poolsize;
                    extm[i][j] = d;
                }
            }
        }
        extcorr = DoubleDoubleArray.corr(extm);//l'ultima colonna rappresenta la correlazione con il campione medio
        if (setmax > poolsize) {
            throw new Exception("optimal set greather than available set " + setmax + ">" + poolsize);
        }
        if (setmin > setmax) {
            throw new Exception("wrong set size " + setmin + ">" + setmax);
        }
        if (setmin < 2) {
            throw new Exception("setmin too short " + setmin);
        }
        
    }

    private static ArrayList<Integer> toArray(Genotype<IntegerGene> gt) {

        ArrayList<Integer> arr = new ArrayList<>();

        if (duplicates) {
            gt.getChromosome().as(IntegerChromosome.class).stream().sorted().forEach((x) -> {
                arr.add(x.intValue());
            });
        } else {
            gt.getChromosome().as(IntegerChromosome.class).stream().distinct().sorted().forEach((x) -> {
                arr.add(x.intValue());
            });
        }
        return arr;
    }

    private static double eval(Genotype<IntegerGene> gt) {
        ArrayList<Integer> set = toArray(gt);
        double fitness = Double.NEGATIVE_INFINITY;
        if (set.size() < setmin) {
            return fitness;
        }
        if (set.size() > setmax) {
            return fitness;
        }
        double[] eqt = new double[samplelen];
        double w = 1.0 / set.size();
        //build var
        double var = 0, meanret = 0;
        for (Integer sa1 : set) {
            for (Integer sa2 : set) {
                var += w * w * cov[sa1][sa2];
            }
            meanret += meanbycols[sa1];
        }
        meanret /= set.size();
        double vcorr = 0;
        for (Integer sa1 : set) {
            vcorr += extcorr[sa1][poolsize];
        }
        

        //build equity
        for (int i = 0; i < samplelen; i++) {
            double mean = 0;
            for (Integer sa1 : set) {
                mean += m[i][sa1];
            }
            mean = mean / set.size();
            eqt[i] = i == 0 ? 1 + mean : eqt[i - 1] * (1 + mean);
        }
        HashMap<String, Double> lrmap = new HashMap<>();
        if (Double.isNaN(eqt[0])){
            LOG.debug("NAN equity, please check");
            
            
        }
        try {
            lrmap = DoubleArray.LinearRegression(eqt);
        } catch (Exception e) {
            System.err.print("errore " + e);
        }
        switch (met) {
            case MAXPROFIT: {
                fitness = eqt[eqt.length - 1];
            }
            break;
            case MAXPROFITNSHARES: {
                fitness = eqt[eqt.length - 1] * (1 + set.size() * .005);
            }
            break;
            case MINDD: {
                try {
                    fitness = DoubleArray.maxDrowDownPerc(eqt);
                } catch (Exception e) {
                    LOG.warn("error MINDD "+e);
                    
                }
            }
            break;
            case MAXSLOPE: {
                fitness = lrmap.get("slope");
            }
            break;
            case MAXSHARPE: {
                fitness = lrmap.get("slope") / lrmap.get("stderr");
            }
            break;
            case MINSTDERR: {
                fitness = 1.0 / lrmap.get("stderr");
            }
            break;
            case PROFITMINDDRATIO: {
                try {
                    fitness = eqt[eqt.length - 1] / Math.abs(DoubleArray.maxDrowDownPerc(eqt));
                } catch (Exception e) {
                    LOG.warn("error PROFITMINDDRATIO "+e);
                }
            }
            break;
            case MINVAR: {
                fitness = 1.0 / var;
            }
            break;
            case SMASHARPE: {
                fitness = meanret / var;
            }
            break;
            case MINCORR: {
                fitness = -vcorr;
            }
            break;
            case MINVARBARRIER: {
                if (meanret > 0) {
                    fitness = 1.0 / var;
                }
            }
            break;
            case MAXCORR: {
                fitness = vcorr;
            }
            break;
            case MAXVAR: {
                fitness = var;
            }
            break;
            default:
//                                throw new Exception("not yet implemented");
        }
        if (Double.isNaN(fitness) || Double.isInfinite(fitness)) {
            fitness = Double.NEGATIVE_INFINITY;
        }
        return fitness;
    }

    public AbstractMap.SimpleEntry<Double, ArrayList<Integer>> run() {
        Factory<Genotype<IntegerGene>> gtf = setmin == setmax
                ? Genotype.of(IntegerChromosome.of(0, poolsize - 1, setmin))
                : Genotype.of(IntegerChromosome.of(IntRange.of(0, poolsize - 1), IntRange.of(setmin, setmax)));// 

        Engine< IntegerGene, Double> engine = Engine
                .builder(GeneticOpt::eval, gtf).populationSize(GeneticOpt.popsize)
                .build();
        Genotype<IntegerGene> result = engine.stream()
                .limit(GeneticOpt.generations)
                .collect(EvolutionResult.toBestGenotype());
        //Genotype<IntegerGene> resultEff= duplicates ? result: result.stream().distinct().sorted();
        ArrayList<Integer> aresult = toArray(result);
        // System.out.println("risultato:  \t"+aresult);
        double mfit = GeneticOpt.eval(result);
        //System.out.println("\neval:  \t" + mfit);
        return new AbstractMap.SimpleEntry<>(mfit, aresult);
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
    public final List<String> hashcodes;
    public final List<String> realnames;
    private final java.util.ArrayList<String> tmp_hashcodes;
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
    public final Fints closeER, closeCampione;
    static final Logger LOG = Logger.getLogger(Portfolio.class);

    /**
     *
     */
    public enum optMethod {
        MINVAR,
        MINVARBARRIER,
        MAXSHARPE,
        MAXPROFIT,
        MAXPROFITNSHARES,
        MINDD,
        MAXSLOPE,
        MINSTDERR,
        PROFITMINDDRATIO,
        SMASHARPE,
        MINCORR,
        MAXVAR,
        MAXCORR
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
        this.tmp_hashcodes = new java.util.ArrayList<>();
        for (String s : hashcodes) {
            if (this.tmp_hashcodes.contains(s)) {
                LOG.warn("symbol " + s + " already inserted, skipping");
                continue;
            }
            this.tmp_hashcodes.add(s);
            try {
                Security t1=new com.ettoremastrogiacomo.sktradingjava.Security(s);
                Fints ft1=t1.getDaily();
                
                if (DoubleDoubleArray.check_exists_le(ft1.getSerieCopy(Security.SERIE.CLOSE.getValue()).getMatrixCopy(), 0.)){
                    throw new Exception ("<=0 in close matrix for "+t1.getName()+"\t"+t1.getCode()+"\t"+t1.getMarket()+"\t"+t1.getIsin());                    
                }
                if (!DoubleDoubleArray.isFinite(ft1.getMatrixCopy())) {
                    throw new Exception ("infinite matrix for "+t1.getName()+"\t"+t1.getCode());                    
                }
                this.securities.add(t1);
                
            } catch (Exception e) {
                LOG.warn("cannot add security : " + e.getMessage());
            }
        }
        this.hashcodes = Collections.unmodifiableList(this.tmp_hashcodes);
        //this.tmp_hashcodes=Collections.unmodifiableList(this.tmp_hashcodes);
        names = Database.getCodeMarketName(this.tmp_hashcodes);
        ArrayList<String> tmp_realNames = new java.util.ArrayList<>();
        //TreeSet<Integer> hashSetToTreeSet = new TreeSet<>(set); 
        this.hashcodes.forEach((x) -> {
            tmp_realNames.add(this.names.get(x));
        });
        this.realnames = Collections.unmodifiableList(tmp_realNames);
        nosecurities = securities.size();
        if (nosecurities==0) throw new Exception("empty portfolio");
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
                case MINUTES3:
                    f = s.getIntradayMinutes3(iday.get());
                    break;
                case MINUTES5:
                    f = s.getIntradayMinutes5(iday.get());
                    break;
                case MINUTES10:
                    f = s.getIntradayMinutes10(iday.get());
                    break;

                case MINUTES15:
                    f = s.getIntradayMinutes15(iday.get());
                    break;
                case MINUTES30:
                    f = s.getIntradayMinutes30(iday.get());
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

        double[][] mat = new double[closeER.getLength() + 1][1];
        mat[0][0] = 1;
        for (int i = 1; i < mat.length; i++) {
            double d = 0;
            for (int j = 0; j < closeER.getNoSeries(); j++) {
                d += closeER.get(i - 1, j);
            }
            d = d / closeER.getNoSeries();
            mat[i][0] = mat[i - 1][0] * (1 + d);
        }
        closeCampione = new Fints(close.getDate(), Arrays.asList("campione"), close.getFrequency(), mat);
    }

    public String getName(String hashcode) {
        //if (!this.tmp_hashcodes.contains(hashcode)) throw new RuntimeException(hashcode+"\t not found");
        return this.names.getOrDefault(hashcode, "NOT FOUND");
    }

    /**
     *
     * @param set
     * @return
     */
    public ArrayList<String> set2names(Set<Integer> set) {
        ArrayList<String> list = new java.util.ArrayList<>();
        TreeSet<Integer> hashSetToTreeSet = new TreeSet<>(set);
        hashSetToTreeSet.forEach((x) -> {
            list.add(this.names.get(this.tmp_hashcodes.get(x)));
        });
        return list;
    }

    public ArrayList<String> list2names(ArrayList<Integer> set) {
        ArrayList<String> list = new java.util.ArrayList<>();
        //TreeSet<Integer> hashSetToTreeSet = new TreeSet<>(set); 
        set.forEach((x) -> {
            list.add(this.names.get(this.tmp_hashcodes.get(x)));
        });
        return list;
    }

    public ArrayList<String> list2hashes(ArrayList<Integer> set) {
        ArrayList<String> list = new java.util.ArrayList<>();
        //TreeSet<Integer> hashSetToTreeSet = new TreeSet<>(set); 
        set.forEach((x) -> {
            list.add(this.tmp_hashcodes.get(x));
        });
        return list;
    }
    
    
    public static Portfolio createStockEURPortfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        ArrayList<String> markets = Database.getMarkets();

        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio createMLSEStockEURPortfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio createNYSEStockUSDPortfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("NYSE")), Optional.of(Arrays.asList("USD")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio createEURStockEURPortfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.empty(), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio createETFSTOCKEURPortfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {

        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and sector like '%CLASSE 2 IND AZIONARIO%' and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    public static Portfolio create_ETF_NYSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {

        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='NYSE' and not name like '%Ultra%' and not name like '%Short%' and not name like '%Bear%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    
/*
            ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%TIPO STRUMENTO=ETF ATTIVI%'"));
        map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%TIPO STRUMENTO=ETF STRUTTURATI%'"));
        map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%'"));
        map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI;CLASSE=CLASSE 2 IND AZIONARIO%'"));
        map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI;CLASSE=CLASSE 1 IND OBBLIGAZIONARIO%'"));

    */
    public static Portfolio create_ETF_STRUTTURATI_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%TIPO STRUMENTO=ETF STRUTTURATI%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    public static Portfolio create_ETF_ATTIVI_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%TIPO STRUMENTO=ETF ATTIVI%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio create_ETF_INDICIZZATI_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio create_ETF_INDICIZZATI_AZIONARIO_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%' and upper(sector) like '%CLASSE=CLASSE 2 IND AZIONARIO%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio create_ETF_INDICIZZATI_AZIONARIO_exCOMMODITIES_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%' and upper(sector) like '%CLASSE=CLASSE 2 IND AZIONARIO%' and not upper(sector) like '%COMMODITIES%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static Portfolio create_ETF_INDICIZZATI_AZIONARIO_GLOBALI_exCOMMODITIES_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%' and upper(sector) like '%CLASSE=CLASSE 2 IND AZIONARIO%' and not upper(sector) like '%COMMODITIES%' and (upper(sector) like '%GLOBAL%' or upper(sector) like '%WORLD%' or upper(sector) like '%MONDO%')"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    public static Portfolio create_ETF_INDICIZZATI_GLOBALI_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%'  and (upper(sector) like '%GLOBAL%' or upper(sector) like '%WORLD%' or upper(sector) like '%MONDO%')"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    
    public static Portfolio create_ETF_INDICIZZATI_OBBLIGAZIONARIO_MLSE_Portfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and upper(sector) like '%SEGMENTO=ETF INDICIZZATI%' and upper(sector) like '%CLASSE=CLASSE 1 IND OBBLIGAZIONARIO%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
    
    public static Portfolio createETFEURPortfolio(Optional<Integer> minlen, Optional<Double> maxgap, Optional<Integer> maxdaygap, Optional<Integer> maxold, Optional<Integer> minvol) throws Exception {
        //ArrayList<String> markets = Database.getMarkets();
        ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.of(" where type= 'ETF' and market='MLSE' and (sector like '%CLASSE 2 IND AZIONARIO%' or sector like '%OBBLIGAZIONARIO%') and not sector like '%Benchmark:=COMMODITIES%' and not sector like '%HEDGED%'"));
        //ArrayList<HashMap<String, String>> map = Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("ETF")), Optional.of(markets), Optional.of(Arrays.asList("EUR")), Optional.empty());
        ArrayList<String> hashcodes = new ArrayList<>();
        map.forEach((x) -> {
            hashcodes.add(x.get("hashcode"));
        });
        ArrayList<String> list = Database.getFilteredPortfolio(Optional.of(hashcodes), minlen, maxgap, maxdaygap, maxold, minvol, Optional.empty());
        return new Portfolio(list, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     *
     * @param i
     * @param headlen campioni in testa da analizzare
     * @return beta del i-esimo titolo nel portafoglio rispetto alla media di
     * tutti
     * @throws Exception
     */
    public double getBeta(int i, int headlen) throws Exception {
        //Fints ref= createFintsFromPortfolio(this, "campione");
        Fints sec = this.securities.get(i).getDaily().getSerieCopy(Security.SERIE.CLOSE.getValue());
        double[][] c = Fints.ER(Fints.merge(closeCampione, sec), 100, true).head(headlen).getCovariance();
        return c[0][1] / c[0][0];
    }

        /**
     *
     * @param i
     * @param j
     * @param headlen campioni in testa da analizzare
     * @return beta del i-esimo titolo nel portafoglio rispetto alla media di
     * tutti
     * @throws Exception
     */
    public double getBetaTo(int i,int j, int headlen) throws Exception {
        //Fints ref= createFintsFromPortfolio(this, "campione");
        Fints sec1 = this.securities.get(i).getDaily().getSerieCopy(Security.SERIE.CLOSE.getValue());
        Fints sec2 = this.securities.get(i).getDaily().getSerieCopy(Security.SERIE.CLOSE.getValue());
        double[][] c = Fints.ER(Fints.merge(sec2, sec1), 100, true).head(headlen).getCovariance();
        return c[0][1] / c[0][0];
    }

    
    public double getVariance(double[] weights, int headlen) throws Exception {
        double v = 0;
        return closeER.head(headlen).getWeightedCovariance(weights);
    }

    public double getLOGVariance(double[] weights, int headlen) throws Exception {
        double v = 0;
        return closeERlog.head(headlen).getWeightedCovariance(weights);
    }

    /**
     *
     * @param i
     * @param headlen campioni in testa da analizzare
     * @return correlazione del i-esimo titolo nel portafoglio rispetto alla
     * media di tutti
     * @throws Exception
     */
    public double getCorrelation(int i, int headlen) throws Exception {
        //Fints ref= createFintsFromPortfolio(this, "campione");
        Fints sec = this.securities.get(i).getDaily().getSerieCopy(Security.SERIE.CLOSE.getValue());
        double[][] c = Fints.ER(Fints.merge(closeCampione, sec), 100, true).head(headlen).getCorrelation();
        return c[0][1];
    }

        /**
     *
     * @param i serie 1 idx 
     * @param j serie 2 idx
     * @param headlen campioni in testa da analizzare
     * @return correlazione del i-esimo titolo nel portafoglio rispetto alla
     * media di tutti
     * @throws Exception
     */
    public double getCorrelationTo(int i, int j,int headlen) throws Exception {
        //Fints ref= createFintsFromPortfolio(this, "campione");
        Fints sec1 = this.securities.get(i).getDaily().getSerieCopy(Security.SERIE.CLOSE.getValue());
        Fints sec2 = this.securities.get(j).getDaily().getSerieCopy(Security.SERIE.CLOSE.getValue());
        double[][] c = Fints.ER(Fints.merge(sec2, sec1), 100, true).head(headlen).getCorrelation();
        return c[0][1];
    }

    public Fints getWeightedtEquity(double[] weights) throws Exception {
        
        double[][] cer=this.close.getMatrixCopy();
        double[][] meq=new double[cer.length][1];
        meq[0][0]=1.0;
        if (weights.length!= cer[0].length) throw new RuntimeException(" wrong weights len ");
        double sw=DoubleArray.sum(weights);
        for (int i=0;i<weights.length;i++) weights[i]/=sw;
        for (int i=1;i<cer.length;i++){
            double cv=0;
            for (int j=0;j<cer[i].length;j++){
                cv+=(cer[i][j]-cer[i-1][j])*weights[j]/cer[i-1][j];                
            }
            meq[i][0]=meq[i-1][0]*(1+cv);        
        }
        return new Fints(dates, Arrays.asList("Weighted Equity"), freq, meq);
    }
    
    public static double equityEfficiency(Fints alleq, int idxeq, int idxbh) throws Exception {
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
            dm = setsize > 0 ? dm / setsize : 0;
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
        /*        
        A minimization problem in the form of:
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
        LOG.debug("\nbest qp=" + bestvar);
        //for (int i = 0; i < sol.length; i++) {
        //  if (sol[i] > 0.001) {
        //LOG.debug(this.names.get(securities.get(i).getHashcode()) + "\t" + sol[i]);
        //LOG.debug(this.securities.get(i).getName() + "\t" + sol[i]);
        //}
        //}
        return sol;
    }

    public Entry<Double, ArrayList<Integer>> opttrain(UDate train_startdate, UDate train_enddate, int setmin, int setmax, optMethod optype, boolean dups, int popsize, int ngen) throws Exception {
        GeneticOpt go = new GeneticOpt(closeER.Sub(train_startdate, train_enddate).getMatrixCopy(), setmin, setmax, optype, Optional.of(dups), Optional.of(popsize), Optional.of(ngen));
        Entry<Double, ArrayList<Integer>> winner = go.run();
        return winner;
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
    public Fints walkForwardTest(Optional<Integer> train_window, Optional<Integer> test_window, Optional<Integer> populationSize, Optional<Integer> generations, Optional<Integer> optsetmin, Optional<Integer> optsetmax, Optional<Boolean> duplicates, Optional<Portfolio.optMethod> optmet) throws Exception {
        int testWin = test_window.orElse(60);//default 60 samples for test window
        int trainWin = train_window.orElse(250);//default 250 samples for train window
        //int sizeOptimalSet = equalWeightSec.orElse(10);//default 10 stock to pick each time        
        Portfolio.optMethod optype = optmet.orElse(Portfolio.optMethod.MAXSHARPE);
        int popsize = populationSize.orElse(10000);
        int ngen = generations.orElse(1000);
        boolean dups = duplicates.orElse(Boolean.FALSE);
        //Fints exret = Fints.ER(this.close, 1, false);
        //double[][] exretmat = exret.getMatrixCopy();
        int step = 0;
        //int stockPoolSize = exret.getNoSeries();

        double lastequity = 1;
        double lastequitybh = 1;
        int setmin = optsetmin.orElse(8);//  sizeOptimalSet - (sizeOptimalSet/4);
        int setmax = optsetmax.orElse(12);//sizeOptimalSet + (sizeOptimalSet/4);        
        if (setmin < 1 || setmin > setmax || setmax > closeER.getNoSeries()) {
            throw new Exception("bad min max opt set size");
        }
        LOG.debug("trainWin " + trainWin);
        LOG.debug("testWin " + testWin);
        LOG.debug("total samples " + closeER.getLength());
        LOG.debug("date range " + closeER.getFirstDate() + "\t->\t" + closeER.getLastDate());
        LOG.debug("opt method " + optype);
        LOG.debug("runtime processors " + Runtime.getRuntime().availableProcessors());
        LOG.debug("population size " + popsize);
        LOG.debug("generations " + ngen);
        LOG.debug("duplicates " + dups);
        LOG.debug("set size " + "\t[" + setmin + "," + setmax + "]");
        LOG.debug("START");
        //LOG.debug("pool " + exret);
        Fints alleq = new Fints();
        ArrayList<Double> efficiencies = new ArrayList<>();
        while (true) {

            int offset = step * testWin;
            //LOG.debug("offset " + offset);

            if ((offset + trainWin + 2) >= closeER.getLength()) {
                LOG.debug("too few samples to test, ending");
                break;
            }
            UDate train_startdate, train_enddate;
            UDate test_startdate, test_enddate;
            train_startdate = closeER.getDate(offset);
            train_enddate = closeER.getDate(offset + trainWin - 1);

            if ((offset + trainWin + testWin) >= closeER.getLength()) {
                test_startdate = closeER.getDate(offset + trainWin);
                test_enddate = closeER.getLastDate();
            } else {
                test_startdate = closeER.getDate(offset + trainWin);
                test_enddate = closeER.getDate(offset + trainWin + testWin - 1);
            }
            //begin train
            LOG.debug("\nTRAIN");
            LOG.debug("date range  " + train_startdate + " -> " + train_enddate);
            LOG.debug("database " + closeER.Sub(train_startdate, train_enddate));

            GeneticOpt go = new GeneticOpt(closeER.Sub(train_startdate, train_enddate).getMatrixCopy(), setmin, setmax, optype, Optional.of(dups), Optional.of(popsize), Optional.of(ngen));
            Entry<Double, ArrayList<Integer>> winner = go.run();
            Fints eqtrain = opttest(winner.getValue(), train_startdate, train_enddate, Optional.empty(), Optional.empty());
            LOG.debug("train profit " + eqtrain.getLastValueInCol(0));
            LOG.debug("train profit BH " + eqtrain.getLastValueInCol(1));
            LOG.debug("maxdd " + eqtrain.getMaxDD(0));
            LOG.debug("maxdd BH " + eqtrain.getMaxDD(1));
            LOG.debug("samples " + eqtrain.getLength());
            LOG.debug("series " + closeER.getNoSeries());
            LOG.debug("overall best : " + winner.getKey() + "\t" + winner.getValue());
            LOG.debug("optimal set size : " + winner.getValue().size());
            List<String> fullnames = list2names(winner.getValue());
            fullnames.forEach((x) -> {
                LOG.debug(x);
            });

            //
            //begin test
            LOG.debug("\nTEST");
            LOG.debug("date range  " + test_startdate + " -> " + test_enddate);
            Fints eq = this.opttest(winner.getValue(), test_startdate, test_enddate, Optional.of(1.0), Optional.of(1.0));
            efficiencies.add(((eq.getLastValueInCol(0) - eq.getLastValueInCol(1)) / eq.getLastValueInCol(1)) * (eq.getMaxDD(1) / eq.getMaxDD(0)) / Math.log(eq.getLength()));
            LOG.debug("test profit " + eq.getLastValueInCol(0));
            LOG.debug("test profit BH " + eq.getLastValueInCol(1));
            LOG.debug("maxdd " + eq.getMaxDD(0));
            LOG.debug("maxdd BH " + eq.getMaxDD(1));
            LOG.debug("samples " + eq.getLength());
            LOG.debug("series " + winner.getValue().size());
            eq = this.opttest(winner.getValue(), test_startdate, test_enddate, Optional.of(lastequity), Optional.of(lastequitybh));
            if (alleq.isEmpty()) {
                alleq = eq;
            } else {
                alleq = Fints.append(alleq, eq);
            }
            lastequity = alleq.getLastValueInCol(0);// getLastRow()[0];
            lastequitybh = alleq.getLastValueInCol(1);
            LOG.debug("ALL test equity optimized " + lastequity + "\tmdd=" + alleq.getMaxDD(0));
            LOG.debug("ALL test equity bh " + lastequitybh + "\tmdd=" + alleq.getMaxDD(1));
            LOG.debug("ALL test equity info " + alleq);
            step++;
        }

        alleq = alleq.merge(Fints.merge(alleq.getLinReg(0), alleq.getLinReg(1)));
        LOG.debug("equity mdd " + alleq.getMaxDD(0));
        LOG.debug("equity bh mdd " + alleq.getMaxDD(1));
        HashMap<String, Double> st1 = DoubleArray.LinearRegression(alleq.getCol(0));
        HashMap<String, Double> st2 = DoubleArray.LinearRegression(alleq.getCol(1));
        LOG.debug("linreg slope " + st1.get("slope"));
        LOG.debug("linreg slope bh " + st2.get("slope"));
        LOG.debug("linreg stderr " + st1.get("stderr"));
        LOG.debug("linreg stderr bh " + st2.get("stderr"));
        LOG.debug("linreg intercept " + st1.get("intercept"));
        LOG.debug("linreg intercept bh " + st2.get("intercept"));

        double efficiency = ((alleq.getLastValueInCol(0) - alleq.getLastValueInCol(1)) / alleq.getLastValueInCol(1)) * (alleq.getMaxDD(1) / alleq.getMaxDD(0)) / Math.log(alleq.getLength());
        LOG.debug("alleq total efficiency " + efficiency);
        double[] eff = efficiencies.stream().mapToDouble(Double::doubleValue).toArray();
        LOG.debug("mean test efficiency " + DoubleArray.mean(eff));
        LOG.debug("max test efficiency " + DoubleArray.max(eff));
        LOG.debug("min test efficiency " + DoubleArray.min(eff));
        LOG.debug("std test efficiency " + DoubleArray.std(eff));
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
        int ret = this.tmp_hashcodes.indexOf(symbol);
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
        try {
            s.append("first date: ").append(this.getDate(0)).append("\n");
            s.append("last date: ").append(this.getDate(this.getLength() - 1)).append("\n");
            s.append("length: ").append(this.getLength()).append("\n");
        } catch (Exception e) {
        }
        s.append("num securities: ").append(this.getNoSecurities()).append("\n");
        s.append("date gap in days: ").append(this.allfints.getMaxDaysDateGap()).append("\n");
        s.append("days from now: ").append(this.allfints.getDaysFromNow()).append("\n");

        securities.forEach((x) -> s.append(names.get(x.getHashcode())).append("\n"));

        //names.keySet().forEach((x) -> {
        //   s.append(names.get(x)).append("\n");
        //});
        /*this.securities.forEach((x) -> {
            
            s.append(x.getIsin()).append("\t").append(x.getCode()).append(".").append(x.getMarket()).append("\t").append(x.getName()).append("\t").append(x.getSector()).append("\n");
        });*/
        return s.toString();
    }

}
