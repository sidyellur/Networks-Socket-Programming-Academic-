
public class PeerConstants {

	public static final String CONFIG_FILE = "Common.cfg";
	public static final String PEER_FILE = "PeerInfo.cfg";
	public static final String DOWNLOAD_FILE = "TheFile.dat";
	public static final String HANDSHAKE_HEADER = "P2PFILESHARINGPROJ";
	public static final String ZERO_BITS_HANDSHAKE = "0000000000";
	
	public static enum messageType{
		CHOKE(0),UNCHOKE(1),INTERESTED(2),NOT_INTERESTED(3),HAVE(4),BITFIELD(5),REQUEST(6),PIECE(7);
		
		private final int type;
		private static messageType[] messageTypes = values();
		
		private messageType(int msgType) {
			this.type = msgType;
		}
		
		public int getValue() {
			return this.type;
		}
		
		public static messageType valueOf(int mType) {
			for(messageType m : messageTypes) {
				if(m.getValue() == mType) {
					return m;
				}
			}
			return null;
		}
	}
	public static String getPeerFileName()
	{
          return PEER_FILE;
	}

	public static String getZeroBits() 
	{
		return ZERO_BITS_HANDSHAKE; 
	}

	public static String getConfigFileName()
	{
		return CONFIG_FILE; 
	}

	public static String getTorrentFileName()
	{
		return DOWNLOAD_FILE;
	}

	public static String getHeaderHandshake()
	{
		return HANDSHAKE_HEADER;
	}
}

