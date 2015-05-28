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
package uk.ac.open.lts.webmaths.mathjax;

import java.awt.RenderingHints;
import java.io.*;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.xpath.*;

import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.render.ps.EPSTranscoder;
import org.w3c.dom.*;
import org.w3c.dom.ls.*;

import uk.ac.open.lts.webmaths.WebMathsService;
import uk.ac.open.lts.webmaths.mathjax.MathJaxNodeExecutable.ConversionResults;
import uk.ac.open.lts.webmaths.mathjax.MathJaxNodeExecutable.Status;

/**
 * Carries out transformations using MathJax.node via an application copied to
 * the server.
 */
public class MathJax
{
	/** When rendering PNGs, we offset the SVG by this many pixels first. */
	public static final double PNG_OFFSET = 0.5;

	/** Default ex size (in pixels) */
	public static final double DEFAULT_EX_SIZE = 7.26667;

	/** Name of attribute in ServletContext that stores singleton value. */
	private static final String ATTRIBUTE_NAME = "uk.ac.open.lts.webmaths.MathJax";

	/** Ratio to use for converting size to ex */
	private static final double CORRECT_DRAWING_UNITS_PER_EX = 428;

	/**
	 * Gets MathJax singleton, starting it if not already running.
	 * @param context Web service context
	 * @return Singleton
	 */
	public synchronized static MathJax get(WebServiceContext context)
	{
		return get((ServletContext)context.getMessageContext().get(
			MessageContext.SERVLET_CONTEXT));
	}

	/**
	 * Gets MathJax singleton, starting it if not already running.
	 * @param context Servlet context
	 * @return Singleton
	 */
	public synchronized static MathJax get(ServletContext servletContext)
	{
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

	private ServletContext context;

	private MathJaxNodeExecutable mjNode;

	private final XPath xpath;
	private final XPathExpression xpathAnnotation, xpathSvgDesc, xpathSvgTitle, xpathNormalizeSpace;

	private ExecutorService threadPool;

	/**
	 * Constructor.
	 * @param servletContext Servlet context
	 * @throws IOException Any problem launching the application
	 */
	protected MathJax(ServletContext servletContext)
	{
		// Set up the executable and thread pool.
		mjNode = createExecutable(servletContext);
		threadPool = Executors.newFixedThreadPool(mjNode.getMaxInstances());

		// Precompile the xpath expressions.
		xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new MathmlAndSvgNamespaceContext());
		try
		{
			xpathAnnotation = InputTexEquation.getXPathExpression(xpath);
			xpathSvgDesc = xpath.compile("/s:svg/s:desc");
			xpathSvgTitle = xpath.compile("/s:svg/s:title");
			xpathNormalizeSpace = xpath.compile("normalize-space(.)");
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}
	}

	/**
	 * Create the executable object. This is a separate function so that it can be
	 * overridden (with a mock) for unit testing.
	 * @param servletContext Servlet context
	 * @return Created executable
	 */
	protected MathJaxNodeExecutable createExecutable(ServletContext servletContext)
	{
		return new MathJaxNodeExecutable(servletContext);
	}

	/**
	 * Closes application and clears buffers.
	 */
	public synchronized void close()
	{
		try
		{
			threadPool.shutdown();
			threadPool.awaitTermination(10, TimeUnit.SECONDS);
			threadPool = null;
		}
		catch(InterruptedException e)
		{
		}
		mjNode.close();
		mjNode = null;
	}

	/**
	 * Converts TeX to MathML.
	 * @param eq TeX equation
	 * @return MathML string
	 * @throws MathJaxException Error processing equation
	 * @throws IOException Other error
	 */
	public String getMathml(InputTexEquation eq) throws MathJaxException, IOException
	{
		return mjNode.convertEquation(eq).getMathml();
	}

	/**
	 * Extracts English text from a TeX or MathML input equation.
	 * @param eq Equation
	 * @return English text alternative
	 * @throws MathJaxException Error processing equation
	 * @throws IOException Other error
	 */
	public String getEnglish(InputEquation eq)
		throws MathJaxException, IOException
	{
		if (eq instanceof InputMathmlEquation)
		{
			// Parse MathML.
			Document doc = WebMathsService.parseMathml(context, eq.getContent());

			// If there is already alt text, just use that.
			String alt = doc.getDocumentElement().getAttribute("alttext");
			if(!alt.isEmpty())
			{
				return alt;
			}

			// If we can get a TeX equation from the MathML, better use that for conversion.
			InputTexEquation tex = InputTexEquation.getFromMathml(doc, xpathAnnotation);
			if(tex != null)
			{
				return getEnglish(tex);
			}
		}

		// Convert the equation and get text from SVG.
		ConversionResults results = mjNode.convertEquation(eq);
		return getEnglishFromSvg(results.getSvg());
	}

	/**
	 * Gets English text from an already-obtained SVG.
	 * @param svg SVG code
	 * @return English text
	 * @throws IOException If any error parsing SVG
	 * @throws IllegalArgumentException If there isn't a description in the SVG
	 */
	public String getEnglishFromSvg(String svg)
		throws IOException, IllegalArgumentException
	{
		Document svgDoc = WebMathsService.parseXml(context, svg);
		try
		{
			Node result = (Node)xpathSvgDesc.evaluate(svgDoc, XPathConstants.NODE);
			if(result == null)
			{
				throw new IllegalArgumentException("SVG does not include <desc>");
			}

			return (String)xpathNormalizeSpace.evaluate(result, XPathConstants.STRING);
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}
	}

	/** Parameter for {@link #getSvg(InputEquation, float)} when using ex sizes */
	public final static double SIZE_IN_EX = -1.0;

	private final static Pattern REGEX_BASELINE_EX = Pattern.compile(
		"^<svg[^>]* style=\"vertical-align: ((-?[0-9]+(?:\\.[0-9]+)?)ex)");
	private final static Pattern REGEX_BASELINE_PIXELS = Pattern.compile(
		"^<svg[^>]* style=\"vertical-align: ((-?[0-9]+(?:\\.[0-9]+)?)px)");
	private final static Pattern REGEX_WIDTH = Pattern.compile(
		"^<svg[^>]* width=\"(([0-9]+(?:\\.[0-9]+)?)ex)\"");
	private final static Pattern REGEX_HEIGHT = Pattern.compile(
		"^<svg[^>]* height=\"(([0-9]+(?:\\.[0-9]+)?)ex)\"");
	private final static Pattern REGEX_VIEWBOX = Pattern.compile(
		"^<svg[^>]* viewBox=\"(-?[0-9.]+) (-?[0-9.]+) (-?[0-9.]+) (-?[0-9.]+)\"");
	private final static Pattern REGEX_HEIGHT_PX = Pattern.compile(
		"^<svg[^>]* height=\"(([0-9]+(?:\\.[0-9]+)?)px)\"");
	private final static Pattern REGEX_COLOUR = Pattern.compile(
		"(<[^>]+ )(stroke=\"black\" fill=\"black\"|fill=\"black\" stroke=\"black\")");

	/**
	 * Rounds numbers suitable for use in SVG. They are rounded to 4 digits but
	 * show as integers if they are integers.
	 * @param number Number
	 * @return Rounded form
	 */
	private static String round(double number)
	{
		String formatted = String.format("%.4f", number);
		return formatted.replaceFirst("\\.0+$", "");
	}

	/**
	 * Gets SVG for an input equation.
	 * <p>
	 * You can optionally convert size from 'ex' into pixels. If no conversion
	 * is required, use SIZE_IN_EX for the float parameter.
	 * <p>
	 * Note that the returned SVG contains two IDs MathJax-SVG-1-Title and
	 * MathJax-SVG-1-Desc. If included on a web page, these should be string
	 * replaced with suitable unique IDs.
	 * <p>
	 * When correcting the baseline, width and height are also corrected. (MathJax
	 * sometimes uses an incorrect ratio of drawing units to ex.)
	 * @param eq Equation
	 * @param correctBaseline If true, adjusts the reported baseline which is wrong
	 * @param exSize SIZE_IN_EX or ex size in pixels
	 * @param rgb Colour code or null to leave as-is
	 * @return SVG as text
	 * @throws MathJaxException Error processing equation
	 * @throws IOException Other error
	 */
	public String getSvg(InputEquation eq, boolean correctBaseline, double exSize, String rgb)
		throws MathJaxException, IOException
	{
		boolean convertToPixels = exSize != SIZE_IN_EX;
		String svg = mjNode.convertEquation(eq).getSvg();

		double correctedHeight = 0.0, correctedWidth = 0.0, correctedBaseline = 0.0;

		try
		{
			// Parse SVG.
			Document svgDom = WebMathsService.parseXml(context, svg);

			// Get the view box.
			Element root = svgDom.getDocumentElement();
			String viewBox = root.getAttribute("viewBox");
			Matcher m = Pattern.compile("(-?[0-9.]+) (-?[0-9.]+) (-?[0-9.]+) (-?[0-9.]+)").matcher(viewBox);
			if(!m.matches())
			{
				throw new IOException("Unexpected SVG format (viewBox)");
			}

			// Get viewbox Y and height.
			double viewY = Double.parseDouble(m.group(2));
			double viewHeight = Double.parseDouble(m.group(4));
			double viewX = Double.parseDouble(m.group(1));
			double viewWidth = Double.parseDouble(m.group(3));

			// In some cases, this has a bogus width of 1 million units.
			if(viewWidth >= 999999.9)
			{
				throw new MathJaxException("Unbounded equation (use of \\\\ outside suitable environment?)");
			}

			if(correctBaseline)
			{
				// Now get the height in ex - this used to read it from the file but
				// we now hardcoded it based on drawing units, because MathJax.Node
				// returns inconsistent results e.g. for the equations "xqx" and "xxx".
				double heightEx = viewHeight / CORRECT_DRAWING_UNITS_PER_EX;
				double widthEx = viewWidth / CORRECT_DRAWING_UNITS_PER_EX;

				// We now can calculate baseline in ex.
				double baselineEx = ((viewY + viewHeight) / viewHeight) * heightEx;

				// If we know pixels, I'm going to make this an exact number of pixels
				// by slightly increasing the height of the equation.
				if (convertToPixels)
				{
					// First make the size from top to baseline into an even number of pixels.
					double ascentPixels = (-viewY / viewHeight) * heightEx * exSize;
					double heightOffsetPixels = Math.ceil(ascentPixels) - ascentPixels;
					double heightOffsetEx = heightOffsetPixels / exSize;
					double oldHeightEx = heightEx;
					heightEx += heightOffsetEx;
					double oldViewHeight = viewHeight;
					viewHeight = (viewHeight / oldHeightEx) * heightEx;
					viewY -= (viewHeight - oldViewHeight);

					// Next make baseline to bottom into an even number.
					baselineEx = (((viewY + viewHeight) / viewHeight) * heightEx);
					double baselinePixels = baselineEx * exSize;
					heightOffsetPixels = Math.ceil(baselinePixels) - baselinePixels;
					heightOffsetEx = heightOffsetPixels / exSize;

					oldHeightEx = heightEx;
					heightEx += heightOffsetEx;
					root.setAttribute("height", round(heightEx) + "ex");
					viewHeight = (viewHeight / oldHeightEx) * heightEx;
					root.setAttribute("viewBox", viewX + " " + round(viewY) + " " +
						viewWidth + " " + round(viewHeight));
					baselineEx = ((viewY + viewHeight) / viewHeight) * heightEx;
				}

				// Replace current value in the style attribute.
				String style = root.getAttribute("style");
				style = style.replaceFirst("vertical-align: -?[0-9.]+",
					"vertical-align: " + round(-baselineEx));
				// Get rid of the 1px margin, it throws off the calculation.
				style = style.replaceAll("margin-(top|bottom): 1px", "margin-$1: 0px");
				root.setAttribute("style", style);

				// Remember the precise figures for next calculation.
				correctedHeight = heightEx;
				correctedWidth = widthEx;
				correctedBaseline = -baselineEx;
			}

			// I don't like the way the title is always set to 'Equation' (and it
			// appears as a popup in Firefox) so we are going to remove it.
			Node result = (Node)xpathSvgTitle.evaluate(svgDom, XPathConstants.NODE);
			if(result == null)
			{
				throw new IllegalStateException("SVG does not include <title>");
			}
			result.getParentNode().removeChild(result);

			// And remove the reference to it in the ARIA attribute.
			svgDom.getDocumentElement().setAttribute("aria-labelledby", "MathJax-SVG-1-Desc");

			// Write back to the serialized version.
			DOMImplementationLS domImplementation = (DOMImplementationLS)svgDom.getImplementation();
			LSSerializer lsSerializer = domImplementation.createLSSerializer();
			lsSerializer.getDomConfig().setParameter("xml-declaration", false);
			svg = lsSerializer.writeToString(svgDom);
		}
		catch(Exception e)
		{
			if(e instanceof MathJaxException)
			{
				throw (MathJaxException)e;
			}
			throw new IOException(e);
		}

		if(convertToPixels)
		{
			Matcher m = REGEX_BASELINE_EX.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected baseline pattern");
			}
			double baseline = correctBaseline ? correctedBaseline : Double.parseDouble(m.group(2));
			baseline *= exSize;
			svg = svg.substring(0, m.start(1)) + round(baseline) + "px" +
				svg.substring(m.end(1));

			m = REGEX_WIDTH.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected width pattern");
			}
			double width = correctBaseline ? correctedWidth : Double.parseDouble(m.group(2));
			svg = svg.substring(0, m.start(1)) + round(width * exSize) + "px" +
				svg.substring(m.end(1));

			m = REGEX_HEIGHT.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected height pattern");
			}
			double height = correctBaseline ? correctedHeight : Double.parseDouble(m.group(2));
			svg = svg.substring(0, m.start(1)) + round(height * exSize) + "px" +
				svg.substring(m.end(1));
		}

		if(rgb != null)
		{
			svg = recolourSvg(svg, rgb);
		}

		return svg;
	}

	/**
	 * Obtains a PNG transcoder that uses high quality settings.
	 * @return Transcoder with settings improved
	 */
	private PNGTranscoder createTranscoder()
	{
    return new PNGTranscoder()
    {
      @Override
      protected ImageRenderer createRenderer()
      {
        ImageRenderer r = super.createRenderer();

        RenderingHints rh = r.getRenderingHints();

        rh.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
        rh.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC));

        rh.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON));

        rh.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING,
            RenderingHints.VALUE_COLOR_RENDER_QUALITY));
        rh.add(new RenderingHints(RenderingHints.KEY_DITHERING,
            RenderingHints.VALUE_DITHER_DISABLE));

        rh.add(new RenderingHints(RenderingHints.KEY_RENDERING,
            RenderingHints.VALUE_RENDER_QUALITY));

        rh.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE));

        r.setRenderingHints(rh);

        return r;
      }
    };
  }

	/**
	 * Recolours an SVG file by changing black to the given colour.
	 * @param svg SVG file
	 * @param rgb Colour code
	 * @return Recoloured SVG
	 */
	private String recolourSvg(String svg, String rgb)
	{
		if(!rgb.matches("#[0-9a-f]{6}"))
		{
			throw new IllegalArgumentException("Invalid RGB colour (must match #000000): " + rgb);
		}
		Matcher m = REGEX_COLOUR.matcher(svg);
		StringBuffer out = new StringBuffer();
		while(m.find())
		{
			String replace = m.group(1) + "stroke=\"" + rgb + "\" fill=\"" + rgb + "\"";
			m.appendReplacement(out, replace);
		}
		m.appendTail(out);
		return out.toString();
	}

	/**
	 * Gets baseline from an SVG image. The SVG must have been converted to pixels.
	 * @param svg SVG (pixel format)
	 * @return Baseline
	 * @throws IllegalArgumentException If it can't be found
	 */
	public double getPxBaselineFromSvg(String svg)
		throws IllegalArgumentException
	{
		Matcher m = REGEX_BASELINE_PIXELS.matcher(svg);
		if(!m.find())
		{
			throw new IllegalArgumentException("Unexpected failure detecting baseline");
		}
		return Double.parseDouble(m.group(2)) * -1;
	}

	/**
	 * Gets baseline from an SVG image. The SVG must be in ex.
	 * @param svg SVG (ex format)
	 * @return Baseline
	 * @throws IllegalArgumentException If it can't be found
	 */
	public double getExBaselineFromSvg(String svg)
		throws IllegalArgumentException
	{
		Matcher m = REGEX_BASELINE_EX.matcher(svg);
		if(!m.find())
		{
			throw new IllegalArgumentException("Unexpected failure detecting baseline");
		}
		return Double.parseDouble(m.group(2)) * -1;
	}

	/**
	 * Offsets an SVG by changing the view box and height.
	 * @param svg SVG (in pixel format)
	 * @param pixels Number of pixels to offset (+ve = move up)
	 * @return Offset SVG
	 * @throws IllegalArgumentException Invalid/unexpected SVG format
	 */
	public static String offsetSvg(String svg, double pixels) throws IllegalArgumentException
	{
		if(pixels == 0.0)
		{
			return svg;
		}
		// Get height.
		Matcher heightMatcher = REGEX_HEIGHT_PX.matcher(svg);
		if(!heightMatcher.find())
		{
			throw new IllegalArgumentException("Unexpected SVG format (no height in px)");
		}
		double height = Double.parseDouble(heightMatcher.group(2));

		// Get viewbox.
		Matcher viewBoxMatcher = REGEX_VIEWBOX.matcher(svg);
		if(!viewBoxMatcher.find())
		{
			throw new IllegalArgumentException("Unexpected SVG format (no viewBox)");
		}

		// Get viewbox Y and height.
		double viewY = Double.parseDouble(viewBoxMatcher.group(2));
		double viewHeight = Double.parseDouble(viewBoxMatcher.group(4));

		// Size increase.
		double newHeightOffset = Math.ceil(Math.abs(pixels));

		viewHeight = viewHeight * (height + newHeightOffset) / height;

		height += newHeightOffset;

		if(pixels > 0)
		{
			// Take the difference between the height change and actual change, and
			// move it up this much.
			viewY -= (newHeightOffset - pixels) * viewHeight / height;
		}
		else
		{
			// Move the viewbox the required amount.
			viewY += pixels * viewHeight / height;

			// Adjust the baseline.
			Matcher baselineMatcher = REGEX_BASELINE_PIXELS.matcher(svg);
			if(!baselineMatcher.find())
			{
				throw new IllegalArgumentException("Unexpected SVG format (no baseline)");
			}
			double baseline = Double.parseDouble(baselineMatcher.group(2));
			baseline += newHeightOffset;
			svg = svg.substring(0, baselineMatcher.start(2)) + round(baseline) +
				svg.substring(baselineMatcher.end(2));

			// Update location of viewbox matcher.
			viewBoxMatcher = REGEX_VIEWBOX.matcher(svg);
			viewBoxMatcher.find();
		}

		// Do the viewbox replace.
		svg = svg.substring(0, viewBoxMatcher.start(2)) +
				round(viewY) +
				svg.substring(viewBoxMatcher.end(2), viewBoxMatcher.start(4)) +
				round(viewHeight) +
				svg.substring(viewBoxMatcher.end(4));

		// Do the height replace
		heightMatcher = REGEX_HEIGHT_PX.matcher(svg);
		heightMatcher.find();
		svg = svg.substring(0, heightMatcher.start(2)) +
				round(height) +
				svg.substring(heightMatcher.end(2));

		return svg;
	}

	/**
	 * Gets PNG from an SVG image. The SVG must have been converted to pixels.
	 * @param svg SVG (pixel format)
	 * @return PNG data
	 * @throws IOException Any error processing
	 */
	public byte[] getPngFromSvg(String svg) throws IOException
	{
		// Offset the SVG slightly, as it renders a fraction lower down than I'd like.
		svg = offsetSvg(svg, PNG_OFFSET);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PNGTranscoder transcoder = createTranscoder();
		try
		{
			transcoder.transcode(new TranscoderInput(new StringReader(svg)),
				new TranscoderOutput(output));
			return output.toByteArray();
		}
		catch(TranscoderException e)
		{
			e.printStackTrace();
			throw new IOException("Transcoder failed", e);
		}
	}

	/**
	 * Gets EPS from an input equation.
	 * @param eq Equation
	 * @return EPS data
	 * @throws MathJaxException If there's a MathJax error processing the equation
	 * @throws IOException Any other problem
	 */
	public byte[] getEps(InputEquation eq, double ex, String rgb)
		throws MathJaxException, IOException
	{
		String svg = getSvg(eq, true, ex, rgb);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		EPSTranscoder transcoder = new EPSTranscoder();
		// There's no logical reason for this size but by trial and error, we found
		// that it matched the size of some PNGs.
		transcoder.addTranscodingHint(EPSTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER,
			new Float(0.247f));
		try
		{
			transcoder.transcode(new TranscoderInput(new StringReader(svg)),
				new TranscoderOutput(output));
		}
		catch(TranscoderException e)
		{
			e.printStackTrace();
			throw new IOException("Transcoder failed", e);
		}
		return output.toByteArray();
	}

	/**
	 * Gets status from the MathJax.Node processor.
	 * @return Status object
	 */
	public Status getStatus()
	{
		return mjNode.getStatus();
	}

	/**
	 * Executes on the thread pool used for MathJax.Node running.
	 * @param runnable Command to execute
	 */
	public void executeOnThreadPool(Runnable runnable)
	{
		threadPool.execute(runnable);
	}
}
