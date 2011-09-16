/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

/*
 * Class with static methods for compression
 */
public class Compression {
    
    /**
     * Compress gzip.
     *
     * @param plainData the plain data
     * @return the byte[]
     */
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

    /**
     * Decompress gzip.
     *
     * @param compressedData the compressed data
     * @return the byte[]
     */
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
    
    /**
     * Compress z.
     *
     * @param plainData the plain data
     * @return the byte[]
     */
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

    /**
     * Decompress z.
     *
     * @param compressedData the compressed data
     * @return the byte[]
     * @throws DataFormatException the data format exception
     */
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
