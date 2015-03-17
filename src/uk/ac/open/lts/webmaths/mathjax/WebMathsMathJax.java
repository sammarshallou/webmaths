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

Copyright 2015 The Open University
*/
package uk.ac.open.lts.webmaths.mathjax;

import java.io.IOException;
import java.util.EnumSet;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import static uk.ac.open.lts.webmaths.mathjax.ConversionType.*;

import uk.ac.open.lts.webmaths.WebMathsService;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.mathjax.MathsMathJaxPort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsMathJax", portName="MathsMathJaxPort")
public class WebMathsMathJax extends WebMathsService implements MathsMathJaxPort
{
	@Resource
	private WebServiceContext context;

	@Override
	public ConvertEquationsReturn convertEquations(ConvertEquationsParams params)
	{
		ConvertEquationsReturn result = new ConvertEquationsReturn();
		final MathJax mathJax = MathJax.get(context);

		// Get parameters.
		final EnumSet<ConversionType> types = EnumSet.noneOf(ConversionType.class);
		types.addAll(params.getOutputs());
		final float exSize = params.getExSize();
		final String rgb = params.getRgb();
		final SourceEquation[] equations = params.getEquations().toArray(
			new SourceEquation[params.getEquations().size()]);

		// Process all the input equations.
		final OutputData[] allOut = new OutputData[equations.length];
		final RuntimeException[] weirdError = new RuntimeException[1];
		final Object synch = new Object();
		for(int i = 0; i < equations.length; i++)
		{
			final int currentIndex = i;
			mathJax.executeOnThreadPool(new Runnable()
			{
				public void run()
				{
					try
					{
						OutputData out = processEquation(mathJax, types, exSize, rgb, equations[currentIndex]);
						synchronized(synch)
						{
							allOut[currentIndex]= out;
							synch.notifyAll();
						}
					}
					catch(RuntimeException e)
					{
						synchronized(synch)
						{
							weirdError[0] = e;
							synch.notifyAll();
						}
					}
				}
			});
		}

		// Wait until all the tasks finish or there is a weird error.
		while(true)
		{
			synchronized(synch)
			{
				if(weirdError[0] != null)
				{
					throw weirdError[0];
				}
				int left = allOut.length;
				for(int i = 0; i < allOut.length; i++)
				{
					if(allOut[i] != null)
					{
						left--;
					}
				}
				if(left == 0)
				{
					break;
				}
			}
		}

		// Add everything to the result and return it.
		for(OutputData out : allOut)
		{
			result.getOutput().add(out);
		}
		return result;
	}

	/**
	 * Processes a single equation.
	 * @param mathJax MathJax object
	 * @param types Required types
	 * @param exSize Ex size
	 * @param rgb RGB string
	 * @param equation Equation to convert
	 * @return Output data for this equation
	 */
	private OutputData processEquation(MathJax mathJax,
		EnumSet<ConversionType> types, float exSize, String rgb,
		SourceEquation equation)
	{
		OutputData out = new OutputData();
		out.setOk(false);

		InputEquation eq = InputEquation.getFromSourceEquation(equation);

		try
		{
			// We need the pixel SVG for lots of things.
			String pixelSvg = null, exSvg = null;
			if(types.contains(SVG_PX) || types.contains(PNG) ||
				types.contains(PNG_BASELINE) || types.contains(SVG_PX_BASELINE) ||
				types.contains(TEXT))
			{
				pixelSvg = mathJax.getSvg(eq, true, exSize, rgb);
			}
			if(types.contains(SVG_EX) || types.contains(SVG_EX_BASELINE))
			{
				exSvg = mathJax.getSvg(eq, true, MathJax.SIZE_IN_EX, rgb);
			}

			// If SVG was turned on, store it.
			if(types.contains(SVG_EX))
			{
				out.setSvg(exSvg);
			}
			else if(types.contains(SVG_PX))
			{
				out.setSvg(pixelSvg);
			}

			if(types.contains(PNG))
			{
				out.setPng(mathJax.getPngFromSvg(pixelSvg));
			}

			if(types.contains(TEXT))
			{
				out.setText(mathJax.getEnglishFromSvg(pixelSvg));
			}

			if(types.contains(SVG_PX_BASELINE))
			{
				out.setSvgPxBaseline((float)mathJax.getPxBaselineFromSvg(pixelSvg));
			}
			if(types.contains(SVG_EX_BASELINE))
			{
				out.setSvgExBaseline((float)mathJax.getExBaselineFromSvg(exSvg));
			}
			if(types.contains(PNG_BASELINE))
			{
				out.setPngBaseline((float)mathJax.getPxBaselineFromSvg(
					MathJax.offsetSvg(pixelSvg, MathJax.PNG_OFFSET)));
			}

			if(types.contains(MATHML) && eq instanceof InputTexEquation)
			{
				out.setMathml(mathJax.getMathml((InputTexEquation)eq));
			}

			if(types.contains(EPS))
			{
				out.setEps(mathJax.getEps(eq, exSize, rgb));
			}

			out.setOk(true);
		}
		catch(MathJaxException e)
		{
			out.setError("MathJax failure: " + e.getMessage());
		}
		catch(IOException e)
		{
			out.setError("Unexpected error: " + e.getMessage());
		}
		return out;
	}
}
