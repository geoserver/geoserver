/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;

import javax.xml.namespace.QName;

import net.opengis.wfs20.FeatureCollectionType;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.GetPropertyValueType;
import net.opengis.wfs20.QueryType;
import net.opengis.wfs20.ResolveValueType;
import net.opengis.wfs20.ValueCollectionType;
import net.opengis.wfs20.Wfs20Factory;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geotools.wfs.PropertyValueCollection;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.PropertyDescriptor;

public class GetPropertyValue {

    GetFeature delegate;
    Catalog catalog;
    
    public GetPropertyValue(WFSInfo info, Catalog catalog) {
        delegate = new GetFeature(info, catalog);
        this.catalog = catalog;
    }

    public ValueCollectionType run(GetPropertyValueType request) throws WFSException {
        //check the request resolve
        if (request.isSetResolve() && !ResolveValueType.NONE.equals(request.getResolve())) {
            throw new WFSException(request, "Only resolve = none is supported", "InvalidParameterValue")
                .locator("resolve");
        }

        if (request.getValueReference() == null) {
            throw new WFSException(request, "No valueReference specified", "MissingParameterValue")
                .locator("valueReference");
        }

        //do a getFeature request
        GetFeatureType getFeature = Wfs20Factory.eINSTANCE.createGetFeatureType();
        getFeature.getAbstractQueryExpression().add(request.getAbstractQueryExpression());
        
        FeatureCollectionType fc = (FeatureCollectionType) 
            delegate.run(GetFeatureRequest.adapt(getFeature)).getAdaptee();

        QueryType query = (QueryType) request.getAbstractQueryExpression();
        QName typeName = (QName) query.getTypeNames().iterator().next();
        FeatureTypeInfo featureType = 
            catalog.getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());

        try {
            //look for the attribute type
            AttributeTypeInfo attribute = null;
            for (AttributeTypeInfo at : featureType.attributes()) {
                if (at.getName().equals(request.getValueReference())) {
                    attribute = at;
                    break;
                }
            }
            if (attribute == null) {
                throw new WFSException(request, "No such attribute: " + request.getValueReference());
            }
    
            AttributeDescriptor descriptor = attribute.getAttribute();
            if (descriptor == null) {
                PropertyDescriptor pd = 
                        featureType.getFeatureType().getDescriptor(attribute.getName());
                if (pd instanceof AttributeDescriptor) {
                    descriptor = (AttributeDescriptor) pd;
                }
            }

            if (descriptor == null) {
                throw new WFSException(request, "Unable to obtain descriptor for " + attribute.getName());
            }
            
            //create value collection type from feature collection
            ValueCollectionType vc = Wfs20Factory.eINSTANCE.createValueCollectionType();
            vc.setTimeStamp(fc.getTimeStamp());
            vc.setNumberMatched(fc.getNumberMatched());
            vc.setNumberReturned(fc.getNumberReturned());
            vc.getMember().add(new PropertyValueCollection(fc.getMember().iterator().next(), 
                descriptor));
            //TODO: next/previous but point back at GetPropertyValue
            //vc.setNext(fc.getNext());
            //vc.setPrevious(fc.getPrevious());
            return vc;
        }
        catch(IOException e) {
            throw new WFSException(request, e);
        }
    }
}
