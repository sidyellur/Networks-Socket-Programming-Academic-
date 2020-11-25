import java.io.File;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

//Write all the common helper functions here
public class PeerCommonUtil {

	//Create peer directory if not created
	public static void makePeerDirectory(int peerId) {
		try {
			File peerDir = new File("peer_"+peerId);
			if(!peerDir.exists()) {
				peerDir.mkdir();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	//split the whole file into chunks and write into the peer directory as chunks
	public static void splitFileintoChunks(String peerId, ConfigFile configFileObj) {
		int noOfChunks = configFileObj.getNoOfChunks();
		int chunkSize = configFileObj.getChunkSize();
		int fileSize = configFileObj.getFileSize();
		String sourceFile = System.getProperty("user.dir")+"\\"+PeerConstants.DOWNLOAD_FILE;

		try {		
			FileInputStream fis = new FileInputStream(sourceFile);
			File[] chunkFiles = new File[chunkSize];
			int i = 0,chunkstobeCopied = 0;
			while(i < noOfChunks) {
				chunkstobeCopied = chunkstobeCopied + chunkSize;
				int chunkLength = chunkSize;
				//Last chunk will have to be fileSize - sizeofchunksCopiedtillnow as it will be of different size and not the same size as chunk size.
				if(fileSize < chunkstobeCopied) {
					chunkstobeCopied = chunkstobeCopied - chunkSize;
					chunkLength = fileSize - chunkstobeCopied;			
				}
				byte[] copy = new byte[chunkLength];
				String fileName = "peer_"+peerId+"/"+"TheFile_"+i+".dat";
				chunkFiles[i] = new File(fileName);
				FileOutputStream fos = new FileOutputStream(fileName);
				fis.read(copy);
				fos.write(copy);
				fos.close();
			//	System.out.println(chunkLength);
				i++;
			}
			fis.close();
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized static byte[] getHandshakePacket(int sourcePeerId) 
	{
		String hsHeader = PeerConstants.HANDSHAKE_HEADER;
		byte[] headerBytes = hsHeader.getBytes();
		String zeroes = PeerConstants.ZERO_BITS_HANDSHAKE;
		byte[] zeroBytes = zeroes.getBytes();
		byte[] peerIdBytes = String.valueOf(sourcePeerId).getBytes();
		int packetLen = headerBytes.length+zeroBytes.length+peerIdBytes.length;
		byte[] hspacket = new byte[packetLen];
		for(int i = 0 ;i<packetLen;i++) {
			if(i < headerBytes.length) {
				hspacket[i] = headerBytes[i];
			}
			else if(i < headerBytes.length + zeroBytes.length){
				hspacket[i] = zeroBytes[i-headerBytes.length];
			}
			else {
				hspacket[i] = peerIdBytes[i - headerBytes.length - zeroBytes.length];
			}
		}
	
		return hspacket; 

	}
}
