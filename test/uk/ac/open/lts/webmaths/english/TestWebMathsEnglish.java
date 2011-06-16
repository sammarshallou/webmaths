package uk.ac.open.lts.webmaths.english;

import org.junit.*;

import uk.ac.open.lts.webmaths.MathmlEntityFixer;

import junit.framework.TestCase;

public class TestWebMathsEnglish extends TestCase
{
	private WebMathsEnglish english;
	
	@Before
	public void setUp() throws Exception
	{
		MathmlEntityFixer fixer = new MathmlEntityFixer();
		english = new WebMathsEnglish(fixer);
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
