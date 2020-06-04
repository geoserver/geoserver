/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical.op;

import com.sun.media.jai.opimage.RIFUtil;
import it.geosolutions.jaiext.range.Range;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.CRIFImpl;
import javax.media.jai.ImageLayout;
import org.opengis.referencing.operation.MathTransform;

public class VerticalTransformCRIF extends CRIFImpl {

    @Override
    public RenderedImage create(ParameterBlock pb, RenderingHints renderingHints) {
        // Get ImageLayout from renderHints if any.
        ImageLayout layout = RIFUtil.getImageLayoutHint(renderingHints);

        RenderedImage[] sources = new RenderedImage[1];
        sources[0] = pb.getRenderedSource(0);

        MathTransform coordinatesTransform = (MathTransform) pb.getObjectParameter(0);
        MathTransform verticalTransform = (MathTransform) pb.getObjectParameter(1);
        Range noData = (Range) pb.getObjectParameter(2);

        return new VerticalTransformOpImage(
                renderingHints, layout, coordinatesTransform, verticalTransform, noData, sources);
    }
}
