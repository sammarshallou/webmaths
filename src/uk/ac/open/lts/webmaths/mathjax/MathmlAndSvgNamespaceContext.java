package uk.ac.open.lts.webmaths.mathjax;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public final class MathmlAndSvgNamespaceContext implements NamespaceContext
{
	@Override
	public String getNamespaceURI(String prefix)
	{
		if(prefix.equals("m"))
		{
			return "http://www.w3.org/1998/Math/MathML";
		}
		else if(prefix.equals("s"))
		{
			return "http://www.w3.org/2000/svg";
		}
		else
		{
			return XMLConstants.NULL_NS_URI;
		}
	}

	@Override
	public String getPrefix(String uri)
	{
		if(uri.equals("http://www.w3.org/1998/Math/MathML"))
		{
			return "m";
		}
		else if(uri.equals("http://www.w3.org/2000/svg"))
		{
			return "s";
		}
		return null;
	}

	@Override
	public Iterator<?> getPrefixes(String uri)
	{
		LinkedList<String> list = new LinkedList<String>();
		list.add(getPrefix(uri));
		return list.iterator();
	}
}