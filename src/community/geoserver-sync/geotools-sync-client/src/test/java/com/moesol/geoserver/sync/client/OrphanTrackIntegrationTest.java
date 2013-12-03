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




import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.geoserver.wfs.WFSTestSupport;
import org.geotools.feature.FeatureIterator;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.xml.sax.SAXException;

import com.moesol.geoserver.sync.client.GeoserverClientSynchronizer;
import com.moesol.geoserver.sync.client.xml.ComplexConfiguration;
import com.moesol.geoserver.sync.client.xml.ComplexFeatureCollection;
import com.moesol.geoserver.sync.core.FeatureSha1;
import com.moesol.geoserver.sync.core.Sha1Value;

import static org.junit.Assert.*;

public class OrphanTrackIntegrationTest extends WFSTestSupport {
	
	private static final String POST_TEMPLATE = "<wfs:GetFeature " 
        + "service=\"WFS\" " 
        + "version=\"1.1.0\" "
        + "outputFormat=\"${outputFormat}\" "
        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + ">\n"
        +  "<wfs:Query typeName=\"cite:Buildings\"> "
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
	private FeatureSha1 m_engine = new FeatureSha1();

	@Test
	public void testParseWfs() throws IOException, SAXException, ParserConfigurationException {
		m_engine.parseAttributesToInclude("-all");
		GeoserverClientSynchronizer clientSynchronizer = new GeoserverClientSynchronizer(makeConfiguration(), "", POST_TEMPLATE);
		InputStream is = loadResource("orphantracks.xml");
		ComplexFeatureCollection features = (ComplexFeatureCollection) clientSynchronizer.parseWfs(is);
		assertEquals(2, features.size());
		FeatureIterator<?> it = features.features();
		try {
			int i = 0;
			while (it.hasNext()) {
				Feature feature = it.next();
				Sha1Value sha1One = m_engine.computeValueSha1(feature);
				switch (i) {
				case 0:
					assertEquals("eef5926af1da677907eb3efbeaf18c086571b5d7", sha1One.toString());
					break;
				case 1:
					assertEquals("570aaf1496af083b6fcbd25012ab4eb9b70b26fe", sha1One.toString());
					break;
				}
				i++;
			}
		} finally {
			it.close();
		}
	}

	private ComplexConfiguration makeConfiguration() {
		URL xsdUrl = getClass().getResource("orphantracks.xsd");
		ComplexConfiguration configuration = new ComplexConfiguration("http://www.forwardslope.com/c2rpc", xsdUrl.toString());
		return configuration;
	}

	private InputStream loadResource(String name) throws FileNotFoundException {
		InputStream is = getClass().getResourceAsStream(name);
		if (is == null) {
			throw new FileNotFoundException(name);
		}
		return is;
	}
	
}
