/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.io.File;
import java.io.IOException;
import org.geoserver.config.GeoServerDataDirectory;
import org.geotools.util.PreventLocalEntityResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * In IDEs during development GeoTools sources can be in the classpath of GeoServer tests, this resolver allows them to
 * be resolved while blocking the rest.
 */
public class DevModeEntityResolver extends PreventLocalEntityResolver {

    public static final EntityResolver INSTANCE = new DevModeEntityResolver();

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        if (isLocalGeoToolsSchema(null, systemId) || isDataDirectory(systemId)) {
            return null;
        } else if (isClassResource(null, systemId)) {
            return null;
        }
        return super.resolveEntity(publicId, systemId);
    }

    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
            throws SAXException, IOException {
        if (isLocalGeoToolsSchema(baseURI, systemId) || isDataDirectory(systemId)) {
            return null;
        } else if (isClassResource(baseURI, systemId)) {
            return null;
        }
        return super.resolveEntity(name, publicId, baseURI, systemId);
    }

    private boolean isClassResource(String baseURI, String systemId) throws SAXException {
        if (systemId.startsWith("../") || !systemId.contains("/")) {
            // relative to local file being parsed
            return true;
        } else if (!systemId.contains("://") && baseURI != null) {
            // location relative to a baseURI
            String path = baseURI.toLowerCase();
            return path.contains("../");
        }
        return false;
    }

    private boolean isLocalGeoToolsSchema(String baseURI, String systemId) {
        if (systemId.startsWith("file:/")) {
            return isLocalGeotoolsSchema(systemId);
        } else if (!systemId.contains("://") && baseURI != null) {
            // location relative to a baseURI
            return isLocalGeotoolsSchema(baseURI);
        }
        return false;
    }

    private boolean isLocalGeotoolsSchema(String path) {
        // Windows case insensitive filesystem work-around
        path = path.toLowerCase();
        // Match the GeoTools locations having schemas we resolve against
        return path.matches(".*modules[\\\\/]extension[\\\\/]xsd[\\\\/].*\\.xsd")
                || path.matches(".*modules[\\\\/]ogc[\\\\/].*\\.xsd");
    }

    private boolean isDataDirectory(String systemId) {
        if (GeoServerSystemTestSupport.applicationContext != null) {
            GeoServerDataDirectory dd =
                    GeoServerSystemTestSupport.applicationContext.getBean(GeoServerDataDirectory.class);
            try {
                String path = dd.getRoot("workspaces").dir().getCanonicalPath();
                if (systemId.startsWith("file:")) systemId = systemId.substring(5);
                String canonicalSystemId = new File(systemId).getCanonicalPath();
                return canonicalSystemId.startsWith(path);
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DevModeEntityResolver";
    }
}
