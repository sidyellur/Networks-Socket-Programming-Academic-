import java.util.concurrent.ConcurrentHashMap;

public class PeerNode {
	private int peerId = -1;
	private String hostName = "";
	private int portNumber = -1;
	private int haveFile = 0;
	private ConcurrentHashMap<Integer,Integer> bitfield = new ConcurrentHashMap<Integer,Integer>();
	private int noOfChunks = 0;
	
	private PeerNode() {}
	
	private PeerNode(int peerId, String hostName, int portNumber, int haveFile) {
		this.setPeerId(peerId);
		this.setHostName(hostName);
		this.setPortNumber(portNumber);
		this.setHaveFile(haveFile);
	}
	
	public static PeerNode getPeerNodeObject(String row) {		
		String[] parameters = row.split(" ");
		int peerId = Integer.parseInt(parameters[0]);
		String hostName = parameters[1];
		int portNumber = Integer.parseInt(parameters[2]);
		int haveFile = Integer.parseInt(parameters[3]);
		PeerNode pn = new PeerNode(peerId,hostName,portNumber,haveFile);
		return pn;
	}
	
	public int getPeerId() {
		return peerId;
	}
	
	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}
	
	public String getHostName() {
		return hostName;
	}
	
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public int getPortNumber() {
		return portNumber;
	}
	
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}
	
	public int getHaveFile() {
		return haveFile;
	}
	
	public void setHaveFile(int haveFile) {
		this.haveFile = haveFile;
	}
	
	public ConcurrentHashMap<Integer, Integer> getBitfield() {
		return bitfield;
	}

	public void setBitfield(ConcurrentHashMap<Integer, Integer> bitfield) {
		this.bitfield = bitfield;
	}
	
	public void updateBitfield(boolean haveFile) {
		int bit = haveFile?1:0;
		for(int i=0;i<noOfChunks;i++) {
			bitfield.put(i, bit);
		}
	}
	
	public void updateBitfield(int indexOfChunkReceived) {
		bitfield.put(indexOfChunkReceived, 1);
	}
	
	public int getNoOfChunks() {
		return noOfChunks;
	}
	
	public void setNoOfChunks(int noOfChunks) {
		this.noOfChunks = noOfChunks;
	}

}
