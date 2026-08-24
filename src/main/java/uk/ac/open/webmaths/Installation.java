package uk.ac.open.webmaths;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages the installation of the Node-based application.
 */
public class Installation {
	private final static Logger LOGGER = Logger.getLogger(Installation.class.getName());

	private final static Pattern REGEX_PACKAGEVERSION = Pattern.compile(
			"(?:^|\n)\\s*\"version\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*(?:\n|$)");	
	
	/** If true, currently installing the app */
	private static boolean installing = false;
	
	/** If installed or not (null = haven't checked yet) */
	private static Boolean isInstalled = null;
	
	public enum InstallStatus
	{
		INSTALLED,
		INSTALLING,
		FAILED		
	};
		
	/**
	 * Gets the install location for the app.
	 * @return Install location
	 */
	protected static File getInstallLocation()
	{
		return new File(System.getProperty("user.home"), "ou-mathjax");		
	}
	
	/**
	 * Gets the path to the app executable .mjs file.
	 * @return Path to .mjs
	 */
	public static File getExecutablePath()
	{
		return new File(getInstallLocation(), "ou-mathjax.mjs");
		
	}
	
	/**
	 * Gets the expected version for the ou-mathjax app.
	 * @return Version string
	 * @throws IOException Any error reading package.json
	 */
	protected static String getExpectedVersion() throws IOException
	{
		String packageJson = Util.loadFromClasspath("/ou-mathjax/package.json");
		Matcher matcher = REGEX_PACKAGEVERSION.matcher(packageJson);
		if (matcher.find())
		{
			return matcher.group(1);
		}
		throw new IOException("Unable to parse version from package.json");	
	}
	
	/**
	 * Gets the version for the MathJax dependency that is actually installed.
	 * (Only works when app is installed.)
	 * @return Version string
	 * @throws IOException Any error reading package.json
	 */
	protected static String getInstalledMathJaxVersion() throws IOException
	{
		File mathJaxPackage = new File(getInstallLocation(), "node_modules/@mathjax/src/package.json");
		String packageJson = Util.loadFile(mathJaxPackage);
		Matcher matcher = REGEX_PACKAGEVERSION.matcher(packageJson);
		if (matcher.find())
		{
			return matcher.group(1);
		}
		throw new IOException("Unable to parse version from @mathjax/src/package.json");	
	}
	
	/**
	 * Gets the file used to mark the current version complete.
	 * @return File location
	 * @throws IOException Any error reading package.json
	 */
	protected static File getInstallCompleteFile() throws IOException
	{
		String version = getExpectedVersion();
		return new File(getInstallLocation(), version + ".installcomplete");		
	}

	/**
	 * Checks if the current app version is installed.
	 * @return True if installed
	 * @throws IOException
	 */
	public synchronized static boolean isInstalled() throws IOException
	{
		if (isInstalled == null)
		{
			isInstalled = getInstallCompleteFile().exists();
		}
		return isInstalled;
	}
	
	/**
	 * Gets installation status.
	 * @return Current status
	 * @throws IOException
	 */
	public synchronized static InstallStatus getInstallStatus() throws IOException
	{
		if (installing)
		{
			return InstallStatus.INSTALLING;			
		}
		else if (isInstalled())
		{
			return InstallStatus.INSTALLED;	
		}
		else
		{
			return InstallStatus.FAILED;
		}
	}
	
	/**
	 * Fakes installation status for unit tests.
	 * @param status New desired status
	 */
	public synchronized static void fakeInstallStatus(InstallStatus status)
	{
		switch(status)
		{
		case INSTALLING:
			installing = true;
			break;
		case INSTALLED:
			installing = false;
			isInstalled = true;
			break;
		case FAILED:
			installing = false;
			isInstalled = false;
		}
	}
	
	/**
	 * Install new app version.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected static void install() throws IOException, InterruptedException
	{
		LOGGER.log(Level.INFO, "Installing new app...");

		// Create folder if necessary.
		File root = getInstallLocation();
		if (!root.exists())
		{
			LOGGER.log(Level.INFO, "Creating folder.");
			if (!root.mkdir())
			{
				throw new IOException("Failed to create directory: " + root);
			}
		}
		
		// Copy package.json and ou-mathjax.mjs.
		LOGGER.log(Level.INFO, "Installing files.");
		installFile("package.json");
		installFile("ou-mathjax.mjs");
		
		// Run the install
		npmInstall();
		
		// Mark installation as finished.
		new FileOutputStream(getInstallCompleteFile()).close();
		LOGGER.log(Level.INFO, "Installation complete...");
		synchronized(Installation.class)
		{
			isInstalled = null;
		}	
	}
	
	/** 
	 * Installs a single file from the application.
	 * @param filename Filename
	 * @throws IOException
	 */
	protected static void installFile(String filename) throws IOException
	{
		InputStream in = Installation.class.getResourceAsStream("/ou-mathjax/"+filename);
		OutputStream out = new FileOutputStream(new File(getInstallLocation(), filename));
		Util.copy(in, out);		
	}
	
	/**
	 * Runs npm install.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	protected static void npmInstall() throws IOException, InterruptedException
	{
		LOGGER.log(Level.INFO, "Running npm install...");
		LOGGER.log(Level.INFO, "Path: " + System.getenv("PATH"));
		String[] commands;
		if (System.getProperty("os.name").startsWith("Windows"))
		{
			commands = new String[] {"powershell", "-command", "npm install"};
		}
		else
		{
			commands = new String[] {"npm", "install"};
		}
		LOGGER.log(Level.INFO, "Command: [" + String.join("] [", commands) + "]");
				
		Process p = Runtime.getRuntime().exec(commands, null, getInstallLocation());
		int result;
		result = p.waitFor();
		String output = Util.loadFromReader(p.inputReader());
		LOGGER.log(Level.INFO, "npm install output:\n" + output);
		String errors = Util.loadFromReader(p.errorReader());
		LOGGER.log(Level.INFO, "npm install errors:\n" + errors);
		if (result != 0)
		{
			throw new IOException("npm install failed (exit code: " + result + ")");
		}		
	}
	
	/**
	 * If a new installation is required, start it in a separate thread.
	 */
	public static void startInstallIfNecessary() throws IOException
	{
		synchronized(Installation.class)
		{
			if (isInstalled())
			{
				return;
			}
			if (installing)
			{
				return;
			}
			installing = true;					
		}
		(new Thread() {
			@Override
			public void run() {
				try
				{
					install();
				}
				catch(Exception e)
				{
					LOGGER.log(Level.SEVERE, "Unable to install ou-mathjax", e);									
				}			
				finally
				{
					synchronized(Installation.class)
					{
						installing = false;
					}
				}
			}
		}).start();
	}

}
