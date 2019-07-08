/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.ResourceErrorHandling;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.request.DescribeFeatureTypeRequest;
import org.geotools.util.logging.Logging;

/**
 * Web Feature Service DescribeFeatureType operation.
 *
 * <p>This operation returns an array of {@link org.geoserver.data.feature.FeatureTypeInfo} metadata
 * objects corresponding to the feature type names specified in the request.
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @version $Id$
 */
public class DescribeFeatureType {
    /** Catalog reference */
    private Catalog catalog;

    /** WFS service */
    private WFSInfo wfs;

    private static Logger LOGGER = Logging.getLogger(DescribeFeatureType.class);

    /**
     * Creates a new wfs 1.0/1.1 DescribeFeatureType operation.
     *
     * @param wfs The wfs configuration
     * @param catalog The geoserver catalog.
     */
    public DescribeFeatureType(WFSInfo wfs, Catalog catalog) {
        this.catalog = catalog;
        this.wfs = wfs;
    }

    public WFSInfo getWFS() {
        return wfs;
    }

    public void setWFS(WFSInfo wfs) {
        this.wfs = wfs;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public FeatureTypeInfo[] run(DescribeFeatureTypeRequest request) throws WFSException {
        List<QName> names = new ArrayList<QName>(request.getTypeNames());

        final boolean citeConformance = getWFS().isCiteCompliant();
        if (!citeConformance) {
            // HACK: as per GEOS-1816, if strict cite compliance is not set, and
            // the user specified a typeName with no namespace prefix, we want
            // it to be interpreted as being in the GeoServer's "default
            // namespace". Yet, the xml parser did its job and since TypeName is
            // of QName type, not having a ns prefix means it got parsed as a
            // QName in the default namespace. That is, in the wfs namespace.
            List<QName> hackedNames = new ArrayList<QName>(names.size());
            final NamespaceInfo defaultNameSpace = catalog.getDefaultNamespace();
            if (defaultNameSpace == null) {
                throw new IllegalStateException("No default namespace configured in GeoServer");
            }
            final String defaultNsUri = defaultNameSpace.getURI();
            for (QName name : names) {
                String nsUri = name.getNamespaceURI();
                if (XMLConstants.NULL_NS_URI.equals(nsUri)
                        || org.geoserver.wfs.xml.v1_1_0.WFS.NAMESPACE.equals(nsUri)
                        || org.geotools.wfs.v2_0.WFS.NAMESPACE.equals(nsUri)) {
                    // for this one we need to assign the default geoserver
                    // namespace
                    name = new QName(defaultNsUri, name.getLocalPart());
                }
                hackedNames.add(name);
            }
            names = hackedNames;
        }

        // list of catalog handles
        List<FeatureTypeInfo> requested = new ArrayList<FeatureTypeInfo>(names.size());

        if (names.isEmpty()) {
            // if there are no specific requested types then get all the ones that
            // are enabled
            final boolean skipMisconfigured =
                    ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(
                            getWFS().getGeoServer().getGlobal().getResourceErrorHandling());

            for (FeatureTypeInfo ftInfo :
                    new ArrayList<FeatureTypeInfo>(catalog.getFeatureTypes())) {
                if (ftInfo.enabled()) {
                    try {
                        ftInfo.getFeatureType(); // check that we can get a connection to this ftype
                        requested.add(ftInfo);
                    } catch (IOException ioe) {
                        if (skipMisconfigured) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Skipping DescribeFeature for "
                                            + ftInfo.prefixedName()
                                            + " because we couldn't connect",
                                    ioe);
                        } else {
                            throw new WFSException(ioe);
                        }
                    }
                }
            }
        } else {
            for (QName name : names) {

                String namespaceURI = name.getNamespaceURI();
                String typeName = name.getLocalPart();
                FeatureTypeInfo typeInfo;
                if (citeConformance && XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                    // under cite conformance, the typeName shall be completely resolved. If there's
                    // no namespace URI and we ask the catalog with only the localName, the catalog
                    // will try to match against the default namespace
                    typeInfo = null;
                } else {
                    typeInfo = catalog.getFeatureTypeByName(namespaceURI, typeName);
                }

                if (typeInfo != null && typeInfo.enabled()) {
                    requested.add(typeInfo);
                } else {
                    // not found
                    String msg = "Could not find type: " + name;
                    if (citeConformance) {
                        msg +=
                                ". \nStrict WFS protocol conformance is being applied.\n"
                                        + "Make sure the type name is correctly qualified";
                    }
                    throw new WFSException(request, msg, ServiceException.INVALID_PARAMETER_VALUE);
                }
            }
        }

        return (FeatureTypeInfo[]) requested.toArray(new FeatureTypeInfo[requested.size()]);
    }
}
