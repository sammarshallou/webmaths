package uk.ac.open.lts.webmaths;

import java.util.*;

import javax.xml.ws.BindingProvider;

import uk.ac.open.lts.webmaths.image.*;

/**
 * Does bulk test of the image rendering to ensure it is reliable and check
 * performance.
 */
public class BulkTest
{
	private final static int DISPLAY_PER_CALLS = 100;

	private static Object synch = new Object();
	private static int threadsLeft = 0;

	public static void main(String[] args) throws Exception
	{
		String endpoint = args[0];
		int threads = Integer.parseInt(args[1]);
		int callsPerThread = Integer.parseInt(args[2]);

		// Set up service
		MathsImage service = new MathsImage();
		MathsImagePort image = service.getMathsImagePort();
		Map<String, Object> ctxt = ((BindingProvider)image).getRequestContext();
		ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

		// Define parameters
		MathsImageParams params = new MathsImageParams();
		params.setMathml("<math xmlns='http://www.w3.org/1998/Math/MathML'>\n" +
			"<mi>a</mi><mo>+</mo><msqrt><mi>b</mi></msqrt><annotation encoding='application/xx-tex'/></math>");
		params.setRgb("#000000");
		params.setSize(1.0f);

		// Run service once to verify result
		MathsImageReturn result = image.getImage(params);
		if(!result.isOk())
		{
			throw new Exception("Result not ok");
		}
		if(result.getImage().length < 100)
		{
			throw new Exception("Invalid result");
		}

		// Get start time
		long time = System.currentTimeMillis();

		// Start threads
		for(int i=0; i<threads; i++)
		{
			new HammerThread(i, callsPerThread, endpoint, params, result);
		}

		// Wait for exit
		synchronized(synch)
		{
			while(threadsLeft > 0)
			{
				synch.wait();
			}
		}

		// End time
		long duration = System.currentTimeMillis() - time;
		System.err.println();
		System.err.println("Duration: " + duration + " ms");
		System.err.println("Per equation: " + String.format("%.1f",
			(double)duration / (double)(callsPerThread * threads)) + " ms");
	}

	private static class HammerThread extends Thread
	{
		private int index;
		private int calls;
		private MathsImagePort image;
		private MathsImageParams params;
		private MathsImageReturn result;

		public HammerThread(int index, int calls, String endpoint, MathsImageParams params,
			MathsImageReturn result)
		{
			// Create new service (I'm not sure they are thread-safe)
			MathsImage service = new MathsImage();
			image = service.getMathsImagePort();
			Map<String, Object> ctxt = ((BindingProvider)image).getRequestContext();
			ctxt.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);

			this.index = index;
			this.calls = calls;
			this.params = params;
			this.result = result;
			synchronized(synch)
			{
				threadsLeft++;
			}
			start();
		}

		@Override
		public void run()
		{
			try
			{
				log("Started");
				for(int i = 0; i < calls; )
				{
					// Call service
					MathsImageReturn newResult = image.getImage(params);

					// Check result
					if(newResult.isOk() != result.isOk() ||
						!newResult.getBaseline().equals(result.getBaseline()) ||
						!newResult.getError().equals(result.getError()) ||
						!Arrays.equals(newResult.getImage(), result.getImage()))
					{
						throw new Exception("Unexpected result, aborting");
					}

					i++;
					if((i % DISPLAY_PER_CALLS) == 0)
					{
						log("Done " + i);
					}
				}
			}
			catch(Throwable t)
			{
				log(t.getMessage());
			}
			finally
			{
				synchronized(synch)
				{
					threadsLeft--;
					log("Completed");
					synch.notify();
				}
			}
		}

		private void log(String message)
		{
			System.err.println("[Thread " + index + "] " + message);
		}
	}
}
