/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.crs;

import org.geotools.api.metadata.citation.Citation;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.referencing.factory.AbstractAuthorityFactory;
import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.referencing.factory.OrderedAxisAuthorityFactory;
import org.geotools.referencing.factory.OrderedAxisCRSAuthorityFactory;
import org.geotools.referencing.factory.epsg.ThreadedEpsgFactory;
import org.geotools.util.factory.Hints;

/**
 * A user authority factory using (<var>longitude</var>, <var>latitude</var>) axis order. This factory wraps a
 * {@link UserAuthorityWKTFactory} into an {@link OrderedAxisAuthorityFactory} when first needed.
 *
 * @see Hints#FORCE_LONGITUDE_FIRST_AXIS_ORDER
 */
public class UserAuthorityLongitudeFirstFactory extends DeferredAuthorityFactory implements CRSAuthorityFactory {
    private static final int RELATIVE_PRIORITY = +7;
    private final UserAuthorityWKTFactory backingStore;

    /*
     * Implementation note: in theory the DatumAuthorityFactory interface is useless here, since
     * "axis order" doesn't make any sense for them. However if we do not register this class for
     * the DatumAuthorityFactory as well, user will get a FactoryNotFoundException when asking for
     * a factory with the FORCE_LONGITUDE_FIRST_AXIS_ORDER hint set.
     */

    /**
     * Creates a factory from the specified set of hints.
     *
     * @param backingStore The lat/lon ordered factory behind this one.
     */
    public UserAuthorityLongitudeFirstFactory(UserAuthorityWKTFactory backingStore) {
        super(null, UserAuthorityWKTFactory.PRIORITY + RELATIVE_PRIORITY);
        // See comment in createBackingStore() method body.
        hints.put(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        hints.put(Hints.FORCE_STANDARD_AXIS_DIRECTIONS, false);
        hints.put(Hints.FORCE_STANDARD_AXIS_UNITS, false);

        this.backingStore = backingStore;
    }

    @Override
    public Citation getAuthority() {
        return backingStore.getAuthority();
    }

    /**
     * Returns the factory instance (usually {@link ThreadedEpsgFactory}) to be used as the backing store.
     *
     * @throws FactoryException If no suitable factory instance was found.
     */
    @Override
    protected AbstractAuthorityFactory createBackingStore() throws FactoryException {
        return new OrderedAxisCRSAuthorityFactory(backingStore, new Hints(hints), null);
    }

    @Override
    public synchronized CoordinateSystem createCoordinateSystem(String code) throws FactoryException {
        return super.createCoordinateSystem(code);
    }
}
