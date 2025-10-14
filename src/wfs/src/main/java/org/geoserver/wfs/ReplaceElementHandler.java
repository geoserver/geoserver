/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.config.GeoServer;
import org.geoserver.wfs.request.Replace;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;

public class ReplaceElementHandler extends AbstractTransactionElementHandler {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.wfs");

    static FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    public ReplaceElementHandler(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    public Class<Replace> getElementClass() {
        return Replace.class;
    }

    @Override
    public QName[] getTypeNames(TransactionRequest request, TransactionElement element) throws WFSTransactionException {
        Replace replace = (Replace) element;

        List<QName> typeNames = new ArrayList<>();

        List features = replace.getFeatures();
        if (!features.isEmpty()) {
            for (Object o : features) {
                SimpleFeature feature = (SimpleFeature) o;

                String name = feature.getFeatureType().getTypeName();
                String namespaceURI = feature.getFeatureType().getName().getNamespaceURI();

                typeNames.add(new QName(namespaceURI, name));
            }
        }

        return typeNames.toArray(new QName[typeNames.size()]);
    }

    @Override
    public void checkValidity(TransactionElement element, Map featureTypeInfos) throws WFSTransactionException {
        if (!getInfo().getServiceLevel().getOps().contains(WFSInfo.Operation.TRANSACTION_REPLACE)) {
            throw new WFSException(element, "Transaction REPLACE support is not enabled");
        }

        if (featureTypeInfos.size() != 1) {
            throw new WFSException(
                    element, "Transaction REPLACE must only specify features from a" + " single feature type");
        }
    }

    @Override
    public void execute(
            TransactionElement element,
            TransactionRequest request,
            Map featureStores,
            TransactionResponse response,
            TransactionListener listener)
            throws WFSTransactionException {

        Replace replace = (Replace) element;

        @SuppressWarnings("unchecked")
        List<SimpleFeature> newFeatures = replace.getFeatures();
        SimpleFeatureStore featureStore = DataUtilities.simple(
                (FeatureStore) featureStores.values().iterator().next());
        if (featureStore == null) {
            throw new WFSException(element, "Could not obtain feature store");
        }

        // ids of replaced features
        Collection<FeatureId> replaced = new ArrayList<>();

        try {
            SimpleFeatureCollection features = featureStore.getFeatures(replace.getFilter());
            if (newFeatures.size() != features.size()) {
                throw new WFSException(
                        element,
                        String.format(
                                "Specified filter matched %d features but " + "%d were supplied",
                                features.size(), newFeatures.size()));
            }

            // replace the features in order...
            // JD, TODO: a better mechanism for replace... this is sort of a hack based on a combo
            // of
            // feature ids and orders
            // may want to check if the store is making feature id's writable and attempt
            // to actually update the ID's

            // load all the existing features into memory
            Map<String, SimpleFeature> oldFeatures = new LinkedHashMap<>();

            try (SimpleFeatureIterator it = features.features()) {
                while (it.hasNext()) {
                    SimpleFeature f = it.next();
                    oldFeatures.put(f.getID(), f);
                }
            }

            // first pass update all the features that match by id
            List<SimpleFeature> leftovers = new ArrayList<>();

            for (SimpleFeature newFeature : newFeatures) {
                SimpleFeature oldFeature = oldFeatures.get(newFeature.getID());
                if (oldFeature == null) {
                    leftovers.add(newFeature);
                    continue;
                }

                // matching feature found, update it
                replace(oldFeature, newFeature, featureStore, oldFeatures, replaced);
            }

            // do left overs
            for (SimpleFeature newFeature : leftovers) {
                // grab the "next" old feature
                SimpleFeature oldFeature = oldFeatures.values().iterator().next();
                replace(oldFeature, newFeature, featureStore, oldFeatures, replaced);
            }
        } catch (IOException e) {
            throw exceptionFactory.newWFSTransactionException("Replace error: " + e.getMessage(), e);
        }

        response.setTotalReplaced(BigInteger.valueOf(replaced.size()));
        response.addReplacedFeatures(replace.getHandle(), replaced);
    }

    void replace(
            SimpleFeature oldFeature,
            SimpleFeature newFeature,
            SimpleFeatureStore featureStore,
            Map<String, SimpleFeature> oldFeatures,
            Collection<FeatureId> ids)
            throws IOException {
        String[] names = new String[oldFeature.getAttributeCount()];
        Object[] valus = new Object[names.length];

        int i = 0;
        for (AttributeDescriptor att : oldFeature.getType().getAttributeDescriptors()) {
            String name = att.getLocalName();
            Object valu = newFeature.getAttribute(name);

            names[i] = name;
            valus[i++] = valu;
        }

        FeatureId id = filterFactory.featureId(oldFeature.getID());
        featureStore.modifyFeatures(names, valus, filterFactory.id(Collections.singleton(id)));

        ids.add(id);
        oldFeatures.remove(oldFeature.getID());
    }
}
