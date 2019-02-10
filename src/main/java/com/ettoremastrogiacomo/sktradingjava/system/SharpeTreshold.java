/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.system;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Orders;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Collections;
import java.util.List;
import org.apache.log4j.Logger;


/**
 *
 * @author a241448
 */
public class SharpeTreshold implements TradingSystem{
	static Logger logger = Logger.getLogger(SharpeTreshold.class);

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
    public SharpeTreshold(Portfolio ptf)throws Exception {
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
    }

    /*@Override
public org.jgap.IChromosome getParams(){
    return (org.jgap.IChromosome)this.sampleChromosome.clone();
}*/
    @Override
public java.util.HashMap<String,Double> getRealParams(Object params) throws Exception{
        if (params instanceof int[]){
                int[] temp=(int[]) params;
                java.util.HashMap<String,Double> map=new java.util.HashMap<>();
                map.put("param1:"+ind.getName(temp[0]), new Double(MIN_PERIOD+temp[0]));
                return map;        
        }
        else throw new Exception("bad params object : "+params.getClass().getName()); //To change body of generated methods, choose Tools | Templates.
}    


    @Override
    public String getInfo(){return "SharpeTreshold CROSSOVER";}

    @Override
    public com.ettoremastrogiacomo.sktradingjava.backtesting.Orders apply(UDate from,UDate to,Object params) throws Exception {
        int p1;
        if (params instanceof int[]) {
            if (((int[]) params).length!=1) throw new Exception("param len must be 1 not "+((int[]) params).length);
            int[] temp=(int[]) params;
            p1=temp[0];   
        } else throw new Exception("cannot cast object "+params.getClass().getCanonicalName());        
        Orders orders=new  Orders(ptf);
        //if (param1==param2) return orders;
        int kstart=dates.indexOf(from),kend=dates.indexOf(to);
        if (kstart<0 || kend<0 || kstart>=kend) throw new Exception("wrong date indexes");
        //Order order=null;
        boolean flat=true;
        for (int i=kstart;i<kend;i++) {
            if (mind[i][p1]>0 && flat) {
                //order=new Order();
                //order.date=dates.get(i+1);
                //order.otype=Order.type.MARKET;
                //order.weights=this.wbuy;
                //orders.add(order);
                orders.add(i+1, this.wbuy, Orders.type.MARKET);
                flat=false;
            } else
            if (mind[i][p1]<0 && !flat) {
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



    @Override
    public int[] getParamsBoundary()  {
        int[] b=new int[]
            {this.ind.getNoSeries()};
        return b;        
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }



}
