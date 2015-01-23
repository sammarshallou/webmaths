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
package uk.ac.open.lts.webmaths;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import junit.framework.TestCase;

import org.junit.*;

public class TestNormaliseXSL extends TestCase
{
	private TransformerPool pool;

	@Override
	@Before
	public void setUp() throws Exception
	{
		MathmlEntityFixer fixer = new MathmlEntityFixer();
		pool = new TransformerPool(fixer, WebMathsService.class, "normalise.xsl");
	}

	@Test
	public void testNull() throws Exception
	{
		assertResult("<mrow><mi>x</mi></mrow>", "<mrow><mi>x</mi></mrow>");
	}

	@Test
	public void testMrowAdding() throws Exception
	{
		innerTestMrowAdding("msqrt");
		innerTestMrowAdding("mstyle");
		innerTestMrowAdding("merror");
		innerTestMrowAdding("mphantom");
		innerTestMrowAdding("menclose");
		innerTestMrowAdding("mtd");
	}

	private void innerTestMrowAdding(String tag) throws Exception
	{
		assertResult("<" + tag + "><mrow><mi>x</mi><mi>y</mi></mrow></" + tag + ">",
			"<" + tag + "><mi>x</mi><mi>y</mi></" + tag + ">");
		assertResult("<" + tag + "><mi>x</mi></" + tag + ">",
			"<" + tag + "><mi>x</mi></" + tag + ">");
	}

	@Test
	public void testFencedDefaults1() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> ( </mo> <mi>x</mi> <mo fence='true'> ) </mo> </mrow></mrow>",
			"<mrow><mfenced> <mi>x</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedDefaults2() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> ( </mo> <mrow> <mi>x</mi> <mo separator='true'>,</mo> <mi>y</mi> </mrow> <mo fence='true'> ) </mo> </mrow></mrow>",
			"<mrow><mfenced> <mi>x</mi> <mi>y</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedDefaults3() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> ( </mo> <mrow> <mi>x</mi> <mo separator='true'>,</mo> <mi>y</mi> <mo separator='true'>,</mo> <mi>z</mi> </mrow> <mo fence='true'> ) </mo> </mrow></mrow>",
			"<mrow><mfenced> <mi>x</mi> <mi>y</mi> <mi>z</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedParams1() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> opening-fence </mo> <mi>x</mi> <mo fence='true'> closing-fence </mo> </mrow></mrow>",
			"<mrow><mfenced open='opening-fence' close='closing-fence'> <mi>x</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedParams3Colon() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> Q </mo> <mrow> <mi>x</mi> <mo separator='true'>:</mo> <mi>y</mi> <mo separator='true'>:</mo> <mi>z</mi> </mrow> <mo fence='true'> Z </mo> </mrow></mrow>",
			"<mrow><mfenced open='Q' close='Z' separators=':'> <mi>x</mi> <mi>y</mi> <mi>z</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedParams3ColonSlash() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> Q </mo> <mrow> <mi>x</mi> <mo separator='true'>:</mo> <mi>y</mi> <mo separator='true'>/</mo> <mi>z</mi> </mrow> <mo fence='true'> Z </mo> </mrow></mrow>",
			"<mrow><mfenced open='Q' close='Z' separators=':/'> <mi>x</mi> <mi>y</mi> <mi>z</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedParams3ColonSlashSpaces() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> Q </mo> <mrow> <mi>x</mi> <mo separator='true'>:</mo> <mi>y</mi> <mo separator='true'>/</mo> <mi>z</mi> </mrow> <mo fence='true'> Z </mo> </mrow></mrow>",
			"<mrow><mfenced open='Q' close='Z' separators='     :   /  '> <mi>x</mi> <mi>y</mi> <mi>z</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedParams3ColonSlashColon() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> Q </mo> <mrow> <mi>x</mi> <mo separator='true'>:</mo> <mi>y</mi> <mo separator='true'>/</mo> <mi>z</mi> </mrow> <mo fence='true'> Z </mo> </mrow></mrow>",
			"<mrow><mfenced open='Q' close='Z' separators=':/:'> <mi>x</mi> <mi>y</mi> <mi>z</mi> </mfenced></mrow>");
	}

	@Test
	public void testFencedParams3None() throws Exception
	{
		assertResult(
			"<mrow><mrow> <mo fence='true'> Q </mo> <mrow> <mi>x</mi> <mi>y</mi> <mi>z</mi> </mrow> <mo fence='true'> Z </mo> </mrow></mrow>",
			"<mrow><mfenced open='Q' close='Z' separators=''> <mi>x</mi> <mi>y</mi> <mi>z</mi> </mfenced></mrow>");
	}

	@Test
	public void testTablesNotUtterlyBroken() throws Exception
	{
		assertResult(
			"<mtable><mtr><mtd><mn>3</mn></mtd></mtr></mtable>",
			"<mtable><mtr><mtd><mn>3</mn></mtd></mtr></mtable>");
	}

	private void assertResult(String expected, String fragment) throws Exception
	{
		String mathmlIn = "<math xmlns='http://www.w3.org/1998/Math/MathML'>" +
			fragment + "</math>";

		Transformer t = pool.reserve();
		try
		{
			StringWriter out = new StringWriter();
			t.transform(
				new StreamSource(new StringReader(mathmlIn)),
				new StreamResult(out));
			String outString = out.toString().replaceAll(
				"[^Q]*<math[^>]*>(.*?)</math>", "$1");
			outString = outString.replaceAll(" xmlns:m=\"[^\"]*\"", "");
			outString = outString.replaceAll("m:", "");
			assertEqualsIgnoringWhitespace(expected, outString);
		}
		finally
		{
			pool.release(t);
		}
	}

	private void assertEqualsIgnoringWhitespace(String expected, String value)
	{
		assertTrue(
			"Unexpected result:\n" + value + "\nExpecting:\n" + expected,
			expected.replaceAll("\\s+", "").replace('"', '\'').equals(
				value.replaceAll("\\s+", "").replace('"', '\'')));
	}

	@Test
	public void testEvilBug() throws Exception
	{
		// This was a bug where processing the second document ONLY fails if another
		// document has already been processed using the same transformer object.
		// It doesn't matter what the content of the first document is. The
		// transformer outputs to stderr as follows:
		// ERROR:  ''

		String maths1="<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><semantics><mstyle displaystyle=\"true\"><mn>1</mn></mstyle><annotation encoding=\"application/x-tex\">1</annotation></semantics></math>";
		// I have tried to reduce the content of this to see if the error still
		// occurs but, at present, removing any single line causes the error to go
		// away. Adding line breaks (\n) does not make any difference.
		String maths2="<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mtable>" +
				"<mtr><mtd/><mtd><mrow><mrow><mtext>Let</mtext></mrow><mi>f</mi></mrow><mo>:</mo><mrow><mrow><mtext> </mtext><mi>x</mi></mrow><mo>↦</mo><mrow><mtext> </mtext><mi>f</mi><mrow><mo>(</mo><mi>x</mi><mo>)</mo></mrow></mrow></mrow><mrow><mtext>bethecubicgiven.</mtext></mrow></mtd></mtr>" +
				"<mtr><mtd/><mtd><mrow><mrow><mi>f</mi><mrow><mo>(</mo><mi>x</mi><mo>)</mo></mrow></mrow><mo>=</mo><mn>0</mn></mrow><mrow><mrow><mrow><mtext>hassolutions</mtext></mrow><mi>x</mi></mrow><mo>=</mo><mi>p</mi></mrow><mrow><mrow><mo>,</mo><mi>x</mi></mrow><mo>=</mo><mi>q</mi></mrow><mrow><mrow><mo>,</mo><mi>x</mi></mrow><mo>=</mo><mi>r</mi></mrow><mrow><mtext>therefore,</mtext></mrow></mtd></mtr><mtr><mtd/><mtd><mrow><mrow><mrow><mo>(</mo><mrow><mi>x</mi><mo>−</mo><mi>p</mi></mrow><mo>)</mo></mrow><mrow><mo>(</mo><mrow><mi>x</mi><mo>−</mo><mi>q</mi></mrow><mo>)</mo></mrow><mrow><mo>(</mo><mrow><mi>x</mi><mo>−</mo><mi>r</mi></mrow><mo>)</mo></mrow></mrow><mo>=</mo><mn>0</mn></mrow><mo>,</mo></mtd></mtr>" +
				"<mtr><mtd/><mtd><mo>⇒</mo><mrow><mrow><mrow><mtext> </mtext><msup><mi>x</mi><mn>3</mn></msup></mrow><mo>−</mo><mrow><msup><mi>x</mi><mn>2</mn></msup><mrow><mo>(</mo><mrow><mi>p</mi><mo>+</mo><mi>q</mi><mo>+</mo><mi>r</mi></mrow><mo>)</mo></mrow></mrow><mo>+</mo><mrow><mi>x</mi><mrow><mo>(</mo><mrow><mrow><mi>p</mi><mi>q</mi></mrow><mo>+</mo><mrow><mi>p</mi><mi>r</mi></mrow><mo>+</mo><mrow><mi>q</mi><mi>r</mi></mrow></mrow><mo>)</mo></mrow></mrow><mo>−</mo><mrow><mi>p</mi><mi>q</mi><mi>r</mi></mrow></mrow><mo>=</mo><mn>0</mn></mrow><mo>,</mo></mtd></mtr>" +
				"<mtr><mtd/><mtd><mrow><mrow><mtext>Attheturningpoints</mtext></mrow><mi>f</mi></mrow><mrow><mrow><mo>′</mo><mrow><mo>(</mo><mi>x</mi><mo>)</mo></mrow></mrow><mo>=</mo><mn>0</mn></mrow><mrow><mtext>so,</mtext></mrow></mtd></mtr>" +
				"<mtr><mtd/><mtd><mrow><mrow><mrow><mn>3</mn><msup><mi>x</mi><mn>2</mn></msup></mrow><mo>−</mo><mrow><mn>2</mn><mi>x</mi><mrow><mo>(</mo><mrow><mi>p</mi><mo>+</mo><mi>q</mi><mo>+</mo><mi>r</mi></mrow><mo>)</mo></mrow></mrow><mo>+</mo><mrow><mo>(</mo><mrow><mrow><mi>p</mi><mi>q</mi></mrow><mo>+</mo><mrow><mi>p</mi><mi>r</mi></mrow><mo>+</mo><mrow><mi>q</mi><mi>r</mi></mrow></mrow><mo>)</mo></mrow></mrow><mo>=</mo><mn>0</mn></mrow><mo>,</mo></mtd></mtr>" +
				"n<mtr><mtd/><mtd><mrow><mrow><mo>⇒</mo><mrow><mo>(</mo><mrow><mrow><mi>p</mi><mi>q</mi></mrow><mo>+</mo><mrow><mi>p</mi><mi>r</mi></mrow><mo>+</mo><mrow><mi>q</mi><mi>r</mi></mrow></mrow><mo>)</mo></mrow></mrow><mo>=</mo><mo>−</mo></mrow><mrow><mrow><mrow><mn>3</mn><msup><mi>x</mi><mn>2</mn></msup></mrow><mo>+</mo><mrow><mn>2</mn><mi>x</mi><mrow><mo>(</mo><mrow><mi>p</mi><mo>+</mo><mi>q</mi><mo>+</mo><mi>r</mi></mrow><mo>)</mo></mrow></mrow></mrow><mo>=</mo><mo>−</mo></mrow><mrow><mn>3</mn><mrow><mo>(</mo><mrow><msup><mi>x</mi><mn>2</mn></msup><mo>−</mo><mrow><mstyle displaystyle=\"false\"><mfrac><mn>2</mn><mn>3</mn></mfrac></mstyle><mi>x</mi><mrow><mo>(</mo><mrow><mi>p</mi><mo>+</mo><mi>q</mi><mo>+</mo><mi>r</mi></mrow><mo>)</mo></mrow></mrow></mrow><mo>)</mo></mrow></mrow><mo>,</mo></mtd></mtr>" +
				"<mtr><mtd/><mtd><mrow><mo>=</mo><mo>−</mo><mrow><mn>3</mn><mrow><mo>(</mo><mrow><msup><mrow><mo>(</mo><mrow><mi>x</mi><mo>−</mo><mrow><mstyle displaystyle=\"false\"><mfrac><mn>1</mn><mn>3</mn></mfrac></mstyle><mrow><mo>(</mo><mrow><mi>p</mi><mo>+</mo><mi>q</mi><mo>+</mo><mi>r</mi></mrow><mo>)</mo></mrow></mrow></mrow><mo>)</mo></mrow><mn>2</mn></msup><mo>−</mo><mfrac><msup><mrow><mo>(</mo><mrow><mi>p</mi><mo>+</mo><mi>q</mi><mo>+</mo><mi>r</mi></mrow><mo>)</mo></mrow><mn>2</mn></msup><mn>9</mn></mfrac></mrow><mo>)</mo></mrow></mrow></mrow><mrow><mtext>bycompletingthesquare.</mtext></mrow></mtd></mtr>" +
				"</mtable></math>";

		// The error was caused by the m:mtr template in normalise.xsl - see
		// comment in that file. A slight change to the syntax fixed it. This looks
		// to be a bug in the XSLT system rather than anything actually wrong with
		// our code.
		Transformer t = pool.reserve();
		StringWriter out = new StringWriter();
		t.transform(
			new StreamSource(new StringReader(maths1)),
			new StreamResult(out));
		t.reset();
		t.transform(
			new StreamSource(new StringReader(maths2)),
			new StreamResult(out));
		pool.release(t);
	}
}
