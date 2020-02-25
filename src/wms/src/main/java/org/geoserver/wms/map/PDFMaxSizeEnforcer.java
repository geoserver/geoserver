/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import com.lowagie.text.pdf.ByteBuffer;
import com.lowagie.text.pdf.PdfGraphics2D;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.RenderListener;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Attaches itself to the renderer and ensures no more than maxSize bytes are used to store the PDF
 * in memory, and stops the renderer in case that happens.
 *
 * @author Andrea Aime - OpenGeo
 */
public class PDFMaxSizeEnforcer {

    long maxSize;

    ByteBuffer pdfBytes;

    /**
     * Builds a new max errors enforcer. If maxErrors is not positive the enforcer will do nothing
     */
    public PDFMaxSizeEnforcer(
            final GTRenderer renderer, final PdfGraphics2D graphics, final int maxSize) {
        this.maxSize = maxSize;
        this.pdfBytes = graphics.getContent().getInternalBuffer();

        if (maxSize > 0) {
            renderer.addRenderListener(
                    new RenderListener() {

                        public void featureRenderer(SimpleFeature feature) {
                            if (pdfBytes.size() > maxSize) {
                                renderer.stopRendering();
                            }
                        }

                        public void errorOccurred(Exception e) {}
                    });
        }
    }

    /** True if the memory used by the PDF buffer exceeds the max memory settings */
    public boolean exceedsMaxSize() {
        return maxSize > 0 && pdfBytes.size() > maxSize;
    }

    /** Returns the amount of memory currently used by the */
    public long memoryUsed() {
        return pdfBytes.size();
    }
}
