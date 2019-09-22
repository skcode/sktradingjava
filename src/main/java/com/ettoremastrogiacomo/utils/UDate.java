/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.utils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author sk
 */
public final class UDate 
   implements Serializable, Comparable<UDate>{
   public final long time;
   
    private static boolean isDateInWeek(UDate date,int week_of_year,int year) {
      Calendar targetCalendar = Calendar.getInstance();
      targetCalendar.setTimeInMillis(date.time);
      int targetWeek = targetCalendar.get(Calendar.WEEK_OF_YEAR);
      int targetYear = targetCalendar.get(Calendar.YEAR);
      return week_of_year == targetWeek && year == targetYear;
    }   
   /**
    * 
    * @param year
    * @param month 0 based month 0=jan
    * @param day
    * @param hour
    * @param min
    * @param sec
    * @return generated Udate 
    */
   public static UDate genDate(int year,int month,int day, int hour, int min,int sec) {
       Calendar c=Calendar.getInstance();       
       c.set(year, month, day, hour, min, sec);
       c.set(Calendar.MILLISECOND, 0);
       return new UDate(c.getTimeInMillis());
   }
   public static UDate roundSecond(UDate date){
       Calendar c=Calendar.getInstance();
       c.setTimeInMillis(date.time);
       c.set(Calendar.MILLISECOND, 0);
       
       return new UDate(c.getTimeInMillis());
   }
   public static UDate roundMinute(UDate date){
       return roundXMinutes(date,1);
   }

   public static UDate roundXMinutes(UDate date,int mins){
       if (mins<0 || mins>59) throw new RuntimeException("bad minutes value "+mins);
       Calendar c=Calendar.getInstance();
       c.setTimeInMillis(date.time);
       int min=c.get(Calendar.MINUTE);
       int diff=min / mins;       
       c.set(Calendar.MILLISECOND, 0);
       c.set(Calendar.SECOND, 0);              
       c.set(Calendar.MINUTE, diff*mins);
       return new UDate(c.getTimeInMillis());
   }


   public static UDate roundHour(UDate date){
       Calendar c=Calendar.getInstance();
       c.setTimeInMillis(date.time);
       c.set(Calendar.MILLISECOND, 0);
       c.set(Calendar.SECOND, 0);              
       c.set(Calendar.MINUTE, 0);              
       return new UDate(c.getTimeInMillis());
   }
   

   

   public static UDate roundDayOfMonth(UDate date){
       Calendar c=Calendar.getInstance();
       c.setTimeInMillis(date.time);
       c.set(Calendar.MILLISECOND, 0);
       c.set(Calendar.SECOND, 0);              
       c.set(Calendar.MINUTE, 0);     
       c.set(Calendar.HOUR_OF_DAY, 0);            
       return new UDate(c.getTimeInMillis());
   }

   public static UDate roundWeek(UDate date){
       Calendar c=Calendar.getInstance();
       c.setTimeInMillis(date.time);       
       c.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);     
       c.set(Calendar.MILLISECOND, 0);
       c.set(Calendar.SECOND, 0);              
       c.set(Calendar.MINUTE, 0);          
       c.set(Calendar.HOUR_OF_DAY, 0); 
       return new UDate(c.getTimeInMillis());
   }   
   public static UDate roundMonth(UDate date){
       Calendar c=Calendar.getInstance();
       c.setTimeInMillis(date.time);
       c.set(Calendar.MILLISECOND, 0);
       c.set(Calendar.SECOND, 0);              
       c.set(Calendar.MINUTE, 0);     
       c.set(Calendar.HOUR_OF_DAY, 0);            
       c.set(Calendar.DAY_OF_MONTH, 0);
       return new UDate(c.getTimeInMillis());
   }
   public static UDate roundYear(UDate date){
       Calendar c=Calendar.getInstance();
       c.setTimeInMillis(date.time);
       c.set(Calendar.MILLISECOND, 0);
       c.set(Calendar.SECOND, 0);              
       c.set(Calendar.MINUTE, 0);     
       c.set(Calendar.HOUR_OF_DAY, 0);            
       c.set(Calendar.DAY_OF_MONTH, 1);
       c.set(Calendar.MONTH, 0);       
       return new UDate(c.getTimeInMillis());
   }

   
   
   public UDate(){
       time=System.currentTimeMillis();
   }
   public UDate(long currtimemills){
       time=currtimemills;
   }
   public java.util.Date getDate() {return new java.util.Date(time);}
   
    public    long getTime() {return time;}
    public boolean after(UDate d) {
       return this.time>d.time;
    }
    public boolean before(UDate d) {
       return this.time<d.time;
    }
    public long diffmills(UDate d) {return this.time-d.time;}
    public double diffdays(UDate d) {return (this.time-d.time-0.0)/(1000.0*60.0*60.0*24.0);}
    @Override
    public int compareTo(UDate o) {
        if (this.time<o.time) return -1;
        if (this.time==o.time) return 0;
        return 1;        
    }  
    public String toYYYYMMDD() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyyMMdd");             
        return sdf.format(this.getDate());        
    }

    public String toDDbMMbYY() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("dd/MM/yy");             
        return sdf.format(this.getDate());        
    }
    
    public int getYear() {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.YEAR);        
    }
    /**
     * 
     * @return month 0 based
     */
    public int getMonth() {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.MONTH);        
    }
    public int getDayofMonth() {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.DAY_OF_MONTH);        
    }
    public int getDayofWeek() {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.DAY_OF_WEEK);        
    }

    public int getHour() {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.HOUR_OF_DAY);        
    }

    public int getMinutes() {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.MINUTE);        
    }
    
    public int getSeconds() {
        Calendar c=Calendar.getInstance();
        c.setTimeInMillis(time);
        return c.get(Calendar.SECOND);        
    }
    
    
    @Override
    public String toString(){
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss.SS");        
        return sdf.format(this.getDate());
    }
    @Override
    public boolean equals(Object other){
        if (other == null) return false;
        if (other == this) return true;
        if (!(other instanceof UDate)) return false;
        UDate o=(UDate) other;
        return (o.time==this.time);
    } 

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (int) (this.time ^ (this.time >>> 32));
        return hash;
    }
}
