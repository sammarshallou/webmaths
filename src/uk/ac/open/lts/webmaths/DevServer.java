package uk.ac.open.lts.webmaths;

import java.io.IOException;

import javax.xml.ws.Endpoint;

/**
 * Manually publishes service endpoints for testing on developer machine.
 */
public class DevServer
{
	/**
	 * @param args Ignored
	 */
	public static void main(String[] args) throws IOException
	{
		MathmlEntityFixer fixer = new MathmlEntityFixer();
		Endpoint.publish("http://pclt1507.open.ac.uk:9998/", new WebMathsEnglish(fixer));
		Endpoint.publish("http://pclt1507.open.ac.uk:9999/", new WebMathsImage(fixer));
	}
}
