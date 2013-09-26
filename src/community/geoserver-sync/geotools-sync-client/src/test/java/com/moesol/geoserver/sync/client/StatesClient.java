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

package com.moesol.geoserver.sync.client;




import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.identity.Identifier;
import org.xml.sax.SAXException;

import com.moesol.geoserver.sync.client.FeatureAccessor;
import com.moesol.geoserver.sync.client.FeatureAccessorImpl;
import com.moesol.geoserver.sync.client.GeoserverClientSynchronizer;
import com.moesol.geoserver.sync.client.xml.ComplexConfiguration;

/**
 * Reconcile with the states feature type that
 * is a sample feature type in geoserver
 * @author hastings
 */
public class StatesClient {
	private static final Logger LOGGER = Logging.getLogger(StatesClient.class.getName());
	private Map<Identifier, FeatureAccessor> m_features = new HashMap<Identifier, FeatureAccessor>();
	private GeoserverClientSynchronizer synchronizer;
	private static final String POST_TEMPLATE = "<wfs:GetFeature " 
        + "service=\"WFS\" " 
        + "version=\"1.1.0\" "
        + "outputFormat=\"${outputFormat}\" "
        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + ">\n"
        +  "<wfs:Query typeName=\"topp:states\"> "
        +   "<ogc:Filter>"
        +    "<ogc:PropertyIsEqualTo> "
        +      "<ogc:Function name=\"sha1Sync\"> "
        +       "<ogc:Literal>-all</ogc:Literal> "
        +       "<ogc:Literal>${sha1Sync}</ogc:Literal> "
        +      "</ogc:Function> "
        +      "<ogc:Literal>true</ogc:Literal> "
        +    "</ogc:PropertyIsEqualTo> "
        +   "</ogc:Filter> "
        +  "</wfs:Query> " 
        + "</wfs:GetFeature>";
	
	public static void main(String args[]) {
		StatesClient client = new StatesClient();
		client.setUp();
		try {
			client.run();
		} finally {
			client.tearDown();
		}
	}

	private void setUp() {
		LOGGER.log(Level.CONFIG, "template {0}", POST_TEMPLATE);
		
		String namespace = "http://www.openplans.org/topp";
		String schemaLocation = "http://localhost/geoserver/ows?service=WFS&version=1.0.0&request=DescribeFeatureType&typeName=topp:states&maxFeatures=50";
		ComplexConfiguration configuration = new ComplexConfiguration(namespace, schemaLocation);

		String wfsUrl = "http://localhost:80/geoserver/wfs";
		synchronizer = new GeoserverClientSynchronizer(configuration, wfsUrl, POST_TEMPLATE);
	}

	private void tearDown() {
	}

	private void run() {
		try {
			while (true) {
				System.out.println("BEGIN PASS");
				onePass();
			}
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "failed", e);
		}
	}

	private void onePass() throws IOException, SAXException, ParserConfigurationException {
		synchronizer.synchronize(m_features);
		dumpFids();
		
		deleteSomeFids();
		
		synchronizer.synchronize(m_features);
		dumpFids();
		
		changeSomeFids();
		
		synchronizer.synchronize(m_features);
		dumpFids();
		
		addSomeExtraStates();

		synchronizer.synchronize(m_features);
		dumpFids();
		
		m_features.clear();
		synchronizer.synchronize(m_features);
		dumpFids();
		
		// Check when same
		synchronizer.synchronize(m_features);
	}

	private void addSomeExtraStates() {
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName( "states" );
		typeBuilder.add( "STATE_NAME", String.class );
		typeBuilder.add( "FIPS", Integer.class );
		SimpleFeatureType type = typeBuilder.buildFeatureType();

		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
		builder.add("Hawaii");
		builder.add(51);
		SimpleFeature feature2 = builder.buildFeature("states.5051");

		builder.add("Alaska");
		builder.add(52);
		SimpleFeature feature3 = builder.buildFeature("states.5052");
		
		m_features.put(feature2.getIdentifier(), new FeatureAccessorImpl(feature2));
		m_features.put(feature3.getIdentifier(), new FeatureAccessorImpl(feature3));
	}

	private void changeSomeFids() {
		for (int i = 0; i < 10; i++) {
			int f = i+1;
			FeatureAccessor accessor = m_features.get(new FeatureIdImpl("states." + f)); 
			Feature feature = accessor.getFeature();
			feature.getProperty("STATE_NAME").setValue("Changed." + i);
		}
	}

	private void deleteSomeFids() {
		for (int i = 0; i < 10; i++) {
			int f = i+1;
			m_features.remove("states." + f);
		}
	}

	private void dumpFids() {
		int i = 1;
		for (Identifier fid : m_features.keySet()) {
			FeatureAccessor accessor = m_features.get(fid);
			Feature feature = accessor.getFeature();
			System.out.printf("%d: fid: %s name: %s%n", 
					i, fid, feature.getProperty("STATE_NAME").getValue());
			i++;
		}
	}
}
