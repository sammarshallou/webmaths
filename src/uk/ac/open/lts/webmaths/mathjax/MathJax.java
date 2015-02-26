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
import org.apache.batik.transcoder.keys.FloatKey;
import org.apache.xmlgraphics.image.codec.png.PNGEncodeParam;
import org.w3c.dom.Document;

import uk.ac.open.lts.webmaths.WebMathsService;
import uk.ac.open.lts.webmaths.mathjax.MathJaxNodeExecutable.ConversionResults;

/**
 * Carries out transformations using MathJax.node via an application copied to
 * the server.
 */
public class MathJax
{
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

	/** Parameter for {@link #getSvg(InputEquation, float)} when using ex sizes */
	public final static double SIZE_IN_EX = -1.0;

	/** MathJax vertical-align value appears to be off by 0.07ex. */
	private final static double MATHJAX_BASELINE_FUDGE = 0.07;

	private final static Pattern REGEX_BASELINE = Pattern.compile(
		"^<svg[^>]* style=\"vertical-align: ((-?[0-9]+(?:\\.[0-9]+)?)ex)");
	private final static Pattern REGEX_BASELINE_PIXELS = Pattern.compile(
		"^<svg[^>]* style=\"vertical-align: ((-?[0-9]+(?:\\.[0-9]+)?)px)");
	private final static Pattern REGEX_DIMENSIONS = Pattern.compile(
		"^<svg[^>]* width=\"(([0-9]+(?:\\.[0-9]+)?)ex)\" height=\"(([0-9]+(?:\\.[0-9]+)?)ex)\"");
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
	 * @return SVG as text
	 * @throws MathJaxException Error processing equation
	 * @throws IOException Other error
	 */
	public String getSvg(InputEquation eq, boolean correctBaseline, double exSize)
		throws MathJaxException, IOException
	{
		String svg = mjNode.convertEquation(eq).getSvg();
		boolean convertToPixels = exSize != SIZE_IN_EX;
		if(correctBaseline)
		{
			Matcher m = REGEX_BASELINE.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected baseline pattern");
			}
			double baseline = Double.parseDouble(m.group(2));
			baseline += MATHJAX_BASELINE_FUDGE;
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
			Matcher m = REGEX_DIMENSIONS.matcher(svg);
			if(!m.find())
			{
				throw new IOException("MathJax SVG does not match expected dimensions pattern");
			}
			double width = Double.parseDouble(m.group(2)),
				height = Double.parseDouble(m.group(4));
			svg = svg.substring(0, m.start(1)) +
				String.format("%.4f", width * exSize) + "px" +
				svg.substring(m.end(1), m.start(3)) +
				String.format("%.4f", height * exSize) + "px" +
				svg.substring(m.end(3));
		}
		return svg;
	}

	/**
	 * Result of a PNG conversion.
	 */
	public static class PngResult
	{
		byte[] png;
		int baseline;

		private PngResult(byte[] png, int baseline)
		{
			this.png = png;
			this.baseline = baseline;
		}

		/**
		 * @return PNG data
		 */
		public byte[] getPng()
		{
			return png;
		}

		/**
		 * @return Baseline in pixels
		 */
		public int getBaseline()
		{
			return baseline;
		}
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
	 * Gets PNG for equation.
	 * @param eq Equation
	 * @param rgb RGB colour e.g. "#000000"
	 * @param size Size (1.0f = normal)
	 * @return Result
	 * @throws MathJaxException Error processing equation
	 * @throws IOException Other error
	 */
	public PngResult getPng(InputEquation eq, String rgb, float size)
		throws MathJaxException, IOException
	{
		double ex = 7.26667 * (double)size;
		String svg = getSvg(eq, true, ex);
		svg = recolourSvg(svg, rgb);

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PNGTranscoder transcoder = createTranscoder();
		transcoder.addTranscodingHint(PNGTranscoder.KEY_GAMMA, new Float(PNGEncodeParam.INTENT_PERCEPTUAL));
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

		Matcher m = REGEX_BASELINE_PIXELS.matcher(svg);
		if(!m.find())
		{
			throw new IOException("Unexpected failure detecting baseline");
		}
		int baseline = (int)Math.round(Double.parseDouble(m.group(2))) * -1;
		return new PngResult(output.toByteArray(), baseline);
	}
}
