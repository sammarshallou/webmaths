/*
 * Unlike most of this project, this files is based on code from another system
 * and is therefore NOT licensed under the GPL. I believe this license is
 * compatible with the GPL (it is also 'weaker', so you may be able to use
 * this specific code more widely).  
 * 
 * Original license:
 * 
 * Copyright (C) 2006 Steve Cheng <stevecheng@users.sourceforge.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy 
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHOR(S) BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALNGS IN THE SOFTWARE.
 *
 * Modified Java port:
 * 
 * Copyright 2011 The Open University.
 */
package uk.ac.open.lts.webmaths.tex;

import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;
import org.w3c.dom.ls.*;

/**
 * Handles tokenising of TeX input and related tasks.
 * @author Steve Cheng
 * @author Robert Hassom
 * @authot sam marshall
 */
public class TokenInput
{
	private boolean debug;
	
//  #the unicode character 2019 is character the plastex replaces the ' character with
//  #the unicode character 201d is character the plastex replaces the '' character with
	
//  tokenize_strict_re = re.compile(ur"""(\\begin|\\operatorname|\\mathrm|\\mathop|\\end)\s*\{\s*([A-Z a-z]+)\s*\}|(\\[a-zA-Z]+|\\[\\#\%\{\},:;!])|(\s+)|([0-9\.])|([\$!"#%&'\u2019\u201d()*+,-.\/:;<=>?\[\]^_`\{\|\}~])|([a-zA-Z@])""")
	private final static Pattern STRICT_RE = Pattern.compile(
		"(\\\\begin|\\\\operatorname|\\\\mathrm|\\\\mathop|\\\\end)\\s*" +
		"\\{\\s*([A-Z a-z]+)\\s*\\}|(\\\\[a-zA-Z]+|\\\\[\\\\#\\%\\{\\},:;!])|(\\s+)|" +
		"([0-9\\.])|([\\$!\"#%&'\u2019\u201d()*+,-.\\/:;<=>?\\[\\]" +
		"^_`\\{\\|\\}~])|([a-zA-Z@])");

//  tokenize_text_re = re.compile(ur"""[\${}\\]|\\[a-zA-Z]+|[^{}\$]+""")
	private final static Pattern TEXT_RE = Pattern.compile(
		"[\\${}\\\\]|\\\\[a-zA-Z]+|[^{}\\$]+");

//  tokenize_text_commands = [u'\\textrm',u'\\textsl',u'\\textit',u'\\texttt',u'\\textbf',u'\\text',u'\\textnormal',u'\\hbox',u'\\mbox']
	private final static Set<String> TEXT_COMMANDS = new HashSet<String>(
		Arrays.asList(new String[] {
		"\\\\textrm", "\\\\textsl", "\\\\textit", "\\\\texttt", "\\\\textbf",
		"\\\\text", "\\\\textnormal", "\\\\hbox", "\\\\mbox"
	}));

	private String source;
	private LinkedList<String> tokens;
	private ListIterator<String> tokensIterator;

	/**
	 * Constructs with given TeX string.
	 * @param tex TeX input string
	 */
  public TokenInput(String tex)
	{
//  def __init__(self, tex):
//  self.source=tex
//  self.tokens = []
//  self.tokens_index = 0
//  self.tokenize_latex_math(tex)
//  self.tokens.append(None)
		this.source = tex;
		this.tokens = new LinkedList<String>();
		tokenizeLatexMath(tex);
		tokensIterator = tokens.listIterator();
	}
  
  private void tokenizeLatexMath(String tex)
  {
//    in_text_mode = 0
//    brace_level = []
//    pos = 0
//    nargs=0
  	int inTextMode = 0;
  	LinkedList<Integer> braceLevel = new LinkedList<Integer>();
  	int pos = 0;
  	
//    tex = unicode(tex)
//    if len(tex)>2 and tex[0] == u'$' and tex[-1] == u'$':
//      tex = tex[1:-1]
  	// Remove surrounding $ signs if any
  	if(tex.startsWith("$") && tex.endsWith("$"))
  	{
  		tex = tex.substring(0, tex.length()-1);
  	}
   
//    while pos<len(tex):
  	while(pos < tex.length())
  	{
//      if not in_text_mode:
  		if(inTextMode == 0)
  		{
  			Matcher m;
  			boolean matched;
  			
//        m = self.tokenize_strict_re.match(tex, pos)
 				m = STRICT_RE.matcher(tex);
 				matched = m.find(pos);
  			
//        #if no match then pass through as a single char token
//        if m is None:
//          if tex[pos]==u'\ud835':#check for two byte unicode
//            self.tokens.append(tex[pos:pos+2])
//            pos=pos+2
//          else:
//            self.tokens.append(tex[pos])
//            pos=pos+1
  			// If no match then pass through as a single char token
  			if(!matched)
  			{
  				// Check for two-surrogate unicode - note I change this logic to do it
  				// properly rather than only supporting one range or whatever,
  				// hopefully that is correct.
  				if(Character.isHighSurrogate(tex.charAt(pos)))  					
  				{
  					tokens.add(tex.substring(pos, pos + 2));
  					pos += 2;
  				}
  				else
  				{
  					tokens.add(tex.substring(pos, pos + 1));
  					pos += 1;
  				}
  			}
//        else:
  			else
  			{
//        if m.end()==pos:
//        print "matched nothing!"
//        return         
  				if(m.end() == pos)
  				{
  					// Matched nothing (I don't know why this had a print but figure
  					// it was only for debugging)
  					return;
  				}
//        pos = m.end()
  				pos = m.end();
//      if m.group(1) is not None:# e.g. \begin{fred}
//        #self.tokens.extend(m.group((1,2))) #should work but doesn't always
//        self.tokens.extend([m.group(1),m.group(2)])
  				if(m.group(1) != null)
  				{
  					// e.g. \begin{fred}
  					tokens.add(m.group(1));
  					tokens.add(m.group(2));
  				}
//      elif m.group(3) == u"\\sp":
//        self.tokens.append(u"^")
  				else if("\\sp".equals(m.group(3)))
  				{
  					tokens.add("^");
  				}
//      elif m.group(3) == u"\\sb":
//        self.tokens.append(u"_")
  				else if("\\sb".equals(m.group(3)))
  				{
  					tokens.add("_");
  				}
//      elif m.group(0) == u"$":
//        in_text_mode = 1
  				else if("$".equals(m.group(0)))
  				{
  					inTextMode = 1;
  				}
//      elif m.group(4) is not None:
//        continue
  				else if(m.group(4) != null)
  				{
  					continue;
  				}
//      elif m.group(5) is not None:#numbers
//        #sanitise numbers by removing \, added for readability 
//        s=m.group(5)
//        #check for trailing \, and do not clobber this
//        se=s[-2:]==ur'\,'
//        s=re.sub(r'\\,','',s)
//        s=re.sub(r'\s','',s)
//        self.tokens.append(s)
//        if se:
//          self.tokens.append(ur'\,')
  				else if(m.group(5) != null)
  				{
  					// sanitise numbers by removing \, added for readability
  					String s = m.group(5);
  					// check for trailing \, and do not clobber this
  					boolean se = s.endsWith("\\,");
  					s = s.replace("\\,", "");
  					s = s.replaceAll("\\s", "");
  					tokens.add(s);
  					if(se)
  					{
  						tokens.add("\\,");
  					}
  				}
//      elif m.group(3) in self.tokenize_text_commands:
//        in_text_mode = 2;
//        brace_level.append(0)
  				else if(TEXT_COMMANDS.contains(m.group(3)))
  				{
  					inTextMode = 2;
  					braceLevel.add(0);
  				}
//      else:
//        self.tokens.append(m.group(0))
  				else
  				{
  					tokens.add(m.group(0));
  				}
  			}
  		}
//    else:# parse text mode
  		else
  		{
  			// parse text mode
//      m = self.tokenize_text_re.match(tex, pos)
  			Matcher m = TEXT_RE.matcher(tex);
  			boolean matched = m.find(pos);
  			
//      if m is None:#should never happen, but just in case.
//        if tex[pos]==u'\ud835':#check for two byte unicode
//          self.tokens.append(tex[pos:pos+2])
//          pos=pos+2
//        else:
//          self.tokens.append(tex[pos])
//          pos=pos+1
  			if (!matched)
  			{
  				// should never happen, but just in case.
  				// Check for two-surrogate unicode - note I change this logic to do it
  				// properly rather than only supporting one range or whatever,
  				// hopefully that is correct.
  				if(Character.isHighSurrogate(tex.charAt(pos)))  					
  				{
  					tokens.add(tex.substring(pos, pos + 2));
  					pos += 2;
  				}
  				else
  				{
  					tokens.add(tex.substring(pos, pos + 1));
  					pos += 1;
  				}  				
  			}
//      else:
  			else
  			{
//        if m.end()==pos:
//        print "matched nothing!"
//        return
  				if(m.end() == pos)
  				{
  					// Matched nothing (I don't know why this had a print but figure
  					// it was only for debugging)
  					return;
  				}

//      pos = m.end()
//      txt=m.group(0)
  				pos = m.end();
  				String txt = m.group(0);
  				
//      if txt == u"$":
//        in_text_mode = 0
//      elif txt == u"{":
//        brace_level[-1] += 1
//      elif txt == u"}":
//        brace_level[-1] -= 1
//        if brace_level[-1] <= 0:
//          in_text_mode = 0
//          brace_level.pop()
  				if(txt.equals("$"))
  				{
  					inTextMode = 0;
  				}
  				else if(txt.equals("{"))
  				{
  					braceLevel.addLast(braceLevel.removeLast() + 1);
  				}
  				else if(txt.equals("}"))
  				{
  					braceLevel.addLast(braceLevel.removeLast() - 1);
  					if(braceLevel.getLast() <= 0)
  					{
  						inTextMode = 0;
  						braceLevel.removeLast();
  					}
  				}
  				
//      #print 'text source (%s)'%txt
//      #replace significant spaces with something that won't
//      #be swallowed by the SC XML eliminating whitespace
//      txt=re.sub(' ',u'\u00A0',txt)
  				// replace significant spaces with something that won't
  				// be swallowed by the SC XML eliminating whitespace
  				txt = txt.replace(' ', '\u00a0');
//      #map tildes to unbreakable spaces
//      txt=re.sub('~',u'\u00A0',txt)
  				// map tildes to unbreakable spaces
  				txt = txt.replace('~', '\u00a0');
//      self.tokens.append(txt)
  				tokens.add(txt);
  			}
  		}
  	}
  }
  
  /**
   * Converts tokenised data to MathML. 
   * @return String containing MathML text
   */
  public String toMathml()
  {
//    def tomathML(self):
//      if self.tokens is None:
//        return '<merror><mtext>Could not parse %s</mtext></merror>'%repr(self.source)
//      try:
//        return LaTeX2MathMLModule.v_subexpr_chain_to_mathml(self, {}).toxml("utf-8")
//      except:
//        return '<merror><mtext>Could not translate %s to mathML</mtext></merror>'%repr(self.source)
  	// I didn't bother reproducing the first part because there is no way that
  	// tokens can be set to none/null (that I can see).
  	try
  	{
  		LatexToMathml converter = new LatexToMathml();
  		Element root = converter.convert(this);
  		postProcess(root);
  		return saveXml(root);
  	}
  	catch(Throwable t)
  	{
  		t.printStackTrace(); // TODO
  		return "<merror><mtext>Could not translate " + source
  			+ " to MathML</mtext></merror>"; 
  	}
  }
  
  /**
   * Applies post-processing to the MathML result.
   * <p>
   * Currently this gloops together &lt;mn&gt; tags for digits into single
   * ones.
   * @param e Root element
   */
  static void postProcess(Element e)
  {
  	// Apply this processing to all <mrow> plus to root <math> element.
  	NodeList list = e.getElementsByTagNameNS(LatexToMathml.NS, "mrow");
  	Element[] targets = new Element[list.getLength() + 1];
  	for(int i=0; i<list.getLength(); i++)
  	{
  		targets[i] = (Element)list.item(i);
  	}
  	targets[targets.length - 1] = e;
  	
  	// Loop through target elements...
  	for(Element target : targets)
  	{
  		// Go through the children
  		for(Node child = target.getFirstChild(); child!=null; 
  			child = child.getNextSibling())
  		{
  			// Skip nodes that aren't digits
  			if(!isMnDigit(child))
  			{
  				continue;
  			}
  			
  			// We found an mn - see if it has any following nodes to gloop
  			String extra = "";
  			boolean alreadyGotDot = false;
  			Node stop = null;
  			for(Node following = child.getNextSibling(); following != null;
  				following = following.getNextSibling())
  			{
  				if(isMnDigit(following))
  				{
  					extra += following.getFirstChild().getNodeValue();
  				}
  				else if(isMtextDot(following) && !alreadyGotDot)
  				{
  					extra += following.getFirstChild().getNodeValue();
  					alreadyGotDot = true;
  				}
  				else
  				{
  					stop = following;
  					break;
  				}
  			}
  			
  			if(extra.length() > 0)
  			{
	  			// Add the extra text to this node
	  			String newText = child.getFirstChild().getNodeValue() + extra;
	  			child.removeChild(child.getFirstChild());
	  			child.appendChild(child.getOwnerDocument().createTextNode(newText));
	  			
	  			// Remove the following nodes
  				Node following = child.getNextSibling();
	  			while(following != stop)
	  			{
	  				Node next = following.getNextSibling();
	  				target.removeChild(following);
	  				following = next;
	  			}
	  			
	  			// If the target was an <mrow> and it now only has one thing in,
	  			// can we replace it with this <mn>
	  			if(target.getLocalName().equals("mrow") &&
	  				target.getFirstChild() == child && target.getLastChild() == child)
	  			{
	  				target.removeChild(child);
	  				target.getParentNode().insertBefore(child, target);
	  				target.getParentNode().removeChild(target);
	  			}
  			}
  		}
  	}
  }
  
  /**
   * Returns true if an XML node is an <mn> element with a single digit in it.
   * @param n Node
   * @return True if it is
   */
  private static boolean isMnDigit(Node n)
  {
		if(!(n instanceof Element))
		{
			return false;
		}
		if(!n.getLocalName().equals("mn"))
		{
			return false;
		}
		if(n.getChildNodes().getLength() != 1)
		{
			return false;
		}
		if(!(n.getFirstChild() instanceof Text))
		{
			return false;
		}
		Text text = (Text)n.getFirstChild();
		if(!text.getNodeValue().matches("[0-9]"))
		{
			return false;
		}
		return true;
  }
  
  /**
   * Checks is a node is an &lt;mtext&gt; element containing a single dot / 
   * decimal point.
   * @param n
   * @return True if it is
   */
  private static boolean isMtextDot(Node n)
  {
		if(!(n instanceof Element))
		{
			return false;
		}
		if(!n.getLocalName().equals("mtext"))
		{
			return false;
		}
		if(n.getChildNodes().getLength() != 1)
		{
			return false;
		}
		if(!(n.getFirstChild() instanceof Text))
		{
			return false;
		}
		Text text = (Text)n.getFirstChild();
		String val = text.getNodeValue();
		if(val.length() != 1)
		{
			return false;
		}
		// Allow full stop or middle dot
		if(!".\u00b7".contains(val))
		{
			return false;
		}
		return true;
  }

  /**
   * Utility method: converts DOM element to string.
   * @param e Element to convert
   * @return String
   */
  static String saveXml(Element e)
  {
		Document document = e.getOwnerDocument();
		DOMImplementationLS domImplLS = (DOMImplementationLS)document.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		return serializer.writeToString(e).replaceFirst("^<\\?xml.*\n", "");
  }  
  
  /**
   * Returns the next token and increments the position.
   * <p>
   * This function was not in the Python version but I added it as a nicer way
   * to get the next token.
   * @return Token string or null if end of list
   */
  public String nextToken()
  {
  	if(!tokensIterator.hasNext())
  	{
  		return null;
  	}
  	String result = tokensIterator.next();
  	if(debug)
  	{
  		System.err.println("TOKEN [" + result + "]");
  	}
  	return result;
  }
  
  /**
   * Peeks at the next token without incrementing position.
   * <p>
   * This function was not in the Python version but I added it.
   * @return Token string or null if end of list
   */
  public String peekToken()
  {
  	if(!tokensIterator.hasNext())
  	{
  		return null;
  	}
  	String result = tokensIterator.next();
  	tokensIterator.previous();
  	return result;
  }
  
  /**
   * Peeks at the next token without incrementing position.
   * <p>
   * This function was not in the Python version but I added it.
   * @param offset Number of tokens to peek ahead (0 = next)
   * @return Token string or null if end of list
   */
  public String peekToken(int offset)
  {
  	if(!tokensIterator.hasNext())
  	{
  		return null;
  	}
  	String result = tokensIterator.next();
  	for(int i=1; i<=offset; i++)
  	{
  		if(!tokensIterator.hasNext())
  		{
  			// If we find a null at any point, we reached end of list, so stop and
  			// rewind the same amount...
  			for(int j=0; j<i; j++)
  			{
  				tokensIterator.previous();
  			}
  			// ...then return the null
  			return null;
  		}
  		result = tokensIterator.next();
  	}
  	for(int i=0; i<=offset; i++)
  	{
  		tokensIterator.previous();
  	}
  	return result;
  }
  
  /**
   * Moves to the previous token and overwrites it. (EWWWW.)
   * <p>
   * This function was not in the Python version but I added it.
   */
  public void backAndOverwriteToken(String value)
  {
  	tokensIterator.previous();
  	tokensIterator.set(value);
  	if(debug)
  	{
  		System.err.println("TOKEN [" + value + "] back");
  	}
  }
  
  /**
   * Sets debugging flag, which causes tokens to be displayed to standard
   * error as they are consumed.
   * <p>
   * This function was not in the Python version.
   * @param debug Debugging flag (true = display)
   */
  public void setDebug(boolean debug)
  {
  	this.debug = debug;
  }
}
