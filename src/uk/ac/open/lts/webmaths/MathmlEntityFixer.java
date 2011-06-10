package uk.ac.open.lts.webmaths;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import uk.ac.open.lts.webmaths.english.WebMathsEnglish;

/**
 * Fixes all the MathML entities into actual characters or numeric entities
 * so that there is no need to include the MathML DTD when parsing MathML.
 */
public class MathmlEntityFixer
{
	private Map<String, String> entityToChar = new HashMap<String, String>();
	private Map<String, String> hexesToDesc = new HashMap<String, String>();
	
	private int longestDescSequence;
	
	private final static Pattern 
		REGEX_ENTITY = Pattern.compile("&([^#][^;]*);"),	
		REGEX_NUMERIC_ENTITY = Pattern.compile("&#([^;]*);"),
		REGEX_HEX_ENTITIES = Pattern.compile("(?:&#x[^;]*;)+");
	
	/**
	 * Constructs and initialises the fixer, loading in all the data it needs.
	 * @throws IOException Any error loading data
	 */
	public MathmlEntityFixer() throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			MathmlEntityFixer.class.getResourceAsStream("mathml.entities.txt"), "UTF-8"));
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
			entityToChar.put(line.substring(0, equals), line.substring(equals+1));
		}
		reader.close();

		readDescriptions(WebMathsEnglish.class.getResourceAsStream("mathml.descriptions.txt"));
		readDescriptions(WebMathsEnglish.class.getResourceAsStream("override.descriptions.txt"));
	}

	/**
	 * Reads from a descriptions file.
	 * @param input Input stream
	 * @throws IOException Any error
	 */
	private void readDescriptions(InputStream input) throws IOException
	{
		BufferedReader reader = new BufferedReader(
			new InputStreamReader(input, "UTF-8"));
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
			if(line.startsWith("#"))
			{
				continue;
			}
			
			int equals = line.indexOf('=');
			if(equals == -1)
			{
				throw new IOException("Invalid line format (no equals): " + line);
			}

			// Store in map
			String hexes = line.substring(0, equals);
			String desc = line.substring(equals+1);
			hexesToDesc.put(hexes, desc);

			// Work out which has the longest sequence of codepoints (= the number
			// of commas plus one)
			longestDescSequence = Math.max(longestDescSequence,
				hexes.replaceAll("[^,]", "").length()+1);
			
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
			String replacement = entityToChar.get(m.group(1));
			if(replacement == null)
			{
				throw new IllegalArgumentException("Unknown entity " + m.group(0));
			}
			m.appendReplacement(out, replacement);
		}
		m.appendTail(out);
		return out.toString();
	}

	/**
	 * Given a text string, converts all non-ASCII characters to hex entitites
	 * &#x; - also converts any decimal entities to hex ones and converts any
	 * existing hex entities to lower-case.
	 * @param text Input text
	 * @return Text normalised as described
	 */
	public static String getWithHexEntities(String text)
	{
		// Normalise all existing entities
		Matcher m = REGEX_NUMERIC_ENTITY.matcher(text);
		StringBuffer out = new StringBuffer();
		while(m.find())
		{
			String replacement;
			String value = m.group(1);
			if(value.startsWith("x") || value.startsWith("X"))
			{
				replacement = "&#" + value.replaceFirst("^x0+", "x") . toLowerCase() + ";";
			}
			else
			{
				int number = Integer.parseInt(value);
				replacement = "&#x" + Integer.toHexString(number) + ";";
			}
			
			m.appendReplacement(out, replacement);
		}
		m.appendTail(out);
		text = out.toString();
		
		// Now replace special characters with entities
		StringBuilder out2 = new StringBuilder();
		for(int i=0; i<text.length(); i++)
		{
			char c = text.charAt(i);
			if (c <= 127)
			{
				out2.append(c);
			}
			else
			{
				// Non-ASCII. But is it a surrogate pair?
				int codePoint;
				if(Character.isHighSurrogate(c))
				{
					codePoint = Character.codePointAt(text, i);
					i++;
				}
				else
				{
					codePoint = (int)c;
				}
				// Write as hex escape
				out2.append("&#x");
				out2.append(Integer.toHexString(codePoint));
				out2.append(';');
			}
		}
		return out2.toString();
	}

	/**
	 * Converts special Unicode characters in the input text to their descriptions
	 * for use in speech output. Also removes excess whitespace.
	 * @param text Input string
	 * @return Result with special characters changed into ASCII text descriptions
	 */
	public String toSpeech(String text)
	{
		// Step 1: Change all non-ASCII characters to numeric hex entities.
		text = getWithHexEntities(text);

		// Step 2: Replace all entities with descriptions where there is one,
		//   preferring longer sequences.
		Matcher m = REGEX_HEX_ENTITIES.matcher(text);
		StringBuffer out = new StringBuffer();
		while(m.find())
		{
			StringBuilder replacement = new StringBuilder();

			// Sequence of one or more hex entities
			String entities = m.group(0);
			
			// Turn into an array; each entry will be normalised (no leading zeros,
			// lower-case)
			String[] hexes = entities.substring(3, entities.length()-1).split(";&#x");
			
			// Loop round handling all
			entityLoop: for(int entity=0; entity<hexes.length; )
			{
				// Try max length first
				for(int length=Math.min(longestDescSequence, hexes.length-entity);
					length>0; length--)
				{
					// Build up string of this
					StringBuilder combined = new StringBuilder();
					for(int index=0; index<length; index++)
					{
						if(index!=0)
						{
							combined.append(',');
						}
						combined.append(hexes[entity + index]);
					}
					
					// Look in map
					String desc = hexesToDesc.get(combined.toString());
					if(desc != null)
					{
						replacement.append(' ');
						replacement.append(desc);
						replacement.append(' ');
						entity += length;
						continue entityLoop;
					}
				}
				
				// No match found so leave it as an entity and go onto next
				replacement.append("&#x");
				replacement.append(hexes[entity]);
				replacement.append(";");
				entity++;
			}
			
			m.appendReplacement(out, replacement.toString());
		}
		m.appendTail(out);
		text = out.toString();
		
		// Step 3: Tidy up whitespace and return
		return text.replaceAll("\\s+", " ").trim();
	}
}