package com.ettoremastrogiacomo.sktradingjava.system;


import com.ettoremastrogiacomo.sktradingjava.Portfolio;
import com.ettoremastrogiacomo.utils.UDate;
import com.ettoremastrogiacomo.sktradingjava.backtesting.Orders;
import java.util.List;

public interface TradingSystem {
      
    public Orders apply(UDate from,UDate to, Object params) throws Exception;
    public int[] getParamsBoundary();//ogni elemento rappresenta il num di valori che il parametro i-esimo può avere
    // per valori reali si fa riferimento ad una quantizzazione dell'intervallo
    public Portfolio getPortfolio();
    public String getInfo();
    public java.util.HashMap<String,Double> getRealParams(Object params) throws Exception;
    public List<UDate> getDates();
}
