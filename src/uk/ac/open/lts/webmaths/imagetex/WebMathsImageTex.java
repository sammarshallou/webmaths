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
package uk.ac.open.lts.webmaths.imagetex;

import java.awt.Color;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.*;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.*;

import uk.ac.open.lts.webmaths.image.*;
import uk.ac.open.lts.webmaths.tex.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.image.MathsImagePort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsImageTex", portName="MathsImagePort")
public class WebMathsImageTex extends WebMathsImage
{
	@Resource
	private WebServiceContext context;

	private final static int MAX_TEMP_FOLDER_ATTEMPTS = 10;

	private final static float BASE_PIXEL_SIZE = 18.0f;

	private static boolean SHOWPERFORMANCE = true, SHOW_COMMANDS = true;
	private final static byte[] EMPTY = new byte[0];

	private MathmlToLatex converter;

	private enum Mode
	{
		MATHML,
		AUTOFALLBACK,
		LATEX;

		public static Mode fromType(String type)
		{
			if(type.equals("mathml"))
			{
				return MATHML;
			}
			else if(type.equals("latex"))
			{
				return LATEX;
			}
			else if(type.equals("autofallback"))
			{
				return AUTOFALLBACK;
			}
			else
			{
				return null;
			}
		}
	};

	@Override
	public MathsImageReturn getImage(MathsImageParams params)
	{
		long start = System.currentTimeMillis();
		MathsImageReturn result = new MathsImageReturn();
		result.setOk(false);
		result.setError("");
		result.setImage(EMPTY);

		try
		{
			// Parse MathML
			Document mathml = parseMathml(params, result, start);
			if(mathml == null)
			{
				return result;
			}

			// Check for annotations indicating specific behaviour
			Mode renderingMode = null;
			NodeList annotations = mathml.getElementsByTagNameNS(NS, "annotation");
			boolean gotTex = false;
			for(int i=0; i<annotations.getLength(); i++)
			{
				// Get attribute encoding or null if none
				Element annotation = (Element)annotations.item(i);
				String encoding = annotation.getAttribute("encoding");

				// application/x-tex is the correct encoding indicating there was a
				// TeX version; TeX is included for OU legacy reasons only
				if("application/x-tex".equals(encoding) || "TeX".equals(encoding))
				{
					gotTex = true;
				}

				// application/x-webmaths is used to manually control rendering
				if("application/x-webmaths".equals(encoding))
				{
					Node child = annotation.getFirstChild();
					if(child.getNodeType() == Node.TEXT_NODE)
					{
						renderingMode = Mode.fromType(child.getNodeValue());
					}
				}
			}

			// If rendering mode is not set, default it
			if(renderingMode == null)
			{
				renderingMode = gotTex ? Mode.AUTOFALLBACK : Mode.MATHML;
			}
			if(SHOWPERFORMANCE)
			{
				System.err.println("Decide mode: " + (System.currentTimeMillis() - start));
			}

			// If we're using the MathML renderer, call that from base class
			if(renderingMode == Mode.MATHML)
			{
				return super.getImage(params, mathml, result, start);
			}

			// Convert MathML to LaTeX
			String tex;
			try
			{
				tex = getMathmlToLatex().convert(mathml, renderingMode == Mode.LATEX);
			}
			catch(UnsupportedMathmlException e)
			{
				// Fallback
				if(SHOWPERFORMANCE)
				{
					System.err.println("Selecting fallback: " + (System.currentTimeMillis() - start));
				}
				return super.getImage(params, mathml, result, start);
			}
			if(SHOWPERFORMANCE)
			{
				System.err.println("Convert to LaTeX: " + (System.currentTimeMillis() - start));
			}

			// Create the PNG
			texToPng(tex, params.getRgb(), params.getSize(), result);

			if(SHOWPERFORMANCE)
			{
				System.err.println("End: " + (System.currentTimeMillis() - start));
			}
			return result;
		}
		catch(Throwable t)
		{
			result.setError("MathML/LaTeX unexpected error - " + t.getMessage());
			t.printStackTrace();
			return result;
		}
	}

	/**
	 * @return Converter used to change MathML to LaTeX
	 */
	synchronized public MathmlToLatex getMathmlToLatex()
	{
		if(converter == null)
		{
			converter = new MathmlToLatex(getFixer());
		}
		return converter;
	}

	/**
	 * Converts TeX to an image. Value (or error) will be placed in the result
	 * parameter.
	 * @param tex TeX string
	 * @param rgb RGB code for foreground
	 * @param size Size as float (1.0 = default)
	 * @param result Out parameter; output image goes here
	 * @throws IOException
	 */
	private void texToPng(String tex, String rgb, float size,
		MathsImageReturn result) throws IOException, InterruptedException,
			IllegalArgumentException
	{
		// Get latex and dvipng executable paths, and temp folder
		ServletContext servletContext = (ServletContext)context.getMessageContext().get(
			MessageContext.SERVLET_CONTEXT);
		String latex = null, dvipng = null, temp = null;
		if(servletContext != null)
		{
			latex = servletContext.getInitParameter("latex-executable");
			dvipng = servletContext.getInitParameter("dvipng-executable");
		}
		if(latex == null)
		{
			latex = "latex";
		}
		if(dvipng == null)
		{
			dvipng = "dvipng";
		}
		if(temp == null)
		{
			temp = "/tmp";
		}

		// Create folder with random name
		File tempFolder;
		int attempts = 0;
		do
		{
			tempFolder = new File(temp, UUID.randomUUID().toString());
			attempts++;
			if(attempts > MAX_TEMP_FOLDER_ATTEMPTS)
			{
				throw new IOException("Error creating temp folder (" +
					MAX_TEMP_FOLDER_ATTEMPTS + " attempts failed): " + tempFolder);
			}
		}
		while(!tempFolder.mkdir());

		// Ensure we delete the folder when finished
		try
		{
			// Create TeX file in folder
			String fullTex = TEX_PROLOG + TEX_PRE_ITEM + tex +
				TEX_POST_ITEM + TEX_EPILOG;
			byte[] fullTexBytes = fullTex.getBytes("US-ASCII");
			File texFile = new File(tempFolder, "eq.tex");
			FileOutputStream out = new FileOutputStream(texFile);
			out.write(fullTexBytes);
			out.close();

			if (SHOW_COMMANDS)
			{
				System.err.println("[WEBMATHS] In folder: " + tempFolder);
				System.err.println("[WEBMATHS] TeX file follows {{\n" + fullTex + "}}");
			}

			// Convert it to .dvi
			runProcess(new String[] {latex, "--interaction=batchmode", "eq.tex"}, tempFolder);

			// Get colour, size parameters
			int dpi = Math.round((float)(BASE_PIXEL_SIZE * 72.27 / 10.0) * size);
			Color fg = convertRgb(rgb);
			float[] components = fg.getRGBColorComponents(null);
			String texFg = "rgb " + components[0] + " " + components[1] + " " + components[2];

			// Convert DVI to PNG
			String[] stdout = runProcess(
				new String[] {dvipng, "-q", "-D", "" + dpi, "-fg", texFg, "-bg", "Transparent",
					"-l", "1", "--depth", "-o", "eq.png", "eq.dvi" }, tempFolder);

			// Get baseline from stdout value
			if(stdout.length < 1)
			{
				throw new IOException("dvipng: no return");
			}
			String lastLine = stdout[stdout.length - 1];
			Matcher m = DVIPNG_DEPTH.matcher(lastLine);
			if(!m.matches())
			{
				throw new IOException("dvipng: unexpected return: " + lastLine);
			}
			result.setBaseline(new BigInteger(m.group(1)));

			// Load PNG image
			File pngFile = new File(tempFolder, "eq.png");
			FileInputStream in = new FileInputStream(pngFile);
			byte[] image = new byte[(int)pngFile.length()];
			in.read(image);
			in.close();

			result.setImage(image);
			result.setOk(true);
		}
		finally
		{
			killFolder(tempFolder);
		}
	}

	// TODO This prolog may not be complete - may need to include AMS stuff
	private final static String TEX_PROLOG =
		"\\documentclass[10pt]{article}\n" +
		// amsthm is needed only for \qedsymbol
		// gensmyb is needed only for \degree
		"\\usepackage{amsmath,amssymb,amsthm,gensymb}\n" +
		"\\begin{document}\n";
	private final static String TEX_PRE_ITEM =
		"\\begin{equation*}\n\\setbox0\n\\hbox{$\n";
	private final static String TEX_POST_ITEM =
		"\n$}\n\\ht 0 0pt\n\\shipout\\box 0\n\\end{equation*}\n";
	private final static String TEX_EPILOG =
		"\\end{document}\n";

	private final static Pattern DVIPNG_DEPTH = Pattern.compile("^ depth=(-?[0-9]+)$");

	/**
	 * Deletes a folder within the temp folder, ignoring errors.
	 * @param folder Folder to delete
	 */
	private static void killFolder(File folder)
	{
		// Delete all files (not recursive as we do not create subfolders)
		File[] files = folder.listFiles();
		for(File file : files)
		{
			file.delete();
		}
		// Then delete folder itself
		folder.delete();
	}

	private static String[] runProcess(String[] command, File cwd)
		throws IOException, InterruptedException
	{
		StringBuilder commandString = new StringBuilder();
		for(String param : command)
		{
			commandString.append(param);
			commandString.append(' ');
		}
		if(SHOW_COMMANDS)
		{
			System.err.println("[WEBMATHS] Exec: " + commandString.toString().trim());
		}
		Process process = Runtime.getRuntime().exec(command, null, cwd);
		new EaterThread(process.getErrorStream());
		EaterThread stdout = new EaterThread(process.getInputStream());
		process.waitFor();
		return stdout.getLines();
	}

	private static class EaterThread extends Thread
	{
		private BufferedReader buffer;
		private LinkedList<String> lines = new LinkedList<String>();
		private boolean running;

		public EaterThread(InputStream stream) throws IOException
		{
			this.buffer = new BufferedReader(new InputStreamReader(stream, "US-ASCII"));
			running = true;
			start();
		}

		public synchronized String[] getLines() throws InterruptedException,
			IllegalStateException
		{
			synchronized(this)
			{
				if(running)
				{
					wait(10000);
				}
				if(running)
				{
					throw new IllegalStateException(
						"Attempt to getLines when process not finished");
				}
			}
			return lines.toArray(new String[lines.size()]);
		}

		@Override
		public void run()
		{
			try
			{
				while(true)
				{
					String line = buffer.readLine();
					if(line == null)
					{
						if(SHOW_COMMANDS)
						{
							System.err.println("[WEBMATHS] (EOF)");
						}
						return;
					}
					if(SHOW_COMMANDS)
					{
						System.err.println("[WEBMATHS] " + line);
					}
					lines.add(line);
				}
			}
			catch(IOException e)
			{
				// If there's any kind of error, exit the thread
			}
			finally
			{
				if(buffer != null)
				{
					try
					{
						buffer.close();
					}
					catch(IOException e)
					{
					}
					buffer = null;
				}
				synchronized(this)
				{
					running = false;
					notify();
				}
			}
		}
	}
}
