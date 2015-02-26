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

import java.util.regex.*;

import javax.xml.xpath.*;

import org.w3c.dom.Document;

/**
 * TeX equations.
 */
public abstract class InputTexEquation extends InputEquation
{
	private final static Pattern REGEX_MATHML_TEXANNOTATION = Pattern.compile(
		"^<[^>]*?( display=\"block\")?[^>]*>.*" +
		"<annotation encoding=\"application/x-tex\">([^<]+)</annotation>", Pattern.DOTALL);

	/**
	 * @param content TeX string
	 */
	protected InputTexEquation(String content)
	{
		super(content);
	}

	/**
	 * Gets the XPath expression needed for getFromMathml.
	 * @param xpath XPath processor (must have prefix 'm' for MathML)
	 * @return Expression
	 * @throws XPathExpressionException Actually shouldn't do
	 */
	public static XPathExpression getXPathExpression(XPath xpath)
		throws XPathExpressionException
	{
		return xpath.compile(
			"normalize-space(/m:math/m:semantics/m:annotation[@encoding='application/x-tex'])");
	}

	/**
	 * Gets a TeX equation from parsed MathML.
	 * @param doc MathML document
	 * @param xpathAnnotation XPath expression from {@link #getXPathExpression()}
	 * @return TeX equation or null if none included
	 */
	public static InputTexEquation getFromMathml(Document doc, XPathExpression xpathAnnotation)
	{
		try
		{
			String tex;
			synchronized(xpathAnnotation)
			{
				tex = (String)xpathAnnotation.evaluate(doc, XPathConstants.STRING);
			}
			if(tex.equals(""))
			{
				return null;
			}
			boolean display = "block".equals(doc.getDocumentElement().getAttribute("display"));
			return display ? new InputTexDisplayEquation(tex) : new InputTexInlineEquation(tex);
		}
		catch(XPathExpressionException e)
		{
			throw new Error(e);
		}
	}

	/**
	 * Gets a TeX equation from MathML as a string.
	 * @param mathml MathML string
	 * @return TeX equation or null if none included
	 */
	public static InputTexEquation getFromMathml(String mathml)
	{
		Matcher m = REGEX_MATHML_TEXANNOTATION.matcher(mathml);
		if(m.find())
		{
			String tex = m.group(2).trim();
			return m.group(1) != null ? new InputTexDisplayEquation(tex) :
				new InputTexInlineEquation(tex);
		}
		else
		{
			return null;
		}
	}
}
