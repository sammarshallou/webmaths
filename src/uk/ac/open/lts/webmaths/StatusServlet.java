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

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import uk.ac.open.lts.webmaths.mathjax.*;
import uk.ac.open.lts.webmaths.mathjax.MathJaxNodeExecutable.Status;

/**
 * This servlet is used only to redirect requests for the root to one of the
 * service pages (so it's easier to see if the service is up and running).
 */
public class StatusServlet extends HttpServlet
{
	private long started;

	@Override
	public void init() throws ServletException
	{
		super.init();
		started = System.currentTimeMillis();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		// Set up response.
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setCharacterEncoding("UTF-8");
		PrintWriter pw = resp.getWriter();

		// Load template.
		String template = loadTemplate("status.html");
		Map<String, String> values = new HashMap<String, String>();

		// Fill basic data.
		values.put("SERVER", esc(getHostName()));
		String mathJaxVersion = "(Unknown)";
		try
		{
			mathJaxVersion = MathJaxNodeExecutable.getVersion(getServletContext());
		}
		catch(IOException e)
		{
		}
		values.put("MATHJAXVERSION", esc(mathJaxVersion));
		values.put("STARTEDAT", formatTime(started));

		// Fill MathJax stats.
		MathJax mj = MathJax.get(getServletContext());
		Status mjStatus = mj.getStatus();
		values.put("CACHEHITS", mjStatus.getCacheHits() + "");
		values.put("MATHJAXRUNS", mjStatus.getCacheMisses() + "");
		values.put("ERRORCOUNT", mjStatus.getErrorCount() + "");
		values.put("ERRORPERCENTAGE",
			mjStatus.getCacheMisses() == 0 ? "N/A%" : String.format("%.1f",
				(100.0 * (double)mjStatus.getErrorCount() /
				(double)mjStatus.getCacheMisses())) + "%");

		// Fill MathJax errors.
		if(mjStatus.getErrors().length == 0)
		{
			values.put("MATHJAXERRORS", "<p>No errors occurred yet.</p>");
		}
		else
		{
			StringBuilder out = new StringBuilder("<ul>");
			for(MathJaxNodeExecutable.Error error : mjStatus.getErrors())
			{
				out.append("<li>");
				out.append("<div class='time'>" + formatTime(error.getTime()) + "</div>");
				out.append("<div class='equation'>" + esc(error.getEquation().getContent()) + "</div>");
				if(error.getText() != null)
				{
					out.append("<div class='message'>" + esc(error.getText()) + "</div>");
				}
				if(error.getCount() != 1)
				{
					out.append("<div class='repeats'>Occurred <strong>" +
						error.getCount() + "</strong> times in a row</div>");
				}
				if(error.getException() != null)
				{
					StringWriter sw = new StringWriter();
					error.getException().printStackTrace(new PrintWriter(sw));
					out.append("<pre class='exception'>" +
						esc(sw.toString()) + "</pre>");
				}
				out.append("</li>");
			}
			out.append("</ul>");
			values.put("MATHJAXERRORS", out.toString());
		}

		// Fill MathJax recent equations.
		if(mjStatus.getRecentEquations().length == 0)
		{
			values.put("MATHJAXRECENT", "<p>No equations converted yet.</p>");
		}
		else
		{
			StringBuilder out = new StringBuilder("<ul>");
			for(MathJaxNodeExecutable.EquationDetails details : mjStatus.getRecentEquations())
			{
				out.append("<li class='" + details.getEquation().getFormat() + "'>");
				out.append("<div class='time'>" + formatTime(details.getTime()) + "</div>");
				out.append("<div class='performance'><strong>" +details.getProcessingTime() + "</strong>ms</div>");
				out.append("<div class='svg'>");
				try
				{
					out.append(mj.getSvg(details.getEquation(), true, 7.26667, null));
				}
				catch(MathJaxException e)
				{
					out.append("<div class='error'>Rendering failed</div>");
				}
				out.append("</div>");
				out.append("<div class='equation'>" + esc(details.getEquation().getContent()) + "</div>");
				out.append("</li>");
			}
			out.append("</ul>");
			values.put("MATHJAXRECENT", out.toString());
		}

		template = fixTemplate(template, values);

		// Write template.
		pw.print(template);
		pw.close();
	}

	public static String esc(String s)
	{
		return s.replace("&", "&amp;").replace("<", "&lt;");
	}

	private static String fixTemplate(String template, Map<String, String> replacements)
	{
		for(String key : replacements.keySet())
		{
			template = template.replaceAll(Pattern.quote("%%" + key + "%%"), Matcher.quoteReplacement(replacements.get(key)));
		}
		return template;
	}

	/**
	 * @return Computer name
	 */
	private static String getHostName()
	{
		try
		{
			return InetAddress.getLocalHost().getHostName();
		}
		catch(UnknownHostException e)
		{
			throw new Error(e);
		}
	}

	/**
	 * @param time
	 * @return String description of time
	 */
	private String formatTime(long time)
	{
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.UK).format(
			new Date(time));
	}

	/**
	 * Loads a template from classpath.
	 * @param filename Filename of template
	 * @return Template file as string
	 * @throws IllegalArgumentException If template of that name can't be loaded
	 */
	private String loadTemplate(String filename) throws IllegalArgumentException
	{
		char[] buffer = new char[65536];
		StringWriter writer = new StringWriter();
		try
		{
			InputStreamReader reader = new InputStreamReader(
				getClass().getResourceAsStream(filename), Charset.forName("UTF-8"));
			while(true)
			{
				int read = reader.read(buffer);
				if(read == -1)
				{
					break;
				}
				writer.write(buffer, 0, read);
			}
			reader.close();
		}
		catch(IOException e)
		{
			throw new IllegalArgumentException("Failed to read template " + filename);
		}
		return writer.toString();
	}
}
