package uk.ac.open.lts.webmaths.mathjax;

import java.io.*;
import java.nio.charset.Charset;
import java.util.logging.*;

/**
 * A single instance of the MathJax process.
 */
class MathJaxNodeInstance
{
	private TimeoutReader stdout;
	private OutputStream stdin;
	private Process process;
	private MathJaxNodeExecutable parent;

	private final static Logger LOGGER = Logger.getLogger(MathJaxNodeInstance.class.getName());

	MathJaxNodeInstance(String[] executableParams, MathJaxNodeExecutable parent) throws IOException
	{
		process = Runtime.getRuntime().exec(executableParams);
		stdout = new TimeoutReader(process.getInputStream());
		stdin = process.getOutputStream();
		this.parent = parent;
		LOGGER.log(Level.WARNING, "Opened: " + this);
	}

	synchronized void closeInstance()
	{
		LOGGER.log(Level.WARNING, "Closing: " + this, new Exception("here"));
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
}