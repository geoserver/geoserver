/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geotools.api.geometry.Bounds;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.cs.CoordinateSystem;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.referencing.cs.DefaultCoordinateSystemAxis;
import org.geotools.util.Utilities;

/**
 * Retain a unique mapping between {@link Envelope} axes and their names.
 *
 * @author Simone Giannecchini, GeoSolutions TODO caching depending on CRS? TODO handle composite
 *     CRS
 */
public class EnvelopeAxesLabelsMapper {

    public List<String> getAxesNames(Bounds envelope, boolean swapAxes) {
        Utilities.ensureNonNull("envelope", envelope);
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        if (crs == null) {
            throw new IllegalStateException(
                    "Unable to determine axes names as envelope does not include srs");
        }

        // handle axes switch for geographic crs
        final boolean axesSwitch = crs instanceof GeographicCRS && swapAxes;

        // additional fields for CRS
        final CoordinateSystem cs = crs.getCoordinateSystem();

        final int dimension = cs.getDimension();
        // loop through dimensions
        final ArrayList<String> retValue = new ArrayList<>();
        if (!axesSwitch) {
            for (int i = 0; i < dimension; i++) {
                CoordinateSystemAxis axis = cs.getAxis(i);
                retValue.add(getAxisLabel(axis)); // use axis abbreviation
            }
        } else {
            int northing = -1, easting = -1;
            for (int i = 0; i < dimension; i++) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (Math.abs(
                                DefaultCoordinateSystemAxis.getAngle(
                                        axis.getDirection(),
                                        DefaultCoordinateSystemAxis.LONGITUDE.getDirection()))
                        < 1E-6) {
                    easting = i;
                } else if (Math.abs(
                                DefaultCoordinateSystemAxis.getAngle(
                                        axis.getDirection(),
                                        DefaultCoordinateSystemAxis.LATITUDE.getDirection()))
                        < 1E-6) {
                    northing = i;
                }
                retValue.add(getAxisLabel(axis)); // use axis abbreviation
            }

            // now switch them
            retValue.add(northing, retValue.remove(easting));
        }

        return retValue;
    }

    public String getAxisLabel(CoordinateSystemAxis axis) {
        // some default axis have weird abbreviations (greek letters), handle them separately
        String label = axis.getAbbreviation();
        // in EPSG 9.6 axis label can be also be Long and Lon
        if (label.equals(DefaultCoordinateSystemAxis.LONGITUDE.getAbbreviation())
                || label.equals("Lon")
                || label.equals("Long")) {
            return "Long";
        } else if (label.equals(DefaultCoordinateSystemAxis.LATITUDE.getAbbreviation())
                || label.equals("Lat")) {
            return "Lat";
        } else {

            return label;
        }
    }

    public int getAxisIndex(final Bounds envelope, final String axisAbbreviation) {
        final int[] val = getAxesIndexes(envelope, Arrays.asList(axisAbbreviation));
        return (val == null ? -1 : val[0]);
    }

    public int[] getAxesIndexes(final Bounds envelope, final List<String> axesAbbreviations) {
        Utilities.ensureNonNull("envelope", envelope);
        Utilities.ensureNonNull("dimensionNames", axesAbbreviations);

        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final CoordinateSystem cs = crs.getCoordinateSystem();
        final int crsDimension = cs.getDimension();

        final int dimension = axesAbbreviations.size();
        final int[] retValue = new int[dimension];
        for (int i = 0; i < dimension; i++) {

            // abbreviation we are looking for
            final String axisAbbreviation = axesAbbreviations.get(i);
            int pos = -1;
            // search for this dimension in cs axes
            for (int j = 0; j < crsDimension; j++) {
                // check exact abbreviation
                CoordinateSystemAxis axis = cs.getAxis(j);
                if (getAxisLabel(axis).equals(axisAbbreviation)) {
                    pos = j; // FOUND!!!
                    break;
                }
            }

            // found?
            if (pos < 0) {
                // NOT FOUND
                return null;
            }

            // yes!!!
            retValue[i] = pos;
        }
        return retValue;
    }
}
