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
import java.util.List;
import java.util.Random;
import java.util.*; 

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


       class GlobalHelperFunc 
  {


	public static byte[] ByteCopyArr(byte[] original, int from, int curr)
	{
		int modlength = curr - from; 
		if (modlength<0) 
		throw new IllegalArgumentException(from + " > " + curr); 
		byte [] new_copy = new byte[modlength]; 
		System.arraycopy(original,from,new_copy, 0, Math.min(original.length - from, modlength)); 
		return new_copy; 
	}

	
	public static boolean missingFileChunk(int[] PeerBit, int[] ConnectedBit, int size) 
	{
		for(int i=0;i< size;i++)
		{
			if(!(PeerBit[i] == 0 && ConnectedBit[i] ==1))                        //DoubleCheck
			{
				return false; 
			}
		}
		return true;
	}

	
	public static int randomFileChunk(int[] PeerBit, int[] ConnectedBit, int size)
	{
		List<Integer> chunklist = new ArrayList<>(); 
		for(int j=0;j<size;j++)
		{
			if(PeerBit[j] == 0 && ConnectedBit[j] ==1)
			{
              chunklist.add(j);
			}
		}

		int chunksize = chunklist.size(); 
	   if(chunksize > 0) 
	   {
		   int res;
		   Random r = new Random(); 
		   int r_index = Math.abs(r.nextInt() % chunksize); 
		   return res = chunklist.get(r_index);                      //Doublecheck
	   }
	   else 
	   {
		   return -1; 
	   }
	}

	
	public static byte[] PacketHandshake(int currIdNode) 
	
	{
		
		String hsHeader = PeerConstants.getHeaderHandshake(); 
		byte[] hspacket = new byte[32];
		byte[] headerbyte = new String(hsHeader).getBytes(); 
		for(int i=0;i<headerbyte.length;i++)
		{
			hspacket[i] = headerbyte[i]; 
		}
		
		byte[] peerIdbyte = ByteBuffer.allocate(4).putInt(currIdNode).array(); 
		for(int i=0;i<peerIdbyte.length; i++)
		{
			hspacket[i] = peerIdbyte[i]; 
		}
		
		int a = PeerConstants.getZeroBits(); 
		String zer = Integer.toString(a); 
		byte[] zerByte = new String(zer).getBytes(); 
		for(int i=0;i<zerByte.length; i++)
		{
			hspacket[i] = zerByte[i]; 
		}

		return hspacket; 

    }

	public static void logContain(BufferedWriter Write, int first_id, int sec_id, int count)
    {
	  Date clock = new Date(); 
	  StringBuffer write_to_log = new StringBuffer(); 
	  DateFormat Format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
	  write_to_log.append(Format.format(clock) + ": The Peer [" + first_id + "] has received the message from [" +sec_id+"] for the peice: " + count + '.' ); 
	  try { 

		String fin = write_to_log.toString();
		Write.write(fin); 
		Write.flush(); 
		Write.newLine(); 
		
	  }catch(Exception e)
	  {}
	}
	  
	 
	public static void downloadLogPeice(BufferedWriter Write, int first_id, int sec_id, int np_peices)
	{ 
	  Date clock = new Date(); 
	  StringBuffer write_to_log = new StringBuffer(); 
	  DateFormat Format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
	  write_to_log.append(Format.format(clock) + ": The Peer [" + first_id + "] has downloaded" + np_peices +  "from [" + sec_id +"]. " + "Number of peices is " + np_peices + '.' );

	  try { 
		String fin = write_to_log.toString();
		Write.write(fin); 
		Write.flush(); 
		Write.newLine();
	  }catch(FileNotFoundException e){

	  }catch(IOException e){

	  }
   }

   public static void checkDownloadComplete(BufferedWriter Write, int first_id)
   { 
	 DateFormat Format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
	 Date clock = new Date(); 
	 StringBuffer write_to_log = new StringBuffer(); 
	 write_to_log.append(Format.format(clock) + "The download of Peer [" + first_id + "] is complete.");
	 try { 

		String fin = write_to_log.toString();
		Write.write(fin); 
		Write.flush(); 
		Write.newLine(); 
		
	  }catch(Exception e)
	  {}
	}


	public static void changeLogNeighbours(BufferedWriter Write, int first_id, int[] listId)
	{ 
		DateFormat Format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		Date clock = new Date();
		StringBuffer write_to_log = new StringBuffer();
		write_to_log.append(Format.format(clock) + "The Peer [" + first_id + "] has the preffered neighbours ["); 

		for(int i=0;i<listId.length; i++)
		{
			write_to_log.append(listId(i));
			if(i<(listId.length-1))
			write_to_log.append(','); 
		}
		try { 

			String fin = write_to_log.toString();
			Write.write(fin); 
			Write.flush(); 
			Write.newLine(); 
			
		  }catch(FileNotFoundException e){

		}catch(Exception e)
		  {}
	}

	public static void logMsgType(BufferedWriter Write, int first_id, int sec_id, String MsgType)
	{
		DateFormat Format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		Date clock = new Date();
		StringBuffer write_to_log = new StringBuffer();

        switch(MsgType){
			case "CHOKE" : 
			write_to_log.append(Format.format(clock)+": Peer [" + first_id  +"] is choked by ["+ sec_id +"].") ;
			break; 
			case "UNCHOKE" : 
			write_to_log.append(Format.format(clock)+": Peer [" + first_id  +"] is unchoked by ["+ sec_id +"].") ;
			break; 
			case "INTERESTED" : 
			write_to_log.append(Format.format(clock)+": : Peer [" + first_id + "] received the 'interested' message from [" + sec_id + "]." ); 
			break; 
			case "NOT_INTERESTED" : 
			write_to_log.append(Format.format(clock)+": : Peer [" + first_id + "] received the ' not interested' message from [" + sec_id + "]." ); 
			break; 

		}
	}


   }
