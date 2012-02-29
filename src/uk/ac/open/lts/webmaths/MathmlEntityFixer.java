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
		REGEX_HEX_ENTITIES = Pattern.compile("(?:&#x[^;]*;)+"),
		REGEX_CDATA = Pattern.compile("<!\\[CDATA\\[.*?\\]\\]>", Pattern.DOTALL),
		REGEX_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	
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

		// Overrides for XML built-in entities
		entityToChar.put("amp", "&amp;");
		entityToChar.put("lt", "&lt;");
		entityToChar.put("gt", "&gt;");
		entityToChar.put("apos", "&apos;");
		entityToChar.put("quot", "&quot;");

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
			String desc = line.substring(equals+1).trim();
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
		StringBuffer out = new StringBuffer();

		// Do the same thing for cdata (first) then comment
		for(Pattern ignoring : new Pattern[] { REGEX_CDATA, REGEX_COMMENT })
		{
			// Find CDATA/comment, split that out and recurse
			Matcher m = ignoring.matcher(original);
			boolean gotIgnoring = false;
			while(m.find())
			{
				String cdata = m.group();
				StringBuffer temp = new StringBuffer();
				m.appendReplacement(temp, "");
				// Fix the bit before the match
				out.append(fix(temp.toString()));
				// Add the cdata unchanged
				out.append(cdata);
				gotIgnoring = true;
			}
			// If there were any cdata/comment, recurse for last part and return result
			if(gotIgnoring)
			{
				StringBuffer temp = new StringBuffer();
				m.appendTail(temp);
				out.append(fix(temp.toString()));
				return out.toString();
			}
		}

		// No cdata or comments, just do replace
		Matcher m = REGEX_ENTITY.matcher(original);
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
						replacement.append(desc);
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