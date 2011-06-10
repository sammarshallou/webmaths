package uk.ac.open.lts.webmaths.tex;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.junit.Test;
import org.w3c.dom.*;

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
		assertEquals("11", tokens.nextToken());
		assertEquals("^", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("3", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("\\times", tokens.nextToken());
		assertEquals("11", tokens.nextToken());
		assertEquals("^", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("4", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("11", tokens.nextToken());
		assertEquals("^", tokens.nextToken());
		assertEquals("{", tokens.nextToken());
		assertEquals("5", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("}", tokens.nextToken());
		assertEquals("\\,", tokens.nextToken());
	}
	
	@Test
	public void testSaveXml() throws Exception
	{
		Document doc = 
			DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		
		Element frog = doc.createElement("frog");
		Element tadpole = doc.createElement("tadpole");
		frog.setAttribute("sound", "ribbit");
		frog.appendChild(tadpole);
		frog.appendChild(doc.createTextNode("Ribbit!"));
		
		assertEquals("<frog sound=\"ribbit\"><tadpole/>Ribbit!</frog>", 
			TokenInput.saveXml(frog));
		
		
	}
}
