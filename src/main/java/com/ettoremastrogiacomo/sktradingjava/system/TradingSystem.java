package com.ettoremastrogiacomo.sktradingjava.system;


import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.utils.UDate;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Orders;
import java.util.List;

public interface TradingSystem {
      
    public Orders apply(UDate from,UDate to, Object params) throws Exception;
    public int[] getParamsBoundary();//min,max,step
    public Portfolio getPortfolio();
    public String getInfo();
    public java.util.HashMap<String,Double> getRealParams(Object params) throws Exception;
    public List<UDate> getDates();
}
