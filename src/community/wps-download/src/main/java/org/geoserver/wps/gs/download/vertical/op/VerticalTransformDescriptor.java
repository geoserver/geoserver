/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical.op;

import it.geosolutions.jaiext.range.Range;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import javax.media.jai.*;
import javax.media.jai.registry.RenderedRegistryMode;
import org.opengis.referencing.operation.MathTransform;

public class VerticalTransformDescriptor extends OperationDescriptorImpl {

    /**
     * The resource strings that provide the general documentation and specify the parameter list
     * for this operation.
     */
    private static final String[][] resources = {
        {"GlobalName", "verticalTransform"},
        {"LocalName", "vertical"},
        {"Vendor", "it.geosolutions.jaiext"},
        {
            "Description",
            "This class executes the operation selected by the user on each pixel of the source images "
        },
        {"DocURL", "Not Defined"},
        {"Version", "1.0"},
        {"arg0Desc", "2D Coordinates Math Transform"},
        {"arg1Desc", "Vertical Math Transform"},
        {"arg2Desc", "No Data Range used"},
    };

    /** Input Parameter name */
    private static final String[] paramNames = {
        "coordinatesTransform", "verticalTransform", "noData"
    };

    /** Input Parameter class */
    private static final Class[] paramClasses = {
        MathTransform.class, MathTransform.class, it.geosolutions.jaiext.range.Range.class
    };

    /** Input Parameter default values */
    private static final Object[] paramDefaults = {null, null, null};

    /** Constructor. */
    public VerticalTransformDescriptor() {
        super(resources, paramClasses, paramNames, paramDefaults);
    }

    /** Returns <code>true</code> since renderable operation is supported. */
    public boolean isRenderableSupported() {
        return true;
    }

    public static RenderedOp create(
            MathTransform coordinatesTransform,
            MathTransform verticalTransform,
            Range noData,
            RenderingHints hints,
            RenderedImage... sources) {

        ParameterBlockJAI pb =
                new ParameterBlockJAI("verticalTransform", RenderedRegistryMode.MODE_NAME);
        RenderedImage img = sources[0];
        pb.setSource(img, 0);

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("coordinatesTransform", coordinatesTransform);
        pb.setParameter("verticalTransform", verticalTransform);
        pb.setParameter("noData", noData);
        return JAI.create("verticalTransform", pb, hints);
    }

    public static RenderableOp createRenderable(
            MathTransform coordinatesTransform,
            MathTransform verticalTransform,
            Range noData,
            RenderingHints hints,
            RenderableImage... sources) {

        ParameterBlockJAI pb =
                new ParameterBlockJAI("verticalTransform", RenderedRegistryMode.MODE_NAME);
        RenderableImage img = sources[0];
        pb.setSource(img, 0);

        if (pb.getNumSources() == 0) {
            throw new IllegalArgumentException("The input images are Null");
        }

        pb.setParameter("coordinatesTransform", coordinatesTransform);
        pb.setParameter("verticalTransform", verticalTransform);
        pb.setParameter("noData", noData);

        return JAI.createRenderable("verticalTransform", pb, hints);
    }
}
