package org.geoserver.bxml.gml_3_1;

import java.io.IOException;

import org.gvsig.bxml.geoserver.Gml3Encoder;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class GeometryEncoder extends AbstractGMLEncoder<Geometry> {

    @Override
    public void encode(final Geometry geometry, final BxmlStreamWriter w) throws IOException {
        Gml3Encoder gml3Encoder = super.getGmlEncoder();
        CoordinateReferenceSystem crs = super.guessCRS(geometry);

        gml3Encoder.encodeGeometry(w, crs, geometry);
    }

}
