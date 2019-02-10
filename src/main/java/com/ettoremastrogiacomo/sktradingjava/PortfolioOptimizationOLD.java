/*
 * PortfolioOptimization.java
 *
 * Created on 23 gennaio 2007, 18.02
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava;

/**
 *
 * @author speedking
 */
import com.ettoremastrogiacomo.utils.UDate;
import org.apache.log4j.Logger;
//import org.jgap.impl.*;
//import org.jgap.*;
/*
class Fitness_MinimumVariance extends FitnessFunction {

    static final long serialVersionUID = 0;
    private PortfolioOptimization pf;
    private int dim;
    private double minpc, maxpc;

    public Fitness_MinimumVariance(PortfolioOptimization pf) {
        this.pf = pf;
        dim = pf.getNoSecurities();
        this.minpc = pf.getMinpc();
        this.maxpc = pf.getMaxpc();
    }

    @Override
    public double evaluate(org.jgap.IChromosome a_subject) {
        double[] w = new double[dim];

        double tot;

        for (int i = 0; i < a_subject.size(); i++) {

            double a = (Double) a_subject.getGene(i).getAllele();
            w[i] = a;
        }
        tot = com.ettoremastrogiacomo.utils.DoubleArray.sum(w);
        boolean outlimit = false;
        for (int i = 0; i < a_subject.size(); i++) {
            w[i] /= tot;
            if ((w[i] < this.minpc) || (w[i] > this.maxpc)) {
                outlimit = true;
            }
        }

        tot = com.ettoremastrogiacomo.utils.DoubleArray.sum(w);
        try {
            pf.setWeights(w);
        } catch (Exception e) {
        }
        if ((tot < 0.9) || (tot > 1.1) || (outlimit)) {
            return 0;
        }
        Double s;
        try {
            //s=1.0/pf.getExcessReturnsStdevCorr();
            s = 1.0 / pf.getExcessReturnsStdev();
        } catch (Exception e) {
            return 0;
        }

        if (s.isInfinite() || s.isNaN()) {
            return 0;
        }

        //double fit=Math.exp(-10*Math.abs(tot-1))*s;        
        //return fit>=0?1000*fit:0;
        return s;
    }
}

class Fitness_MinimumCorrelation extends FitnessFunction {

    static final long serialVersionUID = 0;
    private PortfolioOptimization pf;
    private int dim;
    private double minpc, maxpc;

    public Fitness_MinimumCorrelation(PortfolioOptimization pf) {
        this.pf = pf;
        dim = pf.getNoSecurities();
        this.minpc = pf.getMinpc();
        this.maxpc = pf.getMaxpc();

    }

    @Override
    public double evaluate(org.jgap.IChromosome a_subject) {
        double[] w = new double[dim];

        double tot;

        for (int i = 0; i < a_subject.size(); i++) {

            double a = (Double) a_subject.getGene(i).getAllele();
            w[i] = a;
        }
        tot = com.ettoremastrogiacomo.utils.DoubleArray.sum(w);
        boolean outlimit = false;
        for (int i = 0; i < a_subject.size(); i++) {
            w[i] /= tot;
            if ((w[i] < this.minpc) || (w[i] > this.maxpc)) {
                outlimit = true;
            }
        }

        tot = com.ettoremastrogiacomo.utils.DoubleArray.sum(w);
        try {
            pf.setWeights(w);
        } catch (Exception e) {
        }
        if ((tot < 0.9) || (tot > 1.1) || outlimit) {
            return 0;
        }
        Double s;
        try {
            s = 1.0 / pf.getExcessReturnsStdevCorr();
            //s=1.0/pf.getExcessReturnsStdev();
        } catch (Exception e) {
            return 0;
        }

        if (s.isInfinite() || s.isNaN()) {
            return 0;
        }

        //double fit=Math.exp(-10*Math.abs(tot-1))*s;        
        //return fit>=0?1000*fit:0;
        return s;
    }
}

class Fitness_Sharpe extends FitnessFunction {

    static final long serialVersionUID = 0;
    private PortfolioOptimization pf;
    private int dim;
    private double minpc, maxpc;

    public Fitness_Sharpe(PortfolioOptimization pf) {
        this.pf = pf;
        dim = pf.getNoSecurities();
        this.minpc = pf.getMinpc();
        this.maxpc = pf.getMaxpc();

    }

    @Override
    public double evaluate(org.jgap.IChromosome a_subject) {

        double[] w = new double[dim];
        double tot;
        for (int i = 0; i < a_subject.size(); i++) {
            double a = (Double) a_subject.getGene(i).getAllele();
            w[i] = a;

        }
        tot = com.ettoremastrogiacomo.utils.DoubleArray.sum(w);

        boolean outlimit = false;
        for (int i = 0; i < a_subject.size(); i++) {
            w[i] /= tot;
            if ((w[i] < this.minpc) || (w[i] > this.maxpc)) {
                outlimit = true;
            }
        }

        tot = com.ettoremastrogiacomo.utils.DoubleArray.sum(w);

        try {
            pf.setWeights(w);
        } catch (Exception e) {
        }
        if ((tot < 0.9) || (tot > 1.1) || outlimit) {
            return 0;
        }

        Double s = 0.0;
        try {
            s = pf.getExcessReturnsMean() / pf.getExcessReturnsStdev();
        } catch (Exception e) {
            return 0;
        }
        if (s.isInfinite() || s.isNaN() || s<0) {
            return 0;
        }
        return s;
    }
}
*/
public class PortfolioOptimizationOLD {

    private Fints securities;
    private double[][] covariance, correlation;
    private Fints excess_returns;
    private double[] mean;
    private double[] weights;
    static Logger logger = Logger.getLogger(PortfolioOptimizationOLD.class);
    private int population_size;
    private int generations;
    private double maxpc, minpc;
  //  private Configuration gaConf;
    private com.ettoremastrogiacomo.sktradingjava.Portfolio portfolio;
   // private final UDate dfrom, dto;

    private void start(com.ettoremastrogiacomo.sktradingjava.Portfolio portfolio) throws Exception {
        if (portfolio.getNoSecurities() < 1) {
            throw new Exception("empty security list");
        }
        this.portfolio = portfolio;
        Fints f = portfolio.getClose();//new Fints(dates,names,portfolio.getFrequency(),speedking.utils.DoubleDoubleArray.transpose( portfolio.close));
     //   this.securities = f.Sub(dfrom, dto);
       // this.buildMatrix();
        this.population_size = 3000;
        this.generations = 100;
    //    gaConf = new DefaultConfiguration();
        logger.debug("portfolio: \n"+this.securities);
        logger.debug("days gap: "+this.securities.getMaxDaysDateGap());
        logger.debug("first date: "+ this.securities.getFirstDate());
        logger.debug("last date: "+ this.securities.getLastDate());
        if (this.securities.getMaxDaysDateGap()>7) logger.warn("Days gap too big!");
        if (this.securities.getLength()<60) logger.warn("lenght too short: "+this.securities.getLength());
    }
/*
    private double[] getWeigths(Chromosome potentialSolution) {
        Gene[] g = potentialSolution.getGenes();
        double[] w = new double[g.length];
        for (int i = 0; i < g.length; i++) {
            w[i] = ((double) ((Double) g[i].getAllele()));
        }
        // 	w.set(i,0,((double)((Double)g[i].getAllele())));
        double tot = com.ettoremastrogiacomo.utils.DoubleArray.sum(w);
        for (int i = 0; i < g.length; i++) {
            w[i] /= tot;// w.set(i,0,w.getCopy(i,0)/tot);
        }
        return w;
    }

    public PortfolioOptimization(com.ettoremastrogiacomo.sktradingjava.Portfolio securities) throws Exception {
        dfrom = securities.getDate().get(0);
        dto = securities.getDate().get(securities.getLength() - 1);
        start(securities);

        setLimits(1, 0);
    }

    public PortfolioOptimization(com.ettoremastrogiacomo.sktradingjava.Portfolio securities, UDate from, UDate to) throws Exception {
        dfrom = from;
        dto = to;
        start(securities);

        setLimits(1, 0);
    }

    public void setGeneticParams(int population_size, int generations) throws Exception {
        if ((population_size < 100) || (population_size > 100000) || (generations < 1) || (generations > 100000)) {
            throw new Exception("illegal arguments. Ranges= population_size(100,100000),generations(1,100000),maxpercent(.1,1)");
        }
        this.population_size = population_size;
        this.generations = generations;

    }

    final public void setLimits(double maxpc, double minpc) throws Exception {
        int len = securities.getNoSeries();// .names.size();
        if (maxpc < (1.0 / len) || (maxpc > 1)) {
            throw new Exception("bad limits");
        }
        if ((len * minpc) > 1 || (minpc < 0)) {
            throw new Exception("bad limits");
        }
        this.maxpc = maxpc;
        this.minpc = minpc;
    }

    public double getMinpc() {
        return this.minpc;
    }

    public double getMaxpc() {
        return this.maxpc;
    }

    private void buildMatrix() throws Exception {
        this.excess_returns = Fints.ER(this.securities, 100, true);//this.securities;
        double[][] M = this.excess_returns.getMatrixCopy();
        this.covariance = com.ettoremastrogiacomo.utils.DoubleDoubleArray.cov(M);// MatlabSyntax.cov(M);
        this.correlation = com.ettoremastrogiacomo.utils.DoubleDoubleArray.corr(M);// MatlabSyntax.cov(M);
        this.mean = com.ettoremastrogiacomo.utils.DoubleDoubleArray.mean(M);//MatlabSyntax.mean(M);
        this.weights = new double[this.getNoSecurities()];//new Matrix(this.getNoSecurities(), 1,
        for (int i = 0; i < this.weights.length; i++) {
            this.weights[i]
                    = 1.0 / (double)this.getNoSecurities();
        }
    }

    public String getSecurityName(int i) {
        return this.portfolio.securities.get(i).getName();
    }

    public double[] getExcessReturnsMeanVector() {
        return this.mean.clone();
    }

    public double[][] getExcessReturnsCovarianceMatrix() {
        return com.ettoremastrogiacomo.utils.DoubleDoubleArray.copy(this.covariance);
    }

    public double[][] getExcessReturnsCorrelationMatrix() {
        return com.ettoremastrogiacomo.utils.DoubleDoubleArray.copy(this.correlation);
    }

    public double getExcessReturnsStdev() {
        double[] tmp = new double[this.weights.length];
        for (int i = 0; i < tmp.length; i++) {
            double s = 0;
            for (int j = 0; j < tmp.length; j++) {
                s += this.weights[j] * this.covariance[j][i];
            }
            tmp[i] = s;
        }
        double res = 0;
        for (int i = 0; i < tmp.length; i++) {
            res += tmp[i] * this.weights[i];
        }
        return Math.sqrt(res);
    }
    public double getExcessReturnsVariance() {
        double stdev=getExcessReturnsStdev();
        return (stdev*stdev);
    }
    

    public double getExcessReturnsStdevCorr() {
        double[] tmp = new double[this.weights.length];
        for (int i = 0; i < tmp.length; i++) {
            double s = 0;
            for (int j = 0; j < tmp.length; j++) {
                s += this.weights[j] * this.correlation[j][i];
            }
            tmp[i] = s;
        }
        double res = 0;
        for (int i = 0; i < tmp.length; i++) {
            res += tmp[i] * this.weights[i];
        }
        return Math.sqrt(res);

    }

    public double getExcessReturnsMean() {
        double res = 0;
        for (int i = 0; i < mean.length; i++) {
            res += mean[i] * weights[i];
        }
        return res;
    }

    public Fints getExcessReturns() {
        return this.excess_returns;
    }

    public int getNoSecurities() {
        return securities.getNoSeries();
    }

    public double[] getWeigths() {
        return this.weights.clone();
    }

    public void setWeights(double[] w) throws Exception {
        if ((w.length != securities.getNoSeries())) {
            throw new Exception("length must be " + securities.getNoSeries());
        }
        this.weights = w.clone();
    }

    public double[] Optimizer_MinimumVariance() throws Exception {
        //Configuration conf = new DefaultConfiguration();
        FitnessFunction myFunc
                = new Fitness_MinimumVariance(this);
        gaConf.setFitnessFunction(myFunc);
        Gene[] sampleGenes = new Gene[getNoSecurities()];

        for (int i = 0; i < sampleGenes.length; i++) {
            sampleGenes[i] = new org.jgap.impl.DoubleGene(this.gaConf, 0, 1);
        }

        Chromosome sampleChromosome = new Chromosome(this.gaConf, sampleGenes);
        gaConf.setSampleChromosome(sampleChromosome);
        gaConf.setPopulationSize(this.population_size);
        Genotype population = Genotype.randomInitialGenotype(gaConf);
        for (int i = 0; i < this.generations; i++) {
            //if ((i%10)==0) 
            logger.info("generation " + i + " of " + this.generations);
            population.evolve();

        }
        Chromosome bestSolutionSoFar = (Chromosome) population.getFittestChromosome();
        double[] newW = getWeigths(bestSolutionSoFar);
        return newW;
    }

    public double[] Optimizer_MinimumCorrelation() throws Exception {
        //Configuration conf = new DefaultConfiguration();
        FitnessFunction myFunc
                = new Fitness_MinimumCorrelation(this);
        gaConf.setFitnessFunction(myFunc);
        Gene[] sampleGenes = new Gene[getNoSecurities()];

        for (int i = 0; i < sampleGenes.length; i++) {
            sampleGenes[i] = new org.jgap.impl.DoubleGene(this.gaConf, 0, 1);
        }

        Chromosome sampleChromosome = new Chromosome(this.gaConf, sampleGenes);
        gaConf.setSampleChromosome(sampleChromosome);
        gaConf.setPopulationSize(this.population_size);
        Genotype population = Genotype.randomInitialGenotype(gaConf);
        for (int i = 0; i < this.generations; i++) {
            //if ((i%10)==0) 
            logger.info("generation " + i + " of " + this.generations);
            population.evolve();

        }
        Chromosome bestSolutionSoFar = (Chromosome) population.getFittestChromosome();
        double[] newW = getWeigths(bestSolutionSoFar);
        return newW;
    }

    public double[] Optimizer_Sharpe() throws Exception {
        //Configuration conf = new DefaultConfiguration();
        FitnessFunction myFunc
                = new Fitness_Sharpe(this);
        gaConf.setFitnessFunction(myFunc);
        Gene[] sampleGenes = new Gene[getNoSecurities()];

        for (int i = 0; i < sampleGenes.length; i++) {
            sampleGenes[i] = new org.jgap.impl.DoubleGene(this.gaConf, 0, 1);
        }

        Chromosome sampleChromosome = new Chromosome(this.gaConf, sampleGenes);
        gaConf.setSampleChromosome(sampleChromosome);
        gaConf.setPopulationSize(this.population_size);
        Genotype population = Genotype.randomInitialGenotype(gaConf);
        for (int i = 0; i < this.generations; i++) {
            population.evolve();
        }
        Chromosome bestSolutionSoFar = (Chromosome) population.getFittestChromosome();
        double[] newW = getWeigths(bestSolutionSoFar);
        return newW;
    }

    @Override
    public String toString() {
        int len = this.getNoSecurities();
        StringBuffer b = new StringBuffer();
        b.append("\n");

        for (int i = 0; i < len; i++) {
            try {
                b.append("\n" + (String) securities.getName(i) + " = "
                        + this.weights[i]);
                b.append(" ; mean=" + this.mean[i] + "  stdev=" + Math.sqrt(covariance[i][i]));
            } catch (Exception e) {
            }//dummy
        }
        double m = this.getExcessReturnsMean(), s = this.getExcessReturnsStdev();
        b.append("\nData length=" + this.excess_returns.getLength());
        b.append("\nPortfolio mean=" + m);
        b.append("\nPortfolio stdev=" + s);
        b.append("\nPortfolio sharpe=" + m / s);
        b.append("\nPortfolio weights:\n" + com.ettoremastrogiacomo.utils.DoubleArray.toString(this.weights));
        try {
            b.append("\nPortfolio covariance:\n" + com.ettoremastrogiacomo.utils.DoubleDoubleArray.toString(covariance));
        } catch (Exception e) {
            logger.warn(e);
        }

        return b.toString();
    }*/
}
