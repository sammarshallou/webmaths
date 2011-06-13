package uk.ac.open.lts.webmaths.tex;

import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;

/**
 * Class ported from some Python code that converts LaTeX to MathML.
 * Use via {@link TokenInput#toMathml()}.
 * <p>
 * This version of the source code contains the original Python code in
 * comments that begin at the first character of the line.  
 */
public class LatexToMathml
{
	/**
	 * Java interface equivalent for the lambda functions that take one TokenInput
	 * parameter.
	 */
	private interface LambdaTokenInput
	{
		/**
		 * Calls the function.
		 * @param slf Input parameter
		 * @return Return value
		 */
		public Element call(TokenInput slf); 
	}
	
	/**
	 * Java interface equivalent for the lambda functions with two parameters
	 * as shown.
	 */
	private interface Lambda2
	{
		/**
		 * Calls the function.
		 * @param slf Tokens
		 * @param stopTokens Stop tokens
		 * @return Return value
		 */
		public Element call(TokenInput slf, Map<String, String> stopTokens);
	}
	
	/**
	 * MathML namespace.
	 */
//mmlns = 'http://www.w3.org/1998/Math/MathML'
	public final static String NS = "http://www.w3.org/1998/Math/MathML";
	
//#create empty DOM document
//document = xml.dom.minidom.getDOMImplementation().createDocument(None,None,None)
	private Document document;
	
	/**
	 * @throws ParserConfigurationException Any error creating DOM data
	 */
	public LatexToMathml() throws ParserConfigurationException
	{
		// Note: There wasn't a constructor in the original Python version because
		// it used static methods.
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		document = factory.newDocumentBuilder().newDocument();
	}

//def result_element(tag, num_attrs, *args):
//node = document.createElementNS(mmlns, tag)
//
//for i in range(0, num_attrs):
//  if args[2*i+1] is not None:
//    node.setAttribute(args[2*i], args[2*i+1])
//
//for i in range(num_attrs*2, len(args)):
//  if args[i] is not None:
//    if isinstance(args[i], unicode):
//      node.appendChild(document.createTextNode(args[i]))
//    else:
//      node.appendChild(args[i])
//
//return node
	/**
	 * Creates an XML element.
	 * <p>
	 * I have left this function the same as in Python which is
	 * pretty horrible. The arguments must start with numAttrs*2 strings
	 * as name-value pairs for attributes; after than they can be String
	 * (converted to a text node and appended) or Node (appended).
	 * @param tag Tag name
	 * @param numAttrs Number of attribute name/value pairs included in array
	 * @param args Arguments
	 */
	private Element resultElement(String tag, int numAttrs, Object... args)
	{
		Element node = document.createElementNS(NS, tag);
		for(int i=0; i<numAttrs; i++)
		{
			if(args[2 * i + 1] != null)
			{
				node.setAttribute((String)args[2 * i], (String)args[2 * i + 1]);
			}
		}
		
		for(int i=numAttrs*2; i<args.length; i++)
		{
			if(args[i] != null)
			{
				if(args[i] instanceof String)
				{
					node.appendChild(document.createTextNode((String)args[i]));
				}
				else
				{
					node.appendChild((Node)args[i]);
				}
			}
		}
		
		return node;
	}
	
//def result_element_append(parent, child):
//if (parent is not None) and (child is not None):
//  if isinstance(child, unicode):
//    parent.appendChild(document.createTextNode(child))
//  else:
//    parent.appendChild(child)
	/**
	 * Appends a string (as text node) to the parent node.
	 * Note: This appears never to be used.
	 * @param parent Parent node
	 * @param child Child text
	 */
	private void resultElementAppend(Node parent, String child)
	{
		if(parent != null && child != null)
		{
			parent.appendChild(document.createTextNode(child));
		}
	}
	/**
	 * Appends a DOM node to the parent node.
	 * @param parent Parent node
	 * @param child Child node
	 */
	private void resultElementAppend(Node parent, Node child)
	{
		if(parent != null && child != null)
		{
			parent.appendChild((Node)child);
		}
	}

//def result_element_prepend(parent, child, next):
//if next is None:
//  result_element_append(parent, child)
//elif (parent is not None) and (child is not None):
//  parent.insertBefore(child, next)
	/**
	 * Inserts a DOM node.
	 * @param parent Parent node
	 * @param child Child node to insert
	 * @param next Node to insert before. If null, will insert at end
	 */
	private void resultElementPrepend(Node parent, Node child, Node next)
	{
		if(next == null)
		{
			resultElementAppend(parent, child);
		}
		else if(parent != null && child != null)
		{
			parent.insertBefore(child, next);
		}
	}

//def result_set_attr(elem, attr, value):
//if (elem is not None) and (attr is not None):
//  if value is not None:
//    elem.setAttribute(attr, value)
//  else:
//    elem.removeAttribute(attr)
	/**
	 * Sets an attribute.
	 * @param elem Element to set attribute on
	 * @param attr Attribute name
	 * @param value Attribute value (may be null to unset)
	 */
	void resultSetAttr(Element elem, String attr, String value)
	{
		if(elem != null && attr != null)
		{
			if(value != null)
			{
				elem.setAttribute(attr, value);
			}
			else
			{
				elem.removeAttribute(attr);
			}
		}
	}

//def result_append_attr(elem, attr, value):
//if (elem is not None) and (attr is not None):
//  #old_value = elem.getAttribute(elem, attr) #bug?
//  old_value = elem.getAttribute(attr)
//  if old_value is None:
//    elem.setAttribute(attr, value)
//  else:
//    elem.setAttribute(attr, old_value + value)
	void resultAppendAttr(Element elem, String attr, String value)
	{
		if(elem != null && attr != null)
		{
			// Get old value (returns empty string if not set)
			String oldValue = elem.getAttribute(attr);
			// Append and set
			elem.setAttribute(attr, oldValue + value);
		}
	}
	
	private static Map<String, String> makeMap(String[] data)
	{
		Map<String, String> result = new HashMap<String, String>();
		for(int i=0; i<data.length; i+=2)
		{
			result.put(data[i], data[i+1]);
		}
		return result;
	}

	private final static Map<String, String> PUNCT_AND_SPACE =
		makeMap(new String[] 
  {
		"\\quad", "\u2003",
		"\\qquad", "\u2003\u2003",
		"\\thickspace", "\u2002",
		"\\;", "\u2002",
		"\\medspace", "\u2005",
		"\\:", "\u2005",
		"\\thinspace", "\u2009",
		"\\,", "\u2009",
		"\\!", "\u200b",
		"~", "\u00A0",
		".", ".",
		";", ";",
		"?", "?",
		"\\qedsymbol", "\u25a0"
	});

	private final static Map<String, String> LEFT_DELIMITERS =
		makeMap(new String[] 
  {
		"(", "(",
		"[", "[",
		"\\{", "{",
		"\\lgroup", "(",
		"\\lbrace", "{",
		"\\lvert", "|",
		"\\lVert", "\u2016",
		"\\lceil", "\u2308",
		"\\lfloor", "\u230a",
		"\\lmoustache", "\u23b0",
		"\\langle", "\u2329"
	});

	private final static Map<String, String> RIGHT_DELIMITERS =
		makeMap(new String[] 
  {
		")", ")",
		"]", "]",
		"\\}", "}",
		"\\rbrace", "}",
		"\\rgroup", ")",
		"\\rvert", "|",
		"\\rVert", "\u2016",
		"\\rceil", "\u2309",
		"\\rfloor", "\u230b",
		"\\rmoustache", "\u23b1",
		"\\rangle", "\u232a",
  });

	private final static Map<String, String> OPERATOR_SYMBOLS =
		makeMap(new String[] 
  {
		"\\amalg", "\u2a3f",
		"\\ast", "*",
		"\\ast", "\u2217",
		"\\barwedge", "\u22bc",
		"\\barwedge", "\u2305",
		"\\bigcirc", "\u25cb",
		"\\bigtriangledown", "\u25bd",
		"\\bigtriangleup", "\u25b3",
		"\\boxdot", "\u22a1",
		"\\boxminus", "\u229f",
		"\\boxplus", "\u229e",
		"\\boxtimes", "\u22a0",
		"\\bullet", "\u2022",
		"\\bullet", "\u2219",
		"\\cap", "\u2229",
		"\\intersect", "\u2229",
		"\\Cap", "\u22d2",
		"\\cdot", "\u22c5",
		"\\centerdot", "\u00b7",
		"\\circ", "\u2218",
		"\\circledast", "\u229b",
		"\\circledcirc", "\u229a",
		"\\circleddash", "\u229d",
		"\\cup", "\u222a",
		"\\union", "\u222a",
		"\\Cup", "\u22d3",
		"\\curlyvee", "\u22ce",
		"\\curlywedge", "\u22cf",
		"\\dagger", "\u2020",
		"\\ddagger", "\u2021",
		"\\diamond", "\u22c4",
		"\\div", "\u00f7",
		"\\divideontimes", "\u22c7",
		"\\dotplus", "\u2214",
		"\\doublebarwedge", "\u2306",
		"\\doublecap", "\u22d2",
		"\\doublecup", "\u22d3",
		"\\gtrdot", "\u22d7",
		"\\intercal", "\u22ba",
		"\\land", "\u2227",
		"\\leftthreetimes", "\u22cb",
		"\\lessdot", "\u22d6",
		"\\lor", "\u2228",
		"\\ltimes", "\u22c9",
		"\\mp", "\u2213",
		"\\odot", "\u2299",
		"\\ominus", "\u2296",
		"\\oplus", "\u2295",
		"\\oslash", "\u2298",
		"\\otimes", "\u2297",
		"\\pm", "\u00b1",
		"\\rightthreetimes", "\u22cc",
		"\\rtimes", "\u22ca",
		"\\setminus", "\u2216",
		"\\smallsetminus", "\u2216",
		"\\sqcap", "\u2293",
		"\\sqcup", "\u2294",
		"\\star", "\u22c6",
		"\\times", "\u00d7",
		"\\triangleleft", "\u25c1",
		"\\triangleright", "\u25b7",
		"\\uplus", "\u228e",
		"\\vee", "\u2228",
		"\\veebar", "\u22bb",
		"\\veebar", "\u2a61",
		"\\wedge", "\u2227",
		"\\wr", "\u2240",
		"+", "+",
		"-", "\u2212",
		"*", "*",
		",", ",",
		"/", "\u2215",
		":", ":",
		"\\colon", ":",
		"|", "|",
		"\\vert", "|",
		"\\Vert", "\u2016",
		"\\|", "\u2016",
		"\\backslash", "\\",
		"'", "\u2032",
		"\\#", "#",
		"\\%", "%",
		"\\bmod", "mod",
		"\\mod", "mod",
		"\\downarrow", "\u2193",
		"\\Downarrow", "\u21d3",
		"\\uparrow", "\u2191",
		"\\Uparrow", "\u21d1",
		"\\updownarrow", "\u2195",
		"\\Updownarrow", "\u21d5",
		"\\bigcap", "\u22c2",
		"\\bigcup", "\u22c3",
		"\\bigodot", "\u2a00",
		"\\bigoplus", "\u2a01",
		"\\bigotimes", "\u2a02",
		"\\bigsqcup", "\u2a06",
		"\\biguplus", "\u2a04",
		"\\bigvee", "\u22c1",
		"\\bigwedge", "\u22c0",
		"\\coprod", "\u2210",
		"\\prod", "\u220f",
		"\\sum", "\u2211",
		"\\doublesum", "\u2211\u2211",
		"\\int", "\u222b",
		"\\smallint", "\u222b",
		"\\oint", "\u222e",
		"\\angle", "\u2220",
		"\\backprime", "\u2035",
		"\\bigstar", "\u2605",
		"\\blacklozenge", "\u29eb",
		"\\blacksquare", "\u25a0",
		"\\blacksquare", "\u25aa",
		"\\blacktriangle", "\u25b4",
		"\\blacktriangledown", "\u25be",
		"\\bot", "\u22a5",
		"\\clubsuit", "\u2663",
		"\\diagdown", "\u2572",
		"\\diagup", "\u2571",
		"\\diamondsuit", "\u2662",
		"\\emptyset", "\u2205",
		"\\exists", "\u2203",
		"\\flat", "\u266d",
		"\\forall", "\u2200",
		"\\heartsuit", "\u2661",
		"\\infty", "\u221e",
		"\\lnot", "\u00ac",
		"\\lozenge", "\u25ca",
		"\\measuredangle", "\u2221",
		"\\nabla", "\u2207",
		"\\natural", "\u266e",
		"\\neg", "\u00ac",
		"\\nexists", "\u2204",
		"\\prime", "\u2032",
		"\\sharp", "\u266f",
		"\\spadesuit", "\u2660",
		"\\sphericalangle", "\u2222",
		"\\square", "\u25a1",
		"\\surd", "\u221a",
		"\\top", "\u22a4",
		"\\triangle", "\u25b5",
		"\\triangledown", "\u25bf",
		"\\varnothing", "\u2205",
		"\\aleph", "\u2135",
		"\\Bbbk", "\u1d55C",
		"\\beth", "\u2136",
		"\\circledS", "\u24c8",
		"\\complement", "\u2201",
		"\\daleth", "\u2138",
		"\\ell", "\u2113",
		"\\eth", "\u00f0",
		"\\Finv", "\u2132",
		"\\Game", "\u2141",
		"\\gimel", "\u2137",
		"\\hbar", "\u210f",
		"\\hslash", "\u210f",
		"\\Im", "\u2111",
		"\\mho", "\u2127",
		"\\partial", "\u2202",
		"\\Re", "\u211c",
		"\\wp", "\u2118",
		"\\degree", "\u00B0",
		"\\R", "\u211D",
		"\\Q", "\u211A",
		"\\N", "\u2115",
		"\\C", "\u2102",
		"\\Z", "\u2124"
  });


	private final static Map<String, String> RELATION_SYMBOLS =
		makeMap(new String[] 
  {
		"=", "=",
		"<", "<",
		">", ">",
		"\\approx", "\u2248",
		"\\approxeq", "\u224a",
		"\\asymp", "\u224d",
		"\\backsim", "\u223d",
		"\\backsimeq", "\u22cd",
		"\\bumpeq", "\u224f",
		"\\Bumpeq", "\u224e",
		"\\circeq", "\u2257",
		"\\cong", "\u2245",
		"\\curlyeqprec", "\u22de",
		"\\curlyeqsucc", "\u22df",
		"\\doteq", "\u2250",
		"\\doteqdot", "\u2251",
		"\\eqcirc", "\u2256",
		"\\eqsim", "\u2242",
		"\\eqslantgtr", "\u2a96",
		"\\eqslantless", "\u2a95",
		"\\equiv", "\u2261",
		"\\fallingdotseq", "\u2252",
		"\\ge", "\u2265",
		"\\geq", "\u2265",
		"\\geqq", "\u2267",
		"\\geqslant", "\u2a7e",
		"\\gg", "\u226b",
		"\\gg", "\u2aa2",
		"\\ggg", "\u22d9",
		"\\gggtr", "\u22d9",
		"\\gnapprox", "\u2a8a",
		"\\gneq", "\u2a88",
		"\\gneqq", "\u2269",
		"\\gnsim", "\u22e7",
		"\\gtrapprox", "\u2a86",
		"\\gtreqless", "\u22db",
		"\\gtreqqless", "\u2a8c",
		"\\gtrless", "\u2277",
		"\\gtrsim", "\u2273",
		"\\gvertneqq", "\u2269",
		"\\le", "\u2264",
		"\\leq", "\u2264",
		"\\leqq", "\u2266",
		"\\leqslant", "\u2a7d",
		"\\lessapprox", "\u2a85",
		"\\lesseqgtr", "\u22da",
		"\\lesseqqgtr", "\u2a8b",
		"\\lessgtr", "\u2276",
		"\\lesssim", "\u2272",
		"\\ll", "\u226a",
		"\\llless", "\u22d8",
		"\\lnapprox", "\u2a89",
		"\\lneq", "\u2a87",
		"\\lneqq", "\u2268",
		"\\lnsim", "\u22e6",
		"\\lvertneqq", "\u2268",
		"\\ncong", "\u2247",
		"\\ne", "\u2260",
		"\\neq", "\u2260",
		"\\ngeq", "\u2271",
		"\\ngeqq", "\u2267",
		"\\ngeqslant", "\u2a7e",
		"\\ngtr", "\u226f",
		"\\nleq", "\u2270",
		"\\nleqq", "\u2266",
		"\\nleqslant", "\u2a7d",
		"\\nless", "\u226e",
		"\\nprec", "\u2280",
		"\\npreceq", "\u2aaf",
		"\\nsim", "\u2241",
		"\\nsucc", "\u2281",
		"\\nsucceq", "\u2ab0",
		"\\prec", "\u227a",
		"\\precapprox", "\u2ab7",
		"\\preccurlyeq", "\u227c",
		"\\preceq", "\u2aaf",
		"\\precnapprox", "\u2ab9",
		"\\precneqq", "\u2ab5",
		"\\precnsim", "\u22e8",
		"\\precsim", "\u227e",
		"\\risingdotseq", "\u2253",
		"\\sim", "\u223c",
		"\\simeq", "\u2243",
		"\\succ", "\u227b",
		"\\succapprox", "\u2ab8",
		"\\succcurlyeq", "\u227d",
		"\\succeq", "\u2ab0",
		"\\succnapprox", "\u2aba",
		"\\succneqq", "\u2ab6",
		"\\succnsim", "\u22e9",
		"\\succsim", "\u227f",
		"\\thickapprox", "\u2248",
		"\\thicksim", "\u223c",
		"\\triangleq", "\u225c",
		"\\curvearrowleft", "\u21b6",
		"\\curvearrowright", "\u21b7",
		"\\downdownarrows", "\u21ca",
		"\\downharpoonleft", "\u21c3",
		"\\downharpoonright", "\u21c2",
		"\\gets", "\u2190",
		"\\hookleftarrow", "\u21a9",
		"\\hookrightarrow", "\u21aa",
		"\\leftarrow", "\u2190",
		"\\Leftarrow", "\u21d0",
		"\\leftarrowtail", "\u21a2",
		"\\leftharpoondown", "\u21bd",
		"\\leftharpoonup", "\u21bc",
		"\\leftleftarrows", "\u21c7",
		"\\leftrightarrow", "\u2194",
		"\\leftrightarrows", "\u21c6",
		"\\leftrightharpoons", "\u21cb",
		"\\leftrightsquigarrow", "\u21ad",
		"\\Lleftarrow", "\u21da",
		"\\longleftarrow", "\u27f5",
		"\\Longleftarrow", "\u27f8",
		"\\longleftrightarrow", "\u27f7",
		"\\Longleftrightarrow", "\u27fa",
		"\\looparrowleft", "\u21ab",
		"\\looparrowright", "\u21ac",
		"\\Lsh", "\u21b0",
		"\\mapsto", "\u21a6",
		"\\multimap", "\u22b8",
		"\\nearrow", "\u2197",
		"\\nleftarrow", "\u219a",
		"\\nLeftarrow", "\u21cd",
		"\\nleftrightarrow", "\u21ae",
		"\\nLeftrightarrow", "\u21ce",
		"\\nrightarrow", "\u219b",
		"\\nRightarrow", "\u21cf",
		"\\nwarrow", "\u2196",
		"\\restriction", "\u21be",
		"\\rightarrow", "\u2192",
		"\\Rightarrow", "\u21d2",
		"\\rightarrowtail", "\u21a3",
		"\\rightharpoondown", "\u21c1",
		"\\rightharpoonup", "\u21c0",
		"\\rightleftarrows", "\u21c4",
		"\\rightleftharpoons", "\u21cc",
		"\\rightrightarrows", "\u21c9",
		"\\rightsquigarrow", "\u219d",
		"\\Rrightarrow", "\u21db",
		"\\Rsh", "\u21b1",
		"\\searrow", "\u2198",
		"\\swarrow", "\u2199",
		"\\to", "\u2192",
		"\\twoheadleftarrow", "\u219e",
		"\\twoheadrightarrow", "\u21a0",
		"\\upharpoonleft", "\u21bf",
		"\\upharpoonright", "\u21be",
		"\\upuparrows", "\u21c8",
		"\\backepsilon", "\u03f6",
		"\\because", "\u2235",
		"\\between", "\u226c",
		"\\blacktriangleleft", "\u25c0",
		"\\blacktriangleright", "\u25b6",
		"\\bowtie", "\u22c8",
		"\\dashv", "\u22a3",
		"\\frown", "\u2323",
		"\\in", "\u220a",
		"\\mid", "\u2223",
		"\\models", "\u22a7",
		"\\ni", "\u220b",
		"\\ni", "\u220d",
		"\\nmid", "\u2224",
		"\\notin", "\u2209",
		"\\nparallel", "\u2226",
		"\\nshortmid", "\u2224",
		"\\nshortparallel", "\u2226",
		"\\nsubseteq", "\u2286",
		"\\nsubseteq", "\u2288",
		"\\nsubseteqq", "\u2ac5",
		"\\nsupseteq", "\u2287",
		"\\nsupseteq", "\u2289",
		"\\nsupseteqq", "\u2ac6",
		"\\ntriangleleft", "\u22ea",
		"\\ntrianglelefteq", "\u22ec",
		"\\ntriangleright", "\u22eb",
		"\\ntrianglerighteq", "\u22ed",
		"\\nvdash", "\u22ac",
		"\\nvDash", "\u22ad",
		"\\nVdash", "\u22ae",
		"\\nVDash", "\u22af",
		"\\owns", "\u220d",
		"\\parallel", "\u2225",
		"\\perp", "\u22a5",
		"\\pitchfork", "\u22d4",
		"\\propto", "\u221d",
		"\\shortmid", "\u2223",
		"\\shortparallel", "\u2225",
		"\\smallfrown", "\u2322",
		"\\smallsmile", "\u2323",
		"\\smile", "\u2323",
		"\\sqsubset", "\u228f",
		"\\sqsubseteq", "\u2291",
		"\\sqsupset", "\u2290",
		"\\sqsupseteq", "\u2292",
		"\\subset", "\u2282",
		"\\Subset", "\u22d0",
		"\\subseteq", "\u2286",
		"\\subseteqq", "\u2ac5",
		"\\subsetneq", "\u228a",
		"\\subsetneqq", "\u2acb",
		"\\supset", "\u2283",
		"\\Supset", "\u22d1",
		"\\supseteq", "\u2287",
		"\\supseteqq", "\u2ac6",
		"\\supsetneq", "\u228b",
		"\\supsetneqq", "\u2acc",
		"\\therefore", "\u2234",
		"\\trianglelefteq", "\u22b4",
		"\\trianglerighteq", "\u22b5",
		"\\varpropto", "\u221d",
		"\\varsubsetneq", "\u228a",
		"\\varsubsetneqq", "\u2acb",
		"\\varsupsetneq", "\u228b",
		"\\varsupsetneqq", "\u2acc",
		"\\vartriangle", "\u25b5",
		"\\vartriangleleft", "\u22b2",
		"\\vartriangleright", "\u22b3",
		"\\vdash", "\u22a2",
		"\\vDash", "\u22a8",
		"\\Vdash", "\u22a9",
		"\\Vvdash", "\u22aa",
  });

private final static Map<String, String> NAMED_IDENTIFIERS =
	makeMap(new String[] 
  {
		"\\arccos", "arccos\u2009",
		"\\arcsin", "arcsin\u2009",
		"\\arctan", "arctan\u2009",
		"\\arg", "arg\u2009",
		"\\cos", "cos\u2009",
		"\\cosh", "cosh\u2009",
		"\\cot", "cot\u2009",
		"\\coth", "coth\u2009",
		"\\csc", "csc\u2009",
		"\\deg", "deg\u2009",
		"\\det", "det\u2009",
		"\\dim", "dim\u2009",
		"\\exp", "exp\u2009",
		"\\gcd", "gcd\u2009",
		"\\hom", "hom\u2009",
		"\\ker", "ker\u2009",
		"\\lg", "lg\u2009",
		"\\ln", "ln\u2009",
		"\\log", "log\u2009",
		"\\Pr", "Pr\u2009",
		"\\sec", "sec\u2009",
		"\\sin", "sin\u2009",
		"\\sinh", "sinh\u2009",
		"\\tan", "tan\u2009",
		"\\tanh", "tanh\u2009",
		"\\inf", "inf\u2009",
		"\\injlim", "inj lim\u2009",
		"\\lim", "lim\u2009",
		"\\liminf", "lim inf\u2009",
		"\\limsup", "lum sup\u2009",
		"\\max", "max\u2009",
		"\\min", "min\u2009",
		"\\projlim", "proj lim\u2009",
		"\\sup", "sup\u2009",
		"\\alpha", "\u03b1",
		"\\beta", "\u03b2",
		"\\chi", "\u03c7",
		"\\delta", "\u03b4",
		"\\Delta", "\u0394",
		"\\digamma", "\u03dd",
		"\\epsilon", "\u03f5",
		"\\eta", "\u03b7",
		"\\gamma", "\u03b3",
		"\\Gamma", "\u0393",
		"\\iota", "\u03b9",
		"\\kappa", "\u03ba",
		"\\lambda", "\u03bb",
		"\\Lambda", "\u039b",
		"\\mu", "\u03bc",
		"\\nu", "\u03bd",
		"\\omega", "\u03c9",
		"\\Omega", "\u03a9",
		"\\phi", "\u03d5",
		"\\Phi", "\u03a6",
		"\\pi", "\u03c0",
		"\\Pi", "\u03a0",
		"\\psi", "\u03c8",
		"\\Psi", "\u03a8",
		"\\rho", "\u03c1",
		"\\sigma", "\u03c3",
		"\\Sigma", "\u03a3",
		"\\tau", "\u03c4",
		"\\theta", "\u03b8",
		"\\Theta", "\u0398",
		"\\upsilon", "\u03c5",
		"\\Upsilon", "\u03d2",
		"\\varepsilon", "\u03b5",
		"\\varkappa", "\u03f0",
		"\\varphi", "\u03c6",
		"\\varpi", "\u03d6",
		"\\varrho", "\u03f1",
		"\\varsigma", "\u03c2",
		"\\vartheta", "\u03d1",
		"\\xi", "\u03be",
		"\\Xi", "\u039e",
		"\\zeta", "\u03b6",
		"a", "a",
		"b", "b",
		"c", "c",
		"d", "d",
		"e", "e",
		"f", "f",
		"g", "g",
		"h", "h",
		"i", "i",
		"j", "j",
		"k", "k",
		"l", "l",
		"m", "m",
		"n", "n",
		"o", "o",
		"p", "p",
		"q", "q",
		"r", "r",
		"s", "s",
		"t", "t",
		"u", "u",
		"v", "v",
		"w", "w",
		"x", "x",
		"y", "y",
		"z", "z",
		"A", "A",
		"B", "B",
		"C", "C",
		"D", "D",
		"E", "E",
		"F", "F",
		"G", "G",
		"H", "H",
		"I", "I",
		"J", "J",
		"K", "K",
		"L", "L",
		"M", "M",
		"N", "N",
		"O", "O",
		"P", "P",
		"Q", "Q",
		"R", "R",
		"S", "S",
		"T", "T",
		"U", "U",
		"V", "V",
		"W", "W",
		"X", "X",
		"Y", "Y",
		"Z", "Z",
		"\\vdots", "\u22ee",
		"\\hdots", "\u2026",
		"\\ldots", "\u2026",
		"\\dots", "\u2026",
		"\\cdots", "\u00b7\u00b7\u00b7",
		"\\dotsb", "\u00b7\u00b7\u00b7",
		"\\dotsc", "\u2026",
		"\\dotsi", "\u22c5\u22c5\u22c5",
		"\\dotsm", "\u22c5\u22c5\u22c5",
		"\\dotso", "\u2026",
		"\\ddots", "\u22f1"
  });

	private final static Map<String, String> WORD_OPERATORS =
		makeMap(new String[] 
  {
		"\\arccos", "arccos\u2009",
		"\\arcsin", "arcsin\u2009",
		"\\arctan", "arctan\u2009",
		"\\arg", "arg\u2009",
		"\\cos", "cos\u2009",
		"\\cosh", "cosh\u2009",
		"\\cot", "cot\u2009",
		"\\coth", "coth\u2009",
		"\\csc", "csc\u2009",
		"\\deg", "deg\u2009",
		"\\det", "det\u2009",
		"\\dim", "dim\u2009",
		"\\exp", "exp\u2009",
		"\\gcd", "gcd\u2009",
		"\\hom", "hom\u2009",
		"\\ker", "ker\u2009",
		"\\lg", "lg\u2009",
		"\\ln", "ln\u2009",
		"\\log", "log\u2009",
		"\\Pr", "Pr\u2009",
		"\\sec", "sec\u2009",
		"\\sin", "sin\u2009",
		"\\sinh", "sinh\u2009",
		"\\tan", "tan\u2009",
		"\\tanh", "tanh\u2009"
  });

	/**
	 * Note: This is never used in the original Python either.
	 */
	private final static Map<String, String> BIG_WORD_OPERATORS =
		makeMap(new String[] 
  {
		"\\inf", "inf\u2009",
		"\\injlim", "inj lim\u2009",
		"\\lim", "lim\u2009",
		"\\liminf", "lim inf\u2009",
		"\\limsup", "lum sup\u2009",
		"\\max", "max\u2009",
		"\\min", "min\u2009",
		"\\projlim", "proj lim\u2009",
		"\\sup", "sup\u2009"
  });

	private final static Map<String, String> GREEK_LETTERS =
		makeMap(new String[] 
  {
		"\\alpha", "\u03b1",
		"\\beta", "\u03b2",
		"\\chi", "\u03c7",
		"\\delta", "\u03b4",
		"\\Delta", "\u0394",
		"\\digamma", "\u03dd",
		"\\epsilon", "\u03f5",
		"\\eta", "\u03b7",
		"\\gamma", "\u03b3",
		"\\Gamma", "\u0393",
		"\\iota", "\u03b9",
		"\\kappa", "\u03ba",
		"\\lambda", "\u03bb",
		"\\Lambda", "\u039b",
		"\\mu", "\u03bc",
		"\\nu", "\u03bd",
		"\\omega", "\u03c9",
		"\\Omega", "\u03a9",
		"\\phi", "\u03d5",
		"\\Phi", "\u03a6",
		"\\pi", "\u03c0",
		"\\Pi", "\u03a0",
		"\\psi", "\u03c8",
		"\\Psi", "\u03a8",
		"\\rho", "\u03c1",
		"\\sigma", "\u03c3",
		"\\Sigma", "\u03a3",
		"\\tau", "\u03c4",
		"\\theta", "\u03b8",
		"\\Theta", "\u0398",
		"\\upsilon", "\u03c5",
		"\\Upsilon", "\u03d2",
		"\\varepsilon", "\u03b5",
		"\\varkappa", "\u03f0",
		"\\varphi", "\u03c6",
		"\\varpi", "\u03d6",
		"\\varrho", "\u03f1",
		"\\varsigma", "\u03c2",
		"\\vartheta", "\u03d1",
		"\\xi", "\u03be",
		"\\Xi", "\u039e",
		"\\zeta", "\u03b6",
  });

	private final static Map<String, String> LIMIT_COMMANDS =
		makeMap(new String[] 
  {
		"\\bigcap", "\u22c2",
		"\\bigcup", "\u22c3",
		"\\bigodot", "\u2a00",
		"\\bigoplus", "\u2a01",
		"\\bigotimes", "\u2a02",
		"\\bigsqcup", "\u2a06",
		"\\biguplus", "\u2a04",
		"\\bigvee", "\u22c1",
		"\\bigwedge", "\u22c0",
		"\\coprod", "\u2210",
		"\\prod", "\u220f",
		"\\sum", "\u2211",
		"\\doublesum", "\u2211\u2211",
		"\\inf", "inf",
		"\\injlim", "inj lim",
		"\\lim", "lim",
		"\\liminf", "lim inf",
		"\\limsup", "lum sup",
		"\\max", "max",
		"\\min", "min",
		"\\projlim", "proj lim",
		"\\sup", "sup",
		"\\underbrace", null,
		"\\overbrace", null,
		"\\underline", null,
		"\\overline", null
	});
		
	private final static Map<String, String> OPTIONAL_ARG_STOP_TOKENS =
		makeMap(new String[]
  {
		"&", null,
		"\\\\", null,
		"}", null,
		"$", null,
		"\\end", null,
		"\\right", null,
		"\\bigr", null,
		"\\Bigr", null,
		"\\biggr", null,
		"\\Biggr", null,
		"\\choose", null,
		"\\over", null,
		"]", null
	});
		
	private final static Map<String, String> HARD_STOP_TOKENS =
		makeMap(new String[]
  {
		"&", null,
		"\\\\", null,
		"}", null,
		"$", null,
		"\\end", null,
		"\\right", null,
		"\\bigr", null,
		"\\Bigr", null,
		"\\biggr", null,
		"\\Biggr", null,
		"\\choose", null,
		"\\over", null
	});
	
	private final static Map<String, String> RIGHT_DELIMITER_STOP_TOKENS =
		makeMap(new String[]
  {
		"&", null,
		"\\\\", null,
		"}", null,
		"$", null,
		"\\end", null,
		"\\right", null,
		"\\bigr", null,
		"\\Bigr", null,
		"\\biggr", null,
		"\\Biggr", null,
		"\\choose", null,
		"\\over", null,
		")", ")",
		"]", "]",
		"\\}", "}",
		"\\rbrace", "}",
		"\\rgroup", ")",
		"\\rvert", "|",
		"\\rVert", "\u2016",
		"\\rceil", "\u2309",
		"\\rfloor", "\u230b",
		"\\rmoustache", "\u23b1",
		"\\rangle", "\u232a"
	});

	private final static Map<String, String> RELATIONS_PRECEDENCE_GROUP = 
		RELATION_SYMBOLS;
	
	private final static Map<String, String> ADDITION_PRECEDENCE_GROUP =
		makeMap(new String[]
  {
		"+", null,
		"-", null,
		"\\oplus", null,
	});
	
	private final static Map<String, String> MULTIPLICATION_PRECEDENCE_GROUP =
		makeMap(new String[]
  {
		"*", null,
		"\\times", null,
		"\\cdot", null,
		"/", null
	});
	
	private final static Map<String, String> CHAR_ESCAPE_CODES =
		makeMap(new String[]
  {
	  "93", "#"
  });

//g_tex_commands = {
	private Map<String, LambdaTokenInput> texCommands = 
		new HashMap<String, LambdaTokenInput>();
	{
//u"\\frac": v_fraction_to_mathml, \
		texCommands.put("\\frac", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fractionToMathml(slf);
			}
		});
//u"\\dsum": lambda slf: v_sizevariant_to_mathml(slf,u"true",u"\\sum"), \
		texCommands.put("\\dsum", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeVariantToMathml(slf, "true", "\\sum");
			}
		});
//u"\\tsum": lambda slf: v_sizevariant_to_mathml(slf,u"false",u"\\sum"), \
		texCommands.put("\\tsum", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeVariantToMathml(slf, "false", "\\sum");
			}
		});
//u"\\dint": lambda slf: v_sizevariant_to_mathml(slf,u"true",u"\\int"), \
		texCommands.put("\\dint", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeVariantToMathml(slf, "true", "\\int");
			}
		});
//u"\\tint": lambda slf: v_sizevariant_to_mathml(slf,u"false",u"\\int"), \
		texCommands.put("\\tint", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeVariantToMathml(slf, "false", "\\int");
			}
		});
//u"\\dbinom": lambda slf: v_sizevariant_to_mathml(slf,u"true",u"\\binom"), \
		texCommands.put("\\dbinom", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeVariantToMathml(slf, "true", "\\binom");
			}
		});
//u"\\tbinom": lambda slf: v_sizevariant_to_mathml(slf,u"false",u"\\binom"), \
		texCommands.put("\\tbinom", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeVariantToMathml(slf, "false", "\\binom");
			}
		});
//u"\\dfrac": lambda slf: result_element(u"mstyle",1, u"displaystyle", u"true", v_fraction_to_mathml(slf)), \
		texCommands.put("\\dfrac", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return resultElement("mstyle",1, "displaystyle", "true", fractionToMathml(slf));
			}
		});
//u"\\tfrac": lambda slf: result_element(u"mstyle",2, u"displaystyle", u"false", u"scriptlevel", u"+1",v_fraction_to_mathml(slf)), \
		texCommands.put("\\tfrac", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return resultElement("mstyle",2, "displaystyle", "false", "scriptlevel", "+1",fractionToMathml(slf));
			}
		});
//u"\\binom": v_binom_to_mathml, \
		texCommands.put("\\binom", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return binomToMathml(slf);
			}
		});
//u"\\sqrt": v_sqrt_to_mathml, \
		texCommands.put("\\sqrt", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sqrtToMathml(slf);
			}
		});
//u"\\operatorname": v_operatorname_to_mathml, \
		texCommands.put("\\operatorname", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return operatorNameToMathml(slf);
			}
		});
//u"\\displaystyle": v_displaystyle_to_mathml, \
		texCommands.put("\\displaystyle", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return displayStyleToMathml(slf);
			}
		});
//u"\\pod": lambda slf: v_parenthesized_operator(slf, None), \
		texCommands.put("\\pod", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return parenthesizedOperator(slf, null);
			}
		});
//u"\\pmod": lambda slf: v_parenthesized_operator(slf, u"mod"), \
		texCommands.put("\\pmod", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return parenthesizedOperator(slf, "mod");
			}
		});
//u"\\boldsymbol": lambda slf: v_font_to_mathml(slf, u"bold"), \
		texCommands.put("\\boldsymbol", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "bold");
			}
		});
//u"\\bold": lambda slf: v_font_to_mathml(slf, u"bold"), \
		texCommands.put("\\bold", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "bold");
			}
		});
//u"\\Bbb": lambda slf: v_font_to_mathml(slf, u"double-struck"), \
		texCommands.put("\\Bbb", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "double-struck");
			}
		});
//u"\\mathbb": lambda slf: v_font_to_mathml(slf, u"double-struck"), \
		texCommands.put("\\mathbb", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "double-struck");
			}
		});
//u"\\mathbbmss": lambda slf: v_font_to_mathml(slf, u"double-struck"), \
		texCommands.put("\\mathbbmss", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "double-struck");
			}
		});
//u"\\mathbf": lambda slf: v_font_to_mathml(slf, u"bold"), \
		texCommands.put("\\mathbf", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "bold");
			}
		});
//u"\\mathop": lambda slf: v_font_to_mathml(slf, u"normal"), \
		texCommands.put("\\mathop", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "normal");
			}
		});
//u"\\mathrm": lambda slf: v_font_to_mathml(slf, u"normal"), \
		texCommands.put("\\mathrm", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "normal");
			}
		});
//u"\\mathfrak": lambda slf: v_font_to_mathml(slf, u"fraktur"), \
		texCommands.put("\\mathfrak", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "fraktur");
			}
		});
//u"\\mathit": lambda slf: v_font_to_mathml(slf, u"italic"), \
		texCommands.put("\\mathit", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "italic");
			}
		});
//u"\\mathscr": lambda slf: v_font_to_mathml(slf, u"script"), \
		texCommands.put("\\mathscr", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "script");
			}
		});
//u"\\mathcal": lambda slf: v_font_to_mathml(slf, u"script"), \
		texCommands.put("\\mathcal", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "script");
			}
		});
//u"\\mathsf": lambda slf: v_font_to_mathml(slf, u"sans-serif"), \
		texCommands.put("\\mathsf", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "sans-serif");
			}
		});
//u"\\mathtt": lambda slf: v_font_to_mathml(slf, u"monospace"), \
		texCommands.put("\\mathtt", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "monospace");
			}
		});
//u"\\EuScript": lambda slf: v_font_to_mathml(slf, u"script"), \
		texCommands.put("\\EuScript", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return fontToMathml(slf, "script");
			}
		});
//u"\\bf": lambda slf: v_old_font_to_mathml(slf, u"bold"), \
		texCommands.put("\\bf", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return oldFontToMathml(slf, "bold");
			}
		});
//u"\\rm": lambda slf: v_old_font_to_mathml(slf, u"normal"), \
		texCommands.put("\\rm", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return oldFontToMathml(slf, "normal");
			}
		});
//u"\\big": lambda slf: v_size_to_mathml(slf, u"2", u"2"), \
		texCommands.put("\\big", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeToMathml(slf, "2", "2");
			}
		});
//u"\\Big": lambda slf: v_size_to_mathml(slf, u"3", u"3"), \
		texCommands.put("\\Big", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeToMathml(slf, "3", "3");
			}
		});
//u"\\bigg": lambda slf: v_size_to_mathml(slf, u"4", u"4"), \
		texCommands.put("\\bigg", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeToMathml(slf, "4", "4");
			}
		});
//u"\\Bigg": lambda slf: v_size_to_mathml(slf, u"5", u"5"), \
		texCommands.put("\\Bigg", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return sizeToMathml(slf, "5", "5");
			}
		});
//u"\\acute": lambda slf: v_accent_to_mathml(slf, u"\u0301"), \
		texCommands.put("\\acute", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0301");
			}
		});
//u"\\grave": lambda slf: v_accent_to_mathml(slf, u"\u0300"), \
		texCommands.put("\\grave", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0300");
			}
		});
//u"\\tilde": lambda slf: v_accent_to_mathml(slf, u"\u0303"), \
		texCommands.put("\\tilde", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0303");
			}
		});
//u"\\bar": lambda slf: v_accent_to_mathml(slf, u"\u0304"), \
		texCommands.put("\\bar", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0304");
			}
		});
//u"\\breve": lambda slf: v_accent_to_mathml(slf, u"\u0306"), \
		texCommands.put("\\breve", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0306");
			}
		});
//u"\\check": lambda slf: v_accent_to_mathml(slf, u"\u030c"), \
		texCommands.put("\\check", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u030c");
			}
		});
//u"\\hat": lambda slf: v_accent_to_mathml(slf, u"\u0302"), \
		texCommands.put("\\hat", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0302");
			}
		});
//u"\\vec": lambda slf: v_accent_to_mathml(slf, u"\u20d7"), \
		texCommands.put("\\vec", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u20d7");
			}
		});
//u"\\dot": lambda slf: v_accent_to_mathml(slf, u"\u0307"), \
		texCommands.put("\\dot", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0307");
			}
		});
//u"\\ddot": lambda slf: v_accent_to_mathml(slf, u"\u0308"), \
		texCommands.put("\\ddot", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u0308");
			}
		});
//u"\\dddot": lambda slf: v_accent_to_mathml(slf, u"\u20db"), \
		texCommands.put("\\dddot", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return accentToMathml(slf, "\u20db");
			}
		});
//u"\\underbrace": lambda slf: v_under_to_mathml(slf, u"\ufe38"), \
		texCommands.put("\\underbrace", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return underToMathml(slf, "\ufe38");
			}
		});
//u"\\overbrace": lambda slf: v_over_to_mathml(slf, u"\ufe37"), \
		texCommands.put("\\overbrace", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return overToMathml(slf, "\ufe37");
			}
		});
//u"\\underline": lambda slf: v_under_to_mathml(slf, u"\u0332"), \
		texCommands.put("\\underline", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return underToMathml(slf, "\u0332");
			}
		});
//u"\\overline": lambda slf: v_over_to_mathml(slf, u"\u00af"), \
		texCommands.put("\\overline", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return overToMathml(slf, "\u00af");
			}
		});
//u"\\widetilde": lambda slf: v_over_to_mathml(slf, u"\u0303"), \
		texCommands.put("\\widetilde", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return overToMathml(slf, "\u0303");
			}
		});
//u"\\widehat": lambda slf: v_over_to_mathml(slf, u"\u0302"), \
		texCommands.put("\\widehat", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return overToMathml(slf, "\u0302");
			}
		});
//u"\\not": lambda slf: v_combining_to_mathml(slf, u"\u0338"), \
		texCommands.put("\\not", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return combiningToMathml(slf, "\u0338");
			}
		});
//u"\\left": lambda slf: v_delimiter_to_mathml(slf, u"\\right", u"1", None), \
		texCommands.put("\\left", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return delimiterToMathml(slf, "\\right", "1", null);
			}
		});
//u"\\bigl": lambda slf: v_delimiter_to_mathml(slf, u"\\bigr", u"2", u"2"), \
		texCommands.put("\\bigl", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return delimiterToMathml(slf, "\\bigr", "2", "2");
			}
		});
//u"\\Bigl": lambda slf: v_delimiter_to_mathml(slf, u"\\Bigr", u"3", u"3"), \
		texCommands.put("\\Bigl", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return delimiterToMathml(slf, "\\Bigr", "3", "3");
			}
		});
//u"\\biggl": lambda slf: v_delimiter_to_mathml(slf, u"\\biggr", u"4", u"4"), \
		texCommands.put("\\biggl", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return delimiterToMathml(slf, "\\biggr", "4", "4");
			}
		});
//u"\\Biggl": lambda slf: v_delimiter_to_mathml(slf, u"\\Biggr", u"5", u"5"), \
		texCommands.put("\\Biggl", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return delimiterToMathml(slf, "\\Biggr", "5", "5");
			}
		});
//u"\\char": v_char_escape_to_mathml, \
		texCommands.put("\\char", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return charEscapeToMathml(slf);
			}
		});
//u"\\!": lambda slf: None, \
		texCommands.put("\\!", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput input)
			{
				return null;
			}
		});
//u"\\text": v_text_to_mathml, \
		texCommands.put("\\text", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\textnormal": v_text_to_mathml, \
		texCommands.put("\\textnormal", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\textrm": v_text_to_mathml, \
		texCommands.put("\\textrm", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\textsl": v_text_to_mathml, \
		texCommands.put("\\textsl", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\textit": v_text_to_mathml, \
		texCommands.put("\\textit", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\texttt": v_text_to_mathml, \
		texCommands.put("\\texttt", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\textbf": v_text_to_mathml, \
		texCommands.put("\\textbf", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\hbox": v_text_to_mathml, \
		texCommands.put("\\hbox", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\mbox": v_text_to_mathml, \
		texCommands.put("\\mbox", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return textToMathml(slf);
			}
		});
//u"\\begin": v_latex_block_to_mathml, \
		texCommands.put("\\begin", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return latexBlockToMathml(slf);
			}
		});
//}
	}
	
//def v_fraction_to_mathml(slf):
//v_numerator = v_piece_to_mathml(slf)
//v_denominator = v_piece_to_mathml(slf)
//return result_element(u"mfrac", 0, v_numerator, v_denominator)
	private Element fractionToMathml(TokenInput slf)
	{
		Element numerator = pieceToMathml(slf);
		Element denominator = pieceToMathml(slf);
		return resultElement("mfrac", 0, numerator, denominator);
	}
	
//def v_binom_to_mathml(slf):
//v_top = v_piece_to_mathml(slf)
//v_bottom = v_piece_to_mathml(slf)
//return result_element(u"mrow", 0, result_element(u"mo", 0, u"("), result_element(u"mfrac", 1, u"linethickness", u"0", v_top, v_bottom), result_element(u"mo", 0, u")"))
	private Element binomToMathml(TokenInput slf)
	{
		Element top = pieceToMathml(slf);
		Element bottom = pieceToMathml(slf);
		return resultElement("mrow", 0, resultElement("mo", 0, "("),
			resultElement("mfrac", 1, "linethickness", "0", top, bottom),
			resultElement("mo", 0, ")"));
	}

//def v_sqrt_to_mathml(slf):
//v_index = v_optional_arg_to_mathml(slf)
//v_object = v_piece_to_mathml(slf)
//if (v_index is not None):
// return result_element(u"mroot", 0, v_object, v_index)
//else:
// return result_element(u"msqrt", 0, v_object)
	private Element sqrtToMathml(TokenInput slf)
	{
		Element index = optionalArgToMathml(slf);
		Element obj = pieceToMathml(slf);
		if(index != null)
		{
			return resultElement("mroot", 0, obj, index);
		}
		else
		{
			return resultElement("msqrt", 0, obj);
		}
	}

//def v_parenthesized_operator(slf, v_word):
//v_object = v_piece_to_mathml(slf)
//if (v_word is not None):
// return result_element(u"mrow", 0, result_element(u"mo", 0, u"("), result_element(u"mo", 0, v_word), v_object, result_element(u"mo", 0, u")"))
//else:
// return result_element(u"mrow", 0, result_element(u"mo", 0, u"("), v_object, result_element(u"mo", 0, u")"))
	private Element parenthesizedOperator(TokenInput slf, String word)
	{
		Element obj = pieceToMathml(slf);
		if(word != null)
		{
			return resultElement("mrow", 0, resultElement("mo", 0, "("),
				resultElement("mo", 0, word), obj, resultElement("mo", 0, ")"));
		}
		else
		{
			return resultElement("mrow", 0, resultElement("mo", 0, "("), obj, 
				resultElement("mo", 0, ")"));
		}
	}

//def v_operatorname_to_mathml(slf):
//v_result = result_element(u"mo", 0, slf.tokens[slf.tokens_index])
//slf.tokens_index += 1
//return v_result
	private Element operatorNameToMathml(TokenInput slf)
	{
		return resultElement("mo", 0, slf.nextToken());
	}

//def v_displaystyle_to_mathml(slf):
//v_result = v_subexpr_chain_to_mathml(slf, g_hard_stop_tokens)
//return result_element(u"mstyle", 2, u"displaystyle", u"true", u"scriptlevel", u"0", v_result)
	private Element displayStyleToMathml(TokenInput slf)
	{
		Element result = subExprChainToMathml(slf, HARD_STOP_TOKENS);
		return resultElement("mstyle", 2, "displaystyle", "true",
			"scriptlevel", "0", result);
	}

//def v_displaymath_to_mathml(slf):
//v_result = v_subexpr_chain_to_mathml(slf, g_hard_stop_tokens)
//v_finish_latex_block(slf)
//return result_element(u"mstyle", 2, u"displaystyle", u"true", u"scriptlevel", u"0", v_result)
	private Element displayMathToMathml(TokenInput slf)
	{
		Element result = subExprChainToMathml(slf, HARD_STOP_TOKENS);
		finishLatexBlock(slf);
		return resultElement("mstyle", 2, "displaystyle", "true",
			"scriptlevel", "0", result);
	}
	
//def v_font_to_mathml(slf, v_font_name):
//if (slf.tokens[slf.tokens_index] != u"{"):
// v_result = result_element(u"mi", 1, u"mathvariant", v_font_name, slf.tokens[slf.tokens_index])
// if (v_font_name == u"normal"):
//  result_set_attr(v_result, u"fontstyle", u"normal")
// slf.tokens_index += 1
// return v_result
//else:
// v_result = v_piece_to_mathml(slf)
// result_set_attr(v_result, u"mathvariant", v_font_name)
// if (v_font_name == u"normal"):
//  result_set_attr(v_result, u"fontstyle", u"normal")
// return v_result
	private Element fontToMathml(TokenInput slf, String fontName)
	{
		Element result;
		if(!"{".equals(slf.peekToken()))
		{
			result = resultElement("mi", 1, "mathvariant", fontName,
				slf.nextToken());
		}
		else
		{
			result = pieceToMathml(slf);
			resultSetAttr(result, "mathvariant", fontName);
		}
		if(fontName.equals("normal"))
		{
			resultSetAttr(result, "fontstyle", "normal");
		}
		return result;
	}
	
//def v_old_font_to_mathml(slf, v_font_name):
//return result_element(u"mstyle", 2, u"mathvariant", v_font_name, u"fontstyle", ((v_font_name == u"normal") and u"normal" or None), v_subexpr_chain_to_mathml(slf, g_hard_stop_tokens))
	private Element oldFontToMathml(TokenInput slf, String fontName)
	{
		return resultElement("mstyle", 2, "mathvariant", fontName,
			"fontstyle", (fontName == "normal") ? "normal" : null,
				subExprChainToMathml(slf, HARD_STOP_TOKENS));
	}

//def v_size_to_mathml(slf, v_min_size, v_max_size):
	private Element sizeToMathml(TokenInput slf, String minSize, String maxSize)
	{
//v_result = v_piece_to_mathml(slf)
//result_set_attr(v_result, u"minsize", v_min_size)
//result_set_attr(v_result, u"maxsize", v_max_size)
//return v_result
		Element result = pieceToMathml(slf);
		resultSetAttr(result, "minsize", minSize);
		resultSetAttr(result, "maxsize", maxSize);
		return result;
	}

//def v_accent_to_mathml(slf, v_char):
//return result_element(u"mover", 1, u"accent", u"true", v_piece_to_mathml(slf), result_element(u"mo", 0, v_char))
	private Element accentToMathml(TokenInput slf, String chr)
	{
		return resultElement("mover", 1, "accent", "true",
			pieceToMathml(slf), resultElement("mo", 0, chr));
	}

//def v_matrix_to_mathml(slf, v_open_delim, v_close_delim):

	Element matrixToMathml(TokenInput slf, String openDelim, String closeDelim)
	{
//# ROBHACK skip OUTeX 'optional' arg if present 
//if (slf.tokens[slf.tokens_index] == u"{"):
//	  slf.tokens_index += 1
//	  br=1
//	  s=""
//	  while (slf.tokens[slf.tokens_index] is not None) and br>0:
//	    c=slf.tokens[slf.tokens_index]
//	    if c==u"{":
//	      br+=1
//	    elif c==u"}":
//	      br-=1
//	    s+=c
//	    slf.tokens_index += 1
//#end of skip optional arg
		skipOuOptional(slf);
		
//v_mtable = v_matrix_to_mtable(slf, result_element(u"mtable", 0))
//if ((v_open_delim is not None) or (v_close_delim is not None)):
// v_mrow = result_element(u"mrow", 0)
// if (v_open_delim is not None):
//	  result_element_append(v_mrow, result_element(u"mo", 0, v_open_delim))
// result_element_append(v_mrow, v_mtable)
// if (v_close_delim is not None):
//	  result_element_append(v_mrow, result_element(u"mo", 0, v_close_delim))
// return v_mrow
//else:
// return v_mtable
		Element mtable = matrixToMtable(slf, resultElement("mtable", 0));
		if(openDelim != null || closeDelim != null)
		{
			Element mrow = resultElement("mrow", 0);
			if(openDelim != null)
			{
				resultElementAppend(mrow, resultElement("mo", 0, openDelim));
			}
			resultElementAppend(mrow, mtable);
			if(closeDelim != null)
			{
				resultElementAppend(mrow, resultElement("mo", 0, closeDelim));
			}
			return mrow;
		}
		else
		{
			return mtable;
		}
	}

	/**
	 * Apparently some TeX constructs have additional optional arguments when
	 * used at the OU. Ew. This function skips them, returning the content.
	 * @param slf Tokens
	 * @return Skipped content, empty string if none
	 */
	private String skipOuOptional(TokenInput slf)
	{
		// ROBHACK skip OUTeX 'optional' arg if present
		if("{".equals(slf.peekToken()))
		{
			slf.nextToken();
			int br = 1;
			StringBuilder out = new StringBuilder();
			while(slf.peekToken() != null && br > 0)
			{
				String c = slf.nextToken();
				if(c.equals("{"))
				{
					br++;
				}
				else if(c.equals("}"))
				{
					br--;
				}
				out.append(c);
			}
			return out.toString();
		}
		return "";
	}
	
//def v_array_to_mathml(slf):
	private Element arrayToMathml(TokenInput slf)
	{
//v_mtable = result_element(u"mtable", 0)
		Element mtable = resultElement("mtable", 0);
		
//# ROBHACK skip OUTeX 'optional' arg if present 
//if (slf.tokens[slf.tokens_index] == u"{"):
// slf.tokens_index += 1
// br=1
// s=""
// while (slf.tokens[slf.tokens_index] is not None) and br>0:
//	   c=slf.tokens[slf.tokens_index]
//	   if c==u"{":
//	     br+=1
//	   elif c==u"}":
//	     br-=1
//	   s+=c
//	   slf.tokens_index += 1
		String s = skipOuOptional(slf);
		
// #loop over characters up to the end delimiter
// for c in s[:-1]:
//	  if c==u"c":
//	   result_append_attr(v_mtable, u"columnalign", u"center ")
//	  elif c==u"l":
//	   result_append_attr(v_mtable, u"columnalign", u"left ")
//	  elif c==u"r":
//	   result_append_attr(v_mtable, u"columnalign", u"right ")
//	  elif c==u"@":#quick fudge to parse @{}
//	   result_append_attr(v_mtable, u"columnspacing", u"0.0em ")
//return v_matrix_to_mtable(slf, v_mtable)
		// Loop over characters up to the end delimiter
		for(int i=0; i<s.length()-1; i++)
		{
			char c = s.charAt(i);
			if(c=='c')
			{
				resultAppendAttr(mtable, "columnalign", "center ");
			}
			else if(c=='l')
			{
				resultAppendAttr(mtable, "columnalign", "left ");
			}
			else if(c=='r')
			{
				resultAppendAttr(mtable, "columnalign", "right ");
			}
			else if(c=='@')
			{
				// quick fudge to parse @{}
				resultAppendAttr(mtable, "columnalign", "0.0em ");
			}
		}
		return matrixToMtable(slf, mtable);
	}
	
//def v_matrix_to_mtable(slf, v_mtable):
	Element matrixToMtable(TokenInput slf, Element mtable)
	{
//v_mtr = result_element(u"mtr", 0)
//v_mtd = result_element(u"mtd", 0)
//v_token = slf.tokens[slf.tokens_index]
//result_element_append(v_mtable, v_mtr)
//result_element_append(v_mtr, v_mtd)
//while ((v_token is not None) and (v_token != u"\\end")):
		Element mtr = resultElement("mtr", 0);
		Element mtd = resultElement("mtd", 0);
		String token = slf.peekToken();
		resultElementAppend(mtable, mtr);
		resultElementAppend(mtr, mtd);
		while(token != null && !token.equals("\\end"))
		{
// if (v_token == u"\\\\"):
//	  v_mtr = result_element(u"mtr", 0)
//	  v_mtd = result_element(u"mtd", 0)
//	  result_element_append(v_mtable, v_mtr)
//	  result_element_append(v_mtr, v_mtd)
//	  slf.tokens_index += 1
//	  #look for (and discard optional argument of \\
//	  v_optional_arg_to_mathml(slf)
			if(token.equals("\\\\"))
			{
				mtr = resultElement("mtr", 0);
				mtd = resultElement("mtd", 0);
				resultElementAppend(mtable, mtr);
				resultElementAppend(mtr, mtd);
				slf.nextToken();
				// look for (and discard) optional argument of \\
				optionalArgToMathml(slf);
			}
// elif v_token == u"&":
//	  v_mtd = result_element(u"mtd", 0)
//	  result_element_append(v_mtr, v_mtd)
//	  slf.tokens_index += 1
			else if(token.equals("&"))
			{
				mtd = resultElement("mtd", 0);
				resultElementAppend(mtr, mtd);
				slf.nextToken();
			}
//   elif v_token in g_hard_stop_tokens:
//    slf.tokens_index += 1			
			else if(HARD_STOP_TOKENS.containsKey(token))
			{
				slf.nextToken();
			}
// else:
//	  result_element_append(v_mtd, v_subexpr_chain_to_mathml(slf, g_hard_stop_tokens))
			else
			{
				resultElementAppend(mtd, subExprChainToMathml(slf, HARD_STOP_TOKENS));
			}
			
// v_token = slf.tokens[slf.tokens_index]
			token = slf.peekToken();
		}
//v_finish_latex_block(slf)
//return v_mtable
		finishLatexBlock(slf);
		return mtable;
	}

//def v_over_to_mathml(slf, v_char):
//return result_element(u"mover", 0, v_piece_to_mathml(slf), result_element(u"mo", 0, v_char))
	Element overToMathml(TokenInput slf, String chr)
	{
		return resultElement("mover", 0, pieceToMathml(slf),
			resultElement("mo", 0, chr));
	}

//def v_under_to_mathml(slf, v_char):
//return result_element(u"munder", 0, v_piece_to_mathml(slf), result_element(u"mo", 0, v_char))
	Element underToMathml(TokenInput slf, String chr)
	{
		return resultElement("munder", 0, pieceToMathml(slf),
			resultElement("mo", 0, chr));
	}

//def v_delimiter_to_mathml(slf, v_end_command, v_min_size, v_max_size):
	Element delimiterToMathml(TokenInput slf, String endCommand, String minSize, String maxSize)
	{
//v_mrow = result_element(u"mrow", 0)
//result_element_append(v_mrow, result_element(u"mo", 2, u"minsize", v_min_size, u"maxsize", v_max_size, v_read_delimiter(slf)))
//result_element_append(v_mrow, v_subexpr_chain_to_mathml(slf, g_hard_stop_tokens))
		Element mrow = resultElement("mrow", 0);
		resultElementAppend(mrow, resultElement("mo", 2, "minsize", minSize,
			"maxsize", maxSize, readDelimiter(slf)));
		resultElementAppend(mrow, subExprChainToMathml(slf, HARD_STOP_TOKENS));
//if (slf.tokens[slf.tokens_index] != v_end_command):
// return v_mrow
		if(slf.peekToken() != null && slf.peekToken().equals(endCommand))
		{
			return mrow;
		}
//slf.tokens_index += 1
//result_element_append(v_mrow, result_element(u"mo", 2, u"minsize", v_min_size, u"maxsize", v_max_size, v_read_delimiter(slf)))
//return v_mrow
		slf.nextToken();
		resultElementAppend(mrow, resultElement("mo", 2, "minsize", minSize,
			"maxsize", maxSize, readDelimiter(slf)));
		return mrow;
	}
	
//def v_read_delimiter(slf):
	Object readDelimiter(TokenInput slf)
	{
//v_token = slf.tokens[slf.tokens_index]
//slf.tokens_index += 1
		String token = slf.peekToken();
		slf.nextToken();
	//if (v_token is None):
	// return result_element(u"merror", 0, result_element(u"mtext", 0, 'Unexpected end of math mode'))
		if(token == null)
		{
			return resultElement("merror", 0, resultElement("mtext", 0,
				"Unexpected end of math node"));
		}
//elif (v_token == u"."):
// return u""
		else if(token.equals("."))
		{
			return "";
		}
//elif (v_token == u"<"):
// return u"\u2329"
		else if(token.equals("<"))
		{
			return "\u2329";
		}
//elif (v_token == u">"):
// return u"\u232a"
		else if(token.equals(">"))
		{
			return "\u232a";
		}
//elif (v_token in g_punct_and_space):
// return g_punct_and_space[v_token]
		else if(PUNCT_AND_SPACE.containsKey(token))
		{
			return PUNCT_AND_SPACE.get(token);
		}
//elif (v_token in g_left_delimiters):
// return g_left_delimiters[v_token]
		else if(LEFT_DELIMITERS.containsKey(token))
		{
			return LEFT_DELIMITERS.get(token);
		}
//elif (v_token in g_right_delimiters):
// return g_right_delimiters[v_token]
		else if(RIGHT_DELIMITERS.containsKey(token))
		{
			return RIGHT_DELIMITERS.get(token);
		}
//elif (v_token in g_operator_symbols):
// return g_operator_symbols[v_token]
		else if(OPERATOR_SYMBOLS.containsKey(token))
		{
			return OPERATOR_SYMBOLS.get(token);
		}
//else:
// return result_element(u"merror", 0, result_element(u"mtext", 0, 'Invalid delimiter: '+v_token) )
		else
		{
			return resultElement("merror", 0, resultElement("mtext", 0,
				"Invalid delimiter: " + token));
		}
	}
	
	/**
	 * Map of TeX 'environment' handling functions.
	 * <p>
	 * Unfortunately Java currently has a rather ugly syntax for closures/lambda 
	 * functions. (And map initialisers, come to that.) Frequently-delayed
	 * improvements, currently planned to Java 8, would allow shorter syntax.
	 * For amusement value, the first lambda function according to the current
	 * draft would be:
	 * #{ slf -> matrixToMathml(slf, "(", ")") }
	 * This is slightly shorter than the Python version and about 7 lines shorter
	 * than current Java.
	 * Collection literals (also delayed and delayed and delayed) would make this
	 * type of code nicer, too.
	 */
//g_tex_environments = {u"smallmatrix": lambda slf: v_matrix_to_mathml(slf, u"(", u")"), \
	private final Map<String, LambdaTokenInput> texEnvironments =
		new HashMap<String, LambdaTokenInput>();
	{
		texEnvironments.put("smallmatrix", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return matrixToMathml(slf, "(", ")");
			}
		});
//u"pmatrix": lambda slf: v_matrix_to_mathml(slf, u"(", u")"), \
		// Duplicate
		texEnvironments.put("pmatrix", texEnvironments.get("smallmatrix"));
//u"bmatrix": lambda slf: v_matrix_to_mathml(slf, u"[", u"]"), \
		texEnvironments.put("bmatrix", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return matrixToMathml(slf, "[", "]");
			}
		});
//u"Bmatrix": lambda slf: v_matrix_to_mathml(slf, u"{", u"}"), \
		texEnvironments.put("Bmatrix", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return matrixToMathml(slf, "{", "}");
			}
		});
//u"vmatrix": lambda slf: v_matrix_to_mathml(slf, u"|", u"|"), \
		texEnvironments.put("vmatrix", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return matrixToMathml(slf, "|", "|");
			}
		});
//u"Vmatrix": lambda slf: v_matrix_to_mathml(slf, u"\u2016", u"\u2016"), \
		texEnvironments.put("Vmatrix", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return matrixToMathml(slf, "\u2016", "\u2016");
			}
		});
//u"cases": lambda slf: v_matrix_to_mathml(slf, u"{", None), \
		texEnvironments.put("cases", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return matrixToMathml(slf, "{", null);
			}
		});
//u"array": v_array_to_mathml, \
		texEnvironments.put("array", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return arrayToMathml(slf);
			}
		});
//u"displaymath": v_displaymath_to_mathml, \
//}
		texEnvironments.put("displaymath", new LambdaTokenInput()
		{
			@Override
			public Element call(TokenInput slf)
			{
				return displayMathToMathml(slf);
			}
		});
	}
	
//def v_latex_block_to_mathml(slf):
	private Element latexBlockToMathml(TokenInput slf)
	{
//v_cmd = slf.tokens[slf.tokens_index]
//if (v_cmd in g_tex_environments):
// slf.tokens_index += 1
// return g_tex_environments[v_cmd](slf)
		String cmd = slf.peekToken();
		if(texEnvironments.containsKey(cmd))
		{
			slf.nextToken();
			return texEnvironments.get(cmd).call(slf);
		}
//else:
// return result_element(u"merror", 0, result_element(u"mtext", 0, 'Invalid command: '+repr(v_cmd)) )
		else
		{
			return resultElement("merror", 0, resultElement("mtext", 0,
				"Invalid command: " + cmd));
		}		
	}
	
//def v_finish_latex_block(slf):
//if (slf.tokens[slf.tokens_index] is None):
// return result_element(u"merror", 0, result_element(u"mtext", 0, 'Unexpected end of math mode'))
//slf.tokens_index += 1
//slf.tokens_index += 1
	private Element finishLatexBlock(TokenInput slf)
	{
		// Note: I rewrote this a bit. Didn't like the way it moved token twice
		// but only checked for end-of-file once.
		for(int i=0; i<2; i++)
		{
			if(slf.peekToken() == null)
			{
				return resultElement("merror", 0, resultElement("mtext", 0,
					"Unexpected end of math node"));
			}
			slf.nextToken();
		}
		return null;
	}
	
//def v_combining_to_mathml(slf, v_char):
//v_base = slf.tokens[slf.tokens_index]
//slf.tokens_index += 1
//return result_element(u"mo", 0, v_base, v_char)
	private Element combiningToMathml(TokenInput slf, String chr)
	{
		String base = slf.nextToken();
		return resultElement("mo", 0, base, chr);
	}
	
//def v_char_escape_to_mathml(slf):
//v_result = None
//if (slf.tokens[slf.tokens_index] in g_char_escape_codes):
// v_result = result_element(u"mtext", 0, g_char_escape_codes[slf.tokens[slf.tokens_index]])
//else:
// v_result = result_element(u"merror", 0, u"\\char", slf.tokens[slf.tokens_index])
//slf.tokens_index += 1
//return v_result
	private Element charEscapeToMathml(TokenInput slf)
	{
		String token = slf.nextToken();
		if(CHAR_ESCAPE_CODES.containsKey(token))
		{
			return resultElement("mtext", 0, CHAR_ESCAPE_CODES.get(token));
		}
		else
		{
			return resultElement("merror", 0, "\\char", token);
		}
	}

//def v_text_to_mathml(slf):
	private Element textToMathml(TokenInput slf)
	{
//if (slf.tokens[slf.tokens_index] != u"{"):
// v_result = result_element(u"mtext", 0, slf.tokens[slf.tokens_index])
// slf.tokens_index += 1
// return v_result
//braces=0
//slf.tokens_index += 1
		String token = slf.nextToken();
		if(!token.equals("{"))
		{
			return resultElement("mtext", 0, token);
		}
		int braces = 0;
//v_result = None
//v_mrow = None
//v_node = None
		Element result = null, mrow = null, node = null;
//while (slf.tokens[slf.tokens_index] is not None):
		while(slf.peekToken() != null)
		{
			token = slf.nextToken();
// if (slf.tokens[slf.tokens_index] == u"{"):
//  slf.tokens_index += 1
//  braces+=1
			if(token.equals("{"))
			{
				braces++;
			}
// elif (slf.tokens[slf.tokens_index] == u"}"):
//  slf.tokens_index += 1
//  if braces==0:
//    return v_result
//  braces-=1
			else if(token.equals("}"))
			{
				if(braces == 0)
				{
					return result;
				}
				braces--;
			}
// elif (slf.tokens[slf.tokens_index] == u"$"):
//  slf.tokens_index += 1
//  v_node = v_subexpr_chain_to_mathml(slf, g_hard_stop_tokens)
//  slf.tokens_index += 1
			else if(token.equals("$"))
			{
				node = subExprChainToMathml(slf, HARD_STOP_TOKENS);
				slf.nextToken();
			}
// else:
//  v_node = result_element(u"mtext", 0, slf.tokens[slf.tokens_index])
//  slf.tokens_index += 1
			else
			{
				node = resultElement("mtext", 0, token);
			}
			
// if (v_mrow is not None):
//  result_element_append(v_mrow, v_node)
// elif (v_result is not None):
//  v_mrow = result_element(u"mrow", 0, v_result, v_node)
//  v_result = v_mrow
// else:
//  v_result = v_node
//return v_result
			if(mrow != null)
			{
				resultElementAppend(mrow, node);
			}
			else if(result != null)
			{
				mrow = resultElement("mrow", 0, result, node);
				result = mrow;
			}
			else
			{
				result = node;
			}
		}
			
		return result;
	}

//def v_sizevariant_to_mathml(slf,displaystyle,parent):
//"for implementing \dsum \tsum from \sum and similar"
//#pointer has moved on to after dsum, rewind and interpret as a sum
//slf.tokens_index -= 1
//slf.tokens[slf.tokens_index]=parent
//return result_element(u"mstyle",1, u"displaystyle", displaystyle, v_subexpr_to_mathml(slf))
	private Element sizeVariantToMathml(TokenInput slf, String displayStyle, String parent)
	{
		// for implementing \dsum \tsum from \sum and similar
		// pointer has moved on to after dsum, rewind and interpret as a sum
		// EWWWWW. We are seriously going to write into the token buffer? Oh well.
		// I guess we are!
		slf.backAndOverwriteToken(parent);
		return resultElement("mstyle", 1, "displaystyle", displayStyle,
			subExprToMathml(slf));
	}

//def v_piece_to_mathml(slf):
	private Element pieceToMathml(TokenInput slf)
	{
//v_token = slf.tokens[slf.tokens_index]
//v_result = None
		String token = slf.peekToken();
		Element result;
//if (v_token == u"{"):
// slf.tokens_index += 1
// v_result = v_subexpr_chain_to_mathml(slf, g_hard_stop_tokens)
// if (slf.tokens[slf.tokens_index] == u"}"):
//  slf.tokens_index += 1
		if("{".equals(token))
		{
			slf.nextToken();
			result = subExprChainToMathml(slf, HARD_STOP_TOKENS);
			if("}".equals(slf.peekToken()))
			{
				slf.nextToken();
			}
		}
//elif (v_token in g_relation_symbols):
// v_result = result_element(u"mo", 0, g_relation_symbols[v_token])
// slf.tokens_index += 1
		else if(RELATION_SYMBOLS.containsKey(token))
		{
			result = resultElement("mo", 0, RELATION_SYMBOLS.get(token));
			slf.nextToken();
		}
//elif (v_token in g_operator_symbols):
// v_result = result_element(u"mo", 0, g_operator_symbols[v_token])
// slf.tokens_index += 1
		else if(OPERATOR_SYMBOLS.containsKey(token))
		{
			result = resultElement("mo", 0, OPERATOR_SYMBOLS.get(token));
			slf.nextToken();
		}
//elif (v_token in g_left_delimiters):
// v_result = result_element(u"mo", 0, g_left_delimiters[v_token])
// slf.tokens_index += 1
		else if(LEFT_DELIMITERS.containsKey(token))
		{
			result = resultElement("mo", 0, LEFT_DELIMITERS.get(token));
			slf.nextToken();
		}
//elif (v_token in g_right_delimiters):
// v_result = result_element(u"mo", 0, g_right_delimiters[v_token])
// slf.tokens_index += 1
		else if(RIGHT_DELIMITERS.containsKey(token))
		{
			result = resultElement("mo", 0, RIGHT_DELIMITERS.get(token));
			slf.nextToken();
		}
//elif (v_token in g_word_operators):
// v_result = result_element(u"mi", 1, u"mathvariant", u"normal", g_word_operators[v_token])
// slf.tokens_index += 1
		else if(WORD_OPERATORS.containsKey(token))
		{
			result = resultElement("mi", 1, "mathvariant", "normal", WORD_OPERATORS.get(token));
			slf.nextToken();
		}
//elif (v_token in g_greek_letters):
// v_result = result_element(u"mi", 1, u"fontstyle", u"normal", g_greek_letters[v_token])
// slf.tokens_index += 1
		else if(GREEK_LETTERS.containsKey(token))
		{
			result = resultElement("mi", 1, "fontstyle", "normal", GREEK_LETTERS.get(token));
			slf.nextToken();
		}
//elif (v_token in g_named_identifiers):
// v_result = result_element(u"mi", 0, g_named_identifiers[v_token])
// slf.tokens_index += 1
		else if(NAMED_IDENTIFIERS.containsKey(token))
		{
			result = resultElement("mi", 0, NAMED_IDENTIFIERS.get(token));
			slf.nextToken();
		}
//elif (v_token in g_punct_and_space):
// v_result = result_element(u"mtext", 0, g_punct_and_space[v_token])
// slf.tokens_index += 1
		else if(PUNCT_AND_SPACE.containsKey(token))
		{
			result = resultElement("mtext", 0, PUNCT_AND_SPACE.get(token));
			slf.nextToken();
		}
//elif (v_token in g_tex_commands):
// slf.tokens_index += 1
// v_result = g_tex_commands[v_token](slf)
		else if(texCommands.containsKey(token))
		{
			slf.nextToken();
			result = texCommands.get(token).call(slf);
		}
//else:
// v_result = result_element(u"mn", 0, v_token)
// slf.tokens_index += 1
		else
		{
			result = resultElement("mn", 0, token);
			slf.nextToken();
		}
//return v_result
		return result;
	}

//def v_subexpr_to_mathml(slf):
	Element subExprToMathml(TokenInput slf)
	{
// v_result = None
// v_mmultiscripts = None
// v_mprescripts = None
		Element result = null, mmultiscripts = null, mprescripts = null;
		
		// It seems that Python doesn't have a ternary operator. 
		// GOOD response to that fact:
		// 'Well, ternary operators are kind of hard to read anyway. I'll just
		// go ahead and write out a full-length if statement, assigning the results
		// to a temporary variables for use in my expression.'
		// BAD response to that fact:
// if (((len(slf.tokens) > slf.tokens_index+0 and slf.tokens[slf.tokens_index+0] or None) == u"{") and ((len(slf.tokens) > slf.tokens_index+1 and slf.tokens[slf.tokens_index+1] or None) == u"}") and (((len(slf.tokens) > slf.tokens_index+2 and slf.tokens[slf.tokens_index+2] or None) == u"_") or ((len(slf.tokens) > slf.tokens_index+2 and slf.tokens[slf.tokens_index+2] or None) == u"^"))):

		// I have attempted to split this out into Python with similar semantics
		// before I convert it to Java. There is a slight difference but it seems
		// to me nobody cares if I run v_part3 when I didn't need to.
		// v_part1 = len(slf.tokens) > slf.tokens_index+0 and slf.tokens[slf.tokens_index+0] or None
		// v_part2 = len(slf.tokens) > slf.tokens_index+1 and slf.tokens[slf.tokens_index+1] or None
		// v_part3 = len(slf.tokens) > slf.tokens_index+2 and slf.tokens[slf.tokens_index+2] or None
		// if ((v_part1 == u"{") 
		// and (v_part2 == u"}")
		// and ((v_part3 == u"_") or (v_part3 == u"^"))):
		// Oh look! Now it almost makes sense. Let's turn it into Java.		
		if("{".equals(slf.peekToken(0)) && "}".equals(slf.peekToken(1)) &&
			("_".equals(slf.peekToken(2)) || "^".equals(slf.peekToken(2))))
		{
			
//  v_mmultiscripts = result_element(u"mmultiscripts", 0)
//  v_mprescripts = result_element(u"mprescripts", 0)
//  result_element_append(v_mmultiscripts, v_mprescripts)
			mmultiscripts = resultElement("mmultiscripts", 0);
			mprescripts = resultElement("mprescripts", 0);
			resultElementAppend(mmultiscripts, mprescripts);
			
			// I wrapped this monster and found out it's the same as the last one...
//  while (((len(slf.tokens) > slf.tokens_index+0 and slf.tokens[slf.tokens_index+0] or None) == u"{") and ((len(slf.tokens) > slf.tokens_index+1 and slf.tokens[slf.tokens_index+1] or None) == u"}") and (((len(slf.tokens) > slf.tokens_index+2 and slf.tokens[slf.tokens_index+2] or None) == u"_") or ((len(slf.tokens) > slf.tokens_index+2 and slf.tokens[slf.tokens_index+2] or None) == u"^"))):
			while("{".equals(slf.peekToken(0)) && "}".equals(slf.peekToken(1)) &&
				("_".equals(slf.peekToken(2)) || "^".equals(slf.peekToken(2))))
			{
				
//   v_subscript = None
//   v_superscript = None
				Element subScript = null, superScript = null;
			
//   slf.tokens_index += 1
//   slf.tokens_index += 1
				slf.nextToken();
				slf.nextToken();
				
//   if (slf.tokens[slf.tokens_index] == u"_"):
//    slf.tokens_index += 1
//    v_subscript = v_piece_to_mathml(slf)
				if("_".equals(slf.peekToken()))
				{
					slf.nextToken();
					subScript = pieceToMathml(slf);
				}
//   elif (slf.tokens[slf.tokens_index] == u"^"):
//    slf.tokens_index += 1
//    v_superscript = v_piece_to_mathml(slf)
				else if("^".equals(slf.peekToken()))
				{
					slf.nextToken();
					superScript = pieceToMathml(slf);
				}
//   if (slf.tokens[slf.tokens_index] == u"_"):
//    slf.tokens_index += 1
//    v_subscript = v_piece_to_mathml(slf)
				if("_".equals(slf.peekToken()))
				{
					slf.nextToken();
					subScript = pieceToMathml(slf);
				}
//   elif (slf.tokens[slf.tokens_index] == u"^"):
//    slf.tokens_index += 1
//    v_superscript = v_piece_to_mathml(slf)
				else if("^".equals(slf.peekToken()))
				{
					slf.nextToken();
					superScript = pieceToMathml(slf);
				}
				
				// Note: The above code kind of looks like it won't work if you have
				// two superscripts or two subscripts in a row in this syntax. Hopefully
				// that is disallowed.
					
//   result_element_append(v_mmultiscripts, ((v_subscript is not None) and v_subscript or result_element(u"none", 0)))
				resultElementAppend(mmultiscripts, subScript != null ? subScript : resultElement("none", 0));
//   result_element_append(v_mmultiscripts, ((v_superscript is not None) and v_superscript or result_element(u"none", 0)))
				resultElementAppend(mmultiscripts, superScript != null ? superScript : resultElement("none", 0));
			}
		}
		
// v_limit_style = (slf.tokens[slf.tokens_index] in g_limit_commands)
		boolean limitStyle = LIMIT_COMMANDS.containsKey(slf.peekToken());
		
// if (slf.tokens[slf.tokens_index] is None):
		if(slf.peekToken() == null)
		{
//  if (v_mmultiscripts is not None):
//   result_element_prepend(v_mmultiscripts, result_element(u"mrow", 0), v_mprescripts)
//   return v_mmultiscripts
			if (mmultiscripts != null)
			{
				resultElementPrepend(mmultiscripts, resultElement("mrow", 0), mprescripts);
				return mmultiscripts;
			}
//  else:
//   return result_element(u"mrow", 0)
			else
			{
				return resultElement("mrow", 0);
			}
		}
// elif (slf.tokens[slf.tokens_index] in g_left_delimiters):
//  v_result = v_heuristic_subexpression(slf)
		else if(LEFT_DELIMITERS.containsKey(slf.peekToken()))
		{
			result = heuristicSubExpression(slf);
		}
// else:
//  v_result = v_piece_to_mathml(slf)
		else
		{
			result = pieceToMathml(slf);
		}
		
// v_base = v_result
// v_subscript = None
// v_superscript = None
		Element base = result, subScript = null, superScript = null;
		
// if (slf.tokens[slf.tokens_index] == u"_"):
//  slf.tokens_index += 1
//  v_subscript = v_piece_to_mathml(slf)
		if("_".equals(slf.peekToken()))
		{
			slf.nextToken();
			subScript = pieceToMathml(slf);
		}
// elif (slf.tokens[slf.tokens_index] == u"^"):
//  slf.tokens_index += 1
//  v_superscript = v_piece_to_mathml(slf)
		else if("^".equals(slf.peekToken()))
		{
			slf.nextToken();
			superScript = pieceToMathml(slf);
		}
		
// if (slf.tokens[slf.tokens_index] == u"_"):
//  slf.tokens_index += 1
//  v_subscript = v_piece_to_mathml(slf)
		if("_".equals(slf.peekToken()))
		{
			slf.nextToken();
			subScript = pieceToMathml(slf);
		}
// elif (slf.tokens[slf.tokens_index] == u"^"):
//  slf.tokens_index += 1
//  v_superscript = v_piece_to_mathml(slf)
		else if("^".equals(slf.peekToken()))
		{
			slf.nextToken();
			superScript = pieceToMathml(slf);
		}
		
// if (v_mmultiscripts is not None):
//  result_element_prepend(v_mmultiscripts, v_base, v_mprescripts)
//  result_element_prepend(v_mmultiscripts, ((v_subscript is not None) and v_subscript or result_element(u"none", 0)), v_mprescripts)
//  result_element_prepend(v_mmultiscripts, ((v_superscript is not None) and v_superscript or result_element(u"none", 0)), v_mprescripts)
		if(mmultiscripts != null)
		{
			resultElementPrepend(mmultiscripts, base, mprescripts);
			resultElementPrepend(mmultiscripts, subScript != null ? subScript : resultElement("none", 0), mprescripts);
			resultElementPrepend(mmultiscripts, superScript != null ? superScript : resultElement("none", 0), mprescripts);
		}

		// ARGH THIS AGAIN (it's the same expression, I checked)
// while (((len(slf.tokens) > slf.tokens_index+0 and slf.tokens[slf.tokens_index+0] or None) == u"{") and ((len(slf.tokens) > slf.tokens_index+1 and slf.tokens[slf.tokens_index+1] or None) == u"}") and (((len(slf.tokens) > slf.tokens_index+2 and slf.tokens[slf.tokens_index+2] or None) == u"_") or ((len(slf.tokens) > slf.tokens_index+2 and slf.tokens[slf.tokens_index+2] or None) == u"^"))):
		while("{".equals(slf.peekToken(0)) && "}".equals(slf.peekToken(1)) &&
			("_".equals(slf.peekToken(2)) || "^".equals(slf.peekToken(2))))
		{
		
//  if (v_mmultiscripts is None):
//   v_mmultiscripts = result_element(u"mmultiscripts", 0, v_base)
//   v_mprescripts = None
//   if ((v_superscript is not None) or (v_subscript is not None)):
//    result_element_append(v_mmultiscripts, ((v_subscript is not None) and v_subscript or result_element(u"none", 0)))
//    result_element_append(v_mmultiscripts, ((v_superscript is not None) and v_superscript or result_element(u"none", 0)))
			if(mmultiscripts==null)
			{
				mmultiscripts = resultElement("mmultiscripts", 0, base);
				mprescripts = null;
				if((superScript != null) || (subScript != null))
				{
					resultElementAppend(mmultiscripts, subScript!=null ? subScript : resultElement("none", 0));
					resultElementAppend(mmultiscripts, superScript!=null ? superScript : resultElement("none", 0));
				}
			}

//  v_subscript = None
//  v_superscript = None
//  slf.tokens_index += 1
//  slf.tokens_index += 1
			subScript = null;
			superScript = null;
			slf.nextToken();
			slf.nextToken();
			
//  if (slf.tokens[slf.tokens_index] == u"_"):
//   slf.tokens_index += 1
//   v_subscript = v_piece_to_mathml(slf)
			if("_".equals(slf.peekToken()))
			{
				slf.nextToken();
				subScript = pieceToMathml(slf);
			}
//  elif (slf.tokens[slf.tokens_index] == u"^"):
//   slf.tokens_index += 1
//   v_superscript = v_piece_to_mathml(slf)
			else if("^".equals(slf.peekToken()))
			{
				slf.nextToken();
				superScript = pieceToMathml(slf);
			}
			
//  if (slf.tokens[slf.tokens_index] == u"_"):
//   slf.tokens_index += 1
//   v_subscript = v_piece_to_mathml(slf)
			if("_".equals(slf.peekToken()))
			{
				slf.nextToken();
				subScript = pieceToMathml(slf);
			}
//  elif (slf.tokens[slf.tokens_index] == u"^"):
//   slf.tokens_index += 1
//   v_superscript = v_piece_to_mathml(slf)
			else if("^".equals(slf.peekToken()))
			{
				slf.nextToken();
				superScript = pieceToMathml(slf);
			}
			
//  result_element_prepend(v_mmultiscripts, ((v_subscript is not None) and v_subscript or result_element(u"none", 0)), v_mprescripts)
//  result_element_prepend(v_mmultiscripts, ((v_superscript is not None) and v_superscript or result_element(u"none", 0)), v_mprescripts)
			resultElementPrepend(mmultiscripts, 
				subScript != null ? subScript : resultElement("none", 0), mprescripts);
			resultElementPrepend(mmultiscripts,
				superScript != null ? superScript : resultElement("none", 0), mprescripts);
		}
			
// if (v_mmultiscripts is not None):
//  v_result = v_mmultiscripts
		if(mmultiscripts != null)
		{
			result = mmultiscripts;
		}
// elif ((v_subscript is not None) and (v_superscript is not None)):
//  v_result = result_element((v_limit_style and u"munderover" or u"msubsup"), 0, v_base, v_subscript, v_superscript)
		else if(subScript != null && superScript != null)
		{
			result = resultElement(limitStyle ? "munderover" : "msubsup", 0,
				base, subScript, superScript);
		}
// elif (v_subscript is not None):
//  v_result = result_element((v_limit_style and u"munder" or u"msub"), 0, v_base, v_subscript)
		else if(subScript != null)
		{
			result = resultElement(limitStyle ? "munder" : "msub", 0,
				base, subScript);
		}
// elif (v_superscript is not None):
//  v_result = result_element((v_limit_style and u"mover" or u"msup"), 0, v_base, v_superscript)
		else if(superScript != null)
		{
			result = resultElement(limitStyle ? "mover" : "msup", 0, 
				base, superScript);
		}
// return v_result
			return result;
	}

//def v_subexpr_chain_to_mathml(slf, v_stop_tokens):
	Element subExprChainToMathml(TokenInput slf, Map<String, String> stopTokens)
	{
// v_result = None
// v_mrow = None
// v_mfrac = None
// v_wrapped_result = None
		Element result = null, mrow = null, mfrac = null, wrappedResult = null;
// while ((slf.tokens[slf.tokens_index] is not None) and not ((slf.tokens[slf.tokens_index] in v_stop_tokens))):
		while(slf.peekToken() != null && !stopTokens.containsKey(slf.peekToken()))
		{
//  if (slf.tokens[slf.tokens_index] == u"\\over"):
//   slf.tokens_index += 1
//   v_mfrac = result_element(u"mfrac", 0, v_result)
//   v_wrapped_result = v_mfrac
//   v_mrow = None
//   v_result = None
			if("\\over".equals(slf.peekToken()))
			{
				slf.nextToken();
				mfrac = resultElement("mfrac", 0, result); // Um, this might be null
				wrappedResult = mfrac;
				mrow = null;
				result = null;
			}
//  elif (slf.tokens[slf.tokens_index] == u"\\choose"):
//   slf.tokens_index += 1
//   v_mfrac = result_element(u"mfrac", 1, u"linethickness", u"0", v_result)
//   v_wrapped_result = result_element(u"mrow", 0, result_element(u"mo", 0, u"("), v_mfrac, result_element(u"mo", 0, u")"))
//   v_mrow = None
//   v_result = None
			else if("\\choose".equals(slf.peekToken()))
			{
				slf.nextToken();
				mfrac = resultElement("mfrac", 1, "linethickness", "0", result);
				wrappedResult = resultElement("mrow", 0,
					resultElement("mo", 0, "{"), mfrac, resultElement("mo", 0, "}"));
				mrow = null;
				result = null;
			}
//  else:
			else
			{
				// Another horrific line
//   v_node = v_collect_precedence_group(slf, g_relations_precedence_group, v_stop_tokens, lambda slf, v_stop_tokens: v_collect_precedence_group(slf, g_addition_precedence_group, v_stop_tokens, lambda slf, v_stop_tokens: v_collect_precedence_group(slf, g_multiplication_precedence_group, v_stop_tokens, v_collect_invisible_group)))
				
				// Here it is wrapped 
				//   v_node = v_collect_precedence_group(slf, g_relations_precedence_group, v_stop_tokens, 
				//	   lambda slf, v_stop_tokens: 
				//       v_collect_precedence_group(slf, g_addition_precedence_group, v_stop_tokens, 
				//         lambda slf, v_stop_tokens:
				//				   v_collect_precedence_group(slf, g_multiplication_precedence_group, 
				//				     v_stop_tokens, v_collect_invisible_group)))
				
				// Oh so that's what it's trying to do!
				Element node = collectPrecedenceGroup(slf, RELATIONS_PRECEDENCE_GROUP,
					stopTokens, new Lambda2()
					{
						@Override
						public Element call(TokenInput slf, Map<String, String> stopTokens)
						{
							return collectPrecedenceGroup(slf, ADDITION_PRECEDENCE_GROUP,
								stopTokens, new Lambda2()
							{
								@Override
								public Element call(TokenInput slf, Map<String, String> stopTokens)
								{
									return collectPrecedenceGroup(slf, MULTIPLICATION_PRECEDENCE_GROUP,
										stopTokens, new Lambda2()
									{
										@Override
										public Element call(TokenInput slf, Map<String, String> stopTokens)
										{
											return collectInvisibleGroup(slf, stopTokens);
										}
									});
								}
							});
						}
					});
//   if (v_mrow is not None):
//    result_element_append(v_mrow, v_node)
				if(mrow != null)
				{
					resultElementAppend(mrow, node);
				}
//   elif (v_result is not None):
//    v_mrow = result_element(u"mrow", 0, v_result, v_node)
//    v_result = v_mrow
				else if(result != null)
				{
					mrow = resultElement("mrow", 0, result, node);
					result = mrow;
				}
//   else:
//    v_result = v_node
				else
				{
					result = node;
				}
			}
		}
// if (v_mfrac is not None):
//  result_element_append(v_mfrac, v_result)
//  return v_wrapped_result
		if(mfrac != null)
		{
			resultElementAppend(mfrac, result);
			return wrappedResult;
		}
// else:
//  return v_result
		else
		{
			return result;
		}
	}

//def v_optional_arg_to_mathml(slf):
	Element optionalArgToMathml(TokenInput slf)
	{
// if (slf.tokens[slf.tokens_index] != u"["):
//  return None
		if(!"[".equals(slf.peekToken()))
		{
			return null;
		}
// slf.tokens_index += 1
// v_result = v_subexpr_chain_to_mathml(slf, g_optional_arg_stop_tokens)
		slf.nextToken();
		Element result = subExprChainToMathml(slf, OPTIONAL_ARG_STOP_TOKENS);
		
// if (slf.tokens[slf.tokens_index] == u"]"):
//  slf.tokens_index += 1
		if("]".equals(slf.peekToken()))
		{
			slf.nextToken();
		}
		
// return v_result
		return result;
	}
	
//def v_heuristic_subexpression(slf):
	Element heuristicSubExpression(TokenInput slf)
	{
// v_result = result_element(u"mrow", 0)
// result_element_append(v_result, v_piece_to_mathml(slf))
// result_element_append(v_result, v_subexpr_chain_to_mathml(slf, g_right_delimiter_stop_tokens))
		Element result = resultElement("mrow", 0);
		resultElementAppend(result, pieceToMathml(slf));
		resultElementAppend(result, subExprChainToMathml(slf, 
			RIGHT_DELIMITER_STOP_TOKENS));
		
// if ((slf.tokens[slf.tokens_index] is not None) and not ((slf.tokens[slf.tokens_index] in g_hard_stop_tokens))):
//  result_element_append(v_result, v_piece_to_mathml(slf))
		if(slf.peekToken() != null && !HARD_STOP_TOKENS.containsKey(slf.peekToken()))
		{
			resultElementAppend(result, pieceToMathml(slf));
		}
		
// return v_result
		return result;
	}

//def v_collect_precedence_group(slf, v_operators, v_stop_tokens, v_reader):
	Element collectPrecedenceGroup(TokenInput slf, Map<String, String> operators,
		Map<String, String> stopTokens, Lambda2 reader)
	{
// v_result = v_reader(slf, v_stop_tokens)
// v_mrow = None
		Element result = reader.call(slf, stopTokens);
		Element mrow = null;
		
// while ((slf.tokens[slf.tokens_index] is not None) and not ((slf.tokens[slf.tokens_index] in v_stop_tokens)) and (slf.tokens[slf.tokens_index] in v_operators)):
		while(slf.peekToken() != null && !stopTokens.containsKey(slf.peekToken())
			&& operators.containsKey(slf.peekToken()))
		{
//  if (v_mrow is None):
//   v_mrow = result_element(u"mrow", 0, v_result)
//   v_result = v_mrow
			if(mrow == null)
			{
				mrow = resultElement("mrow", 0, result);
				result = mrow;
			}
//  result_element_append(v_mrow, v_piece_to_mathml(slf))
			resultElementAppend(mrow, pieceToMathml(slf));
//  if ((slf.tokens[slf.tokens_index] is not None) and (slf.tokens[slf.tokens_index] in v_stop_tokens)):
//   return v_result
			if(slf.peekToken() != null && stopTokens.containsKey(slf.peekToken()))
			{
				return result;
			}
//  else:
//   result_element_append(v_mrow, v_reader(slf, v_stop_tokens))
			else
			{
				resultElementAppend(mrow, reader.call(slf, stopTokens));
			}
		}
// return v_result
		return result;
	}

//def v_collect_invisible_group(slf, v_stop_tokens):
	Element collectInvisibleGroup(TokenInput slf, Map<String, String> stopTokens)
	{
// v_result = v_subexpr_to_mathml(slf)
// v_mrow = None
		Element result = subExprToMathml(slf);
		Element mrow = null;
		
// while ((slf.tokens[slf.tokens_index] is not None) and not ((slf.tokens[slf.tokens_index] in v_stop_tokens)) and ((slf.tokens[slf.tokens_index] in g_named_identifiers) or (slf.tokens[slf.tokens_index] in g_left_delimiters))):
		while(slf.peekToken() != null && !stopTokens.containsKey(slf.peekToken()) &&
			(NAMED_IDENTIFIERS.containsKey(slf.peekToken()) || LEFT_DELIMITERS.containsKey(slf.peekToken())))
		{
//  if (v_mrow is None):
//   v_mrow = result_element(u"mrow", 0, v_result)
//   v_result = v_mrow
			if(mrow == null)
			{
				mrow = resultElement("mrow", 0, result);
				result = mrow;
			}
			
//  #The following line was an attempt to guess the semantics of the expression and insert "invisible times"
//  #removed to simplify output
//  #result_element_append(v_mrow, result_element(u"mo", 0, u"\u2062"))
			
//  if ((slf.tokens[slf.tokens_index] is not None) and (slf.tokens[slf.tokens_index] in v_stop_tokens)):
//   return v_result
			if(slf.peekToken() != null && stopTokens.containsKey(slf.peekToken()))
			{
				return result;
			}
//  else:
//   result_element_append(v_mrow, v_subexpr_to_mathml(slf))
			else
			{
				resultElementAppend(mrow, subExprToMathml(slf));
			}
		}
// return v_result
		return result;
	}
	
	/**
	 * Converts an entire equation into MathML.
	 * <p>
	 * This method wasn't in the Python version.
	 * @param tokens Tokens
	 * @return MathML result element (always a &lt;math&gt; tag)
	 */
	public Element convert(TokenInput tokens)
	{
		Element root = subExprChainToMathml(tokens, new HashMap<String, String>());
		Element math = resultElement("math", 0);
		// If the root is an mrow with no attributes, chuck it away and use the <math>
		// instead to make the result shorter
		if(root.getTagName().equals("mrow") 
			&& root.getAttributes().getLength() == 0)
		{
			while(root.getFirstChild() != null)
			{
				Node child = root.getFirstChild();
				root.removeChild(child);
				math.appendChild(child);
			}
		}
		else
		{
			math.appendChild(root);
		}
		return math;
	}
}	
