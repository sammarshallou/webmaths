package uk.ac.open.lts.webmaths;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.*;

import javax.imageio.ImageIO;
import javax.jws.WebService;
import javax.xml.parsers.*;

import net.sourceforge.jeuclid.DOMBuilder;
import net.sourceforge.jeuclid.context.*;
import net.sourceforge.jeuclid.elements.generic.DocumentElement;
import net.sourceforge.jeuclid.layout.JEuclidView;

import org.w3c.dom.Document;
import org.xml.sax.*;

import uk.ac.open.lts.webmaths.image.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.image.MathsImagePort")
public class WebMathsImage implements MathsImagePort
{
	private MathmlEntityFixer fixer;
	
	private static Graphics2D context;
	static
	{
		// Create graphics context used for laying out equation
		BufferedImage silly = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		context = silly.createGraphics();
	}
	
	private static final Pattern REGEX_RGB = Pattern.compile(
		"^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$");
	
	private final static byte[] EMPTY = new byte[0];
	
	public WebMathsImage() throws IOException
	{
		fixer = new MathmlEntityFixer();
	}
	
	@Override
	public MathsImageReturn getImage(MathsImageParams params)
	{
		long start = System.currentTimeMillis();
		MathsImageReturn result = new MathsImageReturn();
		result.setOk(false);
		result.setError("");
		result.setImage(EMPTY);
		
		try
		{
			// Get colour parameter
			Matcher m = REGEX_RGB.matcher(params.getRgb());
			if(!m.matches())
			{
				result.setError("MathML invalid colour '" + params.getRgb() 
					+ "'; expected #rrggbb (lower-case)");
				return result;
			}
			Color fg = new Color(Integer.parseInt(m.group(1), 16),
				Integer.parseInt(m.group(2), 16), Integer.parseInt(m.group(3), 16));
			
			// Check MathML does not already contain doctype, and add special one
			String mathml = params.getMathml();
			if(mathml.contains("<!DOCTYPE"))
			{
				result.setError("MathML must not include doctype");
				return result;
			}
	
			System.err.println("Setup: " + (System.currentTimeMillis() - start));
		
			// Parse XML to JEuclid document
			DocumentElement document;
			try
			{
				Document doc = parseMathml(mathml);
				System.err.println("Parse DOM: " + (System.currentTimeMillis() - start));
				document = DOMBuilder.getInstance().createJeuclidDom(doc);
			}
			catch(SAXParseException e)
			{
				int line = e.getLineNumber(), col = e.getColumnNumber();
				result.setError("MathML parse error at " + line + ":" + col + " - " 
					+ e.getMessage());
				return result;
			}
			System.err.println("Parse: " + (System.currentTimeMillis() - start));
			
			// Set layout options
			LayoutContextImpl layout = new LayoutContextImpl(
				LayoutContextImpl.getDefaultLayoutContext());
			layout.setParameter(Parameter.ANTIALIAS, Boolean.TRUE);
			layout.setParameter(Parameter.MATHSIZE, params.getSize() * 16.8f);
			layout.setParameter(Parameter.MATHCOLOR, fg);
			System.err.println("Layout: " + (System.currentTimeMillis() - start));
		
			// Layout equation
			JEuclidView view = new JEuclidView(document, layout, context);
			float ascent = view.getAscentHeight();
			float descent = view.getDescentHeight();
			float width = view.getWidth();
			System.err.println("View: " + (System.currentTimeMillis() - start));
		
			// Create new image to hold it
			BufferedImage image = new BufferedImage((int)Math.ceil(width),
				(int)Math.ceil(ascent + descent), BufferedImage.TYPE_INT_ARGB);
			System.err.println("Image: " + (System.currentTimeMillis() - start));
			view.draw(image.createGraphics(), 0, ascent);
			System.err.println("Draw: " + (System.currentTimeMillis() - start));
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(image, "png", output);
			System.err.println("PNG: " + (System.currentTimeMillis() - start));
			
			// Save results
			result.setImage(output.toByteArray());
			result.setBaseline(BigInteger.valueOf(image.getHeight()
				- (int)Math.round(ascent)));
			result.setOk(true);

			System.err.println("End: " + (System.currentTimeMillis() - start));
			return result;
		}
		catch(Throwable t)
		{
			result.setError("MathML unexpected error - " + t.getMessage());
			t.printStackTrace();
			return result;
		}
	}
	
	private Document parseMathml(String xml) throws Exception
	{
		xml = fixer.fix(xml);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}
	
	private static class MathmlEntityFixer
	{
		private Map<String, String> map = new HashMap<String, String>();
		
		private final static Pattern REGEX_ENTITY = Pattern.compile("&([^#][^;]*);");
		
		private MathmlEntityFixer() throws IOException
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(
				WebMathsImage.class.getResourceAsStream("mathml.entities.txt"), "UTF-8"));
			while(true)
			{
				String line = reader.readLine();
				if(line == null)
				{
					break;
				}
				if(line.trim().equals(""))
				{
					continue;
				}
				
				int equals = line.indexOf('=');
				map.put(line.substring(0, equals), line.substring(equals+1));
			}
			reader.close();
		}
		
		/**
		 * Fixes all the named entities in the source string.
		 * @param original Original string
		 * @return Fixed string with entities replaced by direct characters
		 * @throws IllegalArgumentException Any unknown entities
		 */
		private String fix(String original) throws IllegalArgumentException
		{
			Matcher m = REGEX_ENTITY.matcher(original);
			StringBuffer out = new StringBuffer();
			while(m.find())
			{
				String replacement = map.get(m.group(1));
				if(replacement == null)
				{
					throw new IllegalArgumentException("Unknown entity " + m.group(0));
				}
				m.appendReplacement(out, replacement);
			}
			m.appendTail(out);
			return out.toString();
		}
	}
}
