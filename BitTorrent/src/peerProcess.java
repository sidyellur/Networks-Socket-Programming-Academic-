import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class peerProcess {
	private static ReadFiles rfObj = null;
	private static ConfigFile configFileObj = null;
	private static Map<Integer,NeighbourPeerNode> neighborPeers = new LinkedHashMap<>();
	private static int sourcePeerId = -1;
	private static ConcurrentHashMap<Integer,Integer> bitfieldHM = new ConcurrentHashMap<>();
	private static ConcurrentHashMap < Integer, Integer > downloadRate = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer,NeighborPeerInteraction> neighborPeerConnections = new ConcurrentHashMap<>();
	private static CopyOnWriteArrayList<Integer> interested_peers = new CopyOnWriteArrayList <> ();
	private static CopyOnWriteArrayList<Integer> unchoked_peers = new CopyOnWriteArrayList <> ();
	private static int currentPeerIndex = -1;
	private static int totalPeers = -1;
	private static ServerSocket listener = null;
	private static int peersWithEntireFile = 0;
	private static int totalChunks = 0; 
	private static int sourcePortNumber = 0;
	private static AtomicInteger optimisticallyUnchokedPeer = new AtomicInteger(-1);
	private static boolean complete_file;
	private static boolean flag = true;

	class NeighborPeerInteraction {
		int peerId = -1;
		Socket socket = null;
		NeighbourPeerNode peerNode = null;
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		boolean unchoked = false;
		byte[] msg = null;

		public NeighborPeerInteraction(Socket socket, NeighbourPeerNode peerNode) throws IOException {
			this.socket = socket;
			this.peerNode = peerNode;
			peerId = peerNode.getPeerId();
			msg = new byte[5];
			inputStream = new DataInputStream(socket.getInputStream());
			outputStream = new DataOutputStream(socket.getOutputStream());		
			NeighborPeerInteractionThread nbit = new NeighborPeerInteractionThread();
			Thread neighborPeerThread = new Thread(nbit,"Thread_"+peerNode.getPeerId());
			neighborPeerThread.start();
		}

		//convert message into bytes that can be sent to other peers
		public synchronized byte[] getMessage(int type,byte[] payload) {
			//message length byte array 
			int payLoadSize = payload != null?payload.length:0;
			int totalLen = 1 + payLoadSize;	
			ByteBuffer bb = ByteBuffer.allocate(4); //allocate 4 bytes to byte buffer
			byte[] messageLength = bb.putInt(totalLen).array();//write the integer value 'totalLen' as 4 bytes

			//message type byte
			byte messageType = (byte)(char)type;

			//concatenate message length byte array, message type byte and payload byte array
			byte[] message = new byte[messageLength.length + totalLen];
			int index = 0;
			for(int i = 0;i<messageLength.length;i++) {
				message[index] = messageLength[i];
				index++;
			}
			message[index] = messageType;
			index++;
			if(payload != null) {
				for(int i=0;i<payload.length;i++) {
					message[index] = payload[i];
					index++;
				}
			}
			return message;
		}

		//convert from int array to byte array
		public synchronized byte[] intArrayTobyteArray(int[] data) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(data.length * 4);        
			IntBuffer intBuffer = byteBuffer.asIntBuffer();
			intBuffer.put(data);
			byte[] byteArray = byteBuffer.array();
			return byteArray;
		}

		//convert byte array to int array
		public synchronized int[] byteArrayTointArray(byte[] bytes) {
			int[] message = new int[bytes.length/4];
			int index = 0;
			for(int i=0;i<bytes.length;i = i + 4) {
				byte[] eachbit = new byte[4];
				System.arraycopy(bytes, i, eachbit, 0, 4);
				message[index] = ByteBuffer.wrap(eachbit).getInt();
				index++;    	
			}
			return message;
		}

		//send bitfield messages
		public void sendBitField(){
			int[] bitfield = new int[totalChunks];
			if(complete_file) {
				Arrays.fill(bitfield, 1);
			}
			else {
				Arrays.fill(bitfield, 0);
			}
			byte[] payload = intArrayTobyteArray(bitfield);
			byte[] message = getMessage(PeerConstants.messageType.BITFIELD.getValue(),payload);
			try {
				outputStream.write(message);
				outputStream.flush();
			}
			catch(IOException ie) {
				ie.printStackTrace();
			}			
		}

		//Don't send choke message if the peer is optimistically unchoked but remove it from unchoked_peers and set unchoked = false
		public synchronized void sendChokeMsg() {
			if(optimisticallyUnchokedPeer.get() != peerId) {
				byte[] message = getMessage(PeerConstants.messageType.CHOKE.getValue(),null);
				try {
					outputStream.write(message);
					outputStream.flush();
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
			if(unchoked) {
				unchoked = false;
				int index = unchoked_peers.indexOf(peerId);
				if(index != -1) {
					unchoked_peers.remove(index);
				}	
			}
		}

		public synchronized void sendUnChokeMsg(boolean isOptimistically) {
			byte[] message = getMessage(PeerConstants.messageType.UNCHOKE.getValue(),null);
			try {
				outputStream.write(message);
				outputStream.flush();
			}catch(IOException e) {
				e.printStackTrace();
			}
			//Set unchoked = true only if it is not optimistically unchoked neighbour
			if(!isOptimistically) {
				unchoked = true;
				unchoked_peers.addIfAbsent(peerId);
			}				
		}

		//check if peer has any interesting pieces that I don't have
		public boolean checkIfPeerHasInterestingPieces() {
			boolean interested = false;
			int[] peer_bitfield = peerNode.getBitfield();
			for(int i =0;i < configFileObj.getNoOfChunks();i++) {
				if(bitfieldHM.get(i) == 0 && peer_bitfield[i] == 1) {
					interested = true;
					break;
				}
			}
			return interested;
		}

		public void sendInterestedorNotMsg() {	
			boolean isInterested = checkIfPeerHasInterestingPieces();
			byte[] message;
			//send interested msg as peer has pieces that I don't have
			message = isInterested ? getMessage(PeerConstants.messageType.INTERESTED.getValue(),null):getMessage(PeerConstants.messageType.NOT_INTERESTED.getValue(),null);		
			try {
				outputStream.write(message);
				outputStream.flush();
			}catch(IOException ie) {
				ie.printStackTrace();
			}	
		}

		//get index of a random chunk that peer has and I don't have. If no such chunk is found return -1
		private int getRandomChunkNeeded() {
			int randomPieceIdx = -1;
			List<Integer> chunksIndicesOfPeer = new ArrayList<>();
			int[] peer_bitfield = peerNode.getBitfield();
			for(int i=0;i<totalChunks;i++) {
				//add index of the chunk that Peer has and I don't have
				if(bitfieldHM.get(i) == 0 && peer_bitfield[i] == 1) { 
					chunksIndicesOfPeer.add(i);
				}
			}
			//select a random chunk that peer has and I don't have
			if(chunksIndicesOfPeer.size() > 0) {
				Random rand = new Random();
				int randomIdx = rand.nextInt(chunksIndicesOfPeer.size());
				randomPieceIdx = chunksIndicesOfPeer.get(randomIdx);
			}

			return randomPieceIdx;
		}

		public void sendRequestMsg() {
			int randomChunkIdx = getRandomChunkNeeded();
			if(randomChunkIdx != -1) {
				ByteBuffer bb = ByteBuffer.allocate(4);
				byte[] payload = bb.putInt(randomChunkIdx).array();
				byte[] message = getMessage(PeerConstants.messageType.REQUEST.getValue(),payload);
				try {
					outputStream.write(message);
					outputStream.flush();
				}catch(IOException ie) {
					ie.printStackTrace();
				}			
			}
		}

		class NeighborPeerInteractionThread implements Runnable{

			public void run() {		
				//System.out.println(peerId);
				downloadRate.put(peerId, 0);
				sendBitField();
				try {
					while(flag) {

						//Read first 4 bytes of the message which is size of the payload
						for(int i = 0;i<4;i++) {
							inputStream.read(msg, i, 1);
						}		

						int size = ByteBuffer.wrap(msg).getInt();
						//System.out.println("size of message = "+size);

						//Read next 1 byte which is the type of message
						inputStream.read(msg,0,1);
						int type = msg[0];
						//System.out.println("type of message = " + type);

						//if type = Bitfield
						if(type == PeerConstants.messageType.BITFIELD.getValue()) {
							byte[] bytes = new byte[size-1];
							for(int i = 0;i<size-1;i++) {
								inputStream.read(bytes, i, 1);
							}
							int[] peer_bitfield = byteArrayTointArray(bytes);
							peerNode.setBitfield(peer_bitfield);
							//if peer has full file, then increase peersWithEntireFile
							boolean hasFullFile = true;
							for(int i=0;i<peer_bitfield.length;i++) {
								if(peer_bitfield[i] == 0) {
									hasFullFile = false;
									break;
								}
							}
							if(hasFullFile) {
								peerNode.setHaveFile(1);
								peersWithEntireFile++;
								sendInterestedorNotMsg();//send interested message since the peer has full file
							}
						}
						else if(type == PeerConstants.messageType.INTERESTED.getValue()) {
							//System.out.println(peerId +" Interested");
							interested_peers.addIfAbsent(peerId);
						}
						else if(type == PeerConstants.messageType.NOT_INTERESTED.getValue()) {
							int index = interested_peers.indexOf(peerId);
							if(index != -1) {
								interested_peers.remove(index);
							}				
							if(peerId == optimisticallyUnchokedPeer.get()) {
								optimisticallyUnchokedPeer.set(-1);
							}
							sendChokeMsg();
						}
						else if(type == PeerConstants.messageType.CHOKE.getValue()) {
							System.out.println(peerId +" has choked me");

						}
						else if(type == PeerConstants.messageType.UNCHOKE.getValue()) {
							//send request message if peer has some interesting piece that I don't have else do nothing
							System.out.println(peerId +" has unchoked me");
							sendRequestMsg(); 
						}
						else if(type == PeerConstants.messageType.REQUEST.getValue()) {
							
						}
					}
					System.out.println(peerId +" Thread finished");
					inputStream.close();
					outputStream.close();
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/* A peerNode with peerId is
	  Only OptimisticallyUnchoked = if optimisticallyUnchokedPeer == peerId 
	  OptimisticallyUnchoked && Unchoked = if (optimisticallyUnchokedPeer == peerId && peerNode.unchoked == true)
	  Only unchoked = if peerNode.unchoked == true
	 */

	class ChokeUnChoke implements Runnable{

		public void run() {

			int unchokeInterval = configFileObj.getUnChokingInterval();
			try {
				while(flag) {

					int interestedPeersSize = interested_peers.size();
					if(interestedPeersSize > 0) {
						int preferredNeighbors = configFileObj.getNoOfNeighbors();
						if(interestedPeersSize < preferredNeighbors) {
							Iterator<Integer> itr = interested_peers.iterator();
							while(itr.hasNext()) {
								int peerId = itr.next();
								NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
								if(!npiObj.unchoked) {//Don't send unchoked if it is already unchoked
									npiObj.sendUnChokeMsg(false);
								}							
							}
						}
						else {
							Random rand = new Random();
							List<Integer> tempPeersList = new ArrayList<>(interested_peers);
							//select preferred Neighbors and unchoke them
							for(int i=0;i<preferredNeighbors;i++) {
								int randomIdx = rand.nextInt(tempPeersList.size());
								int peerId = tempPeersList.get(randomIdx);
								NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
								//Don't send unchoked if it is already unchoked
								if(!npiObj.unchoked) {
									npiObj.sendUnChokeMsg(false);
								}							
								tempPeersList.remove(randomIdx);
							}

							//choke the peers who have not been selected
							Iterator<Integer> itr = tempPeersList.iterator();
							while(itr.hasNext()) {
								int peerId = itr.next();
								NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
								npiObj.sendChokeMsg();
							}
						}
					}
					TimeUnit.SECONDS.sleep(unchokeInterval);
				}
			}catch(InterruptedException ie) {
				ie.printStackTrace();
			}catch(Exception e) {
				e.printStackTrace();
			}
			System.exit(0);
		}
	}

	//Optimistic Unchoke thread 
	class OptimisticUnChoke implements Runnable{

		public void run() {
			try {
				while(flag) {
					if(interested_peers.size() > 0) {
						int optimisticSleepingInterval = configFileObj.getOptUnChokingInterval();
						int interestedPeersSize = interested_peers.size();
						Random rand = new Random();
						int random = rand.nextInt(interestedPeersSize); 
						int peerId = interested_peers.get(random);
						optimisticallyUnchokedPeer.set(peerId);
						NeighborPeerInteraction npiObj = neighborPeerConnections.get(peerId);
						npiObj.sendUnChokeMsg(true);
						TimeUnit.SECONDS.sleep(optimisticSleepingInterval);
						optimisticallyUnchokedPeer.set(-1);

						//choke the peer if it is only optimistically unchoked and not an unchoked peer 
						if(!npiObj.unchoked) {
							npiObj.sendChokeMsg();
						}			
					}
				}
			}catch(InterruptedException ie) {
				ie.printStackTrace();
			}
			System.exit(0);
		}
	}

	//Establishes TCP Connections with all the peers who started before the current peer by exchanging handshake packets with them
	class Client implements Runnable{

		@Override
		public void run() {
			int index = 0;
			Iterator<Entry<Integer, NeighbourPeerNode>> itr = neighborPeers.entrySet().iterator();

			while(index < currentPeerIndex) {
				Entry<Integer, NeighbourPeerNode> entry = itr.next();
				int peerId = entry.getKey();
				NeighbourPeerNode peerObj = entry.getValue();
				String hostName = peerObj.getHostName();
				int portNumber = peerObj.getPortNumber();
				try {	
					//Establish TCP Connection
					Socket socket = new Socket(hostName,portNumber);
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

					//Send handshake
					byte[] handShakeHeader = PeerCommonUtil.getHandshakePacket(sourcePeerId);
					outputStream.write(handShakeHeader);

					//Receive handshake
					byte[] receivedHandshake = new byte[handShakeHeader.length];
					inputStream.readFully(receivedHandshake);
					int receivedPeerId = Integer.parseInt(new String(Arrays.copyOfRange(receivedHandshake,28,32)));

					//Add the socket to connection established 
					if(receivedPeerId == peerId) {		
						//System.out.println(receivedPeerId);
						NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
						neighborPeerConnections.put(peerId, npi);
					}		
					index++;
					outputStream.flush();

				}
				catch(UnknownHostException uhe) {
					uhe.printStackTrace();
				}
				catch(IOException ioe) {
					ioe.printStackTrace();
				}

			}			
		}
	}

	//Establishes TCP Connections with all the peers starting after current peer by waiting for their requests and then exchanging handshake packets with them once it is done.
	class Server implements Runnable{

		@Override
		public void run() {
			int index = currentPeerIndex;
			try {
				listener = new ServerSocket(sourcePortNumber);
				while(index < totalPeers-1) {
					Socket socket = listener.accept();
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

					//Receive handshake
					byte[] handshakePacket = new byte[32];
					inputStream.readFully(handshakePacket);
					int peerId = Integer.parseInt(new String(Arrays.copyOfRange(handshakePacket,28,32)));
					// System.out.println(peerId);

					//send Handshake
					byte[] sendHandshake = PeerCommonUtil.getHandshakePacket(sourcePeerId);
					outputStream.write(sendHandshake);

					NeighbourPeerNode peerObj = neighborPeers.get(peerId);
					NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
					neighborPeerConnections.put(peerId, npi);
					index++;

				}
			}
			catch(UnknownHostException uhe) {
				uhe.printStackTrace();
			}
			catch(IOException ioe) {
				ioe.printStackTrace();
			}

		}
	}

	//set adjacent peer nodes and add to peerMap
	private static void setPeerNodes(List<String> peerRows)throws Exception{
		totalPeers = 0;

		for(String row:peerRows) {
			int peerId = Integer.parseInt(row.split(" ")[0]);
			if(peerId == sourcePeerId) {
				currentPeerIndex = totalPeers;
				sourcePortNumber = Integer.parseInt(row.split(" ")[2]);
				complete_file = Integer.parseInt(row.split(" ")[3]) == 1?true:false;

			}
			else {
				NeighbourPeerNode pnObj = NeighbourPeerNode.getPeerNodeObject(row);	
				neighborPeers.put(peerId,pnObj);
			}
			totalPeers++;
		}

	}

	public static void main(String[] args) throws Exception {

		sourcePeerId = Integer.parseInt(args[0]);
		//Read Common.cfg and set the ConfigFile object
		rfObj = ReadFiles.getReadFilesObj();
		List<String> configRows = rfObj.parseTheFile("Common.cfg");
		configFileObj = ConfigFile.getConfigFileObject(configRows);
		totalChunks = configFileObj.getNoOfChunks();
		//	System.out.println(configFileReader.getNoOfNeighbors());

		//Read PeerInfo.cfg and set the PeerNode.java 
		List<String> peerRows = rfObj.parseTheFile("PeerInfo.cfg");
		setPeerNodes(peerRows);

		//make peer directory
		PeerCommonUtil.makePeerDirectory(sourcePeerId);

		//current peer has file, set bitfield as true for all bits and split the file into chunks
		int bit = 0;
		if(complete_file) {
			peersWithEntireFile++;
			bit = 1;
			PeerCommonUtil.splitFileintoChunks(""+sourcePeerId, configFileObj);
		}

		for(int i = 0;i<totalChunks;i++) {
			bitfieldHM.put(i, bit);
		}

		peerProcess p1 = new peerProcess();
		//start the client and server threads to initialize the TCP Connections with all the other peers
		Client clientObj = p1.new Client();
		Thread client = new Thread(clientObj,"Client Thread");
		client.start();

		Server serverObj = p1.new Server();
		Thread server = new Thread(serverObj,"Server Thread");
		server.start();
		//end

		ChokeUnChoke chokeObj = p1.new ChokeUnChoke();
		Thread chokeThread = new Thread(chokeObj,"Choke thread");
		chokeThread.start();

		OptimisticUnChoke optChokeObj = p1.new OptimisticUnChoke();
		Thread optUnChokeThread = new Thread(optChokeObj,"Optimistic Choke thread");
		optUnChokeThread.start();

		while(flag) {
			//					if(peersWithEntireFile == totalPeers) {
			//						flag = false;
			//					}
			System.out.println("Main Thread");
			TimeUnit.MINUTES.sleep(1);
			System.out.println("Exiting");
			flag = false;
		}

	}

}
