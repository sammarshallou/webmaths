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
	public void testPowers()
	{
		assertMath("<msup><mn>10</mn><mn>3</mn></msup>", "10^3"); 
		assertMath("<msup><mn>10</mn><mn>3.14</mn></msup>", "10^{3.14}");
		assertMath("<msup><mn>10</mn><mn>3</mn></msup><mn>2</mn>", "10^32"); 
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
		assertMath("<munderover><mi fontstyle=\"normal\" mathvariant=\"normal\">Q"
			+ "</mi><mn>1</mn><mi>N</mi></munderover>", "\\mathop{Q}_1^N");
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
		assertMath("<!--Unsupported environment: frog --><mrow><mn>1</mn><mo>+</mo><mrow>"
			+ "<!--Unsupported environment: zombie --><mn>2</mn></mrow></mrow>",
			"\\begin{frog}1+\\begin{zombie}2\\end{zombie}\\end{frog}");
	}

	@Test
	public void testBeginEndMultiToken()
	{
		// Unsupported begin/end should still do nothing even with several tokens
		assertMath("<!--Unsupported environment: frog* --><mrow><mn>1</mn><mo>+</mo><mrow>"
			+ "<!--Unsupported environment: frog zombie --><mn>2</mn></mrow></mrow>",
			"\\begin{frog*}1+\\begin{frog zombie}2\\end{frog zombie}\\end{frog*}");
		assertMath("<!--Unsupported environment: frog* --><mrow><mi>x</mi><mrow>"
			+ "<mn>&amp;</mn><mo>=</mo><mn>1</mn></mrow><mrow><mn>\\\\</mn><mi>y</mi>"
			+ "</mrow><mrow><mn>&amp;</mn><mo>=</mo><mn>2</mn></mrow></mrow>",
			"\\begin{frog*}x&=1\\\\y&=2\\end{frog*}");
	}

	@Test
	public void testAlign()
	{
		assertMath("<mtable><mtr><mtd><mi>x</mi></mtd>" 
			+ "<mtd><mrow><mo>=</mo><mn>1</mn></mrow></mtd></mtr>" 
			+ "<mtr><mtd><mi>y</mi></mtd>"
			+ "<mtd><mrow><mo>=</mo><mn>2</mn></mrow></mtd></mtr></mtable>",
			"\\begin{align*}x&=1\\\\y&=2\\end{align*}"); 
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
	
	private final static Pattern SAMPLES_REGEX = 
		Pattern.compile("^([^,]+),(.*)$");
	
	/**
	 * Loads all the sample equations and tests them. This test is basically
	 * a 'does it crash' kind of test - we don't examine the results.
	 */
	@Test
	public void testSampleLibrary() throws Exception
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
	
	private void assertMath(String expected, String input)
	{
		TokenInput tokens = new TokenInput(input);
		String actual = tokens.toMathml(true);
		// Get rid of math tag, semantics, annotation
		actual = actual.replaceFirst(
			"<math[^>]+><semantics><mstyle displaystyle=\"(?:true|false)\">(.*?)" 
			+ "</mstyle><annotation[^>]+>.*</semantics></math>", "$1");
		// Get rid of outer mrow if included
		actual = actual.replaceFirst("^<mrow>(.*)</mrow>$", "$1");
		assertEquals(expected, actual);
	}

	@Test
	public void testAnnoying()
	{
		String result = new TokenInput("\\big").toMathml(true);
		assertFalse(result.contains("</xerror>"));
	}
}
