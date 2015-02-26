package uk.ac.open.lts.webmaths.mathjax;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.*;

import javax.servlet.ServletContext;

public class MathJaxNodeExecutable
{
	/** Servlet parameter used to specify location of MathJax-node folder. */
	private static final String PARAM_MATHJAXNODEFOLDER = "mathjaxnode-folder";

	/** Time allowed for MathJax to process an equation or return a line of text. */
	private final static int PROCESSING_TIMEOUT = 10000;

	private String[] executableParams;
	private TimeoutReader stdout;
	private OutputStream stdin;
	private Process process;

	/**
	 * Results from conversion via the MathJax-node command-line tool.
	 */
	public static class ConversionResults
	{
		private String svg;
		private String mathMl;

		private ConversionResults(String svg, String mathMl)
		{
			this.svg = svg;
			this.mathMl = mathMl;
		}

		/**
		 * @return SVG code (as string)
		 */
		public String getSvg()
		{
			return svg;
		}

		/**
		 * @return MathJax code (empty string if none)
		 */
		public String getMathMl()
		{
			return mathMl;
		}
	}

	/**
	 * Constructs and sets up parameters. (Does not actually start executable.)
	 * @param servletContext Servlet context
	 */
	public MathJaxNodeExecutable(ServletContext servletContext)
	{
		// Work out parameters for executable.
		String folder = servletContext.getInitParameter(PARAM_MATHJAXNODEFOLDER);
		if(folder == null)
		{
			folder = "c:/users/sm449/workspace/MathJax-Node";
		}

		File executable = new File(servletContext.getRealPath("WEB-INF/ou-mathjax-batchprocessor"));
		executable.setExecutable(true);
		executableParams = new String[]
		{
			"node",
			executable.getAbsolutePath(),
			folder
		};
	}

	/**
	 * Sends a line of text to the application.
	 * @param text Text to send
	 * @throws IOException Any error
	 */
	private synchronized void sendLine(String text) throws IOException
	{
		stdin.write((text + "\n").getBytes(Charset.forName("UTF-8")));
		System.err.println("[SENT] " + text);
	}

	private final static Pattern REGEX_BEGIN = Pattern.compile("^<<BEGIN:([A-Z0-9]+)$");
	private final static Pattern REGEX_END = Pattern.compile("^<<END:([A-Z0-9]+)$");

	/**
	 * Converts an equation using MathJax.
	 * @param eq Equation
	 * @return Converted data
	 * @throws IOException Error running MathJax
	 * @throws MathJaxException MathJax reports an error
	 */
	public synchronized ConversionResults convertEquation(InputEquation eq)
		throws IOException, MathJaxException
	{
		try
		{
			// Start executable if needed.
			if(process == null)
			{
				startExecutable();
			}

			// Send the type value.
			sendLine(eq.getFormat());

			// Strip CRs from value, and ensure there aren't two LFs in a row or any the end.
			String value = eq.getContent().trim().replaceAll("\r", "").replaceAll("\n\n+", "\n");

			// Send value.
			sendLine(value);
			sendLine("");
			stdin.flush();

			// Start reading lines from output.
			String first = stdout.getNextLine(PROCESSING_TIMEOUT);
			System.err.println("[READ] " + first);
			if(!first.equals("<<BEGIN:RESULT"))
			{
				throw new IOException("Expecting result start: " + first);
			}

			// Read the rest of it, splitting it into sections.
			Map<String, String> result = new HashMap<String, String>();
			result.put("ERROR", "");
			result.put("SVG", "");
			result.put("MATHML", "");
			String section = null;
			while(true)
			{
				String line = stdout.getNextLine(PROCESSING_TIMEOUT);
				System.err.println("[READ] " + line);
				if(section == null)
				{
					if(line.equals("<<END:RESULT"))
					{
						break;
					}
					Matcher m = REGEX_BEGIN.matcher(line);
					if(!m.matches())
					{
						throw new IOException("Expecting BEGIN line: " + line);
					}
					section = m.group(1);
					if(!result.containsKey(section))
					{
						throw new IOException("Unknown result section: " + line);
					}
				}
				else
				{
					Matcher m = REGEX_END.matcher(line);
					if(m.matches())
					{
						if(!m.group(1).equals(section))
						{
							throw new IOException("Non-matching END, expecting " + section + ": " + line);
						}
						result.put(section, result.get(section).trim());
						section = null;
					}
					else
					{
						result.put(section, result.get(section) + line + "\n");
					}
				}
			}

			String error = result.get("ERROR");
			if(!error.isEmpty())
			{
				throw new MathJaxException(error);
			}
			return new ConversionResults(result.get("SVG"), result.get("MATHML"));
		}
		catch(IOException e)
		{
			// If an IO exception occurs, stop the processor and read any text from
			// stderr.
			TimeoutReader stderr = new TimeoutReader(process.getErrorStream());
			System.err.println("webmaths: MathJax-node failure (" + e.getMessage() + ")");
			boolean gotStderr = false;
			try
			{
				for(int i=0; i<100; i++)
				{
					System.err.println("webmaths: " + stderr.getNextLine(1000) + "\n");
					gotStderr = true;
				}
			}
			catch(IOException e2)
			{
			}
			if(gotStderr)
			{
				System.err.println("webmaths: (end stderr)");
			}
			stderr.requestExit();
			close();
			stderr.waitForExit();
			throw e;
		}
	}

	/**
	 * Starts the executable.
	 * @throws IOException Any problem launching it
	 */
	private synchronized void startExecutable() throws IOException
	{
		if(process != null)
		{
			throw new IllegalStateException("Already running");
		}
		process = Runtime.getRuntime().exec(executableParams);
		stdout = new TimeoutReader(process.getInputStream());
		stdin = process.getOutputStream();
	}

	/**
	 * Stops the executable.
	 */
	public synchronized void close()
	{
		if(process == null)
		{
			return;
		}
		stdout.requestExit();
		process.destroy();
		process = null;
		stdout.waitForExit();
		stdout = null;
		stdin = null;
	}
}
