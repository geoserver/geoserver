/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 *           (c) 2008 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

// J2SE dependencies
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.registry.RenderedRegistryMode;

import org.geotools.factory.Hints;
import org.geotools.image.jai.Registry;
import org.geotools.util.logging.Logging;

/**
 * Clone of GeoTools color invertion made work against a {@link ColorIndexer}
 * 
 * @source $URL$
 */
public class ColorIndexerDescriptor extends OperationDescriptorImpl {

    static final Logger LOGGER = Logging.getLogger(ColorIndexerCRIF.class);
    
    /**
     * 
     */
    private static final long serialVersionUID = 4951347100540806326L;

    /**
     * The operation name, which is {@value} .
     */
    public static final String OPERATION_NAME = "ColorIndexer";
    
    /**
     * Manually registers the operation in the registry in case it's not already there
     */
    public static void register() {
        try {
            final OperationRegistry opr = JAI.getDefaultInstance().getOperationRegistry();
            if(opr.getDescriptor(RenderedRegistryMode.MODE_NAME, OPERATION_NAME) == null) {
                Registry.registerRIF(JAI.getDefaultInstance(), new ColorIndexerDescriptor(),
                        new ColorIndexerCRIF(), "org.geoserver");
            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.getLocalizedMessage());
            }
        }
    }

    /**
     * Constructs the descriptor.
     */
    /**
     * Constructs the descriptor.
     */
    public ColorIndexerDescriptor() {
        super(
                new String[][] {
                        { "GlobalName", OPERATION_NAME },
                        { "LocalName", OPERATION_NAME },
                        { "Vendor", "org.geoserver" },
                        { "Description",
                                "Produce a paletted image from an RGB or RGBA image using a provided palette." },
                        { "DocURL", "http://www.geo-solutions.it/" }, // TODO:
                        // provides more accurate URL
                        { "Version", "1.0" }, { "arg0Desc", "Indexer." } },
                new String[] { RenderedRegistryMode.MODE_NAME }, 1, // Supported
                // modes
                new String[] { "Indexer" }, // Parameter
                // names
                new Class[] { ColorIndexer.class }, // Parameter
                // classes
                new Object[] { null, }, // Default
                // values
                null // Valid parameter values
        );
    }

    /**
     * Returns {@code true} if this operation supports the specified mode, and is capable of
     * handling the given input source(s) for the specified mode.
     * 
     * @param modeName The mode name (usually "Rendered").
     * @param param The parameter block for the operation to performs.
     * @param message A buffer for formatting an error message if any.
     */
    protected boolean validateSources(final String modeName, final ParameterBlock param,
            final StringBuffer message) {
        if (super.validateSources(modeName, param, message)) {
            for (int i = param.getNumSources(); --i >= 0;) {
                final Object source = param.getSource(i);
                if (!(source instanceof RenderedImage)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the parameters are valids. This implementation check that the number
     * of bands in the source src1 is equals to the number of bands of source src2.
     * 
     * @param modeName The mode name (usually "Rendered").
     * @param param The parameter block for the operation to performs.
     * @param message A buffer for formatting an error message if any.
     */
    protected boolean validateParameters(final String modeName, final ParameterBlock param,
            final StringBuffer message) {
        if (!super.validateParameters(modeName, param, message)) {
            return false;
        }
        if (!(param.getObjectParameter(0) instanceof ColorIndexer))
            return false;
        return true;
    }

    public static RenderedOp create(RenderedImage source, ColorIndexer indexer, Hints hints) {
        ParameterBlockJAI pb =
            new ParameterBlockJAI("ColorIndexer",
                                  RenderedRegistryMode.MODE_NAME);

        pb.setSource("source0", source);

        pb.setParameter("Indexer", indexer);

        return JAI.create("ColorIndexer", pb, hints);
    }
}
