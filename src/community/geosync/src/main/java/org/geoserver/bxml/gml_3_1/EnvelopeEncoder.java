package org.geoserver.bxml.gml_3_1;

import java.io.IOException;

import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.geometry.BoundingBox;

public class EnvelopeEncoder extends AbstractGMLEncoder<BoundingBox> {

    @Override
    public void encode(final BoundingBox envelope, final BxmlStreamWriter w) throws IOException {

        getGmlEncoder().encodeEnvelope(w, envelope);
    }

}
