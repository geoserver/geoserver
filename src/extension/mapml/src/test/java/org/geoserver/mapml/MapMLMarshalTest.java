/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.eclipse.persistence.jaxb.JAXBContextProperties.NAMESPACE_PREFIX_MAPPER;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.mapml.xml.GeometryContent;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class MapMLMarshalTest {

    private static Jaxb2Marshaller marshaller;

    @Before
    public void setupMarshallers() {
        marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("org.geoserver.mapml.xml");
        Map<String, Object> properties = new HashMap<>();
        properties.put(javax.xml.bind.Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        properties.put(NAMESPACE_PREFIX_MAPPER, Map.of("http://www.w3.org/1999/xhtml", ""));
        marshaller.setMarshallerProperties(properties);
    }

    @Test
    public void testMixedContent() throws ParseException, IOException {
        Polygon square =
                (org.locationtech.jts.geom.Polygon)
                        new WKTReader().read("POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))");
        Envelope envelope = new Envelope(5, 15, 5, 15);
        MapMLGeometryClipper clipper = new MapMLGeometryClipper(square, envelope);
        Geometry clipped = clipper.clipAndTag();

        MapMLGenerator generator = new MapMLGenerator();
        GeometryContent geometry = generator.buildGeometry(clipped);

        StringWriter writer = new StringWriter();
        marshaller.marshal(geometry, new StreamResult(writer));
        assertThat(
                writer.toString(),
                Matchers.containsString(
                        "<map-coordinates><map-span class=\"bbox\">5 5 10 5</map-span> 10 5 10 10 5 10 <map-span class=\"bbox\">5 10 5 5</map-span></map-coordinates>"));
    }
}
