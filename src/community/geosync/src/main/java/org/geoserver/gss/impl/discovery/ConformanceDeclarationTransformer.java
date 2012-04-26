/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import org.geoserver.gss.config.GSSInfo;
import org.geoserver.gss.xml.GSSSchema;
import org.geotools.xml.transform.Translator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Transformer for the {@code ConformanceDeclaration} section.
 * 
 * @author Gabriel Roldan
 * 
 */
class ConformanceDeclarationTransformer extends AbstractTransformer {

    private NamespaceSupport namespaceSupport;

    public ConformanceDeclarationTransformer(NamespaceSupport namespaceSupport) {
        this.namespaceSupport = namespaceSupport;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ConformanceDeclarationTranslator(handler, namespaceSupport);
    }

    private class ConformanceDeclarationTranslator extends AbstractTranslator {

        public ConformanceDeclarationTranslator(ContentHandler handler,
                NamespaceSupport namespaceSupport) {
            super(handler, null, null);
            super.nsSupport = namespaceSupport;
        }

        /**
         * @param o
         *            {@link GSSInfo}
         * @see org.geotools.xml.transform.Translator#encode(java.lang.Object)
         */
        public void encode(Object o) throws IllegalArgumentException {
            // not used yet. It should contain some kind of information about the conformance levels
            GSSInfo serviceInfo = (GSSInfo) o;
            start(GSSSchema.ConformanceDeclaration);

            conformanceClass("Discovery_OGC_POX", false);
            conformanceClass("Discovery_OGC_KVP", true);
            conformanceClass("Discovery_SOAP", false);
            conformanceClass("Transaction_OGC_POX", false);
            conformanceClass("Transaction_REST", false);
            conformanceClass("Transaction_SOAP", false);
            conformanceClass("Query_OGC_POX", false);
            conformanceClass("Query_OGC_KVP", true);
            conformanceClass("Query_REST", false);
            conformanceClass("Query_SOAP", false);
            conformanceClass("ChangeManagement_OGC_POX", true);
            conformanceClass("ChangeManagement_OGC_KVP", true);
            conformanceClass("ChangeManagement_REST", false);
            conformanceClass("ChangeManagement_SOAP", false);
            conformanceClass("TopicManagement_OGC_POX", true);
            conformanceClass("TopicManagement_OGC_KVP", true);
            conformanceClass("TopicManagement_REST", false);
            conformanceClass("TopicManagement_SOAP", false);
            conformanceClass("ActiveNotification_OGC_POX", true);
            conformanceClass("ActiveNotification_OGC_KVP", true);
            conformanceClass("ActiveNotification_REST", false);
            conformanceClass("ActiveNotification_SOAP", false);
            conformanceClass("Synchronization_OGC_POX", false);
            conformanceClass("Synchronization_OGC_KVP", true);
            conformanceClass("Synchronization_REST", false);
            conformanceClass("Synchronization_SOAP", false);

            end(GSSSchema.ConformanceDeclaration);
        }

        private void conformanceClass(final String conformanceClassName, final boolean conformant) {
            Attributes name = attributes("name", conformanceClassName);
            element(GSSSchema.ConformanceClass, name, String.valueOf(conformant));
        }
    }
}
