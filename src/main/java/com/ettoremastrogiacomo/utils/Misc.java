/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.channels.FileLock;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
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
    public static <T,V> void map2csv(Map<T,V> map,String filename,Optional<AbstractMap.SimpleEntry<T,V>> head)throws Exception{
        
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(filename))){
            if (head.isPresent()){
                writer.write(head.get().getKey()+";"+head.get().getValue());
                writer.newLine();
            }
            for(Map.Entry<T, V> entry : map.entrySet()){
                writer.write(entry.getKey().toString()+ ";"+entry.getValue().toString());
                writer.newLine();
            }
        }     
    }
    /**
     *
     * @param a
     * @return int value
     * @throws Exception
     */
    public static int booleansToInt(boolean[] a) throws Exception {
        return booleansToInt(a, 0, a.length);
    }

    /**
     *
     * @param a boolean array
     * @param from index included
     * @param to index excluded
     * @return int value
     * @throws Exception
     */
    public static int booleansToInt(boolean[] a, int from, int to) throws Exception {
        if ((to - from) > Integer.SIZE || to <= from || to > a.length || from < 0) {
            throw new Exception("bad range:ì:" + a.length + "\t" + from + "\t" + to);
        }
        int n = 0;
        for (int i = (to - 1); i >= from; --i) {
            n = (n << 1) + (a[i] ? 1 : 0);
        }
        if (n < 0) {
            throw new Exception("negative result");
        }
        return n;
    }

    /**
     *
     * @param a boolean array
     * @param from index included
     * @param to index excluded
     * @return double from [0 to 1] = intValue/maxintValue
     * @throws Exception
     */
    public static double booleansToDoubleNormalized(boolean[] a, int from, int to) throws Exception {
        return (booleansToInt(a, from, to) / (double) ((1 << (to - from)) - 1));
    }

    /**
     *
     * @param v int input
     * @return convert integer to booleans array representing bitstring
     * @throws java.lang.Exception
     */
    public static boolean[] intToBooleans(int v) throws Exception {
        if (v < 0) {
            throw new Exception("negative values not allowed");
        }
        String s = Integer.toBinaryString(v);
        int l = s.length();
        boolean[] b = new boolean[l];
        for (int i = 0; i < l; i++) {
            b[i] = s.charAt(l - i - 1) == '1';
        }
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

    public static double string2double(String s, Locale loc) throws Exception {
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

        Random r = new Random();
        while (intSet.size() < samples) {
            intSet.add(r.nextInt(max));
        }
        //Integer[] d=intSet.toArray(new Integer[intSet.size()]  );        
        return intSet;
    }
    
    /**
     * 
     * @param <E>
     * @param set insieme dal quale prendere n campioni casuali
     * @param samples numero campioni (distinti) da prendere
     * @return
     * @throws Exception 
     */
    public static <E>  Set<E> getRandomElements(Set<? extends E> set,int samples) throws Exception
    {
        Set<Integer> s=getDistinctRandom(samples, set.size()); 
        Iterator<? extends E> iterator = set.iterator(); 
        int currentIndex = 0;
        HashSet<E> res= new HashSet<>();
        while (iterator.hasNext()) {  
            E randomElement = iterator.next();
            if (s.contains(currentIndex))
                res.add(randomElement);
            currentIndex++;
        }  
        return res;
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
        Random r = new Random();
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
    public static List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        final InputStream in
                = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        try (
                //InputStream in = getResourceAsStream( path );
                 BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
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
    public static java.util.HashMap<String, java.util.HashMap<String, String>> list2map(List<java.util.HashMap<String, String>> list, String keycode) throws Exception {
        java.util.HashMap<String, java.util.HashMap<String, String>> map = new java.util.HashMap<>();
        for (java.util.HashMap<String, String> m : list) {
            if (m.get(keycode) == null) {
                throw new Exception("key " + keycode + " not found");
            }
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
    public static String padRight(String s, int n, char padding) {
        if (s.length() >= n) {
            return s;
        }
        int diff = n - s.length();
        StringBuilder builder = new StringBuilder(s.length() + diff);
        builder.append(s);
        for (int i = 0; i < diff; i++) {
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
    public static String padLeft(String s, int n, char padding) {
        if (s.length() >= n) {
            return s;
        }
        int diff = n - s.length();
        StringBuilder builder = new StringBuilder(s.length() + diff);

        for (int i = 0; i < diff; i++) {
            builder.append(padding);
        }
        builder.append(s);
        return builder.toString();
    }

    public static <T> HashSet<T> list2set(List<T> list) {
        return new HashSet<>(list);
    }

    public static <T> ArrayList<T> set2list(Set<T> set) {
        return new ArrayList<>(set);
    }

    /**
     * get fullstacktrace from an exception
     *
     * @param e
     * @return stacktrace in string form
     */
    public static String stackTrace2String(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * divide un set in tanti sottoset il cui gap temporale non supera il
     * maxgapmsec millisecondi
     *
     * @param dates
     * @param maxgapmsec
     * @return array di subset
     */
    public static java.util.ArrayList<TreeSet<UDate>> timesegments(java.util.TreeSet<UDate> dates, long maxgapmsec) {
        //java.util.TreeMap<Integer,TreeSet<UDate> > rank= new java.util.TreeMap< >();
        java.util.TreeSet<UDate> t = new TreeSet<>();
        java.util.ArrayList<TreeSet<UDate>> list = new ArrayList<>();
        //java.util.TreeSet<UDate> last= new TreeSet<>();
        for (UDate d : dates) {
            if (t.isEmpty()) {
                t.add(d);
            } else if (d.diffmills(t.last()) > maxgapmsec) {
                //rank.put(t.size(), t);
                list.add(t);
                //last=t;
                t = new TreeSet<>();
                t.add(d);
            } else {
                t.add(d);
            }
        }
        list.add(t);
        return list;
    }

    public static TreeSet<UDate> mostRecentTimeSegment(java.util.TreeSet<UDate> dates, long maxgapmsec) {

        java.util.TreeSet<UDate> t = new TreeSet<>();
        for (UDate d : dates.descendingSet()) {
            if (t.isEmpty()) {
                t.add(d);
            } else if (Math.abs(d.diffmills(t.first())) > maxgapmsec) {
                break;
            } else {
                t.add(d);
            }
        }

        return t;
    }

    /**
     * prende da un array di set, quello più lungo, non garantisce se ci sono
     * più bestset con stessa lunghezza
     *
     * @param <T>
     * @param list
     * @return
     */
    public static <T> java.util.Set<T> longestSet(ArrayList<TreeSet<T>> list) {
        if (list.isEmpty()) {
            return new java.util.TreeSet<>();
        }
        java.util.TreeSet<T> best = list.get(0);
        for (TreeSet<T> s : list) {
            if (best.size() < s.size()) {
                best = s;
            }
        }
        return best;
    }

    /**
     *
     * @param n
     * @param k
     * @return combinazioni di interi da 0 ad n-1 , presi k a k se k>n ritorna
     * lista vuota
     */
    public static ArrayList<ArrayList<Integer>> combine(int n, int k) {
        ArrayList<ArrayList<Integer>> result = new ArrayList<>();
        if (n < 0 || n < k) {
            return result;
        }
        ArrayList<Integer> item = new ArrayList<>();
        helper_combine(n - 1, k, 0, item, result);
        return result;
    }

    private static void helper_combine(int n, int k, int start, ArrayList<Integer> item,
            ArrayList<ArrayList<Integer>> res) {
        if (item.size() == k) {
            res.add(new ArrayList<>(item));
            return;
        }
        for (int i = start; i <= n; i++) {
            item.add(i);
            helper_combine(n, k, i + 1, item, res);
            item.remove(item.size() - 1);
        }
    }

    public static void writeObjToFile(Object obj, String filename) throws Exception {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(filename)))){
            oos.writeObject(obj);
            oos.flush();
        } 
    }
    public static void writeStringToFile(String  s,String filename) throws Exception{
        try ( FileWriter file = new FileWriter(filename) ){
            file.write(s);
            file.flush();
        }          
    }
    public static Object readObjFromFile(String filename) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(filename)))){
            Object retobj = ois.readObject();
            return retobj;
        }
    }
    public static ArrayList<ArrayList<String>>  CSVreader(String filename,char delimiter,int ntokens) {
        ArrayList<ArrayList<String>> parsed=new ArrayList<>();
        String csvFile = filename;
        String line ;
        String cvsSplitBy = String.valueOf(delimiter);
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(cvsSplitBy);
                if (tokens.length!=ntokens) continue;
                parsed.add(new ArrayList<>(java.util.Arrays.asList(tokens)));
            }
            return parsed;
        } catch (IOException e) {
            logger.warn(e);
        }
        return parsed;
    }
    /**
     * controlla se una stringa è composta di soli spazi o è nulla
     * @param s
     * @return 
     */
    public static boolean isBlank(String s){
         return (s==null || s.trim().equals(""));    
    }
    /**
     * 
     * @param c class  obj e.g. FetchQuotes.class
     * @return true if instance is new, false if instance already running
     */
    public static boolean lockInstance(Class c) {                
        final String lockFile=Misc.getTempDir()+File.separatorChar+"." + c.getProtectionDomain().getCodeSource().getLocation().getPath()
                .replaceAll("[^a-zA-Z0-9_]", "_")     ;
        logger.debug(lockFile);
        try {
            final File file = new File(lockFile);
            final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            final FileLock fileLock = randomAccessFile.getChannel().tryLock();
            if (fileLock != null) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        try {
                            fileLock.release();
                            randomAccessFile.close();
                            file.delete();
                        } catch (IOException e) {
                            logger.error("Unable to remove lock file: " + lockFile, e);
                        }
                    }
                });
                return true;
            }
        } catch (IOException e) {
            logger.error("Unable to create and/or lock file: " + lockFile, e);
        }
        return false;
    }        
    public static TreeMap<UDate,ArrayList<Double>> mapConvert(TreeMap<UDate,Double> m) {
            TreeMap<UDate,ArrayList<Double>> map= new TreeMap<>();
            for (UDate d: m.keySet()) {
                map.put(d, new ArrayList<>(Arrays.asList(m.get(d))));
            }
            return map;
    }
}
