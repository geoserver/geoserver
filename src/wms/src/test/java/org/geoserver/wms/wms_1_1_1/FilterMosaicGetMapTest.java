/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.geoserver.wms.WMSFilterMosaicTestSupport;

/**
 * Class to test ImageMosaic cql filter
 * @see {@link WMSFilterMosaicTestSupport}
 * 
 * @author carlo cancellieri
 *
 */
public class FilterMosaicGetMapTest extends WMSFilterMosaicTestSupport {

	final static String BASE_URL = "wms?service=WMS&version=1.1.0"
			+ "&request=GetMap&layers=watertemp&styles="
			+ "&bbox=0.237,40.562,14.593,44.558&width=200&height=80"
			+ "&srs=EPSG:4326&format=image/png";
	final static String MIME = "image/png";

	public void testElevationAsCQL() throws Exception {
		
		// specifying default filter
		String cql_filter = "elevation=100";
		super.setupMosaicFilter(cql_filter);

		BufferedImage image = getAsImage(BASE_URL, "image/png");

		// at this elevation the pixel is black
		assertPixel(image, 36, 31, new Color(0, 0, 0));
		// and this one a light blue, but slightly darker than before
		assertPixel(image, 68, 72, new Color(240, 240, 255));
		
		image = getAsImage(BASE_URL+ "&cql_filter=INCLUDE", "image/png");
	}
	
	public void testQueryTimeElevationAsCQL() throws Exception {
		
		// specifying default filter
		String cql_filter = "elevation=100 AND ingestion=\'2008-10-31T00:00:00.000Z\'";
		super.setupMosaicFilter(cql_filter);
		
		/*
		 * result:
		Filter = "[[ elevation = 100 ] AND [ ingestion = 2008-10-31T00:00:00.000Z ]]"
		*/
		BufferedImage image = getAsImage(BASE_URL, "image/png");

		// at this elevation the pixel is black
		assertPixel(image, 36, 31, new Color(0, 0, 0));
		// and this one a light blue, but slightly darker than before
		assertPixel(image, 68, 72, new Color(240, 240, 255));
		
		/*
		 * override default filter
		 * result:
		Filter = "[[ elevation = 0 ] AND [ ingestion = 2008-11-01T00:00:00.000Z ]]"
		*/
		cql_filter = "elevation=100 AND ingestion=\'2008-11-01T00:00:00.000Z\'";
		BufferedImage image2 = getAsImage(BASE_URL + "&cql_filter=" + cql_filter,
				"image/png");
		
		assertPixel(image2, 36, 31, new Color(0, 0, 0));
		assertPixel(image2, 68, 72, new Color(246, 246, 255));
		
	}


}
