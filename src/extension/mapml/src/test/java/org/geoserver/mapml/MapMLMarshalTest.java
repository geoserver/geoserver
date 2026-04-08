/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import org.geoserver.mapml.xml.GeometryContent;
import org.geoserver.mapml.xml.ObjectFactory;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

public class MapMLMarshalTest {

    @Test
    public void testMixedContent() throws Exception {
        Polygon square =
                (org.locationtech.jts.geom.Polygon) new WKTReader().read("POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))");
        Envelope envelope = new Envelope(5, 15, 5, 15);
        MapMLGeometryClipper clipper = new MapMLGeometryClipper(square, envelope);
        Geometry clipped = clipper.clipAndTag();

        MapMLGenerator generator = new MapMLGenerator();
        GeometryContent geometry = generator.buildGeometry(clipped);

        // Marshal using JAXB with MapMLEncoder.Wrapper to handle namespaces correctly
        jakarta.xml.bind.JAXBContext context = jakarta.xml.bind.JAXBContext.newInstance(GeometryContent.class);
        jakarta.xml.bind.Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(jakarta.xml.bind.Marshaller.JAXB_FRAGMENT, true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        javax.xml.stream.XMLOutputFactory factory = javax.xml.stream.XMLOutputFactory.newInstance();
        MapMLEncoder.Wrapper wrapper = new MapMLEncoder.Wrapper(factory.createXMLStreamWriter(output));
        // Wrap in JAXBElement since GeometryContent doesn't have @XmlRootElement
        marshaller.marshal(new ObjectFactory().createGeometry(geometry), wrapper);
        wrapper.flush();
        String result = output.toString("UTF-8");

        assertThat(
                result,
                Matchers.containsString(
                        "<map-coordinates><map-span class=\"bbox\">5 5 10 5</map-span> 10 5 10 10 5 10 <map-span class=\"bbox\">5 10 5 5</map-span></map-coordinates>"));
    }
}
