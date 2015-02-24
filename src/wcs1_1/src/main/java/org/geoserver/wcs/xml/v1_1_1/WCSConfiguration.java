/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.xml.v1_1_1;

import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.wcs11.Wcs111Factory;

import org.eclipse.emf.ecore.EFactory;
import org.geoserver.wcs.xml.v1_1_1.bindings.TimePeriodTypeBinding;
import org.geoserver.wcs.xml.v1_1_1.bindings.TimePositionTypeBinding;
import org.geoserver.wcs.xml.v1_1_1.bindings.TimeSequenceTypeBinding;
import org.geotools.gml3.GML;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.ows.v1_1.OWSConfiguration;
import org.geotools.xml.ComplexEMFBinding;
import org.geotools.xml.Configuration;
import org.picocontainer.MutablePicoContainer;

/**
 * Parser configuration for the http://www.opengis.net/wcs/1.1.1 schema.
 *
 * @generated
 */
public class WCSConfiguration extends Configuration {

    /**
     * Creates a new configuration.
     * 
     * @generated
     */     
    public WCSConfiguration() {
       super(WCS.getInstance());
       
       addDependency(new GMLConfiguration());
       addDependency(new OWSConfiguration());
    }
    
    @Override
    protected void registerBindings(Map bindings) {
        super.registerBindings(bindings);
        
        final EFactory wcsFactory = Wcs111Factory.eINSTANCE;
        register(bindings, wcsFactory, WCS._GetCapabilities);
        register(bindings, wcsFactory, WCS.RequestBaseType);
        register(bindings, wcsFactory, WCS._DescribeCoverage);
        register(bindings, wcsFactory, WCS._GetCoverage);
        register(bindings, wcsFactory, WCS.DomainSubsetType);
        register(bindings, wcsFactory, WCS.RangeSubsetType);
        register(bindings, wcsFactory, WCS.RangeSubsetType_FieldSubset);
        register(bindings, wcsFactory, WCS._AxisSubset);
        register(bindings, wcsFactory, WCS.OutputType);
        register(bindings, wcsFactory, WCS.GridCrsType);
        
        bindings.put(GML.TimePositionType, new TimePositionTypeBinding());
        bindings.put(WCS.TimePeriodType, new TimePeriodTypeBinding());
        bindings.put(WCS.TimeSequenceType, new TimeSequenceTypeBinding());
    }
    
    private void register(Map bindings, EFactory factory, QName qname) {
        bindings.put(qname, new ComplexEMFBinding(factory, qname));
    }

    protected void configureContext(MutablePicoContainer container) {
        container.registerComponentInstance(Wcs111Factory.eINSTANCE);
    }
} 
