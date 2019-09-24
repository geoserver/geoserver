/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import java.util.Map;
import java.util.logging.Logger;
import net.opengis.ows10.Ows10Factory;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.wfs.CatalogFeatureTypeCache;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.PropertyTypePropertyExtractor;
import org.geoserver.wfs.xml.WFSHandlerFactory;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geoserver.wfs.xml.gml2.GMLBoxTypeBinding;
import org.geotools.data.DataAccess;
import org.geotools.filter.v1_0.OGCBBOXTypeBinding;
import org.geotools.filter.v1_0.OGCConfiguration;
import org.geotools.filter.v1_1.OGC;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.GML;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.OptionalComponentParameter;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;

/**
 * Parser configuration for wfs 1.0.
 *
 * @author Justin Deoliveira, The Open Planning Project TODO: this class duplicates a lot of what is
 *     is in the 1.1 configuration, merge them
 */
public class WFSConfiguration extends Configuration {
    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.wfs");

    Catalog catalog;
    FeatureTypeSchemaBuilder schemaBuilder;

    public WFSConfiguration(
            Catalog catalog, FeatureTypeSchemaBuilder schemaBuilder, final WFS wfs) {
        super(wfs);

        this.catalog = catalog;
        this.schemaBuilder = schemaBuilder;

        catalog.addListener(
                new CatalogListener() {

                    public void handleAddEvent(CatalogAddEvent event) {
                        if (event.getSource() instanceof FeatureTypeInfo) {
                            reloaded();
                        }
                    }

                    public void handleModifyEvent(CatalogModifyEvent event) {
                        if (event.getSource() instanceof DataStoreInfo
                                || event.getSource() instanceof FeatureTypeInfo
                                || event.getSource() instanceof NamespaceInfo) {
                            reloaded();
                        }
                    }

                    public void handlePostModifyEvent(CatalogPostModifyEvent event) {}

                    public void handleRemoveEvent(CatalogRemoveEvent event) {}

                    public void reloaded() {
                        wfs.dispose();
                    }
                });
        catalog.getResourcePool()
                .addListener(
                        new ResourcePool.Listener() {

                            public void disposed(FeatureTypeInfo featureType, FeatureType ft) {}

                            public void disposed(
                                    CoverageStoreInfo coverageStore, GridCoverageReader gcr) {}

                            public void disposed(DataStoreInfo dataStore, DataAccess da) {
                                wfs.dispose();
                            }
                        });

        addDependency(new OGCConfiguration());
        addDependency(new GMLConfiguration());
    }

    public Catalog getCatalog() {
        return catalog;
    }

    protected void registerBindings(MutablePicoContainer container) {
        // Types
        container.registerComponentImplementation(WFS.ALLSOMETYPE, AllSomeTypeBinding.class);
        container.registerComponentImplementation(
                WFS.DELETEELEMENTTYPE, DeleteElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.DESCRIBEFEATURETYPETYPE, DescribeFeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFS.EMPTYTYPE, EmptyTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURECOLLECTIONTYPE, FeatureCollectionTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURESLOCKEDTYPE, FeaturesLockedTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURESNOTLOCKEDTYPE, FeaturesNotLockedTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GETCAPABILITIESTYPE, GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS.GETFEATURETYPE, GetFeatureTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GETFEATUREWITHLOCKTYPE, GetFeatureWithLockTypeBinding.class);
        container.registerComponentImplementation(
                WFS.INSERTELEMENTTYPE, InsertElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.INSERTRESULTTYPE, InsertResultTypeBinding.class);
        container.registerComponentImplementation(
                WFS.LOCKFEATURETYPE, LockFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS.LOCKTYPE, LockTypeBinding.class);
        container.registerComponentImplementation(WFS.NATIVETYPE, NativeTypeBinding.class);
        container.registerComponentImplementation(WFS.PROPERTYTYPE, PropertyTypeBinding.class);
        container.registerComponentImplementation(WFS.QUERYTYPE, QueryTypeBinding.class);
        container.registerComponentImplementation(WFS.STATUSTYPE, StatusTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONRESULTTYPE, TransactionResultTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONTYPE, TransactionTypeBinding.class);
        container.registerComponentImplementation(
                WFS.UPDATEELEMENTTYPE, UpdateElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.WFS_LOCKFEATURERESPONSETYPE, WFS_LockFeatureResponseTypeBinding.class);
        container.registerComponentImplementation(
                WFS.WFS_TRANSACTIONRESPONSETYPE, WFS_TransactionResponseTypeBinding.class);
    }

    public void configureContext(MutablePicoContainer context) {
        super.configureContext(context);

        context.registerComponentInstance(Ows10Factory.eINSTANCE);
        context.registerComponentInstance(WfsFactory.eINSTANCE);
        context.registerComponentInstance(new WFSHandlerFactory(catalog, schemaBuilder));
        context.registerComponentInstance(catalog);
        context.registerComponentImplementation(PropertyTypePropertyExtractor.class);

        // TODO: this code is copied from the 1.1 configuration, FACTOR IT OUT!!!
        // seed the cache with entries from the catalog
        context.registerComponentInstance(
                FeatureTypeCache.class, new CatalogFeatureTypeCache(getCatalog()));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureBindings(Map bindings) {
        // override the GMLAbstractFeatureTypeBinding
        bindings.put(GML.AbstractFeatureType, GMLAbstractFeatureTypeBinding.class);

        WFSXmlUtils.registerAbstractGeometryTypeBinding(this, bindings, GML.AbstractGeometryType);

        bindings.put(
                GML.BoxType,
                new SetterInjectionComponentAdapter(
                        GML.BoxType,
                        GMLBoxTypeBinding.class,
                        new Parameter[] {
                            new OptionalComponentParameter(CoordinateReferenceSystem.class)
                        }));

        // use setter injection for OGCBBoxTypeBinding to allow an
        // optional crs to be set in teh binding context for parsing, this crs
        // is set by the binding of a parent element.
        // note: it is important that this component adapter is non-caching so
        // that the setter property gets updated properly every time
        bindings.put(
                OGC.BBOXType,
                new SetterInjectionComponentAdapter(
                        OGC.BBOXType,
                        OGCBBOXTypeBinding.class,
                        new Parameter[] {
                            new OptionalComponentParameter(CoordinateReferenceSystem.class)
                        }));
    }
}
