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
import java.net.*;
import java.util.*;

import javax.xml.ws.Endpoint;

import org.apache.xmlgraphics.util.ClasspathResource;

import uk.ac.open.lts.webmaths.english.WebMathsEnglish;
import uk.ac.open.lts.webmaths.image.*;
import uk.ac.open.lts.webmaths.imagetex.WebMathsImageTex;
import uk.ac.open.lts.webmaths.tex.WebMathsTex;

/**
 * Manually publishes service endpoints for testing on developer machine.
 */
public class DevServer
{
	private static boolean CHECK_FONTS = false, WRITE_IMAGE = false;

	/**
	 * @param args Ignored
	 */
	public static void main(String[] args) throws IOException
	{
		// Run special test methods if needed
		if(CHECK_FONTS)
		{
			checkFonts();
			return;
		}
		if(WRITE_IMAGE)
		{
			writeImage();
			return;
		}

		String local = InetAddress.getLocalHost().getHostAddress();
		String mathsTex = "http://" + local + ":9997/";
		Endpoint.publish(mathsTex, new WebMathsTex());
		System.err.println("MathsTex service ready - " + mathsTex);
		String mathsEnglish = "http://" + local + ":9998/";
		Endpoint.publish(mathsEnglish, new WebMathsEnglish());
		System.err.println("MathsEnglish service ready - " + mathsEnglish);
		String mathsImage = "http://" + local + ":9999/";
		Endpoint.publish(mathsImage, new WebMathsImage());
		System.err.println("MathsImage service ready - " + mathsImage);
		String mathsImageTex = "http://" + local + ":9996/";
		Endpoint.publish(mathsImageTex, new WebMathsImageTex());
		System.err.println("MathsImageTex service ready - " + mathsImageTex);
	}

	/**
	 * Test method: Displays the list of available fonts so we can check
	 * the font jar files are installed correctly.
	 */
	private static void checkFonts()
	{
		Set<String> fonts = new TreeSet<String>();
		for(Object o : ClasspathResource.getInstance().listResourcesOfMimeType("application/x-font"))
		{
		URL url = (URL)o;
		fonts.add(url.getPath().replaceFirst(".*/", ""));
		}
		System.err.println("Available fonts");
		System.err.println("===============");
		System.err.println();
		for(String font : fonts)
		{
			System.err.println(font);
		}
		System.err.println();
	}

	/**
	 * Test method: Writes an image directly to save having to call the service
	 * when repeatedly testing.
	 * @throws IOException Any error
	 */
	private static void writeImage() throws IOException
	{
		WebMathsImage image = new WebMathsImage();
		MathsImageParams params = new MathsImageParams();
		params.setRgb("#000000");
		params.setSize(4.0f);
		String input = "<math xmlns='http://www.w3.org/1998/Math/MathML'>"
			+ "<mo>&#x2113;</mo></math>";
		params.setMathml(input);
		MathsImageReturn result = image.getImage(params);
		File file = new File(new File(System.getProperty("user.home")), "out.png");
		FileOutputStream stream = new FileOutputStream(file);
		stream.write(result.getImage());
		stream.close();
		System.out.println("Image written to: " + file.getAbsolutePath());
	}
}
