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
package uk.ac.open.lts.webmaths.tex;

import java.io.*;
import java.net.URL;
import java.util.regex.*;

import junit.framework.TestCase;

import org.junit.Test;

public class TestLatexToMathml extends TestCase
{
	@Test
	public void testSimpleExamples()
	{
		assertMath("<msqrt><mn>1</mn></msqrt>", "\\sqrt{1}");
		assertMath("<mfrac><mi>x</mi><mi>y</mi></mfrac>", "\\frac{x}{y}");
	}

	@Test
	public void testLetters()
	{
		// Normal letters are italic (MathML default for one letter)
		assertMath("<mi>x</mi>", "x");
		// Lower Greek letters are also italic
		assertMath("<mi>\u03b1</mi>", "\\alpha");
		// But upper Greek letters are not
		assertMath("<mi mathvariant=\"normal\">\u0393</mi>", "\\Gamma");
		// Unless you put them in mathit
		assertMath("<mi mathvariant=\"italic\">\u0393</mi>", "\\mathit{\\Gamma}");
	}

	@Test
	public void testPowers()
	{
		assertMath("<msup><mn>10</mn><mn>3</mn></msup>", "10^3");
		assertMath("<msup><mn>10</mn><mn>3.14</mn></msup>", "10^{3.14}");
		assertMath("<msup><mn>10</mn><mn>3</mn></msup><mn>2</mn>", "10^32");
		assertMath("<msup><mn>10</mn><mn>3</mn></msup>", "{10}^3");
	}

	@Test
	public void testDigitsExample()
	{
		assertMath("<msqrt><mn>12</mn></msqrt>", "\\sqrt{12}");
		assertMath("<msqrt><mn>543210</mn></msqrt>", "\\sqrt{543210}");
	}

	@Test
	public void testDecimalDigitsExample()
	{
		assertMath("<msqrt><mn>31.459</mn></msqrt>", "\\sqrt{31.459}");
	}

	@Test
	public void testMathop()
	{
		assertMath("<munderover><mi mathvariant=\"italic\">Q"
			+ "</mi><mn>1</mn><mi>N</mi></munderover>", "\\mathop{Q}_1^N");
	}

	@Test
	public void testIncorrectSeparators()
	{
		// We do not support & or \\ outside the relevant environments.
//		assertMath("<!-- Symbol not supported outside environment: & --><mn>4</mn>" +
//			"<!-- Symbol not supported outside environment: \\\\ -->", "& 4 \\\\");
		// But you can do \&
		assertMath("<mi>x</mi><mo>&amp;</mo><mi>y</mi>", "x \\& y");
	}

	@Test
	public void testFractionDigits()
	{
		assertMath("<mfrac><mn>1</mn><mn>2</mn></mfrac>", "\\frac 12");
		assertMath("<mfrac><mn>12</mn><mn>24</mn></mfrac>", "\\frac{12}{24}");
		assertMath("<mfrac><mrow><mi>x</mi><mo>\u2212</mo><mn>1</mn></mrow>"
			+ "<mn>2</mn></mfrac><mn>12</mn>", "\\frac{x-1}212");
		assertMath("<msqrt><mn>1</mn></msqrt><mn>2</mn>", "\\sqrt 12");
	}

	@Test
	public void testOver()
	{
		// Simple usage
		assertMath("<mfrac><mn>1</mn><mn>2</mn></mfrac>", "1 \\over 2");
		// Scoped with braces
		assertMath("<mfrac><mn>1</mn><mn>2</mn></mfrac><mo>=</mo><mn>3</mn>", "{1 \\over 2} = 3");
		// Complex usage that initially caused infinite loop
		getMath("\\left[  {d \\over d\\beta } \\right]");
	}

	@Test
	public void testBoldSymbol()
	{
		assertMath("<mi mathvariant=\"bold\">\u0393</mi>", "\\boldsymbol \\Gamma");
		assertMath("<mi mathvariant=\"bold-italic\">\u03b3</mi>", "\\boldsymbol \\gamma");
		assertMath("<mi mathvariant=\"bold-italic\">x</mi>" +
			"<mi mathvariant=\"bold-italic\">y</mi>", "\\boldsymbol{xy}");
		assertMath("<mspace/><!-- Unknown TeX command: \\frog -->", "\\boldsymbol \\frog");
		assertMath("<mover accent=\"true\"><mi mathvariant=\"bold-italic\">r</mi>" +
			"<mo mathvariant=\"bold\">^</mo></mover>", "\\boldsymbol{\\hat r}");
	}

	@Test
	public void testDigitGrouping()
	{
		assertMath("<mn>1</mn><mo>−</mo><mn>0.498</mn>", "1-0.498");
	}

	@Test
	public void testCharFunctionThatNobodyWillEverUse()
	{
		assertMath("<mtext>#</mtext><mo>+</mo><mn>1</mn>", "\\char93+1");
		assertMath("<mspace/><!-- Unsupported \\char 93333 -->", "\\char93333");
	}

	@Test
	public void testBogusDelimiters()
	{
		// This isn't sensible TeX
		String result = new TokenInput("\\bigl").toMathml(true);
		assertFalse(result.contains("</xerror>"));
		assertTrue(result.contains("<!-- Missing delimiter -->"));
	}

	@Test
	public void testBeginEnd()
	{
		// Unsupported begin/end should do nothing
		assertMath("<!-- Unsupported environment: frog --><mrow><mn>1</mn><mo>+</mo><mrow>"
			+ "<!-- Unsupported environment: zombie --><mn>2</mn></mrow></mrow>",
			"\\begin{frog}1+\\begin{zombie}2\\end{zombie}\\end{frog}");
	}

	@Test
	public void testBeginEndMultiToken()
	{
		// Unsupported begin/end should still do nothing even with several tokens
		assertMath("<!-- Unsupported environment: frog* --><mrow><mn>1</mn><mo>+</mo><mrow>"
			+ "<!-- Unsupported environment: frog zombie --><mn>2</mn></mrow></mrow>",
			"\\begin{frog*}1+\\begin{frog zombie}2\\end{frog zombie}\\end{frog*}");
		assertMath("<!-- Unsupported environment: frog* --><mrow><mi>x</mi><mrow>" +
			"<!-- Symbol not supported outside environment: & --><mo>=</mo><mn>1</mn>" +
			"</mrow><mrow><!-- Symbol not supported outside environment: \\\\ --><mi>y</mi>" +
			"</mrow><mrow><!-- Symbol not supported outside environment: & -->" +
			"<mo>=</mo><mn>2</mn></mrow></mrow>",
			"\\begin{frog*}x&=1\\\\y&=2\\end{frog*}");
	}

	@Test
	public void testAlign()
	{
		assertMath("<mtable columnalign=\"right left \" rowspacing=\"2ex\"><mtr><mtd><mi>x</mi></mtd>"
			+ "<mtd><mo>=</mo><mn>1</mn></mtd></mtr>"
			+ "<mtr><mtd><mi>y</mi></mtd>"
			+ "<mtd><mo>=</mo><mn>2</mn></mtd></mtr></mtable>",
			"\\begin{align*}x&=1\\\\y&=2\\end{align*}");
	}

	@Test
	public void testTfrac()
	{
		assertMath("<mstyle displaystyle=\"false\">"
			+ "<mfrac><mn>1</mn><mi>x</mi></mfrac></mstyle>",
			"\\tfrac{1}{x}");
	}

	@Test
	public void testTextModeSwitch()
	{
		assertMath("<mtext>a</mtext><mi>x</mi><mtext>b</mtext>", "\\text{a$x$b}");
	}

	@Test
	public void testSubstack()
	{
		assertMath("<munder><mo>\u2211</mo><mtable>"
			+ "<mtr><mtd><mn>0</mn><mo>&lt;</mo><mi>i</mi><mo>&lt;</mo><mi>m</mi></mtd></mtr>"
			+ "<mtr><mtd><mn>0</mn><mo>&lt;</mo><mi>j</mi><mo>&lt;</mo><mi>n</mi></mtd></mtr>"
			+ "</mtable></munder><mi>P</mi><mrow><mo>(</mo><mrow><mi>i</mi>"
			+ "<mrow><mo>,</mo><mi>j</mi></mrow></mrow><mo>)</mo></mrow>",
			"\\sum_{\\substack{ 0<i<m \\\\ 0<j<n }} P(i,j)");
	}

	@Test
	public void testSlightlyLessSimpleExample()
	{
		assertMath("<msqrt><mfrac><mn>1</mn><mi>x</mi></mfrac></msqrt>",
			"\\sqrt{\\frac{1}{x}}");
	}

	@Test
	public void testBloodyHorribleExample()
	{
		// This test is there because it caused the converter to crash initially
		String horribleExample = "\\begin{array}{rl} ({\\bf x}-{\\bfmu })^ T {"
			+ "\\bfupSigma }^{-1}({\\bf x}-{\\bfmu })&  = \\frac1{\\sigma ^2_ X"
			+ "\\sigma ^2_ Y(1-\\rho ^2)} \\times \\\\ & \\quad (x-\\mu _ X ~ y-"
			+	"\\mu _ Y) \\begin{pmatrix}{c} \\sigma ^2_ Y(x-\\mu _ X) -\\rho "
			+	"\\sigma _ X\\sigma _ Y(y-\\mu _ Y) [NEWLINE][NEWLINE]\\\\ "
			+	"\\end{pmatrix}\\begin{pmatrix}{c}-\\rho \\sigma _ X\\sigma _ "
			+ "Y(x-\\mu _ X) +\\sigma ^2_ X(y-\\mu _ Y)[NEWLINE][NEWLINE]\\\\ "
			+ "\\end{pmatrix}\\\\ &  = \\frac1{\\sigma ^2_ X\\sigma ^2_ Y(1-\\rho "
			+ "^2)} \\times \\\\ & \\quad \\left[ \\left\\{  \\sigma ^2_ Y(x-\\mu _ "
			+ "X) -\\rho \\sigma _ X\\sigma _ Y(y-\\mu _ Y)\\right\\}  (x-\\mu _ X) "
			+ "\\right. \\\\ & \\quad \\left. \\quad + \\left\\{  -\\rho \\sigma _ "
			+ "X\\sigma _ Y(x-\\mu _ X) +\\sigma ^2_ X(y-\\mu _ Y)\\right\\} "
			+ "(y-\\mu _ Y) \\right] \\\\ & = \\frac{\\sigma _ Y^2(x-\\mu _ X)^2-2"
			+ "\\rho \\sigma _ X\\sigma _ Y (x-\\mu _ X)(y-\\mu _ Y)+\\sigma _ X^2"
			+ "(y-\\mu _ Y)^2}{\\sigma _ X^2\\sigma _ Y^2(1-\\rho ^2)} \\end{array}";
		TokenInput tokens = new TokenInput(horribleExample);
		tokens.toMathml(true);
	}

	@Test
	public void testInitialSubscript()
	{
		// Subscript at start of equation, or block, doesn't work
		assertMath("<msup><mrow/><mi>x</mi></msup>", "^x");
		assertMath("<mn>3</mn><mo>+</mo><msup><mrow/><mi>x</mi></msup>", "3 + ^x");
		assertMath("<mi>x</mi><mo>+</mo><msup><mrow/><mn>3</mn></msup>", "x + {^3}");
	}

	@Test
	public void testNegativeSpace()
	{
		// Note this adds unnecessary mrow, which is annoying, but I don't really
		// want to fix it right now.
		assertMath("<mi>x</mi><mrow><mspace width=\"negativethinmathspace\"/><mi>y</mi></mrow>", "x\\!y");
	}

	@Test
	public void testTextEscapes()
	{
		// Support for escapes inside text is pretty limited right now but let's
		// test what we've got
		assertMath("<mtext>q^q</mtext>", "\\textrm{q\\textasciicircum q}");
	}

	private final static Pattern SAMPLES_REGEX =
		Pattern.compile("^([^,]+),(.*)$");

	/**
	 * Loads all the sample equations from course content and tests them. This
	 * test is basically a 'does it crash' kind of test - we don't examine the
	 * results.
	 */
	@Test
	public void testSamplesContent() throws Exception
	{
		// Note: I thought this was kind of slow (on second run it takes about 3
		// seconds) but then I realised there are 4,500 equations in the samples
		// file so that's slightly under 1ms to convert one; not too bad.
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			TestLatexToMathml.class.getResourceAsStream("tex.samples"), "UTF-8"));
		int errors = 0;
		while(true)
		{
			String line = reader.readLine();
			if(line == null)
			{
				break;
			}
			if(line.equals("") || line.startsWith("#"))
			{
				continue;
			}
			Matcher m = SAMPLES_REGEX.matcher(line);
			if(!m.matches())
			{
				throw new IOException("Unexpected line data:\n" + line);
			}

			String tex = m.group(2);
			String result = new TokenInput(tex).toMathml(true);
			if(result.contains("</xerror>"))
			{
				System.err.println(tex);
				errors++;
			}
		}
		assertEquals(0, errors);
	}

	/**
	 * Loads all the sample equations from user forum entries and tests them.
	 * This test is basically a 'does it crash' kind of test - we don't examine
	 * the results.
	 */
	@Test
	public void testSamplesForum() throws Exception
	{
		// Get local file and use it to navigate to samples
		URL classUrl = getClass().getResource("tex.samples");
		File localFile = new File(classUrl.toURI());
		File samplesFile = new File(localFile.getParentFile().getParentFile().getParentFile().
			getParentFile().getParentFile().getParentFile().getParentFile().getParentFile(),
			"misc/forum.tex.samples");

		// There are 10,000+ samples in the forum samples file, so it takes a while
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
			if(line.equals("") || line.startsWith("#"))
			{
				continue;
			}
			String result;
			try
			{
				result = new TokenInput(line).toMathml(true);
				if(result.contains("</xerror>"))
				{
					System.err.println(line);
					errors++;
				}
			}
			catch(Throwable t)
			{
				System.err.println(line);
				t.printStackTrace();
				errors++;
			}
		}
		assertEquals(0, errors);
	}

	private void assertMath(String expected, String input)
	{
		assertEquals(expected, getMath(input));
	}

	private String getMath(String input)
	{
		TokenInput tokens = new TokenInput(input);
		String actual = tokens.toMathml(true);
		// Get rid of math tag, semantics, annotation
		actual = actual.replaceFirst(
			"<math[^>]+><semantics><mstyle displaystyle=\"(?:true|false)\">(.*?)"
			+ "</mstyle><annotation[^>]+>.*</semantics></math>", "$1");
		// Get rid of outer mrow if included
		actual = actual.replaceFirst("^<mrow>(.*)</mrow>$", "$1");
		return actual;
	}

	@Test
	public void testAnnoying()
	{
		String result = new TokenInput("\\big").toMathml(true);
		assertFalse(result.contains("</xerror>"));
	}

	@Test
	public void testBrokenDollar()
	{
		// This line used to crash the tokeniser because of $ sign
		new TokenInput("1000\\int u^{-3}\\,du = $-500[u^{-2}]").toMathml(true);
	}

	@Test
	public void testMultipleMtext() throws Exception
	{
		String result = new TokenInput("\\text{frog}").toMathml(true);
		assertTrue(result.contains("<mstyle displaystyle=\"true\"><mtext>frog</mtext></mstyle>"));
	}

	@Test
	public void testFonts() throws Exception
	{
		assertMath("<mtext>frog</mtext>", "\\text{frog}");
		assertMath("<mtext>frog</mtext>", "\\textnormal{frog}");
		assertMath("<mtext>frog</mtext>", "\\textrm{frog}");
		assertMath("<mtext mathvariant=\"bold\">frog</mtext>", "\\textbf{frog}");
		assertMath("<mtext mathvariant=\"italic\">frog</mtext>", "\\textit{frog}");
		assertMath("<mtext mathvariant=\"italic\">frog</mtext>", "\\textsl{frog}");
		assertMath("<mtext mathvariant=\"monospace\">frog</mtext>", "\\texttt{frog}");

		assertMath("<mi mathvariant=\"fraktur\">X</mi>", "\\mathfrak{X}");
		assertMath("<mstyle mathvariant=\"bold\"><mi>X</mi><mi>Y</mi></mstyle>", "\\mathbf{XY}");
	}

	@Test
	public void testSpace() throws Exception
	{
		// Backslash-space
		assertMath("<mi>x</mi><mrow><mspace width=\"mediummathspace\"/><mi>y</mi></mrow>", "x\\ y");
		// \textrm with spaces either side (in something that is already mrow-equivalent)
		assertMath("<mspace width=\"mediummathspace\"/><mtext>q</mtext><mspace width=\"mediummathspace\"/>",
			"\\textrm{ q }");
		// \textrm with a space on one side (in something that is not mrow-equivalent)
		assertMath("<mfrac><mrow><mspace width=\"mediummathspace\"/><mtext>x</mtext></mrow><mn>2</mn></mfrac>",
			"\\frac{\\textrm{ x}}{2}");
	}

	@Test
	public void testTexWhitespace() throws Exception
	{
		// The following examples should all be equal
		assertEquals(getMath("\\text{frog}"), getMath("\\text {frog}"));
		assertEquals(getMath("\\sqrt{x}y"), getMath("\\sqrt xy"));
		assertEquals(getMath("\\sqrt{xy}"), getMath("\\sqrt {xy}"));
		assertEquals(getMath("\\frac 1 2"), getMath("\\frac{1}{2}"));
	}

	@Test
	public void testUnsupportedCommands() throws Exception
	{
		// Unsupported commands ought to be ignored (treated as mspace)
		// with a MathML comment.
		assertMath("<mn>1</mn><mspace/><!-- Unknown TeX command: \\frog -->", "1\\frog");
		assertMath("<mspace/><!-- Unknown TeX command: \\frog --><mn>1</mn>", "\\frog{1}");
	}

	@Test
	public void testTextStyleLimits() throws Exception
	{
		// Should use munder in display style and msub in text style
		assertMath("<munder><mi>lim </mi><mi>i</mi></munder>", "\\lim_i");
		assertMath("<mstyle displaystyle=\"false\" scriptlevel=\"0\"><msub><mi>lim </mi><mi>i</mi></msub></mstyle>",
			"\\textstyle\\lim_i");

		// With text style specified in param
		String sample = "\\lim _{n\\rightarrow \\infty } B(\\tilde\\theta ) = 0";
		String fromParam = new TokenInput(sample).toMathml(false);
		assertTrue(fromParam.contains("<msub>"));
	}

	@Test
	public void testDollarSpacing() throws Exception
	{
		// The space should NOT be included in the <mn> tag around dollars
		assertMath("<mtext>$</mtext><mn>3</mn>", "\\$ 3");
	}

	@Test
	public void testLimSup() throws Exception
	{
		assertMath("<mi>lim sup\u2009</mi><mn>4</mn>", "\\limsup 4");
	}

	@Test
	public void testMods() throws Exception
	{
		assertMath("<mn>13</mn><mrow><mspace width=\"1em\"/><mo>mod</mo><mn>16</mn></mrow>",
			"13 \\mod{16}");
		assertMath("<mn>13</mn><mrow><mo>mod</mo><mn>16</mn></mrow>", "13 \\bmod{16}");
		assertMath("<mn>13</mn><mrow><mspace width=\"1em\"/><mo>(</mo><mn>16</mn><mo>)</mo></mrow>",
			"13 \\pod{16}");
		assertMath("<mn>13</mn><mrow><mspace width=\"1em\"/><mo>(</mo><mo>mod</mo><mn>16</mn><mo>)</mo></mrow>",
			"13 \\pmod{16}");
	}

	@Test
	public void testCases() throws Exception
	{
		// Cases should be left-aligned.
		assertMath("<mo>{</mo><mtable columnalign=\"left left\"><mtr><mtd><mn>1</mn></mtd><mtd><mi>x</mi><mo>=</mo><mn>4</mn></mtd></mtr><mtr><mtd><mn>17</mn></mtd><mtd><mi>x</mi><mo>=</mo><mn>5</mn></mtd></mtr></mtable>",
			"\\begin{cases} 1 & x=4 \\\\ 17 & x=5 \\end{cases}");
	}

	@Test
	public void testCaseBraces() throws Exception
	{
		// There was a bug with the 'a' text in curly braces in this instance.
		assertMath("<mo>{</mo><mtable columnalign=\"left left\">"
			+ "<mtr><mtd><mi>a</mi></mtd><mtd><mtext>A</mtext></mtd></mtr>"
			+ "<mtr><mtd><mi>b</mi></mtd><mtd><mtext>B</mtext></mtd></mtr>"
			+ "</mtable>", "\\begin{cases}{a}&\\mbox{A}\\\\b & \\mbox{B}\\end{cases}");
	}

}
