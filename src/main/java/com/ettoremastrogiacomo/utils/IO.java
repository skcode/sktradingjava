/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ettoremastrogiacomo.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.*;
import org.apache.log4j.Logger;

public class IO {

    static Logger logger = Logger.getLogger(IO.class);

    public static byte[] readBinaryFile(String filename) throws IOException {
        java.io.ByteArrayOutputStream bos;
        try (java.io.BufferedInputStream bis = new java.io.BufferedInputStream(new java.io.FileInputStream(filename))) {
            bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 10];//10KB buffer
            int len;
            while ((len = bis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
        }
        bos.close();
        return bos.toByteArray();
    }

    public static void writeBinaryFile(byte[] input, String filename) throws IOException {
        java.io.BufferedOutputStream bos;
        try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(input)) {
            bos = new java.io.BufferedOutputStream(new java.io.FileOutputStream(filename));
            int len;
            byte[] buf = new byte[1024 * 10];//10KB buffer		
            while ((len = bis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
        }
        bos.close();
    }

    public static void zipFiles(String[] filenames, String outFilename) throws IOException {

        byte[] buf = new byte[1024];
        // Compress the files
        try (//	    	java.io.ByteArrayOutputStream bos=new java.io.ByteArrayOutputStream();
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename))) {
            // Compress the files
            for (String filename : filenames) {
                // Add ZIP entry to output stream.
                try (FileInputStream in = new FileInputStream(filename)) {
                    // Add ZIP entry to output stream.
                    out.putNextEntry(new ZipEntry(filename));
                    // Transfer bytes from the file to the ZIP file
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }   // Complete the entry
                    out.closeEntry();
                }
            }
            // Complete the ZIP file
        }
    }

    public static byte[] zipFile(String filename) throws IOException {
        byte[] buf = new byte[1024];
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(bos);

        // Compress the files
//	        for (int i=0; i<filenames.length; i++) {
        FileInputStream in = new FileInputStream(filename);

        // Add ZIP entry to output stream.
        out.putNextEntry(new ZipEntry(filename));

        // Transfer bytes from the file to the ZIP file
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }

        // Complete the entry
        out.closeEntry();
        in.close();

        //        }
        // Complete the ZIP file
        out.close();
        bos.close();
        byte[] ret = bos.toByteArray();
        return ret;
    }

    public static byte[] compressByteArray(byte[] input) throws IOException {
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);
        compressor.setInput(input);
        compressor.finish();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        // Compress the data
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        bos.close();

        // Get the compressed data
        return bos.toByteArray();
    }

    public static byte[] decompressByteArray(byte[] input) {
        // Create the decompressor and give it the data to compress
        Inflater decompressor = new Inflater();
        decompressor.setInput(input);

        // Create an expandable byte array to hold the decompressed data
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        // Decompress the data
        byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            try {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            } catch (DataFormatException e) {
            }
        }
        try {
            bos.close();
        } catch (IOException e) {
        }

        // Get the decompressed data
        return bos.toByteArray();

    }
}
