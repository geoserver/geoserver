/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * This class is used for managing ROI and its CRS. ROIManager provides utility method like
 * reprojecting the ROI in the desired CRS.
 *
 * @author Simone Giannecchini, GeoSolutions
 */
final class ROIManager {

    private static final Logger LOGGER = Logging.getLogger(ROIManager.class);

    /** Input Geometry */
    final Geometry originalRoi;

    /** ROI reprojected in the input ROI CRS */
    Geometry roiInNativeCRS;

    /** ROI reprojected in the native ROI CRS (reduced to envelope if possible) */
    Geometry safeRoiInNativeCRS;

    /** ROI native CRS */
    CoordinateReferenceSystem nativeCRS;

    /** ROI reprojected in the target CRS */
    Geometry roiInTargetCRS;

    /** ROI reprojected in the target CRS (reduced to envelope if possible) */
    Geometry safeRoiInTargetCRS;

    /** Initial ROI CRS */
    final CoordinateReferenceSystem roiCRS;

    /** ROI target CRS */
    CoordinateReferenceSystem targetCRS;

    /** Boolean indicating if the ROI is a BBOX */
    final boolean isROIBBOX;

    /** Boolean indicating if the roiCRS equals the targetCRS */
    boolean roiCrsEqualsTargetCrs = true;

    /**
     * Constructor.
     *
     * @param roi original ROI as a JTS geometry
     * @param roiCRS {@link CoordinateReferenceSystem} for the provided geometry. If this is null
     *     the CRS must be provided with the USerData of the roi
     */
    public ROIManager(Geometry roi, CoordinateReferenceSystem roiCRS) {
        this.originalRoi = roi;
        DownloadUtilities.checkPolygonROI(roi);
        // Check ROI CRS
        if (roiCRS == null) {
            if (!(roi.getUserData() instanceof CoordinateReferenceSystem)) {
                throw new IllegalArgumentException("ROI without a CRS is not usable!");
            }
            this.roiCRS = (CoordinateReferenceSystem) roi.getUserData();
        } else {
            this.roiCRS = roiCRS;
        }
        roi.setUserData(this.roiCRS);
        // is this a bbox
        isROIBBOX = roi.isRectangle();
    }

    /**
     * Reproject the initial roi to the provided CRS which is supposedly the native CRS of the data
     * to clip.
     *
     * @param nativeCRS a valid instance of {@link CoordinateReferenceSystem}
     * @throws IOException in case something bad happens.
     */
    public void useNativeCRS(final CoordinateReferenceSystem nativeCRS) throws IOException {
        if (nativeCRS == null) {
            throw new IllegalArgumentException("The provided nativeCRS is null");
        }
        roiInNativeCRS = DownloadUtilities.transformGeometry(originalRoi, nativeCRS);
        DownloadUtilities.checkPolygonROI(roiInNativeCRS);
        if (isROIBBOX) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "ROI is a Bounding Box");
            }
            // if the ROI is a BBOX we tend to preserve the fact that it is a BBOX
            safeRoiInNativeCRS = roiInNativeCRS.getEnvelope();
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "ROI is not a Bounding Box");
            }
            safeRoiInNativeCRS = roiInNativeCRS;
        }
        safeRoiInNativeCRS.setUserData(nativeCRS);
        this.nativeCRS = nativeCRS;
    }

    /**
     * Reproject the initial roi to the provided CRS which is supposedly the target CRS as per the
     * request.
     *
     * <p>This method should be called once the native CRS has been set, that is the {@link
     * #useNativeCRS(CoordinateReferenceSystem)} has been called.
     *
     * @param targetCRS a valid instance of {@link CoordinateReferenceSystem}
     * @throws IOException in case something bad happens.
     */
    public void useTargetCRS(final CoordinateReferenceSystem targetCRS)
            throws IOException, FactoryException {
        if (targetCRS == null) {
            throw new IllegalArgumentException("The provided targetCRS is null");
        }
        if (roiInNativeCRS == null) {
            throw new IllegalStateException("It looks like useNativeCRS has not been called yet");
        }
        this.targetCRS = targetCRS;
        if (!CRS.equalsIgnoreMetadata(roiCRS, targetCRS)) {

            MathTransform reprojectionTrasform = CRS.findMathTransform(roiCRS, targetCRS, true);
            if (!reprojectionTrasform.isIdentity()) {
                // avoid doing the transform if this is the identity
                roiCrsEqualsTargetCrs = false;
            }
        }

        if (isROIBBOX) {
            // we need to use a larger bbox in native CRS
            roiInTargetCRS = DownloadUtilities.transformGeometry(safeRoiInNativeCRS, targetCRS);
            DownloadUtilities.checkPolygonROI(roiInTargetCRS);
            safeRoiInTargetCRS = roiInTargetCRS.getEnvelope();
            safeRoiInTargetCRS.setUserData(targetCRS);

            // Back to the minimal roiInTargetCrs for future clipping if needed.
            roiInTargetCRS =
                    roiCrsEqualsTargetCrs
                            ? originalRoi
                            : DownloadUtilities.transformGeometry(originalRoi, targetCRS);

            // touch safeRoiInNativeCRS
            safeRoiInNativeCRS = DownloadUtilities.transformGeometry(safeRoiInTargetCRS, nativeCRS);
            DownloadUtilities.checkPolygonROI(safeRoiInNativeCRS);
            safeRoiInNativeCRS = safeRoiInNativeCRS.getEnvelope();
            safeRoiInNativeCRS.setUserData(nativeCRS);
        } else {
            roiInTargetCRS = DownloadUtilities.transformGeometry(roiInNativeCRS, targetCRS);
            safeRoiInTargetCRS = roiInTargetCRS;
        }
    }

    /** @return the isBBOX */
    public boolean isROIBBOX() {
        return isROIBBOX;
    }

    /** @return the roi */
    public Geometry getOriginalRoi() {
        return originalRoi;
    }

    /** @return the roiInNativeCRS */
    public Geometry getRoiInNativeCRS() {
        return roiInNativeCRS;
    }

    /** @return the safeRoiInNativeCRS */
    public Geometry getSafeRoiInNativeCRS() {
        return safeRoiInNativeCRS;
    }

    /** @return the roiInTargetCRS */
    public Geometry getRoiInTargetCRS() {
        return roiInTargetCRS;
    }

    /** @return the safeRoiInTargetCRS */
    public Geometry getSafeRoiInTargetCRS() {
        return safeRoiInTargetCRS;
    }

    /** @return the roiCRS */
    public CoordinateReferenceSystem getRoiCRS() {
        return roiCRS;
    }

    /** @return the targetCRS */
    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    public CoordinateReferenceSystem getNativeCRS() {
        return nativeCRS;
    }

    public boolean isRoiCrsEqualsTargetCrs() {
        return roiCrsEqualsTargetCrs;
    }

    public Geometry getTargetRoi(boolean clip) {
        if (clip) {
            // clipping means carefully following the ROI shape
            return isRoiCrsEqualsTargetCrs() ? originalRoi : getRoiInTargetCRS();
        } else {
            // use envelope of the ROI to simply crop and not clip the raster. This is important
            // since when reprojecting we might read a bit more than needed!
            return isRoiCrsEqualsTargetCrs()
                    ? originalRoi.getEnvelope()
                    : getSafeRoiInTargetCRS().getEnvelope();
        }
    }
}
