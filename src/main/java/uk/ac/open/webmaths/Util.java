package uk.ac.open.webmaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;

public class Util {

	/**
	 * Reads a string from a Reader character stream.
	 * @param reader Input stream
	 * @return String
	 * @throws IOException Any error
	 */
	public static String loadFromReader(Reader reader) throws IOException
	{
		char[] buffer = new char[65536];
		StringWriter writer = new StringWriter();
		while(true)
		{
			int read = reader.read(buffer);
			if(read == -1)
			{
				break;
			}
			writer.write(buffer, 0, read);
		}
		reader.close();
		return writer.toString();
	}

	/**
	 * Reads a string from a file.
	 * @param path Path
	 * @return String
	 * @throws IOException Error loading
	 */
	public static String loadFile(File path) throws java.io.IOException
	{
		return loadFromReader(new InputStreamReader(
			new FileInputStream(path), Charset.forName("UTF-8")));
	}

	/**
	 * Loads a template from classpath.
	 * @param filename Filename of template
	 * @return Template file as string
	 * @throws IllegalArgumentException If template of that name can't be loaded
	 */
	public static String loadFromClasspath(String filename) throws IllegalArgumentException
	{
		try
		{
			return loadFromReader(new java.io.InputStreamReader(
				Util.class.getResourceAsStream(filename), Charset.forName("UTF-8")));
		}
		catch(java.io.IOException e)
		{
			throw new IllegalArgumentException("Failed to read template " + filename);
		}
	}
	
	/**
	 * Copies from an input stream to output stream, then closes.
	 * 
	 * @param in Input
	 * @param out Output
	 * @throws IOException
	 */
	public static void copy(InputStream in, OutputStream out) throws IOException
	{
		byte[] buffer = new byte[65536];
		while(true)
		{
			int read = in.read(buffer);
			if (read == -1)
			{
				break;
			}
			out.write(buffer, 0, read);
		}
		in.close();
		out.close();		
	}

}
