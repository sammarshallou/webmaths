package uk.ac.open.lts.webmaths.mathjax;

import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.*;

/**
 * A single instance of the MathJax process.
 * <p>
 * Sorted by start date - the reason for this is so that we can close them
 * starting from the older ones first.
 */
class MathJaxNodeInstance implements Comparable<MathJaxNodeInstance>
{
	private long started;
	private TimeoutReader stdout;
	private OutputStream stdin;
	private Process process;
	private MathJaxNodeExecutable parent;
	private String font;

	private final static Logger LOGGER = Logger.getLogger(MathJaxNodeInstance.class.getName());

	/**
	 * Constructor for unit testing only (does nothing).
	 * @param started Time started
	 * @param font Font
	 */
	protected MathJaxNodeInstance(long started, String font)
	{
		this.started = started;
		this.font = font;
	}

	/**
	 * @param executablePath Path to executable script
	 * @param mathJaxFolder Path to MathJax folder
	 * @param font Font to use
	 * @param parent Parent object
	 * @throws IOException Any error
	 */
	MathJaxNodeInstance(String executablePath, String mathJaxFolder, String font,
		MathJaxNodeExecutable parent) throws IOException
	{
		started = System.currentTimeMillis();
		String[] executableParams = new String[]
		{
			"node",
			executablePath,
			mathJaxFolder,
			font
		};
		process = Runtime.getRuntime().exec(executableParams);
		stdout = new TimeoutReader(process.getInputStream());
		stdin = process.getOutputStream();
		this.font = font;
		this.parent = parent;
	}

	/**
	 * @return The font being used by this instance
	 */
	public String getFont()
	{
		return font;
	}

	synchronized void closeInstance()
	{
		checkNotClosed();
		stdout.requestExit();
		process.destroy();
		process = null;
		stdout.waitForExit();
		stdout = null;
		stdin = null;
	}

	/**
	 * Sends a line of text to the application.
	 * @param text Text to send
	 * @throws IOException Any error
	 */
	synchronized void sendLine(String text) throws IOException
	{
		checkNotClosed();
		stdin.write((text + "\n").getBytes(Charset.forName("UTF-8")));
		parent.log("[SENT] " + text);
	}

	private void checkNotClosed()
	{
		if(process == null)
		{
			throw new IllegalStateException("Already closed: " + this);
		}
	}

	synchronized void flush() throws IOException
	{
		checkNotClosed();
		stdin.flush();
	}

	synchronized String readLine() throws IOException
	{
		checkNotClosed();
		return stdout.getNextLine(MathJaxNodeExecutable.PROCESSING_TIMEOUT);
	}

	synchronized String closeWithStderr() throws IOException
	{
		checkNotClosed();
		TimeoutReader stderr = new TimeoutReader(process.getErrorStream());
		StringBuilder out = new StringBuilder();
		try
		{
			for(int i=0; i<100; i++)
			{
				String line = stderr.getNextLine(1000);
				out.append(line);
				out.append('\n');
			}
		}
		catch(IOException e2)
		{
			parent.log("Exception while reading stderr");
			e2.printStackTrace();
		}
		stderr.requestExit();
		closeInstance();
		stderr.waitForExit();
		return out.toString();
	}

	@Override
	public int compareTo(MathJaxNodeInstance o)
	{
		if(started < o.started)
		{
			return 1;
		}
		else if(started > o.started)
		{
			return -1;
		}
		else
		{
			return 0;
		}
	}
}