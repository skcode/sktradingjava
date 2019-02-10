/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.sktradingjava.starters;

import com.ettoremastrogiacomo.sktradingjava.Fints;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.utils.DoubleArray;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author sk
 */
public class RunsTest {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(RunsTest.class);
/** 
 * Returns the height of the normal distribution at the specified z-score
     * @param z
     * @return valore gaussiana normale al punto z
 */
public static double getNormalProbabilityAtZ(double z) {
    return Math.exp(-Math.pow(z, 2) / 2) / Math.sqrt(2 * Math.PI);
}
/**
  * Returns the area under the normal curve between the z-scores z1 and z2
     * @param z1
     * @param z2
     * @return area (probabilità) sottesa dai punti z1 e z2 della gaussiana normale
  */
public static double getAreaUnderNormalCurve(double z1, double z2) {
    double area = 0.0;
    final int rectangles = 1000000; // more rectangles = more precise, less rectangles = quicker execution
    final double width = (z2 - z1) / rectangles;
    for(int i = 0; i < rectangles; i++)
        area += width * getNormalProbabilityAtZ(width * i + z1);
    return area;
}

/**
 * esegue il runs test di una serie di valori (e.g. chiusura titoli azionari)
 * @param values serie di valori (e.g. close)
 * @return true se è significativo al 95% un processo non random dei ritorni, false altrimenti
 */
public static boolean runsTest95(double[] values){
    double[] diff=new double[values.length-1];
    double[] binary=new double[values.length-1];
    double[] runs=new double[values.length-1];
    for (int i=0;i<diff.length;i++) diff[i]=100*(values[i+1]-values[i])/values[i];    
    for (int i=0;i<diff.length;i++) binary[i]=diff[i]>0?1:0;
    runs[0]=1;
    for (int i=1;i<diff.length;i++) runs[i]=binary[i]==binary[i-1]?runs[i-1]:runs[i-1]+1;
    double R=runs[runs.length-1],W=DoubleArray.sum(binary),L=binary.length-W,N=W+L,X=(2*W*L)/(W+L);
    double Z=(R-(X+1))/Math.sqrt(X*(X-1)/(N-1));//http://www.adaptrade.com/Articles/article-dep.htm
    LOG.debug("Z value : " +Z);
    return Math.abs(Z)>1.96; //non random
        //else random    
}

static public boolean runsTest95bis(double[] values) throws Exception{

            //values=DoubleArray.normalizeStd(values);
            double [] diff=new double[values.length-1];
            for (int i=0;i<diff.length;i++) diff[i]=100*(values[i+1]-values[i])/values[i];
            int [] binary=new int[diff.length];
            double [] runs=new double[diff.length];
            for (int i=0;i<diff.length;i++) binary[i]=diff[i]<=0?0:1;
            runs[0]=1;
            for (int i=1;i<diff.length;i++) runs[i]=binary[i]==binary[i-1]?runs[i-1]:runs[i-1]+1;
            double R,n0=0,n1=0,n,eR,varR,stDevR,Z;
            R=runs[runs.length-1];
            for (int i=0;i<diff.length;i++) if (binary[i]==0) n0++;else n1++;
            n=n0+n1;eR=1.0+2.0*n0*n1/(double)n;
            varR=(2*n0*n1*(2*n0*n1-n))/(double)((n*n)*(n-1));
            stDevR=Math.sqrt(varR);
            Z=(R-eR)/stDevR;
            
            /*pvalue=getAreaUnderNormalCurve(-1000, Z)*2;
            double significativita=0.05;//95%*/
            //if (n0<20 || n1<20) LOG.warn("few runs ");//throw new Exception("too few runs");
            LOG.debug("Z value : "+Z);

        return Math.abs(Z)>1.96; //LOG.debug("ipotesi non random accettatta al 95%");else LOG.debug("ipotesi random accettata al 95%");
        //LOG.debug(pvalue);

}

    public static void main(String[] args) throws Exception{
        List<HashMap<String,String>> records=Database.getRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Arrays.asList("STOCK")), Optional.of(Arrays.asList("MLSE")), Optional.of(Arrays.asList("EUR")), Optional.empty());
        List<String> hashes=new java.util.ArrayList<>();
        records.forEach((x)->{hashes.add(x.get("hashcode"));});       
        int win=200;
        List<String> list=Database.getFilteredPortfolio(Optional.of(hashes), Optional.of(win), Optional.of(.2), Optional.of(6), Optional.of(10), Optional.of(100000), Optional.empty());
        for (String s : list) {
                Fints f=Database.getFintsQuotes(s).getSerieCopy(3).head(win);                
                if (runsTest95(f.getSerieVector(0))) {
                     List<HashMap<String,String>> l=Database.getRecords(Optional.of("where hashcode='"+s+"'"));
                     LOG.info(l.get(0).get("name")+"\t"+l.get(0).get("code")+"\t"+l.get(0).get("sector"));
                }
                //LOG.info("analising "+s);
                //LOG.info(runsTest95(f.getSerieVector(0)));
                //LOG.info(runsTest95bis(f.getSerieVector(0)));
        }
        
        
        
    }
    
}
