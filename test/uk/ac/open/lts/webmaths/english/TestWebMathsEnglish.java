package uk.ac.open.lts.webmaths.english;

import junit.framework.TestCase;

import org.junit.*;

public class TestWebMathsEnglish extends TestCase
{
	private WebMathsEnglish english;
	
	@Before
	public void setUp() throws Exception
	{
		english = new WebMathsEnglish();
	}
	
	@Test
	public void testExcessiveBrackets() throws Exception
	{
		assertEnglish("( 1 minus 0.498 ) squared", 
			"<msup>" +
				"<mrow>" +
					"<mo>(</mo>" +
					"<mrow>" +
						"<mrow>" +
							"<mn>1</mn>" +
							"<mo>−</mo>" +
							"<mn>0</mn>" +
						"</mrow>" + 
			  		"<mtext>.</mtext>" +
						"<mn>498</mn>" +
					"</mrow>" +
					"<mo>)</mo>" +
				"</mrow>" +
				"<mn>2</mn>" +
			"</msup>");
	}
	
	@Test
	public void testOuterBrackets() throws Exception
	{
		assertEnglish("1 over z", "<mfrac><mn>1</mn><mi>z</mi></mfrac>");
	}
	
	public void testMenclose() throws Exception
	{
		assertEnglish("( upward diagonal strike through 2 to the power 1 over " +
			"upward diagonal strike through 8 subscript 4 ) = ( 1 over 4 ) .",
			"<math display='block' xmlns='http://www.w3.org/1998/Math/MathML'><mrow>" +
				"<mfrac>" +
					"<mrow>" +
						"<msup>" +
							"<menclose notation='updiagonalstrike'><mn>2</mn></menclose>" +
							"<mn>1</mn>" +
						"</msup>" +
					"</mrow>" +
					"<mrow>" +
						"<msub>" +
							"<menclose notation='updiagonalstrike'><mn>8</mn></menclose>" +
							"<mn>4</mn>" +
						"</msub>" +
					"</mrow>" +
				"</mfrac>" +
				"<mo>=</mo>" +
				"<mfrac>" +
					"<mn>1</mn>" +
					"<mn>4</mn>" +
				"</mfrac>" +
				"<mo>.</mo>" +
			"</mrow></math>");
	}
	
	public void testPlusOrMinus() throws Exception
	{
		assertEnglish("plus or minus 5", "<mo>&#xb1;</mo><mn>5</mn>");
	}

	private void assertEnglish(String expected, String mathml) throws Exception
	{
		MathsEnglishParams params = new MathsEnglishParams();
		params.setMathml("<math xmlns=\"http://www.w3.org/1998/Math/MathML\">"
			+ mathml + "</math>");
		MathsEnglishReturn result = english.getEnglish(params);
		assertTrue(result.ok);
		assertEquals(expected, result.english);
	}

}
