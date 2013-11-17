/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.samples;




import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class Samples {
	private final SimpleFeatureType m_flagType;

	public SimpleFeatureType getFlagType() {
		return m_flagType;
	}

	public Samples() {
		m_flagType = buildType();
	}
	
	public SampleBuilder builder() {
		return new SampleBuilder(this);
	}

	public static SimpleFeatureType buildType() {
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		
		b.setName("Flag");
	
		//set the name
		b.setName( "Flag" );
	
		//add some properties
		b.add( "name", String.class );
		b.add( "classification", Integer.class );
		b.add( "height", Double.class );
	
		//add a geometry property
		b.setCRS( DefaultGeographicCRS.WGS84); // set crs first
		b.add( "location", Point.class ); // then add geometry
	
		return b.buildFeatureType();
	}

	public static Point makePoint(String wellKnownText) throws ParseException {
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
	
		WKTReader reader = new WKTReader( geometryFactory );
		Point point = (Point) reader.read(wellKnownText);
		return point;
	}

	public Feature buildSimpleFeature(String fid) throws ParseException {
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(m_flagType);
		builder.add("US");
		builder.add(1);
		builder.add(20.5);
		builder.add(Samples.makePoint("POINT(1 2)"));
		return builder.buildFeature("fid-0001");
	}

	public Feature buildSimpleFeature(String fid, int classification) throws ParseException {
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(m_flagType);
		builder.add("US");
		builder.add(classification);
		builder.add(20.5);
		builder.add(Samples.makePoint("POINT(1 2)"));
		return builder.buildFeature(fid);
	}
}
