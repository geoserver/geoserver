/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import org.geotools.geometry.AbstractEnvelope;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.cs.DefaultCoordinateSystemAxis;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;

/**
 * A custom {@link Envelope} that allows to set a min value of longitude higher than the max one in
 * selected methods to deal with the dateline crossing case
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCSEnvelope extends AbstractEnvelope {

    private static final int LONGIDUTE_NOT_FOUND = -1;

    CoordinateReferenceSystem crs;

    double[] ordinates;

    int dimensions;

    int longitudeDimension = LONGIDUTE_NOT_FOUND;

    /** Creates an empty envelope based on the given coordinate reference system */
    public WCSEnvelope(CoordinateReferenceSystem crs) {
        if (crs == null) {
            throw new IllegalArgumentException(
                    "WCSEnvelope coordinate reference system cannot be null");
        }
        this.crs = crs;

        // initialize the longitude dimension, if we have a longitude, that is
        CoordinateSystem cs = crs.getCoordinateSystem();
        this.dimensions = cs.getDimension();
        this.ordinates = new double[dimensions * 2];
        for (int i = 0; i < dimensions; i++) {
            CoordinateSystemAxis axis = cs.getAxis(i);
            if (CRS.equalsIgnoreMetadata(axis, DefaultCoordinateSystemAxis.LONGITUDE)) {
                longitudeDimension = i;
                break;
            }
        }
    }

    /** Copies an existing envelope */
    public WCSEnvelope(Envelope other) {
        this(other.getCoordinateReferenceSystem());
        for (int d = 0; d < dimensions; d++) {
            setRange(d, other.getMinimum(d), other.getMaximum(d));
        }
    }

    /**
     * Sets the range for the given dimension. If the dimension is the longitude, it is allowed to
     * set a minimum greater than the maximum, this envelope will be assumed to span the dateline
     */
    public void setRange(int dimension, double minimum, double maximum)
            throws IndexOutOfBoundsException {
        if (minimum > maximum
                && (longitudeDimension != LONGIDUTE_NOT_FOUND && dimension != longitudeDimension)) {
            // Make an empty envelope (min == max)
            // while keeping it legal (min <= max).
            minimum = maximum = 0.5 * (minimum + maximum);
            ordinates[dimension + ordinates.length / 2] = maximum;
            ordinates[dimension] = minimum;
        } else if (dimension >= 0 && dimension < ordinates.length / 2) {
            ordinates[dimension + ordinates.length / 2] = maximum;
            ordinates[dimension] = minimum;
        } else {
            throw indexOutOfBounds(dimension);
        }
    }

    /**
     * Returns true if the envelope has a empty span on at least one dimension. A span is empty if
     * its zero or negative, but in case a dimension is the longitude, a negative span will be
     * treated as a dateline crossing, and thus treated as non empty
     */
    public boolean isEmpty() {
        for (int i = 0; i < dimensions; i++) {
            double span = getSpan(i);
            if (span == 0) {
                return true;
            } else if (span < 0 && i != longitudeDimension) {
                return true;
            }
        }

        return false;
    }

    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem() {
        return crs;
    }

    @Override
    public int getDimension() {
        return dimensions;
    }

    @Override
    public double getMinimum(int dimension) throws IndexOutOfBoundsException {
        if (dimension < this.dimensions) {
            return ordinates[dimension];
        } else {
            throw indexOutOfBounds(dimension);
        }
    }

    private IndexOutOfBoundsException indexOutOfBounds(int dimension) {
        return new IndexOutOfBoundsException("Invalid dimension " + dimension);
    }

    @Override
    public double getMaximum(int dimension) throws IndexOutOfBoundsException {
        if (dimension < this.dimensions) {
            return ordinates[dimension + ordinates.length / 2];
        } else {
            throw indexOutOfBounds(dimension);
        }
    }

    @Override
    public double getMedian(int dimension) throws IndexOutOfBoundsException {
        if (dimension < ordinates.length / 2) {
            return 0.5 * (ordinates[dimension] + ordinates[dimension + ordinates.length / 2]);
        } else {
            throw indexOutOfBounds(dimension);
        }
    }

    @Override
    public double getSpan(int dimension) throws IndexOutOfBoundsException {
        if (dimension < ordinates.length / 2) {
            return ordinates[dimension + ordinates.length / 2] - ordinates[dimension];
        } else {
            throw indexOutOfBounds(dimension);
        }
    }

    /**
     * Returns a list of envelopes that avoid the dateline crossing "odd" representation, that is,
     * in that case two envelopes will be returned covering the portion before and after the
     * dateline
     */
    public GeneralEnvelope[] getNormalizedEnvelopes() {
        if (!isCrossingDateline()) {
            return new GeneralEnvelope[] {new GeneralEnvelope(this)};
        } else {
            GeneralEnvelope e1 = new GeneralEnvelope(crs);
            GeneralEnvelope e2 = new GeneralEnvelope(crs);
            for (int i = 0; i < dimensions; i++) {
                if (i == longitudeDimension) {
                    e1.setRange(i, getMinimum(i), 180);
                    if (getSpan(longitudeDimension) < 0) {
                        e2.setRange(i, -180, getMaximum(i));
                    } else {
                        e2.setRange(i, -180, getMaximum(i) - 360);
                    }
                } else {
                    e1.setRange(i, getMinimum(i), getMaximum(i));
                    e2.setRange(i, getMinimum(i), getMaximum(i));
                }
            }

            return new GeneralEnvelope[] {e1, e2};
        }
    }

    /**
     * Checks if this envelope intersects the provided one, taking into account the case of dateline
     * crossing.
     */
    public void intersect(GeneralEnvelope other) {
        assert other.getDimension() == dimensions : other;
        assert CRS.equalsIgnoreMetadata(crs, other.getCoordinateReferenceSystem()) : other;

        if (isCrossingDateline()) {
            GeneralEnvelope[] normalizedEnvelopes = getNormalizedEnvelopes();
            for (GeneralEnvelope ge : normalizedEnvelopes) {
                ge.intersect(other);
            }
            for (int i = 0; i < dimensions; i++) {
                if (i == longitudeDimension) {
                    if (normalizedEnvelopes[0].getSpan(i) == 0) {
                        ordinates[i] = normalizedEnvelopes[1].getMinimum(i);
                        ordinates[i + dimensions] = normalizedEnvelopes[1].getMaximum(i);
                    } else if (normalizedEnvelopes[1].getSpan(i) == 0) {
                        ordinates[i] = normalizedEnvelopes[0].getMinimum(i);
                        ordinates[i + dimensions] = normalizedEnvelopes[0].getMaximum(i);
                    } else {
                        ordinates[i] = normalizedEnvelopes[0].getMinimum(i);
                        ordinates[i + dimensions] = normalizedEnvelopes[1].getMaximum(i);
                    }
                } else {
                    ordinates[i] = normalizedEnvelopes[0].getMinimum(i);
                    ordinates[i + dimensions] = normalizedEnvelopes[0].getMaximum(i);
                }
            }
        } else {
            for (int i = 0; i < dimensions; i++) {
                double min = Math.max(ordinates[i], other.getMinimum(i));
                double max = Math.min(ordinates[i + dimensions], other.getMaximum(i));
                if (min > max) {
                    // Make an empty envelope (min==max)
                    // while keeping it legal (min<=max).
                    min = max = 0.5 * (min + max);
                }
                ordinates[i] = min;
                ordinates[i + dimensions] = max;
            }
        }
    }

    /** Returns true if this envelope is crossing the dateline */
    public boolean isCrossingDateline() {
        // TODO: handle the case the envelope is in a projected system and still
        // crossing the dateline (e.g. polar, or mercator centered around the dateline)
        return longitudeDimension != LONGIDUTE_NOT_FOUND
                && (getSpan(longitudeDimension) < 0
                        || (getMinimum(longitudeDimension) < 180
                                && getMaximum(longitudeDimension) > 180));
    }

    /** Returns true if the specified dimension index is matching the longitude axis */
    public boolean isLongitude(int dimension) {
        return longitudeDimension == dimension;
    }
}
