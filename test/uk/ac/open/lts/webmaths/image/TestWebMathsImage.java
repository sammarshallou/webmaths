package uk.ac.open.lts.webmaths.image;

import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.ls.*;

import static org.junit.Assert.*;

public class TestWebMathsImage
{
	private WebMathsImage image;

	@Before
	public void setUp() throws Exception
	{
		image = new WebMathsImage();
	}

	@Test
	public void testAmpThing() throws Exception
	{
		// At one point I got a weird xml error to do with &amp; - but it looks like
		// it probably isn't the Java code causing this.
		MathsImageParams params = new MathsImageParams();
		params.mathml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><semantics><mstyle displaystyle=\"true\"><mstyle displaystyle=\"true\"><mtable columnalign=\"right left \"><mtr><mtd><mi>A</mi></mtd><mtd><mrow><mo>=</mo><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi><mo>?</mo><mi>B</mi></mrow></mtd></mtr><mtr><mtd/><mtd><mrow><mo>=</mo><mo>?</mo><mi>C</mi></mrow></mtd></mtr></mtable></mstyle><annotation encoding=\"text\">\\begin{array}{rl} A &amp; = BBBBBBBBBBBBBBBBBBBBBBBBBBBBB \\\\ &amp; = C \\end{array}</annotation></mstyle></semantics></math>";
		params.rgb = "#000000";
		params.size = 1.0f;
		assertEquals("", image.getImage(params).error);
	}
	
	@Test
	public void testEmptyEquation() throws Exception
	{
		// Equations with no meaningful content gave error on rendering when it
		// tried to make a zero-size image.
		MathsImageParams params = new MathsImageParams();
		params.mathml = "<math xmlns=\"http://www.w3.org/1998/Math/MathML\"></math>";
		params.rgb = "#000000";
		params.size = 1.0f;
		assertEquals("", image.getImage(params).error);
	}

	@Test
	public void testPreprocessForJEuclid() throws Exception
	{
		Document doc = image.parseMathml("<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><semantics><mstyle displaystyle=\"false\"><mstyle displaystyle=\"true\" scriptlevel=\"0\"><munder><mrow><mi>x</mi><mo>+</mo><mn>1</mn></mrow><mo>&#xFE38;</mo></munder></mstyle></mstyle><annotation encoding=\"application/x-tex\">\\displaystyle \\underbrace{x+1}</annotation></semantics></math>");
		WebMathsImage.preprocessForJEuclid(doc);
		DOMImplementationLS domImplLS = (DOMImplementationLS)doc.getImplementation();
		LSSerializer serializer = domImplLS.createLSSerializer();
		String after = serializer.writeToString(doc);
		assertFalse(after.contains("\ufe38"));
	}
}
