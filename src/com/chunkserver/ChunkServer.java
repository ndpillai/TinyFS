package com.chunkserver;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Vector;

import com.client.Client;
import com.interfaces.ChunkServerInterface;

/**
 * implementation of interfaces at the chunkserver side
 * @author Shahram Ghandeharizadeh
 *
 */

public class ChunkServer extends Thread implements ChunkServerInterface {
	final static String filePath = "csci485/";	//or C:\\newfile.txt
	public static long counter;
	
	private static ServerSocket serverSocket;
	private static int port = 5656;
	
	private static ObjectOutputStream writeOut;
	private static ObjectInputStream readIn;
	
	public static int PayloadSize = Integer.SIZE/Byte.SIZE;
	public static int CommandSize = Integer.SIZE/Byte.SIZE; //size of each command code
	
	public static final int initCode = 100;
	public static final int putCode = 101;
	public static final int getCode = 102;
	
	/**
	 * Initialize the chunk server
	 */
	public ChunkServer(){
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
		
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			return;
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
	 * Each chunk is corresponding to a file.
	 * Return the chunk handle of the last chunk in the file.
	 */
	public String initializeChunk() {
		counter++;
		return String.valueOf(counter);
	}
	
	/**
	 * Write the byte array to the chunk at the offset
	 * The byte array size should be no greater than 4KB
	 */
	public boolean putChunk(String ChunkHandle, byte[] payload, int offset) {
		try {
			//If the file corresponding to ChunkHandle does not exist then create it before writing into it
			RandomAccessFile raf = new RandomAccessFile(filePath + ChunkHandle, "rw");
			raf.seek(offset);
			raf.write(payload, 0, payload.length);
			raf.close();
			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	/**
	 * read the chunk at the specific offset
	 */
	public byte[] getChunk(String ChunkHandle, int offset, int NumberOfBytes) {
		try {
			//If the file for the chunk does not exist the return null
			boolean exists = (new File(filePath + ChunkHandle)).exists();
			if (exists == false) return null;
			
			//File for the chunk exists then go ahead and read it
			byte[] data = new byte[NumberOfBytes];
			RandomAccessFile raf = new RandomAccessFile(filePath + ChunkHandle, "rw");
			raf.seek(offset);
			raf.read(data, 0, NumberOfBytes);
			raf.close();
			return data;
		} catch (IOException ex){
			ex.printStackTrace();
			return null;
		}
	}
		
	public void run() {
		Socket server = null;
		while(true) {
			try {
				server = serverSocket.accept();			 				
				writeOut = new ObjectOutputStream(server.getOutputStream());
				readIn = new ObjectInputStream(server.getInputStream());
				
				while (true) {
					int payloadlength = Client.readInt(readIn);
					if (payloadlength==-1)
						break;
					
					int code = Client.readInt(readIn); //which operation is being performed
					if (code==initCode) {
						String chunk = initializeChunk();
						byte[] chunkHandlePayload = chunk.getBytes();
						writeOut.writeInt(chunkHandlePayload.length);
						writeOut.write(chunkHandlePayload);
						writeOut.flush();
					}
					else if (code==putCode) {
						int offset = Client.readInt(readIn);
						int payloadsize = Client.readInt(readIn);
						byte[] payload = Client.receivePayload(readIn, payloadsize);
						int chunksize = payloadlength-ChunkServer.PayloadSize-ChunkServer.CommandSize-8-payloadsize;
						byte[] chunkHandlePayload = Client.receivePayload(readIn, chunksize);
						String chunk = new String(chunkHandlePayload);
						
						if (putChunk(chunk, payload, offset))
							writeOut.writeInt(1);
						else
							writeOut.writeInt(0);
						writeOut.flush();
					}
					else if (code==getCode) {
						int offset = Client.readInt(readIn);
						int payloadsize = Client.readInt(readIn);
						int chunksize = payloadlength-ChunkServer.PayloadSize-ChunkServer.CommandSize-8;
						byte[] chunkHandlePayload = Client.receivePayload(readIn, chunksize);
						String chunk = new String(chunkHandlePayload);	
						
						byte[] output = getChunk(chunk, offset, payloadsize);
						if (output==null)
							writeOut.writeInt(ChunkServer.PayloadSize);
						else {
							writeOut.writeInt(ChunkServer.PayloadSize+output.length);
							writeOut.write(output);
						}
						writeOut.flush();
					}
				}
			     
			} catch (Exception e) {
				e.printStackTrace();
				break;
			} finally {
				try {
					if (server!=null)
						server.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static void main(String [] args) {
		Thread t = new ChunkServer();
		t.start();
	}	
}
