/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.logging.Level;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CRSFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.AbstractIdentifiedObject;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.wms.WebCRSFactory;

/**
 * A factory for Coordinate Reference Systems (CRS) that overrides the default CRS authority and binds the CRS codes to
 * the equivalent EPSG CRSs, ensuring that accurate reprojection leveraging the EPSG database knowledge can be used.
 */
public class GeoServerOverridingWebFactory extends WebCRSFactory {

    private static final Map<Integer, String> CRS_TO_EPSG = Map.of(
            84, "EPSG:4326", // WGS 84
            83, "EPSG:4269", // NAD83
            27, "EPSG:4267" // NAD27
            );

    public GeoServerOverridingWebFactory() {
        super(null);
    }

    @Override
    public int getPriority() {
        return MAXIMUM_PRIORITY;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(String code) throws FactoryException {
        final int i = getIntegerCode(code);
        if (CRS_TO_EPSG.containsKey(i)) {
            try {
                // get the CRS from the EPSG authority, preserving all extra information in the EPSG database
                GeographicCRS crs = (GeographicCRS) CRS.decode(CRS_TO_EPSG.get(i), true);
                CRSFactory crsFactory = ReferencingFactoryFinder.getCRSFactory(null);
                Map<String, Object> properties = new HashMap<>(AbstractIdentifiedObject.getProperties(crs));
                // add the CRS identifier as first, then the others
                LinkedHashSet<ReferenceIdentifier> identifiers = new LinkedHashSet<>();
                identifiers.add(new NamedIdentifier(Citations.CRS, String.valueOf(i)));
                identifiers.addAll(crs.getIdentifiers());
                properties.put(
                        AbstractIdentifiedObject.IDENTIFIERS_KEY, identifiers.toArray(n -> new ReferenceIdentifier[n]));
                properties.put(AbstractIdentifiedObject.NAME_KEY, crs.getName().getCode() + " longitude-latitude");
                return crsFactory.createGeographicCRS(properties, crs.getDatum(), crs.getCoordinateSystem());
            } catch (NoSuchAuthorityCodeException e) {
                LOGGER.log(Level.WARNING, "Could not decode EPSG code: {0}", code);
            }
        }

        LOGGER.log(
                Level.WARNING,
                "Could not find a EPSG equivalent for code: {0}, falling back to default implementation",
                code);
        // fall back to the default implementation
        return super.createCoordinateReferenceSystem(code);
    }
}
