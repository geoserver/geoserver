/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.geoserver.geopkg.GeoPkg.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.feature.ReprojectingFeatureCollection;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.geotools.referencing.CRS;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * WFS GetFeature OutputFormat for GeoPackage
 *
 * @author Niels Charlier
 */
public class GeoPackageGetFeatureOutputFormat extends WFSGetFeatureOutputFormat {

    public static final String PROPERTY_INDEXED = "geopackage.wfs.indexed";

    public GeoPackageGetFeatureOutputFormat(GeoServer gs) {
        super(gs, Sets.union(Sets.newHashSet(MIME_TYPES), Sets.newHashSet(NAMES)));
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return MIME_TYPE;
    }

    @Override
    public String getCapabilitiesElementName() {
        return NAMES.iterator().next();
    }

    @Override
    public List<String> getCapabilitiesElementNames() {
        return Lists.newArrayList(NAMES);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return EXTENSION;
    }

    /**
     * According to GeoPKG spec, the coordinates MUST be in XY order. If this FC is in YX format, we
     * reproject to the equivalent XY project.
     *
     * <p>If already XY, return the input FC.
     *
     * @param fc underlying feature collection
     * @return feature collection which is has axis order in XY (NORTH_EAST)
     */
    public static SimpleFeatureCollection forceXY(SimpleFeatureCollection fc) {
        CoordinateReferenceSystem sourceCRS = fc.getSchema().getCoordinateReferenceSystem();
        if ((CRS.getAxisOrder(sourceCRS) == CRS.AxisOrder.EAST_NORTH)
                || (CRS.getAxisOrder(sourceCRS) == CRS.AxisOrder.INAPPLICABLE)) {
            return fc;
        }

        for (ReferenceIdentifier identifier : sourceCRS.getIdentifiers()) {
            try {
                String _identifier = identifier.toString();
                CoordinateReferenceSystem flippedCRS = CRS.decode(_identifier, true);
                if (CRS.getAxisOrder(flippedCRS) == CRS.AxisOrder.EAST_NORTH) {
                    ReprojectingFeatureCollection result =
                            new ReprojectingFeatureCollection(fc, flippedCRS);
                    result.setDefaultSource(sourceCRS);
                    return result;
                }
            } catch (Exception e) {
                // couldn't flip - try again
            }
        }
        return fc;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {

        File file = File.createTempFile("geopkg", ".tmp.gpkg");
        GeoPackage geopkg = GeoPkg.getGeoPackage(file);

        for (FeatureCollection collection : featureCollection.getFeatures()) {

            FeatureEntry e = new FeatureEntry();

            if (!(collection instanceof SimpleFeatureCollection)) {
                throw new ServiceException(
                        "GeoPackage OutputFormat does not support Complex Features.");
            }

            SimpleFeatureCollection features = (SimpleFeatureCollection) collection;

            features = forceXY(features); // geopkg requires data in XY orientation

            FeatureTypeInfo meta = lookupFeatureType(features);
            if (meta != null) {
                // initialize entry metadata
                e.setIdentifier(meta.getTitle());
                e.setDescription(abstractOrDescription(meta));
            }

            geopkg.add(e, features);

            if (!"false".equals(System.getProperty(PROPERTY_INDEXED))) {
                geopkg.createSpatialIndex(e);
            }
        }

        geopkg.close();

        // write to output and delete temporary file
        InputStream temp = new FileInputStream(geopkg.getFile());
        IOUtils.copy(temp, output);
        output.flush();
        temp.close();
        geopkg.getFile().delete();
    }

    FeatureTypeInfo lookupFeatureType(SimpleFeatureCollection features) {
        FeatureType featureType = features.getSchema();
        if (featureType != null) {
            Catalog cat = gs.getCatalog();
            FeatureTypeInfo meta = cat.getFeatureTypeByName(featureType.getName());
            if (meta != null) {
                return meta;
            }

            LOGGER.fine("Unable to load feature type metadata for: " + featureType.getName());
        } else {
            LOGGER.fine("No feature type for collection, unable to load metadata");
        }

        return null;
    }

    String abstractOrDescription(FeatureTypeInfo meta) {
        return meta.getAbstract() != null ? meta.getAbstract() : meta.getDescription();
    }
}
