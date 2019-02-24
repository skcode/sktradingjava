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
import com.ettoremastrogiacomo.utils.UDate;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Orders;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import org.apache.log4j.Logger;


public final class  BUYANDHOLD implements TradingSystem{
    static Logger logger = Logger.getLogger(BUYANDHOLD.class);
    final List<UDate> dates;
    final int PARTS=20;//divido [0,1] in 20 parti
    //int param1,param2;
    //double [] wbuy={1},wsell={0},wshort={-1},wcover={0};
    final Portfolio ptf;
    @Override
    public Portfolio getPortfolio() {return ptf;}
    /** Creates a new instance 
     * @param ptf
     * @throws java.lang.Exception */
    public BUYANDHOLD(Portfolio ptf)throws Exception {
        logger.debug("generating configuration for "+this.getClass().getName());
        logger.debug("loading portfolio "+ptf.toString());
        if (ptf.getNoSecurities()<1) throw new Exception("TS applicable to >=1 securities");        
        this.ptf=ptf;
        dates=Collections.unmodifiableList(ptf.getDate()) ;
    }
    @Override public List<UDate> getDates() {return Collections.unmodifiableList(dates);}
    /*@Override
public org.jgap.IChromosome getParams(){
    return (org.jgap.IChromosome)this.sampleChromosome.clone();
}*/
    @Override
public java.util.HashMap<String,Double> getRealParams(Object params) throws Exception {
                java.util.HashMap<String,Double> map=new java.util.HashMap<>();
    if (params instanceof int[]){
                int[] temp=(int[]) params;
                double sum=0;
                for (int i=0;i<temp.length;i++) sum+=temp[i];
                for (int i=0;i<this.ptf.securities.size();i++){
                    map.put(this.ptf.securities.get(i).getHashcode(), (double)temp[i]/sum);
                }
                return map;        
        } else if (params instanceof double[]){
            double [] temp=(double[]) params;
            double sum=com.ettoremastrogiacomo.utils.DoubleArray.sum(temp);
            for (int i=0;i<this.ptf.securities.size();i++){
                    map.put(this.ptf.securities.get(i).getHashcode(), temp[i]/sum);
                }
            return map;
        }
        else throw new Exception("bad params object : "+params.getClass().getName());
}
    @Override
    public String getInfo(){return "BUY AND HOLD";}

    @Override
    public Orders apply(UDate from,UDate to,Object params) throws Exception {
        double []p=new double[this.ptf.getNoSecurities()];
        java.util.HashMap<String,Double> pmap=this.getRealParams(params);
        for (int i=0;i<p.length;i++) p[i]=pmap.get(this.ptf.securities.get(i).getHashcode());
        Orders orders=new Orders(ptf);        
        int kstart=dates.indexOf(from),kend=dates.indexOf(to);
        if (kstart<0 || kend<0 || kstart>=kend) throw new Exception("wrong date indexes");
        orders.add(kstart,p,Orders.type.MARKET); 
        double [] closew=new double[ptf.getNoSecurities()];//all 0
        for (int i=0;i<ptf.getNoSecurities();i++) closew[i]=0;
        orders.add(kend,closew,Orders.type.MARKET);
        return orders;
    }

    @Override
    public int[] getParamsBoundary() {
        int[] r= new int[this.ptf.getNoSecurities()];
        for (int i=0;i<r.length;i++){r[i]=PARTS;}
        return r;
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}

