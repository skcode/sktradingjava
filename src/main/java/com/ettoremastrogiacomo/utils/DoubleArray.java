package com.ettoremastrogiacomo.utils;

import java.util.HashMap;
import org.apache.log4j.Logger;

public class DoubleArray {

    static Logger logger = Logger.getLogger(DoubleArray.class);

    static public double[] linearize(double min, double max, int n) throws Exception {
        if (min >= max || n <= 1) {
            throw new Exception("bad inputs:" + min + " " + max + " " + n);
        }
        double diff = max - min;
        double r = diff / (double) (n - 1);
        double[] ret = new double[n];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = min + r * i;
        }
        return ret;
    }

    /**
    Ritorna il sottovettore specificato dagli indici from e len
    es.
    vector[]= [2 , 4 , 1, 9]
    Sub(Vector,1,2)=[4,1]
    @param v: array di double da quale ricavare il nuovo array
    @param from : indice di partenza
     * @param len
     * @return 
     * @throws java.lang.Exception
     */
    static public double[] sub(double[] v, int from, int len) throws Exception {
        if ((from < 0) || ((from + len) > v.length) || (len < 0)) {
            throw new Exception("bad inputs");
        }
        double[] ret = new double[len];
        System.arraycopy(v, from, ret, 0, len);
        return ret;
    }

    
    
    /**
     * 
     * @param v
     * @return max drow down of a serie, not absolute, (<=0 value)
     * @throws Exception 
     */
    static public double maxDrowDownPerc(double[] v) throws Exception {
        //maxDD calculus
        double mdd=0,mddp=0,maxmdd=v[0];
        for (int i=1;i<v.length;i++) {
            if (v[i]>=v[i-1]) {
                if (v[i]>maxmdd) maxmdd=v[i];
                continue;
            }
            double t1=v[i]-maxmdd;
            double t1p=t1/maxmdd;
            if (t1<mdd) mdd=t1;
            if (t1p<mddp) mddp=t1p;
        }//    
        return mddp;    
    }    
    /**
     * 
     * @param equityval, vector (time series usually)
     * @return map with (slope, intercept,stderr)
     * @throws Exception 
     */
        static public java.util.HashMap<String,Double> LinearRegression(double[] equityval) throws Exception {
        //regression slope 
        java.util.HashMap<String,Double> map=new HashMap<>();
        double meanx=0,meany=0,varx=0,vary=0,covxy=0;
        for (int i=0;i<equityval.length;i++){meany+=equityval[i];meanx+=i;}
        meany/=(double)equityval.length;// portfolio.dates.size();
        meanx/=(double)equityval.length;
        for (int i=0;i<equityval.length;i++)  {varx+=Math.pow(i-meanx,2);vary+=Math.pow(equityval[i]-meany,2);}
        vary/=(double)(equityval.length-1);varx/=(double)(equityval.length-1);
        for (int i=0;i<equityval.length;i++) covxy+=(i-meanx)*(equityval[i]-meany);covxy/=(double)(equityval.length-1);
        double reg_b=covxy/varx;
        double reg_a=meany-reg_b*meanx;
        map.put("slope", reg_b);
        map.put("intercept", reg_a);        
        double stdeverr=0;
        for (int i=0;i<equityval.length;i++) stdeverr+=Math.pow(equityval[i]- (reg_a+reg_b*i),2);        
        stdeverr=Math.sqrt((1.0/(equityval.length-1))*stdeverr);
        map.put("stderr", stdeverr);        
        return map;
    }
    
    
    
    
    static public double min(double[] v) {
        if (v.length < 1) {
            return Double.NaN;
        }
        double ret = v[0];
        for (int i = 1; i < v.length; i++) {
            if (ret > v[i]) {
                ret = v[i];
            }
        }
        return ret;
    }

    static public int minpos(double[] v) {
        if (v.length < 1) {
            return -1;
        }
        double ret = v[0];
        int p=0;
        for (int i = 1; i < v.length; i++) {
            if (ret > v[i]) {
                ret = v[i];p=i;
            }
        }
        return p;
    }

        static public int maxpos(double[] v) {
        if (v.length < 1) {
            return -1;
        }
        double ret = v[0];
        int p=0;
        for (int i = 1; i < v.length; i++) {
            if (ret < v[i]) {
                ret = v[i];p=i;
            }
        }
        return p;
    }

    static public double max(double[] v) {
        if (v.length < 1) {
            return Double.NaN;
        }
        double ret = v[0];
        for (int i = 1; i < v.length; i++) {
            if (ret < v[i]) {
                ret = v[i];
            }
        }
        return ret;
    }

    static public double abs_max(double[] v) {
        if (v.length < 1) {
            return Double.NaN;
        }
        double ret = Math.abs(v[0]);
        for (int i = 1; i < v.length; i++) {
            if (ret < Math.abs(v[i])) {
                ret = Math.abs(v[i]);
            }
        }
        return ret;
    }
    
    
    static public double sum(double[] v) {
        double s = 0;
        for (int i = 0; i < v.length; i++) {
            s += v[i];
        }
        return s;
    }

    static public double sum_abs(double[] v) {
        double s = 0;
        for (int i = 0; i < v.length; i++) {
            s += Math.abs(v[i]);
        }
        return s;
    }

    static public void fill(double[] v, double d) {
        for (int i = 0; i < v.length; i++) {
            v[i] = d;
        }
    }

    static public double[] normalize(double[] v, double min, double max) throws
            Exception {
        if (min >= max) {
            throw new Exception("bad min-max values");
        }
        if (v.length == 0) {
            return new double[0];
        }
        double[] ret = new double[v.length];
        double diff = max - min, vmax = max(v), vmin = min(v);
        double diffv = vmax - vmin;
        if (Math.abs(diffv) <= Float.MIN_VALUE) {
            throw new Exception("min(v)==max(v)");
        }
        double r = diff / diffv;
        for (int i = 0; i < v.length; i++) {
            ret[i] = (v[i] - vmin) * r + min;
        }
        return ret;
    }

    static public double mean(double[] d) {
        if (d.length < 1) {
            logger.warn("0 length vector");
        }
        double m = 0;
        for (int i = 0; i < d.length; i++) {
            m += d[i];
        }
        return (m / d.length);
    }

    static public double cov(double[] d1, double[] d2) throws Exception {
        if (d1.length != d2.length) {
            throw new Exception("lengths doesn't match");
        }
        if (d1.length < 1) {
            logger.warn("0 length vector");
        }
        double m1 = mean(d1), m2 = mean(d2), s = 0;
        for (int i = 0; i < d1.length; i++) {
            s += (d1[i] - m1) * (d2[i] - m2);
        }
        return (1.0 / d1.length) * s;
    }

    static public double corr(double[] d1, double[] d2) throws Exception {
        double s1 = std(d1), s2 = std(d2);
        return cov(d1, d2) / (s1 * s2);
    }

    static public double var(double[] d) {
        return Math.pow(std(d), 2);
    }

    static public double std(double[] d) {
        if (d.length < 2) {
            logger.warn("<2 length vector");
        }
        double m = mean(d), s = 0;
        for (int i = 0; i < d.length; i++) {
            double t = (d[i] - m);
            s += t * t;
        }
        return Math.sqrt((1.0 / (d.length - 1)) * s);
    }


    /*
    Turning Point
    @param v input vector double[]
    @param p min percent to find turning point
     */
    static private int tp_minmax(double[] v, int start, double p, boolean max) {
        int idx = start;
        for (int i = start + 1; i < v.length; i++) {
            if (max) {
                if (v[i] > v[idx]) {
                    idx = i;
                } else if ((v[idx] - v[i]) / v[i] >= p) {
                    return idx;
                }
            } else {
                if (v[i] < v[idx]) {
                    idx = i;
                } else if ((v[i] - v[idx]) / v[idx] >= p) {
                    return idx;
                }
            }
        }
        return -1;
    }

    static public double[] TurningPoint(double[] v, double p) throws Exception {
        if (p > 1 || p < 0) {
            throw new Exception("percent not in [0,1]");
        }
        double lmax = 1, lmin = 0, lmid = .5;
        if (v.length == 0) {
            return new double[0];
        }
        boolean up, onoff;
        int i = tp_minmax(v, 0, p, true), j = tp_minmax(v, 0, p, false);
        if ((i * j) <= 0) {
            up = i > 0;
        } else {
            up = i < j;
        }
        int idx = up ? i : j;
        //log("TurningPoint","i,j"+i+","+j);
        //log("TurningPoint","first "+up+ " " +idx);
        if (idx <= 0) {
            throw new Exception("no turning points founds");
        }
        double[] tmp;
        double[] ret = new double[v.length];
        int k = 0;
        if (up) {
            tmp = normalize(sub(v, k, idx - k + 1), lmid, lmax);
        } else {
            tmp = normalize(sub(v, k, idx - k + 1), lmin, lmid);
        }
        for (int cc = k; cc < tmp.length + k; cc++) {
            ret[cc] = tmp[cc - k];
        }
        onoff = !up;
        while (idx > 0) {
            k = idx;
            if (idx >= v.length) {
                break;
            }
            idx = tp_minmax(v, idx, p, onoff);
            if (idx > 0) {
                tmp = normalize(sub(v, k, idx - k + 1), lmin, lmax);
                for (int cc = k; cc < tmp.length + k; cc++) {
                    ret[cc] = tmp[cc - k];
                }
                onoff = !onoff;
            }
        }
        if (onoff) {
            tmp = normalize(sub(v, k, v.length - k), lmin, lmid);
        } else {
            tmp = normalize(sub(v, k, v.length - k), lmid, lmax);
        }
        for (int cc = k; cc < tmp.length + k; cc++) {
            ret[cc] = tmp[cc - k];
        }

        return ret;
    }

    static public double[] normalizeStd(double[] v) {
        double[] r = new double[v.length];
        double std = std(v);
        double mean = mean(v);
        for (int i = 0; i < v.length; i++) {
            r[i] = (v[i] - mean) / std;
        }
        return r;
    }

    static public double[] denormalizeStd(double[] v, double mean, double std) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = (v[i] * std + mean);
        }
        return r;
    }

    static public double[] sign(double[] v, double val) {
        double[] r = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            r[i] = v[i] < val ? 0 : 1;
        }
        return r;
    }

    static public double[] removeOutliers(double[] v, int noStd) throws Exception {
        double[] r = new double[v.length];
        double std = std(v);
        double mean = mean(v);
        if ((noStd < 1) || (noStd > 3)) {
            throw new Exception("valid values for noStd are in [1..3]");
        }
        for (int i = 0; i < v.length; i++) {
            double sgn = v[i] >= 0 ? 1 : -1;
            r[i] = Math.abs(v[i]) > (mean + noStd * std) ? (mean + sgn * noStd * std) : v[i];

        }
        return r;
    }

    static public double[] TurningPointL(double[] v, double p) throws Exception {
        if (p > 1 || p < 0) {
            throw new Exception("percent not in [0,1]");
        }
        double lmax = 1, lmin = 0, lmid = .5;
        if (v.length == 0) {
            return new double[0];
        }
        boolean up, onoff;
        int i = tp_minmax(v, 0, p, true), j = tp_minmax(v, 0, p, false);
        if ((i * j) <= 0) {
            up = i > 0;
        } else {
            up = i < j;
        }
        int idx = up ? i : j;
        if (idx <= 0) {
            throw new Exception("no turning points founds");
        }
        double[] tmp;
        double[] ret = new double[v.length];
        int k = 0;
        if (up) {
            tmp = linearize(lmid, lmax, idx - k + 1); //Normalize(Sub(v,k,idx),lmid,lmax);
        } else {
            tmp = linearize(lmin, lmid, idx - k + 1); //   Normalize(Sub(v,k,idx),lmin,lmid);
        }
        for (int cc = k; cc < tmp.length + k; cc++) {
            ret[cc] = tmp[cc - k];
        }
        onoff = !up;
        while (idx > 0) {
            k = idx;
            if (idx >= v.length) {
                break;
            }
            idx = tp_minmax(v, idx, p, onoff);
            if (idx > 0) {
                tmp = linearize(lmin, lmax, idx - k + 1); //Normalize(Sub(v,k,idx),lmin,lmax);
                for (int cc = k; cc < tmp.length + k; cc++) {
                    ret[cc] = tmp[cc - k];
                }
                onoff = !onoff;
            }
        }
        if (onoff) {
            tmp = linearize(lmin, lmid, v.length - k); //Normalize(Sub(v,k,v.length-1),lmin,lmid);
        } else {
            tmp = linearize(lmid, lmax, v.length - k); //Normalize(Sub(v,k,v.length-1),lmid,lmax);
        }
        for (int cc = k; cc < tmp.length + k; cc++) {
            ret[cc] = tmp[cc - k];
        }

        return ret;
    }

    static public String toString(double[] m) {
        String ret = "";
        //ret="\nVector dimension:"+m.length+"\n";
        for (int i = 0; i < m.length; i++) {
            ret += m[i] + "\t";

        }
        return ret;
    }

    static public void show(double[] m) throws Exception {
        System.out.println("\nVector dimension:" + m.length);
        for (int i = 0; i < m.length; i++) {
            System.out.printf("%9.4f ", m[i]);
            System.out.println();
        }
    }
}
