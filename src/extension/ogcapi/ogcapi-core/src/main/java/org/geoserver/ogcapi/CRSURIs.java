/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.crs.CapabilitiesCRSProvider;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;

/**
 * The CRS identifiers an OGC API service publishes, always as OGC URIs. The SafeCURIE and URN forms are accepted on
 * input only, see {@link APIBBoxParser}.
 */
public final class CRSURIs {

    private CRSURIs() {}

    /**
     * The OGC URI of an authority code, e.g. {@code http://www.opengis.net/def/crs/EPSG/0/4326} for {@code EPSG:4326}
     * or for a bare {@code 4326}, which the service SRS lists are allowed to use.
     */
    public static String uri(String srs) {
        return SrsSyntax.OGC_HTTP_URI.getSRS(srs);
    }

    /**
     * The OGC URI of a CRS, {@link CollectionExtents#WGS84} for CRS84, which has an authority and a version of its own.
     */
    public static String uri(CoordinateReferenceSystem crs) throws FactoryException {
        if (CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) return CollectionExtents.WGS84;
        return uri(ResourcePool.lookupIdentifier(crs, false));
    }

    /**
     * The CRSs a service can deliver, {@link CollectionExtents#WGS84} first: the configured SRS list, or every code the
     * referencing database knows when that list is empty.
     *
     * @param configuredSRS the service SRS list, authority codes or bare EPSG numbers, may be null or empty
     * @return a mutable list, so callers can apply per-resource overrides on top
     */
    public static List<String> serviceList(List<String> configuredSRS) {
        if (configuredSRS != null && !configuredSRS.isEmpty()) return list(configuredSRS);

        CapabilitiesCRSProvider provider = new CapabilitiesCRSProvider();
        provider.getAuthorityExclusions().add("CRS");
        provider.setCodeMapper(SrsSyntax.OGC_HTTP_URI::getSRS);
        return crs84First(new ArrayList<>(provider.getCodes()));
    }

    /**
     * The same list as {@link #serviceList(List)}, but with no fallback: an empty input yields
     * {@link CollectionExtents#WGS84} alone. This is what a per-resource SRS override means.
     */
    public static List<String> list(List<String> srsList) {
        List<String> result = new ArrayList<>(srsList.size() + 1);
        for (String srs : srsList) result.add(uri(srs));
        return crs84First(result);
    }

    private static List<String> crs84First(List<String> uris) {
        // CRS84 is always supported, and is spelled differently from anything the lists above hold
        uris.remove(CollectionExtents.WGS84);
        uris.add(0, CollectionExtents.WGS84);
        return uris;
    }
}
