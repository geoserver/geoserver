/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.gml2;

import java.net.URI;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.xsd.ElementInstance;
import org.geotools.xsd.Node;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Subclass of {@link GMLBoxTypeBinding} that parses srsName and can inherit the CRS from the
 * containing elements
 *
 * @author Andrea Aime
 */
public class GMLBoxTypeBinding extends org.geotools.gml2.bindings.GMLBoxTypeBinding {

    CoordinateReferenceSystem crs;

    public void setCRS(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        Envelope envelope = (Envelope) super.parse(instance, node, value);

        // handle the box CRS
        CoordinateReferenceSystem crs = this.crs;
        if (node.hasAttribute("srsName")) {
            URI srs = (URI) node.getAttributeValue("srsName");
            crs = CRS.decode(srs.toString());
        }

        if (crs != null) {
            return new ReferencedEnvelope(envelope, crs);
        } else {
            return envelope;
        }
    }
}
