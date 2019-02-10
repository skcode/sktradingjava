package com.ettoremastrogiacomo.sktradingjava.starters;

/*
 * charts_main.java
 *
 * Created on 23 gennaio 2007, 17.56
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */


import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.Charts;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import java.util.Optional;
import org.apache.log4j.*;

//import speedking.trading.data.DBExplorer;

import org.jfree.chart.plot.*;

/**
 *
 * @author speedking
 */
public class Charts_main {
    
    /** Creates a new instance of charts_main */
    public Charts_main() {
    }

    public static void main(String[] args) throws Exception{
		//PropertyConfigurator.configure("src/log4j.properties");
                //String [] markets_by_type={"EEM","SPY","QQQQ","IEV","SDS","IYR","TLT","GLD","EWJ"};
                String [] markets_by_type={"X25E","IBTM","XEMB","IUSA","EQQQ","EUE","XSFR","IJPN","XMEM","EXXY","IPRP"};
               
        

        Fints f=Database.getFilteredPortfolioOfClose(Optional.of(300), Optional.of("STOCK"), Optional.of("MLSE"), Optional.empty(), Optional.empty(), Optional.of(50),Optional.empty(), Optional.empty());
        Charts c1=new Charts("MOVING_SHARPE");
        Fints f2=Fints.Diff(f);
        XYPlot p1=c1.createXYPlot("er",f);
        c1.plot(p1,640,480);
        java.util.TreeMap<Double,String> tmap=new java.util.TreeMap<Double,String>();
        for (int i=0;i<f.getNoSeries();i++) {
            double r1=f.get(f.getLength()-1, i);
            double r2=f2.get(f2.getLength()-1, i);
            double val=Math.exp(r1+10*r2)-1;
            tmap.put(val,  f.getName(i));
            System.out.println(f.getName(i)+" : "+r1+"\t"+r2+"\t"+(val));
        }
        java.util.Iterator<Double> itd=tmap.keySet().iterator();
        
        
        while (itd.hasNext()) {
            Double d=itd.next();
            System.out.println(tmap.get(d)+"\t"+d);
        }
        //XYPlot[] arr=new XYPlot[3];
        //arr[0]=p1;arr[1]=p2;arr[2]=p3;
        //c1.plot(p1,640,480);
        //CombinedDomainXYPlot  cp=  c1.createCombinedDomainXYPlot("",arr,false);
        //c1.plotCombined(cp,640,480);

    }    
}
