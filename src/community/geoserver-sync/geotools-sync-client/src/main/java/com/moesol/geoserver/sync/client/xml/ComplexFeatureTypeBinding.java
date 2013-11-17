/**
 *
 *  #%L
 *  geoserver-sync-core
 *  $Id:$
 *  $HeadURL:$
 *  %%
 *  Copyright (C) 2013 Moebius Solutions Inc.
 *  %%
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation, either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program.  If not, see
 *  <http://www.gnu.org/licenses/gpl-2.0.html>.
 *  #L%
 *
 */

package com.moesol.geoserver.sync.client.xml;




import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.xsd.XSDTypeDefinition;
import org.geotools.data.complex.config.FeatureTypeRegistry;
import org.geotools.feature.AttributeImpl;
import org.geotools.feature.ComplexAttributeImpl;
import org.geotools.feature.FeatureImpl;
import org.geotools.feature.NameImpl;
import org.geotools.filter.identity.FeatureIdImpl;
import org.geotools.gml3.GML;
import org.geotools.gml3.XSDIdRegistry;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.BindingWalkerFactory;
import org.geotools.xml.Configuration;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.identity.FeatureId;
import org.picocontainer.MutablePicoContainer;

public class ComplexFeatureTypeBinding extends AbstractComplexBinding {
    private static final Log LOG = LogFactory.getLog(ComplexFeatureTypeBinding.class);
    private final FeatureTypeRegistry registry;
    private final Configuration configuration;
    private final XSDIdRegistry idRegistry;
    private final BindingWalkerFactory bwFactory;

    public ComplexFeatureTypeBinding(FeatureTypeRegistry registry,
        Configuration configuration, XSDIdRegistry idRegistry, BindingWalkerFactory bwFactory) {
        this.registry = registry;
        this.configuration = configuration;
        this.idRegistry = idRegistry;
        this.bwFactory = bwFactory;
    }

    @Override
    public void initializeChildContext(ElementInstance childInstance, Node node, MutablePicoContainer context) {
        super.initializeChildContext(childInstance, node, context);
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        AttributeDescriptor ad = null;

        try {
            ad = registry.getDescriptor(new NameImpl(instance.getName()), null);
            
        	//ad = registry.getDescriptor(new NameImpl(instance.getNamespace(), instance.getName()));
        } catch(Throwable t) {
            LOG.warn("Getting descriptor: ", t);
        }

        FeatureType featureType = (FeatureType) (ad == null ? (FeatureType) getType(instance) : ad.getType());
        String sfid = (String) node.getAttributeValue("id");
        if(sfid == null)
            sfid = (String) node.getAttributeValue("fid");
        FeatureId fid = new FeatureIdImpl(sfid == null ? UUID.randomUUID().toString() : sfid);
        List<Property> props = new ArrayList<Property>();
        for(Node n : (List<Node>) node.getChildren()) {
            String name = n.getComponent().getName();
            String namespace = n.getComponent().getNamespace();
            AttributeDescriptor descriptor =
                (AttributeDescriptor) featureType.getDescriptor(new NameImpl(namespace, name));
            if(n.getValue() instanceof Property) {
                Property p = (Property) n.getValue();
                if(p instanceof Feature)
                    props
                        .add(new FeatureImpl(((Feature) p).getProperties(), descriptor, ((Feature) p).getIdentifier()));
                else if(p instanceof ComplexAttribute)
                    props.add(new ComplexAttributeImpl(((ComplexAttribute) p).getProperties(), descriptor,
                        ((ComplexAttribute) p).getIdentifier()));
                else if(p instanceof Attribute)
                    props
                        .add(new AttributeImpl(((Attribute) p).getValue(), descriptor, ((Attribute) p).getIdentifier()));
            } else if(descriptor != null) {
                try {
                    props.add(new AttributeImpl(n.getValue(), descriptor, null));
                } catch(IllegalAttributeException e) {
                    LOG.warn(n.getValue().getClass() + " cannot be assigned to attribute " +
                        descriptor.getName().getLocalPart() + " of feature type " +
                        featureType.getName().getLocalPart(), e);
                }
            } else {
                LOG.warn("Skipping unknown attribute: " + n.getComponent().getName() + " for type: " + featureType.getName());
            }
        }

        if(ad == null)
            return new FeatureImpl(props, featureType, fid);
        else
            return new FeatureImpl(props, ad, fid);
    }

    private AttributeType getType(ElementInstance instance) {
        AttributeType featureType = null;
        XSDTypeDefinition def = instance.getTypeDefinition();

        try {
            featureType = registry.getDescriptor(new NameImpl(instance.getName()),  null).getType();
            
        	//featureType = registry.getDescriptor(new NameImpl(instance.getNamespace(), instance.getName())).getType();
        } catch(Throwable t) {
            // ignore
        }

        if(featureType == null)
            featureType = registry.getAttributeType(new NameImpl(def.getTargetNamespace(), def.getName()));

        return featureType;
    }

    /**
     * @generated
     */
    public QName getTarget() {
        return GML.AbstractFeatureType;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated modifiable
     */
    public Class getType() {
        return Feature.class;
    }

}
