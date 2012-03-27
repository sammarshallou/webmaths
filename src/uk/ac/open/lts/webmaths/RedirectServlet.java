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

Copyright 2012 The Open University
*/
package uk.ac.open.lts.webmaths;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.*;

/**
 * This servlet is used only to redirect requests for the root to one of the
 * service pages (so it's easier to see if the service is up and running).
 */
public class RedirectServlet extends HttpServlet
{
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		// Redirect to JAX-WS page for TeX service (doesn't matter which one,
		// they all show the same)
		StringBuffer url = req.getRequestURL();
		url.append("tex");
		resp.sendRedirect(url.toString());
	}
}
