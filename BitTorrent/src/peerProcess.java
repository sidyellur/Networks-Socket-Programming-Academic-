import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class peerProcess {
	private static ReadFiles rfObj = null;
	private static ConfigFile configFileObj = null;
	private static Map<Integer,PeerNode> allPeersLHMap = null;
	private static int sourcePeerId = -1;
	private static PeerNode currentPeer = null;
	private static ConcurrentHashMap<Integer,Socket> connectionsEstablished = new ConcurrentHashMap<>();
	private static int currentPeerIndex = -1;
	private static int totalPeers = -1;
	private static ServerSocket listener = null;
	private static int peersWithEntireFile = 0;
	private static int totalChunks = 0;

	//Establishes TCP Connections with all the peers who started before the current peer by exchanging handshake packets with them
	static class Client implements Runnable{
		
		@Override
		public void run() {
			int index = 0;
			Iterator<Entry<Integer, PeerNode>> itr = allPeersLHMap.entrySet().iterator();
			
			while(index < currentPeerIndex) {
				Entry<Integer, PeerNode> entry = itr.next();
				int peerId = entry.getKey();
				PeerNode peerObj = entry.getValue();
				String hostName = peerObj.getHostName();
				int portNumber = peerObj.getPortNumber();
				try {	
					//Establish TCP Connection
					Socket socket = new Socket(hostName,portNumber);
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

					//Send handshake
					byte[] handShakeHeader = String.valueOf(sourcePeerId).getBytes();
					outputStream.write(handShakeHeader);
					
					//Receive handshake
					byte[] repliedHandshake = new byte[handShakeHeader.length];
					inputStream.readFully(repliedHandshake);
					//ByteBuffer repliedHandshakeBB = ByteBuffer.wrap(repliedHandshake);
					int serverPeerId = Integer.parseInt(new String(repliedHandshake));
					
					//Add the socket to connection established 
					if(serverPeerId == peerId) {
						System.out.println(serverPeerId);
						connectionsEstablished.put(peerId, socket);
					}		
					index++;
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
	static class Server implements Runnable{

		@Override
		public void run() {
			int index = currentPeerIndex;
			int sourcePortNumber = currentPeer.getPortNumber();
			try {
				listener = new ServerSocket(sourcePortNumber);
				while(index < totalPeers-1) {
					Socket socket = listener.accept();
					DataInputStream inputStream = new DataInputStream(socket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
					
					//Receive handshake
                    byte[] handshakePacket = new byte[String.valueOf(sourcePeerId).getBytes().length];
                    inputStream.readFully(handshakePacket);
                  //  ByteBuffer handshakeBB = ByteBuffer.wrap(handshakePacket);
                    int peerId = Integer.parseInt(new String(handshakePacket));
                    System.out.println(peerId);
                    
                    //send Handshake
                    byte[] sendHandshake = String.valueOf(sourcePeerId).getBytes();
                    outputStream.write(sendHandshake);
                    
                    connectionsEstablished.put(peerId, socket);
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

	//set peer nodes and add to peerMap
	private static void setPeerNodes(List<String> peerRows)throws Exception{
		allPeersLHMap = new LinkedHashMap<>();
		totalPeers = 0;

		for(String row:peerRows) {
			PeerNode pnObj = PeerNode.getPeerNodeObject(row);
			int peerId = pnObj.getPeerId();
			if(peerId == sourcePeerId) {
				currentPeerIndex = totalPeers;
			}
			//	System.out.println(peerId);		
			allPeersLHMap.put(peerId,pnObj);
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
		currentPeer = allPeersLHMap.get(sourcePeerId);
		
		//make peer directory
		PeerCommonUtil.makePeerDirectory(sourcePeerId);
		
		//current peer has file
		if(currentPeer.getHaveFile() == 1) {
			peersWithEntireFile++;
			currentPeer.setNoOfChunks(totalChunks);
			currentPeer.updateBitfield(true);
			PeerCommonUtil.splitFileintoChunks(""+sourcePeerId, configFileObj);
		}
		else {
			currentPeer.updateBitfield(false);
		}
		
		//start the client and server threads to initialize the TCP Connections with all the other peers
		Client clientObj = new Client();
		Thread client = new Thread(clientObj,"Client Thread");
		client.start();
		
		Server serverObj = new Server();
		Thread server = new Thread(serverObj,"Server Thread");
		server.start();
		//end
		
	}

}
