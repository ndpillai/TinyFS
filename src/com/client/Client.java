package com.client;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import com.chunkserver.ChunkServer;
import com.interfaces.ClientInterface;

/**
 * implementation of interfaces at the client side
 * @author Shahram Ghandeharizadeh
 *
 */
public class Client implements ClientInterface {	
	private static Socket client;
	private static int port = 5656;
	private static String hostname = "localhost";

	private static ObjectOutputStream writeOut;
	private static ObjectInputStream readIn;
	
	/**
	 * Initialize the client
	 */
	public Client(){
		if (client!=null)
			return;
		
		try {
			client = new Socket(hostname, port);	          
			writeOut = new ObjectOutputStream(client.getOutputStream());
			readIn = new ObjectInputStream(client.getInputStream());			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create a chunk at the chunk server from the client side.
	 */
	public String initializeChunk() {
		try {
			writeOut.writeInt(ChunkServer.CommandSize);
			writeOut.writeInt(ChunkServer.initCode);
			writeOut.flush();
						
			int chunkSize = readInt(readIn);
			byte[] chunkHandlePayload = receivePayload(readIn, chunkSize);
			return (new String(chunkHandlePayload));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Write a chunk at the chunk server from the client side.
	 */
	public boolean putChunk(String ChunkHandle, byte[] payload, int offset) {
		if(offset + payload.length > ChunkServer.ChunkSize){
			System.out.println("The chunk write should be within the range of the file, invalide chunk write!");
			return false;
		}
		try {
			byte[] chunkHandlePayload = ChunkHandle.getBytes();
			writeOut.writeInt(ChunkServer.PayloadSize+ChunkServer.CommandSize+4+4+payload.length+chunkHandlePayload.length);
			writeOut.writeInt(ChunkServer.putCode);
			writeOut.writeInt(offset);
			writeOut.writeInt(payload.length);
			writeOut.write(payload);
			writeOut.write(chunkHandlePayload);
			writeOut.flush();
			int output = readInt(readIn);
			if (output==0)
				return false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Read a chunk at the chunk server from the client side.
	 */
	public byte[] getChunk(String ChunkHandle, int offset, int NumberOfBytes) {
		if(NumberOfBytes + offset > ChunkServer.ChunkSize){
			System.out.println("The chunk read should be within the range of the file, invalide chunk read!");
			return null;
		}
		//return cs.getChunk(ChunkHandle, offset, NumberOfBytes);
		try {
			byte[] chunkHandlePayload = ChunkHandle.getBytes();
			writeOut.writeInt(ChunkServer.PayloadSize+ChunkServer.CommandSize+4+4+chunkHandlePayload.length);
			writeOut.writeInt(ChunkServer.getCode);
			writeOut.writeInt(offset);
			writeOut.writeInt(NumberOfBytes);
			writeOut.write(chunkHandlePayload);
			writeOut.flush();
			int chunkSize = readInt(readIn) - ChunkServer.PayloadSize;
			return receivePayload(readIn, chunkSize);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static byte[] receivePayload(ObjectInputStream in, int size) {
		byte[] tmp = new byte[size];
		byte[] buffer = new byte[size];
		int bytes = 0;
		
		while (bytes != size) {
			int counter=-1;
			try {
				counter = in.read(tmp, 0, (size-bytes)); //read in a payload carefully
				for (int j=0; j < counter; j++){
					buffer[bytes+j]=tmp[j];
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			if (counter == -1) {
				return null;
			}
			else { 
				bytes += counter;	
			}
		} 
		return buffer;
	}

	public static int readInt(ObjectInputStream in) { //read one int in
		byte[] buffer = receivePayload(in, 4);
		if (buffer!=null)
			return ByteBuffer.wrap(buffer).getInt();
		return -1;
	}
}
