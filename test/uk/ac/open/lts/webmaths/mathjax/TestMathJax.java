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

import java.io.IOException;

import javax.servlet.ServletContext;

import org.junit.*;
import static org.junit.Assert.*;

public class TestMathJax
{
	/**
	 * SVG returned by MathJax.Node (with --speech option) for TeX 'x'.
	 */
	private final static String SVG_X =
		"<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
		+ "style=\"vertical-align: -0.167ex; margin-left: 0ex; margin-right: 0ex; "
		+ "margin-bottom: 1px; margin-top: 1px;\" width=\"1.333ex\" "
		+ "height=\"1.167ex\" viewBox=\"0 -465.9 577 500.9\" "
		+ "xmlns=\"http://www.w3.org/2000/svg\" role=\"math\" "
		+ "aria-labelledby=\"MathJax-SVG-1-Title MathJax-SVG-1-Desc\">\n"
		+ "<title id=\"MathJax-SVG-1-Title\">Equation</title>\n"
		+ "<desc id=\"MathJax-SVG-1-Desc\">x</desc>\n"
		+ "<defs aria-hidden=\"true\">\n"
		+ "<path stroke-width=\"10\" id=\"E1-MJMATHI-78\" d=\"M52 289Q59 331 106 "
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
		+ "<g stroke=\"black\" fill=\"black\" stroke-width=\"0\" "
		+ "transform=\"matrix(1 0 0 -1 0 0)\" aria-hidden=\"true\">\n"
		+ " <use xlink:href=\"#E1-MJMATHI-78\"></use>\n"
		+ "</g>\n"
		+ "</svg>";

	/**
	 * An excerpt (just the view box etc) from q^{z^y}, which had rounding issues
	 */
	private final static String SVG_QZY_EXCERPT =
		"<svg xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
		+ "style=\"vertical-align: -0.5ex; margin-left: 0ex; margin-right: 0ex; "
		+ "margin-bottom: 1px; margin-top: 1px;\" width=\"3ex\" height=\"2.667ex\" "
		+ "viewBox=\"0 -943.5 1263.3 1161.4\" xmlns=\"http://www.w3.org/2000/svg\" "
		+ "role=\"math\" aria-labelledby=\"MathJax-SVG-1-Title MathJax-SVG-1-Desc\">"
		+ "</svg>";

	/**
	 * MathML return by MathJax.Node (with --speech) for TeX 'x'.
	 */
	private final static String MATHML_X =
		"<math xmlns=\"http://www.w3.org/1998/Math/MathML\" display=\"block\" "
		+ "alttext=\"x\">\n"
		+ "<mi>x</mi>\n"
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
		protected MathJaxNodeExecutable createExecutable(
			ServletContext servletContext)
		{
			return mockExecutable;
		}

		public String publicOffsetSvg(String svg, double pixels) throws IOException
		{
			return MathJax.offsetSvg(svg, pixels);
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
		InputEquation eq = new InputTexDisplayEquation("x");

		// First test with 'don't mess' settings.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		String svg = mathJax.getSvg(eq, false, MathJax.SIZE_IN_EX, null);
		assertEquals(SVG_X, svg);

		// Convert ex to pixels.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, false, 10.0, null);

		// Check that vertical-align, width, and height were all converted (*10).
		assertTrue(svg.contains("vertical-align: -1.6700px"));
		assertTrue(svg.contains("width=\"13.3300px\""));
		assertTrue(svg.contains("height=\"11.6700px\""));

		// Change the colour.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, false, MathJax.SIZE_IN_EX, "#ff0000");
		assertTrue(svg.contains("<g stroke=\"#ff0000\" fill=\"#ff0000\""));

		// Fix the baseline (in ex). This is calculated by the location of 0 in
		// the co-ordinate system. In this case the view box starts at -465.9
		// with height of 500.9 (meaning the difference between zero and bottom is
		// 35 units => negative vertical align of -35 units), and the ex height is
		// recalculated to 428 units per ex (1.170ex). -35 / 500.9 * 1.170 = about -0.08.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, true, MathJax.SIZE_IN_EX, null);
		assertTrue(svg.contains("vertical-align: -0.0818ex"));

		// Also the margins are removed as these confuse the vertical-align.
		assertTrue(svg.contains("margin-bottom: 0px;"));
		assertTrue(svg.contains("margin-top: 0px;"));

		// Now fix the baseline in pixels. Same as above except that it adjusts
		// the size above and below the baseline to be an integer number of pixels
		// by changing the view box.
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		svg = mathJax.getSvg(eq, true, 10.0, null);
		assertTrue(svg.contains("margin-bottom: 0px;"));
		assertTrue(svg.contains("margin-top: 0px;"));

		// I calculated the correct sizes in Excel (it's quite complicated) to
		// verify that they match.
		assertTrue(svg.contains("viewBox=\"0.0 -470.8000 577.0 513.6000\""));
		assertTrue(svg.contains("height=\"12px\""));
		assertTrue(svg.contains("vertical-align: -1px"));

		// Now try for q^{z^y}, which had rounding problems.
		mockExecutable.expect(eq, SVG_QZY_EXCERPT, MATHML_X);
		svg = mathJax.getSvg(eq, true, 7.26667, null);
		assertTrue(svg.contains("margin-bottom: 0px;"));
		assertTrue(svg.contains("margin-top: 0px;"));

		assertTrue(svg.contains("viewBox=\"0.0 -1001.2839 1263.3 1236.8802\""));
		assertTrue(svg.contains("height=\"21px\""));
		assertTrue(svg.contains("vertical-align: -4px"));
	}

	@Test
	public void testOffsetSvg() throws Exception
	{
		// Try with one that doesn't use pixels and see if we get the error.
		try
		{
			mathJax.publicOffsetSvg(SVG_X, 0.5);
			fail();
		}
		catch(IOException e)
		{
			assertTrue(e.getMessage().contains("no height in px"));
		}

		// Convert to pixels.
		InputEquation eq = new InputTexDisplayEquation("x");
		mockExecutable.expect(eq, SVG_X, MATHML_X);
		String svg = mathJax.getSvg(eq, true, 10.0, null);

		// Try with one that doesn't have a valid viewBox and see if we get the
		// other error.
		try
		{
			mathJax.publicOffsetSvg(svg.replace("viewBox", "viewbbbbox"), 0.5);
			fail();
		}
		catch(IOException e)
		{
			assertTrue(e.getMessage().contains("no viewBox"));
		}

		// Original viewBox: 0.0 -470.8000 577.0 513.6000 (12px).
		// 1 pixel is 42.8 units, 0.1 pixels is 4.28.

		// Try moving it UP 0.1 pixels.
		String up = mathJax.publicOffsetSvg(svg, 0.1);
		assertTrue(up.contains("viewBox=\"0.0 -509.3200 577.0 556.4000\""));
		assertTrue(up.contains("height=\"13px"));
		assertTrue(up.contains("vertical-align: -1px"));

		// Move it DOWN 0.1 pixels. The baseline should change.
		String down = mathJax.publicOffsetSvg(svg, -0.1);
		assertTrue(down.contains("viewBox=\"0.0 -475.0800 577.0 556.4000\""));
		assertTrue(down.contains("height=\"13px"));
		assertTrue(down.contains("vertical-align: -2px"));
	}

}
