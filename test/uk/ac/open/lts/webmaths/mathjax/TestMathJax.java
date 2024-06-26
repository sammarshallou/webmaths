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

Copyright 2015 The Open University
*/
package uk.ac.open.lts.webmaths.mathjax;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.servlet.ServletContext;

import org.junit.*;

public class TestMathJax
{
	/**
	 * SVG returned by MathJax-node-sre for TeX 'x'.
	 */
	final static String SVG_X =
		"<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
		+ "width=\"1.33ex\" height=\"1.676ex\" style=\"vertical-align: -0.338ex;\" "
		+ "viewBox=\"0 -576.1 572.5 721.6\" role=\"img\" focusable=\"false\" "
		+ "xmlns=\"http://www.w3.org/2000/svg\" "
		+ "aria-labelledby=\"MathJax-SVG-1-Title\">\n" 
		+ "<title id=\"MathJax-SVG-1-Title\">x</title>\n" 
		+ "<defs aria-hidden=\"true\">\n" 
		+ "<path stroke-width=\"1\" id=\"E1-MJMATHI-78\" d=\"M52 289Q59 331 106 "
		+ "386T222 442Q257 442 286 424T329 379Q371 442 430 442Q467 442 494 "
		+ "420T522 361Q522 332 508 314T481 292T458 288Q439 288 427 299T415 "
		+ "328Q415 374 465 391Q454 404 425 404Q412 404 406 402Q368 386 350 "
		+ "336Q290 115 290 78Q290 50 306 38T341 26Q378 26 414 59T463 140Q466 150 "
		+ "469 151T485 153H489Q504 153 504 145Q504 144 502 134Q486 77 440 33T333 "
		+ "-11Q263 -11 227 52Q186 -10 133 -10H127Q78 -10 57 16T35 71Q35 103 54 "
		+ "123T99 143Q142 143 142 101Q142 81 130 66T107 46T94 41L91 40Q91 39 97 "
		+ "36T113 29T132 26Q168 26 194 71Q203 87 217 139T245 247T261 313Q266 "
		+ "340 266 352Q266 380 251 392T217 404Q177 404 142 372T93 290Q91 281 88 "
		+ "280T72 278H58Q52 284 52 289Z\"></path>\n"
		+ "</defs>\n"
		+ "<g stroke=\"currentColor\" fill=\"currentColor\" stroke-width=\"0\" "
		+ "transform=\"matrix(1 0 0 -1 0 0)\" aria-hidden=\"true\">\n"
		+ " <use xlink:href=\"#E1-MJMATHI-78\" x=\"0\" y=\"0\"></use>\n"
		+ "</g>\n"
		+ "</svg>";

	/**
	 * An excerpt (just the view box etc) from q^{z^y}, which had rounding issues
	 */
	final static String SVG_QZY_EXCERPT =
		"<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
		+ "style=\"vertical-align: -0.5ex; margin-left: 0ex; margin-right: 0ex; "
		+ "margin-bottom: 1px; margin-top: 1px;\" width=\"3ex\" height=\"2.667ex\" "
		+ "viewBox=\"0 -943.5 1263.3 1161.4\" xmlns=\"http://www.w3.org/2000/svg\" "
		+ "role=\"math\" aria-labelledby=\"MathJax-SVG-1-Title MathJax-SVG-1-Desc\">"
		+ "<title id=\"MathJax-SVG-1-Title\">Equation</title>\n"
		+ "</svg>";

	/**
	 * MathML return by MathJax-node-sre (with --semantics) for TeX 'x'.
	 */
	final static String MATHML_X =
		"<math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"block\" "
		+ "alttext=\"x\">\n"
		+ "  <semantics>"
		+ "    <mi>x</mi>"
		+ "    <annotation encoding=\"application/x-tex\">x</annotation>"
		+ "  </semantics>"
		+ "</math>";

		/**
	 * Mock of the MathJax.Node executable handler so that we can run tests
	 * without running MathJAx.Node.
	 */
	private class MathJaxNodeExecutableMock extends MathJaxNodeExecutable
	{
		private InputEquation expected;
		private ConversionResults results;

		/**
		 * Constructor for testing.
		 */
		public MathJaxNodeExecutableMock()
		{
		}

		/**
		 * Sets the next expected equation.
		 * @param expected Equation
		 * @param svg SVG
		 * @param mathml MathML or "" if none
		 */
		void expect(InputEquation expected, String svg, String mathml)
		{
			checkNothingExpected();
			this.expected = expected;
			this.results = new ConversionResults(svg, mathml);
		}

		/**
		 * Checks that the expected parameter was used.
		 */
		void checkNothingExpected()
		{
			assertNull(expected);
		}

		@Override
		public ConversionResults convertEquation(InputEquation eq)
			throws IOException, MathJaxException
		{
			assertNotNull("Not expecting a convert call", expected);
			assertEquals("Equation does not match expected", expected, eq);
			expected = null;
			ConversionResults local = results;
			results = null;
			return local;
		}
	}

	/**
	 * Test version of the MathJax class. Only change is to use the mock executable.
	 */
	private class MathJaxTester extends MathJax
	{
		protected MathJaxTester()
		{
			super(null);
		}

		@Override
		protected MathJaxNodeExecutable createExecutable(ServletContext servletContext)
		{
			return mockExecutable;
		}
	}

	private MathJaxNodeExecutableMock mockExecutable;
	private MathJaxTester mathJax;

	@Before
	public void before()
	{
		mockExecutable = new MathJaxNodeExecutableMock();
		mathJax = new MathJaxTester();
	}

	@After
	public void after()
	{
		mockExecutable.checkNothingExpected();
	}

	@Test
	public void testGetSvg() throws Exception
	{
		InputEquation eq = new InputTexDisplayEquation("x", null);

		// First test with 'no change' settings. It still removes the title.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		String svg = mathJax.getSvg(eq, false, MathJax.SIZE_IN_EX, null);
		assertTrue(svg.contains("<title id=\"MathJax-SVG-1-Title\">"));
		assertFalse(svg.contains("<desc"));
		assertTrue(svg.contains("aria-labelledby=\"MathJax-SVG-1-Title\""));

		// Convert ex to pixels.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, false, 10.0, null);

		// Check that vertical-align, width, and height were all converted (*10).
		assertTrue(svg.contains("vertical-align: -3.3800px"));
		assertTrue(svg.contains("width=\"13.3000px\""));
		assertTrue(svg.contains("height=\"16.7600px\""));

		// Change the colour.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, false, MathJax.SIZE_IN_EX, "#ff0000");
		assertTrue(svg.contains("fill=\"#ff0000\""));
		assertTrue(svg.contains("stroke=\"#ff0000\""));

		// Try changing the colour when the colours are the wrong way around.
		mockExecutable.expect(eq,
			SVG_X.replace("stroke=\"black\" fill=\"black\"", "fill=\"black\" stroke=\"black\""),
			MATHML_X);
		svg = mathJax.getSvg(eq, false, MathJax.SIZE_IN_EX, "#ff0000");
		assertTrue(svg.contains("stroke=\"#ff0000\""));
		assertTrue(svg.contains("fill=\"#ff0000\""));

		// Fix the baseline (in ex). This is calculated by the location of 0 in
		// the co-ordinate system. In this case the view box starts at -576.1
		// with height of 721.6 (meaning the difference between zero and bottom is
		// 145.5 units => negative vertical align of -145.5 units), and the ex height is
		// recalculated to 428 units per ex. -145.5 / 428 = about -0.3ex.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, true, MathJax.SIZE_IN_EX, null);
		assertTrue(svg.contains("vertical-align: -0.3400ex"));

		// Now fix the baseline in pixels. Same as above except that it adjusts
		// the size above and below the baseline to be an integer number of pixels
		// by changing the view box.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, true, 10.0, null);

		// I calculated the correct sizes in Excel (it's quite complicated) to
		// verify that they match.
		assertTrue(svg.contains("viewBox=\"0.0 -599.2000 572.5 770.4000\""));
		assertTrue(svg.contains("height=\"18px\""));
		assertTrue(svg.contains("vertical-align: -4px"));

		// Now try for q^{z^y}, which had rounding problems.
		mockExecutable.expect(eq, SVG_QZY_EXCERPT, MATHML_X);
		svg = mathJax.getSvg(eq, true, 7.26667, null);

		assertTrue(svg.contains("viewBox=\"0.0 -1001.2839 1263.3 1236.8802\""));
		assertTrue(svg.contains("height=\"21px\""));
		assertTrue(svg.contains("vertical-align: -4px"));

		// Check case with bogus width.
		mockExecutable.expect(eq, SVG_X.replace(
			"viewBox=\"0 -576.1 572.5 721.6\"", "viewBox=\"0 -476.1 1000000.0 721.6\""), MATHML_X);
		try
		{
			svg = mathJax.getSvg(eq, true, 7.26667, null);
			fail();
		}
		catch(MathJaxException e)
		{
			// Error is about the \\ in equations which causes this
			assertTrue(e.getMessage().contains("\\\\"));
		}
	}

	@Test
	public void testOffsetSvg() throws Exception
	{
		// Try with one that doesn't use pixels and see if we get the error.
		try
		{
			MathJax.offsetSvg(SVG_X, 0.5);
			fail();
		}
		catch(IllegalArgumentException e)
		{
			assertTrue(e.getMessage().contains("no height in px"));
		}

		// Convert to pixels.
		InputEquation eq = new InputTexDisplayEquation("x", null);
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		String svg = mathJax.getSvg(eq, true, 10.0, null);

		// Try with one that doesn't have a valid viewBox and see if we get the
		// other error.
		try
		{
			MathJax.offsetSvg(svg.replace("viewBox", "viewbbbbox"), 0.5);
			fail();
		}
		catch(IllegalArgumentException e)
		{
			assertTrue(e.getMessage().contains("no viewBox"));
		}

		// Original viewbox: 0.0 -599.2000 572.5 770.4000 (18px)
		// 1 pixel is 42.8 units, 0.1 pixels is 4.28.

		// Try moving it UP 0.1 pixels.
		String up = MathJax.offsetSvg(svg, 0.1);
		assertTrue(up.contains("viewBox=\"0.0 -637.7200 572.5 813.2000\""));
		assertTrue(up.contains("height=\"19px"));
		assertTrue(up.contains("vertical-align: -4px"));

		// Move it DOWN 0.1 pixels. The baseline should change.
		String down = MathJax.offsetSvg(svg, -0.1);
		assertTrue(down.contains("viewBox=\"0.0 -603.4800 572.5 813.2000\""));
		assertTrue(down.contains("height=\"19px"));
		assertTrue(down.contains("vertical-align: -3px"));
	}

	@Test
	public void testGetEnglish() throws Exception
	{
		// Try basic case with TeX equation.
		InputEquation eq = new InputTexDisplayEquation("x", null);
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		assertEquals("x", mathJax.getEnglish(eq));

		// Try with MathML equation (does not need to use the executable).
		eq = new InputMathmlEquation(MATHML_X, null);
		assertEquals("x", mathJax.getEnglish(eq));

		// MathML equation without alt text but with TeX (will use executable again).
		String mathmlWithoutAlt = MATHML_X.replaceFirst("alttext=\"[^\"]+\"", "");
		eq = new InputMathmlEquation(mathmlWithoutAlt, null);
		mockExecutable.expect(new InputTexDisplayEquation("x", null), SVG_X, MATHML_X);
		assertEquals("x", mathJax.getEnglish(eq));

		// MathML equation without alt text or TeX.
		String mathmlWithoutTeX = mathmlWithoutAlt.replaceFirst("<annotation.*?</annotation>", "");
		eq = new InputMathmlEquation(mathmlWithoutTeX, null);
		mockExecutable.expect(eq, SVG_X, "");
		assertEquals("x", mathJax.getEnglish(eq));
	}

	@Test
	public void testGetEnglishFromSvg() throws Exception
	{
		assertEquals("x", mathJax.getEnglishFromSvg(SVG_X));

		// If there is no title then this should fail.
		String svgNoTitle = SVG_X.replaceFirst("<title.*?</title>", "");
		try
		{
			mathJax.getEnglishFromSvg(svgNoTitle);
			fail();
		}
		catch(IllegalArgumentException e)
		{
			assertTrue(e.getMessage().contains("does not include <title>"));
		}

		// If there is a title but empty, it should still work.
		String svgEmptyTitle = SVG_X.replaceFirst("(<title[^>]*>).*?(</title>)", "$1$2");
		assertEquals("", mathJax.getEnglishFromSvg(svgEmptyTitle));
	}

	@Test
	public void testGetEps() throws Exception
	{
		InputEquation eq = new InputTexDisplayEquation("x", null);
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		byte[] eps = mathJax.getEps(eq, 7.26667, null);
		String header = new String(Arrays.copyOfRange(eps, 0, 10),
			Charset.forName("ISO-8859-1"));
		assertEquals("%!PS-Adobe", header);
	}

	@Test
	public void testGetExBaselineFromSvg() throws Exception
	{
		// Get baseline from ex SVG.
		assertEquals(0.338, mathJax.getExBaselineFromSvg(SVG_X), 0.000001);

		// Check we get an error if it's a pixel SVG (no ex).
		try
		{
			// Get pixel SVG.
			InputEquation eq = new InputTexDisplayEquation("x", null);
			mockExecutable.expect(eq, SVG_X, MATHML_X);
			String svg = mathJax.getSvg(eq, true, 10.0, null);

			// Attempt to get ex baseline from it.
			mathJax.getExBaselineFromSvg(svg);
			fail();
		}
		catch(IllegalArgumentException e)
		{
			assertTrue(e.getMessage().contains("failure detecting baseline"));
		}
	}

	@Test
	public void testGetPxBaselineFromSvg() throws Exception
	{
		// Get pixel SVG.
		InputEquation eq = new InputTexDisplayEquation("x", null);
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		String svg = mathJax.getSvg(eq, true, 10.0, null);

		// Get baseline from it.
		assertEquals(4.0, mathJax.getPxBaselineFromSvg(svg), 0.000001);

		// Check we get an error if it's not a pixel SVG.
		try
		{
			mathJax.getPxBaselineFromSvg(SVG_X);
			fail();
		}
		catch(IllegalArgumentException e)
		{
			assertTrue(e.getMessage().contains("failure detecting baseline"));
		}
	}

	@Test
	public void testGetMathml() throws Exception
	{
		InputTexEquation eq = new InputTexDisplayEquation("x", null);
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		assertEquals(MATHML_X, mathJax.getMathml(eq));
	}

	@Test
	public void testGetPngFromSvg() throws Exception
	{
		// Get pixel SVG.
		InputEquation eq = new InputTexDisplayEquation("x", null);
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		String svg = mathJax.getSvg(eq, true, 10.0, null);

		// Convert to PNG.
		byte[] png = mathJax.getPngFromSvg(svg);

		// Check it's a reasonable length and the first 4 bytes match the PNG header.
		assertTrue(png.length > 100);
		assertArrayEquals(new byte[] { (byte)0x89, 0x50, 0x4e, 0x47 },
			Arrays.copyOfRange(png, 0, 4));
	}

}
