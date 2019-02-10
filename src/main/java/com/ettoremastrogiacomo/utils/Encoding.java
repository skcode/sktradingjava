/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.utils;

/**
 *
 * @author a241448
 */

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.apache.log4j.Logger;
 
public class Encoding {
 static Logger logger = Logger.getLogger(Encoding.class);

    public static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
        	int halfbyte = (data[i] >>> 4) & 0x0F;
        	int two_halfs = 0;
        	do {
	            if ((0 <= halfbyte) && (halfbyte <= 9))
	                buf.append((char) ('0' + halfbyte));
	            else
	            	buf.append((char) ('a' + (halfbyte - 10)));
	            halfbyte = data[i] & 0x0F;
        	} while(two_halfs++ < 1);
        }
        return buf.toString();
    }
 
    public static byte[] String2Byte(String input) throws UnsupportedEncodingException {
    	return input.getBytes("UTF-8");
    }

    public static byte[] getSHA1(byte[] input) throws NoSuchAlgorithmException,
    UnsupportedEncodingException{
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        return digest.digest(input);                
    }

    public static byte[] getMD5(byte[] input) throws NoSuchAlgorithmException,
    UnsupportedEncodingException{
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.reset();
        return digest.digest(input);                
    }
    
    public static byte[] getSHA256(byte[] input) throws NoSuchAlgorithmException,
    UnsupportedEncodingException{
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.reset();
        return digest.digest(input);                
    }
 
    public static byte[] getSHA512(byte[] input) throws NoSuchAlgorithmException,
    UnsupportedEncodingException{
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        digest.reset();
        return digest.digest(input);                
    }
    
    public static String base64encode(byte[] input) {
        
    	return Base64.getEncoder().encodeToString(input);
    }

    public static byte[] base64decode(String input) throws  IOException {

    	return Base64.getDecoder().decode(input);
    }

    public static byte[] AES256encode(byte[] input,byte[] key32) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(256); // 192 and 256 bits may not be available
        SecretKeySpec skeySpec = new SecretKeySpec(key32, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        
        return cipher.doFinal(input);        
    } 

    public static byte[] AES256decode(byte[] input,byte[] key32) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(256); // 192 and 256 bits may not be available
        SecretKeySpec skeySpec = new SecretKeySpec(key32, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        return cipher.doFinal(input);        
    } 
    
    
}
