/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.kml.KMLEncoder;
import org.geoserver.kml.KMLMapOutputFormat;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.iterator.IteratorList;
import org.geoserver.kml.iterator.WFSFeatureIteratorFactory;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.v22.KMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.StreamingParser;
import org.locationtech.jts.geom.Geometry;

/** PPIO for KML 2.2. */
public class KMLPPIO extends CDataPPIO {
    private static final Logger LOGGER = Logging.getLogger(KMLPPIO.class);

    GeoServer gs;

    EntityResolverProvider resolverProvider;

    Configuration xml;

    SimpleFeatureType type;

    public KMLPPIO(GeoServer gs, EntityResolverProvider resolverProvider) {
        super(FeatureCollection.class, FeatureCollection.class, KMLMapOutputFormat.MIME_TYPE);

        this.gs = gs;
        this.resolverProvider = resolverProvider;
        this.xml = new KMLConfiguration();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

        b.setName("puregeometries");

        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("location", Geometry.class);

        this.type = b.buildFeatureType();
    }

    private static final Map<Name, Class<?>> getSignature(SimpleFeature f) {
        Map<Name, Class<?>> ftype = new HashMap<>();
        Collection<Property> properties = f.getProperties();
        for (Property p : properties) {
            Class<?> c = p.getType().getBinding();
            if ((c.isAssignableFrom(String.class))
                    || (c.isAssignableFrom(Boolean.class))
                    || (c.isAssignableFrom(Integer.class))
                    || (c.isAssignableFrom(Float.class))
                    || (c.isAssignableFrom(Double.class))
                    || (c.isAssignableFrom(Geometry.class))) {
                ftype.put(p.getName(), c);
            }
        }
        return ftype;
    }

    private SimpleFeatureType getType(Map<Name, Class<?>> ftype) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

        b.setName("puregeometries");

        b.setCRS(DefaultGeographicCRS.WGS84);
        for (Entry<Name, Class<?>> entry : ftype.entrySet()) {
            b.add(entry.getKey().toString(), entry.getValue());
        }
        return b.buildFeatureType();
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        StreamingParser parser = new StreamingParser(new KMLConfiguration(), input, SimpleFeature.class);
        parser.setEntityResolver(resolverProvider.getEntityResolver());
        SimpleFeature f;
        ListFeatureCollection features = null;
        Map<Name, Class<?>> oldftype = null;
        SimpleFeatureType type = null;
        SimpleFeatureBuilder featureBuilder = null;

        while ((f = (SimpleFeature) parser.parse()) != null) {
            Map<Name, Class<?>> ftype = getSignature(f);
            if (oldftype == null) {
                oldftype = ftype;
                type = getType(ftype);
                featureBuilder = new SimpleFeatureBuilder(type);
                features = new ListFeatureCollection(type);
            } else {
                if (!oldftype.equals(ftype)) {
                    break;
                }
            }
            for (Map.Entry<Name, Class<?>> entry : ftype.entrySet()) {
                featureBuilder.add(f.getAttribute((entry.getKey())));
            }
            SimpleFeature fnew = featureBuilder.buildFeature(f.getID());
            features.add(fnew);
        }
        return features;
    }

    @Override
    public Object decode(String input) throws Exception {
        return decode(new ByteArrayInputStream(input.getBytes()));
    }

    @Override
    public void encode(Object obj, OutputStream os) throws Exception {
        LOGGER.info("KMLPPIO::encode: obj is of class "
                + obj.getClass().getName()
                + ", handler is of class "
                + os.getClass().getName());

        // prepare the encoding context
        KMLEncoder encoder = new KMLEncoder();
        SimpleFeatureCollection fcObj = (SimpleFeatureCollection) obj;
        CoordinateReferenceSystem crs = fcObj.getSchema().getCoordinateReferenceSystem();
        // gpx is defined only in wgs84
        if (crs != null && !CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
            fcObj = new ReprojectingFeatureCollection(fcObj, DefaultGeographicCRS.WGS84);
        }

        List<SimpleFeatureCollection> collections = new ArrayList<>();
        collections.add(fcObj);
        KmlEncodingContext context = new WFSKmlEncodingContext(collections);

        // create the document
        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();

        // get the callbacks for the document and let them loose
        List<KmlDecorator> docDecorators = context.getDecoratorsForClass(Document.class);
        for (KmlDecorator decorator : docDecorators) {
            document = (Document) decorator.decorate(document, context);
            if (document == null) {
                throw new ServiceException(
                        "Coding error in decorator " + decorator + ", document objects cannot be set to null");
            }
        }

        // build the contents
        for (SimpleFeatureCollection fc : collections) {
            // create the folder
            Folder folder = document.createAndAddFolder();
            folder.setName(fc.getSchema().getTypeName());

            // have it be decorated
            List<KmlDecorator> folderDecorators = context.getDecoratorsForClass(Folder.class);
            for (KmlDecorator decorator : folderDecorators) {
                folder = (Folder) decorator.decorate(folder, context);
                if (folder == null) {
                    break;
                }
            }
            if (folder == null) {
                continue;
            }

            // create the streaming features
            context.setCurrentFeatureCollection(fc);
            List<Feature> features = new IteratorList<>(new WFSFeatureIteratorFactory(context));
            context.addFeatures(folder, features);
        }

        // write out the output
        encoder.encode(kml, os, context);
        os.flush();
    }

    /**
     * A special KML encoding context for the WFS case
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class WFSKmlEncodingContext extends KmlEncodingContext {

        private List<SimpleFeatureCollection> collections;

        private SimpleFeatureType featureType;

        public WFSKmlEncodingContext(List<SimpleFeatureCollection> collections) {
            this.service = getService();
            this.collections = collections;

            // set some defaults for wfs encoding
            this.descriptionEnabled = false;
            this.kmScore = 100;
            this.extendedDataEnabled = true;
            this.kmz = false;
        }

        @Override
        public List<SimpleFeatureType> getFeatureTypes() {
            List<SimpleFeatureType> results = new ArrayList<>();
            for (SimpleFeatureCollection fc : collections) {
                results.add(fc.getSchema());
            }

            return results;
        }

        @Override
        public void setCurrentFeatureCollection(SimpleFeatureCollection currentFeatureCollection) {
            super.setCurrentFeatureCollection(currentFeatureCollection);
            this.layerIndex++;
            this.featureType = currentFeatureCollection.getSchema();
        }

        @Override
        public SimpleFeatureType getCurrentFeatureType() {
            return featureType;
        }
    }

    @Override
    public String getFileExtension() {
        return "kml";
    }
}
