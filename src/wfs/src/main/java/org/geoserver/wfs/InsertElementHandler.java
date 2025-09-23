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
import java.util.LinkedHashMap;
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
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml3.v3_2.GML;
import org.geotools.referencing.operation.projection.PointOutsideEnvelopeException;
import org.geotools.util.Version;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;

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

    @Override
    public void checkValidity(TransactionElement element, Map<QName, FeatureTypeInfo> featureTypeInfos)
            throws WFSTransactionException {
        if (!getInfo().getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_INSERT)) {
            throw new WFSException(element, "Transaction INSERT support is not enabled");
        }
    }

    @Override
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
            HashMap /* <SimpleFeatureType,FeatureCollection> */ schema2features = new LinkedHashMap<>();

            List featureList = insert.getFeatures();
            for (Object item : featureList) {
                SimpleFeature feature = (SimpleFeature) item;
                SimpleFeatureType schema = feature.getFeatureType();
                ListFeatureCollection collection = (ListFeatureCollection) schema2features.get(schema);

                if (collection == null) {
                    collection = new ListFeatureCollection(schema);
                    schema2features.put(schema, collection);
                }

                // do a check for idegen = useExisting, if set try to tell the datastore to use
                // the provided fid
                if (insert.isIdGenUseExisting()) {
                    feature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                } else {
                    Object identifier = feature.getAttribute(new NameImpl(GML.NAMESPACE, "identifier"));
                    if (WFSInfo.Version.V_20.compareTo(insert.getVersion()) >= 0
                            && identifier instanceof String string) {
                        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(feature.getFeatureType());
                        fb.init(feature);
                        feature = fb.buildFeature(string);
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
            Map<String, List<FeatureId>> schema2fids = new HashMap<>();

            for (Object value : schema2features.values()) {
                SimpleFeatureCollection collection = (SimpleFeatureCollection) value;
                SimpleFeatureType schema = collection.getSchema();

                final QName elementName = new QName(schema.getName().getNamespaceURI(), schema.getTypeName());
                SimpleFeatureStore store = DataUtilities.simple((FeatureStore) featureStores.get(elementName));

                if (store == null) {
                    throw new WFSException(request, "Could not locate FeatureStore for '" + elementName + "'");
                }

                if (collection != null) {
                    // if we really need to, make sure we are inserting coordinates that do
                    // match the CRS area of validity
                    if (getInfo().isCiteCompliant()) {
                        checkFeatureCoordinatesRange(collection);
                    }

                    // reprojection
                    final GeometryDescriptor defaultGeometry = store.getSchema().getGeometryDescriptor();
                    if (defaultGeometry != null) {
                        CoordinateReferenceSystem target = defaultGeometry.getCoordinateReferenceSystem();
                        if (target != null /* && !CRS.equalsIgnoreMetadata(collection.getSchema()
                                        .getCoordinateReferenceSystem(), target) */) {
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
                        fids = new LinkedList<>();
                        schema2fids.put(schema.getTypeName(), fids);
                    }

                    // fire pre insert event
                    TransactionEvent event =
                            new TransactionEvent(TransactionEventType.PRE_INSERT, request, elementName, collection);
                    event.setSource(Insert.WFS11.unadapt(insert));

                    listener.dataStoreChange(event);
                    fids.addAll(store.addFeatures(collection));

                    // fire post insert event
                    SimpleFeatureCollection features = store.getFeatures(filterFactory.id(new HashSet<>(fids)));
                    event = new TransactionEvent(
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
            for (Object o : featureList) {
                SimpleFeature feature = (SimpleFeature) o;
                SimpleFeatureType schema = feature.getFeatureType();

                // get the next fid
                LinkedList<FeatureId> fids = (LinkedList<FeatureId>) schema2fids.get(schema.getTypeName());
                FeatureId fid = fids.removeFirst();

                response.addInsertedFeature(insert.getHandle(), fid);
            }

            // update the insert counter
            inserted += featureList.size();
        } catch (Exception e) {
            throw exceptionFactory.newWFSTransactionException("Insert error: " + e.getMessage(), e, insert.getHandle());
        }

        // update transaction summary
        response.setTotalInserted(BigInteger.valueOf(inserted));
    }

    /** Checks that all features coordinates are within the expected coordinate range */
    void checkFeatureCoordinatesRange(SimpleFeatureCollection collection) throws PointOutsideEnvelopeException {
        List types = collection.getSchema().getAttributeDescriptors();
        try (SimpleFeatureIterator fi = collection.features()) {
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                for (int i = 0; i < types.size(); i++) {
                    if (types.get(i) instanceof GeometryDescriptor) {
                        GeometryDescriptor gat = (GeometryDescriptor) types.get(i);
                        if (gat.getCoordinateReferenceSystem() != null) {
                            Geometry geom = (Geometry) f.getAttribute(i);
                            if (geom != null) JTS.checkCoordinatesRange(geom, gat.getCoordinateReferenceSystem());
                        }
                    }
                }
            }
        }
    }

    @Override
    public Class<Insert> getElementClass() {
        return Insert.class;
    }

    @Override
    public QName[] getTypeNames(TransactionRequest request, TransactionElement element) throws WFSTransactionException {
        Insert insert = (Insert) element;

        List<QName> typeNames = new ArrayList<>();

        List features = insert.getFeatures();
        if (!features.isEmpty()) {
            for (Object next : features) {
                // if parsing fails the parser just returns a Map, do throw an error in this case
                if (!(next instanceof SimpleFeature)) {
                    String version = request.getVersion();
                    String code;
                    if (version == null || new Version(version).compareTo(WFSInfo.Version.V_20.getVersion()) >= 0) {
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

        return typeNames.toArray(new QName[typeNames.size()]);
    }
}
