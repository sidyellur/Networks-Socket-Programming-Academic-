import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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
	private static Map<Integer,NeighbourPeerNode> neighborPeers = null;
	private static int sourcePeerId = -1;
	private static ConcurrentHashMap<Integer,Socket> connectionsEstablished = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<Integer,Integer> bitfield = new ConcurrentHashMap<Integer,Integer>();
	private static int currentPeerIndex = -1;
	private static int totalPeers = -1;
	private static ServerSocket listener = null;
	private static int peersWithEntireFile = 0;
	private static int totalChunks = 0;
	private static int portNumber = 0;
	private static boolean complete_file;

	class NeighborPeerInteraction implements Runnable{
		Socket socket = null;
		NeighbourPeerNode peerNode = null;
		DataInputStream inputStream = null;
		DataOutputStream outputStream = null;
		
		public NeighborPeerInteraction(Socket socket, NeighbourPeerNode peerNode) {
			this.socket = socket;
			this.peerNode = peerNode;
		}
		
		public void run() {
			
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
						System.out.println(receivedPeerId);
						connectionsEstablished.put(peerId, socket);
						NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
						Thread neighborPeerThread = new Thread(npi,"Thread_"+peerId);
						neighborPeerThread.start();
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
				listener = new ServerSocket(portNumber);
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
                    
                    connectionsEstablished.put(peerId, socket);
                    NeighbourPeerNode peerObj = neighborPeers.get(peerId);
                    NeighborPeerInteraction npi = new NeighborPeerInteraction(socket,peerObj);
					Thread neighborPeerThread = new Thread(npi,"Thread_"+peerId);
					
					neighborPeerThread.start();
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
		neighborPeers = new LinkedHashMap<>();
		totalPeers = 0;

		for(String row:peerRows) {
			int peerId = Integer.parseInt(row.split(" ")[0]);
			if(peerId == sourcePeerId) {
				currentPeerIndex = totalPeers;
				portNumber = Integer.parseInt(row.split(" ")[2]);
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
			bitfield.put(i, bit);
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
