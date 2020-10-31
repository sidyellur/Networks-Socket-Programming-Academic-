import java.util.ArrayList;
import java.util.List;

public class ConfigFile {
	private int noOfNeighbors = 0;
	private int unChokingInterval = 0;
	private int optUnChokingInterval = 0;
	private String fileName = "";
	private int fileSize = 0;
	private int chunkSize = 0;
	private int noOfChunks = 0;
	
	private ConfigFile() {}
	
	public static ConfigFile getConfigFileObject(List<String> rows) {
		if(rows != null && rows.size() == 6) {
			ConfigFile configFileObj = new ConfigFile();
			configFileObj.setNoOfNeighbors(Integer.parseInt(rows.get(0).split(" ")[1]));
			configFileObj.setUnChokingInterval(Integer.parseInt(rows.get(1).split(" ")[1]));
			configFileObj.setOptUnChokingInterval(Integer.parseInt(rows.get(2).split(" ")[1]));
			configFileObj.setFileName(rows.get(3).split(" ")[1]);
			configFileObj.setFileSize(Integer.parseInt(rows.get(4).split(" ")[1]));
			configFileObj.setChunkSize(Integer.parseInt(rows.get(5).split(" ")[1]));
			return configFileObj;
		}
		return null;
	}
	
	public int getNoOfNeighbors() {
		return noOfNeighbors;
	}
	
	public void setNoOfNeighbors(int noOfNeighbors) {
		this.noOfNeighbors = noOfNeighbors;
	}
	
	public int getUnChokingInterval() {
		return unChokingInterval;
	}
	
	public void setUnChokingInterval(int unChokingInterval) {
		this.unChokingInterval = unChokingInterval;
	}
	
	public int getOptUnChokingInterval() {
		return optUnChokingInterval;
	}
	
	public void setOptUnChokingInterval(int optUnChokingInterval) {
		this.optUnChokingInterval = optUnChokingInterval;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public int getFileSize() {
		return fileSize;
	}
	
	public void setFileSize(int fileSize) {
		this.fileSize = fileSize;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}
	
	public int getNoOfChunks() {
		return noOfChunks;
	}
	
	public void setNoOfChunks(int noOfChunks) {
		this.noOfChunks = noOfChunks;
	}
  
}
