/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ettoremastrogiacomo.sktradingjava.system;

/**
 *
 * @author ettore
 */
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Orders;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


import org.apache.log4j.Logger;



public final class SHARPE_CROSSOVER implements TradingSystem{
	static Logger logger = Logger.getLogger(SHARPE_CROSSOVER.class);
    static final int MIN_PERIOD=5;
    static final int MAX_PERIOD=300;
    final Fints f,ind;
    final double[][]mind;
    final java.util.List<UDate> dates;
    double [] wbuy={1},wsell={0},wshort={-1},wcover={0};
    final Portfolio ptf;
        @Override public List<UDate> getDates() {return Collections.unmodifiableList(dates);}
    @Override
    public Portfolio getPortfolio() {return ptf;}
    /** Creates a new instance of TS_macrossover
     * @param ptf
     * @throws java.lang.Exception */
    public SHARPE_CROSSOVER(Portfolio ptf)throws Exception {
        if (ptf.getNoSecurities()!=1) throw new Exception("TS applicable to 1 security");
        Fints all;
        this.ptf=ptf;
        Fints tf=ptf.getClose();
        Fints tind=null;
        Fints ER=Fints.ER(tf, 1, false);
        Fints SHARPE=Fints.Sharpe(ER, 20);
        for (int i=MIN_PERIOD;i<=MAX_PERIOD;i++)
        	if (i==MIN_PERIOD) tind=Fints.SMA(SHARPE, i);
        	else tind=Fints.merge(tind, Fints.SMA(SHARPE, i));
        all=Fints.merge(tf, tind);
        dates=Collections.unmodifiableList(all.getDate());
        f=all.getSerieCopy(0);ind=all.Sub(0, all.getLength()-1, 1, all.getNoSeries()-1);
        mind=ind.getMatrixCopy();
        /*Configuration.reset();
        gaConf = new DefaultConfiguration();
        gaConf.setPopulationSize(100);
        Gene[] sampleGenes= new Gene[2];

        sampleGenes[0]=new org.jgap.impl.IntegerGene(gaConf,0,ind.getNoSeries()-1);
        sampleGenes[1]=new org.jgap.impl.IntegerGene(gaConf,0,ind.getNoSeries()-1);
        sampleGenes[0].setAllele(0);
        sampleGenes[1].setAllele(1);
        sampleChromosome = new Chromosome(this.gaConf,sampleGenes);
        gaConf.setSampleChromosome(sampleChromosome);
        //setParams(sampleChromosome);*/
    }
    /*
    public boolean checkParams(org.jgap.IChromosome chromosome) {
        if (chromosome.size()!=2)
            return false;
        Object o1=chromosome.getGene(0).getAllele(),o2=chromosome.getGene(1).getAllele();
        if (o1 instanceof java.lang.Integer){
            this.param1=((Integer)o1);
        } else return false;
        if (this.param1<0 || this.param1>=ind.getNoSeries())
            return false;
        if (o2 instanceof java.lang.Integer){
            this.param2=((Integer)o2);
        } else return false;
        return !(this.param2<0 || this.param2>=ind.getNoSeries());
    }
    public java.util.ArrayList<org.jgap.IChromosome> getNearest(org.jgap.IChromosome chromosome,int distance) throws Exception{
        java.util.ArrayList<org.jgap.IChromosome> chrlist=new java.util.ArrayList<>();
        if (!checkParams(chromosome)) throw new Exception("bad chromosome");
        Object o1=chromosome.getGene(0).getAllele(),o2=chromosome.getGene(1).getAllele();
        int p1=((Integer)o1);
        int p2=((Integer)o2);

        int minv=0,maxv=ind.getNoSeries()-1;
        for (int i=minv;i<maxv;i++)
            for (int j=minv;j<maxv;j++)
        {
            int d1=Math.abs(i-p1),d2=Math.abs(j-p2);
            int s=d1+d2;
            if (s==distance) {
                Gene[] sampleGenes= new Gene[2];
                sampleGenes[0]=new org.jgap.impl.IntegerGene(gaConf,0,maxv);
                sampleGenes[1]=new org.jgap.impl.IntegerGene(gaConf,0,maxv);
                sampleGenes[0].setAllele(i);
                sampleGenes[1].setAllele(j);
                sampleChromosome = new Chromosome(this.gaConf,sampleGenes);
                chrlist.add(sampleChromosome);
            }
        }
        return chrlist;
    }*/
    /*@Override
    public void setParams(org.jgap.IChromosome chromosome)throws Exception{
        if (chromosome.size()!=2)
            throw new Exception("system accept 2 parameters only");
        Object o1=chromosome.getGene(0).getAllele(),o2=chromosome.getGene(1).getAllele();
        if (o1 instanceof java.lang.Integer){
            this.param1=((Integer)o1);
        } else throw new Exception("incompatible type " +o1.getClass().getName());
        if (this.param1<0 || this.param1>=ind.getNoSeries())
            throw new Exception("param1 out of range:"+param1);
        if (o2 instanceof java.lang.Integer){
            this.param2=((Integer)o2);
        } else throw new Exception("incompatible type " +o2.getClass().getName());
        if (this.param2<0 || this.param2>=ind.getNoSeries())
            throw new Exception("param2 out of range:"+param2);
        this.sampleChromosome=(Chromosome) chromosome;
    }*/

    /*@Override
public org.jgap.IChromosome getParams(){
    return (org.jgap.IChromosome)this.sampleChromosome.clone();
}*/
    /*@Override
public java.util.HashMap<String,Double> getRealParams() {
    java.util.HashMap<String,Double> map=new java.util.HashMap<>();
    map.put("param1:"+ind.getName(param1), new Double(MIN_PERIOD+param1));
    map.put("param2:"+ind.getName(param2), new Double(MIN_PERIOD+param2));
    return map;
}
    @Override
    public org.jgap.Configuration getGaConf(){return this.gaConf;}
*/
    @Override
    public String getInfo(){return "SHARPE CROSSOVER";}

  /*  @Override
    public com.ettoremastrogiacomo.sktradingjava.backtesting.Orders apply(UDate from,UDate to) throws Exception {
        Orders orders=new  Orders(ptf);
        if (param1==param2) return orders;
        int kstart=dates.indexOf(from),kend=dates.indexOf(to);
        if (kstart<0 || kend<0 || kstart>=kend) throw new Exception("wrong date indexes: "+kstart +"\t"+kend);
        //Order order=null;
        boolean flat=true;
        for (int i=kstart;i<kend;i++) {
            if (mind[i][this.param1]>mind[i][this.param2] && flat) {
                //order=new Order();
                //order.date=dates.get(i+1);
                //order.otype=Order.type.MARKET;
                //order.weights=this.wbuy;
                //orders.add(order);
                orders.add(i+1, this.wbuy, Orders.type.MARKET);
                flat=false;
            } else
            if (mind[i][this.param1]<mind[i][this.param2] && !flat) {
                    //order=new Order();
                    //order.date=dates.get(i+1);
                    //order.otype=Order.type.MARKET;
                    //order.weights=this.wsell;
                    //orders.add(order);
                    orders.add(i+1, this.wsell, Orders.type.MARKET);
                    flat=true;
            }
        }
        return orders;
    }

*/
    @Override
    public int[] getParamsBoundary() {
        return new int[]
            {ind.getNoSeries()-1,ind.getNoSeries()-1}
        ;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Orders apply(UDate from, UDate to, Object params) throws Exception {
        int p1,p2;
        if (params instanceof int[]) {
            if (((int[]) params).length!=2) throw new Exception("param len must be 2 not "+((int[]) params).length);
            int[] temp=(int[]) params;
            p1=temp[0];p2=temp[1];            
        } else throw new Exception("cannot cast object "+params.getClass().getCanonicalName());
        
        Orders orders=new  Orders(ptf);
        if (p1==p2) return orders;
        int kstart=dates.indexOf(from),kend=dates.indexOf(to);
        if (kstart<0 || kend<0 || kstart>=kend) throw new Exception("wrong date indexes: "+kstart +"\t"+kend);
        //Order order=null;
        boolean flat=true;
        for (int i=kstart;i<kend;i++) {
            if (mind[i][p1]>mind[i][p2] && flat) {
                orders.add(i+1, this.wbuy, Orders.type.MARKET);
                flat=false;
            } else
            if (mind[i][p1]<mind[i][p2] && !flat) {
                    orders.add(i+1, this.wsell, Orders.type.MARKET);
                    flat=true;
            }
        }
        return orders;

    }

    @Override
    public HashMap<String, Double> getRealParams(Object params) throws Exception{
        if (params instanceof int[]){
                int[] temp=(int[]) params;
                java.util.HashMap<String,Double> map=new java.util.HashMap<>();
                map.put("param1:"+ind.getName(temp[0]), new Double(MIN_PERIOD+temp[0]));
                map.put("param2:"+ind.getName(temp[1]), new Double(MIN_PERIOD+temp[1]));
                return map;        
        }
        else throw new Exception("bad params object : "+params.getClass().getName()); //To change body of generated methods, choose Tools | Templates.
    }
}


