import java.util.List;

public class peerProcess {
	private static ReadFiles rfObj = null;
	private static ConfigFile configFileReader = null;
	
	public static void main(String[] args) throws Exception {
		
		//Read Common.cfg and set the ConfigFile object
		rfObj = ReadFiles.getReadFilesObj();
		List<String> rows = rfObj.parseTheFile("Common.cfg");
		configFileReader = ConfigFile.getConfigFileObject(rows);
		System.out.println(configFileReader.getFileName());

	}

}
