package uk.ac.open.lts.webmaths.mathjax;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

import javax.servlet.ServletContext;

public class MathJaxNodeExecutable
{
	private final static Logger LOGGER = Logger.getLogger(MathJaxNodeExecutable.class.getName());

	/** Servlet parameter used to specify location of MathJax-node folder. */
	private static final String PARAM_MATHJAXNODEFOLDER = "mathjaxnode-folder";

	/** Servlet parameter used to indicate maximum number of Node instances. */
	private static final String PARAM_MATHJAXNODEINSTANCES = "mathjaxnode-instances";

	/** If true, logs content sent/retrieved to executable to stderr */
	private final static boolean LOG_COMMUNICATION = false;

	/** Time allowed for MathJax to process an equation or return a line of text. */
	final static int PROCESSING_TIMEOUT = 30000;

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

	/** If true, fakes errors for most responses. */
	private final static boolean FAKE_ERRORS = false;

	/** Regex used for package version from json file */
	private final static Pattern REGEX_PACKAGEVERSION = Pattern.compile(
		"^\\s*\"version\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*$");

	/** Maximum number of instances of MathJax.node to run at once. */
	private int maxInstances = 4;

	/** Path to executable script */
	private String executablePath;

	/** Folder path to MathJax.node */
	private String mathJaxFolder;

	/** Current instances. */
	private ArrayList<MathJaxNodeInstance> instances;
	/** Available instances (subset of Instances) */
	private Set<MathJaxNodeInstance> availableInstances = new TreeSet<MathJaxNodeInstance>();

	/** Time at which an instance was last created (so we don't create too fast) */
	private long lastCreatedInstance;

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

	/** Time it took to process each of the last STATS_SIZE equations. */
	private EquationDetails[] equationTimes = new EquationDetails[STATS_SIZE];

	/**
	 * Index of next equation time to write (circular buffer).
	 */
	private int equationTimeIndex = 0;

	/**
	 * List of recent errors.
	 */
	private LinkedList<Error> errors = new LinkedList<Error>();

	/** Time at which N+1 instances were last simultaneously used. */
	protected long[] lastSimultaneousUsed;

	/** Check whether to flush spares once a minute. */
	private static long FLUSH_SPARES_CHECK_PERIOD = 60000L;
	/** Flush spares after 2 minutes. */
	private final static long FLUSH_SPARES_AFTER = 2 * 60 * 1000L;
	/** Spin up a new instance only once per 1000ms. */
	private final static long INSTANCE_CREATION_DELAY = 1000L;
	/** Time it will wait for an instance to become available (if one exists) */
	private final static long INSTANCE_WAIT_TIME = 500L;
	/** Arbitrary long time for waiting forever */
	private final static long LONG_TIME = 100000L;

	/** Checker for flushing spares */
	protected PeriodicChecker checker;

	/**
	 * Details about an equation that was processed recently.
	 * <p>
	 * Comparable - will sort in reverse date order.
	 */
	public static class EquationDetails implements Comparable<EquationDetails>
	{
		private long date;
		private InputEquation equation;
		private int processingTime;

		/**
		 * @param equation Equation
		 * @param processingTime Time in milliseconds
		 */
		public EquationDetails(InputEquation equation, int processingTime)
		{
			this.date = System.currentTimeMillis();
			this.equation = equation;
			this.processingTime = processingTime;
		}

		/**
		 * @return Equation
		 */
		public InputEquation getEquation()
		{
			return equation;
		}

		/**
	   * @return Date of conversion
		 */
		public long getTime()
		{
			return date;
		}

		/**
		 * @return Processing time in milliseconds
		 */
		public int getProcessingTime()
		{
			return processingTime;
		}

		@Override
		public int compareTo(EquationDetails o)
		{
			if (o.date < date)
			{
				return -1;
			}
			else if(o.date == date)
			{
				return 0;
			}
			else
			{
				return 1;
			}
		}
	}

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

		/**
		 * @return Equation that caused error
		 */
		public InputEquation getEquation()
		{
			return equation;
		}
	}

	/**
	 * Stats about how many equations we have processed.
	 */
	public static class Status
	{
		private int cacheHits, cacheMisses, errorCount;
		private Error[] errors;
		private EquationDetails[] recentEquations;

		public Status(int cacheHits, int cacheMisses, int errorCount, Error[] errors,
			EquationDetails[] recentEquations)
		{
			this.cacheHits = cacheHits;
			this.cacheMisses = cacheMisses;
			this.errorCount = errorCount;
			this.errors = errors;

			// If the recent equations list has some null values...
			if(recentEquations[recentEquations.length - 1] == null)
			{
				// Find the last non-null.
				int last;
				for (last = recentEquations.length - 1; last >= 0; last--)
				{
					if(recentEquations[last] != null)
					{
						break;
					}
				}
				this.recentEquations = Arrays.copyOfRange(recentEquations, 0, last + 1);
			}
			else
			{
				// Just use the list directly if full.
				this.recentEquations = Arrays.copyOf(recentEquations, recentEquations.length);
			}

			// Sort the recent equations list by time
			Arrays.sort(this.recentEquations);
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
		 * @return Array of most recent errors (newest first)
		 */
		public Error[] getErrors()
		{
			return errors;
		}

		/**
		 * @return Array of most recent equations (newest first)
		 */
		public EquationDetails[] getRecentEquations()
		{
			return recentEquations;
		}
	}

	/**
	 * Results from conversion via the MathJax-node command-line tool.
	 */
	public static class ConversionResults
	{
		private String svg;
		private String mathml;

		protected ConversionResults(String svg, String mathMl)
		{
			this.svg = svg;
			this.mathml = mathMl;
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
		public String getMathml()
		{
			return mathml;
		}
	}

	/**
	 * Thread that runs periodically to see if we can close down spare Node instances.
	 */
	protected class PeriodicChecker extends Thread
	{
		private boolean close, closed;

		/**
		 * Starts the checker.
		 */
		public PeriodicChecker()
		{
			super("Node instance shutdown checker");
			setPriority(Thread.MIN_PRIORITY);
			start();
		}

		/**
		 * Closes the checker.
		 */
		public void close()
		{
			synchronized(this)
			{
				close = true;
				while(!closed)
				{
					try
					{
						wait();
					}
					catch(InterruptedException e)
					{
						return;
					}
				}
			}

		}

		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					synchronized(this)
					{
						try
						{
							wait(FLUSH_SPARES_CHECK_PERIOD);
						}
						catch(InterruptedException e)
						{
						}
						if(close)
						{
							return;
						}
					}

					closeSpareInstances();
				}
			}
			finally
			{
				synchronized(this)
				{
					closed = true;
					notifyAll();
				}
			}
		}
	}

	/**
	 * Empty constructor for unit test.
	 */
	protected MathJaxNodeExecutable()
	{
		maxInstances = 4;
		basicInit();
	}

	/**
	 * Constructs and sets up parameters. (Does not actually start executable.)
	 * @param servletContext Servlet context
	 */
	public MathJaxNodeExecutable(ServletContext servletContext)
	{
		// Work out parameters for executable.
		String folder = getFolder(servletContext);

		File executable = new File(servletContext.getRealPath("WEB-INF/ou-mathjax-batchprocessor"));
		executable.setExecutable(true);
		executablePath = executable.getAbsolutePath();
		mathJaxFolder = folder;

		try
		{
			maxInstances = Integer.parseInt(servletContext.getInitParameter(PARAM_MATHJAXNODEINSTANCES));
		}
		catch(NumberFormatException e)
		{
			throw new IllegalArgumentException("Incorrect value of " + PARAM_MATHJAXNODEINSTANCES + " (must be integer)");
		}
		catch(NullPointerException e)
		{
			throw new IllegalArgumentException("Required parameter " + PARAM_MATHJAXNODEINSTANCES + " missing");
		}

		basicInit();
	}

	/**
	 * Gets the folder parameter locating MathJax.node.
	 * @param servletContext Servlet context
	 * @return Parameter value
	 * @throws IllegalArgumentException If not set
	 */
	private static String getFolder(ServletContext servletContext)
		throws IllegalArgumentException
	{
		String folder = servletContext.getInitParameter(PARAM_MATHJAXNODEFOLDER);
		if(folder == null)
		{
			throw new IllegalArgumentException("Servlet parameter "
				+ PARAM_MATHJAXNODEFOLDER + " must be set");
		}
		return folder;
	}

	/**
	 * Gets the MathJax version by reading package.json.
	 * @param servletContext Servlet context
	 * @return MathJax version number
	 * @throws IOException If any error finding it
	 */
	public static String getVersion(ServletContext servletContext) throws IOException
	{
		String version = null;
		File packageJson = new File(getFolder(servletContext), "package.json");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			new FileInputStream(packageJson), "UTF-8"));
		while(true)
		{
			String line = reader.readLine();
			if(line == null)
			{
				break;
			}
			Matcher m = REGEX_PACKAGEVERSION.matcher(line);
			if(m.matches())
			{
				version = m.group(1);
				break;
			}
		}
		reader.close();
		if(version == null)
		{
			throw new IOException("Unable to find version in package.json");
		}
		return version;
	}

	/**
	 * Shared part of constructor.
	 */
	private void basicInit()
	{
		instances = new ArrayList<MathJaxNodeInstance>(maxInstances);
		lastSimultaneousUsed = new long[maxInstances];
		checker = new PeriodicChecker();
	}

	/**
	 * Logs a message when extra logging is turned on.
	 * @param message Message to log
	 */
	void log(String message)
	{
		if(!LOG_COMMUNICATION)
		{
			return;
		}
		LOGGER.log(Level.INFO, "[WebMaths] " + message);
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

		MathJaxNodeInstance instance = null;
		long startedWaiting = System.currentTimeMillis();
		synchronized(instances)
		{
			outer: while(instance == null)
			{
				// Find a free instance with the correct font.
				for(MathJaxNodeInstance possible : availableInstances)
				{
					if(possible.getFont().equals(eq.getFont()))
					{
						instance = possible;
						availableInstances.remove(instance);
						break outer;
					}
				}

				// Check if there are ANY instances with the correct font.
				boolean some = false;
				for(MathJaxNodeInstance possible : instances)
				{
					if(possible.getFont().equals(eq.getFont()))
					{
						some = true;
						break;
					}
				}

				// If there is already at least one instance dealing with this font,
				// give it a little while before creating another.
				if(some)
				{
					long delay = (startedWaiting + INSTANCE_WAIT_TIME) - System.currentTimeMillis();
					if (delay > 0)
					{
						// Wait up to the delay limit.
						instancesWait(delay);

						// After waiting, retry outer loop to see if one is available now.
						continue outer;
					}
				}

				// Check if we've already created the maximum number.
				if(instances.size() >= maxInstances)
				{
					// We are already at max instances so can't create another one.
					// Instead, grab another available instance.
					MathJaxNodeInstance available = null;
					for(MathJaxNodeInstance possible : availableInstances)
					{
						available = possible;
						break;
					}
					if(available != null)
					{
						// Close this instance.
						available.closeInstance();
						availableInstances.remove(available);
						instances.remove(available);

						// Replace it with a new one with this font.
						instance = createInstance(eq.getFont());
						instances.add(instance);
						break;
					}

					// Wait indefinitely until something becomes available.
					instancesWait(LONG_TIME);

					// Retry outer loop so we can recheck if there are any of our
					// font after waiting. Otherwise we could potentially be doing this
					// code branch (where there are supposed to be none of that font)
					// twice at once.
					continue;
				}

				// We are not at max instances. Create another instance of the right
				// font, provided we aren't creating instances too quickly.
				long delay = (lastCreatedInstance + INSTANCE_CREATION_DELAY) - System.currentTimeMillis();
				if (delay > 0)
				{
					// Wait up to the delay limit.
					instancesWait(delay);

					// After waiting, retry outer loop in case something else created an
					// instance etc.
					continue outer;
				}

				// If we get here then we haven't added an instance lately, so add one
				// now.
				lastCreatedInstance = System.currentTimeMillis();
				instance = createInstance(eq.getFont());
				instances.add(instance);
				break;
			}

			// Track how many instances are currently in use.
			int currentlyUsed = instances.size() - availableInstances.size();
			lastSimultaneousUsed[currentlyUsed - 1] = System.currentTimeMillis();
		}

		boolean instanceRemoved = false;
		try
		{
			long start = System.currentTimeMillis();
			try
			{
				// Send the type value.
				instance.sendLine(eq.getFormat());

				// Strip CRs from value, and ensure there aren't two LFs in a row or any the end.
				String value = eq.getContent().trim().replaceAll("\r", "").replaceAll("\n\n+", "\n");

				// Send value.
				instance.sendLine(value);
				instance.sendLine("");
				instance.flush();

				// Start reading lines from output.
				String first = instance.readLine();
				log("[READ] " + first);
				if(!first.equals("<<BEGIN:RESULT"))
				{
					throw new IOException("Expecting result start: " + first);
				}

				// Read the rest of it, splitting it into sections.
				Map<String, String> result = new HashMap<String, String>();
				result.put("ERRORS", "");
				result.put("SVG", "");
				result.put("MATHML", "");
				String section = null;
				while(true)
				{
					String line = instance.readLine();
					log("[READ] " + line);
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

				String error = result.get("ERRORS");
				if(FAKE_ERRORS)
				{
					if(Math.random() < 0.7)
					{
						if(Math.random() < 0.5)
						{
							error = "Error caused by random number";
						}
						else
						{
							throw new IOException("Error for testing");
						}
					}
				}

				// If no error is reported but there's no SVG either, it's an error.
				if(error.isEmpty() && result.get("SVG").isEmpty())
				{
					error = "Empty result";
				}
				if(!error.isEmpty())
				{
					trackError(new Error(eq, error));
					throw new MathJaxException(error);
				}
				got = new ConversionResults(result.get("SVG"), result.get("MATHML"));
			}
			catch(IOException e)
			{
				log("[FAILURE] " + e.getMessage());

				// If an IO exception occurs, stop the processor and read any text from
				// stderr.
				String stderr = instance.closeWithStderr();
				instanceRemoved = true;
				log("[STDERR DUMP]\n" + stderr);

				// Add stderr information to error if present.
				if(!stderr.isEmpty())
				{
					IOException combined = new IOException(e.getMessage() + "\n" + stderr);
					combined.initCause(e);
					e = combined;
				}

				// Record the error.
				trackError(new Error(eq, e));
				throw e;
			}

			synchronized(equationTimes)
			{
				equationTimes[equationTimeIndex] = new EquationDetails(eq,
					(int)(System.currentTimeMillis() - start));
				equationTimeIndex++;
				if(equationTimeIndex >= STATS_SIZE)
				{
					equationTimeIndex = 0;
				}
			}
		}
		finally
		{
			synchronized(instances)
			{
				if(instanceRemoved)
				{
					instances.remove(instance);
				}
				else
				{
					availableInstances.add(instance);
				}
				instances.notifyAll();
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
	 * Waits on the instances object. Must be called inside synchronization.
	 * @param delay Time to wait
	 * @throws IOException If waiting is interrupted
	 */
	private void instancesWait(long delay) throws IOException
	{
		try
		{
			instances.wait(delay);
		}
		catch(InterruptedException e)
		{
			throw new IOException("MathJax processing thread interrupted", e);
		}
	}

	/**
	 * Called regularly to check for any spare Node instances that we can close.
	 */
	private void closeSpareInstances()
	{
		synchronized(instances)
		{
			// Don't close instances if there's only one left.
			if(instances.size() <= 1)
			{
				return;
			}

			// Work out the max number simultaneously used at any point in the recent past.
			int maxUsed = 1;
			long now = System.currentTimeMillis();
			for(int i = 1; i < lastSimultaneousUsed.length; i++)
			{
				if(lastSimultaneousUsed[i] > now - FLUSH_SPARES_AFTER)
				{
					maxUsed = i + 1;
				}
			}

			// If it's less than currently active, remove some.
			if(maxUsed < instances.size())
			{
				int flush = instances.size() - maxUsed;
				List<MathJaxNodeInstance> forTheChop = new ArrayList<MathJaxNodeInstance>(maxInstances);
				// First remove anything using a non-default font.
				for(MathJaxNodeInstance spare : availableInstances)
				{
					if(!spare.getFont().equals(InputEquation.DEFAULT_FONT))
					{
						forTheChop.add(spare);
						flush--;
						if(flush <= 0)
						{
							break;
						}
					}
				}
				// Now remove default-font instances too.
				if(flush > 0)
				{
					for(MathJaxNodeInstance spare : availableInstances)
					{
						forTheChop.add(spare);
						flush--;
						if(flush <= 0)
						{
							break;
						}
					}
				}
				for(MathJaxNodeInstance spare : forTheChop)
				{
					spare.closeInstance();
					availableInstances.remove(spare);
					instances.remove(spare);
				}
			}
		}
	}

	/**
	 * Creates a new instance. (Included for unit testing.)
	 * @param font Font to use
	 * @return New instance
	 * @throws IOException Any error
	 */
	protected MathJaxNodeInstance createInstance(String font) throws IOException
	{
		return new MathJaxNodeInstance(executablePath, mathJaxFolder, font, this);
	}

	/**
	 * Stops the executable.
	 */
	public void close()
	{
		checker.close();
		checker = null;
		synchronized(instances)
		{
			while(!instances.isEmpty())
			{
				MathJaxNodeInstance instance = instances.remove(0);
				instance.closeInstance();
			}
		}
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
				errors.toArray(new Error[errors.size()]), equationTimes);
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

			if(!errors.isEmpty())
			{
				Error first = errors.getFirst();
				if(first != null && first.isBasicallyTheSame(error))
				{
					first.increaseCount();
					return;
				}
			}

			errors.addFirst(error);
			if(errors.size() > ERROR_COUNT)
			{
				errors.removeLast();
			}
		}
	}

	/**
	 * @return Max number of Node instances that can be run at once
	 */
	public int getMaxInstances()
	{
		return maxInstances;
	}
}
