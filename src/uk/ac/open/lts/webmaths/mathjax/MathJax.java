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
import java.util.regex.*;

import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import javax.xml.xpath.*;

import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.transcoder.*;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.fop.render.ps.EPSTranscoder;
import org.apache.xmlgraphics.image.codec.png.PNGEncodeParam;
import org.w3c.dom.*;
import org.w3c.dom.ls.*;

import com.sun.org.apache.xerces.internal.xni.QName;

import uk.ac.open.lts.webmaths.WebMathsService;
import uk.ac.open.lts.webmaths.mathjax.MathJaxNodeExecutable.ConversionResults;

/**
 * Carries out transformations using MathJax.node via an application copied to
 * the server.
 */
public class MathJax
{
	/** Default ex size (in pixels) */
	public static final double DEFAULT_EX_SIZE = 7.26667;

	/** Name of attribute in ServletContext that stores singleton value. */
	private static final String ATTRIBUTE_NAME = "uk.ac.open.lts.webmaths.MathJax";

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

	private ServletContext context;

	private MathJaxNodeExecutable mjNode;

	private final XPath xpath;
	private final XPathExpression xpathAnnotation, xpathSvgDesc;

	/**
	 * Starts application.
	 * @param servletContext Servlet context
	 * @throws IOException Any problem launching the application
	 */
	private MathJax(ServletContext servletContext)
	{
		// Set up the executable
		mjNode = new MathJaxNodeExecutable(servletContext);

		// Precompile the xpath expressions.
		xpath = XPathFactory.newInstance().newXPath();
		xpath.setNamespaceContext(new MathmlAndSvgNamespaceContext());
		try
		{
			xpathAnnotation = InputTexEquation.getXPathExpression(xpath);
			xpathSvgDesc = xpath.compile("normalize-space(/s:svg/s:desc)");
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}
	}

	/**
	 * Closes application and clears buffers.
	 */
	public synchronized void close()
	{
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
		return mjNode.convertEquation(eq).getMathMl();
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
	 * @throws IOException If any error extracting text
	 */
	public String getEnglishFromSvg(String svg)
		throws IOException
	{
		Document svgDoc = WebMathsService.parseXml(context, svg);
		try
		{
			return (String)xpathSvgDesc.evaluate(svgDoc, XPathConstants.STRING);
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}
	}

	/** Parameter for {@link #getSvg(InputEquation, float)} when using ex sizes */
	public final static double SIZE_IN_EX = -1.0;

	/** MathJax vertical-align value appears to be off by 0.07ex. */
	private final static double MATHJAX_BASELINE_FUDGE = 0.07;

	private final static Pattern REGEX_BASELINE = Pattern.compile(
		"^<svg[^>]* style=\"vertical-align: ((-?[0-9]+(?:\\.[0-9]+)?)ex)");
	private final static Pattern REGEX_BASELINE_PIXELS = Pattern.compile(
		"^<svg[^>]* style=\"vertical-align: ((-?[0-9]+(?:\\.[0-9]+)?)px)");
	private final static Pattern REGEX_WIDTH = Pattern.compile(
		"^<svg[^>]* width=\"(([0-9]+(?:\\.[0-9]+)?)ex)\"");
	private final static Pattern REGEX_HEIGHT = Pattern.compile(
		"^<svg[^>]* height=\"(([0-9]+(?:\\.[0-9]+)?)ex)\"");
	private final static Pattern REGEX_COLOUR = Pattern.compile(
		"(<[^>]+ )stroke=\"black\" fill=\"black\"");

	/**
	 * Gets SVG for an input equation.
	 * <p>
	 * You can optionally convert size from 'ex' into pixels. If no conversion
	 * is required, use SIZE_IN_EX for the float parameter.
	 * <p>
	 * Note that the returned SVG contains two IDs MathJax-SVG-1-Title and
	 * MathJax-SVG-1-Desc. If included on a web page, these should be string
	 * replaced with suitable unique IDs.
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
		// When correcting the baseline, bodge in a 1 at the start.
		if(correctBaseline)
		{
			if(eq instanceof InputTexDisplayEquation)
			{
				eq = new InputTexDisplayEquation("1" + eq.getContent());
			}
			else if(eq instanceof InputTexInlineEquation)
			{
				eq = new InputTexInlineEquation("1" + eq.getContent());
			}
			else
			{
				eq = new InputMathmlEquation(eq.getContent().replaceAll(
					"^(.*?)>\\s*(<semantics>)?", "$0<mn>1</mn>"));
			}
		}
		String svg = mjNode.convertEquation(eq).getSvg();
		if(correctBaseline)
		{
			// Parse the svg and mess with it.
			try
			{
				// Parse.
				Document svgDom = WebMathsService.parseXml(context, svg);

//				// Find the first 'use' and remove it.
//				Node n = (Node)xpath.compile("//s:use[1]").evaluate(svgDom, XPathConstants.NODE);
//				n.getParentNode().removeChild(n);
//
//				// Find the desc and remove the 1 from it.
//				Element e = (Element)xpath.compile("//s:desc[1]").evaluate(svgDom, XPathConstants.NODE);
//				String desc = e.getFirstChild().getNodeValue();
//				System.err.println("[[" + desc + "]]");
//				e.removeChild(e.getFirstChild());
//				e.appendChild(svgDom.createTextNode(desc.replaceFirst("^1 ", "")));

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
				String viewX = m.group(1), viewWidth = m.group(3);

				// Now get the height in ex.
				m = Pattern.compile("([0-9.]+)ex").matcher(root.getAttribute("height"));
				if(!m.matches())
				{
					throw new IOException("Unexpected SVG format (height)");
				}
				double heightEx = Double.parseDouble(m.group(1));

				// We now can calculate baseline in ex.
				double baselineEx = (((viewY + viewHeight - 5) / viewHeight) * heightEx);

				// If we know pixels, I'm going to make this an exact number of pixels
				// by slightly increasing the height of the equation.
				if (exSize != SIZE_IN_EX)
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
					baselineEx = (((viewY + viewHeight - 5) / viewHeight) * heightEx);
					double baselinePixels = baselineEx * exSize;
					heightOffsetPixels = Math.ceil(baselinePixels) - baselinePixels;
					heightOffsetEx = heightOffsetPixels / exSize;

					oldHeightEx = heightEx;
					heightEx += heightOffsetEx;
					root.setAttribute("height", String.format("%.4f", heightEx) + "ex");
					viewHeight = (viewHeight / oldHeightEx) * heightEx;
					root.setAttribute("viewBox", viewX + " " + String.format("%.4f", viewY) + " " +
						viewWidth + " " + String.format("%.4f", viewHeight));
					baselineEx = (((viewY + viewHeight - 5) / viewHeight) * heightEx);
				}

				// Replace current value in the style attribute.
				String style = root.getAttribute("style");
				style = style.replaceFirst("vertical-align: -?[0-9.]+",
					"vertical-align: " + String.format("%.4f", -baselineEx));
				// Get rid of the 1px margin, it throws off the calculation.
				style = style.replaceAll("margin-(top|bottom): 1px", "margin-$1: 0px");
				root.setAttribute("style", style);

//				double x = Double.parseDouble(m.group(1)), width = Double.parseDouble(m.group(3));

//				// Add 505 to the start co-ordinate and subtract from width.
//				e.setAttribute("viewBox",
//					String.format("%.1f", x + 505.0) + " " + m.group(2) + " " +
//					String.format("%.1f", width - 505.0) + " " + m.group(4));
//
//				// Change the overall width (in ex) proportionally.
//				m = Pattern.compile("([0-9.]+)ex").matcher(e.getAttribute("width"));
//				if(!m.matches())
//				{
//					throw new IOException("Unexpected SVG format (width)");
//				}
//				double widthEx = Double.parseDouble(m.group(1));
//				e.setAttribute("width", String.format("%.4f", widthEx * (width - 505.0) / width) + "ex");

				// Write back to a file.
				DOMImplementationLS domImplementation = (DOMImplementationLS)svgDom.getImplementation();
				LSSerializer lsSerializer = domImplementation.createLSSerializer();
				lsSerializer.getDomConfig().setParameter("xml-declaration", false);
				svg = lsSerializer.writeToString(svgDom);
				System.err.println(svg);
			}
			catch(Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


		boolean convertToPixels = exSize != SIZE_IN_EX;
		{
			Matcher m = REGEX_BASELINE.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected baseline pattern");
			}
			double baseline = Double.parseDouble(m.group(2));
			if(correctBaseline && false)
			{
				baseline += MATHJAX_BASELINE_FUDGE;
			}
			String unit = "ex";
			if(convertToPixels)
			{
				baseline *= exSize;
				unit = "px";
			}
			svg = svg.substring(0, m.start(1)) + String.format("%.4f", baseline) + unit +
				svg.substring(m.end(1));
		}
		if(convertToPixels)
		{
			Matcher m = REGEX_WIDTH.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected width pattern");
			}
			double width = Double.parseDouble(m.group(2));
			svg = svg.substring(0, m.start(1)) +
				String.format("%.4f", width * exSize) + "px" +
				svg.substring(m.end(1));

			m = REGEX_HEIGHT.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected height pattern");
			}
			double height = Double.parseDouble(m.group(2));
			svg = svg.substring(0, m.start(1)) +
				String.format("%.4f", height * exSize) + "px" +
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
	 * @throws IOException If it can't be found
	 */
	public double getBaselineFromSvg(String svg)
		throws IOException
	{
		Matcher m = REGEX_BASELINE_PIXELS.matcher(svg);
		if(!m.find())
		{
			throw new IOException("Unexpected failure detecting baseline");
		}
		return Double.parseDouble(m.group(2)) * -1;
	}

	/**
	 * Gets PNG from an SVG image. The SVG must have been converted to pixels.
	 * @param svg SVG (pixel format)
	 * @return PNG data
	 * @throws IOException Any error processing
	 */
	public byte[] getPngFromSvg(String svg) throws IOException
	{
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

	public byte[] getEps(InputEquation eq)
		throws MathJaxException, IOException
	{
		double ex = 7.26667;
		String svg = getSvg(eq, true, ex, null);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		EPSTranscoder transcoder = new EPSTranscoder();
		// This arbitrary size makes it appear with the same x height as text font.
		transcoder.addTranscodingHint(EPSTranscoder.KEY_PIXEL_UNIT_TO_MILLIMETER,
			new Float(0.30900239f));
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
}
