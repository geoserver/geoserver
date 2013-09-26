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




import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.xsd.XSDTypeDefinition;
import org.geotools.data.complex.config.FeatureTypeRegistry;
import org.geotools.feature.ComplexAttributeImpl;
import org.geotools.feature.NameImpl;
import org.geotools.gml3.GML;
import org.geotools.xml.AbstractComplexBinding;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.opengis.feature.ComplexAttribute;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;

public class ComplexFeaturePropertyTypeBinding extends AbstractComplexBinding {
    private final FeatureTypeRegistry registry;

    public ComplexFeaturePropertyTypeBinding(FeatureTypeRegistry registry) {
        this.registry = registry;
    }
    @Override
    public QName getTarget() {
        return GML.FeaturePropertyType;
    }

    @Override
    public Class getType() {
        return ComplexAttribute.class;
    }

    @Override
    public Object parse(ElementInstance instance, Node node, Object value) throws Exception {
        ComplexType featureType = (ComplexType) getType(instance);
        Node featureNode = node.getChild(Feature.class);
        List<Property> props;
        if(featureNode != null) {
            props = Collections.<Property>singletonList((Feature)featureNode.getValue());
        } else {
            props = Collections.EMPTY_LIST;
        }

        return new ComplexAttributeImpl(props, featureType, null);
    }

    private AttributeType getType(ElementInstance instance) {
        AttributeType featureType;
        XSDTypeDefinition def = instance.getTypeDefinition();
        if(def.getName() == null)
            featureType =
                registry.getDescriptor(new NameImpl(instance.getNamespace(), instance.getName()))
                    .getType();
        else
            featureType =
                registry.getAttributeType(new NameImpl(def.getTargetNamespace(), def.getName()));
        return featureType;
    }
}
