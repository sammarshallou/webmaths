/*
This file is part of OU webmaths

OU webmaths is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

OU webmaths is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with OU webmaths. If not, see <http://www.gnu.org/licenses/>.

Copyright 2015 The Open University
*/
package uk.ac.open.lts.webmaths;

import static uk.ac.open.lts.webmaths.StatusServlet.esc;

import java.io.*;
import java.net.InetAddress;
import java.nio.charset.Charset;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.ws.BindingProvider;

import org.apache.commons.codec.binary.Base64;

import uk.ac.open.lts.webmaths.image.*;
import uk.ac.open.lts.webmaths.tex.*;

/**
 * The check servlet works by converting an equation using each of the currently
 * available mechanisms.
 */
public class CheckServlet extends HttpServlet
{
	/** Current or previous MathJax check thread (null if none) */
	private MathJaxChecker mathJaxCheck;

	/** Length of time the check must take before counting as failed */
	private final static long MATHJAX_CHECK_LIMIT = 60000;
	/** Length of time we wait for the check within the initial request */
	private final static long MATHJAX_CHECK_WAIT = 3000;

	/** Object used to synch the MathJax checks */
	private Object mathJaxCheckSynch = new Object();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		resp.setCharacterEncoding("UTF-8");
		PrintWriter pw = resp.getWriter();

		// Make up an equation using current time.
		String tex = "\\sqrt{\\frac{\\textrm{System check}}{" + System.currentTimeMillis() + "}}";

		boolean failed = false;

		StringBuilder out = new StringBuilder();
		out.append("<h2>MathML conversion</h2>");

		String host = InetAddress.getLocalHost().getHostName();
		String baseUrl = req.getRequestURL().toString().replaceFirst("check$", "").replaceFirst(
			"(https?://)[^:/]+", "$1" + host);
		out.append("<p>Base url: " + baseUrl + "</p>");

		// Convert TeX to MathML using service.
		String mathml = null;
		try
		{
			MathsTexParams texParams = new MathsTexParams();
			texParams.setTex(tex);
			texParams.setDisplay(true);
			MathsTexPort mathsTexPort = (new MathsTex()).getMathsTexPort();
			setUrl(mathsTexPort, baseUrl + "tex");
			MathsTexReturn texResult = mathsTexPort.getMathml(texParams);
			if(!texResult.isOk())
			{
				out.append("<p>MathML conversion error: " + esc(texResult.getError()) + "</p>");
				failed = true;
			}
			else
			{
				mathml = texResult.getMathml();
				out.append("<p>Successful</p>");
			}
		}
		catch(Throwable t)
		{
			out.append(getExceptionText(t));
			failed = true;
		}

		// If the MathML one fails we can't do the others.
		if (!failed)
		{
			// Get image params.
			MathsImageParams imageParams = new MathsImageParams();
			imageParams.setMathml(mathml);
			imageParams.setSize(1.0f);

			// Obtain image (JEuclid - redish).
			out.append("<h2>Image display (<tt>image</tt> service; JEuclid)</h2>");
			try
			{
				imageParams.setRgb("#aa8888");
				MathsImagePort mathsImagePort = new MathsImage().getMathsImagePort();
				setUrl(mathsImagePort, baseUrl + "image");
				MathsImageReturn imageResult = mathsImagePort.getImage(imageParams);
				if(imageResult.isOk())
				{
					out.append("<p>" + getPngImage(imageResult.getImage()) + "</p>");
				}
				else
				{
					out.append("<p>Service reports error: " + esc(imageResult.getError()) + "</p>");
					failed = true;
				}
			}
			catch(Throwable t)
			{
				out.append(getExceptionText(t));
				failed = true;
			}

			// Obtain image (LaTeX - greenish).
			out.append("<h2>Image display (<tt>imagetex</tt> service; LaTeX)</h2>");
			try
			{
				MathsImagePort mathsImagePort = new MathsImage().getMathsImagePort();
				setUrl(mathsImagePort, baseUrl + "imagetex");
				imageParams.setRgb("#88aa88");
				MathsImageReturn imageResult = mathsImagePort.getImage(imageParams);
				if(imageResult.isOk())
				{
					out.append("<p>" + getPngImage(imageResult.getImage()) + "</p>");
				}
				else
				{
					out.append("<p>Service reports error: " + esc(imageResult.getError()) + "</p>");
					failed = true;
				}
			}
			catch(Throwable t)
			{
				out.append(getExceptionText(t));
				failed = true;
			}

			// Obtain image (MathJax - blueish).
			out.append("<h2>Image display (<tt>mj-image</tt> service; MathJax.node)</h2>");
			imageParams.setRgb("#8888aa");
			if(mathJaxCheck(out, baseUrl, imageParams))
			{
				failed = true;
			}
		}

		out.append("<h2>More information</h2>");
		out.append("<p><a href='./'>Status page</a></p>");

		String heading;
		if(failed)
		{
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			heading = "<h1>Check failed</h1>";
		}
		else
		{
			resp.setStatus(HttpServletResponse.SC_OK);
			heading = "<h1>Check OK</h1>";
		}

		pw.print("<!doctype html><html><head><title>WebMaths check</title></head>"
			+ "<body>" + heading + out +"</body></html>");
		pw.close();
	}

	/**
	 * Checks MathJax to see if it is failing. Includes logic so that if it takes
	 * a while because the server is busy, it doesn't fail immediately or take
	 * ages to report, but instead comes back after 3 seconds with a 'pending'
	 * result that will be checked on the next attempt.
	 *
	 * @param out StringBuilder for the output HTML
	 * @param baseUrl Base URL for equation service
	 * @param imageParams Image paramters for the equation
	 * @return True if it failed, false if it is OK (or OK for now)
	 */
	private boolean mathJaxCheck(StringBuilder out, String baseUrl, MathsImageParams imageParams)
	{
		synchronized(mathJaxCheckSynch)
		{
			if(mathJaxCheck != null)
			{
				if(mathJaxCheck.isRunning())
				{
					long time = mathJaxCheck.getRunTime();
					if (time < MATHJAX_CHECK_LIMIT)
					{
						out.append("<p>Request in progress (" + time + "ms)</p>");
						return false;
					}
					else
					{
						out.append("<p>Request took too long (" + time + "ms)</p>");
						mathJaxCheck = null;
						return true;
					}
				}
				if(mathJaxCheck.isError())
				{
					out.append("<p>Previous request failed (" + mathJaxCheck.getError() + ")</p>");
					mathJaxCheck = null;
					return true;
				}
				if(mathJaxCheck.isSuccessful())
				{
					out.append("<p>Previous request successful (" + mathJaxCheck.getSuccessfulTime() + "ms):<br />" +
						mathJaxCheck.getSuccessfulResult() + "</p>");
					mathJaxCheck = null;
					// Don't do another check yet, maybe it's been heavily loaded.
					return false;
				}
				throw new Error("Unexpected status");
			}

			// Now that we've dealt with the previous check we will start a new one!
			mathJaxCheck = new MathJaxChecker(baseUrl, imageParams);
			try
			{
				mathJaxCheckSynch.wait(MATHJAX_CHECK_WAIT);
			}
			catch(InterruptedException e)
			{
			}
			if(mathJaxCheck.isRunning())
			{
				out.append("<p>Request in progress (taking a while, will confirm next time).</p>");
				return false;
			}
			else
			{
				if(mathJaxCheck.isError())
				{
					out.append("<p>Request failed (" + mathJaxCheck.getError() + ")</p>");
					mathJaxCheck = null;
					return true;
				}
				if(mathJaxCheck.isSuccessful())
				{
					out.append("<p>Request successful (" + mathJaxCheck.getSuccessfulTime() + "ms): " +
						mathJaxCheck.getSuccessfulResult() + "</p>");
					mathJaxCheck = null;
					return false;
				}
				out.append("<p>Status unknown (unexpected)</p>");
				mathJaxCheck = null;
				return true;
			}
		}
	}

	/**
	 * Checker thread that makes a MathJax request.
	 */
	private class MathJaxChecker extends Thread
	{
		private String baseUrl;
		private MathsImageParams imageParams;

		private long started;
		private long time;
		private String result, error;

		/**
		 * @param baseUrl Base URL for server
		 * @param imageParams Parameters for equation request
		 */
		public MathJaxChecker(String baseUrl, MathsImageParams imageParams)
		{
			super("MathJax checker");
			this.baseUrl = baseUrl;
			this.imageParams = imageParams;
			start();
		}

		/**
		 * (Must call from within synch.)
		 * @return True if check is currently in progress
		 */
		boolean isRunning()
		{
			return started != 0;
		}

		/**
		 * (Must call from within synch.)
		 * @return True if check ended in error
		 */
		boolean isError()
		{
			return error != null;
		}

		/**
		 * (Must call from within synch.)
		 * @return True if check ended successfully
		 */
		boolean isSuccessful()
		{
			return started == 0 && result != null;
		}

		/**
		 * (Must call from within synch.)
		 * @return Error HTML
		 */
		String getError()
		{
			if(!isError())
			{
				throw new Error("Not an error");
			}
			return error;
		}

		/**
		 * (Must call from within synch.)
		 * @return Runtime so far in milliseconds
		 */
		long getRunTime()
		{
			if(!isRunning())
			{
				throw new Error("Not running");
			}
			return System.currentTimeMillis() - started;
		}

		/**
		 * (Must call from within synch.)
		 * @return Time taken for successful check
		 */
		long getSuccessfulTime()
		{
			if(!isSuccessful())
			{
				throw new Error("Not successful");
			}
			return time;
		}

		/**
		 * (Must call from within synch.)
		 * @return Successful result HTML
		 */
		String getSuccessfulResult()
		{
			if(!isSuccessful())
			{
				throw new Error("Not successful");
			}
			return result;
		}

		@Override
		public void run()
		{
			synchronized(mathJaxCheckSynch)
			{
				started = System.currentTimeMillis();
			}

			try
			{
				MathsImagePort mathsImagePort = new MathsImage().getMathsImagePort();
				setUrl(mathsImagePort, baseUrl + "mj-image");
				imageParams.setRgb("#8888aa");
				MathsImageReturn imageResult = mathsImagePort.getImage(imageParams);
				synchronized(mathJaxCheckSynch)
				{
					if(imageResult.isOk())
					{
						time = System.currentTimeMillis() - started;
						result = getPngImage(imageResult.getImage());
					}
					else
					{
						error = esc(imageResult.getError());
					}
				}
			}
			catch(Throwable t)
			{
				synchronized(mathJaxCheckSynch)
				{
					error = getExceptionText(t);
				}
			}
			synchronized(mathJaxCheckSynch)
			{
				started = 0;
				mathJaxCheckSynch.notifyAll();
			}
		}
	}

	private static void setUrl(Object port, String url)
	{
		BindingProvider bp = (BindingProvider)port;
		bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
	}

	private static String getExceptionText(Throwable t)
	{
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return "<pre class='exception'>" +
			esc(sw.toString()) + "</pre>";
	}

	private static String getPngImage(byte[] png)
	{
		StringBuilder out = new StringBuilder();
		out.append("<img src='data:image/png;base64,");
		out.append(new String(Base64.encodeBase64(png, false), Charset.forName("UTF-8")));
		out.append("' />");
		return out.toString();
	}
}
