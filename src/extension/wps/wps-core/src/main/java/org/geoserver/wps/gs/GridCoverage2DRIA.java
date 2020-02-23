/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import com.sun.media.jai.util.ImageUtil;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.util.Utilities;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * A RenderedImage that provides values coming from a source GridCoverage2D, with a backing grid
 * addressable as the target GridCoverage2D.
 *
 * <p>The exposed Layout will be the same as the target, and each Point in the target grid can be
 * used in the resulting RenderedImage,
 *
 * @author ETj <etj at geo-solutions.it>
 */
@SuppressWarnings("unchecked")
public class GridCoverage2DRIA extends GeometricOpImage {

    /** DEFAULT_BORDEREXTENDER */
    public static final BorderExtender DEFAULT_BORDEREXTENDER =
            BorderExtender.createInstance(BorderExtender.BORDER_COPY);

    private static final Logger LOGGER = Logger.getLogger(GridCoverage2DRIA.class.getName());

    private final GridCoverage2D src;

    private final GridGeometry2D dstGridGeometry;

    private AffineTransform g2wd;

    private AffineTransform g2ws;

    private AffineTransform w2gd;

    private AffineTransform w2gs;

    private MathTransform src2dstCRSTransform;

    private MathTransform dst2srcCRSTransform;

    /** Color table representing source's IndexColorModel. */
    // just for keeping compiler quiet: let's see if we really need it
    private byte[][] ctable = null;

    /** Do we need a reprojection. */
    private boolean needReprojection;

    private AffineTransform concatenatedForwardTransform;

    private AffineTransform concatenatedBackwardTransform;

    private int kwidth;

    private int kheight;

    private int maxX;

    private int maxY;

    private RandomIter iter;

    private int lpad;

    private int tpad;

    private int rpad;

    private int bpad;

    /**
     * Wrap the src coverage in the dst layout. <br>
     * The resulting RenderedImage will contain the data in src, and will be accessible via the grid
     * specs of dst,
     *
     * @param src the data coverage to be remapped on dst grid
     * @param dst the provider of the final grid
     * @param nodata the nodata value to set for cells not covered by src but included in dst. All
     *     bands will share the same nodata value.
     * @return an instance of Coverage2RenderedImageAdapter
     */
    public static GridCoverage2DRIA create(
            final GridCoverage2D src, final GridCoverage2D dst, final double nodata) {

        Utilities.ensureNonNull("dst", dst);
        return GridCoverage2DRIA.create(src, dst.getGridGeometry(), nodata);
    }

    /**
     * Wrap the src coverage in the dst {@link GridGeometry2D}. <br>
     * The resulting RenderedImage will contain the data in src, and will be accessible via the grid
     * specs of dst,
     *
     * @param src the data coverage to be remapped on dst grid
     * @param dstGridGeometry the final {@link GridGeometry2D}
     * @param nodata the nodata value to set for cells not covered by src but included in dst. All
     *     bands will share the same nodata value.
     * @return an instance of Coverage2RenderedImageAdapter
     */
    public static GridCoverage2DRIA create(
            final GridCoverage2D src, final GridGeometry2D dstGridGeometry, double nodata) {

        Utilities.ensureNonNull("dstGridGeometry", dstGridGeometry);
        Utilities.ensureNonNull("src", src);

        // === Create destination Layout, retaining source tiling to minimize quirks
        // TODO allow to override tiling
        final GridEnvelope2D destinationRasterDimension = dstGridGeometry.getGridRange2D();
        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(destinationRasterDimension.x).setMinY(destinationRasterDimension.y);
        imageLayout
                .setWidth(destinationRasterDimension.width)
                .setHeight(destinationRasterDimension.height);
        imageLayout
                .setTileHeight(src.getRenderedImage().getSampleModel().getHeight())
                .setTileWidth(src.getRenderedImage().getSampleModel().getWidth());

        //
        // SampleModel and ColorModel are related to data itself, so we
        // copy them from the source
        imageLayout.setColorModel(src.getRenderedImage().getColorModel());
        imageLayout.setSampleModel(src.getRenderedImage().getSampleModel());

        // === BorderExtender
        //
        // We have yet to check for it usefulness: it might be more convenient
        // to check for region overlapping and return a nodata value by hand,
        // so to avoid problems with interpolation at source raster borders.
        //
        BorderExtender extender = new BorderExtenderConstant(new double[] {nodata});

        // add tile caching to the mix
        final RenderingHints hints =
                new RenderingHints(JAI.KEY_TILE_CACHE, JAI.getDefaultInstance().getTileCache());
        return new GridCoverage2DRIA(
                src,
                dstGridGeometry,
                vectorize(src.getRenderedImage()),
                imageLayout,
                hints,
                false,
                extender,
                Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                new double[] {nodata});
    }

    @SuppressWarnings("rawtypes")
    protected GridCoverage2DRIA(
            final GridCoverage2D src,
            final GridGeometry2D dstGridGeometry,
            final Vector sources,
            final ImageLayout layout,
            final Map configuration,
            final boolean cobbleSources,
            final BorderExtender extender,
            final Interpolation interp,
            final double[] nodata) {

        super(sources, layout, configuration, cobbleSources, extender, interp, nodata);

        // put aside ths source GridCoverage2D
        this.src = src;
        this.dstGridGeometry = new GridGeometry2D(dstGridGeometry);

        // init tranformations between spaces
        initTransformations();

        // initi iterators and so on
        initIterator();
    }

    /** */
    private void initIterator() {

        // --- check interpolation
        if (interp != null) {
            kwidth = interp.getWidth();
            kheight = interp.getHeight();
            lpad = interp.getLeftPadding();
            rpad = interp.getRightPadding();
            tpad = interp.getTopPadding();
            bpad = interp.getBottomPadding();
        } else {
            lpad = rpad = tpad = bpad = 0;
        }
        // do we really need an extender?

        // --- check extender
        if (extender == null) {
            extender = GridCoverage2DRIA.DEFAULT_BORDEREXTENDER;
        }
        PlanarImage srcImage = PlanarImage.wrapRenderedImage(src.getRenderedImage());
        if (!(lpad == rpad
                && rpad == tpad
                && tpad == bpad
                && bpad == 0)) { // there is need to extend
            minX = srcImage.getMinX();
            maxX = srcImage.getMaxX() - 1;
            minY = srcImage.getMinY();
            maxY = srcImage.getMaxY() - 1;
            Rectangle bounds =
                    new Rectangle(
                            srcImage.getMinX() - lpad,
                            srcImage.getMinY() - tpad,
                            srcImage.getWidth() + lpad + rpad,
                            srcImage.getHeight() + tpad + bpad);
            iter = RandomIterFactory.create(srcImage.getExtendedData(bounds, extender), bounds);
        } else {
            minX = srcImage.getMinX();
            maxX = srcImage.getMaxX() - 1;
            minY = srcImage.getMinY();
            maxY = srcImage.getMaxY() - 1;
            iter = RandomIterFactory.create(srcImage, srcImage.getBounds());
        }
    }

    /** */
    private void initTransformations() throws IllegalArgumentException {
        // === Take one for all all the transformation we need to pass from
        // model, sample, src, target and viceversa.
        g2wd = (AffineTransform) dstGridGeometry.getGridToCRS2D(PixelOrientation.UPPER_LEFT);

        try {
            w2gd = g2wd.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Can't compute source W2G", e);
        }

        g2ws = (AffineTransform) src.getGridGeometry().getGridToCRS2D(PixelOrientation.UPPER_LEFT);
        try {
            w2gs = g2ws.createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("Can't compute source W2G", e);
        }

        try {
            CoordinateReferenceSystem sourceCRS = src.getCoordinateReferenceSystem2D();
            CoordinateReferenceSystem targetCRS = dstGridGeometry.getCoordinateReferenceSystem2D();
            if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
                src2dstCRSTransform = CRS.findMathTransform(sourceCRS, targetCRS, true);
                dst2srcCRSTransform = src2dstCRSTransform.inverse();
                if (!src2dstCRSTransform.isIdentity()) {
                    needReprojection = true;
                    return;
                }
            }

            // === if we got here we don't need to reproject, let's concatenate the transforms
            // we don't reproject, let's simplify
            needReprojection = false;
            concatenatedForwardTransform = (AffineTransform) w2gd.clone();
            concatenatedForwardTransform.concatenate(g2ws);
            concatenatedBackwardTransform = concatenatedForwardTransform.createInverse();
            if (XAffineTransform.isIdentity(
                    concatenatedForwardTransform, 1E-6)) { // TODO improve this check
                concatenatedForwardTransform = new AffineTransform(); // identity
                concatenatedBackwardTransform = new AffineTransform(); // identity
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Can't create a transform between CRS", e);
        }
    }

    /** Maps the provided point from source space to destination space for the provided source. */
    @Override
    public Point2D mapSourcePoint(Point2D srcPt, int sourceIndex) {
        if (srcPt == null) {
            throw new IllegalArgumentException("Bad dest pt"); // JaiI18N.getString("Generic0"));
        } else if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException("Bad src"); // JaiI18N.getString("Generic1"));
        }

        double coords[] = new double[] {srcPt.getX(), srcPt.getY()};

        try {
            mapSourcePoint(coords);
        } catch (TransformException e) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.log(Level.WARNING, "Error transforming coords", e);
            }
            return null;
        }

        Point2D ret = ((Point2D) srcPt.clone());
        ret.setLocation(coords[0], coords[1]);
        if (dstGridGeometry.getGridRange2D().contains(ret)) return ret;
        else {
            LOGGER.log(
                    Level.WARNING,
                    "{0} mapped to {1} lies outside {2},{3}+{4}x{5}",
                    new Object[] {
                        srcPt,
                        ret,
                        dstGridGeometry.getGridRange2D().x,
                        dstGridGeometry.getGridRange2D().y,
                        dstGridGeometry.getGridRange2D().width + dstGridGeometry.getGridRange2D().x,
                        dstGridGeometry.getGridRange2D().height + dstGridGeometry.getGridRange2D().y
                    });
            return null;
        }
    }

    /**
     * Returns the minimum bounding box of the region of the destination to which a particular
     * <code>Rectangle</code> of the specified source will be mapped.
     *
     * <p>The integral source rectangle coordinates should be considered pixel indices. The "energy"
     * of each pixel is defined to be concentrated in the continuous plane of pixels at an offset of
     * (0.5,&nbsp;0.5) from the index of the pixel. Forward mappings must take this (0.5,&nbsp;0.5)
     * pixel center into account. Thus given integral source pixel indices as input, the fractional
     * destination location, as calculated by functions Xf(xSrc,&nbsp;ySrc), Yf(xSrc,&nbsp;ySrc), is
     * given by:
     *
     * <pre>
     *
     *     xDst = Xf(xSrc+0.5, ySrc+0.5) - 0.5
     *     yDst = Yf(xSrc+0.5, ySrc+0.5) - 0.5
     *
     * </pre>
     *
     * @param pxRect the <code>Rectangle</code> in source coordinates.
     * @param i the index of the source image.
     * @return a <code>Rectangle</code> indicating the destination bounding box, or <code>null
     *     </code> if the bounding box is unknown.
     * @throws IllegalArgumentException if <code>sourceIndex</code> is negative or greater than the
     *     index of the last source.
     * @throws IllegalArgumentException if <code>sourceRect</code> is <code>null</code>.
     */
    @Override
    protected Rectangle forwardMapRect(Rectangle pxRect, int i) {
        // transformation from out target coverage toward the source one.
        // note that source/target names from OpImage are reversed with respect to our
        // definitions

        // i is not used, only one source raster

        float[] pts = rect2PointArr(pxRect);

        try {
            g2ws.transform(pts, 0, pts, 0, 4);
            src2dstCRSTransform.transform(pts, 0, pts, 0, 4);
            w2gd.transform(pts, 0, pts, 0, 4);
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Error transforming coords", e);
            return null;
        }

        Rectangle srcRect = pointArr2Rect(pts);
        return srcRect; // .intersection(src.getGridGeometry().getGridRange2D());
    }

    /** Maps the provided point from destination space to source space for the provided source. */
    @Override
    public Point2D mapDestPoint(Point2D destPt, int sourceIndex) {
        if (destPt == null) {
            throw new IllegalArgumentException("Bad dest pt"); // JaiI18N.getString("Generic0"));
        }
        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException("Bad src"); // JaiI18N.getString("Generic1"));
        }

        double coords[] = new double[] {destPt.getX(), destPt.getY()};

        try {
            mapDestPoint(coords);
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Error transforming coords", e);
            return null;
        }

        Point2D ret = ((Point2D) destPt.clone());
        ret.setLocation(coords[0], coords[1]);
        if (src.getGridGeometry().getGridRange2D().contains(coords[0], coords[1])) return ret;
        else return null;
    }

    /** Maps the provided point from destination space to source space for the provided source. */
    private void mapDestPoint(double[] coords) throws TransformException {
        final int npoints = coords.length / 2;
        // == optimized route
        if (!needReprojection) {
            if (!concatenatedBackwardTransform.isIdentity()) {
                concatenatedBackwardTransform.transform(coords, 0, coords, 0, npoints);
            }
            return;
        }

        // == standard route
        g2wd.transform(coords, 0, coords, 0, npoints); // going from raster to model in destination
        dst2srcCRSTransform.transform(coords, 0, coords, 0, npoints); // reprojection
        w2gs.transform(coords, 0, coords, 0, npoints); // from model to raster in source space
    }

    /** Maps the provided point from source space to destination space for the provided source. */
    private void mapSourcePoint(double[] coords) throws TransformException {
        final int npoints = coords.length / 2;

        // optimized route
        if (!needReprojection) {
            if (!concatenatedForwardTransform.isIdentity()) {
                concatenatedForwardTransform.transform(coords, 0, coords, 0, npoints);
            }
            return;
        }

        // == going from source to destination raster space
        // StringBuilder sb = new StringBuilder();
        // sb.append("SRC[").append(coords[0]).append(',').append(coords[1]).append(']').append("--g2wd->");
        g2ws.transform(coords, 0, coords, 0, npoints);

        // sb.append("[").append(coords[0]).append(',').append(coords[1]).append(']').append("--d2sCRS->");
        src2dstCRSTransform.transform(coords, 0, coords, 0, npoints);

        // sb.append('[').append(coords[0]).append(',').append(coords[1]).append(']').append("--w2gs->");
        w2gd.transform(coords, 0, coords, 0, npoints);
        // sb.append('[').append(coords[0]).append(',').append(coords[1]).append(']');
        // System.out.println(sb);
    }

    /**
     * Returns the minimum bounding box of the region of the specified source to which a particular
     * <code>Rectangle</code> of the destination will be mapped.
     *
     * <p>The integral destination rectangle coordinates should be considered pixel indices. The
     * "energy" of each pixel is defined to be concentrated in the continuous plane of pixels at an
     * offset of (0.5,&nbsp;0.5) from the index of the pixel. Backward mappings must take this
     * (0.5,&nbsp;0.5) pixel center into account. Thus given integral destination pixel indices as
     * input, the fractional source location, as calculated by functions Xb(xDst,&nbsp;yDst),
     * Yb(xDst,&nbsp;yDst), is given by:
     *
     * <pre>
     *
     *     xSrc = Xb(xDst+0.5, yDst+0.5) - 0.5
     *     ySrc = Yb(xDst+0.5, yDst+0.5) - 0.5
     *
     * </pre>
     *
     * @param destRect the <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex the index of the source image.
     * @return a <code>Rectangle</code> indicating the source bounding box, or <code>null</code> if
     *     the bounding box is unknown.
     * @throws IllegalArgumentException if <code>sourceIndex</code> is negative or greater than the
     *     index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is <code>null</code>.
     */
    @Override
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {
        float[] pts = rect2PointArr(destRect);

        try {
            g2wd.transform(pts, 0, pts, 0, 4);
            dst2srcCRSTransform.transform(pts, 0, pts, 0, 4);
            w2gs.transform(pts, 0, pts, 0, 4);
        } catch (TransformException e) {
            LOGGER.log(Level.WARNING, "Error transforming coords", e);
            return null;
        }

        Rectangle pxRect = pointArr2Rect(pts);
        return pxRect;
    }

    private static float[] rect2PointArr(Rectangle rect) {
        float dx0 = (float) rect.x;
        float dy0 = (float) rect.y;
        float dw = (float) (rect.width);
        float dh = (float) (rect.height);

        return new float[] {dx0, dy0, (dx0 + dw), dy0, (dx0 + dw), (dy0 + dh), dx0, (dy0 + dh)};
    }

    private Rectangle pointArr2Rect(float[] points) {
        float f_sx0 = Float.MAX_VALUE;
        float f_sy0 = Float.MAX_VALUE;
        float f_sx1 = -Float.MAX_VALUE;
        float f_sy1 = -Float.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            float px = points[i * 2];
            float py = points[i * 2 + 1];

            f_sx0 = Math.min(f_sx0, px);
            f_sy0 = Math.min(f_sy0, py);
            f_sx1 = Math.max(f_sx1, px);
            f_sy1 = Math.max(f_sy1, py);
        }

        int s_x0 = 0, s_y0 = 0, s_x1 = 0, s_y1 = 0;

        // Find the bounding box of the source rectangle
        if (interp instanceof InterpolationNearest) {
            s_x0 = (int) Math.floor(f_sx0);
            s_y0 = (int) Math.floor(f_sy0);

            // Fix for bug 4485920 was to add " + 0.05" to the following
            // two lines. It should be noted that the fix was made based
            // on empirical evidence and tested thoroughly, but it is not
            // known whether this is the root cause.
            s_x1 = (int) Math.ceil(f_sx1 + 0.5);
            s_y1 = (int) Math.ceil(f_sy1 + 0.5);
        } else {
            s_x0 = (int) Math.floor(f_sx0 - 0.5);
            s_y0 = (int) Math.floor(f_sy0 - 0.5);
            s_x1 = (int) Math.ceil(f_sx1);
            s_y1 = (int) Math.ceil(f_sy1);
        }

        //
        // Return the new rectangle
        //
        return new Rectangle(s_x0, s_y0, s_x1 - s_x0, s_y1 - s_y0);
    }

    /**
     * Warps a rectangle.
     *
     * <p>Copied and adapted from WarpGeneralOpImage
     */
    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        switch (d.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                computeRectByte(sources[0], d);
                break;
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(sources[0], d);
                break;
            case DataBuffer.TYPE_SHORT:
                computeRectShort(sources[0], d);
                break;
            case DataBuffer.TYPE_INT:
                computeRectInt(sources[0], d);
                break;
            case DataBuffer.TYPE_FLOAT:
                computeRectFloat(sources[0], d);
                break;
            case DataBuffer.TYPE_DOUBLE:
                computeRectDouble(sources[0], d);
                break;
        }

        if (d.isDataCopy()) {
            d.clampDataArrays();
            d.copyDataToRaster();
        }
    }

    private void computeRectByte(PlanarImage src, RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        byte[][] data = dst.getByteDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

        byte[] backgroundByte = new byte[dstBands];
        for (int i = 0; i < dstBands; i++) {
            backgroundByte[i] = (byte) backgroundValues[i];
        }

        if (ctable == null) { // source does not have IndexColorModel

            // == cycle on destination image
            int minx = dst.getX(), x = 0;
            int miny = dst.getY(), y = 0;
            final double coords[] = new double[2]; // temp point
            // == cycle on Y
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;

                y = miny + h;
                // --- cycle on X
                for (int w = 0; w < dstWidth; w++) {
                    x = minx + w;
                    // map destinaton point to source point
                    try {
                        coords[0] = x;
                        coords[1] = y;
                        mapDestPoint(coords);
                    } catch (TransformException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }

                    // compute integer position in source space
                    int xint = floor(coords[0]);
                    int yint = floor(coords[1]);
                    int xfrac = (int) ((coords[0] - xint) * precH);
                    int yfrac = (int) ((coords[1] - yint) * precV);

                    if (xint < minX || xint > maxX || yint < minY || yint > maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundByte[b];
                            }
                        }
                    } else {
                        // --- optimize nearest interpolation
                        if (interp instanceof InterpolationNearest) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] =
                                        ImageUtil.clampByte(iter.getSample(xint, yint, b));
                            }

                        } else {

                            xint -= lpad;
                            yint -= tpad;

                            for (int b = 0; b < dstBands; b++) {
                                for (int j = 0; j < kheight; j++) {
                                    for (int i = 0; i < kwidth; i++) {
                                        samples[j][i] =
                                                iter.getSample(xint + i, yint + j, b) & 0xFF;
                                    }
                                }

                                data[b][pixelOffset + bandOffsets[b]] =
                                        ImageUtil.clampByte(
                                                interp.interpolate(samples, xfrac, yfrac));
                            }
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        } else { // source has IndexColorModel
            // == cycle on destination image
            int minx = dst.getX(), x = 0;
            int miny = dst.getY(), y = 0;
            final double coords[] = new double[2]; // temp point
            // --- cycle on Y
            for (int h = 0; h < dstHeight; h++) {
                int pixelOffset = lineOffset;
                lineOffset += lineStride;
                y = miny + h; // y coord in the position to set in dest space

                // --- cycle on X
                for (int w = 0; w < dstWidth; w++) {
                    x = minx + w; // x coord in the position to set in dest space

                    // map destinaton point to source space for getting the values
                    try {
                        coords[0] = x;
                        coords[1] = y;
                        mapDestPoint(coords);
                    } catch (TransformException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                        throw new RuntimeException(e);
                    }

                    // compute integer position in source space
                    int xint = floor(coords[0]);
                    int yint = floor(coords[1]);
                    int xfrac = (int) ((coords[0] - xint) * precH);
                    int yfrac = (int) ((coords[1] - yint) * precV);

                    if (xint < minX || xint >= maxX || yint < minY || yint >= maxY) {
                        /* Fill with a background color. */
                        if (setBackground) {
                            for (int b = 0; b < dstBands; b++) {
                                data[b][pixelOffset + bandOffsets[b]] = backgroundByte[b];
                            }
                        }
                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            byte[] t = ctable[b];

                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] =
                                            t[iter.getSample(xint + i, yint + j, 0) & 0xFF] & 0xFF;
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] =
                                    ImageUtil.clampByte(interp.interpolate(samples, xfrac, yfrac));
                        }
                    }

                    pixelOffset += pixelStride;
                }
            }
        }
    }

    private void computeRectUShort(PlanarImage src, RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

        short[] backgroundUShort = new short[dstBands];
        for (int i = 0; i < dstBands; i++) {
            backgroundUShort[i] = (short) backgroundValues[i];
        }

        // == cycle on destination image
        int minx = dst.getX(), x = 0;
        int miny = dst.getY(), y = 0;
        final double coords[] = new double[2]; // temp point
        // --- cycle on Y
        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;
            y = miny + h; // y coord in the position to set in dest space

            // --- cycle on X
            for (int w = 0; w < dstWidth; w++) {
                x = minx + w; // x coord in the position to set in dest space

                // map destinaton point to source space for getting the values
                try {
                    coords[0] = x;
                    coords[1] = y;
                    mapDestPoint(coords);
                } catch (TransformException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                    throw new RuntimeException(e);
                }

                // compute integer position in source space
                int xint = floor(coords[0]);
                int yint = floor(coords[1]);
                int xfrac = (int) ((coords[0] - xint) * precH);
                int yfrac = (int) ((coords[1] - yint) * precV);

                if (xint < minX || xint > maxX || yint < minY || yint > maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] = backgroundUShort[b];
                        }
                    }
                } else {
                    // --- optimize nearest interpolation
                    if (interp instanceof InterpolationNearest) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] =
                                    ImageUtil.clampUShort(iter.getSample(xint, yint, b));
                        }

                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(xint + i, yint + j, b) & 0xFFFF;
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] =
                                    ImageUtil.clampUShort(
                                            interp.interpolate(samples, xfrac, yfrac));
                        }
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectShort(PlanarImage src, RasterAccessor dst) {

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        short[][] data = dst.getShortDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

        short[] backgroundShort = new short[dstBands];
        for (int i = 0; i < dstBands; i++) {
            backgroundShort[i] = (short) backgroundValues[i];
        }

        // == cycle on destination image
        int minx = dst.getX(), x = 0;
        int miny = dst.getY(), y = 0;
        final double coords[] = new double[2]; // temp point
        // --- cycle on Y
        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;
            y = miny + h; // y coord in the position to set in dest space

            // --- cycle on X
            for (int w = 0; w < dstWidth; w++) {
                x = minx + w; // x coord in the position to set in dest space

                // map destinaton point to source space for getting the values
                try {
                    coords[0] = x;
                    coords[1] = y;
                    mapDestPoint(coords);
                } catch (TransformException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                    throw new RuntimeException(e);
                }

                // compute integer position in source space
                int xint = floor(coords[0]);
                int yint = floor(coords[1]);
                int xfrac = (int) ((coords[0] - xint) * precH);
                int yfrac = (int) ((coords[1] - yint) * precV);

                if (xint < minX || xint > maxX || yint < minY || yint > maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] = backgroundShort[b];
                        }
                    }
                } else {
                    // --- optimize nearest interpolation
                    if (interp instanceof InterpolationNearest) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] =
                                    ImageUtil.clampShort(iter.getSample(xint, yint, b));
                        }

                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(xint + i, yint + j, b);
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] =
                                    ImageUtil.clampShort(interp.interpolate(samples, xfrac, yfrac));
                        }
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectInt(PlanarImage src, RasterAccessor dst) {

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        int[][] data = dst.getIntDataArrays();

        int precH = 1 << interp.getSubsampleBitsH();
        int precV = 1 << interp.getSubsampleBitsV();

        int[][] samples = new int[kheight][kwidth];

        int lineOffset = 0;

        int[] backgroundInt = new int[dstBands];
        for (int i = 0; i < dstBands; i++) {
            backgroundInt[i] = (int) backgroundValues[i];
        }

        // == cycle on destination image
        int minx = dst.getX(), x = 0;
        int miny = dst.getY(), y = 0;
        final double coords[] = new double[2]; // temp point
        // --- cycle on Y
        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;
            y = miny + h; // y coord in the position to set in dest space

            // --- cycle on X
            for (int w = 0; w < dstWidth; w++) {
                x = minx + w; // x coord in the position to set in dest space

                // map destinaton point to source space for getting the values
                try {
                    coords[0] = x;
                    coords[1] = y;
                    mapDestPoint(coords);
                } catch (TransformException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                    throw new RuntimeException(e);
                }

                // compute integer position in source space
                int xint = floor(coords[0]);
                int yint = floor(coords[1]);
                int xfrac = (int) ((coords[0] - xint) * precH);
                int yfrac = (int) ((coords[1] - yint) * precV);

                if (xint < minX || xint > maxX || yint < minY || yint > maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] = backgroundInt[b];
                        }
                    }
                } else {
                    // --- optimize nearest interpolation
                    if (interp instanceof InterpolationNearest) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] = iter.getSample(xint, yint, b);
                        }

                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSample(xint + i, yint + j, b);
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] =
                                    interp.interpolate(samples, xfrac, yfrac);
                        }
                    }
                }

                pixelOffset += pixelStride;
            }
        }
    }

    private void computeRectFloat(PlanarImage src, RasterAccessor dst) {

        // -- prepare for cycling on the destination tile
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        float[][] samples = new float[kheight][kwidth];
        int lineOffset = 0;

        // == cycle on destination image
        int minx = dst.getX(), x = 0;
        int miny = dst.getY(), y = 0;
        final double coords[] = new double[2]; // temp point
        // --- cycle on Y
        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += dstLineStride;
            y = miny + h; // y coord in the position to set in dest space

            // --- cycle on X
            for (int w = 0; w < dstWidth; w++) {
                x = minx + w; // x coord in the position to set in dest space

                // map destinaton point to source space for getting the values
                try {
                    coords[0] = x;
                    coords[1] = y;
                    mapDestPoint(coords);
                } catch (TransformException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                    throw new RuntimeException(e);
                }

                // compute integer position in source space
                int xint = floor(coords[0]);
                int yint = floor(coords[1]);
                float xfrac = (float) (coords[0] - xint);
                float yfrac = (float) (coords[1] - yint);
                if (xint < minX || xint > maxX || yint < minY || yint > maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][pixelOffset + dstBandOffsets[b]] =
                                    (float) backgroundValues[b];
                        }
                    }
                } else {

                    // --- optimize nearest interpolation
                    if (interp instanceof InterpolationNearest) {
                        for (int b = 0; b < dstBands; b++) {
                            dstData[b][pixelOffset + dstBandOffsets[b]] =
                                    iter.getSampleFloat(xint, yint, b);
                        }

                    } else {
                        // generic  interpolation
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSampleFloat(xint + i, yint + j, b);
                                }
                            }

                            dstData[b][pixelOffset + dstBandOffsets[b]] =
                                    interp.interpolate(samples, xfrac, yfrac);
                        }
                    }
                }

                pixelOffset += dstPixelStride;
            }
        }
    }

    private void computeRectDouble(PlanarImage src, RasterAccessor dst) {

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int lineStride = dst.getScanlineStride();
        int pixelStride = dst.getPixelStride();
        int[] bandOffsets = dst.getBandOffsets();
        double[][] data = dst.getDoubleDataArrays();

        double[][] samples = new double[kheight][kwidth];

        int lineOffset = 0;
        // == cycle on destination image
        int minx = dst.getX(), x = 0;
        int miny = dst.getY(), y = 0;
        final double coords[] = new double[2]; // temp point
        // --- cycle on Y
        for (int h = 0; h < dstHeight; h++) {
            int pixelOffset = lineOffset;
            lineOffset += lineStride;

            y = miny + h;
            // --- cycle on X
            for (int w = 0; w < dstWidth; w++) {
                x = minx + w;
                // map destinaton point to source point
                try {
                    coords[0] = x;
                    coords[1] = y;
                    mapDestPoint(coords);
                } catch (TransformException e) {
                    LOGGER.log(Level.FINER, e.getMessage(), e);
                    throw new RuntimeException(e);
                }

                // compute integer position in source space
                int xint = floor(coords[0]);
                int yint = floor(coords[1]);
                float xfrac = (float) (coords[0] - xint);
                float yfrac = (float) (coords[1] - yint);
                if (xint < minX || xint > maxX || yint < minY || yint > maxY) {
                    /* Fill with a background color. */
                    if (setBackground) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] = backgroundValues[b];
                        }
                    }
                } else {
                    // --- optimize nearest interpolation
                    if (interp instanceof InterpolationNearest) {
                        for (int b = 0; b < dstBands; b++) {
                            data[b][pixelOffset + bandOffsets[b]] =
                                    iter.getSampleDouble(xint, yint, b);
                        }

                    } else {
                        xint -= lpad;
                        yint -= tpad;

                        for (int b = 0; b < dstBands; b++) {
                            for (int j = 0; j < kheight; j++) {
                                for (int i = 0; i < kwidth; i++) {
                                    samples[j][i] = iter.getSampleDouble(xint + i, yint + j, b);
                                }
                            }

                            data[b][pixelOffset + bandOffsets[b]] =
                                    interp.interpolate(samples, xfrac, yfrac);
                        }
                    }
                }
                pixelOffset += pixelStride;
            }
        }
    }

    /** Returns the "floor" value of a double. */
    private static final int floor(double f) {
        return f >= 0 ? (int) f : (int) f - 1;
    }

    /**
     * @param x0 The minimum X coordinate of the destination region.
     * @param y0 The minimum Y coordinate of the destination region.
     * @param width The width of the destination region.
     * @param height The height of the destination region.
     * @param periodX The horizontal sampling period.
     * @param periodY The vertical sampling period.
     * @param destRect A <code>float</code> array containing at least <code>
     *     2*((width+periodX-1)/periodX)*
     *                ((height+periodY-1)/periodY)</code> elements, or <code>null</code>. If <code>
     *     null</code>, a new array will be constructed.
     * @return A reference to the <code>destRect</code> parameter if it is non-<code>null</code>, or
     *     a new <code>float</code> array otherwise.
     */
    public float[] warpSparseRect(
            int x0, int y0, int width, int height, int periodX, int periodY, float[] destRect) {

        // XXX: This method should do its calculations in doubles
        if (destRect == null) {
            destRect =
                    new float
                            [((width + periodX - 1) / periodX)
                                    * ((height + periodY - 1) / periodY)
                                    * 2];
        }

        width += x0;
        height += y0;
        int index = 0; // destRect index

        double xy[] = new double[2];

        for (int y = y0; y < height; y += periodY) {
            for (int x = x0; x < width; x += periodX) {
                xy[0] = x;
                xy[1] = y;
                try {
                    mapSourcePoint(xy);
                    destRect[index++] = (float) xy[0];
                    destRect[index++] = (float) xy[1];
                } catch (TransformException e) {
                    LOGGER.log(Level.WARNING, "Error transforming {0}", xy);
                    destRect[index++] = Float.NaN; // ???
                    destRect[index++] = Float.NaN; // ???
                }
            }
        }

        return destRect;
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        // dispose iterator
        if (iter != null) {
            iter.done();
        }

        // remove from cache
        // TODO improve cache management
        JAI.getDefaultInstance().getTileCache().removeTiles(this);
    }
}
