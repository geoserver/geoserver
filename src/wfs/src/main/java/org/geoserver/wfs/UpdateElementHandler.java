/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.Property;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geoserver.wfs.request.Update;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureLocking;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureLocking;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml2.GML;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.PointOutsideEnvelopeException;
import org.geotools.util.Converters;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Processes standard update elements
 *
 * @author Andrea Aime - TOPP
 */
public class UpdateElementHandler extends AbstractTransactionElementHandler {

    static final Map<String, Class> GML_PROPERTIES_BINDINGS =
            new HashMap<String, Class>() {
                {
                    put("name", String.class);
                    put("description", String.class);
                    put("boundedBy", Geometry.class);
                    put("location", Geometry.class);
                    put("metaDataProperty", String.class);
                }
            };

    static final Set<String> GML_NAMESPACES =
            new HashSet<>(
                    Arrays.asList(
                            GML.NAMESPACE,
                            org.geotools.gml3.GML.NAMESPACE,
                            org.geotools.gml3.v3_2.GML.NAMESPACE));

    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    public UpdateElementHandler(GeoServer gs) {
        super(gs);
    }

    public void checkValidity(TransactionElement element, Map<QName, FeatureTypeInfo> typeInfos)
            throws WFSTransactionException {

        // check inserts are enabled
        if (!getInfo().getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_UPDATE)) {
            throw new WFSException(element, "Transaction Update support is not enabled");
        }

        Update update = (Update) element;
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

        try {
            FeatureTypeInfo meta = typeInfos.values().iterator().next();
            FeatureType featureType = meta.getFeatureType();

            List<Property> props = update.getUpdateProperties();
            for (Iterator<Property> prop = props.iterator(); prop.hasNext(); ) {
                Property property = prop.next();

                // check that valus that are non-nillable exist
                if (property.getValue() == null) {
                    String propertyName = property.getName().getLocalPart();
                    AttributeDescriptor attributeType = null;
                    PropertyDescriptor pd = featureType.getDescriptor(propertyName);
                    if (pd instanceof AttributeDescriptor) {
                        attributeType = (AttributeDescriptor) pd;
                    }
                    if ((attributeType != null) && (attributeType.getMinOccurs() > 0)) {
                        String msg =
                                "Property '"
                                        + attributeType.getLocalName()
                                        + "' is mandatory but no value specified.";
                        throw new WFSException(element, msg, "MissingParameterValue");
                    }
                }

                // check that property names are actually valid
                QName name = property.getName();
                PropertyName propertyName = null;

                if (name.getPrefix() != null && !"".equals(name.getPrefix())) {
                    propertyName = ff.property(name.getPrefix() + ":" + name.getLocalPart());
                } else {
                    propertyName = ff.property(name.getLocalPart());
                }

                AttributeDescriptor descriptor =
                        propertyName.evaluate(featureType, AttributeDescriptor.class);
                if (descriptor == null) {
                    if (getInfo().isCiteCompliant()) {
                        // was it a common GML property that we don't have backing storage for?
                        String namespace = name.getNamespaceURI();
                        Class binding = GML_PROPERTIES_BINDINGS.get(name.getLocalPart());
                        if (GML_NAMESPACES.contains(namespace) && binding != null) {
                            // the hack is here, CITE tests want us to report that updating with an
                            // un-parseable KML point
                            // is an invalid value
                            validateValue(element, property, binding);

                            // if the above did not throw, then move on with the usual behavior
                        }
                    }

                    String msg = "No such property: " + name;
                    throw new WFSException(element, msg, ServiceException.INVALID_PARAMETER_VALUE);
                }

                // validate contents
                validateValue(element, property, descriptor.getType().getBinding());
            }
        } catch (IOException e) {
            throw new WFSTransactionException(
                    "Could not locate feature type information for " + update.getTypeName(),
                    e,
                    update.getHandle());
        }
    }

    private void validateValue(TransactionElement element, Property property, Class<?> binding) {
        Object value = property.getValue();

        // was it a null? If so, assume valid (already checked for nulls before)
        if (value == null // parsed as null
                || (value instanceof String
                        && ((String) value).trim().isEmpty()) // as an empty string
                || (value instanceof Map
                        && ((Map) value).isEmpty()) // or the usual map that the parser creates
        ) {
            return;
        }

        // see if the datastore machinery will be able to convert
        Object converted = Converters.convert(value, binding);
        if (converted == null) {
            String propertyName = property.getName().getLocalPart();
            WFSException e =
                    new WFSException(
                            element,
                            "Invalid value for property " + propertyName,
                            WFSException.INVALID_VALUE);
            e.setLocator(propertyName);
            throw e;
        }
    }

    public void execute(
            TransactionElement element,
            TransactionRequest request,
            @SuppressWarnings("rawtypes") Map<QName, FeatureStore> featureStores,
            TransactionResponse response,
            TransactionListener listener)
            throws WFSTransactionException {

        Update update = (Update) element;
        final QName elementName = update.getTypeName();
        String handle = update.getHandle();

        long updated = response.getTotalUpdated().longValue();

        SimpleFeatureStore store =
                DataUtilities.simple((FeatureStore) featureStores.get(elementName));

        if (store == null) {
            throw new WFSException(
                    request, "Could not locate FeatureStore for '" + elementName + "'");
        }

        LOGGER.finer("Transaction Update:" + update);

        try {
            Filter filter = update.getFilter();

            // make sure all geometric elements in the filter have a crs, and that the filter
            // is reprojected to store's native crs as well
            CoordinateReferenceSystem declaredCRS =
                    WFSReprojectionUtil.getDeclaredCrs(store.getSchema(), request.getVersion());
            if (filter != null) {
                filter =
                        WFSReprojectionUtil.normalizeFilterCRS(
                                filter, store.getSchema(), declaredCRS);
            } else {
                filter = Filter.INCLUDE;
            }

            List<Property> properties = update.getUpdateProperties();
            AttributeDescriptor[] types = new AttributeDescriptor[properties.size()];
            String[] names = new String[properties.size()];
            Object[] values = new Object[properties.size()];

            // If no properties are defined for an update, there's nothing to do
            if (properties.isEmpty()) {
                return;
            }

            for (int j = 0; j < properties.size(); j++) {
                Property property = properties.get(j);
                QName propertyName = property.getName();
                names[j] = cleanupXPath(propertyName.getLocalPart());
                types[j] = store.getSchema().getDescriptor(names[j]);
                values[j] = property.getValue();

                // if geometry, it may be necessary to reproject it to the native CRS before
                // update
                if (values[j] instanceof Geometry) {
                    Geometry geometry = (Geometry) values[j];

                    // get the source crs, check the geometry itself first. If not set, assume
                    // the default one
                    CoordinateReferenceSystem source = null;
                    if (geometry.getUserData() instanceof CoordinateReferenceSystem) {
                        source = (CoordinateReferenceSystem) geometry.getUserData();
                    } else {
                        geometry.setUserData(declaredCRS);
                        source = declaredCRS;
                    }

                    // see if the geometry has a CRS other than the default one
                    CoordinateReferenceSystem target = null;
                    if (types[j] instanceof GeometryDescriptor) {
                        target = ((GeometryDescriptor) types[j]).getCoordinateReferenceSystem();
                    }

                    if (getInfo().isCiteCompliant())
                        JTS.checkCoordinatesRange(geometry, source != null ? source : target);

                    // if we have a source and target and they are not equal, do
                    // the reprojection, otherwise just update the value as is
                    if (source != null
                            && target != null
                            && !CRS.equalsIgnoreMetadata(source, target)) {
                        try {
                            // TODO: this code should be shared with the code
                            // from ReprojectingFeatureCollection --JD
                            MathTransform tx = CRS.findMathTransform(source, target, true);
                            GeometryCoordinateSequenceTransformer gtx =
                                    new GeometryCoordinateSequenceTransformer();
                            gtx.setMathTransform(tx);

                            values[j] = gtx.transform(geometry);
                        } catch (Exception e) {
                            String msg = "Failed to reproject geometry:" + e.getLocalizedMessage();
                            throw new WFSTransactionException(msg, e);
                        }
                    }
                }
            }

            // Pass through data to collect fids and damaged
            // region
            // for validation
            //
            Set<FeatureId> fids = new HashSet<FeatureId>();
            LOGGER.finer("Preprocess to remember modification as a set of fids");

            SimpleFeatureCollection features = store.getFeatures(filter);
            TransactionEvent event =
                    new TransactionEvent(
                            TransactionEventType.PRE_UPDATE, request, elementName, features);
            event.setSource(Update.WFS11.unadapt(update));

            listener.dataStoreChange(event);

            FeatureIterator preprocess = features.features();

            try {
                while (preprocess.hasNext()) {
                    SimpleFeature feature = (SimpleFeature) preprocess.next();
                    fids.add(feature.getIdentifier());
                }
            } catch (NoSuchElementException e) {
                throw new WFSException(request, "Could not aquire FeatureIDs", e);
            } finally {
                preprocess.close();
            }

            try {
                store.modifyFeatures(names, values, filter);
            } catch (Exception e) {
                // JD: this is a bit hacky but some of the wfs cite tests require
                // that the 'InvalidParameterValue' code be set on exceptions in
                // cases where a "bad" value is being suppliedin an update, so
                // we always set to that code
                throw new WFSTransactionException(
                        "Update error: " + e.getMessage(), e, "InvalidParameterValue");

            } finally {
                // make sure we unlock
                if ((request.getLockId() != null)
                        && store instanceof FeatureLocking
                        && (request.isReleaseActionSome())) {
                    SimpleFeatureLocking locking;
                    locking = (SimpleFeatureLocking) store;
                    locking.unLockFeatures(filter);
                }
            }

            // Post process - gather the same features after the update, and
            if (!fids.isEmpty()) {
                LOGGER.finer("Post process update for boundary update and featureValidation");

                Set<FeatureId> featureIds = new HashSet<FeatureId>();

                FilterFactory2 ff =
                        CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
                for (Iterator<FeatureId> f = fids.iterator(); f.hasNext(); ) {
                    // create new FeatureIds without any possible version information in order to
                    // query for the latest version
                    featureIds.add(ff.featureId(f.next().getID()));
                }

                Id modified = ff.id(featureIds);

                SimpleFeatureCollection changed = store.getFeatures(modified);

                // grab final ids. Not using fetureIds as they may contain different version
                // information after the update
                Set<FeatureId> changedIds = new HashSet<FeatureId>();
                SimpleFeatureIterator iterator = changed.features();
                try {
                    while (iterator.hasNext()) {
                        changedIds.add(iterator.next().getIdentifier());
                    }
                } finally {
                    iterator.close();
                }
                response.addUpdatedFeatures(handle, changedIds);

                listener.dataStoreChange(
                        new TransactionEvent(
                                TransactionEventType.POST_UPDATE,
                                request,
                                elementName,
                                changed,
                                Update.WFS11.unadapt(update)));
            }

            // update the update counter
            updated += fids.size();
        } catch (IOException ioException) {
            // JD: changing from throwing service exception to
            // adding action that failed
            throw new WFSTransactionException(ioException, null, handle);
        } catch (PointOutsideEnvelopeException poe) {
            throw new WFSTransactionException(poe, null, handle);
        }

        // update transaction summary
        response.setTotalUpdated(BigInteger.valueOf(updated));
    }

    private String cleanupXPath(String name) {
        // saying foo or foo[1] is the same
        if (name.endsWith("[1]")) {
            return name.substring(0, name.length() - 3);
        }
        return name;
    }

    /** @see org.geoserver.wfs.TransactionElementHandler#getElementClass() */
    public Class getElementClass() {
        return Update.class;
    }

    /**
     * @see org.geoserver.wfs.TransactionElementHandler#getTypeNames(org.eclipse.emf.ecore.EObject)
     */
    public QName[] getTypeNames(TransactionRequest request, TransactionElement element)
            throws WFSTransactionException {
        return new QName[] {element.getTypeName()};
    }
}
