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
import com.ettoremastrogiacomo.utils.DoubleArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

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
               
        
        ArrayList<HashMap<String,String>> map=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(java.util.Arrays.asList("STOCK")), Optional.of(java.util.Arrays.asList("MLSE")), Optional.empty(), Optional.empty());
        ArrayList<String> hc=new ArrayList<>();
        map.forEach((x)->{hc.add(x.get( "hashcode"));});
        List<String> list=Database.getFilteredPortfolio(Optional.of(hc), Optional.of(5000), Optional.of(.2), Optional.of(6), Optional.of(10), Optional.of(1000000), Optional.empty());
        Fints f= new Fints();
        for (String x: list) {f=f.isEmpty()?Database.getFintsQuotes(x).getSerieCopy(3).head(3500):f.merge(Database.getFintsQuotes(x).getSerieCopy(3).head(3500));}
        f=Fints.SMA(Fints.ER(f, 100, true), 200);
        Fints fm=Fints.MEANCOLS(f);
        //Fints f=Database.getFilteredPortfolioOfClose(Optional.of(300), Optional.of("STOCK"), Optional.of("MLSE"), Optional.empty(), Optional.empty(), Optional.of(50),Optional.empty(), Optional.empty());
        Charts c1=new Charts("MOVING_SHARPE");
        System.out.println("MEDIA AL "+f.getLastDate()+"\t:"+ DoubleArray.mean(f.getLastRow()));
        XYPlot p1=c1.createXYPlot("er",f);
        c1.plot(p1,640,480);
        

        //XYPlot[] arr=new XYPlot[3];
        //arr[0]=p1;arr[1]=p2;arr[2]=p3;
        //c1.plot(p1,640,480);
        //CombinedDomainXYPlot  cp=  c1.createCombinedDomainXYPlot("",arr,false);
        //c1.plotCombined(cp,640,480);

    }    
}
