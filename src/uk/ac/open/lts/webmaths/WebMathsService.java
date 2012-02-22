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
package uk.ac.open.lts.webmaths;

import java.io.*;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.xml.parsers.*;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Base class for maths services including shared code.
 */
public class WebMathsService
{
	@Resource
	private WebServiceContext context;

	/**
	 * MathML namespace.
	 */
	public final static String NS = "http://www.w3.org/1998/Math/MathML";

	private static MathmlEntityFixer localFixer;
	
	private final static Pattern REGEX_DOCTYPE = Pattern.compile(
		"^\\s*<!DOCTYPE[^>]+>");

	/**
	 * Parses a MathML string.
	 * @param xml MathML content
	 * @return XML document
	 * @throws Exception Any error
	 */
	public Document parseMathml(String xml) throws Exception
	{
		// Get rid of doctype if supplied
		xml = REGEX_DOCTYPE.matcher(xml).replaceFirst("");
		// Fix entities
		xml = getFixer().fix(xml);
		// Parse final string
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}

	/**
	 * @return The fixer object
	 */
	protected MathmlEntityFixer getFixer()
	{
		ServletContext servletContext = null;
		if(context != null)
		{
			servletContext = (ServletContext)context.getMessageContext().get(
				MessageContext.SERVLET_CONTEXT);
		}
		
		if(servletContext != null)
		{
			synchronized(servletContext)
			{
				String key = "uk.ac.open.lts.webmaths.Fixer";
				MathmlEntityFixer fixer = (MathmlEntityFixer)servletContext.getAttribute(key);
				if(fixer == null)
				{
					try
					{
						fixer = new MathmlEntityFixer();
					}
					catch(IOException e)
					{
						throw new Error(e);
					}
					servletContext.setAttribute(key, fixer);
				}
				return fixer;
			}
		}
		else
		{
			// If this isn't running as part of a servlet, we'll use a static - 
			// I don't trust statics in webapps.
			synchronized(WebMathsService.class)
			{
				if(localFixer == null)
				{
					try
					{
						localFixer = new MathmlEntityFixer();
					}
					catch(IOException e)
					{
						throw new Error(e);
					}
				}
				return localFixer;
			}
		}
	}
}