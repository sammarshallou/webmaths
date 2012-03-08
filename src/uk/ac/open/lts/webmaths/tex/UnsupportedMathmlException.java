package uk.ac.open.lts.webmaths.tex;

/**
 * Exception thrown if we can't convert MathML to TeX for some reason.
 */
public class UnsupportedMathmlException extends Exception
{
	/**
	 * @param message Error message
	 */
	public UnsupportedMathmlException(String message)
	{
		super(message);
	}
}
