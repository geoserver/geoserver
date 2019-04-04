/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.util.Map;
import java.util.logging.Logger;
import net.opengis.wfs.WfsFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.xml.v1_0.OWSConfiguration;
import org.geoserver.wfs.CatalogFeatureTypeCache;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.PropertyTypePropertyExtractor;
import org.geoserver.wfs.xml.WFSHandlerFactory;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geoserver.wfs.xml.XSQNameBinding;
import org.geoserver.wfs.xml.filter.v1_1.FilterTypeBinding;
import org.geoserver.wfs.xml.filter.v1_1.PropertyNameTypeBinding;
import org.geoserver.wfs.xml.gml3.CircleTypeBinding;
import org.geotools.filter.v1_0.OGCBBOXTypeBinding;
import org.geotools.filter.v1_1.OGC;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.geometry.jts.CurvedGeometryFactory;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml3.GML;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.util.logging.Logging;
import org.geotools.xs.XS;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.OptionalComponentParameter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;

public class WFSConfiguration extends Configuration {
    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.wfs");

    /** catalog */
    protected Catalog catalog;

    /** Schema builder */
    protected FeatureTypeSchemaBuilder schemaBuilder;

    public WFSConfiguration(
            GeoServer geoServer, FeatureTypeSchemaBuilder schemaBuilder, final WFS wfs) {
        super(wfs);

        this.catalog = geoServer.getCatalog();
        this.schemaBuilder = schemaBuilder;

        addDependency(new OGCConfiguration());
        addDependency(new OWSConfiguration());
        addDependency(new GMLConfiguration());
        // OGC and OWS add two extra GML configurations in the mix, make sure to configure them
        // all...
        CurvedGeometryFactory gf = new CurvedGeometryFactory(Double.MAX_VALUE);
        for (Object configuration : allDependencies()) {
            if (configuration instanceof GMLConfiguration) {
                GMLConfiguration gml = (GMLConfiguration) configuration;
                gml.setGeometryFactory(gf);
            }
        }
    }

    public void setSrsSyntax(SrsSyntax srsSyntax) {
        WFSXmlUtils.setSrsSyntax(this, srsSyntax);
    }

    public SrsSyntax getSrsSyntax() {
        return WFSXmlUtils.getSrsSyntax(this);
    }

    protected void registerBindings(MutablePicoContainer container) {
        // Types
        container.registerComponentImplementation(WFS.ACTIONTYPE, ActionTypeBinding.class);
        container.registerComponentImplementation(WFS.ALLSOMETYPE, AllSomeTypeBinding.class);
        container.registerComponentImplementation(
                WFS.BASE_TYPENAMELISTTYPE, Base_TypeNameListTypeBinding.class);
        container.registerComponentImplementation(
                WFS.BASEREQUESTTYPE, BaseRequestTypeBinding.class);
        container.registerComponentImplementation(
                WFS.DELETEELEMENTTYPE, DeleteElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.DESCRIBEFEATURETYPETYPE, DescribeFeatureTypeTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURECOLLECTIONTYPE, FeatureCollectionTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURESLOCKEDTYPE, FeaturesLockedTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURESNOTLOCKEDTYPE, FeaturesNotLockedTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURETYPELISTTYPE, FeatureTypeListTypeBinding.class);
        container.registerComponentImplementation(
                WFS.FEATURETYPETYPE, FeatureTypeTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GETCAPABILITIESTYPE, GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS.GETFEATURETYPE, GetFeatureTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GETFEATUREWITHLOCKTYPE, GetFeatureWithLockTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GETGMLOBJECTTYPE, GetGmlObjectTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GMLOBJECTTYPELISTTYPE, GMLObjectTypeListTypeBinding.class);
        container.registerComponentImplementation(
                WFS.GMLOBJECTTYPETYPE, GMLObjectTypeTypeBinding.class);
        container.registerComponentImplementation(
                WFS.IDENTIFIERGENERATIONOPTIONTYPE, IdentifierGenerationOptionTypeBinding.class);
        container.registerComponentImplementation(
                WFS.INSERTEDFEATURETYPE, InsertedFeatureTypeBinding.class);
        container.registerComponentImplementation(
                WFS.INSERTELEMENTTYPE, InsertElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.INSERTRESULTSTYPE, InsertResultTypeBinding.class);
        container.registerComponentImplementation(
                WFS.LOCKFEATURERESPONSETYPE, LockFeatureResponseTypeBinding.class);
        container.registerComponentImplementation(
                WFS.LOCKFEATURETYPE, LockFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS.LOCKTYPE, LockTypeBinding.class);
        container.registerComponentImplementation(
                WFS.METADATAURLTYPE, MetadataURLTypeBinding.class);
        container.registerComponentImplementation(WFS.NATIVETYPE, NativeTypeBinding.class);
        container.registerComponentImplementation(WFS.OPERATIONSTYPE, OperationsTypeBinding.class);
        container.registerComponentImplementation(WFS.OPERATIONTYPE, OperationTypeBinding.class);
        container.registerComponentImplementation(
                WFS.OUTPUTFORMATLISTTYPE, OutputFormatListTypeBinding.class);
        container.registerComponentImplementation(WFS.PROPERTYTYPE, PropertyTypeBinding.class);
        container.registerComponentImplementation(WFS.QUERYTYPE, QueryTypeBinding.class);
        container.registerComponentImplementation(WFS.RESULTTYPETYPE, ResultTypeTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONRESPONSETYPE, TransactionResponseTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONRESULTSTYPE, TransactionResultsTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONSUMMARYTYPE, TransactionSummaryTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TRANSACTIONTYPE, TransactionTypeBinding.class);
        container.registerComponentImplementation(
                WFS.TYPENAMELISTTYPE, TypeNameListTypeBinding.class);
        container.registerComponentImplementation(
                WFS.UPDATEELEMENTTYPE, UpdateElementTypeBinding.class);
        container.registerComponentImplementation(
                WFS.WFS_CAPABILITIESTYPE, WFS_CapabilitiesTypeBinding.class);
        container.registerComponentImplementation(
                WFS.XLINKPROPERTYNAME, XlinkPropertyNameBinding.class);

        // cite specific bindings
        container.registerComponentImplementation(
                FeatureReferenceTypeBinding.FeatureReferenceType,
                FeatureReferenceTypeBinding.class);
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void addDependency(Configuration dependency) {
        // override to make public
        super.addDependency(dependency);
    }

    protected void configureContext(MutablePicoContainer context) {
        super.configureContext(context);

        context.registerComponentInstance(WfsFactory.eINSTANCE);
        context.registerComponentInstance(new WFSHandlerFactory(catalog, schemaBuilder));
        context.registerComponentInstance(catalog);
        context.registerComponentImplementation(PropertyTypePropertyExtractor.class);
        context.registerComponentInstance(getSrsSyntax());

        // seed the cache with entries from the catalog
        context.registerComponentInstance(
                FeatureTypeCache.class, new CatalogFeatureTypeCache(getCatalog()));

        context.registerComponentInstance(new CurvedGeometryFactory(Double.MAX_VALUE));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureBindings(Map bindings) {
        // register our custom bindings
        bindings.put(OGC.FilterType, FilterTypeBinding.class);
        bindings.put(OGC.PropertyNameType, PropertyNameTypeBinding.class);
        bindings.put(GML.CircleType, CircleTypeBinding.class);

        WFSXmlUtils.registerAbstractGeometryTypeBinding(this, bindings, GML.AbstractGeometryType);

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

        // override XSQName binding
        bindings.put(XS.QNAME, XSQNameBinding.class);
    }

    public FeatureTypeSchemaBuilder getSchemaBuilder() {
        return schemaBuilder;
    }
}
