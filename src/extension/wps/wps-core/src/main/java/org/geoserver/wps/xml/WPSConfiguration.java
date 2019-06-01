/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.xml;

import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.CatalogNamespaceSupport;
import org.geoserver.wfs.xml.v1_0_0.GetFeatureTypeBinding;
import org.geotools.wfs.WFSParserDelegate;
import org.geotools.wfs.v1_0.WFS;
import org.geotools.wfs.v1_1.WFSConfiguration;
import org.geotools.wfs.v2_0.bindings.CopyingHandler;
import org.geotools.wps.WPS;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.ParserDelegate;
import org.geotools.xsd.ParserDelegate2;
import org.geotools.xsd.XSDParserDelegate;
import org.geotools.xsd.impl.Handler;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.NamespaceSupport;

public class WPSConfiguration extends org.geotools.wps.WPSConfiguration {

    protected void registerBindings(Map bindings) {
        super.registerBindings(bindings);

        // binding overrides
        bindings.put(WPS.ComplexDataType, ComplexDataTypeBinding.class);
    }

    @Override
    protected void configureContext(MutablePicoContainer container) {
        super.configureContext(container);

        container.registerComponentInstance(new org.geoserver.wcs.xml.v1_1_1.WCSParserDelegate());
        container.registerComponentInstance(new org.geoserver.wcs.xml.v1_0_0.WCSParserDelegate());
        container.registerComponentInstance(new org.geoserver.wcs2_0.xml.WCSParserDelegate());
        container.registerComponentInstance(container);
        // replace WFSParserDelegate from GeoTools with a new one using GeoServer
        // GetFeatureTypeBinding,
        // able to parse viewParams attribute and enable usage of SQL views
        Object wfs = container.getComponentInstanceOfType(WFSParserDelegate.class);
        container.unregisterComponentByInstance(wfs);
        // XSDParserDelegate with CatalogNamespaceSupport
        container.registerComponentInstance(
                new WPSInternalXSDParserDelegate(
                        new WFSConfiguration() {

                            @Override
                            protected void configureBindings(MutablePicoContainer container) {
                                super.configureBindings(container);
                                container.registerComponentImplementation(
                                        WFS.GetFeatureType, GetFeatureTypeBinding.class);
                            }
                        },
                        new CatalogNamespaceSupport(
                                GeoServerExtensions.bean(LocalWorkspaceCatalog.class))));
        container.registerComponentImplementation(ComplexDataHandler.class);
    }

    public static class ComplexDataHandler extends CopyingHandler
            implements ParserDelegate, ParserDelegate2 {

        private List<ParserDelegate> delegates;
        private final PicoContainer container;
        String result = null;

        public ComplexDataHandler(NamespaceSupport ns, PicoContainer container) {
            super(ns);
            this.container = container;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (buffer == null) {
                buffer = new StringBuffer();
            }

            String escapedXML = StringEscapeUtils.escapeXml10(new String(ch, start, length));
            buffer.append(escapedXML);
        }

        @Override
        public boolean canHandle(
                QName elementName, Attributes attributes, Handler handler, Handler parent) {
            if (parent == null || !("ComplexData".equals(parent.getComponent().getName()))) {
                return false;
            }

            // make sure we're not going over the toes of any other delegate
            for (ParserDelegate delegate : getDelegates()) {
                // skip copies of self
                if (delegate instanceof ComplexDataHandler) {
                    continue;
                }
                if (delegate instanceof ParserDelegate
                        && delegate.canHandle(elementName, attributes, handler, parent)) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void endDocument() throws SAXException {
            this.result = buffer != null ? buffer.toString() : null;
            this.buffer = null;
        }

        @Override
        public Object getParsedObject() {
            return result;
        }

        public List<ParserDelegate> getDelegates() {
            if (this.delegates == null) {
                this.delegates = container.getComponentInstancesOfType(ParserDelegate.class);
            }
            return this.delegates;
        }
    }

    private static final class WPSInternalXSDParserDelegate extends XSDParserDelegate {

        public WPSInternalXSDParserDelegate(
                Configuration configuration, NamespaceSupport nsSupport) {
            super(configuration);
            handler.getNamespaceSupport().add(nsSupport);
        }
    }
}
