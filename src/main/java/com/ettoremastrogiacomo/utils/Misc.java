/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.apache.log4j.Logger;


/**
 *
 * @author sk
 */
public class Misc {
    static Logger logger = Logger.getLogger(Misc.class);


/**
 * 
 * @param len length of generated boolean array
 * @return booleans array
 */
    public static boolean[] randomBools(int len) {
        boolean[] arr = new boolean[len];
        java.util.Random random = new java.util.Random(System.currentTimeMillis());// Random();
        for (int i = 0; i < len; i++) {
            arr[i] = random.nextBoolean();
        }
        return arr;
    }
/**
 * 
 * @param a
 * @return int value
 * @throws Exception 
 */
    public static int booleansToInt(boolean[] a) throws Exception{
        return booleansToInt(a, 0, a.length);
    }
/**
 * 
 * @param a boolean array
 * @param from  index included
 * @param to    index excluded
 * @return  int value
 * @throws Exception 
 */
    public static int booleansToInt(boolean[] a, int from, int to)throws Exception {
        if ((to-from)>Integer.SIZE || to<=from || to>a.length || from<0) throw new Exception("bad range:ì:"+a.length+"\t"+from+"\t"+to);
        int n = 0;
        for (int i = (to-1); i >= from; --i) {
            n = (n << 1) + (a[i] ? 1 : 0);
        }
        if (n<0) throw new Exception("negative result");
        return n;
    }
    /**
     * 
     * @param a boolean array
     * @param from  index included
     * @param to    index excluded
     * @return  double from [0 to 1] = intValue/maxintValue
     * @throws Exception 
     */
    public static double booleansToDoubleNormalized(boolean[] a,int from, int to) throws Exception {
        return (booleansToInt(a,from,to)/(double) ( (1<<(to-from)) -1));
    }
    /**
     * 
     * @param v int input
     * @return convert integer to booleans array representing bitstring
     * @throws java.lang.Exception
     */
    public static boolean[] intToBooleans(int v) throws Exception{
        if (v<0) throw new Exception("negative values not allowed");
        String s=Integer.toBinaryString(v);
        int l=s.length();boolean[] b=new boolean[l];
        for (int i=0;i<l;i++) b[i]=s.charAt(l-i-1)=='1';
        return b;
    }


    /**
     * 
     * @param s string of date "M/d/yy - h:m:s a"
     * @return
     * @throws Exception 
     */
    public static Date string2date(String s) throws Exception {
        //assume date format 7/25/14 - 5:34:45 PM                    
        SimpleDateFormat parserSDF = new SimpleDateFormat("M/d/yy - h:m:s a");
        Date result = parserSDF.parse(s);
        return result;
    }

    public static double string2double(String s,Locale loc) throws Exception {
        NumberFormat nf = NumberFormat.getInstance(loc);
        return nf.parse(s).doubleValue();

    }    
    /**
     * 
     * @return system temporary dir diretory , e.g. /tmp
     */
    public static String getTempDir() {
        return System.getProperty("java.io.tmpdir");
    }
/**
 * 
 * @param samples
 * @param max
 * @return array of no. samples from 0 to max - 1
 * @throws Exception 
 */
    public static java.util.Set<Integer> getDistinctRandom(int samples, int max) throws Exception {
        if (max < samples) {
            throw new Exception("bad inputs");
        }
        final java.util.Set<Integer> intSet = new java.util.HashSet<>();
        
        Random r= new Random();        
        while (intSet.size() < samples) {
            intSet.add(r.nextInt(max));            
        }
        //Integer[] d=intSet.toArray(new Integer[intSet.size()]  );        
        return intSet;
    }    
    
    /**
     * 
     * @param samples
     * @param max 
     * @return array of no. samples from 0 to max-1
     * @throws Exception 
     */
    public static java.util.ArrayList<Integer> getRandom(int samples, int max) throws Exception {
        final java.util.ArrayList<Integer> intSet = new java.util.ArrayList<>();      
        Random r= new Random();        
        while (intSet.size() < samples) {
            intSet.add(r.nextInt(max)); 
        }
        return intSet;
    }    
    /**
     * 
     * @param path 
     * @return list of resources in path
     * @throws IOException 
     */
    public static List<String> getResourceFiles( String path ) throws IOException {
    List<String> filenames = new ArrayList<>();
    final InputStream in
      = Thread.currentThread().getContextClassLoader().getResourceAsStream( path );
    try(
      //InputStream in = getResourceAsStream( path );
      BufferedReader br = new BufferedReader( new InputStreamReader( in ) ) ) {
      String resource;

      while( (resource = br.readLine()) != null ) {
        filenames.add( resource );
      }
      br.close();
    }

    return filenames;
  }
    
    /**
     * 
     * @param list
     * @param keycode
     * @return map containing keycode,hashmap
     * @throws Exception 
     */
    public static java.util.HashMap<String,java.util.HashMap<String,String>> list2map(List<java.util.HashMap<String,String>> list,String keycode) throws Exception {
        java.util.HashMap<String,java.util.HashMap<String,String>> map=new java.util.HashMap<>();
        for (java.util.HashMap<String,String> m : list ){
            if (m.get(keycode)==null) throw new Exception("key "+keycode+" not found");
            map.put(m.get(keycode), m);            
        }
        return map;
    }
/**
 * 
 * @param s string to right pad
 * @param n total n of chars
 * @param padding 
 * @return padded string
 */
public static String padRight(String s, int n, char padding){
    if (s.length()>=n) return s;
    int diff=n-s.length();
    StringBuilder builder = new StringBuilder(s.length() + diff);
    builder.append(s);
    for(int i = 0; i < diff; i++){
        builder.append(padding);
    }
    return builder.toString();
}
/**
 * 
 * @param s string to left pad
 * @param n total n of chars
 * @param padding
 * @return padded string
 */
public static String padLeft(String s, int n, char padding){
    if (s.length()>=n) return s;
    int diff=n-s.length();
    StringBuilder builder = new StringBuilder(s.length() + diff);
    
    for(int i = 0; i < diff; i++){
        builder.append(padding);
    }
    builder.append(s);
    return builder.toString();
}
    
}
