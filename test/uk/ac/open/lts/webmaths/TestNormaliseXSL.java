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

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.*;

import junit.framework.TestCase;

import org.junit.*;

public class TestNormaliseXSL extends TestCase
{
	private TransformerPool pool;
	
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
}
