/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.xml.v1_1_0.WFSConfiguration;
import org.geotools.xml.Configuration;
import org.picocontainer.MutablePicoContainer;

/**
 * Parser configuration for the http://geoserver.org/gss schema.
 */
public class GSSConfiguration extends Configuration {
    Catalog catalog;

    /**
     * Creates a new configuration.
     * 
     * @generated
     */
    public GSSConfiguration(WFSConfiguration wfsConfiguration, GSS gss, Catalog catalog) {
        super(gss);
        this.catalog = catalog;
        addDependency(wfsConfiguration);
    }

    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    protected void registerBindings(MutablePicoContainer container) {
        // Types
        container.registerComponentImplementation(GSS.GetCentralRevisionType,
                GetCentralRevisionTypeBinding.class);
        container.registerComponentImplementation(GSS.CentralRevisionsType,
                CentralRevisionsTypeBinding.class);
        container.registerComponentImplementation(GSS.LayerRevisionType,
                LayerRevisionTypeBinding.class);
        container.registerComponentImplementation(GSS.PostDiffType, PostDiffTypeBinding.class);
        container.registerComponentImplementation(GSS.PostDiffResponseType,
                PostDiffResponseTypeBinding.class);
        container.registerComponentImplementation(GSS.GetDiffType, GetDiffTypeBinding.class);
        container.registerComponentImplementation(GSS.GetDiffResponseType,
                GetDiffResponseTypeBinding.class);
    }

    // public void configureContext(MutablePicoContainer bindings) {
    // super.configureContext(bindings);
    // // bindings.registerComponentInstance(WfsvFactory.eINSTANCE);
    // // bindings.registerComponentInstance(new VersionedFeaturePropertyExtractor(catalog));
    // }
}
