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

import uk.ac.open.lts.webmaths.WebMathsService;
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

	private String convertToTex(String mathml, boolean ignoreUnsupported)
		throws Exception
	{
		return imageTexService.getMathmlToLatex().convert(
			imageTexService.parseMathml(mathml), ignoreUnsupported);
	}

	private static Map<String, String> NO_ROUND_TRIP = makeMap(new String[]
	{
		// Wrapper thingies
		"\\displaystyle \\lgroup x \\rgroup", "\\displaystyle (x)",
		"\\displaystyle \\lbrace x \\rbrace", "\\displaystyle \\{ x \\}",
		"\\displaystyle \\lvert x \\rvert", "\\displaystyle |x|",
		"\\displaystyle \\lVert x \\rVert", "\\displaystyle \\Vert x \\Vert",

		// Whitespace difference
		"\\displaystyle \\{x\\}", "\\displaystyle \\{ x \\}",

		// \begin{displaymath} (which is weird and shouldn't really be supported
		// anyhow)
		"\\displaystyle \\begin{displaymath} \\frac{1}{x}", "\\displaystyle \\displaystyle \\frac{1}{x}",

		// Symbols with multiple names
		"\\displaystyle \\intersect", "\\displaystyle \\cap",
		"\\displaystyle \\Cap", "\\displaystyle \\doublecap",
		"\\displaystyle \\union", "\\displaystyle \\cup",
		"\\displaystyle \\Cup", "\\displaystyle \\doublecup",
		"\\displaystyle \\land", "\\displaystyle \\wedge",
		"\\displaystyle \\lor", "\\displaystyle \\vee",
		"\\displaystyle \\smallsetminus", "\\displaystyle \\setminus",
		"\\displaystyle \\colon", "\\displaystyle :",
		"\\displaystyle \\vert", "\\displaystyle |",
		"\\displaystyle \\|", "\\displaystyle |",
		"\\displaystyle '", "\\displaystyle \\prime",
		"\\displaystyle \\bmod", "\\displaystyle mod",
		"\\displaystyle \\mod", "\\displaystyle mod",
		"\\displaystyle \\smallint", "\\displaystyle \\int",
		"\\displaystyle \\bot", "\\displaystyle \\perp",
		"\\displaystyle \\lnot", "\\displaystyle \\neg",
		"\\displaystyle \\varnothing", "\\displaystyle \\emptyset",
		"\\displaystyle \\hslash", "\\displaystyle \\hbar",
		"\\displaystyle \\ge", "\\displaystyle \\geq",
		"\\displaystyle \\gggtr", "\\displaystyle \\ggg",
		"\\displaystyle \\gvertneqq", "\\displaystyle \\gneqq",
		"\\displaystyle \\le", "\\displaystyle \\leq",
		"\\displaystyle \\lvertneqq", "\\displaystyle \\lneqq",
		"\\displaystyle \\ne", "\\displaystyle \\neq",
		"\\displaystyle \\ngeqq", "\\displaystyle \\geqq",
		"\\displaystyle \\ngeqslant", "\\displaystyle \\geqslant",
		"\\displaystyle \\nleqq", "\\displaystyle \\leqq",
		"\\displaystyle \\nleqslant", "\\displaystyle \\leqslant",
		"\\displaystyle \\npreceq", "\\displaystyle \\preceq",
		"\\displaystyle \\nsucceq", "\\displaystyle \\succeq",
		"\\displaystyle \\thickapprox", "\\displaystyle \\approx",
		"\\displaystyle \\thicksim", "\\displaystyle \\sim",
		"\\displaystyle \\gets", "\\displaystyle \\leftarrow",
		"\\displaystyle \\restriction", "\\displaystyle \\upharpoonright",
		"\\displaystyle \\to", "\\displaystyle \\rightarrow",
		"\\displaystyle \\nshortmid", "\\displaystyle \\nmid",
		"\\displaystyle \\nshortparallel", "\\displaystyle \\nparallel",
		"\\displaystyle \\nsubseteqq", "\\displaystyle \\subseteqq",
		"\\displaystyle \\nsupseteqq", "\\displaystyle \\supseteqq",
		"\\displaystyle \\shortmid", "\\displaystyle \\mid",
		"\\displaystyle \\shortparallel", "\\displaystyle \\parallel",
		"\\displaystyle \\smallfrown", "\\displaystyle \\frown",
		"\\displaystyle \\smallsmile", "\\displaystyle \\smile",
		"\\displaystyle \\varpropto", "\\displaystyle \\propto",
		"\\displaystyle \\varsubsetneqq", "\\displaystyle \\subsetneqq",
		"\\displaystyle \\varsupsetneqq", "\\displaystyle \\supsetneqq",
		"\\displaystyle \\vartriangle", "\\displaystyle \\triangle",
		
		// Dot ambiguity
		"\\displaystyle \\dots", "\\displaystyle \\ldots",
		"\\displaystyle \\dotso", "\\displaystyle \\ldots",
		"\\displaystyle \\dotsc", "\\displaystyle \\ldots",
		"\\displaystyle \\hdots", "\\displaystyle \\cdots",
		"\\displaystyle \\dotsb", "\\displaystyle \\cdots",
		"\\displaystyle \\dotsi", "\\displaystyle \\cdots",

		// Space ambiguity
		"\\displaystyle x \\; y", "\\displaystyle x \\thickspace y",
		"\\displaystyle x \\: y", "\\displaystyle x \\medspace y",
		"\\displaystyle x \\, y", "\\displaystyle x \\thinspace y",
	});

	private void checkRoundTrip(StringBuilder out, String tex) throws Exception
	{
		String mathml = convertToMathml(tex);
		String round;
		try
		{
			round = convertToTex(mathml, false);
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
			out.append("  " + mathml.replaceFirst("^.*<semantics>(.*?)<annotation .*$", "$1") + "\n");
		}
	}

	private void assertRoundTrip(String tex) throws Exception
	{
		StringBuilder out = new StringBuilder();
		checkRoundTrip(out, tex);
		System.err.println(out);
		assertTrue(out.length() == 0);
	}

	@Test
	public void testBasic() throws Exception
	{
		assertEquals("3", convertToTex(
			"<math xmlns='" + WebMathsService.NS + "'><mn>3</mn></math>", false));
		assertRoundTrip("x+1");
		assertRoundTrip("\\frac{x}{y}");
	}

	@Test
	public void testUnsupported() throws Exception
	{
		String unsupportedElement =
			"<math xmlns='" + WebMathsService.NS + "'><q>3</q></math>";
		String unsupportedAttribute =
			"<math xmlns='" + WebMathsService.NS + "'><mn silly='x'>3</mn></math>";

		try
		{
			convertToTex(
				unsupportedElement, false);
			fail();
		}
		catch(UnsupportedMathmlException e)
		{
			assertEquals("element q", e.getMessage());
		}
		try
		{
			convertToTex(unsupportedAttribute, false);
			fail();
		}
		catch(UnsupportedMathmlException e)
		{
			assertEquals("attribute mn/@silly", e.getMessage());
		}
		assertEquals("", convertToTex(unsupportedElement, true));
		assertEquals("3", convertToTex(unsupportedAttribute, true));
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
		System.err.println(out.toString().replaceAll("[^\n]", "").length() / 2 + " errors");
		assertTrue(out.length() == 0);
	}
}
