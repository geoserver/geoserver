/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import static org.geotools.ows.v1_1.OWS.ContactInfo;
import static org.geotools.ows.v1_1.OWS.IndividualName;
import static org.geotools.ows.v1_1.OWS.NAMESPACE;
import static org.geotools.ows.v1_1.OWS.PositionName;
import static org.geotools.ows.v1_1.OWS.ServiceProvider;

import javax.xml.namespace.QName;

import org.geoserver.config.ContactInfo;
import org.geotools.xml.transform.Translator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Transformer for a {@link ContactInfo}
 * 
 * @author Gabriel Roldan
 * 
 */
class ServiceProviderTransformer extends AbstractTransformer {

    private NamespaceSupport namespaceSupport;

    public ServiceProviderTransformer(NamespaceSupport namespaceSupport) {
        this.namespaceSupport = namespaceSupport;
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new ServiceProviderTranslator(handler, namespaceSupport);
    }

    private class ServiceProviderTranslator extends AbstractTranslator {

        public ServiceProviderTranslator(ContentHandler handler, NamespaceSupport namespaceSupport) {
            super(handler, null, null);
            super.nsSupport = namespaceSupport;
        }

        public void encode(Object o) throws IllegalArgumentException {
            ContactInfo contact = (ContactInfo) o;
            start(ServiceProvider);

            element(new QName(NAMESPACE, "ProviderName"), null, contact.getContactOrganization());
            Attributes onlineResource = null;
            if (null != contact.getOnlineResource()) {
                onlineResource = attributes("xlink:href", contact.getOnlineResource());
                element(new QName(NAMESPACE, "ProviderSite"), onlineResource, null);
            }

            start(new QName(NAMESPACE, "ServiceContact"));
            element(IndividualName, null, contact.getContactPerson());
            element(PositionName, null, contact.getContactPosition());

            start(ContactInfo);
            if (contact.getContactFacsimile() != null | contact.getContactVoice() != null) {
                start(new QName(NAMESPACE, "Phone"));
                element(new QName(NAMESPACE, "Voice"), null, contact.getContactVoice());
                element(new QName(NAMESPACE, "Facsimile"), null, contact.getContactFacsimile());
                end(new QName(NAMESPACE, "Phone"));
            }
            start(new QName(NAMESPACE, "Address"));
            element(new QName(NAMESPACE, "DeliveryPoint"), null, contact.getAddress());
            element(new QName(NAMESPACE, "City"), null, contact.getAddressCity());
            element(new QName(NAMESPACE, "AdministrativeArea"), null, contact.getAddressState());
            element(new QName(NAMESPACE, "PostalCode"), null, contact.getAddressPostalCode());
            element(new QName(NAMESPACE, "Country"), null, contact.getAddressCountry());
            element(new QName(NAMESPACE, "ElectronicEmailAddress"), null, contact.getContactEmail());
            end(new QName(NAMESPACE, "Address"));

            if (onlineResource != null) {
                element(new QName(NAMESPACE, "OnlineResource"), onlineResource, null);
            }
            end(ContactInfo);
            end(new QName(NAMESPACE, "ServiceContact"));

            end(ServiceProvider);
        }
    }
}
