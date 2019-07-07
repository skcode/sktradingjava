/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ettoremastrogiacomo.sktradingjava.starters;
import com.ettoremastrogiacomo.sktradingjava.Init;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.sktradingjava.data.FetchData;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Calendar;


/**
 *
 * @author a241448
 */
public class FetchQuotes {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FetchQuotes.class);
    public static void main(String[] args)throws Exception {
       String dbname=(Init.db_url.split(":"))[2];
       java.io.File f=new java.io.File(dbname);
       UDate t1=new UDate();
       LOG.debug("start at "+t1);
       Database.createSecTable();       
       try {FetchData.fetchSharesDetails();}    catch (Exception e) {LOG.warn(e);}
       try {FetchData.fetchIntraday();}catch (Exception e) {LOG.warn(e);}
       try {Database.fetchEODquotesST();}catch (Exception e) {LOG.warn(e);}
       UDate t2=new UDate();
       LOG.debug("end at "+t2);
       double diff=(t2.time-t1.time)/(1000.0*60.0);
       LOG.debug("elapsed min : "+diff);
       /*
       Database.fetchSecInfo();
       
       Database.fetchIntradayQuotes();
       //System.out.println(new java.util.Date());
       Calendar c=  Calendar.getInstance();
       if (c.get(Calendar.DAY_OF_WEEK)==Calendar.SATURDAY){//fetch one time a week
           Database.fetchQuotes(); 
       }*/
       
       //System.out.println(new java.util.Date());
    }
}
