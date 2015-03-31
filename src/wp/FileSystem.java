package wp;


import java.util.logging.Logger;


public class FileSystem {
	private String FileSystemName = "";
	private int FreeSpace = 0;
	private int rocheck = 0;
	private static final Logger log = Logger
			.getLogger(LinuxFilesystemMonitor.class.getName());
	
	public FileSystem(String name, String space, String fullname, int ro){
		FileSystemName = name;
		//log.info("Start Set");
		rocheck = ro;
		//log.info("roCheck");
		FreeSpace = Integer.parseInt(space);
		//log.info("FreeCheck");
	}
	public int getFreeSpace()
	{
		return FreeSpace;
	}
	public String getFSName()
	{
		return FileSystemName;
	}
	public int getROCheck()
	{
		return rocheck;
	}
}