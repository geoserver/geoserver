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



import java.io.IOException;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geotools.data.complex.config.EmfAppSchemaReader;
import org.geotools.data.complex.config.FeatureTypeRegistry;
import org.geotools.gml3.GML;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xs.XS;
import org.geotools.xs.XSConfiguration;
import org.picocontainer.MutablePicoContainer;

import com.moesol.geoserver.sync.client.xml.pwfs.GeoLabelBinding;

public class ComplexConfiguration extends Configuration {
    
    private FeatureTypeRegistry registry = null;

    public ComplexConfiguration(String namespace, String schemaLocation) {
        super(new ResolvingApplicationSchemaXSD(namespace, schemaLocation));
        addDependency(new XSConfiguration());
        addDependency(new GMLConfiguration());
    }

    @Override
    protected void configureBindings(Map bindings) {
        super.configureBindings(bindings);

    }

    @Override
    protected void configureContext(MutablePicoContainer container) {
        super.configureContext(container);

        if(registry == null) {
            registry = new FeatureTypeRegistry();
            try {
                registry.addSchemas(EmfAppSchemaReader.newInstance().parse(this));
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        container.registerComponentInstance(registry);
    }

    @Override
    protected void registerBindings(Map bindings) {
        super.registerBindings(bindings);

        // Use our parsers for these types (so far...)
        bindings.put(GML.AbstractFeatureType, ComplexFeatureTypeBinding.class);
        bindings.put(GML.FeaturePropertyType, ComplexFeaturePropertyTypeBinding.class);
        bindings.put(GML.FeatureArrayPropertyType, ComplexFeatureArrayPropertyTypeBinding.class);
        bindings.put(GML.AbstractFeatureCollectionType, ComplexFeatureCollectionTypeBinding.class);

        /*
         * gml:CodeType is a simple type with attributes and requires special
         * handling. All such types should probably be handled by one binding
         * class.
         */
        bindings.put(GML.CodeType, CodeTypeBinding.class);

        /*
         * DateTime binding is messed up in GeoTools. XSSchema has it bound to
         * Timestamp, XSDateTimeBinding has it bound to Calendar. We can't
         * control XSSchema (static final fields), but we can override the
         * binding so java.sql.Timestamp it is.
         */
        bindings.put(XS.DATETIME, DateTimeBinding.class);
        
        /*
         * Polexis has their own geometry type for labels.
         */
        bindings.put(GeoLabelBinding.QN_GEO_LABEL_TYPE, GeoLabelBinding.class);

        /*
         * Override a couple of types to get around warnings.
         */
        QName sld = new QName("http://www.opengis.net/sld", "StyledLayerDescriptor");
        QName boundingShapeType = new QName("http://www.opengis.net/gml", "BoundingShapeType");
        bindings.put(sld, new IgnoredTypeBinding(sld));
        bindings.put(boundingShapeType, new IgnoredTypeBinding(boundingShapeType));
    }
}
