/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 *           (c) 2008 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.CRIFImpl;

/**
 * Slightly modified version of the existing Color inversion operation
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * 
 * @source $URL$
 */
public class ColorIndexerCRIF extends CRIFImpl {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        final RenderedImage image = (RenderedImage) pb.getSource(0);
        final ColorIndexer indeder = (ColorIndexer) pb.getObjectParameter(0);
        return new ColorIndexerOpImage(image, indeder, hints);
    }

}
