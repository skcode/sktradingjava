package com.ettoremastrogiacomo.sktradingjava.backtesting;

import com.ettoremastrogiacomo.sktradingjava.Fints;

public class Statistics {
    public double profit_percent_sum;
    public int trades;
    public int bars;
    //public int winning_trades;
    //public int losing_trades;
    public double maxdrawdown_percent;
    public double maxdrawdown_points;    
   // public double mean;
   // public double stdev;
    public Fints equity;
    public double linregslope;
    public double linregstderr;
    public double linregsharpe;    
    @Override
    public String toString() {
        String s="\n***** STATISTICS *****";
        s+="\nprofit percents sum (pps)="+profit_percent_sum;
      //  s+="\npps mean="+mean;
        //s+="\npps stdev="+stdev;
        s+="\ntrades="+trades;
       // s+="\nwinning trades="+winning_trades;
        //s+="\nlosing trades="+losing_trades;
        s+="\nbars="+bars;
        s+="\nfirst date="+equity.getDate(0);
        s+="\nlast date="+equity.getDate(equity.getLength()-1);
        s+="\ninitial equity="+equity.get(0, 0);
        s+="\nfinal equity="+equity.get(equity.getLength()-1,0);        
        s+="\nmaxdrawdown percent="+maxdrawdown_percent;
        s+="\nmaxdrawdown points="+maxdrawdown_points;
        s+="\nequity linear regression slope="+linregslope;
        s+="\nequity linear regression error standard deviation="+linregstderr;
        s+="\nequity linear regression ratio (slope/errstdev)="+linregsharpe;
        
        return s;
    }
}
