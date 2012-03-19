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

Copyright 2011 The Open University
*/
package uk.ac.open.lts.webmaths.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.*;

import javax.imageio.ImageIO;
import javax.jws.WebService;

import net.sourceforge.jeuclid.DOMBuilder;
import net.sourceforge.jeuclid.context.*;
import net.sourceforge.jeuclid.elements.generic.DocumentElement;
import net.sourceforge.jeuclid.layout.JEuclidView;

import org.w3c.dom.*;
import org.xml.sax.SAXParseException;

import uk.ac.open.lts.webmaths.WebMathsService;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.image.MathsImagePort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsImage", portName="MathsImagePort")
public class WebMathsImage extends WebMathsService implements MathsImagePort
{
	private static boolean SHOWPERFORMANCE = false;

	private Graphics2D context;

	/**
	 * Initialises the graphics context (first time).
	 */
	private void initContext()
	{
		if(context == null)
		{
			// Create graphics context used for laying out equation
			BufferedImage silly = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
			context = silly.createGraphics();
		}
	}

	private static final Pattern REGEX_RGB = Pattern.compile(
		"^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$");

	private final static byte[] EMPTY = new byte[0];

	@Override
	public MathsImageReturn getImage(MathsImageParams params)
	{
		long start = System.currentTimeMillis();
		MathsImageReturn result = new MathsImageReturn();
		result.setOk(false);
		result.setImage(EMPTY);
		result.setError("");

		try
		{
			// Parse XML
			Document mathml = parseMathml(params, result, start);
			if(mathml == null)
			{
				return result;
			}
			return getImage(params, mathml, result, start);
		}
		catch(Throwable t)
		{
			result.setError("MathML unexpected error - " + t.getMessage());
			t.printStackTrace();
			return result;
		}
	}

	/**
	 * Parses mathml from the input into a DOM document. Split out so that
	 * subclass can call.
	 * @param params Parameters including MathML
	 * @param result Blank result object
	 * @param start Request start time (milliseconds since epoch)
	 * @return Document or null if failed (in which case should return result)
	 */
	protected Document parseMathml(MathsImageParams params, MathsImageReturn result,
		long start) throws Exception
	{
		try
		{
			Document mathml = parseMathml(params.getMathml());
			if(SHOWPERFORMANCE)
			{
				System.err.println("Parse DOM: " + (System.currentTimeMillis() - start));
			}
			return mathml;
		}
		catch(SAXParseException e)
		{
			int line = e.getLineNumber(), col = e.getColumnNumber();
			result.setError("MathML parse error at " + line + ":" + col + " - "
				+ e.getMessage());
			return null;
		}
	}

	/**
	 * Converts a colour from #rrggbb to Java Color object.
	 * @param rgb String e.g. #ff00ee
	 * @return Colour
	 * @throws IllegalArgumentException If colour string is not valid
	 */
	protected Color convertRgb(String rgb) throws IllegalArgumentException
	{
		// Get colour parameter
		Matcher m = REGEX_RGB.matcher(rgb);
		if(!m.matches())
		{
			throw new IllegalArgumentException("MathML invalid colour '" + rgb +
				"'; expected #rrggbb (lower-case)");
		}
		return new Color(Integer.parseInt(m.group(1), 16),
			Integer.parseInt(m.group(2), 16), Integer.parseInt(m.group(3), 16));
	}

	/**
	 * Does real work; separated out so it can be efficiently called from
	 * subclass.
	 * @param params Request parameters
	 * @param doc MathML as DOM document
	 * @param result Initialised result object with blank fields
	 * @param start Start time of request (milliseconds since epoch)
	 * @return Return result
	 * @throws IOException Any error creating image file
	 */
	protected MathsImageReturn getImage(MathsImageParams params, Document doc,
		MathsImageReturn result, long start) throws IOException
	{
		initContext();

		Color fg;
		try
		{
			fg = convertRgb(params.getRgb());
		}
		catch(IllegalArgumentException e)
		{
			result.setError(e.getMessage());
			return result;
		}

		if(SHOWPERFORMANCE)
		{
			System.err.println("Setup: " + (System.currentTimeMillis() - start));
		}

		// Parse XML to JEuclid document
		DocumentElement document;
		preprocessForJEuclid(doc);
		document = DOMBuilder.getInstance().createJeuclidDom(doc);
		if(SHOWPERFORMANCE)
		{
			System.err.println("Parse: " + (System.currentTimeMillis() - start));
		}

		// Set layout options
		LayoutContextImpl layout = new LayoutContextImpl(
			LayoutContextImpl.getDefaultLayoutContext());
		layout.setParameter(Parameter.ANTIALIAS, Boolean.TRUE);
		// This size is hardcoded to go well with our default text size
		// and be one of the sizes that doesn't look too horrible.
		layout.setParameter(Parameter.MATHSIZE, params.getSize() * 15f);
		layout.setParameter(Parameter.SCRIPTSIZEMULTIPLIER, 0.86667f);
		layout.setParameter(Parameter.MATHCOLOR, fg);

		// These fonts are included with the JEuclid build so ought to work
		layout.setParameter(Parameter.FONTS_SERIF,
			Arrays.asList(new String[] {"DejaVu Serif", "Quivira"}));
		layout.setParameter(Parameter.FONTS_SCRIPT,
			Arrays.asList(new String[] {"Allura"}));
		layout.setParameter(Parameter.FONTS_SANSSERIF, "DejaVu Sans");
		layout.setParameter(Parameter.FONTS_MONOSPACED, "DejaVu Sans Mono");

		if(SHOWPERFORMANCE)
		{
			System.err.println("Layout: " + (System.currentTimeMillis() - start));
		}

		// Layout equation
		JEuclidView view = new JEuclidView(document, layout, context);
		float ascent = view.getAscentHeight();
		float descent = view.getDescentHeight();
		float width = view.getWidth();
		if(SHOWPERFORMANCE)
		{
			System.err.println("View: " + (System.currentTimeMillis() - start));
		}

		// Create new image to hold it
		int pixelWidth = Math.max(1, (int)Math.ceil(width)),
			pixelHeight = Math.max(1, (int)Math.ceil(ascent + descent));

		BufferedImage image = new BufferedImage(pixelWidth, pixelHeight,
			BufferedImage.TYPE_INT_ARGB);
		if(SHOWPERFORMANCE)
		{
			System.err.println("Image: " + (System.currentTimeMillis() - start));
		}
		view.draw(image.createGraphics(), 0, ascent);
		if(SHOWPERFORMANCE)
		{
			System.err.println("Draw: " + (System.currentTimeMillis() - start));
		}
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "png", output);
		if(SHOWPERFORMANCE)
		{
			System.err.println("PNG: " + (System.currentTimeMillis() - start));
		}

		// Save results
		result.setImage(output.toByteArray());
		result.setBaseline(BigInteger.valueOf(image.getHeight()
			- (int)Math.round(ascent)));
		result.setOk(true);

		if(SHOWPERFORMANCE)
		{
			System.err.println("End: " + (System.currentTimeMillis() - start));
		}
		return result;
	}

	/**
	 * Carries out preprocessing that makes JEuclid handle the document better.
	 * @param doc Document
	 */
	static void preprocessForJEuclid(Document doc)
	{
		// underbrace and overbrace
		NodeList list = doc.getElementsByTagName("mo");
		for(int i=0; i<list.getLength(); i++)
		{
			Element mo = (Element)list.item(i);
			String parentName = ((Element)mo.getParentNode()).getTagName();
			if(parentName == null)
			{
				continue;
			}
			if(parentName.equals("munder") && isTextChild(mo, "\ufe38"))
			{
				mo.setAttribute("stretchy", "true");
				mo.removeChild(mo.getFirstChild());
				mo.appendChild(doc.createTextNode("\u23df"));
			}
			else if(parentName.equals("mover") && isTextChild(mo, "\ufe37"))
			{
				mo.setAttribute("stretchy", "true");
				mo.removeChild(mo.getFirstChild());
				mo.appendChild(doc.createTextNode("\u23de"));
			}
		}
	}

	private static boolean isTextChild(Node parent, String text)
	{
		NodeList list = parent.getChildNodes();
		if(list.getLength() != 1)
		{
			return false;
		}
		Node child = list.item(0);
		if(child.getNodeType() != Node.TEXT_NODE)
		{
			return false;
		}
		return child.getNodeValue().equals(text);
	}
}
