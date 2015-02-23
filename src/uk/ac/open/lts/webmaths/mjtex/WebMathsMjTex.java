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
package uk.ac.open.lts.webmaths.mjtex;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.WebServiceContext;

import org.w3c.dom.Document;

import uk.ac.open.lts.webmaths.*;
import uk.ac.open.lts.webmaths.tex.*;

@WebService(endpointInterface="uk.ac.open.lts.webmaths.tex.MathsTexPort",
	targetNamespace="http://ns.open.ac.uk/lts/vle/filter_maths/",
	serviceName="MathsTex", portName="MathsTexPort")
public class WebMathsMjTex extends WebMathsService implements MathsTexPort
{
	@Resource
	private WebServiceContext context;

	@Override
	public MathsTexReturn getMathml(MathsTexParams params)
	{
		// Set up default return values
		MathsTexReturn result = new MathsTexReturn();
		result.setOk(false);
		result.setError("");
		result.setMathml("");


		return result;
	}

	@Override
	public GetTexReturn getTex(GetTexParams params)
	{
		// Set up default return values
		GetTexReturn result = new GetTexReturn();
		result.setOk(false);
		result.setError("");
		result.setTex("");


		return result;
	}
}
