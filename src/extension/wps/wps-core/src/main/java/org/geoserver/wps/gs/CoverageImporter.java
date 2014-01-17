package org.geoserver.wps.gs;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.media.jai.Interpolation;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wps.WPSException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.process.ProcessException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.vfny.geoserver.util.WCSUtils;

public class CoverageImporter {

    static final Logger LOGGER = Logging.getLogger(CoverageImporter.class);

    private final static GeoTiffWriteParams DEFAULT_WRITE_PARAMS;

    static {
        // setting the write parameters (we my want to make these configurable in the future
        DEFAULT_WRITE_PARAMS = new GeoTiffWriteParams();
        DEFAULT_WRITE_PARAMS.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setCompressionType("LZW");
        DEFAULT_WRITE_PARAMS.setCompressionQuality(0.75F);
        DEFAULT_WRITE_PARAMS.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        DEFAULT_WRITE_PARAMS.setTiling(512, 512);
    }

    public Catalog catalog;

    public CoverageImporter(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Creates a new Coverage Layer and assign the 'targetStyle' to it.
     * 
     * @param coverage
     * @param name
     * @param cb
     * @param ws
     * @param storeInfo
     * @param srs
     * @param srsHandling
     * @param targetStyle
     * @return
     * @throws ProcessException
     */
    public String execute(GridCoverage2D coverage, String name, CatalogBuilder cb,
            WorkspaceInfo ws, StoreInfo storeInfo, CoordinateReferenceSystem srs,
            ProjectionPolicy srsHandling, StyleInfo targetStyle) throws ProcessException {
        try {
            final File directory = this.catalog.getResourceLoader().findOrCreateDirectory("data",
                    ws.getName(), storeInfo.getName());
            final File file = File.createTempFile(storeInfo.getName(), ".tif", directory);
            ((CoverageStoreInfo) storeInfo).setURL(file.toURL().toExternalForm());
            ((CoverageStoreInfo) storeInfo).setType("GeoTIFF");

            // check the target crs
            CoordinateReferenceSystem cvCrs = coverage.getCoordinateReferenceSystem();
            String targetSRSCode = null;
            if (srs != null) {
                try {
                    Integer code = CRS.lookupEpsgCode(srs, true);
                    if (code == null) {
                        throw new WPSException("Could not find a EPSG code for " + srs);
                    }
                    targetSRSCode = "EPSG:" + code;
                } catch (Exception e) {
                    throw new ProcessException(
                            "Could not lookup the EPSG code for the provided srs", e);
                }
            } else {
                // check we can extract a code from the original data
                if (cvCrs == null) {
                    // data is geometryless, we need a fake SRS
                    targetSRSCode = "EPSG:4326";
                    srsHandling = ProjectionPolicy.FORCE_DECLARED;
                    srs = DefaultGeographicCRS.WGS84;
                } else {
                    CoordinateReferenceSystem nativeCrs = cvCrs;
                    if (nativeCrs == null) {
                        throw new ProcessException("The original data has no native CRS, "
                                + "you need to specify the srs parameter");
                    } else {
                        try {
                            Integer code = CRS.lookupEpsgCode(nativeCrs, true);
                            if (code == null) {
                                throw new ProcessException("Could not find an EPSG code for data "
                                        + "native spatial reference system: " + nativeCrs);
                            } else {
                                targetSRSCode = "EPSG:" + code;
                                srs = CRS.decode(targetSRSCode, true);
                            }
                        } catch (Exception e) {
                            throw new ProcessException(
                                    "Failed to loookup an official EPSG code for "
                                            + "the source data native "
                                            + "spatial reference system", e);
                        }
                    }
                }
            }

            MathTransform tx = CRS.findMathTransform(cvCrs, srs);

            if (!tx.isIdentity() || !CRS.equalsIgnoreMetadata(cvCrs, srs)) {
                coverage = WCSUtils.resample(coverage, cvCrs, srs, null,
                        Interpolation.getInstance(Interpolation.INTERP_NEAREST));
            }

            GeoTiffWriter writer = new GeoTiffWriter(file);

            // setting the write parameters for this geotiff
            final ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
            params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                    .setValue(DEFAULT_WRITE_PARAMS);
            final GeneralParameterValue[] wps = (GeneralParameterValue[]) params.values().toArray(
                    new GeneralParameterValue[1]);

            try {
                writer.write(coverage, wps);
            } finally {
                try {
                    writer.dispose();
                } catch (Exception e) {
                    // we tried, no need to fuss around this one
                }
            }

            // add or update the datastore info
            // if (add) {
            this.catalog.add((CoverageStoreInfo) storeInfo);
            /*
             * } else { this.catalog.save((CoverageStoreInfo) storeInfo); }
             */

            cb.setStore((CoverageStoreInfo) storeInfo);

            GridCoverage2DReader reader = new GeoTiffReader(file);
            if (reader == null) {
                throw new ProcessException("Could not aquire reader for coverage.");
            }

            // coverage read params
            final Map customParameters = new HashMap();
            /*
             * String useJAIImageReadParam = "USE_JAI_IMAGEREAD"; if (useJAIImageReadParam != null) {
             * customParameters.put(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString(), Boolean.valueOf(useJAIImageReadParam)); }
             */

            CoverageInfo cinfo = cb.buildCoverage(reader, customParameters);

            // check if the name of the coverage was specified
            if (name != null) {
                cinfo.setName(name);
            }

            // do some post configuration, if srs is not known or unset, transform to 4326
            if ("UNKNOWN".equals(cinfo.getSRS())) {
                // CoordinateReferenceSystem sourceCRS = cinfo.getBoundingBox().getCoordinateReferenceSystem();
                // CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326", true);
                // ReferencedEnvelope re = cinfo.getBoundingBox().transform(targetCRS, true);
                cinfo.setSRS("EPSG:4326");
                // cinfo.setCRS( targetCRS );
                // cinfo.setBoundingBox( re );
            }

            // add/save
            this.catalog.add(cinfo);

            LayerInfo layerInfo = cb.buildLayer(cinfo);
            if (targetStyle != null) {
                layerInfo.setDefaultStyle(targetStyle);
            }
            // JD: commenting this out, these sorts of edits should be handled
            // with a second PUT request on the created coverage
            /*
             * String styleName = form.getFirstValue("style"); if ( styleName != null ) { StyleInfo style = catalog.getStyleByName( styleName ); if (
             * style != null ) { layerInfo.setDefaultStyle( style ); if ( !layerInfo.getStyles().contains( style ) ) { layerInfo.getStyles().add(
             * style ); } } else { LOGGER.warning( "Client specified style '" + styleName + "'but no such style exists."); } }
             * 
             * String path = form.getFirstValue( "path"); if ( path != null ) { layerInfo.setPath( path ); }
             */

            boolean valid = true;
            try {
                if (!this.catalog.validate(layerInfo, true).isEmpty()) {
                    valid = false;
                }
            } catch (Exception e) {
                valid = false;
            }

            layerInfo.setEnabled(valid);
            this.catalog.add(layerInfo);

            return layerInfo.prefixedName();

        } catch (MalformedURLException e) {
            throw new ProcessException("URL Error", e);
        } catch (IOException e) {
            throw new ProcessException("I/O Exception", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ProcessException("Exception", e);
        }
    }
}