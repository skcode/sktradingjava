package com.ettoremastrogiacomo.sktradingjava;

import com.ettoremastrogiacomo.utils.UDate;
import com.ettoremastrogiacomo.utils.DoubleArray;
import com.ettoremastrogiacomo.utils.DoubleDoubleArray;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
//import java.util.Locale;
import org.apache.log4j.*;
import org.jfree.chart.plot.XYPlot;

public final class Fints {

    public static enum frequency {
        SECOND(10), MINUTE(100), HOUR(1000), DAILY(10000), WEEKLY(100000), MONTHLY(1000000), YEARLY(10000000);
        private final int value;
        private frequency(int value) {this.value=value;}
    };

    private static final Logger LOG = Logger.getLogger(Fints.class);
    private final int length;
    private final frequency freq;
    private final List<UDate> dates;
    //final int length, noseries;

    private final List<String> names;
    private final double[][] matrix;

    public int getLength() {
        return dates.size();
    }

    public int getNoSeries() {
        return names.size();
    }

    public Fints() {//empty fints
        this.freq = Fints.frequency.DAILY;
        this.dates = Collections.unmodifiableList( new ArrayList<>());//dates.add(Calendar.getInstance().getTime());
        this.names = Collections.unmodifiableList( new ArrayList<>());//names.add(this.toString());
        matrix = new double[0][0];
        this.length=this.dates.size();
    }

    public Fints(List<UDate> dates, List<String> names, frequency freq, double[][] matrix) throws Exception {
        this.freq = freq;        
        //this.names = new ArrayList<>();
        //check
        java.util.ArrayList<UDate> newdate=new java.util.ArrayList<>();
        if (com.ettoremastrogiacomo.utils.DoubleDoubleArray.nRows(matrix) != dates.size()
                || com.ettoremastrogiacomo.utils.DoubleDoubleArray.nCols(matrix) != names.size()) {
            throw new Exception("incompatible matrix dimension: dates="+dates.size()+"\tnames="+names.size()+"\tmatrix="+com.ettoremastrogiacomo.utils.DoubleDoubleArray.nRows(matrix)+"x"+com.ettoremastrogiacomo.utils.DoubleDoubleArray.nCols(matrix));
            //newdate.add(UDate.re)
        }
        
        for (int k = 0; k < dates.size(); k++) {

            switch (freq){
                case SECOND:
                    newdate.add(UDate.roundSecond(dates.get(k)));break;
                case MINUTE:
                    newdate.add(UDate.roundMinute(dates.get(k)));break;
                case HOUR:
                    newdate.add(UDate.roundHour(dates.get(k)));break;
                case DAILY:
                    newdate.add(UDate.roundDayOfMonth(dates.get(k)));break;
                case MONTHLY:
                    newdate.add(UDate.roundMonth(dates.get(k)));break;
                case WEEKLY:
                    newdate.add(UDate.roundWeek(dates.get(k)));break;
                case YEARLY:
                    newdate.add(UDate.roundYear(dates.get(k)));break;
                default:
                    throw new Exception("frequency "+freq+" not yet implemented");
            }
            if (k>0)
                if (newdate.get(k).getTime() <= newdate.get(k - 1).getTime()) {
                    throw new Exception("no sequential dates :" + newdate.get(k) + "<=" + newdate.get(k - 1));
                }            
            
        }
        //
        
        this.dates=Collections.unmodifiableList(newdate);
        this.names=Collections.unmodifiableList(names);//   addAll(names);
        this.matrix = new double[getLength()][getNoSeries()];
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(matrix[i], 0, this.matrix[i], 0, getNoSeries());
        }
        this.length=this.dates.size();
    }

    public boolean isEmpty() {
        return dates.isEmpty();
    }

    public int getDateIdxFrom(UDate d) throws Exception{
        int k=-1;
        for (int i=0;i<this.length;i++) if (dates.get(i).compareTo(d)>=0) {k=i;break;}
        if (k<0) throw new Exception("dates before "+d);
        return k;
    } 

    public int getDateIdxTo(UDate d) throws Exception{
        int k=-1;
        for (int i=(this.length-1);i>=0;i--) if (dates.get(i).compareTo(d)<=0) {k=i;break;}
        if (k<0) throw new Exception("dates after "+d);
        return k;
    } 
     
    
    static public Fints removeOutliers(Fints f, int nsigma, int idx) throws Exception {
        if (nsigma < 1) {
            throw new Exception("nsigma must be >= 1");
        }
        if (f.getNoSeries() < 1) {
            throw new Exception("empty Fints");
        }
        if (idx < 0 || idx >= f.getNoSeries()) {
            throw new Exception("idx must be in [0-" + (f.getNoSeries() - 1));
        }
        //ArrayList<Date> date = new ArrayList<>();
        double[] v = new double[f.getLength()];
        for (int k = 0; k < f.getLength(); k++) {
            v[k] = f.matrix[k][idx];
        }
        double std = com.ettoremastrogiacomo.utils.DoubleArray.std(v);
        double mean = com.ettoremastrogiacomo.utils.DoubleArray.mean(v);
        java.util.ArrayList<Integer> listok = new java.util.ArrayList<>();
        for (int i = 0; i < f.getLength(); i++) {
            if (f.matrix[i][idx] <= (mean + nsigma * std) && f.matrix[i][idx] >= (mean - nsigma * std)) {
                listok.add(i);
            }
        }
        int newsize = listok.size();
        java.util.ArrayList<UDate> newdate = new java.util.ArrayList<>();
        double[][] newmatrix = new double[newsize][f.getNoSeries()];
        int k = 0;
        listok.stream().map((i) -> {
            newdate.add(f.dates.get(i));
            return i;
        }).forEachOrdered((i) -> {
            System.arraycopy(f.matrix[i], 0, newmatrix[k], 0, f.getNoSeries());
        });
        return new Fints(newdate, f.names, f.freq, newmatrix);
    }

    static public Fints append(Fints f1, Fints f2) throws Exception {
        if (f1.freq != f2.freq) {
            throw new Exception("frequence doesn't match");
        }
        if (f1.getNoSeries() != f2.getNoSeries()) {
            throw new Exception("TS number doesn't match");
        }
        ArrayList<UDate> date = new ArrayList<>();
        int k = -1;
        for (int i = 0; i < f2.dates.size(); i++) {
            if (f2.dates.get(i).after(f1.dates.get(f1.dates.size() - 1))) {
                k = i;
                break;
            }
        }
        if (k < 0) {
            return f1;
        }
        int newlen = f1.getLength() + f2.getLength() - k;
        double[][] mat = new double[newlen][f1.getNoSeries()];
        for (int i = 0; i < f1.getLength(); i++) {
            date.add(f1.dates.get(i));
            System.arraycopy(f1.matrix[i], 0, mat[i], 0, f1.getNoSeries());
        }
        for (int i = k; i < f2.getLength(); i++) {
            date.add(f2.dates.get(i));
            System.arraycopy(f2.matrix[i], 0, mat[f1.getLength() - k + i], 0, f1.getNoSeries());
        }
        Fints newf = new Fints(date, f1.names, f1.freq, mat);
        return newf;
    }

    static public ArrayList<UDate> mergeDate(ArrayList<UDate> d1, ArrayList<UDate> d2) throws Exception {
        ArrayList<UDate> date = new ArrayList<>();
        d1.stream().filter((d) -> (d2.indexOf(d) >= 0)).forEachOrdered((d) -> {
            date.add(d);
        });
        return date;
    }

    static public Fints merge(Fints f1, Fints f2) throws Exception {
        if (f1.freq != f2.freq) {
            throw new Exception("frequence doesn't match");
        }
        ArrayList<String> names = new ArrayList<>();
        ArrayList<UDate> date = new ArrayList<>();
        ArrayList<Integer> v1int = new ArrayList<>(), v2int = new ArrayList<>();
                    for (int i = 0; i < f1.getLength(); i++) {
                        int k = f2.dates.indexOf(f1.dates.get(i));
                        if (k >= 0) {
                            v2int.add(k);
                            v1int.add(i);
                            date.add(f1.dates.get(i));
                        }
                    }
                    /*
        if (null == f1.freq) {
            throw new Exception("frequence unknown");
        } else {
            switch (f1.freq) {
                case DAILY:
                    for (int i = 0; i < f1.getLength(); i++) {
                        int k = f2.dates.indexOf(f1.dates.get(i));
                        if (k >= 0) {
                            v2int.add(k);
                            v1int.add(i);
                            date.add(f1.dates.get(i));
                        }
                    }
                    break;
                case WEEKLY: {
                    int k = 0;
                    for (int i = 0; i < f1.getLength(); i++) {
                        java.util.Calendar c = Calendar.getInstance();
                        c.setTime(f1.getDate(i).getDate());
                        int f1w = c.get(Calendar.WEEK_OF_YEAR);
                        int f1y = c.get(Calendar.YEAR);
                        for (int j = k; j < f2.getLength(); j++) {
                            c.setTime(f2.getDate(j).getDate());
                            int f2w = c.get(Calendar.WEEK_OF_YEAR);
                            int f2y = c.get(Calendar.YEAR);
                            if (f2w == f1w && f2y == f1y) {
                                v1int.add(i);
                                v2int.add(j);
                                date.add(f1.dates.get(i));
                                k = j + 1;
                                break;
                            }
                        }
                    }
                    break;
                }
                case MONTHLY: {
                    int k = 0;
                    for (int i = 0; i < f1.getLength(); i++) {
                        java.util.Calendar c = Calendar.getInstance();
                        c.setTime(f1.getDate(i).getDate());
                        int f1w = c.get(Calendar.MONTH);
                        int f1y = c.get(Calendar.YEAR);
                        for (int j = k; j < f2.getLength(); j++) {
                            c.setTime(f2.getDate(j).getDate());
                            int f2w = c.get(Calendar.MONTH);
                            int f2y = c.get(Calendar.YEAR);
                            if (f2w == f1w && f2y == f1y) {
                                v1int.add(i);
                                v2int.add(j);
                                date.add(f1.dates.get(i));
                                k = j + 1;
                                break;
                            }
                        }
                    }
                    break;
                }
                default:
                    throw new Exception("frequence unknown");
            }
        }
                    */
        if (date.size() < 1) {
            throw new Exception("disjoint Fints");
        }

        int col1 = f1.getNoSeries();
        int col2 = f2.getNoSeries();
        for (int i = 0; i < col1; i++) {
            names.add(f1.names.get(i));
        }
        for (int i = 0; i < col2; i++) {
            names.add(f2.names.get(i));
        }
        double[][] matrix = new double[date.size()][col1 + col2];
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(f1.matrix[v1int.get(i)], 0, matrix[i], 0, col1);
            System.arraycopy(f2.matrix[v2int.get(i)], 0, matrix[i], col1, col2);
        }
        Fints newf = new Fints(date, names, f1.freq, matrix);
        return newf;
    }

    public Fints merge(Fints f) throws Exception {
        return Fints.merge(this, f);
    }

    public int getIndex(String name) {
        return this.names.indexOf(name);
    }

    public int getIndex(UDate date) {
        return this.dates.indexOf(date);
    }

    public String getName(int i) {
        return this.names.get(i);
    }
    
    public List<String> getName() {
        return this.names;
    }
    //public void setName(int i,String name) {this.names.set(i, name);}

    public double[][] getMatrixCopy() {
        double[][] mat = new double[getLength()][getNoSeries()];
        for (int i = 0; i < getLength(); i++) {
            System.arraycopy(this.matrix[i], 0, mat[i], 0, getNoSeries());
        }
        return mat;
    }

    public List<UDate> getDate() {
        return this.dates;
    }

    public Fints getSerieCopy(int i) throws Exception {

        ArrayList<String> vname = new ArrayList<>();
        vname.add(this.names.get(i));
        double[][] mat = new double[getLength()][1];
        for (int k = 0; k < getLength(); k++) {
            mat[k][0] = this.matrix[k][i];
        }
        Fints fints = new Fints(this.dates, vname, this.freq, mat);
        return fints;
    }

    public double[] getSerieVector(int i) throws Exception {
        double[] v = new double[getLength()];
        for (int k = 0; k < getLength(); k++) {
            v[k] = this.matrix[k][i];
        }
        return v;
    }

    public UDate getDate(int i) {
        return dates.get(i);
    }

    public UDate getFirstDate() {
        return getDate(0);
    }

    public UDate getLastDate() {
        return getDate(getLength() - 1);
    }
    
    public double[] getRow (int i) {
        double [] d=new double[this.getNoSeries()];
        System.arraycopy(matrix[i], 0, d, 0, d.length);
        return d;
    }

    public double[] getLastRow () {
        return getRow(matrix.length-1);
    }


    public double getSecondsFromNow() {
        UDate d=new UDate();        
        return ((d.getTime()-getLastDate().getTime())/1000.0);        
    }
    
    public double getDaysFromNow() {
        double d=60*60*24;
        return getSecondsFromNow()/d;
    }
    public double get(int i, int j) {
        return this.matrix[i][j];
    }

    public Fints.frequency getFrequency() {
        return this.freq;
    }

    public long getMaxDateGap() {
        long gap = -1;
        for (int i = 1; i < getLength(); i++) {
            long t1 = this.dates.get(i).getTime() - this.dates.get(i - 1).getTime();
            if (t1 > gap) {
                gap = t1;
                //LOG.debug("gap for "+this.names.get(0)+"\t"+this.dates.get(i)+"\t"+this.dates.get(i-1));
            }
        }
        return gap;
    }

    public double getMaxDaysDateGap() {
        double d=1000*60*60*24;
        return (getMaxDateGap()/d);
    }
    
    public double getMaxAbsPercentValueGap(int col) throws Exception {
        double gap = 0;
        if (col < 0 || col >= this.getNoSeries()) {
            throw new Exception("bad index :" + col);
        }
        for (int i = 1; i < getLength(); i++) {
            double t1 = this.matrix[i - 1][col] != 0 ? (this.matrix[i][col] - this.matrix[i - 1][col]) / this.matrix[i - 1][col] : 0;// this.dates.get(i).getTime() - this.dates.get(i - 1).getTime();
            t1 = Math.abs(t1);
            if (t1 > gap) {
                gap = t1;
                //logger.info("gap for "+this.names.get(0)+"\t"+this.dates.get(i)+"\t"+this.dates.get(i-1));
            }
        }
        return gap;
    }
    /**
     * 
     * @param subset lista di indici rappresentanti le colonne del nuovo fints
     * @return nuovo fints contenente le colonne indicate dal subset (ammette duplicati)
     * @throws Exception 
     */
    public Fints SubSeries(ArrayList<Integer> subset) throws Exception {
        int newNoSeries=subset.size();
        double[][] newmat=new double[this.matrix.length][newNoSeries];
        ArrayList<String> newNames=new ArrayList<>();
        for (int i=0;i<this.matrix.length;i++) {            
            for (int j=0;j<subset.size();j++) {
                newmat[i][j]=this.matrix[i][subset.get(j)];
            }
        }
        for (int i=0;i<subset.size();i++) newNames.add(this.names.get(subset.get(i)));
        Fints newf = new Fints(this.dates, newNames, this.freq, newmat);
        return newf;
    }
/**
 * 
 * @param from
 * @param to
 * @return new Fints extracted from original with date range [from..to] 
 * @throws Exception 
 */
    public Fints Sub(UDate from, UDate to) throws Exception {
        ArrayList<UDate> newd = new ArrayList<>();
        for (int i = 0; i < getLength(); i++) {
            if (this.dates.get(i).compareTo(from) >= 0
                    && this.dates.get(i).compareTo(to) <= 0) {
                newd.add(this.dates.get(i));
            }
        }
        if (newd.size() < 1) {
            throw new Exception("no data in range");
        }
        int k0 = this.dates.indexOf(newd.get(0));
        int newi = newd.size(), newj = this.getNoSeries();
        double[][] newm = new double[newi][newj];
        for (int i = 0; i < newi; i++) {
            System.arraycopy(this.matrix[i + k0], 0, newm[i], 0, newj);
        }
        Fints newf = new Fints(newd, names, this.freq, newm);

        return newf;
    }

    
/**
 * 
 * @param from
 
 * @return new Fints extracted from original with date range [from..end] 
 * @throws Exception 
 */
    public Fints head(UDate from) throws Exception {
        return Sub(from,this.getLastDate());
    }


/**
 * 
 * @param to
 
 * @return new Fints extracted from original with date range [from..end] 
 * @throws Exception 
 */
    public Fints tail(UDate to) throws Exception {
        return Sub(this.getFirstDate(),to);
    }

    
    /**
     * 
     * @param Yfrom
     * @param Yto
     * @param Xfrom
     * @param Xto
     * @return new Fints with date idx range in [Yfrom..Yto] and serie column range in [Xfrom..Xto]
     * @throws Exception 
     */
    public Fints Sub(int Yfrom, int Yto, int Xfrom, int Xto) throws Exception {
        if (Yfrom < 0 || Yfrom > Yto || Yto >= getLength()
                || Xfrom < 0 || Xfrom > Xto || Xto >= this.getNoSeries()) {
            throw new Exception("bad ranges: " + Yfrom + " " + Yto + " " + Xfrom + " " + Xto);
        }
        ArrayList<UDate> newd = new ArrayList<>();
        ArrayList<String> newn = new ArrayList<>();
        double[][] m = new double[Yto - Yfrom + 1][Xto - Xfrom + 1];
        for (int i = Yfrom; i <= Yto; i++) {
            newd.add(this.dates.get(i));
            for (int j = Xfrom; j <= Xto; j++) {
                m[i - Yfrom][j - Xfrom] = this.matrix[i][j];
            }
        }
        for (int j = Xfrom; j <= Xto; j++) {
            newn.add(this.names.get(j));
        }
        Fints f = new Fints(newd, newn, this.freq, m);

        return f;
    }
/**
 * 
 * @param Yfrom
 * @param Yto
 * @return new Fints with date idx range in [Yfrom..Yto] 
 * @throws Exception 
 */
    public Fints SubRows(int Yfrom, int Yto) throws Exception {
        return Sub(Yfrom, Yto, 0, this.getNoSeries() - 1);
    }
/**
 * 
 * @param size
 * @return new Fints with the last 'size' date samples
 * @throws Exception 
 */
    public Fints head(int size) throws Exception {
        return Sub(this.getLength() - size, this.getLength() - 1, 0, this.getNoSeries() - 1);
    }
/**
 * 
 * @param size
 * @return new Fints with the first 'size' date samples
 * @throws Exception 
 */
    public Fints tail(int size) throws Exception {
        return Sub(0, size - 1, 0, this.getNoSeries() - 1);
    }


    public static Fints multiLag(Fints f,int size) throws Exception {
        if (size<=0) return f;
        java.util.ArrayList<Fints> af=new java.util.ArrayList<>();
        Fints t=Lag(f,1),ret=new Fints();
        for (int i=0;i<size;i++) {
            ret=i==0?t:ret.merge(t);
            t=Lag(t,1);
        }
        return ret;
    }
    
    public static Fints Lag(Fints f, int step) throws Exception {
        if (step == 0) {
            return f;
        }
        int len = f.getLength();
        double[][] m = f.getMatrixCopy();
        ArrayList<UDate> newd = new ArrayList<>();
        //newd.setSize(len-step);
        double[][] newm = new double[len - step][f.getNoSeries()];
        if (step > 0) {
            for (int j = 0; j < f.getNoSeries(); j++) {
                for (int i = 0; i < (len - step); i++) {
                    newm[i][j] = m[i][j];
                }
            }
            for (int i = 0; i < (len - step); i++) {
                newd.add(f.dates.get(i + step));
            }
        } else {
            for (int j = 0; j < f.getNoSeries(); j++) {
                for (int i = 0; i < (len - step); i++) {
                    newm[i][j] = m[i + step][j];
                }
            }
            for (int i = 0; i < (len - step); i++) {
                newd.add(f.dates.get(i));
            }
        }
        ArrayList<String> newn = new ArrayList<>();
        //newn.setSize(f.getNoSeries());
        for (int i = 0; i < f.getNoSeries(); i++) {
            newn.add("LAG(" + f.names.get(i) + "," + step + ")");
        }
        Fints ret = new Fints(newd, newn, f.freq, newm);

        return ret;
    }

    static public Fints Sign(Fints f, double threshold, double low, double high) throws Exception {
        double[][] m = f.matrix;
        int len = f.getLength(), ts = f.getNoSeries();
        ArrayList<String> newn = new ArrayList<>();
        //newn.setSize(ts);
        double[][] newm = new double[len][ts];
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < ts; j++) {
                newm[i][j] = m[i][j] < threshold ? low : high;
            }
        }
        for (int i = 0; i < f.getNoSeries(); i++) {
            newn.add("SIGN(" + f.names.get(i) + "," + threshold + "," + low + "," + high + ")");
        }
        Fints ret = new Fints(f.dates, newn, f.freq, newm);

        return ret;
    }
/**
 * 
 * @param f
 * @param mult 
 * @param log
 * @return Fints with excess returns (row-1 samples),  mult*(x(t)-x(t-1))/x(t-1) or mult*(log10(x(t)/x(t-1)))
 * @throws Exception 
 */
    static public Fints ER(Fints f, double mult, boolean log) throws Exception {
        if (f.getLength() < 2) {
            throw new Exception("data length must be >=2");
        }
        int n = f.getNoSeries();
        double[][] newm = new double[f.getLength() - 1][n];
        ArrayList<UDate> newd = new ArrayList<>();
        //newd.setSize(f.getLength()-1);
        ArrayList<String> newn = new ArrayList<>();
        //newn.setSize(n);
        for (int i = 1; i < f.getLength(); i++) {
            newd.add(f.dates.get(i));
            for (int j = 0; j < n; j++) {
                newm[i - 1][j] = mult * (log ? Math.log10(f.matrix[i][j]) - Math.log10(f.matrix[i - 1][j])
                        : (f.matrix[i][j] - f.matrix[i - 1][j]) / f.matrix[i - 1][j]);
            }
        }
        for (int i = 0; i < f.getNoSeries(); i++) {
            newn.add("ER(" + f.names.get(i) + "," + mult + "," + log + ")");
        }
        Fints newf = new Fints(newd, newn, f.freq, newm);

        return newf;
    }
/**
 * 
 * @param f
 * @return Fints with first difference x(t)-x(t-1)
 * @throws Exception 
 */
    static public Fints Diff(Fints f) throws Exception {
        if (f.getLength() < 2) {
            throw new Exception("data length must be >=2");
        }
        int n = f.getNoSeries();
        double[][] newm = new double[f.getLength() - 1][n];
        ArrayList<UDate> newd = new ArrayList<>();
        //newd.setSize(f.getLength()-1);
        ArrayList<String> newn = new ArrayList<>();
        //newn.setSize(n);
        for (int i = 1; i < f.getLength(); i++) {
            newd.add(f.dates.get(i));
            for (int j = 0; j < n; j++) {
                newm[i - 1][j] = f.matrix[i][j] - f.matrix[i - 1][j];
            }
        }
        for (int i = 0; i < f.getNoSeries(); i++) {
            newn.add("DIFF(" + f.names.get(i) + ")");
        }
        Fints newf = new Fints(newd, newn, f.freq, newm);

        return newf;
    }

    /**
     * differenza per righe dei 2 fints viene effettuato
     *
     * @param f1
     * @param f2
     * @return f1-f2
     * @throws Exception
     */
    static public Fints Diff(Fints f1, Fints f2) throws Exception {
        if (f1.getNoSeries() != f2.getNoSeries()) {
            throw new Exception("series number must be equal");
        }
        Fints newf = Fints.merge(f1, f2);
        double[][] newm = new double[newf.getLength()][f1.getNoSeries()];
        List<UDate> newd = newf.getDate();
        List<String> newn = new ArrayList<>();
        for (int j = 0; j < f1.getNoSeries(); j++) {
            newn.add("DIFF(" + f1.getName(j) + "," + f2.getName(j) + ")");
            for (int i = 0; i < newf.getLength(); i++) {
                newm[i][j] = newf.get(i, j) - newf.get(i, j + f1.getNoSeries());
            }
        }
        newf = new Fints(newd, newn, f1.freq, newm);
        return newf;
    }

    static public Fints SUM(Fints f1, Fints f2) throws Exception {
        if (f1.getNoSeries() != f2.getNoSeries()) {
            throw new Exception("series number must be equal");
        }
        Fints newf = Fints.merge(f1, f2);
        double[][] newm = new double[newf.getLength()][f1.getNoSeries()];
        List<UDate> newd = newf.getDate();
        List<String> newn = new ArrayList<>();
        for (int j = 0; j < f1.getNoSeries(); j++) {
            newn.add("SUM(" + f1.getName(j) + "," + f2.getName(j) + ")");
            for (int i = 0; i < newf.getLength(); i++) {
                newm[i][j] = newf.get(i, j) + newf.get(i, j + f1.getNoSeries());
            }
        }
        newf = new Fints(newd, newn, f1.freq, newm);
        return newf;
    }

    static public Fints PROD(Fints f1, Fints f2) throws Exception {
        if (f1.getNoSeries() != f2.getNoSeries()) {
            throw new Exception("series number must be equal");
        }
        Fints newf = Fints.merge(f1, f2);
        double[][] newm = new double[newf.getLength()][f1.getNoSeries()];
        List<UDate> newd = newf.getDate();
        List<String> newn = new ArrayList<>();
        for (int j = 0; j < f1.getNoSeries(); j++) {
            newn.add("PROD(" + f1.getName(j) + "," + f2.getName(j) + ")");
            for (int i = 0; i < newf.getLength(); i++) {
                newm[i][j] = newf.get(i, j) * newf.get(i, j + f1.getNoSeries());
            }
        }
        newf = new Fints(newd, newn, f1.freq, newm);
        return newf;
    }

    static public Fints DIV(Fints f1, Fints f2) throws Exception {
        if (f1.getNoSeries() != f2.getNoSeries()) {
            throw new Exception("series number must be equal");
        }
        Fints newf = Fints.merge(f1, f2);
        double[][] newm = new double[newf.getLength()][f1.getNoSeries()];
        List<UDate> newd = newf.getDate();
        List<String> newn = new ArrayList<>();
        for (int j = 0; j < f1.getNoSeries(); j++) {
            newn.add("DIV(" + f1.getName(j) + "," + f2.getName(j) + ")");
            for (int i = 0; i < newf.getLength(); i++) {
                newm[i][j] = newf.get(i, j) / newf.get(i, j + f1.getNoSeries());
            }
        }
        newf = new Fints(newd, newn, f1.freq, newm);
        return newf;
    }

    /**
     * prodotto di kronecker per righe
     *
     * @param f1
     * @param f2
     * @return prodotto di kronecker per righe
     * @throws Exception
     */
    static public Fints Kron(Fints f1, Fints f2) throws Exception {
        if (f1.getLength() != f2.getLength() || f1.freq != f2.freq) {
            throw new Exception("size doesn't match");
        }

        ArrayList<String> newn = new ArrayList<>();
        int newser = f1.getNoSeries() * f2.getNoSeries();
        double[][] newm = new double[f1.getLength()][newser];
        for (int i = 0; i < f1.getLength(); i++) {
            for (int j1 = 0; j1 < f1.getNoSeries(); j1++) {
                for (int j2 = j1 * f2.getNoSeries(); j2 < ((j1 + 1) * f2.getNoSeries()); j2++) {
                    newm[i][j2] = f1.matrix[i][j1] * f2.matrix[i][j2 - (j1 * f2.getNoSeries())];
                    if (i == 0) {
                        newn.add("KRON(" + f1.getName(j1) + "," + f2.getName(j2 - (j1 * f2.getNoSeries())) + ")");
                    }
                }
            }
        }
        //logger.debug(newn.size()+"   "+newm.getLength()+"   "+newm[0].getLength());
        Fints newf = new Fints(f1.dates, newn, f1.freq, newm);
        return newf;
    }

    /**
     * simple moving average
     *
     * @param source fints di input
     * @param period periodo
     * @return media mobile
     * @throws Exception
     */
    static public Fints SMA(Fints source, int period) throws Exception {
        if ((period < 1) || period > source.getLength()) {
            throw new Exception("bad length :" + period);
        }
        int newlen = source.getLength() - period + 1;
        ArrayList<UDate> newdate = new ArrayList<>();
        double[][] newm = new double[newlen][source.getNoSeries()];
        for (int i = period - 1, k = 0; i < source.getLength(); i++, k++) {
            newdate.add(source.dates.get(i));
        }

        for (int i = (period - 1); i < source.getLength(); i++) {
            for (int j = 0; j < source.getNoSeries(); j++) {
                double d1 = 0;
                for (int k = i - period + 1; k < (i + 1); k++) {
                    d1 += source.matrix[k][j];
                }
                d1 /= period;
                newm[i - period + 1][j] = d1;
            }
        }
        ArrayList<String> newn = new ArrayList<>();
        for (int i = 0; i < source.getNoSeries(); i++) {
            newn.add("SMA(" + source.names.get(i) + "," + period + ")");
        }
        Fints dest = new Fints(newdate, newn, source.freq, newm);
        return dest;
    }

    static public Fints Sharpe(Fints source, int period) throws Exception {
        if ((period < 1) || period > source.getLength()) {
            throw new Exception("bad length :" + period + "\n" + source.toString());
        }
        int newlen = source.getLength() - period + 1;
        ArrayList<UDate> newdate = new ArrayList<>();
        double[][] newm = new double[newlen][source.getNoSeries()];
        for (int i = period - 1, k = 0; i < source.getLength(); i++, k++) {
            newdate.add(source.dates.get(i));
        }

        for (int i = (period - 1); i < source.getLength(); i++) {
            for (int j = 0; j < source.getNoSeries(); j++) {
                double d1 = 0, d2 = 0;
                for (int k = i - period + 1; k < (i + 1); k++) {
                    d1 += source.matrix[k][j];
                }
                d1 /= period;
                for (int k = i - period + 1; k < (i + 1); k++) {
                    d2 += Math.pow((source.matrix[k][j] - d1), 2);
                }
                d2 /= period;
                d2 = Math.sqrt(d2);
                newm[i - period + 1][j] = d1 / d2;
            }
        }
        ArrayList<String> newn = new ArrayList<>();
        for (int i = 0; i < source.getNoSeries(); i++) {
            newn.add("SHARPE(" + source.names.get(i) + "," + period + ")");
        }
        Fints dest = new Fints(newdate, newn, source.freq, newm);
        return dest;
    }

    static public Fints Volatility(Fints source, int period) throws Exception {
        if ((period < 1) || period > source.getLength()) {
            throw new Exception("bad length :" + period + "\n" + source.toString());
        }
        int newlen = source.getLength() - period + 1;
        ArrayList<UDate> newdate = new ArrayList<>();
        double[][] newm = new double[newlen][source.getNoSeries()];
        for (int i = period - 1, k = 0; i < source.getLength(); i++, k++) {
            newdate.add(source.dates.get(i));
        }

        for (int i = (period - 1); i < source.getLength(); i++) {
            for (int j = 0; j < source.getNoSeries(); j++) {
                double d1 = 0, d2 = 0;
                for (int k = i - period + 1; k < (i + 1); k++) {
                    d1 += source.matrix[k][j];
                }
                d1 /= period;
                for (int k = i - period + 1; k < (i + 1); k++) {
                    d2 += Math.pow((source.matrix[k][j] - d1), 2);
                }
                d2 /= period;
                d2 = Math.sqrt(d2);
                newm[i - period + 1][j] = d2;
            }
        }
        ArrayList<String> newn = new ArrayList<>();
        for (int i = 0; i < source.getNoSeries(); i++) {
            newn.add("VOLATILITY(" + source.names.get(i) + "," + period + ")");
        }
        Fints dest = new Fints(newdate, newn, source.freq, newm);
        return dest;
    }

    public double[][] getCovariance() throws Exception {
        return DoubleDoubleArray.cov(matrix);
    }

    public double getWeightedCovariance(double[] w) throws Exception {
        if (w.length != this.getNoSeries()) {
            throw new Exception("weights lenght must match :" + w.length + "!=" + this.getNoSeries());
        }
        double[][] cov = DoubleDoubleArray.cov(matrix);
        double s = DoubleArray.sum(w);
        if (Math.abs(s - 1.0) > 0.0000001) {
            throw new Exception("weights sum must be near 1.0 :"+s);
        }
        double res = 0;
        for (int i = 0; i < w.length; i++) {
            for (int j = 0; j < w.length; j++) {
                res += w[i] * w[j] * cov[i][j];
            }
        }
        return res;
    }

    public double getEqualWeightedCovariance() throws Exception {
        double[] w= new double[this.getNoSeries()];
        for (int i=0;i<w.length;i++) w[i]=1.0/(double)w.length;
        return getWeightedCovariance(w);
    }
    public double[][] getCorrelation() throws Exception {
        return DoubleDoubleArray.corr(matrix);
    }

    public double[] getMeans() throws Exception {
        return DoubleDoubleArray.mean(matrix);
    }
    public double[] getSums() throws Exception {
        return DoubleDoubleArray.sum(matrix);
    }
    
    public double[] getMax() throws Exception {
        return DoubleDoubleArray.max(matrix);
    }

    public double[] getMin() throws Exception {
        return DoubleDoubleArray.min(matrix);
    }
    
/**
 * esegue il runs test di una serie di valori (e.g. chiusura titoli azionari)
 * @param serieidx indice serie
 * @return true se è significativo al 95% un processo non random dei ritorni, false altrimenti
     * @throws java.lang.Exception
 */
    public  boolean runsTest95(int serieidx) throws Exception{
        if (serieidx>=this.getNoSeries()) throw new Exception("bad idx:"+ serieidx);
        double[] values=new double[matrix.length];
        for (int i=0;i<values.length;i++) values[i]=matrix[i][serieidx];
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

    
    @Override
    public String toString() {
        String s = "\nsize= " + getLength() + "x" + this.getNoSeries() + "\t"+this.getFrequency()+ "\tfrom "+this.getFirstDate()+ "\tto "+this.getLastDate()+ "\tSERIES: ";
        for (int i = 0; i < this.names.size(); i++) {
            s += names.get(i) + "\t";
        }
        return s;
    }

    public String toStringL() {
        String s = "\nsize= " + getLength() + "x" + this.getNoSeries() + "\t"+this.getFrequency() + "\tfrom "+this.getFirstDate()+ "\tto "+this.getLastDate()+ "\tSERIES: ";
        for (int i = 0; i < this.names.size(); i++) {

            s += names.get(i) + "\t";
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat("###0.0000");
        for (int i = 0; i < getLength(); i++) {
            s += "\n" + this.dates.get(i);
            for (int j = 0; j < this.names.size(); j++) {
                s += "\t" + df.format(this.matrix[i][j]);
            }
        }
        return s;
    }

    public void plot(String title, String ylabel) throws Exception {
        Charts c1 = new Charts(title);
        XYPlot p1 = c1.createXYPlot(ylabel, this);
        c1.plot(p1, 640, 480);
    }

    public void toCSV(String filename) throws Exception {
        try (PrintWriter pw = new PrintWriter(new File(filename))) {
            StringBuilder builder = new StringBuilder();
            builder.append("date");
            for (int i=0;i<names.size();i++) { builder.append(";").append(names.get(i));}
            builder.append("\n");
            java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat("YYYY-MM-dd HH.mm.SS");
            for (int i=0;i<matrix.length;i++) {
                builder.append(sdf.format( new Date(dates.get(i).time)));
                for (int j=0;j<names.size();j++) builder.append(";").append(matrix[i][j]);
                builder.append("\n");
            }
            pw.write(builder.toString());
        }
    }
}
