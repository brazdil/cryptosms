package uk.ac.cam.db538.cryptosms.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class Compression {
    public static byte[] compressGzip(byte[] plainData){
    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    	try {
    		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
    		gzipOutputStream.write(plainData);
    		gzipOutputStream.close();
    	} catch(IOException e){
        	throw new RuntimeException(e);
    	}
        return byteArrayOutputStream.toByteArray();
    }

    public static byte[] decompressGzip(byte[] compressedData){
    	ByteArrayOutputStream out = new ByteArrayOutputStream();
    	byte[] buf = new byte[1024];
    	int len;
    	try {
    		GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(compressedData));
    		while ((len = gzipInputStream.read(buf)) > 0) {
    			out.write(buf, 0, len);
    		}
    	} catch(IOException e){
    		throw new RuntimeException(e);
    	}
    	return out.toByteArray();
	}
    
    public static byte[] compressZ(byte[] plainData) {
    	byte[] buffer = new byte[1024];
    	int bytesCompressed;
    	Deflater deflater = new Deflater();
    	ByteArrayOutputStream bos = new ByteArrayOutputStream(plainData.length);
    	
    	deflater.setInput(plainData);
    	deflater.finish();
    	while(!deflater.finished()) {
    		bytesCompressed = deflater.deflate(buffer);
    		bos.write(buffer, 0, bytesCompressed);
    	}
    	return bos.toByteArray();	
    }

    public static byte[] decompressZ(byte[] compressedData) throws DataFormatException {
    	byte[] buffer = new byte[1024];
    	int bytesDecompressed;
    	Inflater inflater = new Inflater();
    	ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
    	
    	inflater.setInput(compressedData);
    	while(!inflater.finished()) {
    		bytesDecompressed = inflater.inflate(buffer);
    		bos.write(buffer, 0, bytesDecompressed);
    	}
    	return bos.toByteArray();
    }
}
