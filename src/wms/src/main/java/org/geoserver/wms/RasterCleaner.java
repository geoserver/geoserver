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
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.RenderedImageAdapter;
import org.eclipse.imagen.RenderedImageList;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.wms.map.RenderedImageTimeDecorator;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.util.ImageUtilities;

public class RasterCleaner extends AbstractDispatcherCallback {
    static final ThreadLocal<List<RenderedImage>> images = new ThreadLocal<>();

    static final ThreadLocal<List<GridCoverage2D>> coverages = new ThreadLocal<>();

    /** Schedules a RenderedImage for cleanup at the end of the request */
    public static void addImage(RenderedImage image) {
        if (image == null) {
            return;
        }

        List<RenderedImage> list = images.get();
        if (list == null) {
            list = new ArrayList<>();
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
            list = new ArrayList<>();
            coverages.set(list);
        }
        list.add(coverage);
    }

    @Override
    public void finished(Request request) {
        cleanup();
    }

    /** Immediately cleans up all images and coverages scheduled for cleanup */
    public static void cleanup() {
        disposeCoverages();
        disposeImages();
    }

    private static void disposeImages() {
        List<RenderedImage> list = images.get();
        if (list != null) {
            images.remove();
            for (RenderedImage image : list) {
                if (image instanceof RenderedImageAdapter adapter) {
                    image = adapter.getWrappedImage();
                }

                if (image instanceof RenderedImageTimeDecorator decorator) image = decorator.getDelegate();

                if (image instanceof RenderedImageList ril) {
                    for (Object o : ril) {
                        disposeImage((RenderedImage) o);
                    }
                } else {
                    disposeImage(image);
                }
            }
            list.clear();
        }
    }

    /** Immediately disposes an image, the image might not be usable any longer after this call */
    public static void disposeImage(RenderedImage image) {
        if (image instanceof PlanarImage planarImage) {
            ImageUtilities.disposePlanarImageChain(planarImage);
        } else if (image instanceof BufferedImage bi) {
            bi.flush();
        }
    }

    private static void disposeCoverages() {
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
