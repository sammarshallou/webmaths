package uk.ac.open.lts.webmaths.mathjax;

/**
 * Exception thrown when MathJax gives an error.
 */
public class MathJaxException extends Exception
{
	MathJaxException(String message)
	{
		super(message);
	}
}
