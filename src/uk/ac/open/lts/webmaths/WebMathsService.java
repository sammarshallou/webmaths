package uk.ac.open.lts.webmaths;

import java.io.StringReader;
import java.util.regex.Pattern;

import javax.xml.parsers.*;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Base class for maths services including shared code.
 */
public class WebMathsService
{
	private final static Pattern REGEX_DOCTYPE = Pattern.compile(
		"^\\s*<!DOCTYPE[^>]+>");

	private MathmlEntityFixer fixer;
	
	protected WebMathsService(MathmlEntityFixer fixer)
	{
		this.fixer = fixer;
	}

	/**
	 * Parses a MathML string.
	 * @param xml MathML content
	 * @return XML document
	 * @throws Exception Any error
	 */
	protected Document parseMathml(String xml) throws Exception
	{
		// Get rid of doctype if supplied
		xml = REGEX_DOCTYPE.matcher(xml).replaceFirst("");
		// Fix entities
		xml = fixer.fix(xml);
		// Parse final string
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}
}