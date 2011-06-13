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

import static uk.ac.open.lts.webmaths.tex.LatexToMathml.NS;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.junit.Test;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public class TestTokenInput extends TestCase
{
	@Test
	public void testSimple()
	{
		TokenInput tokens = new TokenInput("*");
		assertEquals("*", tokens.nextToken());
		assertEquals(null, tokens.nextToken());
	}
	
	@Test
	public void testOverrun()
	{
		TokenInput tokens = new TokenInput("*");
		tokens.nextToken();
		tokens.nextToken();
		assertEquals(null, tokens.nextToken());
		assertEquals(null, tokens.nextToken());
	}
	
	@Test
	public void testPeek()
	{
		TokenInput tokens = new TokenInput("*");
		assertEquals("*", tokens.peekToken());
		assertEquals("*", tokens.peekToken());
		tokens.nextToken();
		tokens.nextToken();
		assertEquals(null, tokens.peekToken());
	}
	
	@Test
	public void testPeekAhead()
	{
		TokenInput tokens = new TokenInput("1*2");
		assertEquals("1", tokens.peekToken(0));
		assertEquals("*", tokens.peekToken(1));
		assertEquals("2", tokens.peekToken(2));
		assertEquals(null, tokens.peekToken(3));
		assertEquals(null, tokens.peekToken(999));
		// Just check it didn't mess up current position
		assertEquals("1", tokens.peekToken(0));
	}
	
	@Test
	public void testBackAndOverwrite()
	{
		TokenInput tokens = new TokenInput("1*");
		assertEquals("1", tokens.nextToken());
		tokens.backAndOverwriteToken("q");
		assertEquals("q", tokens.nextToken());
		assertEquals("*", tokens.nextToken());
		assertEquals(null, tokens.nextToken());
	}
	
	@Test
	public void testReal()
	{
		TokenInput tokens = new TokenInput(
			"\\displaystyle {\\frac{11^{3}\\times 11^{4}}{11^{5}}}\\,");
		assertEquals("\\displaystyle", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("\\frac", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("1", tokens.nextToken());
		assertEquals("1", tokens.nextToken());
		assertEquals("^", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("3", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("\\times", tokens.nextToken());
		assertEquals("1", tokens.nextToken());
		assertEquals("1", tokens.nextToken());
		assertEquals("^", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("4", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("1", tokens.nextToken());
		assertEquals("1", tokens.nextToken());
		assertEquals("^", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("5", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("\\,", tokens.nextToken());
	}
	
	private Element parse(String xml) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document d = factory.newDocumentBuilder().
			parse(new InputSource(new StringReader(xml)));
		return d.getDocumentElement();
	}

	@Test
	public void testSaveXml() throws Exception
	{
		Element frog = parse("<frog sound='ribbit'><tadpole/>Ribbit!</frog>"); 
		assertEquals("<frog sound=\"ribbit\"><tadpole/>Ribbit!</frog>", 
			TokenInput.saveXml(frog));
	}
	
	public void testPostProcess() throws Exception
	{
		// Two MN in root math tag
		assertPostProcess("<mn>12</mn>", "<mn>1</mn><mn>2</mn>");
		
		// Two MN in mrow - the mrow disappears
		assertPostProcess("<mn>12</mn>", "<mrow><mn>1</mn><mn>2</mn></mrow>");
		
		// Two MN plus something else in mrow
		assertPostProcess("<mrow><grr/><mn>12</mn></mrow>",
			"<mrow><grr/><mn>1</mn><mn>2</mn></mrow>");
		
		// Three MN in mrow
		assertPostProcess("<mn>123</mn>",
			"<mrow><mn>1</mn><mn>2</mn><mn>3</mn></mrow>");
		
		// MN with a dot
		assertPostProcess("<mn>1.3</mn>",
			"<mrow><mn>1</mn><mtext>.</mtext><mn>3</mn></mrow>");

		// MN with TWO dots (it won't accept the second)
		assertPostProcess("<mrow><mn>1.3</mn><mtext>.</mtext><mn>4</mn></mrow>",
			"<mrow><mn>1</mn><mtext>.</mtext><mn>3</mn><mtext>.</mtext><mn>4</mn></mrow>");
		
		// Two things to change in one (root)
		assertPostProcess("<mn>12</mn><mo>+</mo><mn>13</mn>",
			"<mn>1</mn><mn>2</mn><mo>+</mo><mn>1</mn><mn>3</mn>");
	}
	
	private void assertPostProcess(String expected, String input) throws Exception
	{
		String ns = " xmlns=\"" + NS +"\"";
		Element root = parse("<math" + ns + ">" + input + "</math>");
		TokenInput.postProcess(root);
		assertEquals("<math" + ns + ">" + expected + "</math>", 
			TokenInput.saveXml(root));
	}
}
