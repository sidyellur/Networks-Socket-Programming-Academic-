import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

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
				System.out.println(chunkLength);
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

}
