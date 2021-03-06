package com.ettoremastrogiacomo.utils;

import com.ettoremastrogiacomo.sktradingjava.Init;
import java.net.*;
import java.io.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.log4j.Logger;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.TrustManager;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 *
 * @author ettore mastrogiacomo
 */
public class HttpFetch {

    boolean useproxy;
    //private String proxy_user,proxy_pass;
    private Proxy proxy;
    static Logger logger = Logger.getLogger(HttpFetch.class);
    List<HttpCookie> cookieList;
    Map<String, List<String>> headers;
    final int TIMEOUT=60000;

    public static String myIP() throws Exception {        
        com.ettoremastrogiacomo.utils.HttpFetch http = new com.ettoremastrogiacomo.utils.HttpFetch();
        if (Init.use_http_proxy.equals("true")) {
            http.setProxy(Init.http_proxy_host, Integer.parseInt(Init.http_proxy_port), Init.http_proxy_type,Init.http_proxy_user, Init.http_proxy_password);
        }
        return new String(http.HttpGetUrl("http://checkip.amazonaws.com", Optional.empty(), Optional.empty()));        
    }
    
    public static void disableSSLcheck() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};
 
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());       
    }
    
    
    public HttpFetch() {
        useproxy = false;
    }
    
    public List<HttpCookie>  getCookies() {
        return Collections.unmodifiableList(cookieList);
    }
    public Map<String,List<String>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
    public void setProxy(String http_proxy, int http_proxy_port, Proxy.Type proxyType,final String user_name, final String password) {
        useproxy = true;        
        proxy = new Proxy(proxyType, new InetSocketAddress(http_proxy, http_proxy_port));
        if (!Misc.isBlank(user_name)){
            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(user_name, password.toCharArray());
                        }
                    });        
        }
    }

    public void unsetProxy() {
        useproxy = false;
    }

    /*public byte[] HttpGetUrl(String s_url) throws Exception {
        DataInputStream di;
        byte[] b = new byte[8 * 1024];
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try {
            URL u = new URL(s_url);
            
            HttpURLConnection con = useproxy ? (HttpURLConnection) u.openConnection(proxy) : (HttpURLConnection) u.openConnection();            
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            headers = con.getHeaderFields();
            cookies = con.getHeaderFields().get("Set-Cookie");            
            //urlConn.setRequestProperty("Cookie", cookie);
            di = new DataInputStream(con.getInputStream());
            int bread;
            while ((bread = di.read(b)) != -1) {
                bos.write(b, 0, bread);
            }
            
            logger.debug("fetched " + s_url + " bytes " + bos.size());
        } catch (IOException e) {
            //logger.error(e);
            throw e;
        }
        return bos.toByteArray();
    }*/
    
    
    public byte[] HttpGetUrl(String s_url,Optional<Integer> retries,Optional<List<HttpCookie>> cookie) throws Exception {
        int retryCount =0;
        int MAX_RETRY_COUNT= retries.orElse(10);
        byte[] b=null;
        while(true)
        {
            try
            {
                 b=cookie.isPresent() ? HttpGetUrlSession(s_url,cookie.get()): HttpGetUrlSession(s_url,new java.util.ArrayList<>());
                break;
            }
            catch(Exception e)
            {
                if(retryCount > MAX_RETRY_COUNT)
                {
                    throw new RuntimeException("Could not fetch '"+s_url+"', max retries="+MAX_RETRY_COUNT, e);
                }

                retryCount++;
                
            }
        }        
        return b;
    }
     byte[] HttpGetUrlSession(String s_url,List<HttpCookie> cookie) throws Exception {
        DataInputStream di = null;
        byte[] b = new byte[8 * 1024];
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
        
        try {
            URL u = new URL(s_url);
            
            HttpURLConnection con = useproxy ? (HttpURLConnection) u.openConnection(proxy) : (HttpURLConnection) u.openConnection();    
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);
            
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");                                    
            //con.setRequestProperty("Content-Type", "application/octet-stream");                        
            for (HttpCookie c : cookie) {
               cookieManager.getCookieStore().add(u.toURI(), c);
            }
            con.getContent();
            int ReturnCode = con.getResponseCode();
            logger.debug("return code "+ReturnCode+" for "+s_url);
            if (ReturnCode!=200) {
                //
                if (ReturnCode==301) {
                    String redirect = con.getHeaderField("Location");//to avoid 301 error redirect
                    if (redirect != null) {
                        u = new URL(redirect);
                        con = useproxy ? (HttpURLConnection) u.openConnection(proxy) : (HttpURLConnection) u.openConnection();
                        //con = new URL(redirect).openConnection();
                    }                    
                    //throw new Exception("Error code 301 for "+s_url+"\nnew location : " +con.getHeaderField("Location"));
                } else
                throw new Exception("bad return code "+ReturnCode+" for "+s_url);
            }
            
            //CookieStore cookieStore = cookieManager.getCookieStore();  
            cookieList = cookieManager.getCookieStore().getCookies();
            
            //cookies = con.getHeaderFields().get("Set-Cookie");
            di = new DataInputStream(con.getInputStream());
            int bread;
            while ((bread = di.read(b)) != -1) {
                bos.write(b, 0, bread);
            }
            
            logger.debug("fetched " + s_url + " bytes " + bos.size());
        } catch (Exception e) {
            logger.error(e);
            throw e;
        }
        return bos.toByteArray();
    }


    
    public byte[] FTPGetUrl(String s_url) throws Exception {
        DataInputStream di ;
        byte[] b = new byte[8 * 1024];
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try {
            URL u = new URL(s_url);
            
            URLConnection con = useproxy ?  u.openConnection(proxy) :  u.openConnection();
            con.setConnectTimeout(TIMEOUT);
            con.setReadTimeout(TIMEOUT);
            di = new DataInputStream(con.getInputStream());
            int bread ;
            while ((bread = di.read(b)) != -1) {
                bos.write(b, 0, bread);
            }
            logger.debug("fetched " + s_url + " bytes " + bos.size());
        } catch (IOException e) {
            logger.error(e);
            throw e;
        }
        return bos.toByteArray();
    }


    /**
     * Makes an HTTP request using POST method to the specified URL.
     *
     * @param requestURL
     *            the URL of the remote server
     * @param params
     *            A map containing POST data in form of key-value pairs
     * @return An HttpURLConnection object
     * @throws IOException
     *             thrown if any I/O error occurred
     */
    public HttpURLConnection sendPostRequest(String requestURL,
            Map<String, String> params) throws IOException {
        URL url = new URL(requestURL);
       
        HttpURLConnection httpConn = useproxy ? (HttpURLConnection) url.openConnection(proxy): (HttpURLConnection) url.openConnection();
        
                    httpConn.setConnectTimeout(TIMEOUT);
            httpConn.setReadTimeout(TIMEOUT);
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            

        httpConn.setUseCaches(false);
 
        httpConn.setDoInput(true); // true indicates the server returns response
 
        StringBuilder requestParams = new StringBuilder();
 
        if (params != null && params.size() > 0) {
 
            httpConn.setDoOutput(true); // true indicates POST request
 
            // creates the params string, encode them using URLEncoder
            Iterator<String> paramIterator = params.keySet().iterator();
            while (paramIterator.hasNext()) {
                String key = paramIterator.next();
                String value = params.get(key);
                requestParams.append(URLEncoder.encode(key, "UTF-8"));
                requestParams.append("=").append(
                        URLEncoder.encode(value, "UTF-8"));
                requestParams.append("&");
            }
 
            // sends POST data
            OutputStreamWriter writer = new OutputStreamWriter(
                    httpConn.getOutputStream());
            writer.write(requestParams.toString());
            writer.flush();
        }
 
        return httpConn;
    }

    /**
     * 
     * @param requestURL url to post json query
     * @param jsonstr json format input string 
     * @return response
     * @throws IOException 
     */
    public String sendjsonPostRequest(String requestURL,
            String jsonstr) throws IOException {
            URL url = new URL(requestURL);       
            HttpURLConnection httpConn = useproxy ? (HttpURLConnection) url.openConnection(proxy): (HttpURLConnection) url.openConnection();       
            httpConn.setConnectTimeout(TIMEOUT);
            httpConn.setReadTimeout(TIMEOUT);
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            httpConn.setRequestProperty("Content-Type", "application/json");            
            httpConn.setRequestMethod("POST");            
            httpConn.setUseCaches(false);            
            httpConn.setDoInput(true); // true indicates the server returns response
            httpConn.setDoOutput(true);
            try(OutputStream os= httpConn.getOutputStream()){
                byte[] input=jsonstr.getBytes("utf-8");
                os.write(input,0,input.length);
            }
            StringBuilder response = new StringBuilder();
            try(BufferedReader br= new BufferedReader(new InputStreamReader(httpConn.getInputStream(),"utf-8"))) {
                
                String responseLine=null;
                while( (responseLine=br.readLine())!=null) {
                    response.append(responseLine.trim());
                }
                logger.debug(response.toString());
            }             
        return response.toString();
    }


    
}
