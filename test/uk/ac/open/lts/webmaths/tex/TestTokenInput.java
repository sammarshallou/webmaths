package uk.ac.open.lts.webmaths.tex;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.junit.Test;
import org.w3c.dom.*;

import uk.ac.open.lts.webmaths.*;
import static uk.ac.open.lts.webmaths.tex.LatexToMathml.NS;

public class TestTokenInput extends TestCase
{
	@Test
	public void testSimple()
	{
		TokenInput tokens = new TokenInput("*", null);
		assertEquals("*", tokens.nextToken());
		assertEquals(null, tokens.nextToken());
	}
	
	@Test
	public void testOverrun()
	{
		TokenInput tokens = new TokenInput("*", null);
		tokens.nextToken();
		tokens.nextToken();
		assertEquals(null, tokens.nextToken());
		assertEquals(null, tokens.nextToken());
	}
	
	@Test
	public void testPeek()
	{
		TokenInput tokens = new TokenInput("*", null);
		assertEquals("*", tokens.peekToken());
		assertEquals("*", tokens.peekToken());
		tokens.nextToken();
		tokens.nextToken();
		assertEquals(null, tokens.peekToken());
	}
	
	@Test
	public void testPeekAhead()
	{
		TokenInput tokens = new TokenInput("1*2", null);
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
		TokenInput tokens = new TokenInput("1*", null);
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
			"\\displaystyle {\\frac{11^{3}\\times 11^{4}}{11^{5}}}\\,", null);
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
	
	private TokenInput getTokenInputForSave() throws IOException
	{
		MathmlEntityFixer fixer = new MathmlEntityFixer();
		TransformerPool postProcess = new TransformerPool(
			fixer, WebMathsTex.class, "postprocess.xsl");
		TokenInput tokens = new TokenInput("", postProcess);
		return tokens;
	}
	
	@Test
	public void testSaveXml() throws Exception
	{
		TokenInput tokens = getTokenInputForSave();
		
		Document doc = 
			DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		String ns = " xmlns=\"" + NS + "\"";
		Element frog = doc.createElement("frog");
		Element tadpole = doc.createElement("tadpole");
		frog.setAttribute("sound", "ribbit");
		frog.appendChild(tadpole);
		frog.appendChild(doc.createTextNode("Ribbit!"));
		
		assertEquals("<frog" + ns + " sound=\"ribbit\"><tadpole/>Ribbit!</frog>", 
			tokens.saveXml(frog));
	}

	@Test
	public void testSaveXmlPost() throws Exception
	{
		TokenInput tokens = getTokenInputForSave();
		
		Document doc = 
			DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		String ns = " xmlns=\"" + NS + "\"";
		Element mrow = doc.createElementNS(NS, "mrow");
		Element mn = doc.createElementNS(NS, "mn");
		mn.appendChild(doc.createTextNode("1"));
		mrow.appendChild(mn);
		mn = doc.createElementNS(NS, "mn");
		mn.appendChild(doc.createTextNode("2"));
		mrow.appendChild(mn);
		
		assertEquals("<mn" + ns + ">12</mn>", tokens.saveXml(mrow));
		
		Element grr = doc.createElementNS(NS, "grr");
		mrow.appendChild(grr);
		assertEquals("<mrow" + ns + "><mn>1</mn><mn>2</mn><grr/></mrow>",
			tokens.saveXml(mrow));
	}
	
}
