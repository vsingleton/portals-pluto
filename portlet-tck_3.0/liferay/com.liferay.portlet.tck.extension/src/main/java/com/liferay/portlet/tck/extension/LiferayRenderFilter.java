/**
 * Copyright (c) 2000-2017 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package com.liferay.portlet.tck.extension;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.annotations.PortletLifecycleFilter;
import javax.portlet.filter.FilterChain;
import javax.portlet.filter.FilterConfig;
import javax.portlet.filter.RenderFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutTypePortlet;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.WebKeys;


/**
 * @author  Neil Griffin
 * @author  Vernon Singleton
 */
@PortletLifecycleFilter(portletNames = "*")
public class LiferayRenderFilter implements RenderFilter {

	private static final Logger _log = LoggerFactory.getLogger(LiferayRenderFilter.class);

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(RenderRequest renderRequest, RenderResponse renderResponse, FilterChain filterChain)
		throws IOException, PortletException {

		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);

		if (themeDisplay.isStateExclusive()) {

			PrintWriter writer = renderResponse.getWriter();
			writer.write("<ul>");

			Layout layout = themeDisplay.getLayout();

			if (layout.isRootLayout()) {

				try {

					String pageUrl = PortalUtil.getLayoutFriendlyURL(layout, themeDisplay);

					String pageName = pageUrl.substring(pageUrl.lastIndexOf("/") + 1);

					writer.write("<li>");
					writer.write("<a href=\"" + pageUrl + "\">" + pageName+ "</a>");
					writer.write("</li>");

					LayoutTypePortlet layoutTypePortlet = (LayoutTypePortlet) layout.getLayoutType();
					List<Portlet> portlets = layoutTypePortlet.getPortlets();

					for (Portlet p : portlets) {

						String portletId = p.getPortletId();

						StringBuilder sb = new StringBuilder(pageUrl);
						sb.append("?p_p_id=");
						sb.append(portletId);
						sb.append("&p_p_state=exclusive");

						String removedWarName = portletId.substring(0, portletId.lastIndexOf("_"));
						String underscoredClass = removedWarName.substring(0, removedWarName.lastIndexOf("_"));

						writer.write("<li>");
						writer.write("<a href=\"" + sb.toString() + "\">" + underscoredClass + "</a>");
						writer.write("</li>");
					}

				}
				catch (Exception e) {
					System.err.println("doFilter: e.getMessage() = " + e.getMessage());
				}
			}

			writer.write("</ul>");
		}

		filterChain.doFilter(renderRequest, renderResponse);
	}

	@Override
	public void init(FilterConfig filterConfig) throws PortletException {
	}
}
