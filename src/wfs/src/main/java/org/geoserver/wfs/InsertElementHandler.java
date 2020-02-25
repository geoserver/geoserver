/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.feature.ReprojectingFeatureCollection;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.Insert;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml3.v3_2.GML;
import org.geotools.referencing.operation.projection.PointOutsideEnvelopeException;
import org.geotools.util.Version;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Handler for the insert element
 *
 * @author Andrea Aime - TOPP
 */
public class InsertElementHandler extends AbstractTransactionElementHandler {
    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    private FilterFactory filterFactory;

    public InsertElementHandler(GeoServer gs, FilterFactory filterFactory) {
        super(gs);
        this.filterFactory = filterFactory;
    }

    public void checkValidity(
            TransactionElement element, Map<QName, FeatureTypeInfo> featureTypeInfos)
            throws WFSTransactionException {
        if (!getInfo().getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_INSERT)) {
            throw new WFSException(element, "Transaction INSERT support is not enabled");
        }
    }

    @SuppressWarnings("unchecked")
    public void execute(
            TransactionElement element,
            TransactionRequest request,
            Map featureStores,
            TransactionResponse response,
            TransactionListener listener)
            throws WFSTransactionException {

        Insert insert = (Insert) element;
        LOGGER.finer("Transaction Insert:" + insert);

        long inserted = response.getTotalInserted().longValue();

        try {
            // group features by their schema
            HashMap /* <SimpleFeatureType,FeatureCollection> */ schema2features = new HashMap();

            List featureList = insert.getFeatures();
            for (Iterator f = featureList.iterator(); f.hasNext(); ) {
                SimpleFeature feature = (SimpleFeature) f.next();
                SimpleFeatureType schema = feature.getFeatureType();
                ListFeatureCollection collection =
                        (ListFeatureCollection) schema2features.get(schema);

                if (collection == null) {
                    collection = new ListFeatureCollection(schema);
                    schema2features.put(schema, collection);
                }

                // do a check for idegen = useExisting, if set try to tell the datastore to use
                // the provided fid
                if (insert.isIdGenUseExisting()) {
                    feature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                } else {
                    Object identifier =
                            feature.getAttribute(new NameImpl(GML.NAMESPACE, "identifier"));
                    if (WFSInfo.Version.V_20.compareTo(insert.getVersion()) >= 0
                            && identifier instanceof String) {
                        SimpleFeatureBuilder fb =
                                new SimpleFeatureBuilder(feature.getFeatureType());
                        fb.init(feature);
                        feature = fb.buildFeature((String) identifier);
                        feature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                    }
                }

                collection.add(feature);
            }

            // JD: change from set fo list because if inserting
            // features into different feature stores, they could very well
            // get given the same id
            // JD: change from list to map so that the map can later be
            // processed and we can report the fids back in the same order
            // as they were supplied
            Map<String, List<FeatureId>> schema2fids = new HashMap<String, List<FeatureId>>();

            for (Iterator c = schema2features.values().iterator(); c.hasNext(); ) {
                SimpleFeatureCollection collection = (SimpleFeatureCollection) c.next();
                SimpleFeatureType schema = collection.getSchema();

                final QName elementName =
                        new QName(schema.getName().getNamespaceURI(), schema.getTypeName());
                SimpleFeatureStore store;
                store = DataUtilities.simple((FeatureStore) featureStores.get(elementName));

                if (store == null) {
                    throw new WFSException(
                            request, "Could not locate FeatureStore for '" + elementName + "'");
                }

                if (collection != null) {
                    // if we really need to, make sure we are inserting coordinates that do
                    // match the CRS area of validity
                    if (getInfo().isCiteCompliant()) {
                        checkFeatureCoordinatesRange(collection);
                    }

                    // reprojection
                    final GeometryDescriptor defaultGeometry =
                            store.getSchema().getGeometryDescriptor();
                    if (defaultGeometry != null) {
                        CoordinateReferenceSystem target =
                                defaultGeometry.getCoordinateReferenceSystem();
                        if (target
                                != null /* && !CRS.equalsIgnoreMetadata(collection.getSchema().getCoordinateReferenceSystem(), target) */) {
                            collection = new ReprojectingFeatureCollection(collection, target);
                        }
                    }

                    // Need to use the namespace here for the
                    // lookup, due to our weird
                    // prefixed internal typenames. see
                    // https://osgeo-org.atlassian.net/browse/GEOS-143

                    // Once we get our datastores making features
                    // with the correct namespaces
                    // we can do something like this:
                    // FeatureTypeInfo typeInfo =
                    // catalog.getFeatureTypeInfo(schema.getTypeName(),
                    // schema.getNamespace());
                    // until then (when geos-144 is resolved) we're
                    // stuck with:
                    // QName qName = (QName) typeNames.get( i );
                    // FeatureTypeInfo typeInfo =
                    // catalog.featureType( qName.getPrefix(),
                    // qName.getLocalPart() );

                    // this is possible with the insert hack above.
                    LOGGER.finer("Use featureValidation to check contents of insert");

                    // featureValidation(
                    // typeInfo.getDataStore().getId(), schema,
                    // collection );
                    List<FeatureId> fids = schema2fids.get(schema.getTypeName());

                    if (fids == null) {
                        fids = new LinkedList<FeatureId>();
                        schema2fids.put(schema.getTypeName(), fids);
                    }

                    // fire pre insert event
                    TransactionEvent event =
                            new TransactionEvent(
                                    TransactionEventType.PRE_INSERT,
                                    request,
                                    elementName,
                                    collection);
                    event.setSource(Insert.WFS11.unadapt(insert));

                    listener.dataStoreChange(event);
                    fids.addAll(store.addFeatures(collection));

                    // fire post insert event
                    SimpleFeatureCollection features =
                            store.getFeatures(filterFactory.id(new HashSet<FeatureId>(fids)));
                    event =
                            new TransactionEvent(
                                    TransactionEventType.POST_INSERT,
                                    request,
                                    elementName,
                                    features,
                                    Insert.WFS11.unadapt(insert));
                    listener.dataStoreChange(event);
                }
            }

            // report back fids, we need to keep the same order the
            // fids were reported in the original feature collection
            for (Iterator f = featureList.iterator(); f.hasNext(); ) {
                SimpleFeature feature = (SimpleFeature) f.next();
                SimpleFeatureType schema = feature.getFeatureType();

                // get the next fid
                LinkedList<FeatureId> fids =
                        (LinkedList<FeatureId>) schema2fids.get(schema.getTypeName());
                FeatureId fid = fids.removeFirst();

                response.addInsertedFeature(insert.getHandle(), fid);
            }

            // update the insert counter
            inserted += featureList.size();
        } catch (Exception e) {
            String msg = "Error performing insert: " + e.getMessage();
            throw new WFSTransactionException(msg, e, insert.getHandle());
        }

        // update transaction summary
        response.setTotalInserted(BigInteger.valueOf(inserted));
    }

    /** Checks that all features coordinates are within the expected coordinate range */
    void checkFeatureCoordinatesRange(SimpleFeatureCollection collection)
            throws PointOutsideEnvelopeException {
        List types = collection.getSchema().getAttributeDescriptors();
        SimpleFeatureIterator fi = collection.features();
        try {
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                for (int i = 0; i < types.size(); i++) {
                    if (types.get(i) instanceof GeometryDescriptor) {
                        GeometryDescriptor gat = (GeometryDescriptor) types.get(i);
                        if (gat.getCoordinateReferenceSystem() != null) {
                            Geometry geom = (Geometry) f.getAttribute(i);
                            if (geom != null)
                                JTS.checkCoordinatesRange(geom, gat.getCoordinateReferenceSystem());
                        }
                    }
                }
            }
        } finally {
            fi.close();
        }
    }

    public Class getElementClass() {
        return Insert.class;
    }

    public QName[] getTypeNames(TransactionRequest request, TransactionElement element)
            throws WFSTransactionException {
        Insert insert = (Insert) element;

        List typeNames = new ArrayList();

        List features = insert.getFeatures();
        if (!features.isEmpty()) {
            for (Iterator f = features.iterator(); f.hasNext(); ) {
                Object next = f.next();
                // if parsing fails the parser just returns a Map, do throw an error in this case
                if (!(next instanceof SimpleFeature)) {
                    String version = request.getVersion();
                    String code;
                    if (version == null
                            || new Version(version).compareTo(WFSInfo.Version.V_20.getVersion())
                                    >= 0) {
                        code = WFSException.INVALID_VALUE;
                    } else {
                        code = ServiceException.INVALID_PARAMETER_VALUE;
                    }
                    throw new WFSException(request, "Could not parse input features", code);
                }
                SimpleFeature feature = (SimpleFeature) next;

                String name = feature.getFeatureType().getTypeName();
                String namespaceURI = feature.getFeatureType().getName().getNamespaceURI();

                typeNames.add(new QName(namespaceURI, name));
            }
        } else {
            LOGGER.finer("Insert was empty - does not need a FeatureSource");
        }

        return (QName[]) typeNames.toArray(new QName[typeNames.size()]);
    }
}
