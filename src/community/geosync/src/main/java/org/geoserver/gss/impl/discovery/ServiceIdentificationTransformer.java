/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import static org.geotools.ows.v1_1.OWS.Abstract;
import static org.geotools.ows.v1_1.OWS.AccessConstraints;
import static org.geotools.ows.v1_1.OWS.Fees;
import static org.geotools.ows.v1_1.OWS.Keywords;
import static org.geotools.ows.v1_1.OWS.ServiceIdentification;
import static org.geotools.ows.v1_1.OWS.ServiceType;
import static org.geotools.ows.v1_1.OWS.Title;

import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.catalog.KeywordInfo;
import org.geoserver.gss.config.GSSInfo;
import org.geotools.ows.v1_1.OWS;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.NamespaceSupport;

class ServiceIdentificationTransformer extends AbstractTransformer {

    private NamespaceSupport namespaceSupport;

    public ServiceIdentificationTransformer(NamespaceSupport namespaceSupport) {
        this.namespaceSupport = namespaceSupport;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ServiceIdentificationTranslator(handler, namespaceSupport);
    }

    private class ServiceIdentificationTranslator extends AbstractTranslator {

        public ServiceIdentificationTranslator(ContentHandler handler,
                NamespaceSupport namespaceSupport) {
            super(handler, null, null);
            super.nsSupport = namespaceSupport;
        }

        public void encode(Object o) throws IllegalArgumentException {
            GSSInfo gss = (GSSInfo) o;
            start(ServiceIdentification);

            element(Title, null, gss.getTitle());
            element(Abstract, null, gss.getAbstract());

            keywords(gss.getKeywords());

            element(ServiceType, null, "GSS");
            element(new QName(OWS.NAMESPACE, "ServiceTypeVersion"), null, "1.0.0");

            element(Fees, null, gss.getFees());
            element(AccessConstraints, null, gss.getAccessConstraints());

            end(ServiceIdentification);
        }

        void keywords(List<KeywordInfo> list) {
            if ((list == null) || (list.size() == 0)) {
                return;
            }

            start(Keywords);
            QName Keyword = new QName(OWS.NAMESPACE, "Keyword");
            for (KeywordInfo kw : list) {
                if (kw != null && kw.getValue() != null) {
                    element(Keyword, null, kw.getValue());
                }
            }

            end(Keywords);
        }
    }
}
