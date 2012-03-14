package uk.ac.open.lts.webmaths.tex;

import java.io.*;
import java.util.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;

import uk.ac.open.lts.webmaths.*;
import static uk.ac.open.lts.webmaths.MapUtil.*;

/**
 * Converter from MathML code to LaTeX, so that it can be rendered using
 * latex and dvipng utilities.
 * <p>
 * This class is supposed to support the LaTeX code generated using the
 * {@link uk.ac.open.lts.webmaths.tex.LatexToMathml} converter, producing
 * round-trip results where possible or similar results otherwise. It may also
 * work with some other simple MathML equations.
 */
public class MathmlToLatex
{
	private TransformerPool convertXsl, normaliseXsl;

	private static boolean DEBUG_DUMP_XML = false;

	/**
	 * Constructs (requires entity fixer).
	 * @param fixer Entity fixer
	 */
	public MathmlToLatex(MathmlEntityFixer fixer)
	{
		convertXsl = new TransformerPool(fixer, MathmlToLatex.class, "latex.xsl");
		normaliseXsl = new TransformerPool(fixer, WebMathsService.class,
			"normalise.xsl", "latex");
	}

	/**
	 * Converts a MathML document to a LaTeX string.
	 * @param mathml Input document
	 * @param ignoreUnsupported Set to silently ignore unsupported elements,
	 *   attributes and characters instead of throwing an exception
	 * @return LaTeX string suitable for use in display mode equation
	 * @throws TransformerException Error with transformation
	 * @throws IOException Error loading XSL
	 * @throws UnsupportedMathmlException If equation cannot be converted
	 */
	public String convert(Document mathml, boolean ignoreUnsupported)
		throws TransformerException, IOException, UnsupportedMathmlException {
		// Normalise MathML
		DOMSource in = new DOMSource(mathml);
		DOMResult out = new DOMResult();
		Transformer t = normaliseXsl.reserve();
		try
		{
			t.transform(in, out);
		}
		finally
		{
			normaliseXsl.release(t);
		}

		// Escape special characters and convert Unicode
		Document intermediate = (Document)out.getNode();
		escapeTex(intermediate.getDocumentElement(), ignoreUnsupported);

		if(DEBUG_DUMP_XML)
		{
			dumpXml(intermediate);
		}

		// Now convert to LaTeX
		in = new DOMSource(intermediate);
		out = new DOMResult();
		t = convertXsl.reserve();
		try
		{
			t.transform(in, out);
		}
		finally
		{
			convertXsl.release(t);
		}

		// Get text from DOM
		StringBuilder text = new StringBuilder();
		Document after = (Document)out.getNode();
		for(Node child = after.getDocumentElement().getFirstChild();
			child != null; child = child.getNextSibling())
		{
			if(child instanceof Text)
			{
				text.append(child.getNodeValue());
			}
		}
		String result = text.toString();

		// If it contains any unsupported elements, either throw exception or
		// else strip out
		if (ignoreUnsupported)
		{
			result = result.replaceAll("\\\\UNSUPPORTED\\{[^}]*\\}", "");
		}
		else if(result.contains("\\UNSUPPORTED"))
		{
			String message = result.replaceAll(
				"^.*?\\\\UNSUPPORTED\\{([^}]*)\\}.*$", "$1");
			throw new UnsupportedMathmlException(message);
		}

		// Add space before each \ command
		result = result.replaceAll("([A-Za-z0-9])(\\\\)", "$1 $2");

		// In situation like \symbol ^2 or \symbol _2, remove the space
		result = result.replaceAll("(\\\\[A-Za-z]+) ([_^])", "$1$2");

		// Whitespace at edges
		result = result.trim();

		// Get rid of surrounding { } if any
		result = result.replaceFirst("^\\{\\s*(.*?)\\s*\\}$", "$1");

		return result.trim();
	}

	private void dumpXml(Document doc) throws TransformerException
	{
		DOMSource in = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult outw = new StreamResult(writer);
		Transformer t = TransformerFactory.newInstance().newTransformer();
		t.transform(in, outw);
		String result = writer.toString();
		System.err.println(result);
	}

	/**
	 * Escapes all TeX special characters and replaces Unicode characters with
	 * TeX.
	 * @param n Root node (recursive)
	 * @param ignoreUnsupported If true, replaces unsupported chars with '?'
	 * @throws UnsupportedMathmlException If any characters are not unknown
	 */
	private void escapeTex(Node n, boolean ignoreUnsupported)
		throws UnsupportedMathmlException
	{
		if (n instanceof Text)
		{
			String before = n.getNodeValue();
			Collection<Node> after = replaceChars(before, ignoreUnsupported, n.getOwnerDocument());
			if(after != null)
			{
				for(Node add : after)
				{
					n.getParentNode().insertBefore(add, n);
				}
				n.getParentNode().removeChild(n);
			}
		}
		else if(n instanceof Element)
		{
			// Don't do annotations
			if(((Element)n).getTagName().equals("annotation"))
			{
				return;
			}
			for(Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
			{
				escapeTex(child, ignoreUnsupported);
			}
		}
	}

	private Collection<Node> replaceChars(String in, boolean ignoreUnsupported, Document d)
		throws UnsupportedMathmlException
	{
		LinkedList<Node> nodes = new LinkedList<Node>();
		StringBuilder out = new StringBuilder();
		boolean changed = false;

		outerloop: for(int i=0; i<in.length(); i++)
		{
			for(int chars = MAX_REPLACE_CHARS; chars > 0; chars--)
			{
				if(i+chars > in.length())
				{
					continue;
				}
				String original = in.substring(i, i+chars);
				String replace = REPLACE_CHARS.get(original);
				if(replace != null)
				{
					addEscape(original, replace, nodes, out, d);
					changed = true;
					i += (chars - 1);
					continue outerloop;
				}
				else if(chars == 1)
				{
					char c = original.charAt(0);
					if(c > 0x7f && !ALLOWED_CHARACTERS.contains(original)) // Permit combining NOT
					{
						if(ignoreUnsupported)
						{
							// If unsupported, use ? character
							addEscape(original, "?", nodes, out, d);
							changed = true;
						}
						else
						{
							throw new UnsupportedMathmlException(
								"MathML contains unknown special character: " + original);
						}
					}
					else
					{
						out.append(original);
					}
				}
			}
		}

		if(changed)
		{
			addEscape(null, null, nodes, out, d);
			return nodes;
		}
		else
		{
			return null;
		}
	}

	private static void addEscape(String original, String escape,
		LinkedList<Node> nodes, StringBuilder out, Document d)
	{
		// Add text node for previous text
		if(out.length() > 0)
		{
			nodes.add(d.createTextNode(out.toString()));
			out.setLength(0);
		}

		// Add escape (unless null == finishing off)
		if(escape != null)
		{
			Element esc = d.createElementNS(
				"http://ns.open.ac.uk/lts/webmaths", "esc");
			esc.appendChild(d.createTextNode(original));
			esc.setAttribute("tex", escape);
			nodes.add(esc);
		}
	}

	/**
	 * Non-ASCII characters which are permitted through to the XSL stage (they
	 * will be decoded into ASCII during the XSL).
	 */
	private final static Set<String> ALLOWED_CHARACTERS = new HashSet<String>(
		Arrays.asList(new String[]
	{
		// Combining NOT
		"\u0338",
		// Accents (all calls to accentToMathml in LatexToMathml.java)
		"\u00b4",
		"\u00af",
		"\u02d8",
		"\u02c7",
		"\u2192",
		"\u02d9",
		"\u00a8",
		"\u20db",
		// Under or over characters (underToMathml, overToMathml)
		"\ufe38",
		"\ufe37",
		"\u0332",
		"\u00af",
		"\u02dc",

	}));

	private final static int MAX_REPLACE_CHARS = 3;
	private final static Map<String, String> REPLACE_CHARS = makeMap(new String[]
	{
		"{", "\\{ ",
		"}", "\\} ",
		"\\", "\\backslash ",
		"#", "\\# ",
		"%", "\\% ",
		"&", "\\& ",
		"\u2016", "\\Vert ",
		"\u2003", "\\quad ",
		"\u2003\u2003", "\\qquad ",
		"\u2002", "\\thickspace ",
		"\u2005", "\\medspace ",
		"\u2009", "\\thinspace ",
		"\u200b", "\\! ",
		"\u00A0", "~",
		"\u220e", "\\qedsymbol ",
		"\u00a3", "\\pounds ",
		"\u2308", "\\lceil ",
		"\u230a", "\\lfloor ",
		"\u23b0", "\\lmoustache ",
		"\u2329", "\\langle ",
		"\u2309", "\\rceil ",
		"\u230b", "\\rfloor ",
		"\u23b1", "\\rmoustache ",
		"\u232a", "\\rangle ",
		"\u2a3f", "\\amalg ",
		"\u2217", "\\ast ",
		"\u22bc", "\\barwedge ",
		"\u2305", "\\barwedge ",
		"\u25cb", "\\bigcirc ",
		"\u25bd", "\\bigtriangledown ",
		"\u25b3", "\\bigtriangleup ",
		"\u22a1", "\\boxdot ",
		"\u229f", "\\boxminus ",
		"\u229e", "\\boxplus ",
		"\u22a0", "\\boxtimes ",
		"\u2022", "\\bullet ",
		"\u2219", "\\bullet ",
		"\u2229", "\\cap ",
		"\u22c5", "\\cdot ",
		"\u00b7", "\\centerdot ",
		"\u2218", "\\circ ",
		"\u229b", "\\circledast ",
		"\u229a", "\\circledcirc ",
		"\u229d", "\\circleddash ",
		"\u222a", "\\cup ",
		"\u22ce", "\\curlyvee ",
		"\u22cf", "\\curlywedge ",
		"\u2020", "\\dagger ",
		"\u2021", "\\ddagger ",
		"\u22c4", "\\diamond ",
		"\u00f7", "\\div ",
		"\u22c7", "\\divideontimes ",
		"\u2214", "\\dotplus ",
		"\u2306", "\\doublebarwedge ",
		"\u22d2", "\\doublecap ",
		"\u22d3", "\\doublecup ",
		"\u22d7", "\\gtrdot ",
		"\u22ba", "\\intercal ",
		"\u22cb", "\\leftthreetimes ",
		"\u22d6", "\\lessdot ",
		"\u22c9", "\\ltimes ",
		"\u2213", "\\mp ",
		"\u2299", "\\odot ",
		"\u2296", "\\ominus ",
		"\u2295", "\\oplus ",
		"\u2298", "\\oslash ",
		"\u2297", "\\otimes ",
		"\u00b1", "\\pm ",
		"\u22cc", "\\rightthreetimes ",
		"\u22ca", "\\rtimes ",
		"\u2216", "\\setminus ",
		"\u2293", "\\sqcap ",
		"\u2294", "\\sqcup ",
		"\u22c6", "\\star ",
		"\u00d7", "\\times ",
		"\u25c1", "\\triangleleft ",
		"\u25b7", "\\triangleright ",
		"\u228e", "\\uplus ",
		"\u2228", "\\vee ",
		"\u22bb", "\\veebar ",
		"\u2a61", "\\veebar ",
		"\u2227", "\\wedge ",
		"\u2240", "\\wr ",
		"\u2212", "-",
		"\u2193", "\\downarrow ",
		"\u21d3", "\\Downarrow ",
		"\u2191", "\\uparrow ",
		"\u21d1", "\\Uparrow ",
		"\u2195", "\\updownarrow ",
		"\u21d5", "\\Updownarrow ",
		"\u22c2", "\\bigcap ",
		"\u22c3", "\\bigcup ",
		"\u2a00", "\\bigodot ",
		"\u2a01", "\\bigoplus ",
		"\u2a02", "\\bigotimes ",
		"\u2a06", "\\bigsqcup ",
		"\u2a04", "\\biguplus ",
		"\u22c1", "\\bigvee ",
		"\u22c0", "\\bigwedge ",
		"\u2210", "\\coprod ",
		"\u220f", "\\prod ",
		"\u2211", "\\sum ",
		"\u222b", "\\int ",
		"\u222e", "\\oint ",
		"\u2220", "\\angle ",
		"\u2035", "\\backprime ",
		"\u2605", "\\bigstar ",
		"\u29eb", "\\blacklozenge ",
		"\u25a0", "\\blacksquare ",
		"\u25aa", "\\blacksquare ",
		"\u25b4", "\\blacktriangle ",
		"\u25be", "\\blacktriangledown ",
		"\u2663", "\\clubsuit ",
		"\u2572", "\\diagdown ",
		"\u2571", "\\diagup ",
		"\u2662", "\\diamondsuit ",
		"\u2205", "\\emptyset ",
		"\u2203", "\\exists ",
		"\u266d", "\\flat ",
		"\u2200", "\\forall ",
		"\u2661", "\\heartsuit ",
		"\u221e", "\\infty ",
		"\u25ca", "\\lozenge ",
		"\u2221", "\\measuredangle ",
		"\u2207", "\\nabla ",
		"\u266e", "\\natural ",
		"\u00ac", "\\neg ",
		"\u2204", "\\nexists ",
		"\u2032", "\\prime ",
		"\u266f", "\\sharp ",
		"\u2660", "\\spadesuit ",
		"\u2222", "\\sphericalangle ",
		"\u25a1", "\\square ",
		"\u221a", "\\surd ",
		"\u22a4", "\\top ",
		"\u25b5", "\\triangle ",
		"\u25bf", "\\triangledown ",
		"\u2135", "\\aleph ",
		"\u1d55C", "\\Bbbk ",
		"\u2136", "\\beth ",
		"\u24c8", "\\circledS ",
		"\u2201", "\\complement ",
		"\u2138", "\\daleth ",
		"\u2113", "\\ell ",
		"\u00f0", "\\eth ",
		"\u2132", "\\Finv ",
		"\u2141", "\\Game ",
		"\u2137", "\\gimel ",
		"\u210f", "\\hbar ",
		"\u2111", "\\Im ",
		"\u2127", "\\mho ",
		"\u2202", "\\partial ",
		"\u211c", "\\Re ",
		"\u2118", "\\wp ",
		"\u00B0", "\\degree ",
		"\u211D", "\\mathbb{R} ",
		"\u211A", "\\mathbb{Q} ",
		"\u2115", "\\mathbb{N} ",
		"\u2102", "\\mathbb{C} ",
		"\u2124", "\\mathbb{Z} ",
		"\u2248", "\\approx ",
		"\u224a", "\\approxeq ",
		"\u224d", "\\asymp ",
		"\u223d", "\\backsim ",
		"\u22cd", "\\backsimeq ",
		"\u224f", "\\bumpeq ",
		"\u224e", "\\Bumpeq ",
		"\u2257", "\\circeq ",
		"\u2245", "\\cong ",
		"\u22de", "\\curlyeqprec ",
		"\u22df", "\\curlyeqsucc ",
		"\u2250", "\\doteq ",
		"\u2251", "\\doteqdot ",
		"\u2256", "\\eqcirc ",
		"\u2242", "\\eqsim ",
		"\u2a96", "\\eqslantgtr ",
		"\u2a95", "\\eqslantless ",
		"\u2261", "\\equiv ",
		"\u2252", "\\fallingdotseq ",
		"\u2265", "\\geq ",
		"\u2267", "\\geqq ",
		"\u2a7e", "\\geqslant ",
		"\u226b", "\\gg ",
		"\u2aa2", "\\gg ",
		"\u22d9", "\\ggg ",
		"\u2a8a", "\\gnapprox ",
		"\u2a88", "\\gneq ",
		"\u2269", "\\gneqq ",
		"\u22e7", "\\gnsim ",
		"\u2a86", "\\gtrapprox ",
		"\u22db", "\\gtreqless ",
		"\u2a8c", "\\gtreqqless ",
		"\u2277", "\\gtrless ",
		"\u2273", "\\gtrsim ",
		"\u2264", "\\leq ",
		"\u2266", "\\leqq ",
		"\u2a7d", "\\leqslant ",
		"\u2a85", "\\lessapprox ",
		"\u22da", "\\lesseqgtr ",
		"\u2a8b", "\\lesseqqgtr ",
		"\u2276", "\\lessgtr ",
		"\u2272", "\\lesssim ",
		"\u226a", "\\ll ",
		"\u22d8", "\\llless ",
		"\u2a89", "\\lnapprox ",
		"\u2a87", "\\lneq ",
		"\u2268", "\\lneqq ",
		"\u22e6", "\\lnsim ",
		"\u2247", "\\ncong ",
		"\u2260", "\\neq ",
		"\u2271", "\\ngeq ",
		"\u2267\u0338", "\\ngeqq ",
		"\u2a7e\u0338", "\\ngeqslant ",
		"\u226f", "\\ngtr ",
		"\u2270", "\\nleq ",
		"\u2266\u0338", "\\nleqq ",
		"\u2a7d\u0338", "\\nleqslant ",
		"\u226e", "\\nless ",
		"\u2280", "\\nprec ",
		"\u2aaf\u0338", "\\npreceq ",
		"\u2241", "\\nsim ",
		"\u2281", "\\nsucc ",
		"\u2ab0\u0338", "\\nsucceq ",
		"\u227a", "\\prec ",
		"\u2ab7", "\\precapprox ",
		"\u227c", "\\preccurlyeq ",
		"\u2aaf", "\\preceq ",
		"\u2ab9", "\\precnapprox ",
		"\u2ab5", "\\precneqq ",
		"\u22e8", "\\precnsim ",
		"\u227e", "\\precsim ",
		"\u2253", "\\risingdotseq ",
		"\u223c", "\\sim ",
		"\u2243", "\\simeq ",
		"\u227b", "\\succ ",
		"\u2ab8", "\\succapprox ",
		"\u227d", "\\succcurlyeq ",
		"\u2ab0", "\\succeq ",
		"\u2aba", "\\succnapprox ",
		"\u2ab6", "\\succneqq ",
		"\u22e9", "\\succnsim ",
		"\u227f", "\\succsim ",
		"\u225c", "\\triangleq ",
		"\u21b6", "\\curvearrowleft ",
		"\u21b7", "\\curvearrowright ",
		"\u21ca", "\\downdownarrows ",
		"\u21c3", "\\downharpoonleft ",
		"\u21c2", "\\downharpoonright ",
		"\u21a9", "\\hookleftarrow ",
		"\u21aa", "\\hookrightarrow ",
		"\u2190", "\\leftarrow ",
		"\u21d0", "\\Leftarrow ",
		"\u21a2", "\\leftarrowtail ",
		"\u21bd", "\\leftharpoondown ",
		"\u21bc", "\\leftharpoonup ",
		"\u21c7", "\\leftleftarrows ",
		"\u2194", "\\leftrightarrow ",
		"\u21d4", "\\Leftrightarrow ",
		"\u21c6", "\\leftrightarrows ",
		"\u21cb", "\\leftrightharpoons ",
		"\u21ad", "\\leftrightsquigarrow ",
		"\u21da", "\\Lleftarrow ",
		"\u27f5", "\\longleftarrow ",
		"\u27f8", "\\Longleftarrow ",
		"\u27f6", "\\longrightarrow ",
		"\u27f9", "\\Longrightarrow ",
		"\u27f7", "\\longleftrightarrow ",
		"\u27fa", "\\Longleftrightarrow ",
		"\u21ab", "\\looparrowleft ",
		"\u21ac", "\\looparrowright ",
		"\u21b0", "\\Lsh ",
		"\u21a6", "\\mapsto ",
		"\u22b8", "\\multimap ",
		"\u2197", "\\nearrow ",
		"\u219a", "\\nleftarrow ",
		"\u21cd", "\\nLeftarrow ",
		"\u21ae", "\\nleftrightarrow ",
		"\u21ce", "\\nLeftrightarrow ",
		"\u219b", "\\nrightarrow ",
		"\u21cf", "\\nRightarrow ",
		"\u2196", "\\nwarrow ",
		"\u2192", "\\rightarrow ",
		"\u21d2", "\\Rightarrow ",
		"\u21a3", "\\rightarrowtail ",
		"\u21c1", "\\rightharpoondown ",
		"\u21c0", "\\rightharpoonup ",
		"\u21c4", "\\rightleftarrows ",
		"\u21cc", "\\rightleftharpoons ",
		"\u21c9", "\\rightrightarrows ",
		"\u219d", "\\rightsquigarrow ",
		"\u21db", "\\Rrightarrow ",
		"\u21b1", "\\Rsh ",
		"\u2198", "\\searrow ",
		"\u2199", "\\swarrow ",
		"\u219e", "\\twoheadleftarrow ",
		"\u21a0", "\\twoheadrightarrow ",
		"\u21bf", "\\upharpoonleft ",
		"\u21be", "\\upharpoonright ",
		"\u21c8", "\\upuparrows ",
		"\u03f6", "\\backepsilon ",
		"\u2235", "\\because ",
		"\u226c", "\\between ",
		"\u25c0", "\\blacktriangleleft ",
		"\u25b6", "\\blacktriangleright ",
		"\u22c8", "\\bowtie ",
		"\u22a3", "\\dashv ",
		"\u2322", "\\frown ",
		"\u220a", "\\in ",
		"\u2223", "\\mid ",
		"\u22a7", "\\models ",
		"\u220b", "\\ni ",
		"\u2224", "\\nmid ",
		"\u2209", "\\notin ",
		"\u2226", "\\nparallel ",
		"\u2286\u0338", "\\nsubseteq ",
		"\u2288", "\\nsubseteq ",
		"\u2ac5\u0338", "\\nsubseteqq ",
		"\u2289", "\\nsupseteq ",
		"\u2289\u0338", "\\nsupseteq ",
		"\u2ac6\u0338", "\\nsupseteqq ",
		"\u22ea", "\\ntriangleleft ",
		"\u22ec", "\\ntrianglelefteq ",
		"\u22eb", "\\ntriangleright ",
		"\u22ed", "\\ntrianglerighteq ",
		"\u22ac", "\\nvdash ",
		"\u22ad", "\\nvDash ",
		"\u22ae", "\\nVdash ",
		"\u22af", "\\nVDash ",
		"\u220d", "\\owns ",
		"\u2225", "\\parallel ",
		"\u22a5", "\\perp ",
		"\u22d4", "\\pitchfork ",
		"\u221d", "\\propto ",
		"\u2323", "\\smile ",
		"\u228f", "\\sqsubset ",
		"\u2291", "\\sqsubseteq ",
		"\u2290", "\\sqsupset ",
		"\u2292", "\\sqsupseteq ",
		"\u2282", "\\subset ",
		"\u22d0", "\\Subset ",
		"\u2286", "\\subseteq ",
		"\u2ac5", "\\subseteqq ",
		"\u228a", "\\subsetneq ",
		"\u2acb", "\\subsetneqq ",
		"\u2283", "\\supset ",
		"\u22d1", "\\Supset ",
		"\u2287", "\\supseteq ",
		"\u2ac6", "\\supseteqq ",
		"\u228b", "\\supsetneq ",
		"\u2acc", "\\supsetneqq ",
		"\u2234", "\\therefore ",
		"\u22b4", "\\trianglelefteq ",
		"\u22b5", "\\trianglerighteq ",
		"\u228a\ufe00", "\\varsubsetneq ",
		"\u2acb\ufe00", "\\varsubsetneqq ",
		"\u228b\ufe00", "\\varsupsetneq ",
		"\u2acc\ufe00", "\\varsupsetneqq ",
		"\u22b2", "\\vartriangleleft ",
		"\u22b3", "\\vartriangleright ",
		"\u22a2", "\\vdash ",
		"\u22a8", "\\vDash ",
		"\u22a9", "\\Vdash ",
		"\u22aa", "\\Vvdash ",
		"\u22ee", "\\vdots ",
		"\u2026", "\\ldots ",
		"\u22ef", "\\cdots ",
		"\u22c5\u22c5\u22c5", "\\dotsm ",
		"\u22f1", "\\ddots ",
		"\u03b1", "\\alpha ",
		"\u03b2", "\\beta ",
		"\u03c7", "\\chi ",
		"\u03b4", "\\delta ",
		"\u0394", "\\Delta ",
		"\u03dd", "\\digamma ",
		"\u03f5", "\\epsilon ",
		"\u03b7", "\\eta ",
		"\u03b3", "\\gamma ",
		"\u0393", "\\Gamma ",
		"\u03b9", "\\iota ",
		"\u03ba", "\\kappa ",
		"\u03bb", "\\lambda ",
		"\u039b", "\\Lambda ",
		"\u03bc", "\\mu ",
		"\u03bd", "\\nu ",
		"\u03c9", "\\omega ",
		"\u03a9", "\\Omega ",
		"\u03d5", "\\phi ",
		"\u03a6", "\\Phi ",
		"\u03c0", "\\pi ",
		"\u03a0", "\\Pi ",
		"\u03c8", "\\psi ",
		"\u03a8", "\\Psi ",
		"\u03c1", "\\rho ",
		"\u03c3", "\\sigma ",
		"\u03a3", "\\Sigma ",
		"\u03c4", "\\tau ",
		"\u03b8", "\\theta ",
		"\u0398", "\\Theta ",
		"\u03c5", "\\upsilon ",
		"\u03d2", "\\Upsilon ",
		"\u03b5", "\\varepsilon ",
		"\u03f0", "\\varkappa ",
		"\u03c6", "\\varphi ",
		"\u03d6", "\\varpi ",
		"\u03f1", "\\varrho ",
		"\u03c2", "\\varsigma ",
		"\u03d1", "\\vartheta ",
		"\u03be", "\\xi ",
		"\u039e", "\\Xi ",
		"\u03b6", "\\zeta "
	});
}
