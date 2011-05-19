package uk.ac.open.lts.webmaths;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Fixes all the MathML entities into actual characters or numeric entities
 * so that there is no need to include the MathML DTD when parsing MathML.
 */
public class MathmlEntityFixer
{
	private Map<String, String> map = new HashMap<String, String>();
	
	private final static Pattern REGEX_ENTITY = Pattern.compile("&([^#][^;]*);");
	
	/**
	 * Constructs and initialises the fixer, loading in all the data it needs.
	 * @throws IOException Any error loading data
	 */
	public MathmlEntityFixer() throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			WebMathsImage.class.getResourceAsStream("mathml.entities.txt"), "UTF-8"));
		while(true)
		{
			String line = reader.readLine();
			if(line == null)
			{
				break;
			}
			if(line.trim().equals(""))
			{
				continue;
			}
			
			int equals = line.indexOf('=');
			map.put(line.substring(0, equals), line.substring(equals+1));
		}
		reader.close();
	}
	
	/**
	 * Fixes all the named entities in the source string.
	 * @param original Original string
	 * @return Fixed string with entities replaced by direct characters
	 * @throws IllegalArgumentException Any unknown entities
	 */
	public String fix(String original) throws IllegalArgumentException
	{
		Matcher m = REGEX_ENTITY.matcher(original);
		StringBuffer out = new StringBuffer();
		while(m.find())
		{
			String replacement = map.get(m.group(1));
			if(replacement == null)
			{
				throw new IllegalArgumentException("Unknown entity " + m.group(0));
			}
			m.appendReplacement(out, replacement);
		}
		m.appendTail(out);
		return out.toString();
	}
}