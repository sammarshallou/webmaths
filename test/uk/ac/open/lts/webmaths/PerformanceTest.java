package uk.ac.open.lts.webmaths;

import java.util.Random;

import javax.xml.ws.BindingProvider;

import uk.ac.open.lts.webmaths.english.*;
import uk.ac.open.lts.webmaths.image.*;
import uk.ac.open.lts.webmaths.mathjax.*;
import uk.ac.open.lts.webmaths.tex.*;

/**
 * Performance testing class that runs a standard check against a server.
 */
public class PerformanceTest
{
	private static abstract class TestAction
	{
		protected String baseUrl;

		protected TestAction(String baseUrl)
		{
			this.baseUrl = baseUrl;
		}

		abstract void run(String[] texEquations) throws Exception;
	}

	/**
	 * Replicates the web service request needed to convert some equations using
	 * the new system.
	 */
	private static class MathJaxTestAction extends TestAction
	{
		protected MathJaxTestAction(String baseUrl)
		{
			super(baseUrl);
		}

		@Override
		void run(String[] texEquations) throws Exception
		{
			ConvertEquationsParams params = new ConvertEquationsParams();
			params.setExSize(7.26667f);
			params.setRgb("#1a1a1a");
			params.getOutputs().add(ConversionType.SVG_PX);
			params.getOutputs().add(ConversionType.SVG_PX_BASELINE);
			params.getOutputs().add(ConversionType.TEXT);
			for(String tex : texEquations)
			{
				SourceEquation eq = new SourceEquation();
				eq.setTex(tex);
				eq.setDisplay(true);
				params.getEquations().add(eq);
			}

			MathsMathJaxPort port = new MathsMathJax().getMathsMathJaxPort();
			setUrl(port, baseUrl + "mathjax");
			ConvertEquationsReturn result = port.convertEquations(params);
			for(OutputData out : result.getOutput())
			{
				if(!out.isOk())
				{
					throw new Exception("Conversion failure");
				}
			}
		}
	}

	/**
	 * Replicates the sequence of web service requests used to generate an
	 * equation in the legacy system.
	 */
	private static class LegacyTestAction extends TestAction
	{
		protected LegacyTestAction(String baseUrl)
		{
			super(baseUrl);
		}

		@Override
		void run(String[] texEquations) throws Exception
		{
			for(String tex : texEquations)
			{
				// Convert to MathML.
				MathsTexParams texParams = new MathsTexParams();
				texParams.setTex(tex);
				texParams.setDisplay(true);
				MathsTexPort mathsTexPort = (new MathsTex()).getMathsTexPort();
				setUrl(mathsTexPort, baseUrl + "tex");
				MathsTexReturn texResult = mathsTexPort.getMathml(texParams);
				if(!texResult.isOk())
				{
					throw new Exception("Failed: tex");
				}

				// Get the image.
				MathsImageParams imageParams = new MathsImageParams();
				imageParams.setMathml(texResult.getMathml());
				imageParams.setSize(1.0f);
				imageParams.setRgb("#1a1a1a");
				MathsImagePort mathsImagePort = new MathsImage().getMathsImagePort();
				setUrl(mathsImagePort, baseUrl + "imagetex");
				MathsImageReturn imageResult = mathsImagePort.getImage(imageParams);
				if(!imageResult.isOk())
				{
					throw new Exception("Failed: imagetex");
				}

				// Also get the English text.
				MathsEnglishParams englishParams = new MathsEnglishParams();
				englishParams.setMathml(texResult.getMathml());
				MathsEnglishPort mathsEnglishPort = new MathsEnglish().getMathsEnglishPort();
				setUrl(mathsEnglishPort, baseUrl + "english");
				MathsEnglishReturn englishResult = mathsEnglishPort.getEnglish(englishParams);
				if(!englishResult.isOk())
				{
					throw new Exception("Failed: english");
				}
			}
		}
	}

	public static void main(String[] args)
	{
		if(args.length != 2)
		{
			System.err.println("Parameters: {legacy/mathjax} <service base url>");
			return;
		}

		String url = args[1];
		if(!url.endsWith("/"))
		{
			url += "/";
		}

		TestAction action;
		if(args[0].equals("mathjax"))
		{
			action = new MathJaxTestAction(url);
		}
		else if(args[0].equals("legacy"))
		{
			action = new LegacyTestAction(url);
		}
		else
		{
			System.err.println("Invalid first parameter");
			return;
		}

		runTest(action);
	}

	private static void runTest(TestAction action)
	{
		StringBuilder out = new StringBuilder();
		out.append("Batch size,Threads,Count,Milliseconds\n");
		// Run test for single equations at a time, or up to 3 at once.
		for(int batch = 1; batch <= 3; batch++)
		{
			// Run test with thread counts 1, 2, 5, and 10.
			for(int threadCount : new int[] {1, 2, 5, 10})
			{
				System.err.println("TEST SET: batch=" + batch + ", threads=" + threadCount);

				// Do 100 equations per batch.
				Countdown countdown = new Countdown(100, threadCount);
				for(int thread = 0; thread < threadCount; thread++)
				{
					new TestThread(batch, countdown, action);
				}
				countdown.waitForThreads();
				if(countdown.hasError())
				{
					countdown.getError().printStackTrace();
					return;
				}
				System.err.println();

				out.append(batch + "," + threadCount + "," +
					countdown.getCount() + "," + countdown.getTime() + "\n");
			}
		}
		System.out.println();
		System.out.println(out);
	}

	private static class Countdown
	{
		private int count = 0, max, threads;
		private long started;
		private long finished = 0L;
		private Exception error;

		Countdown(int max, int threads)
		{
			this.max = max;
			this.threads = threads;
			this.started = System.currentTimeMillis();
		}

		synchronized boolean count(int num)
		{
			// Don't add to count after finished, just exit.
			if(finished != 0L)
			{
				return true;
			}

			for(int i = 0; i < num; i++)
			{
				System.err.print(".");
			}

			count += num;
			if(count >= max)
			{
				finished = System.currentTimeMillis();
				return true;
			}
			return false;
		}

		synchronized void error(Exception e)
		{
			finished = 1;
			error = e;
		}

		synchronized void threadDone()
		{
			threads--;
			if(threads == 0)
			{
				notifyAll();
			}
		}

		synchronized void waitForThreads()
		{
			while(threads > 0)
			{
				try
				{
					wait();
				}
				catch(InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}

		public synchronized int getCount()
		{
			return count;
		}

		public synchronized long getTime()
		{
			if(finished == 0L)
			{
				throw new IllegalStateException("Not finished yet");
			}
			System.err.println();

			return finished - started;
		}

		public synchronized boolean hasError()
		{
			return error != null;
		}

		public synchronized Exception getError()
		{
			return error;
		}
	}

	private static class TestThread extends Thread
	{
		private Countdown countdown;
		private int batch;
		private TestAction action;

		TestThread(int batch, Countdown countdown, TestAction action)
		{
			this.countdown = countdown;
			this.batch = batch;
			this.action = action;
			start();
		}

		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					String[] equations = new String[batch];
					for(int i = 0; i < batch; i++)
					{
						equations[i] = "\\sqrt{\\frac{1}{" + getUniqueLetters() + "}}";
					}
					action.run(equations);
					if(countdown.count(batch))
					{
						return;
					}
				}
			}
			catch(Exception e)
			{
				countdown.error(e);
				return;
			}
			finally
			{
				countdown.threadDone();
			}
		}

		private static Random random = new Random();

		private static String getUniqueLetters()
		{
			StringBuilder s = new StringBuilder(10);
			for(int i=0; i<10; i++)
			{
				s.append((char)(random.nextInt(26) + (int)'a'));
			}
			return s.toString();
		}
	}


	private static void setUrl(Object port, String url)
	{
		BindingProvider bp = (BindingProvider)port;
		bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
	}
}
