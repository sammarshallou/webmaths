package uk.ac.open.webmaths;

import javax.servlet.*;

import uk.ac.open.webmaths.mathjax.MathJax;

/**
 * Receives events when the servlet context is destroyed.
 */
public class WebMathsServletContextListener implements ServletContextListener
{
	@Override
	public void contextDestroyed(ServletContextEvent e)
	{
		MathJax.cleanup(e.getServletContext());
	}

	@Override
	public void contextInitialized(ServletContextEvent e)
	{
	}
}
