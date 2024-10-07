/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.geotools.api.metadata.Identifier;
import org.geotools.api.metadata.citation.Citation;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.IdentifiedObject;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.api.referencing.crs.SingleCRS;
import org.geotools.api.referencing.cs.CartesianCS;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.api.referencing.cs.EllipsoidalCS;
import org.geotools.api.referencing.datum.Datum;
import org.geotools.api.referencing.datum.GeodeticDatum;
import org.geotools.api.referencing.operation.Conversion;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml2.bindings.GML2EncodingUtils;
import org.geotools.metadata.iso.IdentifierImpl;
import org.geotools.metadata.iso.citation.CitationImpl;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.NamedIdentifier;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.factory.AllAuthoritiesFactory;
import org.geotools.referencing.factory.AuthorityFactoryAdapter;
import org.geotools.util.factory.Hints;

/**
 * Exposes available {@link TiledCRS} to GeoServer as valid Coordinate Reference Systems. The
 * current implementation loads a database of TiledCRS from TiledCRSConstant, will use the TCRS
 * database in the future, if those are made to be configurable
 */
public class TiledCRSFactory extends AuthorityFactoryAdapter implements CRSAuthorityFactory {
    /** The authority prefix */
    public static final String AUTHORITY = "MapML";

    public static final Citation MAPML;

    static {
        final CitationImpl c = new CitationImpl("MapML");
        c.getIdentifiers().add(new IdentifierImpl(AUTHORITY));
        c.freeze();
        MAPML = c;
    }

    /** Builds the MapML CRS factory with no hints. */
    public TiledCRSFactory() {
        this(null);
    }

    /** Constructs a default factory for the {@code CRS} authority. */
    public TiledCRSFactory(Hints hints) {
        super(new AllAuthoritiesFactory(hints));
    }

    /** Returns the authority for this factory, which is {@link Citations#CRS CRS}. */
    @Override
    public Citation getAuthority() {
        return MAPML;
    }

    @Override
    public Set<String> getAuthorityCodes(Class<? extends IdentifiedObject> type)
            throws FactoryException {
        return TiledCRSConstants.tiledCRSDefinitions.values().stream()
                .map(TiledCRSParams::getName)
                .collect(Collectors.toSet());
    }

    @Override
    public CoordinateReferenceSystem createCoordinateReferenceSystem(String code)
            throws FactoryException, NoSuchAuthorityCodeException {
        final CRSAuthorityFactory factory = getCRSAuthorityFactory(code);
        CoordinateReferenceSystem crs =
                factory.createCoordinateReferenceSystem(toBackingFactoryCode(code));
        final CoordinateReferenceSystem replaced = replace(crs, code);
        notifySuccess("createCoordinateReferenceSystem", code, factory, replaced);

        return replaced;
    }

    @Override
    protected String toBackingFactoryCode(String code) throws FactoryException {
        String identifier = getIdentifier(code);

        TiledCRSParams definition = TiledCRSConstants.lookupTCRSParams(identifier);
        if (definition == null) {
            throw new NoSuchAuthorityCodeException("No such CRS: " + code, AUTHORITY, code);
        }

        // get the backing store code in form "authority:code", axis order handling
        // happens in wrappers of this factory
        String definitionCode = definition.getCode();
        CoordinateReferenceSystem crs = CRS.decode(definitionCode);
        return GML2EncodingUtils.toURI(crs, SrsSyntax.AUTH_CODE, true);
    }

    /**
     * Grabs the identifier from the code, which may be in the form "authority:code" or just "code"
     *
     * @param code
     * @return
     */
    private String getIdentifier(String code) {
        String identifier = trimAuthority(code).toUpperCase();
        if (identifier.startsWith(AUTHORITY.toUpperCase())) {
            identifier = identifier.substring(AUTHORITY.length() + 1);
        }
        return identifier;
    }

    /**
     * Replaces the CRS with one that has the MapML identifiers. Avoids going throught the CRS
     * factory to avoid axis flipping issues.
     *
     * @throws FactoryException if the CRS is not supported
     */
    protected CoordinateReferenceSystem replace(CoordinateReferenceSystem crs, String code)
            throws FactoryException {

        final Datum datum;
        if (crs instanceof SingleCRS) {
            datum = ((SingleCRS) crs).getDatum();
        } else {
            datum = null;
        }
        CoordinateSystem cs = crs.getCoordinateSystem();
        final Map<String, ?> properties = getProperties(crs, code);

        if (crs instanceof ProjectedCRS) {
            final ProjectedCRS projectedCRS = (ProjectedCRS) crs;
            final CoordinateReferenceSystem baseCRS = projectedCRS.getBaseCRS();
            Conversion fromBase = projectedCRS.getConversionFromBase();
            return new DefaultProjectedCRS(
                    properties,
                    fromBase,
                    (GeographicCRS) baseCRS,
                    ((ProjectedCRS) crs).getConversionFromBase().getMathTransform(),
                    (CartesianCS) cs);
        } else if (crs instanceof GeographicCRS) {
            return new DefaultGeographicCRS(properties, (GeodeticDatum) datum, (EllipsoidalCS) cs);
        }

        // do not know of a way to attach the code to the copy
        throw new IllegalArgumentException("Unsupported crs: " + crs);
    }

    /**
     * Returns the properties to be given to an object replacing an original one. If the new object
     * keep the same authority, then all metadata are preserved. Otherwise (i.e. if a new authority
     * is given to the new object), then the old identifiers will be removed from the new object
     * metadata.
     *
     * @param object The original object.
     * @return The properties to be given to the object created as a substitute of {@code object}.
     */
    private Map<String, Object> getProperties(final IdentifiedObject object, String code) {
        final Citation authority = getAuthority();
        String identifier = getIdentifier(code);

        Map<String, Object> properties = new HashMap<>();
        properties.put(IdentifiedObject.NAME_KEY, identifier);
        properties.put(Identifier.AUTHORITY_KEY, authority);
        List<ReferenceIdentifier> aliases = new ArrayList<>(object.getIdentifiers());
        List<ReferenceIdentifier> identifiers = new ArrayList<>();
        aliases.add(0, new NamedIdentifier(authority, identifier));
        properties.put(
                IdentifiedObject.IDENTIFIERS_KEY,
                aliases.toArray(new ReferenceIdentifier[identifiers.size()]));
        properties.put(
                IdentifiedObject.ALIAS_KEY,
                aliases.toArray(new ReferenceIdentifier[aliases.size()]));

        return properties;
    }
}
