/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedImageList;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.wms.map.RenderedImageTimeDecorator;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.util.ImageUtilities;

public class RasterCleaner extends AbstractDispatcherCallback {
    static final ThreadLocal<List<RenderedImage>> images = new ThreadLocal<List<RenderedImage>>();

    static final ThreadLocal<List<GridCoverage2D>> coverages =
            new ThreadLocal<List<GridCoverage2D>>();

    /** Schedules a RenderedImage for cleanup at the end of the request */
    public static void addImage(RenderedImage image) {
        if (image == null) {
            return;
        }

        List<RenderedImage> list = images.get();
        if (list == null) {
            list = new ArrayList<RenderedImage>();
            images.set(list);
        }
        list.add(image);
    }

    /** Schedules a RenderedImage for cleanup at the end of the request */
    public static void addCoverage(GridCoverage2D coverage) {
        if (coverage == null) {
            return;
        }

        List<GridCoverage2D> list = coverages.get();
        if (list == null) {
            list = new ArrayList<GridCoverage2D>();
            coverages.set(list);
        }
        list.add(coverage);
    }

    @Override
    public void finished(Request request) {
        disposeCoverages();
        disposeImages();
    }

    private void disposeImages() {
        List<RenderedImage> list = images.get();
        if (list != null) {
            images.remove();
            for (RenderedImage image : list) {
                if (image instanceof RenderedImageTimeDecorator)
                    image = ((RenderedImageTimeDecorator) image).getDelegate();

                if (image instanceof RenderedImageList) {
                    RenderedImageList ril = (RenderedImageList) image;
                    for (int i = 0; i < ril.size(); i++) {
                        disposeImage((RenderedImage) ril.get(i));
                    }
                } else {
                    disposeImage(image);
                }
            }
            list.clear();
        }
    }

    private void disposeImage(RenderedImage image) {
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        } else if (image instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) image;
            bi.flush();
        }
    }

    private void disposeCoverages() {
        List<GridCoverage2D> list = coverages.get();
        if (list != null) {
            coverages.remove();
            for (GridCoverage2D coverage : list) {
                coverage.dispose(true);
            }
            list.clear();
        }
    }

    public List<RenderedImage> getImages() {
        return images.get();
    }

    public List<GridCoverage2D> getCoverages() {
        return coverages.get();
    }
}
