/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gml3.GML;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geotools.wfs.v1_0.WFSConfiguration_1_0;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Encoder;
import org.geotools.xsd.Parser;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.ContentHandler;

/** Allows reading and writing a WFS feature collection */
public class WFSPPIO extends XMLPPIO {

    Configuration configuration;
    private static final Logger LOGGER = Logging.getLogger(WFSPPIO.class);
    private static final String METADATA = GML.metaDataProperty.getLocalPart();
    private static final String BOUNDEDBY = GML.boundedBy.getLocalPart();
    private static final String LOCATION = GML.location.getLocalPart();

    protected WFSPPIO(Configuration configuration, String mimeType, QName element) {
        super(FeatureCollectionType.class, FeatureCollection.class, mimeType, element);
        this.configuration = configuration;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Parser p = getParser(configuration);
        byte[] streamBytes = null;
        if (LOGGER.isLoggable(Level.FINEST)) {
            // allow WFS result to be logged for debugging purposes
            // WFS result can be large, so use only for debugging
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteStreams.copy(input, outputStream);
            streamBytes = outputStream.toByteArray();
            input = new ByteArrayInputStream(streamBytes);
        }
        Object result = p.parse(input);
        if (result instanceof FeatureCollectionType) {
            FeatureCollectionType fct = (FeatureCollectionType) result;
            return decode(fct);
        } else {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(
                        Level.FINEST,
                        "Decoding the following WFS response did not result in an object of type FeatureCollectionType: \n"
                                + new String(streamBytes));
            }
            throw new IllegalArgumentException(
                    "Decoded WFS result is not a feature collection, got a: " + result);
        }
    }

    @Override
    public Object decode(Object input) throws Exception {
        // xml parsing will most likely return it as parsed already, but if CDATA is used or if
        // it's a KVP parse it will be a string instead
        if (input instanceof String) {
            Parser p = getParser(configuration);
            input = p.parse(new StringReader((String) input));
        }

        // cast and handle the axis flipping
        FeatureCollectionType fct = (FeatureCollectionType) input;
        SimpleFeatureCollection fc = (SimpleFeatureCollection) fct.getFeature().get(0);
        // Axis flipping issue, we should determine if the collection needs flipping
        if (fc.getSchema().getGeometryDescriptor() != null) {
            CoordinateReferenceSystem crs = getCollectionCRS(fc);
            if (crs != null) {
                // do we need to force the crs onto the collection?
                CoordinateReferenceSystem nativeCrs =
                        fc.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
                if (nativeCrs == null) {
                    // we need crs forcing
                    fc = new ForceCoordinateSystemFeatureResults(fc, crs, false);
                }

                // we assume the crs has a valid EPSG code
                Integer code = CRS.lookupEpsgCode(crs, false);
                if (code != null) {
                    CoordinateReferenceSystem lonLatCrs = CRS.decode("EPSG:" + code, true);
                    if (!CRS.equalsIgnoreMetadata(crs, lonLatCrs)) {
                        // we need axis flipping
                        fc = new ReprojectingFeatureCollection(fc, lonLatCrs);
                    }
                }
            }
        }

        return eliminateFeatureBounds(fc);
    }

    /**
     * Parsing GML we often end up with empty attributes that will break shapefiles and common
     * processing algorithms because they introduce bounding boxes (boundedBy) or hijack the default
     * geometry property (location). We sanitize the collection in this method by removing them. It
     * is not the best approach, but works in most cases, whilst not doing it would break the code
     * in most cases. Would be better to find a more general approach...
     */
    private SimpleFeatureCollection eliminateFeatureBounds(SimpleFeatureCollection fc) {
        final SimpleFeatureType original = fc.getSchema();

        List<String> names = new ArrayList<String>();
        boolean alternateGeometry = false;
        for (AttributeDescriptor ad : original.getAttributeDescriptors()) {
            final String name = ad.getLocalName();

            if (!BOUNDEDBY.equals(name) && !METADATA.equals(name)) {
                names.add(name);
            }
            if (!LOCATION.equals(name) && ad instanceof GeometryDescriptor) {
                alternateGeometry = true;
            }
        }
        // if there is another geometry we assume "location" is going to be empty,
        // otherwise we're going to get always a null geometry
        if (alternateGeometry) {
            names.remove("location");
        }

        if (names.size() < original.getDescriptors().size()) {
            String[] namesArray = names.toArray(new String[names.size()]);
            SimpleFeatureType target = SimpleFeatureTypeBuilder.retype(original, namesArray);
            return new RetypingFeatureCollection(fc, target);
        }
        return fc;
    }

    /** Gets the collection CRS, either from metadata or by scanning the collection contents */
    CoordinateReferenceSystem getCollectionCRS(SimpleFeatureCollection fc) throws Exception {
        // this is unlikely to work for remote or embedded collections, but it's also easy to check
        if (fc.getSchema().getCoordinateReferenceSystem() != null) {
            return fc.getSchema().getCoordinateReferenceSystem();
        }

        // ok, let's scan the entire collection then...
        CoordinateReferenceSystem crs = null;
        try (SimpleFeatureIterator fi = fc.features()) {
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                CoordinateReferenceSystem featureCrs = null;
                GeometryDescriptor gd = f.getType().getGeometryDescriptor();
                if (gd != null && gd.getCoordinateReferenceSystem() != null) {
                    featureCrs = gd.getCoordinateReferenceSystem();
                }
                if (f.getDefaultGeometry() != null) {
                    Geometry g = (Geometry) f.getDefaultGeometry();
                    if (g.getUserData() instanceof CoordinateReferenceSystem) {
                        featureCrs = (CoordinateReferenceSystem) g.getUserData();
                    }
                }

                // collect the feature crs, if it's new, use it, otherwise
                // check the collection does not have mixed crs
                if (featureCrs != null) {
                    if (crs == null) {
                        crs = featureCrs;
                    } else if (!CRS.equalsIgnoreMetadata(featureCrs, crs)) {
                        return null;
                    }
                }
            }
        }

        return crs;
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        FeatureCollection features = (FeatureCollection) object;
        SimpleFeatureType featureType = (SimpleFeatureType) features.getSchema();

        FeatureCollectionType fc = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fc.getFeature().add(features);

        Encoder e = new Encoder(configuration);
        e.getNamespaces().declarePrefix("feature", featureType.getName().getNamespaceURI());
        e.encode(fc, getElement(), handler);
    }

    public static class WFS10 extends WFSPPIO {

        public WFS10() {
            super(
                    new WFSConfiguration_1_0(),
                    "text/xml; subtype=wfs-collection/1.0",
                    org.geoserver.wfs.xml.v1_0_0.WFS.FEATURECOLLECTION);
        }
    }

    /**
     * A WFS 1.0 PPIO using alternate MIME type without a ";" that creates troubles in KVP parsing
     */
    public static class WFS10Alternate extends WFSPPIO {

        public WFS10Alternate() {
            super(
                    new WFSConfiguration_1_0(),
                    "application/wfs-collection-1.0",
                    org.geoserver.wfs.xml.v1_0_0.WFS.FEATURECOLLECTION);
        }
    }

    public static class WFS11 extends WFSPPIO {

        public WFS11() {
            super(
                    new org.geotools.wfs.v1_1.WFSConfiguration(),
                    "text/xml; subtype=wfs-collection/1.1",
                    org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION);
        }
    }

    /**
     * A WFS 1.1 PPIO using alternate MIME type without a ";" that creates troubles in KVP parsing
     */
    public static class WFS11Alternate extends WFSPPIO {

        public WFS11Alternate() {
            super(
                    new org.geotools.wfs.v1_1.WFSConfiguration(),
                    "application/wfs-collection-1.1",
                    org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION);
        }
    }
}
