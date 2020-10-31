
public class PeerNode {
	private int peerId = -1;
	private String hostName = "";
	private int portNumber = -1;
	private int haveFile = 0;
	private int[] pieces = null;
	private int noOfChunks = 0;
	
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
	
	public int[] getPieces() {
		return pieces;
	}
	
	public void setPieces(int[] pieces) {
		this.pieces = pieces;
	}
	
	public int getNoOfChunks() {
		return noOfChunks;
	}
	
	public void setNoOfChunks(int noOfChunks) {
		this.noOfChunks = noOfChunks;
	}


}
