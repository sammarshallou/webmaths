package uk.ac.open.lts.webmaths;

import java.io.IOException;

import javax.xml.ws.Endpoint;

import uk.ac.open.lts.webmaths.english.WebMathsEnglish;
import uk.ac.open.lts.webmaths.image.WebMathsImage;
import uk.ac.open.lts.webmaths.tex.WebMathsTex;

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
		Endpoint.publish("http://pclt1507.open.ac.uk:9997/", new WebMathsTex(fixer));
		Endpoint.publish("http://pclt1507.open.ac.uk:9998/", new WebMathsEnglish(fixer));
		Endpoint.publish("http://pclt1507.open.ac.uk:9999/", new WebMathsImage(fixer));
		System.out.println("Services ready");
	}
}
