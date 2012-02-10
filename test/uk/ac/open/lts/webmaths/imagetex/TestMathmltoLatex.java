package uk.ac.open.lts.webmaths.imagetex;

import static org.junit.Assert.*;
import static uk.ac.open.lts.webmaths.MapUtil.*;

import java.io.*;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.*;

import org.junit.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import uk.ac.open.lts.webmaths.tex.TokenInput;

public class TestMathmltoLatex
{
	private WebMathsImageTex imageTexService;

	@Before
	public void setUp() throws Exception
	{
		imageTexService = new WebMathsImageTex();
	}

	private String convertToMathml(String tex)
	{
		TokenInput tokens = new TokenInput(tex);
		return tokens.toMathml(true);
	}

	private String convertToTex(String mathml) throws Exception
	{
		return imageTexService.getMathmlToLatex().convert(
			imageTexService.parseMathml(mathml));
	}

	private static Map<String, String> NO_ROUND_TRIP = makeMap(new String[]
	{
		"\\displaystyle \\lgroup x \\rgroup", "\\displaystyle (x)",
		"\\displaystyle \\lbrace x \\rbrace", "\\displaystyle \\{ x \\}",
		"\\displaystyle \\lvert x \\rvert", "\\displaystyle |x|",
		"\\displaystyle \\lVert x \\rVert", "\\displaystyle \\Vert x \\Vert",
		"\\displaystyle \\{x\\}", "\\displaystyle \\{ x \\}",
	});

	private void checkRoundTrip(StringBuilder out, String tex) throws Exception
	{
		String mathml = convertToMathml(tex);
		String round;
		try
		{
			round = convertToTex(mathml);
		}
		catch(UnsupportedMathmlException e)
		{
			System.err.println(tex);
			throw e;
		}
		String expected = NO_ROUND_TRIP.get(tex);
		if (expected == null)
		{
			expected = tex;
		}
		if(!expected.equals(round))
		{
			out.append(tex + " => " + round + "\n");
		}
	}

	private void assertRoundTrip(String tex) throws Exception
	{
		StringBuilder out = new StringBuilder();
		checkRoundTrip(out, tex);
		assertTrue(out.length() == 0);
	}

	@Test
	public void testBasic() throws Exception
	{
		assertRoundTrip("x+1");
		assertRoundTrip("\\frac{x}{y}");
	}

	@Test
	public void testSupported() throws Exception
	{
		// Checks all the equations from the 'supported' document. There is an
		// assumption that this class is compiled to 'bin/uk/ac/etc' within the
		// project folder where 'misc/supported.text.commands.xml' exists.

		// Get a file in this folder
		URL classUrl = getClass().getResource("placeholder.txt");
		File localFile = new File(classUrl.toURI());

		// Use it to navigate to the xml
		File xmlFile = new File(localFile.getParentFile().getParentFile().getParentFile().
			getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(),
			"misc/supported.tex.commands.xml");

		// Load xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new FileInputStream(xmlFile)));

		// Get all the TeX
		NodeList allTex = doc.getElementsByTagName("TeX");
		StringBuilder out = new StringBuilder();
		for(int i=0; i<allTex.getLength(); i++)
		{
			Element el = (Element)allTex.item(i);
			String tex = ((Text)el.getFirstChild()).getNodeValue();

			checkRoundTrip(out, tex);
		}
		System.err.println(out);
		assertTrue(out.length() == 0);
	}
}
