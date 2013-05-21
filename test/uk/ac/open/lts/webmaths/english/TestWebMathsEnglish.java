package uk.ac.open.lts.webmaths.english;

import junit.framework.TestCase;

import org.junit.*;

import uk.ac.open.lts.webmaths.tex.TokenInput;

public class TestWebMathsEnglish extends TestCase
{
	private WebMathsEnglish english;

	@Override
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

	@Test
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

	@Test
	public void testOperatorNames() throws Exception
	{
		assertEnglish("plus or minus 5", "<mo>&#xb1;</mo><mn>5</mn>");
		assertEnglish("much greater than 5", "<mo>&#x226b;</mo><mn>5</mn>");
		assertEnglish("much less than 5", "<mo>&#x226a;</mo><mn>5</mn>");
	}

	@Test
	public void testAccents() throws Exception
	{
		assertEnglishTex("x-bar x-double-dot x-tilde x-tilde",
			"\\bar{x} \\ddot{x} \\widetilde{x} \\tilde{x}");
	}

	@Test
	public void testStyles() throws Exception
	{
		// One character mi special cases
		assertEnglish("x", "<mi mathvariant='italic'>x</mi>");
		assertEnglish("bold x", "<mi mathvariant='bold-italic'>x</mi>");

		// Two character mi (not special cases)
		assertEnglish("italic xx", "<mi mathvariant='italic'>xx</mi>");
		assertEnglish("bold italic xx", "<mi mathvariant='bold-italic'>xx</mi>");

		// mtext (not special cases)
		assertEnglish("italic x", "<mtext mathvariant='italic'>x</mtext>");
		assertEnglish("bold italic x", "<mtext mathvariant='bold-italic'>x</mtext>");

		// Other examples
		assertEnglish("3", "<mn>3</mn>");
		assertEnglish("3", "<mn mathvariant='normal'>3</mn>");
		assertEnglish("bold 3", "<mn mathvariant='bold'>3</mn>");
		assertEnglish("italic 3", "<mn mathvariant='italic'>3</mn>");
		assertEnglish("bold italic 3", "<mn mathvariant='bold-italic'>3</mn>");
		assertEnglish("double-struck 3", "<mn mathvariant='double-struck'>3</mn>");
		assertEnglish("bold fraktur 3", "<mn mathvariant='bold-fraktur'>3</mn>");
		assertEnglish("script 3", "<mn mathvariant='script'>3</mn>");
		assertEnglish("bold script 3", "<mn mathvariant='bold-script'>3</mn>");
		assertEnglish("fraktur 3", "<mn mathvariant='fraktur'>3</mn>");
		assertEnglish("sans-serif 3", "<mn mathvariant='sans-serif'>3</mn>");
		assertEnglish("bold sans-serif 3", "<mn mathvariant='bold-sans-serif'>3</mn>");
		assertEnglish("italic sans-serif 3", "<mn mathvariant='sans-serif-italic'>3</mn>");
		assertEnglish("bold italic sans-serif 3", "<mn mathvariant='sans-serif-bold-italic'>3</mn>");
		assertEnglish("monospace 3", "<mn mathvariant='monospace'>3</mn>");

		// Inheritance
		assertEnglish("bold x bold y", "<mstyle mathvariant='bold'><mi>x</mi><mi>y</mi></mstyle>");
	}

	@Test
	public void testRemovedEquals() throws Exception
	{
		assertEnglish("double dagger", "<mo>&#x2021;</mo>");
	}

	@Test
	public void testMultiCharacter() throws Exception
	{
		assertEnglish("right arrow-curved", "<mo>&#x2933;</mo>");
		assertEnglish("not right arrow-curved", "<mo>&#x2933;&#x338;</mo>");
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

	private void assertEnglishTex(String expected, String tex) throws Exception
	{
		MathsEnglishParams params = new MathsEnglishParams();
		params.setMathml(new TokenInput(tex).toMathml(true));
		MathsEnglishReturn result = english.getEnglish(params);
		assertTrue(result.ok);
		assertEquals(expected, result.english);
	}

}
