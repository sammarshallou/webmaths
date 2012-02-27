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

	private String convertToMathml(String tex, boolean display)
	{
		TokenInput tokens = new TokenInput(tex);
		return tokens.toMathml(display);
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
		"\\lgroup x \\rgroup", "(x)",
		"\\lbrace x \\rbrace", "\\{ x \\}",
		"\\lvert x \\rvert", "|x|",
		"\\lVert x \\rVert", "\\Vert x \\Vert",

		// Whitespace differences
		"\\{x\\}", "\\{ x \\}",
		"\\left( \\frac{1}{x} \\right\\}", "\\left( \\frac{1}{x} \\right \\}",
		"\\bigl( \\frac{1}{x} \\bigr\\}", "\\bigl( \\frac{1}{x} \\bigr \\}",
		"\\Bigl( \\frac{1}{x} \\Bigr\\}", "\\Bigl( \\frac{1}{x} \\Bigr \\}",
		"\\biggl( \\frac{1}{x} \\biggr\\}", "\\biggl( \\frac{1}{x} \\biggr \\}",
		"\\Biggl( \\frac{1}{x} \\Biggr\\}", "\\Biggl( \\frac{1}{x} \\Biggr \\}",
		"\\mathit{\\Gamma}", "\\mathit{\\Gamma }",

		// \begin{displaymath} (which is weird and shouldn't really be supported
		// anyhow)
		"\\begin{displaymath} \\frac{1}{x}", "\\displaystyle \\frac{1}{x}",

		// Unnecessary \displaystyle
		"\\displaystyle \\frac{1}{x}", "\\frac{1}{x}",

		// \pod{n} is currently converted in a way that can't be distinguished
		"3 \\pod{6}", "3(6)",

		// Symbols with multiple names
		"\\intersect", "\\cap",
		"\\Cap", "\\doublecap",
		"\\union", "\\cup",
		"\\Cup", "\\doublecup",
		"\\land", "\\wedge",
		"\\lor", "\\vee",
		"\\smallsetminus", "\\setminus",
		"\\colon", ":",
		"\\vert", "|",
		"\\|", "|",
		"'", "\\prime",
		"\\bmod", "\\mod",
		"\\smallint", "\\int",
		"\\bot", "\\perp",
		"\\lnot", "\\neg",
		"\\varnothing", "\\emptyset",
		"\\hslash", "\\hbar",
		"\\ge", "\\geq",
		"\\gggtr", "\\ggg",
		"\\gvertneqq", "\\gneqq",
		"\\le", "\\leq",
		"\\lvertneqq", "\\lneqq",
		"\\ne", "\\neq",
		"\\ngeqq", "\\geqq",
		"\\ngeqslant", "\\geqslant",
		"\\nleqq", "\\leqq",
		"\\nleqslant", "\\leqslant",
		"\\npreceq", "\\preceq",
		"\\nsucceq", "\\succeq",
		"\\thickapprox", "\\approx",
		"\\thicksim", "\\sim",
		"\\gets", "\\leftarrow",
		"\\restriction", "\\upharpoonright",
		"\\to", "\\rightarrow",
		"\\nshortmid", "\\nmid",
		"\\nshortparallel", "\\nparallel",
		"\\nsubseteqq", "\\subseteqq",
		"\\nsupseteqq", "\\supseteqq",
		"\\shortmid", "\\mid",
		"\\shortparallel", "\\parallel",
		"\\smallfrown", "\\frown",
		"\\smallsmile", "\\smile",
		"\\varpropto", "\\propto",
		"\\varsubsetneqq", "\\subsetneqq",
		"\\varsupsetneqq", "\\supsetneqq",
		"\\vartriangle", "\\triangle",
		"\\char93", "\\#",
		
		// Dot ambiguity
		"\\dots", "\\ldots",
		"\\dotso", "\\ldots",
		"\\dotsc", "\\ldots",
		"\\hdots", "\\cdots",
		"\\dotsb", "\\cdots",
		"\\dotsi", "\\cdots",

		// Space ambiguity
		"x \\; y", "x \\thickspace y",
		"x \\: y", "x \\medspace y",
		"x \\, y", "x \\thinspace y",

		// Extra brackets
		"\\mathop{XQX}_{i=1}^n", "{\\mathop{XQX}}_{i=1}^n",

		// Unnecessary \dfrac, \dsum, \dint
		"\\dfrac{4}{x}", "\\frac{4}{x}",
		"\\dsum_{i=1}^x X_i" , "\\sum_{i=1}^x X_i",
		"\\dint_{i=1}^x X_i" , "\\int_{i=1}^x X_i",
		"\\dbinom{4}{x}", "\\binom{4}{x}",

		// Font styles that are converted the same
		"\\textnormal{frog}", "\\text{frog}",
		"\\textrm{frog}", "\\text{frog}",
		"\\textsl{frog}", "\\textit{frog}",
		"\\boldsymbol{X}", "\\mathbf{X}",
		"\\bold{XY}", "\\mathbf{XY}",
		"\\Bbb{X}", "\\mathbb{X}",
		"\\mathbbmss{X}", "\\mathbb{X}",
		"\\mathcal{X}", "\\mathscr{X}",
		"\\EuScript{X}", "\\mathscr{X}",

		// Obsolete \bf, \rm turn into the normal ones
		"{\\bf X Y}", "\\mathbf{XY}",
		"{\\bf X \\rm Y}", "\\mathbf{X \\mathrm{Y}}",

		// hbox, mbox are converted as text
		"\\sqrt{\\hbox{frog}}", "\\sqrt{\\text{frog}}",
		"\\sqrt{\\mbox{frog}}", "\\sqrt{\\text{frog}}",
	});

	private String doRoundTrip(String tex) throws Exception
	{
		String mathml = convertToMathml(tex, true);
		String round;
		try
		{
			round = convertToTex(mathml, false);
		}
		catch(UnsupportedMathmlException e)
		{
			round = "[Unsupported: " + e.getMessage() + "]";
		}
		return round;
	}
	
	private void checkRoundTrip(StringBuilder out, String tex, boolean display) throws Exception
	{
		String mathml = convertToMathml(tex, display);
		String round;
		try
		{
			round = convertToTex(mathml, false);
		}
		catch(UnsupportedMathmlException e)
		{
			round = "[Unsupported: " + e.getMessage() + "]";
		}
		String expected = NO_ROUND_TRIP.get(tex);
		if (expected == null)
		{
			expected = tex;
		}
		if(!expected.equals(round))
		{
			String extrabit = "";
			if (!expected.equals(tex)) {
				extrabit = " (expected " + expected + ")";
			}
			out.append(tex + " => " + round + extrabit + "\n");
			out.append("  " + mathml.replaceFirst("^.*<semantics>(.*?)<annotation .*$", "$1") + "\n");
		}
	}

	private void assertRoundTrip(String tex) throws Exception
	{
		StringBuilder out = new StringBuilder();
		checkRoundTrip(out, tex, true);
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

	/**
	 * Display style can be complicated becuase of multiple switching.
	 * @throws Exception
	 */
	@Test
	public void testDisplayStyle() throws Exception
	{
		String result = doRoundTrip("\\displaystyle \\tfrac{1}{x} + \\frac{1}{x}");
		assertEquals("\\tfrac{1}{x} +\\frac{1}{x}", result);
		String mathml = convertToMathml("1 + \\displaystyle 2", false);
		result = convertToTex(mathml, false);
		assertEquals("\\textstyle 1+{ \\displaystyle 2}", result);
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

			// Strip \displaystyle as we aren't very good about round-tripping from
			// that element - just treat as a display equation in the first place
			if (tex.startsWith("\\displaystyle "))
			{
				tex = tex.substring("\\displaystyle ".length());
			}

			checkRoundTrip(out, tex, true);
		}
		System.err.println(out);
		System.err.println(out.toString().replaceAll("[^\n]", "").length() / 2 + " errors");
		assertTrue(out.length() == 0);
	}
}
