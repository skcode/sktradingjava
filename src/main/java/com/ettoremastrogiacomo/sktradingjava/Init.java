/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ettoremastrogiacomo.sktradingjava;
import com.ettoremastrogiacomo.utils.Misc;
import java.io.*;
import java.net.Proxy;
import java.util.*;
/**
 *
 * @author a241448
 */
public class Init {
    public static String db_driver,db_url,db_user,db_password,db_sharestable,db_eoddatatable,db_intradaytable,db_providerstable;
    public static String use_http_proxy,http_proxy_host,http_proxy_port,http_proxy_user,http_proxy_password;
    public static Proxy.Type http_proxy_type;
    //public static String csvtickersfile;
    static  {
            //PropertyConfigurator.configure("src/log4j.properties");
            Properties properties = new Properties();
            
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("projectproperties.properties" );// Init.class.getClass().getResourceAsStream("jsktrading.properties"); 
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));            
            //java.net.URL url=Thread.currentThread().getContextClassLoader().getResource("jsktrading.properties");
            //java.net.URL url=Init.class.getResource("/jsktrading.properties");
            //InputStream stream=Init.class.getClass().getClassLoader().getResourceAsStream("/jsktrading.properties");
            try { 
                properties.load(reader);
                //properties.load(new FileInputStream(url.getFile()));
                db_driver=properties.getProperty("jsktrading.db.driver");
                db_url=properties.getProperty("jsktrading.db.url");
                db_user=properties.getProperty("jsktrading.db.user");
                db_password=properties.getProperty("jsktrading.db.password");
                use_http_proxy=properties.getProperty("jsktrading.use_http_proxy");
                http_proxy_host=properties.getProperty("jsktrading.http_proxy_host");
                http_proxy_port=properties.getProperty("jsktrading.http_proxy_port");
                http_proxy_user=properties.getProperty("jsktrading.http_proxy_user");
                http_proxy_password=properties.getProperty("jsktrading.http_proxy_password");
                //csvtickersfile= properties.getProperty("jsktrading.db.csvtickersfile");
                http_proxy_type= Misc.isBlank(properties.getProperty("jsktrading.http_proxy_type"))?Proxy.Type.HTTP:Proxy.Type.valueOf(properties.getProperty("jsktrading.http_proxy_type")) ;
                db_sharestable= properties.getProperty("jsktrading.db.sharestable");
                db_eoddatatable= properties.getProperty("jsktrading.db.eoddatatable");
                db_intradaytable=properties.getProperty("jsktrading.db.intradaytable");                
                db_providerstable=properties.getProperty("jsktrading.db.providerstable");                
         
            }
            catch (Exception e) {e.printStackTrace(); }
            finally{
                try{reader.close();} catch(IOException e){}
            }
    }

}
