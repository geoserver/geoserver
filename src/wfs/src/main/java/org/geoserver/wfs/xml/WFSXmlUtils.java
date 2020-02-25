/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml;

import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.XmlRequestReader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.CatalogNamespaceSupport;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.gml3.AbstractGeometryTypeBinding;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.SrsSyntax;
import org.geotools.util.Converters;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.OptionalComponentParameter;
import org.geotools.xsd.Parser;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.PicoContainer;
import org.picocontainer.defaults.BasicComponentParameter;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;
import org.xml.sax.InputSource;

/**
 * Some utilities shared among WFS xml readers/writers.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class WFSXmlUtils {

    public static final String ENTITY_EXPANSION_LIMIT =
            "org.geoserver.wfs.xml.entityExpansionLimit";

    public static final String XLINK_DEFAULT_PREFIX = "xlink";

    public static void initRequestParser(Parser parser, WFSInfo wfs, GeoServer geoServer, Map kvp) {
        // check the strict flag to determine if we should validate or not
        Boolean strict = (Boolean) kvp.get("strict");
        if (strict == null) {
            strict = Boolean.FALSE;
        }

        // check for cite compliance, we always validate for cite
        if (wfs.isCiteCompliant()) {
            strict = Boolean.TRUE;
        }
        parser.setValidating(strict.booleanValue());
        WFSURIHandler.addToParser(geoServer, parser);

        Catalog catalog = geoServer.getCatalog();

        // "inject" namespace mappings
        parser.getNamespaces().add(new CatalogNamespaceSupport(catalog));
    }

    public static Object parseRequest(Parser parser, Reader reader, WFSInfo wfs) throws Exception {
        // set the input source with the correct encoding
        InputSource source = new InputSource(reader);
        source.setEncoding(wfs.getGeoServer().getSettings().getCharset());

        return parser.parse(source);
    }

    public static void checkValidationErrors(Parser parser, XmlRequestReader requestReader) {
        // TODO: HACK, disabling validation for transaction
        if (!"Transaction".equalsIgnoreCase(requestReader.getElement().getLocalPart())) {
            if (!parser.getValidationErrors().isEmpty()) {
                WFSException exception =
                        new WFSException("Invalid request", "InvalidParameterValue");

                for (Iterator e = parser.getValidationErrors().iterator(); e.hasNext(); ) {
                    Exception error = (Exception) e.next();
                    exception.getExceptionText().add(error.getLocalizedMessage());
                }

                throw exception;
            }
        }
    }

    public static void initWfsConfiguration(
            Configuration config, GeoServer gs, FeatureTypeSchemaBuilder schemaBuilder) {

        MutablePicoContainer context = config.getContext();

        // seed the cache with entries from the catalog
        FeatureTypeCache featureTypeCache = new FeatureTypeCache();

        Collection featureTypes = gs.getCatalog().getFeatureTypes();
        for (Iterator f = featureTypes.iterator(); f.hasNext(); ) {
            FeatureTypeInfo meta = (FeatureTypeInfo) f.next();
            if (!meta.enabled()) {
                continue;
            }

            FeatureType featureType = null;
            try {
                featureType = meta.getFeatureType();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            featureTypeCache.put(featureType);
        }

        // add the wfs handler factory to handle feature elements
        context.registerComponentInstance(featureTypeCache);
        context.registerComponentInstance(new WFSHandlerFactory(gs.getCatalog(), schemaBuilder));
    }

    public static void registerAbstractGeometryTypeBinding(
            final Configuration config, Map bindings, QName qName) {
        // use setter injection for AbstractGeometryType bindign to allow an
        // optional crs to be set in teh binding context for parsing, this crs
        // is set by the binding of a parent element.
        // note: it is important that this component adapter is non-caching so
        // that the setter property gets updated properly every time
        bindings.put(
                qName,
                new SetterInjectionComponentAdapter(
                        qName,
                        AbstractGeometryTypeBinding.class,
                        new Parameter[] {
                            new OptionalComponentParameter(CoordinateReferenceSystem.class),
                            new DirectObjectParameter(config, Configuration.class),
                            new DirectObjectParameter(getSrsSyntax(config), SrsSyntax.class)
                        }));
    }

    public static SrsSyntax getSrsSyntax(Configuration obj) {
        for (Configuration dep : ((List<Configuration>) obj.getDependencies())) {
            if (dep instanceof org.geotools.gml2.GMLConfiguration) {
                return ((org.geotools.gml2.GMLConfiguration) dep).getSrsSyntax();
            }
            if (dep instanceof org.geotools.gml3.GMLConfiguration) {
                return ((org.geotools.gml3.GMLConfiguration) dep).getSrsSyntax();
            }
        }
        return null;
    }

    public static void setSrsSyntax(Configuration obj, SrsSyntax srsSyntax) {
        for (Configuration dep : ((List<Configuration>) obj.getDependencies())) {
            if (dep instanceof org.geotools.gml2.GMLConfiguration) {
                ((org.geotools.gml2.GMLConfiguration) dep).setSrsSyntax(srsSyntax);
            }
            if (dep instanceof org.geotools.gml3.GMLConfiguration) {
                ((org.geotools.gml3.GMLConfiguration) dep).setSrsSyntax(srsSyntax);
            }
        }
    }

    /**
     * Returns the Entity Expansion Limit configuration from system property
     * "org.geoserver.wfs.xml.entityExpansionLimit". Returns 100 as default if no system property is
     * configured.
     */
    public static Integer getEntityExpansionLimitConfiguration() {
        return Optional.ofNullable(GeoServerExtensions.getProperty(ENTITY_EXPANSION_LIMIT))
                .map(p -> Converters.convert(p, Integer.class))
                .orElse(100);
    }

    static class DirectObjectParameter extends BasicComponentParameter {
        Object obj;
        Class clazz;

        public DirectObjectParameter(Object obj, Class clazz) {
            super(clazz);
            this.obj = obj;
            this.clazz = clazz;
        }

        public boolean isResolvable(
                PicoContainer container, ComponentAdapter adapter, Class expectedType) {
            if (clazz.isAssignableFrom(expectedType)) {
                return true;
            }
            return super.isResolvable(container, adapter, expectedType);
        };

        @Override
        public Object resolveInstance(
                PicoContainer container, ComponentAdapter adapter, Class expectedType) {
            if (clazz.isAssignableFrom(expectedType)) {
                return obj;
            }
            return super.resolveInstance(container, adapter, expectedType);
        }
    }
}
