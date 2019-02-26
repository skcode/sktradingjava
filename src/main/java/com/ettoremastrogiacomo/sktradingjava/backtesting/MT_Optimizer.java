/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.backtesting;

import com.ettoremastrogiacomo.sktradingjava.system.TradingSystem;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Optional;
import org.apache.log4j.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;



/**
 *
 * @author a241448
 */
public class MT_Optimizer {
    final int[][] searchSpace;    
    final double initCapital,penalty_pc,penalty_abs;
    final TradingSystem ts;
    final int epochs;//switch from brute force to random
    final boolean bruteOrRand;
    final UDate from,to;
    final com.ettoremastrogiacomo.sktradingjava.backtesting.Backtest  bt;
    final int[] paramsBoundary;
    int [][] allpar;
    final long permutationsNum;
    final int TRESHOLD=500000;
    final java.util.ArrayList<int[]> permutations=new java.util.ArrayList<>();
    fitobjective fitobj;
    static public  enum fitobjective {NETPROFIT,SHARPE,MINDRAWDOWN};
    static Logger logger = Logger.getLogger(MT_Optimizer.class);
    
        

    //javafx.util.Pair<Integer,Integer> p;
    public MT_Optimizer(TradingSystem ts,UDate from, UDate to,Optional<Integer> epochs,Optional<Double> initCapital,Optional<Double> penalty_pc,Optional<Double> penalty_abs) throws Exception{
        this.searchSpace = null;
        this.initCapital=initCapital.orElse(1000.0);this.penalty_pc=penalty_pc.orElse(0.);this.penalty_abs=penalty_abs.orElse(0.);
        this.ts=ts;
        this.from=from;this.to=to;
        this.epochs=epochs.orElse(TRESHOLD);        
        bt=new Backtest(ts.getPortfolio(),initCapital,penalty_pc,penalty_abs);
        this.paramsBoundary=ts.getParamsBoundary();
        if (this.paramsBoundary.length==0) throw new Exception("no parameters assigned");
        //for (int i=0;i<this.paramsBoundary.length;i++) if (this.paramsBoundary[i].length!=2) throw new Exception("wrong low,high for param "+i);
        this.fitobj=fitobjective.NETPROFIT;
        
        this.allpar=new int[this.paramsBoundary.length][];
        long complexity=1;boolean bor=true;//this.bruteOrRand=true;
        for (int i=0;i<this.paramsBoundary.length;i++){
            int d=(this.paramsBoundary[i])  +1;
            this.allpar[i]=new int[d];
            
            try {
            complexity=Math.multiplyExact(complexity, allpar[i].length);
            if (complexity>this.epochs) bor=false;
            }
            catch (Exception e) {
                bor=false;
                complexity=Long.MAX_VALUE;
            }
            for (int j=0;j<=this.paramsBoundary[i];j++){
                allpar[i][j]=j;
            }                
        }
        this.bruteOrRand=bor;
        this.permutationsNum=complexity;
        if (this.bruteOrRand){
        int[] outputCol = new int[allpar.length];
        recursiveHelper(0, allpar.length, allpar[0].length, allpar, outputCol);
        }//set permutations       
        logger.debug("paramset length "+this.paramsBoundary.length+"\tcomplexity:"+(complexity==Long.MAX_VALUE?"infinite":complexity));
        logger.debug("algo "+(this.bruteOrRand ? "bruteForce": "Random"));
        logger.debug("epochs "+(this.bruteOrRand ?this.permutationsNum:this.epochs));
    }
    
    public com.ettoremastrogiacomo.sktradingjava.backtesting.Statistics getStats(int[] params) throws Exception{
    	logger.debug("get stats with params "+vec2string(params)+" \t"+evaluate(params));
        
        //this.ts.setParams(params);
        com.ettoremastrogiacomo.sktradingjava.backtesting.Statistics stats= bt.apply(ts.apply(from, to,params), from, to);// .backtest(ts.apply(from,to));
    	return stats;
    }    
    public void setFitnessObjective(fitobjective fitobj) {
        this.fitobj=fitobj;
    }   

    

    final void recursiveHelper(int j, int row, int col, int[][] first, int[] outputCol) {    
    for (int i = 0; i < first[j].length; i++) {
        outputCol[j] = first[j][i];
        // recursively continue to populate outputRow until we reach the last column (j == col -1)
        if (j < row - 1) {
            recursiveHelper(j + 1, row, first[j].length, first, outputCol);               
        }
        // we have reached the last column (j == col -1) so now we could print current permutation
        if (j == row - 1) {            
            permutations.add(outputCol.clone());
        }
    }
}    


    public int[] run(java.util.HashMap<int[],Double> map) throws Exception{
        int [] best=null;
        double b=0;
        if (map==null) throw new Exception("map is null");
        map.clear();
        //map=new java.util.HashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());//.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        java.util.HashMap<int[],Future> fmap=new java.util.HashMap<>();
        //int psize=permutations.size();
        logger.debug("running threads...please wait");
        if (this.bruteOrRand){
            for (int[] x : permutations ){
                Callable <Double> callobg= ()->{return evaluate(x);};
                Future<Double> future=pool.submit(callobg);
                fmap.put( x,future);
            }
        } else {
            java.util.Random r=new java.util.Random(System.currentTimeMillis());
            int itMAX=this.epochs;
            int i=0;            
            while (i<itMAX) {
                int [] v= new int[this.paramsBoundary.length];
                for (int j=0;j<this.allpar.length;j++) v[j]=this.allpar[j][ r.nextInt(this.allpar[j].length)];
                if (fmap.containsKey(v)) continue;
                Callable <Double> callobg= ()->{return evaluate(v);};
                Future<Double> future=pool.submit(callobg);
                fmap.put( v,future);
                i++;
            }        
        }
        logger.debug("getting results...");
        
        for (int[] f : fmap.keySet()){            
            double newb= (double) fmap.get(f).get();
            map.put(f, newb);
            if (newb>b) {
                logger.debug("new best : "+newb+" ; "+vec2string(f));
                b=newb;
                best=f;
            }                    
        }
        //logger.debug("BEST=" +b);
        //for (int i=0;i<best.length;i++) logger.debug(best[i]);
        return best;
    }


    double evaluate(int[] params) {        
        try {                        
            //ts.setParams(params);            
            com.ettoremastrogiacomo.sktradingjava.backtesting.Statistics stats= bt.apply(ts.apply(from,to,params),from,to);
            //stats.linregsharpe
            double fineq=stats.equity.get(stats.equity.getLength()-1,0);//double tv=stats.equity.get;
            if (null==this.fitobj) return 0; else switch (this.fitobj) {
                case NETPROFIT:
                    return fineq;//if (fineq>0) return fineq;else return 0;
            //if (stats.linregsharpe>0) return stats.linregsharpe; else return 0;
                case SHARPE:
                    return stats.linregsharpe;//return Math.exp(stats.linregsharpe);//evaluate must return non negative value
                case MINDRAWDOWN:
                    return (fineq*Math.abs(stats.maxdrawdown_percent));
                default:
                    return 0;
            }
            
        }catch (Exception ex){
            logger.error("error evaluating fitness",ex);
        }
        return 0;
    }    
    public static double distance(int[] a,int[] b)throws Exception {
        double d=0;
        if (a.length!=b.length) throw new Exception("length mismatch "+a.length+"\t"+b.length);
        for (int i=0;i<a.length;i++) d+=Math.pow(a[i]-b[i],2);
        return Math.sqrt(d);
    }
    public static String vec2string(int[] v) {
        if (v==null) return "[]";
        if (v.length==0) return "[]";
        String s="[";
        for (int i=0;i<v.length;i++) if (i==0) s+=v[i];else s+=","+v[i];
        return (s+"]");
        
    }
}
