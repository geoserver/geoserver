/** 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Arne Kepp / OpenGeo
 */
package org.geowebcache.layer.wms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.ows.Dispatcher;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileResponseReceiver;
import org.geowebcache.mime.XMLMime;
import org.geowebcache.util.ServletUtils;
import org.springframework.util.Assert;

public class WMSGeoServerHelper extends WMSSourceHelper {

	private static Logger log = Logging.getLogger(WMSGeoServerHelper.class
			.toString());

	Dispatcher gsDispatcher;

	public WMSGeoServerHelper(Dispatcher gsDispatcher) {
		this.gsDispatcher = gsDispatcher;
	}

	@Override
	protected void makeRequest(TileResponseReceiver tileRespRecv,
			WMSLayer layer, Map<String, String> wmsParams,
			String expectedMimeType, Resource target)
			throws GeoWebCacheException {

		// work around GWC setting the wrong regionation params
		if (tileRespRecv instanceof ConveyorTile
				&& ((ConveyorTile) tileRespRecv).getMimeType() == XMLMime.kml) {
			wmsParams.put("format_options=", "regionateBy:auto");
		}

		HttpServletRequest actualRequest = null;
		HttpServletResponse actualResponse = null;
		Cookie[] cookies = null;

		if (tileRespRecv instanceof Conveyor) {
			actualRequest = ((Conveyor) tileRespRecv).servletReq;
			actualResponse = ((Conveyor) tileRespRecv).servletResp;
			cookies = actualRequest.getCookies();
		}

		FakeHttpServletRequest req = new FakeHttpServletRequest(wmsParams,
				cookies);
		FakeHttpServletResponse resp = new FakeHttpServletResponse();

		try {
			gsDispatcher.handleRequest(req, resp);
		} catch (Exception e) {
			log.fine(e.getMessage());

			throw new GeoWebCacheException(
					"Problem communicating with GeoServer" + e.getMessage());
		}

		if (actualResponse != null) {
			cookies = resp.getCachedCookies();
			for (Cookie c : cookies) {
				actualResponse.addCookie(c);
			}
		}

		if (super.mimeStringCheck(expectedMimeType, resp.getContentType())) {
			int responseCode = resp.getResponseCode();
			tileRespRecv.setStatus(responseCode);
			if (responseCode == 200) {
				byte[] bytes = resp.getBytes();
				try {
					target.transferFrom(Channels
							.newChannel(new ByteArrayInputStream(bytes)));
				} catch (IOException e) {
					throw new GeoWebCacheException(e);
				}
				Assert.isTrue(target.getSize() == bytes.length);
			} else if (responseCode == 204) {
				return;
			} else {
				throw new GeoWebCacheException(
						"Unexpected response from GeoServer for request "
								+ wmsParams + ", got response code "
								+ responseCode);
			}
		} else {
			log.severe("Unexpected response from GeoServer for request: "
					+ wmsParams);

			throw new GeoWebCacheException(
					"Unexpected response from GeoServer for request "
							+ wmsParams);
		}
	}

}
