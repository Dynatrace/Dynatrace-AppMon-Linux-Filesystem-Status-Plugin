package wp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;






import java.util.logging.Logger;
import java.util.logging.Level;

import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.Status;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;

public class LinuxFilesystemMonitor {

	private static final String CONFIG_FILESYSTEM = "Filesystem";
	private static final String CONFIG_SERVER_USERNAME = "serverUsername";
	private static final String CONFIG_SERVER_PASSWORD = "serverPassword";
	private static final String CONFIG_MOUNT_FILTER = "mountFilter";
	private static final String CONFIG_MOUNT_FILTER_STRING = "mountFilterString";
	private static final String METRIC_GROUP = "Linux Filesystem Status Monitor";
	private static final String MSR_PERCENT_USAGE = "PercentUsage";
	private static final String MSR_RO = "ReadOnly Check";
	private static final String CONFIG_MOUNT_FILTER_CHECK = "MountFilterCheck";



	Collection<FileSystem> FS = new ArrayList<FileSystem>();

	private String username = null;
	private String password = null;
	private String Filesystem = null;
	private String host = null;
	private boolean mountTypeFilter = false;
	private String mountTypeFilterString = null;
	private boolean mountTypeFilterCheck = false;
	private static final Logger log = Logger
			.getLogger(LinuxFilesystemMonitor.class.getName());
	

	protected Status setup(MonitorEnvironment env) throws Exception {
		Status result = new Status(Status.StatusCode.Success);
		host = env.getHost().getAddress();
		Filesystem = env.getConfigString(CONFIG_FILESYSTEM);
		username = env.getConfigString(CONFIG_SERVER_USERNAME);
		password = env.getConfigPassword(CONFIG_SERVER_PASSWORD);
		mountTypeFilter = env.getConfigBoolean(CONFIG_MOUNT_FILTER);
		if(mountTypeFilter)
		{
			mountTypeFilterString = env.getConfigString(CONFIG_MOUNT_FILTER_STRING);
		}
		String mountTypeFilterCheckString = env.getConfigString(CONFIG_MOUNT_FILTER_CHECK);
		if(mountTypeFilterCheckString.equals("Include if match"))
		{
			mountTypeFilterCheck = true;
		}
		return result;
	}

	protected Status execute(MonitorEnvironment env) throws Exception {
		Status result = new Status(Status.StatusCode.Success);
		if (Filesystem != null && host != null) {
			if (Filesystem.equals("*") == false) {
				doSingle();

			} else {
				getInstance();

			}
		}
		if(Filesystem != null && Filesystem.equals("*") == false)
		{
			FileSystem[] temp = FS.toArray(new FileSystem[FS.size()]);
			Collection<MonitorMeasure> measures;
			if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_PERCENT_USAGE)) != null) {
				for (MonitorMeasure measure : measures)
					measure.setValue(temp[0].getFreeSpace());
			}
			if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_RO)) != null) {
				for (MonitorMeasure measure : measures)
					measure.setValue(temp[0].getROCheck());
			}
		}
		else if(Filesystem != null && Filesystem.equals("*"))
		{
			for (FileSystem current : FS)
			{
				Collection<MonitorMeasure> measures;
				if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_PERCENT_USAGE)) != null) {
					for (MonitorMeasure measure : measures)
					{
						MonitorMeasure dm = env.createDynamicMeasure(measure, "FileSystem", current.getFSName());
						dm.setValue(current.getFreeSpace());
					}

				}
				if ((measures = env.getMonitorMeasures(METRIC_GROUP, MSR_RO)) != null) {
					for (MonitorMeasure measure : measures)
					{
						MonitorMeasure dm = env.createDynamicMeasure(measure, "FileSystem", current.getFSName());
						dm.setValue(current.getROCheck());
					}

				}
			}
		}
		return result;

	}

	public void getInstance() {
		Connection conn = new Connection(host);
		try {
			Collection<String> mounts = roCheck();
			Collection<String> filterMounts = getFilteredMounts();
			conn.connect();
			String line = "";
			FS = new ArrayList<FileSystem>();
			boolean isAuthenticated = conn.authenticateWithPassword(username,
					password);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");
			Session sess = conn.openSession();
			sess.execCommand("df -Plk |grep dev|awk '{print $1,$5,$6}' | sed \"s/\\%/\"\"/g\"");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					sess.getStdout()));
			String[] temp = null;
			while ((line = br.readLine()) != null) {
				int current = 0;
				temp = line.split("\\s");
				for(String a : mounts)
				{
					if(temp[0].equals(a))
					{
						current = 1;
					}
				}
				boolean filteredMount = false;
				for(String a : filterMounts)
				{
					if(temp[0].equals(a))
					{
						filteredMount = true;
					}
				}
				if(!filteredMount)
				{
					FS.add(new FileSystem(temp[2], temp[1], temp[0], current));
				}
			}
			BufferedReader er = new BufferedReader(new InputStreamReader(
					sess.getStderr()));
			while ((line = er.readLine()) != null) {
				log.severe(line);
			}
			sess.close();
			conn.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, "an exception was thrown", e);
			e.printStackTrace();
			
		} catch (Exception e){
			log.log(Level.SEVERE, "an exception was thrown", e);
			e.printStackTrace();
		}

		return;
	}
	private Collection<String> getFilteredMounts() {
		Collection<String> filterMounts = new ArrayList<String>();
		if(mountTypeFilter)
		{
			String line = "";
			Connection conn = new Connection(host);
			try {
				conn.connect();
				boolean isAuthenticated = conn.authenticateWithPassword(username,
						password);
				if (isAuthenticated == false)
					throw new IOException("Authentication failed.");
				String[] filters = mountTypeFilterString.split("\\r?\\n");
				for(int x=0; x<filters.length; x++)
				{
					Session sess = conn.openSession();
					String filterCommand = "mount | grep \'";
					if(mountTypeFilterCheck)
					{
						filterCommand = "mount | grep -v \'";
					}
					filterCommand += filters[x] + "\' | awk \'{print $1}\'";
					sess.execCommand(filterCommand);
					BufferedReader br = new BufferedReader(new InputStreamReader(sess.getStdout()));
					while ((line = br.readLine()) != null) {
						filterMounts.add(line);
					}
					BufferedReader er = new BufferedReader(new InputStreamReader(
							sess.getStderr()));
					while ((line = er.readLine()) != null) {
						log.severe(line);
					}
					sess.close();
					
				}
				conn.close();
			} catch (IOException e) {
				log.log(Level.SEVERE, "an exception was thrown", e);
				e.printStackTrace();
				
			} catch (Exception e){
				log.log(Level.SEVERE, "an exception was thrown", e);
				e.printStackTrace();
			}
		}
		return filterMounts;
	}

	public void doSingle() {
		Connection conn = new Connection(host);
		try {
			Collection<String> mounts = roCheck();
			conn.connect();
			String line = "";
			FS = new ArrayList<FileSystem>();
			boolean isAuthenticated = conn.authenticateWithPassword(username,
					password);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");
			Session sess = conn.openSession();
			sess.execCommand("df -Plk " + Filesystem +" |grep dev|awk '{print $1,$5,$6}' | sed \"s/\\%/\"\"/g\"");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					sess.getStdout()));
			String[] temp = null;
			while ((line = br.readLine()) != null) {
				int current = 0;
				temp = line.split("\\s");
				for(String a : mounts)
				{
					if(temp[0].equals(a))
					{
						current = 1;
					}
				}
				FS.add(new FileSystem(temp[2], temp[1], temp[0], current));
			}
			BufferedReader er = new BufferedReader(new InputStreamReader(
					sess.getStderr()));
			while ((line = er.readLine()) != null) {
				log.severe(line);
			}
			sess.close();
			conn.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, "an exception was thrown", e);
			e.printStackTrace();
			
		} catch (Exception e){
			log.log(Level.SEVERE, "an exception was thrown", e);
			e.printStackTrace();
		}

		return;
	}
	protected void teardown(MonitorEnvironment env) throws Exception {
	}
	private Collection<String> roCheck() {

		String hostname = host;
		String line = "";
		Collection<String> mounts = new ArrayList<String>();
		try {
			Connection conn = new Connection(hostname);
			conn.connect();
			boolean isAuthenticated = conn.authenticateWithPassword(username,
					password);
			if (isAuthenticated == false)
				throw new IOException("Authentication failed.");
			Session sess = conn.openSession();
			sess.execCommand("mount | grep \'(ro\' | awk \'{print $1}\'") ;
			BufferedReader br = new BufferedReader(new InputStreamReader(sess.getStdout()));

			while ((line = br.readLine()) != null) {
				mounts.add(line);
			}
			BufferedReader er = new BufferedReader(new InputStreamReader(
					sess.getStderr()));
			while ((line = er.readLine()) != null) {
				log.severe(line);
			}
			sess.close();
			conn.close();
		} catch (IOException e) {
			log.log(Level.SEVERE, "an exception was thrown", e);
			e.printStackTrace();
			
		} catch (Exception e){
			log.log(Level.SEVERE, "an exception was thrown", e);
			e.printStackTrace();
		}
		return mounts;
	}
}
