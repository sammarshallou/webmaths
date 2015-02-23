package uk.ac.open.lts.webmaths;

import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 * Carries out transformations using MathJax.node via an application copied to
 * the server.
 */
@WebListener()
public class MathJax
{
	/** Name of attribute in ServletContext that stores singleton value. */
	private static final String ATTRIBUTE_NAME = "uk.ac.open.lts.webmaths.MathJax";

	public synchronized static MathJax get(WebServiceContext context)
	{
		ServletContext servletContext =
			(ServletContext)context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);

		MathJax mathJax = (MathJax)servletContext.getAttribute(ATTRIBUTE_NAME);
		if(mathJax == null)
		{
			mathJax = new MathJax();
			servletContext.setAttribute("", mathJax);
		}
		return mathJax;
	}

}
