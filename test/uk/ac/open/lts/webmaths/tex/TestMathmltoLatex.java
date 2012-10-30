package uk.ac.open.lts.webmaths.tex;

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
import uk.ac.open.lts.webmaths.imagetex.WebMathsImageTex;

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

	private String convertToTexInner(String inner, boolean ignoreUnsupported)
		throws Exception
	{
		return convertToTex(
			"<math xmlns='" + WebMathsService.NS + "'>" + inner + "</math>",
			ignoreUnsupported);
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
		"\\begin{pmatrix} a&b \\\\ c&d \\end{pmatrix}", "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",
		"\\begin{bmatrix} a&b \\\\ c&d \\end{bmatrix}", "\\begin{bmatrix} a & b \\\\ c & d \\end{bmatrix}",
		"\\begin{Bmatrix} a&b \\\\ c&d \\end{Bmatrix}", "\\begin{Bmatrix} a & b \\\\ c & d \\end{Bmatrix}",
		"\\begin{vmatrix} a&b \\\\ c&d \\end{vmatrix}", "\\begin{vmatrix} a & b \\\\ c & d \\end{vmatrix}",
		"\\begin{Vmatrix} a&b \\\\ c&d \\end{Vmatrix}", "\\begin{Vmatrix} a & b \\\\ c & d \\end{Vmatrix}",
		"x \\  y", "x \\ y",

		// Array columns
		"\\begin{array} a&b \\\\ c&d \\end{array}", "\\begin{array}{ll} a & b \\\\ c & d \\end{array}",
		"\\left( \\begin{array} a \\\\ b \\end{array} \\right)", "\\left( \\begin{array}{l} a \\\\ b \\end{array} \\right)",

		// \begin{displaymath} (which is weird and shouldn't really be supported
		// anyhow)
		"\\begin{displaymath} \\frac{1}{x}", "\\frac{1}{x}",

		// Unnecessary \displaystyle
		"\\displaystyle \\frac{1}{x}", "\\frac{1}{x}",

		// \pod{n} is currently converted in a way that can't be distinguished
		"3 \\pod{6}", "3(6)",

		// \mod and \bmod are treated like operators
		"13 \\mod{16}", "13 \\mod 16",
		"13 \\bmod{16}", "13 \\mod 16",

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
		"\\prime", "'",
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
		"\\iff", "\\Leftrightarrow",
		"\\implies", "\\Rightarrow",

		// \doublesum not really supported
		"\\doublesum", "\\operatorname{\\sum \\sum }",

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

		// Unnecessary \dfrac, \dsum, \dint
		"\\dfrac{4}{x}", "\\frac{4}{x}",
		"\\dsum_{i=1}^x X_i" , "\\sum_{i=1}^x X_i",
		"\\dint_{i=1}^x X_i" , "\\int_{i=1}^x X_i",
		"\\dbinom{4}{x}", "\\binom{4}{x}",

		// \tsum, \tint not supported in our TeX setup
		"\\tsum_{i=1}^x X_i", "{ \\textstyle \\sum_{i=1}^x } X_i",
		"\\tint_{i=1}^x X_i", "{ \\textstyle \\int_{i=1}^x } X_i",

		// Font styles that are converted the same
		"\\textnormal{frog}", "\\textrm{frog}",
		"\\text{frog}", "\\textrm{frog}",
		"\\textsl{frog}", "\\textit{frog}",
		"\\boldsymbol \\Gamma", "\\boldsymbol{\\Gamma }",
		"\\bold{XY}", "\\mathbf{XY}",
		"\\bold{1}", "\\mathbf{1}",
		"\\Bbb{X}", "\\mathbb{X}",
		"\\mathbbmss{X}", "\\mathbb{X}",
		"\\mathcal{X}", "\\mathscr{X}",
		"\\EuScript{X}", "\\mathscr{X}",

		// \over is same as \frac
		"1 \\over x", "\\frac{1}{x}",

		// Obsolete \bf, \rm turn into the normal ones
		"{\\bf X Y}", "\\mathbf{XY}",
		"{\\bf X \\rm Y}", "\\mathbf{X \\mathrm{Y}}",

		// hbox, mbox are converted as text
		"\\sqrt{\\hbox{frog}}", "\\sqrt{\\textrm{frog}}",
		"\\sqrt{\\mbox{frog}}", "\\sqrt{\\textrm{frog}}",

		// smallmatrix = pmatrix
		"\\begin{smallmatrix} a&b \\\\ c&d \\end{smallmatrix}", "\\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix}",

		// Evil letter things
		"\\R", "\\mathbb{R}",
		"\\Q", "\\mathbb{Q}",
		"\\N", "\\mathbb{N}",
		"\\C", "\\mathbb{C}",
		"\\Z", "\\mathbb{Z}",
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
			round += " - " + convertToTex(mathml, true);
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
		if (out.length() > 0)
		{
			System.err.println(out);
		}
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
	public void testLetters() throws Exception
	{
		// Test various letter categories
		assertRoundTrip("x");
		assertRoundTrip("\\alpha");
		assertRoundTrip("\\Gamma");
		assertRoundTrip("\\mathit{\\Gamma}");
	}

	@Test
	public void testNegativeRoot() throws Exception
	{
		assertRoundTrip("\\sqrt[-3]{x}");
	}

	@Test
	public void testMultiscripts() throws Exception
	{
		// Both
		assertRoundTrip("{}^nC_r");
		assertRoundTrip("{}^nC_r{}_q");

		// Before only
		assertRoundTrip("{}^nC");

		// After only
		assertRoundTrip("C^x{}^y");
	}

	@Test
	public void testText() throws Exception
	{
		assertEquals("\\textrm{$\\backslash $}", convertToTexInner("<mtext>\\</mtext>", true));
		assertEquals("\\textrm{$\\backslash $omega}", convertToTexInner("<mtext>\\omega</mtext>", true));
	}

	@Test
	public void testWhitespace() throws Exception
	{
		String tex = convertToTex("<math xmlns='http://www.w3.org/1998/Math/MathML'>\n" +
			"<mi>a</mi>\n<mo>+</mo>\n<mi>b</mi></math>", false);
		assertEquals("a+b", tex);
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
	public void testPrimes() throws Exception
	{
		// Check that a prime symbol, not in a subscript, uses ascii '
		assertRoundTrip("f'(x)");

		// Check that a prime symbol in subscript/superscript uses \prime
		assertRoundTrip("f_\\prime^\\prime");
	}

	/**
	 * There was a case where we too-eagerly trimmed away brackets from the edge.
	 * @throws Exception
	 */
	@Test
	public void testBraces() throws Exception
	{
		assertRoundTrip("{10}^{-10}");
	}

	@Test
	public void testTrimSurroundingBraces()
	{
		assertEquals("{1}+{x}", MathmlToLatex.trimSurroundingBraces("{{ {1}+{x} }} "));
		assertEquals("ws", MathmlToLatex.trimSurroundingBraces("   ws   "));
		assertEquals("ws", MathmlToLatex.trimSurroundingBraces(" {   ws   } "));
		assertEquals("ws\\ ", MathmlToLatex.trimSurroundingBraces("ws\\ "));
		assertEquals("ws\\ ", MathmlToLatex.trimSurroundingBraces("ws\\      "));
		assertEquals("ws\\\\", MathmlToLatex.trimSurroundingBraces("ws\\\\    "));
		assertEquals("ws\\\\\\ ", MathmlToLatex.trimSurroundingBraces("ws\\\\\\      "));
		assertEquals("ws\\\\\\\\", MathmlToLatex.trimSurroundingBraces("ws\\\\\\\\    "));
	}

	@Test
	public void testEvilSpaceHandling() throws Exception
	{
		// Becase of the way \textrm with spaces at the end is converted to MathML,
		// without special processing it would turn into separate "\ " commands;
		// for niceness, I want to ensure it gets put back inside the \textrm.
		assertRoundTrip("z \\textrm{ frog }y");
		assertRoundTrip("z \\textbf{ frog }y");
		// Check it handles spaces that could be at end of one, or start of next
		// (it is supposed to glue them to the end in that case)
		assertRoundTrip("\\textbf{frog   }\\textrm{zombie}");
		// A 'special' mtext that we don't glue things to
		assertRoundTrip("\\ \\# \\ ");
	}

	@Test
	public void testMathop() throws Exception
	{
		assertRoundTrip("\\mathop{frog}_x^y");
		assertEquals("\\mathop{frog}_x", convertToTexInner(
			"<munder><mi mathvariant='italic'>frog</mi><mi>x</mi></munder>", false));
		assertEquals("{\\mathit{frog}}_x", convertToTexInner(
			"<msub><mi mathvariant='italic'>frog</mi><mi>x</mi></msub>", false));
	}

	@Test
	public void testMathrm() throws Exception
	{
		assertRoundTrip("\\mathrm{x}");
		assertRoundTrip("\\mathrm{distance}");
	}

	@Test
	public void testEmpty() throws Exception
	{
		// Round trip
		assertRoundTrip("");

		// Empty MathML which actually has other junk in it
		assertEquals("", convertToTex("<math xmlns='" + WebMathsService.NS + "'>" +
			"<semantics><mstyle displaystyle=\"true\"></mstyle>" +
			"<annotation encoding=\"TeX\"></annotation></semantics></math>",
			false));
	}

	@Test
	public void testEscaping() throws Exception
	{
		assertEquals("\\textrm{$\\{ $f$\\} $r$\\backslash $o\\textasciicircum g}",
			convertToTex("<math xmlns='" + WebMathsService.NS + "'>" +
			"<semantics><mstyle displaystyle=\"true\">" +
			"<mtext>{f}r\\o^g</mtext>" +
			"</mstyle><annotation encoding=\"TeX\"></annotation></semantics></math>",
			false));
	}

	@Test
	public void testTextEscapes() throws Exception
	{
		assertEquals("\\textrm{q\\textasciicircum q}",
			convertToTexInner("<mtext>q^q</mtext>", true));
		assertRoundTrip("\\textrm{x \\quad y}");
	}

	@Test
	public void testLeftRightDots() throws Exception
	{
		// Test that it copes with \\left. and \\right
		assertRoundTrip("\\left. \\frac{1}{y} \\right|");
		assertRoundTrip("\\left| \\frac{1}{y} \\right.");
		assertRoundTrip("\\left. X \\right)");
		// Test something else isn't broken
		assertRoundTrip("\\big( X \\big)");
		assertRoundTrip("\\Bigl( \\frac{1}{x} \\Bigr\\}");
	}

	@Test
	public void testNotUsingTsum() throws Exception
	{
		// \tsum is not supported in our TeX install (also \tint, \dsum, etc).
		// \tfrac and others work fine.
		assertEquals("\\textstyle \\sum_{I=0}^n",
			convertToTex(convertToMathml("\\tsum_{I=0}^n", true),
			false));
	}

	@Test
	public void testInvisibleTimes() throws Exception
	{
		assertEquals("19x", convertToTexInner(
			"<mn>19</mn><mo>\u2062</mo><mi>x</mi>", false));
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
		if(out.length() > 0)
		{
			System.err.println(out);
			System.err.println(out.toString().replaceAll("[^\n]", "").length() / 2 + " errors");
		}
		assertTrue(out.length() == 0);
	}

	@Test
	public void testForumSamples() throws Exception
	{
		// Checks all the equations from the 'supported' document. There is an
		// assumption that this class is compiled to 'bin/uk/ac/etc' within the
		// project folder where 'misc/forum.tex.samples' exists.

		// Note that this is JUST a 'does-not-crash' test and doesn't actually
		// check the results.

		// Get a file in this folder
		URL classUrl = getClass().getResource("placeholder.txt");
		File localFile = new File(classUrl.toURI());

		// Use it to navigate to samples
		File samplesFile = new File(localFile.getParentFile().getParentFile().getParentFile().
			getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(),
			"misc/forum.tex.samples");

		BufferedReader reader = new BufferedReader(new InputStreamReader(
			new FileInputStream(samplesFile), "UTF-8"));
		int errors = 0;
		while(true)
		{
			String line = reader.readLine();
			if(line == null)
			{
				break;
			}

			// Do round trip, discarding results
			try
			{
				StringBuilder out = new StringBuilder();
				checkRoundTrip(out, line, true);
				if(out.toString().contains("[Unsupported"))
				{
					System.err.println(out.toString());
					throw new Exception("Unsupported");
				}
				else if(out.toString().contains("<!-- TeX to MathML conversion failure"))
				{
					System.err.println(line);
					errors++;
				}
			}
			catch(Throwable t)
			{
				System.err.println(line);
				errors++;
			}
		}
		reader.close();
		assertEquals(0, errors);
	}
}
