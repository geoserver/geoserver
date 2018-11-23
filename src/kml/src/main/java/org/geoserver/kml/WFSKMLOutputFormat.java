/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.kml.decorator.KmlDecoratorFactory.KmlDecorator;
import org.geoserver.kml.iterator.IteratorList;
import org.geoserver.kml.iterator.WFSFeatureIteratorFactory;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class WFSKMLOutputFormat extends WFSGetFeatureOutputFormat {

    private KMLEncoder encoder;

    public WFSKMLOutputFormat(KMLEncoder encoder, GeoServer gs) {
        super(
                gs,
                new HashSet(
                        Arrays.asList(
                                new String[] {
                                    "KML",
                                    KMLMapOutputFormat.MIME_TYPE,
                                    // added this one to allow people copying and pasting the format
                                    // name
                                    KMLMapOutputFormat.MIME_TYPE.replace('+', ' ')
                                })));
        this.encoder = encoder;
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return KMLMapOutputFormat.MIME_TYPE;
    }

    @Override
    protected void write(
            FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {

        // prepare the encoding context
        List<SimpleFeatureCollection> collections = getFeatureCollections(featureCollection);
        KmlEncodingContext context =
                new WFSKmlEncodingContext(gs.getService(WFSInfo.class), collections);

        // create the document
        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();

        // get the callbacks for the document and let them loose
        List<KmlDecorator> docDecorators = context.getDecoratorsForClass(Document.class);
        for (KmlDecorator decorator : docDecorators) {
            document = (Document) decorator.decorate(document, context);
            if (document == null) {
                throw new ServiceException(
                        "Coding error in decorator "
                                + decorator
                                + ", document objects cannot be set to null");
            }
        }

        // build the contents
        for (SimpleFeatureCollection collection : collections) {
            // create the folder
            SimpleFeatureCollection fc = (SimpleFeatureCollection) collection;
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
            List<Feature> features =
                    new IteratorList<Feature>(new WFSFeatureIteratorFactory(context));
            context.addFeatures(folder, features);
        }

        // write out the output
        encoder.encode(kml, output, context);
    }

    private List<SimpleFeatureCollection> getFeatureCollections(
            FeatureCollectionResponse featureCollection) {
        List<FeatureCollection> inputs = featureCollection.getFeatures();
        List<SimpleFeatureCollection> result = new ArrayList<SimpleFeatureCollection>();
        for (FeatureCollection fc : inputs) {
            if (!(fc instanceof SimpleFeatureCollection)) {
                throw new ServiceException(
                        "The KML output format can only be applied to simple features");
            }

            // KML is defined only over wgs84 in lon/lat order, ignore what the WFS thinks the
            // output
            // crs should be
            SimpleFeatureCollection sfc = (SimpleFeatureCollection) fc;
            CoordinateReferenceSystem sourceCRS = sfc.getSchema().getCoordinateReferenceSystem();
            if (sourceCRS != null
                    && !CRS.equalsIgnoreMetadata(sourceCRS, DefaultGeographicCRS.WGS84)) {
                sfc = new ReprojectingFeatureCollection(sfc, DefaultGeographicCRS.WGS84);
            }
            result.add(sfc);
        }

        return result;
    }

    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        GetFeatureRequest request = GetFeatureRequest.adapt(operation.getParameters()[0]);
        String outputFileName = request.getQueries().get(0).getTypeNames().get(0).getLocalPart();
        return outputFileName + ".kml";
    }

    @Override
    public String getCapabilitiesElementName() {
        return "KML";
    }

    /**
     * A special KML encoding context for the WFS case
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class WFSKmlEncodingContext extends KmlEncodingContext {

        private List<SimpleFeatureCollection> collections;
        private SimpleFeatureType featureType;

        public WFSKmlEncodingContext(ServiceInfo si, List<SimpleFeatureCollection> collections) {
            this.service = getService();
            this.collections = collections;

            // set some defaults for wfs encoding
            this.descriptionEnabled = false;
            this.kmScore = 100;
            this.extendedDataEnabled = true;
            this.kmz = false;
        }

        public List<SimpleFeatureType> getFeatureTypes() {
            List<SimpleFeatureType> results = new ArrayList<SimpleFeatureType>();
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
}
