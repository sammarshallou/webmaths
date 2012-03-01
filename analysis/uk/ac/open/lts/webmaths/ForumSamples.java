package uk.ac.open.lts.webmaths;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 * Class to do analysis on the forum samples corpus (this is a list of TeX
 * equations extracted from forum messages on our systems).
 */
public class ForumSamples
{
	private List<String> samples = new LinkedList<String>();

	public ForumSamples() throws Exception
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			getClass().getResourceAsStream("forum.tex.samples.txt"), "UTF-8"));
		while(true)
		{
			String line = reader.readLine();
			if(line == null)
			{
				break;
			}
			samples.add(line);
		}
		System.err.println("Loaded " + samples.size() + " samples.");
		System.err.println();
	}

	public void showUnsupportedCommands() throws Exception
	{
		System.err.println("Find unsupported commands:");

		// TeX commands matcher - backslash followed by one or more letters, or
		// a single other character.
		Pattern TEX_COMMANDS = Pattern.compile("\\\\([A-Za-z]+|[^A-Za-z0-9])");

		// Make list of supported commands
		//////////////////////////////////

		Set<String> supportedCommands = new HashSet<String>();

		// Get a file in this folder
		URL classUrl = getClass().getResource("forum.tex.samples.txt");
		File localFile = new File(classUrl.toURI());

		// Use it to navigate to the xml
		File xmlFile = new File(localFile.getParentFile().getParentFile().getParentFile().
			getParentFile().getParentFile().getParentFile().getParentFile(),
			"misc/supported.tex.commands.xml");

		// Load xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new FileInputStream(xmlFile)));

		// Get all the TeX
		NodeList allTex = doc.getElementsByTagName("TeX");
		for(int i=0; i<allTex.getLength(); i++)
		{
			Element el = (Element)allTex.item(i);
			String tex = ((Text)el.getFirstChild()).getNodeValue();

			// Find all commands from supported TeX
			Matcher m = TEX_COMMANDS.matcher(tex);
			while(m.find())
			{
				supportedCommands.add(m.group());
			}
		}

		// Display info
		System.err.println("  Total supported commands = " + supportedCommands.size());

		// Compare samples against supported list
		Map<String, Integer> unsupportedCommands = new HashMap<String, Integer>();
		int samplesUnsupported = 0;
		for(String sample : samples)
		{
			boolean supported = true;

			// Find all commands from supported TeX
			Matcher m = TEX_COMMANDS.matcher(sample);
			while(m.find())
			{
				String command = m.group();
				if(!supportedCommands.contains(command))
				{
					supported = false;
					Integer i = unsupportedCommands.get(command);
					if(i == null)
					{
						unsupportedCommands.put(command, 1);
					}
					else
					{
						unsupportedCommands.put(command, i+1);
					}
				}
			}

			if(!supported)
			{
				samplesUnsupported++;
			}
		}

		System.err.println("  Samples using unsupported commands = " +
			showWithPercentage(samplesUnsupported, samples.size()));
		System.err.println("  Total unsupported commands = " +
			unsupportedCommands.size());

		// Index by count
		TreeMap<Integer, Set<String>> unsupportedByCount =
			new TreeMap<Integer, Set<String>>(Collections.reverseOrder());
		for(Map.Entry<String, Integer> entry : unsupportedCommands.entrySet())
		{
			Set<String> commands = unsupportedByCount.get(entry.getValue());
			if(commands == null)
			{
				commands = new TreeSet<String>();
				unsupportedByCount.put(entry.getValue(), commands);
			}
			commands.add(entry.getKey());
		}

		System.err.println("  Unsupported commands by count:");
		for(Map.Entry<Integer, Set<String>> entry : unsupportedByCount.entrySet())
		{
			String count = String.format("%-4d", entry.getKey());
			for(String command : entry.getValue())
			{
				System.err.println("  " + count + " " + command);
			}
		}
	}

	private static String showWithPercentage(int number, int total)
	{
		String value = number + " / " + total;
		if(total == 0)
		{
			return value;
		}
		double percentage = 100.0 * (double)number / (double)total;
		return value + " (" + String.format("%.1f", percentage) + "%)";
	}

	public static void main(String[] args) throws Exception
	{
		ForumSamples samples = new ForumSamples();
		samples.showUnsupportedCommands();
	}
}
