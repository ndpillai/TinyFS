package com.chunkserver;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Vector;

import com.interfaces.ChunkServerInterface;

/**
 * implementation of interfaces at the chunkserver side
 * 
 * @author Shahram Ghandeharizadeh
 *
 */

public class ChunkServer implements ChunkServerInterface {
	//final static String filePath = "C:\\Users\\shahram\\Documents\\TinyFS-2\\csci485Disk\\"; // or C:\\newfile.txt
	final static String filePath = "csci485Disk/";
	public static long counter;

	/**
	 * Initialize the chunk server
	 */
	public ChunkServer() {
		File[] files = (new File(filePath)).listFiles();
		if (files==null || files.length==0) {
			counter = 0;
		}
		else { //in case of deleted files, we need to make sure a new file is the next sequential one
			Vector<Long> filenames = new Vector<Long>();
			for (File f: files) {
				if (isNumeric(f.getName())) //for hidden files such as .DS_Store on Mac 
					filenames.add(Long.valueOf(f.getName()));
			}
			
			if (filenames.size()==0)
				counter = 0;
			else {
				Collections.sort(filenames);
				counter = filenames.lastElement();
			}
		}
	}
	
	private boolean isNumeric(String s) {  
		try {
			Long l = Long.valueOf(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;		
	}	

	/**
	 * Each chunk corresponds to a file. Return the chunk handle of the last chunk
	 * in the file.
	 */
	public String initializeChunk() {
		counter++;
		return String.valueOf(counter);
	}

	/**
	 * Write the byte array to the chunk at the specified offset The byte array size
	 * should be no greater than 4KB
	 */
	public boolean putChunk(String ChunkHandle, byte[] payload, int offset) {
		try {
			RandomAccessFile raf = new RandomAccessFile(filePath+ChunkHandle, "rw");
			raf.seek(offset);
			raf.write(payload, 0, payload.length);
			raf.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * read the chunk at the specific offset
	 */
	public byte[] getChunk(String ChunkHandle, int offset, int NumberOfBytes) {
		try {
			File f = new File(filePath+ChunkHandle);
			if (!f.exists()) {
				return null;
			}
			
			byte[] data = new byte[NumberOfBytes];
			RandomAccessFile raf = new RandomAccessFile(filePath+ChunkHandle, "rw");
			raf.seek(offset);
			raf.read(data, 0, NumberOfBytes);
			raf.close();
			return data;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
