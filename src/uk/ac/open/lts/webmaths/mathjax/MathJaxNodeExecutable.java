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

	/**
	 * Number of recent results to cache - quite low as we mainly only expect this
	 * to be used to improve performance within a request or in rapidly sequential
	 * requests.
	 */
	private final static int CACHE_SIZE = 100;

	/**
	 * Number of recent results to keep performance stats on.
	 */
	private final static int STATS_SIZE = 100;

	/**
	 * Number of recent errors to keep.
	 */
	private final static int ERROR_COUNT = 100;

	private String[] executableParams;
	private TimeoutReader stdout;
	private OutputStream stdin;
	private Process process;

	/** Cache of recent conversion results. */
	private Map<InputEquation, ConversionResults> cache =
		new HashMap<InputEquation, ConversionResults>(CACHE_SIZE);

	/** Ordered list of items in cache so we can remove older ones. */
	private LinkedList<InputEquation> cacheKeys = new LinkedList<InputEquation>();

	/** Number of cache hits. */
	private int countCacheHits;

	/** Number of cache misses. */
	private int countCacheMisses;

	/** Number of errors. */
	private int countErrors;

	/**
	 * Time it took (in milliseconds) to process each of the last STATS_SIZE
	 * equations.
	 */
	private int[] equationTimes = new int[STATS_SIZE];

	/**
	 * Index of next equation time to write (circular buffer).
	 */
	private int equationTimeIndex = 0;

	/**
	 * List of recent errors.
	 */
	private LinkedList<Error> errors = new LinkedList<Error>();

	/**
	 * An error that occurred.
	 */
	public static class Error
	{
		private final long time = System.currentTimeMillis();
		private final InputEquation equation;
		private final String text;
		private final Exception exception;
		private int count = 1;

		private Error(InputEquation equation, String text)
		{
			this.equation = equation;
			this.text = text;
			this.exception = null;
		}

		private Error(InputEquation equation, Exception exception)
		{
			this.equation = equation;
			this.exception = exception;
			this.text = null;
		}

		private boolean isBasicallyTheSame(Error other)
		{
			// Must have same equation.
			if(!other.equation.equals(equation))
			{
				return false;
			}
			if(text != null)
			{
				return text.equals(other.text);
			}
			else
			{
				return other.exception != null &&
					exception.toString().equals(other.exception.toString());
			}
		}

		private void increaseCount()
		{
			count++;
		}

		/**
		 * Text of error or null if it is not a MathJax reported error.
		 * @return Error text
		 */
		public String getText()
		{
			return text;
		}

		/**
		 * Exception of error or null if it is a MathJax error
		 * @return Exception that occurred
		 */
		public Exception getException()
		{
			return exception;
		}

		/**
		 * @return Time at which error occurred
		 */
		public long getTime()
		{
			return time;
		}

		/**
		 * @return Number of times the error occurred (sequentially)
		 */
		public int getCount()
		{
			return count;
		}
	}

	/**
	 * Stats about how many equations we have processed.
	 */
	public static class Status
	{
		private int cacheHits, cacheMisses, errorCount;
		private Error[] errors;

		public Status(int cacheHits, int cacheMisses, int errorCount, Error[] errors)
		{
			this.cacheHits = cacheHits;
			this.cacheMisses = cacheMisses;
			this.errorCount = errorCount;
			this.errors = errors;
		}

		/**
		 * @return Number of equations that were retrieved from cache
		 */
		public int getCacheHits()
		{
			return cacheHits;
		}

		/**
		 * @return Number of equations that were actually converted
		 */
		public int getCacheMisses()
		{
			return cacheMisses;
		}

		/**
		 * @return Number of calls that resulted in an error
		 */
		public int getErrorCount()
		{
			return errorCount;
		}

		/**
		 * @return Array of most recent errors (oldest first)
		 */
		public Error[] getErrors()
		{
			return errors;
		}
	}

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
	public ConversionResults convertEquation(InputEquation eq)
		throws IOException, MathJaxException
	{
		ConversionResults got;

		// Use cache if available.
		synchronized(cache)
		{
			got = cache.get(eq);
			if(got != null)
			{
				countCacheHits++;
				return got;
			}
			countCacheMisses++;
		}

		long start = System.currentTimeMillis();
		synchronized(this)
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
					trackError(new Error(eq, error));
					throw new MathJaxException(error);
				}
				got = new ConversionResults(result.get("SVG"), result.get("MATHML"));
			}
			catch(IOException e)
			{
				trackError(new Error(eq, e));

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

		synchronized(equationTimes)
		{
			equationTimes[equationTimeIndex] = (int)(System.currentTimeMillis() - start);
			equationTimeIndex++;
			if(equationTimeIndex >= STATS_SIZE)
			{
				equationTimeIndex = 0;
			}
		}

		synchronized(cache)
		{
			cache.put(eq, got);
			cacheKeys.addLast(eq);
			if(cacheKeys.size() > CACHE_SIZE)
			{
				InputEquation remove = cacheKeys.removeFirst();
				cache.remove(remove);
			}
		}

		return got;
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

	/**
	 * Gets current status and system stats.
	 * @return Stats about number of equations processed, etc.
	 */
	public Status getStatus()
	{
		int hits, misses;
		synchronized(cache)
		{
			hits = countCacheHits;
			misses = countCacheMisses;
		}

		synchronized(errors)
		{
			return new Status(hits, misses, countErrors,
				errors.toArray(new Error[errors.size()]));
		}
	}

	/**
	 * Adds error to the error list.
	 * @param error Error that occurred
	 */
	private void trackError(Error error)
	{
		synchronized(errors)
		{
			countErrors++;

			Error last = errors.getLast();
			if(last != null && last.isBasicallyTheSame(error))
			{
				last.increaseCount();
				return;
			}

			errors.addLast(error);
			if(errors.size() > ERROR_COUNT)
			{
				errors.removeFirst();
			}
		}
	}

}
