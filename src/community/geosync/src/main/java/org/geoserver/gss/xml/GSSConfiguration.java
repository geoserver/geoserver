/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.gss.xml;

import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geotools.xml.Configuration;
import org.geotools.xml.XSD;
import org.picocontainer.MutablePicoContainer;

/**
 * Parser configuration for the {@code http://www.opengis.net/gss/1.0} schema.
 */
public class GSSConfiguration extends Configuration {

    private Catalog catalog;

    /**
     * Creates a new configuration.
     * 
     * @generated
     */
    public GSSConfiguration(List<Configuration> dependencies, XSD gssSchema, Catalog catalog) {
        super(gssSchema);
        this.catalog = catalog;
        for (Configuration dependedXmlConfig : dependencies)
            addDependency(dependedXmlConfig);
    }

    public Catalog getCatalog() {
        return catalog;
    }

    @Override
    protected void registerBindings(MutablePicoContainer container) {
        // Types
        /*
         * container.registerComponentImplementation(GSS.GetCentralRevisionType,
         * GetCentralRevisionTypeBinding.class);
         * container.registerComponentImplementation(GSS.CentralRevisionsType,
         * CentralRevisionsTypeBinding.class);
         * container.registerComponentImplementation(GSS.LayerRevisionType,
         * LayerRevisionTypeBinding.class);
         * container.registerComponentImplementation(GSS.PostDiffType, PostDiffTypeBinding.class);
         * container.registerComponentImplementation(GSS.PostDiffResponseType,
         * PostDiffResponseTypeBinding.class);
         * container.registerComponentImplementation(GSS.GetDiffType, GetDiffTypeBinding.class);
         * container.registerComponentImplementation(GSS.GetDiffResponseType,
         * GetDiffResponseTypeBinding.class);
         */
    }

    // public void configureContext(MutablePicoContainer bindings) {
    // super.configureContext(bindings);
    // // bindings.registerComponentInstance(WfsvFactory.eINSTANCE);
    // // bindings.registerComponentInstance(new VersionedFeaturePropertyExtractor(catalog));
    // }
}
