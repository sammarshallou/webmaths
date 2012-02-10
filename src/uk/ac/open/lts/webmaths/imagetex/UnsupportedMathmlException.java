package uk.ac.open.lts.webmaths.imagetex;

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
