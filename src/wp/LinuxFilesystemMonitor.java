package wp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.dynatrace.diagnostics.pdk.Monitor;
import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.PluginEnvironment;
import com.dynatrace.diagnostics.pdk.Status;
import java.util.logging.Logger;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class LinuxFilesystemMonitor {

	private static final String CONFIG_FILESYSTEM = "Filesystem";
	private static final String CONFIG_SERVER_USERNAME = "serverUsername";
	private static final String CONFIG_SERVER_PASSWORD = "serverPassword";
	private static final String CONFIG_METHOD = "method";
	private static final String CONFIG_AUTH_METHOD="authMethod";
	private static final String CONFIG_PASSPHRASE = "publickeyPassphrase";
	
	private boolean matchRuleSuccess;
	private String CMD = null;
	private String param = null;
	private String server = null;
	private String param2 = null;
	private String param3 = null;
	private String FilesystemName = null; 
	private String param4 = null;
	private int count = 0;
	
	private String username = null;
	private String password = null;
	private String method = null;
	private String authMethod = null;
	private String Filesystem = null;
	private String host = null; 
	
	private static final Logger log = Logger.getLogger(LinuxFilesystemMonitor.class.getName());

	protected Status setup(PluginEnvironment env) throws Exception {
		Status result = new Status(Status.StatusCode.Success);
        String host = env.getHost().getAddress();
		String Filesystem = env.getConfigString(CONFIG_FILESYSTEM);
		username = env.getConfigString(CONFIG_SERVER_USERNAME);
        method = env.getConfigString(CONFIG_METHOD) == null ? "SSH" : env
                .getConfigString(CONFIG_METHOD).toUpperCase();
        authMethod = env.getConfigString(CONFIG_AUTH_METHOD) == null ? "PASSWORD" : env
                .getConfigString(CONFIG_AUTH_METHOD).toUpperCase();
        password = (authMethod.equals("PUBLICKEY")) ? env.getConfigPassword(CONFIG_PASSPHRASE) : env.getConfigPassword(CONFIG_SERVER_PASSWORD);
        
			if (Filesystem != null && host != null) {
				server = host;		
				FilesystemName = Filesystem;

			}
			
		return result;
	}

	protected Status execute(PluginEnvironment env) throws Exception {
		Status result = new Status(Status.StatusCode.Success);

		count = executeCommand(CMD, param, param2, param3, FilesystemName, param4, result);

		if (log.isLoggable(Level.FINE)) log.fine("Command output was: " + count);
		matchRuleSuccess = false;
		
				result.setMessage("Filesystem Usage is: " + count);
				matchRuleSuccess = true;

		return result;
		
	}
	
	protected boolean isMatchRuleSuccess() {
		return matchRuleSuccess;
	}

	protected int returnPercentUsage() {
		return count;
	}
	protected void teardown(PluginEnvironment env) throws Exception {
	}

	private int executeCommand(String CMD, String param, String param2, String param3, String Filesystem, String param4, Status status) {
		
		String[] command = {"df -Plk " + Filesystem + " |grep dev|awk '{print $5}' | sed \"s/\\%/\"\"/g\""};
		
		String hostname = server;
		String inputstream = "";
		String line = "";

		
		try {
			Connection conn = new Connection(hostname);
			conn.connect();
			boolean isAuthenticated = conn.authenticateWithPassword(username, password);
			
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");

			Session sess = conn.openSession();

			sess.execCommand("df -Plk " + Filesystem + " |grep dev|awk '{print $5}' | sed \"s/\\%/\"\"/g\"");

			BufferedReader br = new BufferedReader(new InputStreamReader(sess.getStdout()));

			line = br.readLine();

			sess.close();

			conn.close();
		}
		catch (IOException e){
			e.printStackTrace(System.err);
		}

		line = line.trim();
		return Integer.parseInt(line);



	}
	
}
