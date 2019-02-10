/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.backtesting;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Orders.Order;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.log4j.Logger;

/**
 *
 * @author sk
 */
public class Backtest {
    static final Logger LOG = Logger.getLogger(Backtest.class);
    private final Portfolio portfolio;
    private final double initCapital,trade_penalty_abs,trade_penalty_pc;    
    private final double[][] open,close;
    private final int noSecurities;
    public Backtest(Portfolio portfolio,Optional<Double> initCapital,Optional<Double> penalty_pc,Optional<Double> penalty_abs) {
        this.portfolio=portfolio;
        this.initCapital=initCapital.orElse(1000.0);
        this.trade_penalty_pc=penalty_pc.orElse(0.0);
        this.trade_penalty_abs=penalty_abs.orElse(0.0);                
        this.open=portfolio.getOpen().getMatrixCopy();
        this.close=portfolio.getClose().getMatrixCopy();
        this.noSecurities=portfolio.getNoSecurities();
    }    
    public Statistics apply( Orders orders,UDate dfrom, UDate dto) throws Exception{
        //List<Double> current_weights =Collections.nCopies(this.portfolio.getNoSecurities(), Double.valueOf(0));
        //check orders
        double [] current_weights=new double[this.portfolio.getNoSecurities()];
        int kstart=portfolio.getOffset(dfrom);        
        int kend=portfolio.getOffset(dto);
        if (kstart <0 || kend <0 || kend<=kstart || kend >= portfolio.getLength()) throw new Exception("bad ranges");
        int btlength=kend-kstart+1;
        double [][] equityval=new double[btlength][2];
        java.util.ArrayList<UDate> equitydate=new java.util.ArrayList<>();
        equitydate.addAll(portfolio.dates.subList(kstart, kend+1));
        java.util.ArrayList<Double> netgains=new java.util.ArrayList<>();
        int dateidx=kstart;         
        
        double [] zerow=new double[this.portfolio.getNoSecurities()];
             for (int i=0;i<orders.orders.size();i++){
                 
                Order current =  orders.orders.get(i);
        	Order previous=i<1?null:orders.orders.get(i-1),next=i==(orders.orders.size()-1)?null:orders.orders.get(i+1);
        	//int prevoff=portfolio.offset(previous.date),curroff=portfolio.offset(current.date),nextoff=portfolio.offset(next.date);
        	int nextoff=next!=null?next.dateidx /*portfolio.getOffset(next.date)*/:kend+1;
        	double[] preweigths=previous==null?zerow:previous.weigths,currweigths=current.weigths;
        	//if (current.tp ==Orders.type. .type.LIMIT) throw new Exception("LIMIT order non implemented yet");
                //sono implementati solo ordini a mercato in apertura
        	double pe=dateidx>kstart? equityval[dateidx-kstart-1][0]:this.initCapital;
        	if (dateidx>kend) throw new Exception("dateidx="+dateidx+" ; "+next+" ; "+portfolio.getDate(current.dateidx) +" ; "+dto+" ; "+this.portfolio.getLength());
        	pe=pe*(1-this.trade_penalty_pc)-currweigths.length*this.trade_penalty_abs;
        	//calcolo delta prev
        	double asum=com.ettoremastrogiacomo.utils.DoubleArray.sum_abs(preweigths);
        	double res=pe*(1-asum),gap=0;
        	if (dateidx>kstart)
        		for (int j=0;j<this.noSecurities;j++)
        			gap+=preweigths[j]*(open[dateidx][j]  - close[dateidx-1][j] )/close[dateidx-1][j];
        	pe=(1+gap)*(pe-res)+res;
        	asum=(current.tp==Orders.type.MARKET)?com.ettoremastrogiacomo.utils.DoubleArray.sum_abs(currweigths):com.ettoremastrogiacomo.utils.DoubleArray.sum_abs(preweigths);        		        	
        	res=pe*(1-asum);gap=0;
        	for (int j=0;j<this.noSecurities;j++)
                    gap+= ((current.tp==Orders.type.MARKET)? currweigths[j]:preweigths[j])*(close[dateidx][j]-open[dateidx][j])/open[dateidx][j];
        	pe=(1+gap)*(pe-res)+res;
        	equityval[dateidx-kstart][0]=pe;
        	//logger.debug(portfolio.dates[dateidx]+"\t"+equityval[dateidx-kstart][0]);
        	dateidx++;
        	if (dateidx>kend && current!=orders.orders.get(orders.orders.size()-1)) throw new Exception("Ops "+portfolio.getDate(current.dateidx) +"   "+portfolio.getDate(previous.dateidx)+" "+portfolio.getDate(next.dateidx));
        	asum=com.ettoremastrogiacomo.utils.DoubleArray.sum_abs(currweigths);
        	if (asum>1.01) throw new Exception("weigths abs sum must be <= 1 :"+asum);//1% tolerance
        	while (dateidx<nextoff){
        		res=pe*(1-asum);gap=0;
        		for (int j=0;j<this.noSecurities;j++)
                            gap+=currweigths[j]*(close[dateidx][j]-close[dateidx-1][j])/close[dateidx-1][j];
        		pe=(1+gap)*(pe-res)+res;
        		equityval[dateidx-kstart][0]=pe;
        		//logger.debug(portfolio.dates[dateidx]+"\t"+equityval[dateidx-kstart][0]);
        		dateidx++;
        	}
        }
        for (int i=dateidx;i<=kend;i++) 
            equityval[i-kstart][0]=i==kstart?this.initCapital:equityval[i-kstart-1][0];

        //calcolo statistiche
        Statistics stats = new Statistics();
        java.util.Iterator<Double>	it=netgains.iterator();
        //stats.profit_percent_sum=stats.stdev=0;
        while (it.hasNext()) stats.profit_percent_sum+=it.next();
        //stats.winning_trades=nwins;
        stats.bars=btlength;//portfolio.dates.size();
        //stats.losing_trades=nlosses;
        stats.trades=orders.orders.size();//nwins+nlosses;
        //maxDD calculus
        double mdd=0,mddp=0,maxmdd=this.initCapital;
        for (int i=1;i<equityval.length;i++) {
            if (equityval[i][0]>=equityval[i-1][0]) {
                if (equityval[i][0]>maxmdd) maxmdd=equityval[i][0];
                continue;
            }
            double t1=equityval[i][0]-maxmdd;
            double t1p=t1/maxmdd;
            if (t1<mdd) mdd=t1;
            if (t1p<mddp) mddp=t1p;
        }//
        stats.maxdrawdown_percent=mddp;stats.maxdrawdown_points=mdd;
        //regression slope 
        double meanx=0,meany=0,varx=0,vary=0,covxy=0;
        for (int i=0;i<equityval.length;i++){meany+=equityval[i][0];meanx+=i;}
        meany/=(double)portfolio.dates.size();meanx/=(double)equityval.length;
        for (int i=0;i<equityval.length;i++)  {varx+=Math.pow(i-meanx,2);vary+=Math.pow(equityval[i][0]-meany,2);}
        vary/=(double)(equityval.length-1);varx/=(double)(equityval.length-1);
        for (int i=0;i<equityval.length;i++) covxy+=(i-meanx)*(equityval[i][0]-meany);covxy/=(double)(equityval.length-1);
        double reg_b=covxy/varx;
        double reg_a=meany-reg_b*meanx;
        for (int i=0;i<equityval.length;i++) equityval[i][1]=reg_a+reg_b*i;
        java.util.ArrayList<String> eqname=new java.util.ArrayList<>();
        eqname.add("equity");eqname.add("linreg");
        stats.linregslope=reg_b;
        double stdeverr=0;
        for (double[] equityval1 : equityval) {        
            stdeverr += Math.pow(equityval1[0] - equityval1[1], 2);
        }
        stdeverr=Math.sqrt((1.0/(portfolio.dates.size()-1))*stdeverr);
        stats.linregstderr=stdeverr;
        stats.linregsharpe=stats.linregslope/stats.linregstderr;
        stats.equity=new Fints(equitydate,eqname,this.portfolio.getFrequency(),equityval);
        return stats;

    }
}
