package uk.ac.open.lts.webmaths;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.xml.transform.stream.StreamSource;

import net.sourceforge.jeuclid.*;
import net.sourceforge.jeuclid.context.*;
import net.sourceforge.jeuclid.elements.generic.DocumentElement;
import net.sourceforge.jeuclid.layout.JEuclidView;
import net.sourceforge.jeuclid.parser.*;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.w3c.dom.Document;

public class Test extends JFrame
{
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		byte[] data;
		
		// Create PNG data
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(getImage(), "png", out);
		data = out.toByteArray();

		// Read PNG data
		ByteArrayInputStream input = new ByteArrayInputStream(data);
		Image image = ImageIO.read(input);
		ImageIcon icon = new ImageIcon(image);
		
		// Create display
		new Test(icon);
	}
	
	private static RenderedImage getImage() throws Exception
	{
		Reader reader = new InputStreamReader(Test.class.getResourceAsStream(
			"test.mathml"), "UTF-8");		
		StreamSource source = new StreamSource(reader);
		
		Parser parser = Parser.getInstance();
		Document doc = parser.parseStreamSource(source);
		DocumentElement document = DOMBuilder.getInstance().createJeuclidDom(doc);

		BufferedImage test = new BufferedImage(300,200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = test.createGraphics();
		
		LayoutContextImpl context = new LayoutContextImpl(LayoutContextImpl.getDefaultLayoutContext());
		context.setParameter(Parameter.ANTIALIAS, Boolean.TRUE);
		context.setParameter(Parameter.MATHSIZE, 24f);
		context.setParameter(Parameter.MATHCOLOR, Color.RED);
//		context.setParameter(Parameter., arg1)
		
		JEuclidView view = new JEuclidView(document, context, g2);
		float ascent = view.getAscentHeight();
		float descent = view.getDescentHeight();
		float width = view.getWidth();
		
		test = new BufferedImage((int)Math.ceil(width),
			(int)Math.ceil(ascent + descent), BufferedImage.TYPE_INT_ARGB);
		view.draw(test.createGraphics(), 0, ascent);
		
		return test;
	}
	
	public Test(ImageIcon icon)
	{
		super("MathML web test");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setBackground(Color.YELLOW);
		
		getContentPane().add(new JLabel(icon));
		
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
