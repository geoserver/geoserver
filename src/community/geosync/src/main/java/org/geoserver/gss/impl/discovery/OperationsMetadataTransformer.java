/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.impl.discovery;

import static org.geotools.ows.v1_1.OWS.DCP;
import static org.geotools.ows.v1_1.OWS.HTTP;
import static org.geotools.ows.v1_1.OWS.NAMESPACE;
import static org.geotools.ows.v1_1.OWS.Operation;
import static org.geotools.ows.v1_1.OWS.OperationsMetadata;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.referencing.CRS;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Transformer for the {@code OperationsMetadata} section, receives a String for the GeoServer's
 * base URL.
 * 
 * @author Gabriel Roldan
 * 
 */
class OperationsMetadataTransformer extends AbstractTransformer {

    private NamespaceSupport namespaceSupport;

    private Set<String> getEntriesOutputFormats;

    public OperationsMetadataTransformer(NamespaceSupport namespaceSupport) {
        this.namespaceSupport = namespaceSupport;
        this.getEntriesOutputFormats = new HashSet<String>();
    }

    public void setGetEntriesOutputFormats(Set<String> getEntriesOutputFormats) {
        this.getEntriesOutputFormats.clear();
        if (getEntriesOutputFormats != null) {
            this.getEntriesOutputFormats.addAll(getEntriesOutputFormats);
        }
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new OperationsMetadataTranslator(handler, namespaceSupport);
    }

    private class OperationsMetadataTranslator extends AbstractTranslator {

        public OperationsMetadataTranslator(ContentHandler handler,
                NamespaceSupport namespaceSupport) {
            super(handler, null, null);
            super.nsSupport = namespaceSupport;
        }

        public void encode(Object o) throws IllegalArgumentException {
            final String baseURL = (String) o;
            final String serviceURL = ResponseUtils.buildURL(baseURL, "ows", null, URLType.SERVICE);

            start(OperationsMetadata);

            startOperation(serviceURL, "GetCapabilities", true, false);
            operationParameter("AcceptVersions", "1.0.0");
            operationParameter("AcceptFormats", "text/xml");
            operationParameter("Sections", "ServiceIdentification", "ServiceProvider",
                    "OperationsMetadata", "Service", "Filter_Capabilities");
            end(Operation);

            startOperation(serviceURL, "Transaction", false, true);
            end(Operation);

            startOperation(serviceURL, "GetEntries", true, true);
            if (getEntriesOutputFormats.size() > 0) {
                String[] ofmts;
                ofmts = getEntriesOutputFormats.toArray(new String[getEntriesOutputFormats.size()]);
                operationParameter("outputFormat", ofmts);
            }
            end(Operation);

            startOperation(serviceURL, "AcceptChange", true, true);
            end(Operation);

            startOperation(serviceURL, "RejectChange", true, true);
            end(Operation);

            startOperation(serviceURL, "CreateTopic", true, true);
            end(Operation);

            startOperation(serviceURL, "RemoveTopic", true, true);
            end(Operation);

            startOperation(serviceURL, "ListTopics", true, true);
            end(Operation);

            startOperation(serviceURL, "Subscribe", true, true);
            end(Operation);

            startOperation(serviceURL, "ListSubscriptions", true, true);
            end(Operation);

            startOperation(serviceURL, "PauseSubscription", true, true);
            end(Operation);

            startOperation(serviceURL, "ResumeSubscription", true, true);
            end(Operation);

            startOperation(serviceURL, "CancelSubscription", true, true);
            end(Operation);

            startOperation(serviceURL, "ListSubscriptions", true, true);
            end(Operation);

            // Parameters required by all operations
            operationParameter("service", "GSS");
            operationParameter("version", "1.0.0");

            Collection<String> supportedCodes = CRS.getSupportedCodes("EPSG");
            Set<Long> plainCodes = new TreeSet<Long>();
            for (String code : supportedCodes) {
                if ("WGS84(DD)".equals(code)) {
                    continue;
                }
                try {
                    plainCodes.add(Long.valueOf(code));
                } catch (NumberFormatException ignore) {
                    continue;
                }
            }

            String[] allowedValues = new String[plainCodes.size()];
            int i = 0;
            for (Long code : plainCodes) {
                allowedValues[i] = "urn:ogc:def:crs:EPSG::" + code;
                i++;
            }
            operationParameter("srsName", allowedValues);

            end(OperationsMetadata);
        }

        private void operationParameter(final String parameterName, String... allowedValues) {
            QName parameter = new QName(NAMESPACE, "Parameter");
            QName allowed = new QName(NAMESPACE, "AllowedValues");
            QName value = new QName(NAMESPACE, "Value");

            AttributesImpl paramName = attributes("name", parameterName);
            start(parameter, paramName);
            start(allowed);
            for (String allowedValue : allowedValues) {
                element(value, null, allowedValue);
            }
            end(allowed);
            end(parameter);
        }

        private void startOperation(final String serviceURL, final String operationName,
                boolean methodGET, boolean methodPOST) {
            AttributesImpl atts = attributes("name", operationName);
            start(Operation, atts);
            if (methodGET || methodPOST) {
                start(DCP);
                start(HTTP);
                AttributesImpl url = attributes("xlink:href", serviceURL);
                if (methodGET) {
                    element(new QName(NAMESPACE, "Get"), url, null);
                }
                if (methodPOST) {
                    element(new QName(NAMESPACE, "Post"), url, null);
                }
                end(HTTP);
                end(DCP);
            }
        }
    }
}
