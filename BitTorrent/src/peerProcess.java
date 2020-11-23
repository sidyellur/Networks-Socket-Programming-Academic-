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
import java.util.concurrent.ConcurrentHashMap;

public class peerProcess {
	private static ReadFiles rfObj = null;
	private static ConfigFile configFileObj = null;
	private static Map<Integer,NeighbourPeerNode> neighborPeers = new LinkedHashMap<>();
	private static int sourcePeerId = -1;
	private static ConcurrentHashMap<Integer,Integer> bitfieldHM = new ConcurrentHashMap<>();
	private static ConcurrentHashMap < Integer, Integer > downloadRate = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer,NeighborPeerInteraction> neighborPeerConnections = new ConcurrentHashMap<>();
	private static int currentPeerIndex = -1;
	private static int totalPeers = -1;
	private static ServerSocket listener = null;
	private static int peersWithEntireFile = 0;
	private static int totalChunks = 0;
	private static int sourcePortNumber = 0;
	private static boolean complete_file;

	class NeighborPeerInteraction {
		Socket socket = null;
		NeighbourPeerNode peerNode = null;
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		boolean unchoked = false;
		byte[] msg = null;

		public NeighborPeerInteraction(Socket socket, NeighbourPeerNode peerNode) throws IOException {
			this.socket = socket;
			this.peerNode = peerNode;
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

		class NeighborPeerInteractionThread implements Runnable{

			public void run() {		
				int peerId = peerNode.getPeerId();
				System.out.println(peerId);
				downloadRate.put(peerId, 0);
				sendBitField();
				boolean flag = true;
				while(flag) {
					try {
						//Read first 4 bytes of the message which is size of the payload
						for(int i = 0;i<4;i++) {
							inputStream.read(msg, i, 1);
						}		

						int size = ByteBuffer.wrap(msg).getInt();
						//System.out.println("size of message = "+size);

						//Read next 1 byte which is the type of message
						inputStream.read(msg,0,1);
						int type = msg[0];
						System.out.println("type of message = " + type);

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
								//send interested as peer has pieces that I don't have
								
							}
							
						}
						flag = false;	
					}catch(IOException e) {
						e.printStackTrace();
					}

				}

			}
		}

	}

	class Choke implements Runnable{

		public void run() {

		}
	}

	class OptimisticUnChoke implements Runnable{

		public void run() {

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

	}

}
