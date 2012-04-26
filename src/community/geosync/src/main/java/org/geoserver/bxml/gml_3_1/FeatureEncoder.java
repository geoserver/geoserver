package org.geoserver.bxml.gml_3_1;

import java.io.IOException;

import org.gvsig.bxml.geoserver.SimpleFeatureEncoder;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

public class FeatureEncoder<F extends Feature> extends AbstractGMLEncoder<F> {

    @Override
    public void encode(final F feature, final BxmlStreamWriter w) throws IOException {
        if (!(feature instanceof SimpleFeature)) {
            throw new UnsupportedOperationException(
                    "Feature encoding other than SimpleFeature is not yet supported");
        }

        SimpleFeatureEncoder simpleFeatureEncoder = new SimpleFeatureEncoder(getGmlEncoder());
        simpleFeatureEncoder.encode((SimpleFeature) feature, w);
    }

}
