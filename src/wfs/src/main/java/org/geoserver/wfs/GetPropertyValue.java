/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import net.opengis.wfs20.FeatureCollectionType;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.GetPropertyValueType;
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.ValueCollectionType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.wfs.PropertyValueCollection;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.xml.sax.helpers.NamespaceSupport;

public class GetPropertyValue {

    Pattern FEATURE_ID_PATTERN = Pattern.compile("@(\\w+:)?id");

    GetFeature delegate;

    Catalog catalog;

    FilterFactory2 filterFactory;

    public GetPropertyValue(WFSInfo info, Catalog catalog, FilterFactory2 filterFactory) {
        delegate = new GetFeature(info, catalog);
        delegate.setFilterFactory(filterFactory);

        this.catalog = catalog;
        this.filterFactory = filterFactory;
    }

    /** @return NamespaceSupport from Catalog */
    public NamespaceSupport getNamespaceSupport() {
        NamespaceSupport ns = new NamespaceSupport();
        Iterator<NamespaceInfo> it = catalog.getNamespaces().iterator();
        while (it.hasNext()) {
            NamespaceInfo ni = it.next();
            ns.declarePrefix(ni.getPrefix(), ni.getURI());
        }
        return ns;
    }

    public ValueCollectionType run(GetPropertyValueType request) throws WFSException {

        if (request.getValueReference() == null) {
            throw new WFSException(request, "No valueReference specified", "MissingParameterValue")
                    .locator("valueReference");
        } else if ("".equals(request.getValueReference().trim())) {
            throw new WFSException(
                            request,
                            "ValueReference cannot be empty",
                            ServiceException.INVALID_PARAMETER_VALUE)
                    .locator("valueReference");
        }

        // do a getFeature request
        GetFeatureType getFeature = Wfs20Factory.eINSTANCE.createGetFeatureType();
        getFeature.setBaseUrl(request.getBaseUrl());
        getFeature.getAbstractQueryExpression().add(request.getAbstractQueryExpression());
        getFeature.setResolve(request.getResolve());
        getFeature.setResolveDepth(request.getResolveDepth());
        getFeature.setResolveTimeout(request.getResolveTimeout());
        getFeature.setCount(request.getCount());

        FeatureCollectionType fc =
                (FeatureCollectionType)
                        delegate.run(GetFeatureRequest.adapt(getFeature)).getAdaptee();

        QueryType query = (QueryType) request.getAbstractQueryExpression();
        QName typeName = (QName) query.getTypeNames().iterator().next();
        FeatureTypeInfo featureType =
                catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());

        try {

            PropertyName propertyName =
                    filterFactory.property(request.getValueReference(), getNamespaceSupport());
            PropertyName propertyNameNoIndexes =
                    filterFactory.property(
                            request.getValueReference().replaceAll("\\[.*\\]", ""),
                            getNamespaceSupport());
            AttributeDescriptor descriptor =
                    (AttributeDescriptor)
                            propertyNameNoIndexes.evaluate(featureType.getFeatureType());
            boolean featureIdRequest =
                    FEATURE_ID_PATTERN.matcher(request.getValueReference()).matches();
            if (descriptor == null && !featureIdRequest) {
                throw new WFSException(
                        request, "No such attribute: " + request.getValueReference());
            }

            // create value collection type from feature collection
            ValueCollectionType vc = Wfs20Factory.eINSTANCE.createValueCollectionType();
            vc.setTimeStamp(fc.getTimeStamp());
            vc.setNumberMatched(fc.getNumberMatched());
            vc.setNumberReturned(fc.getNumberReturned());
            vc.getMember()
                    .add(
                            new PropertyValueCollection(
                                    fc.getMember().iterator().next(), descriptor, propertyName));
            return vc;
        } catch (IOException e) {
            throw new WFSException(request, e);
        }
    }

    public void setFilterFactory(FilterFactory2 filterFactory) {
        this.filterFactory = filterFactory;
    }
}
