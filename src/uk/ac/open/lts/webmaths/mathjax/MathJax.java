package uk.ac.open.lts.webmaths.mathjax;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.regex.*;

import javax.servlet.*;
import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.xpath.*;

import org.w3c.dom.*;

import uk.ac.open.lts.webmaths.WebMathsService;

/**
 * Carries out transformations using MathJax.node via an application copied to
 * the server.
 */
public class MathJax
{
	/** Name of attribute in ServletContext that stores singleton value. */
	private static final String ATTRIBUTE_NAME = "uk.ac.open.lts.webmaths.MathJax";

	/** Servlet parameter used to specify location of MathJax-node folder. */
	private static final String PARAM_MATHJAXNODEFOLDER = "mathjaxnode-folder";

	/**
	 * Get MathJax singleton, starting it if not already running.
	 * @param context
	 * @return
	 */
	public synchronized static MathJax get(WebServiceContext context)
	{
		ServletContext servletContext =
			(ServletContext)context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);

		MathJax mathJax = (MathJax)servletContext.getAttribute(ATTRIBUTE_NAME);
		if(mathJax == null)
		{
			mathJax = new MathJax(servletContext);
			servletContext.setAttribute(ATTRIBUTE_NAME, mathJax);
		}
		return mathJax;
	}

	/**
	 * Cleanup function kills process if running.
	 * @param servletContext Servlet context
	 */
	public synchronized static void cleanup(ServletContext servletContext)
	{
		MathJax mathJax = (MathJax)servletContext.getAttribute(ATTRIBUTE_NAME);
		if(mathJax != null)
		{
			mathJax.close();
			servletContext.removeAttribute(ATTRIBUTE_NAME);
		}
	}

	private final static int PROCESSING_TIMEOUT = 10000;

	private ServletContext context;

	private String[] executableParams;
	private TimeoutReader stdout;
	private OutputStream stdin;
	private Process process;

	private final XPath xpath;
	private final XPathExpression xpathAnnotation, xpathSvgDesc;

	/**
	 * Starts application.
	 * @param servletContext Servlet context
	 * @throws IOException Any problem launching the application
	 */
	private MathJax(ServletContext servletContext)
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

		// Precompile the xpath expressions.
		xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new MathmlAndSvgNamespaceContext());
		try
		{
			xpathAnnotation = xpath.compile(
				"normalize-space(/m:math/m:semantics/m:annotation[@encoding='application/x-tex'])");
			xpathSvgDesc = xpath.compile("normalize-space(/s:svg/s:desc)");
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}

		System.err.println(executableParams[1] + " " + executableParams[2]);
	}

	private static final class MathmlAndSvgNamespaceContext implements NamespaceContext
	{
		@Override
		public String getNamespaceURI(String prefix)
		{
			if(prefix.equals("m"))
			{
				return "http://www.w3.org/1998/Math/MathML";
			}
			else if(prefix.equals("s"))
			{
				return "http://www.w3.org/2000/svg";
			}
			else
			{
				return XMLConstants.NULL_NS_URI;
			}
		}

		@Override
		public String getPrefix(String uri)
		{
			if(uri.equals("http://www.w3.org/1998/Math/MathML"))
			{
				return "m";
			}
			else if(uri.equals("http://www.w3.org/2000/svg"))
			{
				return "s";
			}
			return null;
		}

		@Override
		public Iterator<?> getPrefixes(String uri)
		{
			LinkedList<String> list = new LinkedList<String>();
			list.add(getPrefix(uri));
			return list.iterator();
		}
	}

	private static class ConversionResults
	{
		private String svg;
		private String mathMl;

		private ConversionResults(String svg, String mathMl)
		{
			this.svg = svg;
			this.mathMl = mathMl;
		}

		public String getSvg()
		{
			return svg;
		}

		public String getMathMl()
		{
			return mathMl;
		}
	}

	private enum EquationType
	{
		TEX("TeX"), MATHML("MathML"), INLINE_TEX("inline-TeX");

		private String name;
		EquationType(String name)
		{
			this.name = name;
		}

		public String getName()
		{
			return this.name;
		}
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
	 * @param type Equation type
	 * @param value Equation text
	 * @param bool If true, uses display mode (for TeX)
	 * @return Converted data
	 * @throws IOException Error running MathJax
	 * @throws MathJaxException MathJax reports an error
	 */
	private synchronized ConversionResults convertEquation(EquationType type,
		String value)
		throws IOException, MathJaxException
	{
		// Start executable if needed.
		if(!isExecutableRunning())
		{
			startExecutable();
		}

		// Send the type value.
		sendLine(type.getName());

		// Strip CRs from value, and ensure there aren't two LFs in a row or any the end.
		value = value.trim().replaceAll("\r", "").replaceAll("\n\n+", "\n");

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

	/**
	 * @return True if it's running
	 */
	private synchronized boolean isExecutableRunning()
	{
		return process != null;
	}

	/**
	 * Starts the executable.
	 * @throws IOException Any problem launching it
	 */
	private synchronized void startExecutable() throws IOException
	{
		process = Runtime.getRuntime().exec(executableParams);
		stdout = new TimeoutReader(process.getInputStream());
		stdin = process.getOutputStream();
	}

	/**
	 * Stops the executable.
	 */
	private synchronized void stopExecutable()
	{
		stdout.requestExit();
		process.destroy();
		process = null;
		stdout.waitForExit();
		stdout = null;
		stdin = null;
	}

	/**
	 * Closes application and clears buffers.
	 */
	public synchronized void close()
	{
		stopExecutable();
	}

	/**
	 * Converts TeX to MathML.
	 * @param tex TeX equation
	 * @param display True if in display mode
	 * @return MathML string
	 */
	public String getMathml(String tex, boolean display) throws MathJaxException, IOException
	{
		try
		{
			System.err.println("---convertStart---");
			return convertEquation(
				display ? EquationType.TEX : EquationType.INLINE_TEX, tex).getMathMl();
		}
		catch(IOException e)
		{
			System.err.println("---Exception---");
			e.printStackTrace();
			TimeoutReader stderr = new TimeoutReader(process.getErrorStream());
			StringBuilder out = new StringBuilder();
			try
			{
				for(int i=0; i<100; i++)
				{
					out.append(stderr.getNextLine(1000) + "\n");
				}
			}
			catch(IOException e2)
			{
			}
			throw new IOException("Message: [" + out + "]");
		}
	}

	private static class TexDetails
	{
		private final static Pattern REGEX_MATHML_TEXANNOTATION = Pattern.compile(
			"^<[^>]*?( display=\"block\")?[^>]*><annotation encoding=\"application/x-tex\">([^<]+)</annotation>");

		private String tex;
		private boolean display;

		private TexDetails(String tex, boolean display)
		{
			this.tex = tex;
			this.display = display;
		}

		public String getTex()
		{
			return tex;
		}

		public boolean isDisplay()
		{
			return display;
		}

		public static TexDetails getFromMathml(Document doc, XPathExpression xpathAnnotation)
		{
			try
			{
				String tex = (String)xpathAnnotation.evaluate(doc, XPathConstants.STRING);
				if(tex.equals(""))
				{
					return null;
				}
				boolean display = "block".equals(doc.getDocumentElement().getAttribute("display"));
				return new TexDetails(tex, display);
			}
			catch(XPathExpressionException e)
			{
				throw new Error(e);
			}
		}

		public static TexDetails getFromMathml(String mathml)
		{
			Matcher m = REGEX_MATHML_TEXANNOTATION.matcher(mathml);
			if(m.matches())
			{
				return new TexDetails(m.group(2), !m.group(1).equals(""));
			}
			else
			{
				return null;
			}
		}
	}

	private final static Pattern REGEX_MATHML_ALTTEXT = Pattern.compile(
		"^<[^>]* alttext=\"([^\">]*)\"");


	public String getEnglishFromMathml(String mathml)
		throws MathJaxException, IOException
	{
		// Parse MathML.
		Document doc = WebMathsService.parseMathml(context, mathml);

		// If there is already alt text, just use that.
		String alt = doc.getDocumentElement().getAttribute("alttext");
		if(!alt.isEmpty())
		{
			return alt;
		}

		// If we can get a TeX equation from the MathML, better use that for conversion.
		TexDetails details = TexDetails.getFromMathml(doc, xpathAnnotation);
		if(details != null)
		{
			return getEnglishFromTex(details.getTex(), details.isDisplay());
		}

		// Convert the MathML.
		ConversionResults results = convertEquation(EquationType.MATHML, mathml);
		Document svgDoc = WebMathsService.parseXml(context, results.getSvg());
		try
		{
			return (String)xpathSvgDesc.evaluate(svgDoc, XPathConstants.STRING);
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}
	}

	public String getEnglishFromTex(String tex, boolean display) throws MathJaxException, IOException
	{
		ConversionResults results = convertEquation(
			display ? EquationType.TEX : EquationType.INLINE_TEX, tex);
		Document svgDoc = WebMathsService.parseXml(context, results.getSvg());
		try
		{
			return (String)xpathSvgDesc.evaluate(svgDoc, XPathConstants.STRING);
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}
	}
}
