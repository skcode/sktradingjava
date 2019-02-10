package com.ettoremastrogiacomo.sktradingjava;


import java.util.Locale;
import java.util.TimeZone;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeries;
import org.apache.log4j.Logger;
import org.jfree.data.time.TimeSeriesDataItem;



public class Charts extends ApplicationFrame{
	static final long serialVersionUID=0;
	
    String title;
    static Logger logger = Logger.getLogger(Charts.class);
    public Charts(final String title) {
         super(title);
         this.title=title;
    }

    public void plotCombined(CombinedDomainXYPlot plot,int dim_x,int dim_y) {
        final JFreeChart chart = new JFreeChart(title,JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
        panel.setPreferredSize(new java.awt.Dimension(dim_x, dim_y));
        setContentPane(panel);
        this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);
        //chart.pack();
        //chart.setVisible(true);
    }

    public void plot(XYPlot plot,int dim_x,int dim_y) {
        final JFreeChart chart = new JFreeChart(title,JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
        panel.setPreferredSize(new java.awt.Dimension(dim_x, dim_y));
        setContentPane(panel);
        this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);
    }


    public XYPlot createXYPlot(String range,Fints f)throws Exception {
        XYDataset data1=Charts.Fints2Dataset(f);
        final XYItemRenderer renderer1 = new StandardXYItemRenderer();
        final NumberAxis rangeAxis1 = new NumberAxis(range);
        rangeAxis1.setAutoRangeIncludesZero(false);
        org.jfree.chart.axis.DateAxis range2= new org.jfree.chart.axis.DateAxis();
        final XYPlot plot = new XYPlot(data1, range2, rangeAxis1, renderer1);
        return plot;
    }
    
    public CombinedDomainXYPlot createCombinedDomainXYPlot(String domain,XYPlot[] subplot,boolean firstbigger) {
        CombinedDomainXYPlot plot=new CombinedDomainXYPlot(new org.jfree.chart.axis.DateAxis(domain));
        plot.setGap(10.0);
        // add the subplots...
        for (int i=0;i<subplot.length;i++)
            if (i==0 && firstbigger)  plot.add(subplot[i], 3);
        else plot.add(subplot[i], 1);
        plot.setOrientation(PlotOrientation.VERTICAL);
        return plot;
    }

    static public XYDataset Fints2Dataset(Fints f) throws Exception {
        int nots=f.getNoSeries();
        int len=f.getLength();
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        for (int i=0;i<nots;i++) {
           // TimeSeries ts2=new TimeSeries(f.getName(i),org.jfree.data.time.Second.class);
            //TimeSeries ts=new TimeSeries(f.getName(i),org.jfree.data.time.Second.class);
            TimeSeries ts=new TimeSeries(f.getName(i));
            for (int j=0;j<len;j++) {                
                //logger.debug(f.getDate(j));
                //Second s= new Second(f.getDate(j).getSeconds(), f.getDate(j).getMinutes(), f.getDate(j).getHour(),f.getDate(j).getDayofMonth(), f.getDate(j).getMonth(), f.getDate(j).getYear());                
                ts.add(new Second(f.getDate(j).getDate()),f.get(j, i));
            
            }
            dataset.addSeries(ts);
        }
        return dataset;
    }


}
