/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import net.opengis.wfs.DescribeFeatureTypeType;
import net.opengis.wfs.WfsFactory;
import org.eclipse.emf.ecore.EFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.xml.sax.helpers.NamespaceSupport;

public class DescribeFeatureTypeKvpRequestReader extends WFSKvpRequestReader {

    private final Catalog catalog;

    public DescribeFeatureTypeKvpRequestReader(final Catalog catalog) {
        super(DescribeFeatureTypeType.class, WfsFactory.eINSTANCE);
        this.catalog = catalog;
    }

    public DescribeFeatureTypeKvpRequestReader(
            final Catalog catalog, Class requestBean, EFactory factory) {
        super(requestBean, factory);
        this.catalog = catalog;
    }

    @SuppressWarnings("unchecked")
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // let super do its thing
        request = super.read(request, kvp, rawKvp);

        // do an additional check for outputFormat, because the default
        // in wfs 1.1 is not the default for wfs 1.0
        DescribeFeatureTypeRequest req = DescribeFeatureTypeRequest.adapt(request);

        if (!req.isSetOutputFormat()) {
            switch (WFSInfo.Version.negotiate(req.getVersion())) {
                case V_10:
                    req.setOutputFormat("XMLSCHEMA");
                    break;
                case V_11:
                    req.setOutputFormat("text/xml; subtype=gml/3.1.1");
                    break;
                case V_20:
                default:
                    req.setOutputFormat("application/gml+xml; version=3.2");
            }
        }

        // handle the name differences in property names between 1.1 and 2.0
        // The specification here is inconsistent, the KVP param table says "TYPENAME",
        // but an explanation just below states KVP should be using TYPENAMES and CITE users the
        // latter
        // So let's support both...
        if (req instanceof DescribeFeatureTypeRequest.WFS20 && kvp.containsKey("typenames")) {
            List<List<QName>> typenames = (List<List<QName>>) kvp.get("typenames");
            req.getTypeNames().clear();
            req.getTypeNames().addAll(typenames.get(0));
        }

        // did the user supply alternate namespace prefixes?
        NamespaceSupport namespaces = null;
        if (kvp.containsKey("NAMESPACE") || kvp.containsKey("NAMESPACES")) {
            if (kvp.get("NAMESPACE") instanceof NamespaceSupport) {
                namespaces = (NamespaceSupport) kvp.get("namespace");
            } else if (kvp.get("NAMESPACES") instanceof NamespaceSupport) {
                namespaces = (NamespaceSupport) kvp.get("namespaces");
            } else {
                LOGGER.warning(
                        "There's a namespace parameter but it seems it wasn't parsed to a "
                                + NamespaceSupport.class.getName()
                                + ": "
                                + kvp.get("namespace"));
            }
        }
        if (namespaces != null) {
            List<QName> typeNames = req.getTypeNames();
            List<QName> newList = new ArrayList<QName>(typeNames.size());
            for (QName name : typeNames) {
                String localPart = name.getLocalPart();
                String prefix = name.getPrefix();
                String namespaceURI = name.getNamespaceURI();
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                    // no prefix specified, did the request specify a default namespace?
                    namespaceURI = namespaces.getURI(XMLConstants.DEFAULT_NS_PREFIX);
                } else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                    // prefix specified, does a namespace mapping were declared for it?
                    if (namespaces.getURI(prefix) != null) {
                        namespaceURI = namespaces.getURI(prefix);
                    }
                }
                if (catalog.getNamespaceByURI(namespaceURI) != null) {
                    prefix = catalog.getNamespaceByURI(namespaceURI).getPrefix();
                }
                newList.add(new QName(namespaceURI, localPart, prefix));
            }
            req.setTypeNames(newList);
        }
        return request;
    }
}
