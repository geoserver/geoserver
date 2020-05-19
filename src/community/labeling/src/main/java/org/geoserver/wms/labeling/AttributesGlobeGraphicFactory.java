/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.labeling;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;

/**
 * {@link ExternalGraphicFactory} implementation made for rendering the attributes globe label
 * graphic on WMS output images. Tracks the {@code geoserver/label} format.
 */
public class AttributesGlobeGraphicFactory implements ExternalGraphicFactory {

    public static final String GEOSERVER_LABEL = "geoserver/label";

    @Override
    public Icon getIcon(Feature feature, Expression url, String format, int size) throws Exception {
        if (!GEOSERVER_LABEL.equalsIgnoreCase(format)) return null;
        if (feature != null && !(feature instanceof SimpleFeature)) return null;
        // if (feature == null) return prototypeIcon(url);
        SimpleFeature sf = (SimpleFeature) feature;
        // build and return the generated image
        AttributesGlobeGraphicProcessor processor = new AttributesGlobeGraphicProcessor(url, sf);
        return new ImageIcon(processor.buildImage());
    }
}
