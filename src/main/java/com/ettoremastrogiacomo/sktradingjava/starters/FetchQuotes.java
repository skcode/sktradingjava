/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ettoremastrogiacomo.sktradingjava.starters;
import com.ettoremastrogiacomo.sktradingjava.data.Database;
import com.ettoremastrogiacomo.sktradingjava.data.EURONEXT_DataFetch;
import com.ettoremastrogiacomo.sktradingjava.data.FetchData;
import static com.ettoremastrogiacomo.sktradingjava.data.FetchData.loadEODdata;
import com.ettoremastrogiacomo.sktradingjava.data.MLSE_DataFetch;
import com.ettoremastrogiacomo.sktradingjava.data.XETRA_DataFetch;
import com.ettoremastrogiacomo.utils.HttpFetch;
import com.ettoremastrogiacomo.utils.Misc;
import com.ettoremastrogiacomo.utils.UDate;
import java.util.Calendar;


/**
 *
 * @author a241448
 */
public class FetchQuotes {
    static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FetchQuotes.class);
    

    
    public static void main(String[] args)throws Exception {
       if (!Misc.lockInstance(FetchQuotes.class)) throw new Exception("cannot lock instance, instance already running?");
       UDate t1=new UDate();
       LOG.debug("start at "+t1);
        HttpFetch.disableSSLcheck();
        Calendar c1= Calendar.getInstance();
        if (c1.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
            c1.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)        {
            try {Database.deleteSharesTable(); } catch (Exception e){LOG.warn(e);}   
            try {Database.createSecTable();     } catch (Exception e){LOG.warn(e);}               
        }   
       try {loadEODdata(); }    catch (Exception e) {LOG.warn(e);}             
       try {FetchData.fetchNYSESharesDetails();}    catch (Exception e) {LOG.warn(e);}       
       //try {FetchData.fetchIntraday();}catch (Exception e) {LOG.warn(e);}
       try {Database.fetchEODquotesST();}catch (Exception e) {LOG.warn(e);}
       UDate t2=new UDate();
       LOG.debug("end at "+t2);
       double diff=(t2.time-t1.time)/(1000.0*60.0);
       LOG.debug("elapsed min : "+diff);
    }
}
