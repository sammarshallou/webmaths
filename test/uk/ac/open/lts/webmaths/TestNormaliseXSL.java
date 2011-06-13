package uk.ac.open.lts.webmaths;

import java.io.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import junit.framework.TestCase;

import org.junit.*;

import uk.ac.open.lts.webmaths.english.WebMathsEnglish;

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
		assertResult("<mrow><mi>x</mi></mrow>", "<mi>x</mi>");
		innerTestMrowAdding("msqrt");
		innerTestMrowAdding("mstyle");
		innerTestMrowAdding("merror");
		innerTestMrowAdding("mphantom");
		innerTestMrowAdding("menclose");
		innerTestMrowAdding("mtd");
	}
	
	private void innerTestMrowAdding(String tag) throws Exception
	{
		assertResult("<mrow><" + tag + "><mrow><mi>x</mi></mrow></" + tag + "></mrow>",
			"<" + tag + "><mi>x</mi></" + tag + ">"); 
		assertResult("<mrow><" + tag + "><mrow><mi>x</mi></mrow></" + tag + "></mrow>",
			"<" + tag + "><mrow><mi>x</mi></mrow></" + tag + ">"); 
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
