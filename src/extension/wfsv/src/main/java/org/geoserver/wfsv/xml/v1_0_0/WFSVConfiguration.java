/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfsv.xml.v1_0_0;

import net.opengis.wfsv.WfsvFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.xml.v1_0_0.WFSConfiguration;
import org.geoserver.wfsv.xml.v1_1_0.DescribeVersionedFeatureTypeTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.DifferenceQueryTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.GetDiffTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.GetLogTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.GetVersionedFeatureTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.RollbackTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.VersionedDeleteElementTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.VersionedFeatureCollectionTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.VersionedFeaturePropertyExtractor;
import org.geoserver.wfsv.xml.v1_1_0.VersionedUpdateElementTypeBinding;
import org.geoserver.wfsv.xml.v1_1_0.WFSV;
import org.geotools.xml.Configuration;
import org.picocontainer.MutablePicoContainer;

/**
 * Parser configuration for the http://www.opengis.net/wfsv schema.
 *
 * @generated
 */
public class WFSVConfiguration extends Configuration {
    Catalog catalog;

    /**
     * Creates a new configuration.
     *
     * @generated
     */
    public WFSVConfiguration(WFSConfiguration wfsConfiguration, org.geoserver.wfsv.xml.v1_0_0.WFSV wfsv, Catalog catalog) {
        super(wfsv);
        this.catalog = catalog;
        addDependency(wfsConfiguration);
    }
    
    public Catalog getCatalog() {
        return catalog;
    }

    protected void registerBindings(MutablePicoContainer container) {
        //Types
        container.registerComponentImplementation(WFSV.DifferenceQueryType,DifferenceQueryTypeBinding.class);
        container.registerComponentImplementation(WFSV.DescribeVersionedFeatureTypeType,DescribeVersionedFeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFSV.GetDiffType,GetDiffTypeBinding.class);
        container.registerComponentImplementation(WFSV.GetLogType,GetLogTypeBinding.class);
        container.registerComponentImplementation(WFSV.GetVersionedFeatureType,GetVersionedFeatureTypeBinding.class);
        container.registerComponentImplementation(WFSV.RollbackType,RollbackTypeBinding.class);
        container.registerComponentImplementation(WFSV.VersionedDeleteElementType,VersionedDeleteElementTypeBinding.class);
        container.registerComponentImplementation(WFSV.VersionedFeatureCollectionType,VersionedFeatureCollectionTypeBinding.class);
        container.registerComponentImplementation(WFSV.VersionedUpdateElementType,VersionedUpdateElementTypeBinding.class);
    }
    
    public void configureContext(MutablePicoContainer bindings) {
        super.configureContext(bindings);
        bindings.registerComponentInstance(WfsvFactory.eINSTANCE);
        bindings.registerComponentInstance(new VersionedFeaturePropertyExtractor(catalog));
    }
}
