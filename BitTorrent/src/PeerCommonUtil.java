import java.io.File;
import java.io.IOException;

//Write all the common helper functions here
public class PeerCommonUtil {
      
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
	
}
