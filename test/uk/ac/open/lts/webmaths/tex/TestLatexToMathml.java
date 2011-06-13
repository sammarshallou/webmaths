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
	public void testSimpleExample()
	{
		assertMath("<msqrt><mn>1</mn></msqrt>", "\\sqrt{1}"); 
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
	public void testFractionDigits()
	{
		assertMath("<mfrac><mn>1</mn><mn>2</mn></mfrac>", "\\frac 12"); 
		assertMath("<mfrac><mn>12</mn><mn>24</mn></mfrac>", "\\frac{12}{24}");
		assertMath("<mfrac><mrow><mi>x</mi><mo>\u2212</mo><mn>1</mn></mrow>" 
			+ "<mn>2</mn></mfrac><mn>12</mn>", "\\frac{x-1}212");
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
		tokens.toMathml();
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
			Matcher m = SAMPLES_REGEX.matcher(line);
			if(!m.matches())
			{
				throw new IOException("Unexpected line data:\n" + line);
			}
			
			String tex = m.group(2);
			String result = new TokenInput(tex).toMathml();
			if(result.contains("</merror>"))
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
		String actual = tokens.toMathml();
		actual = actual.replaceFirst("<math[^>]+>(.*)</math>", "$1");
		assertEquals(expected, actual);
	}
}
