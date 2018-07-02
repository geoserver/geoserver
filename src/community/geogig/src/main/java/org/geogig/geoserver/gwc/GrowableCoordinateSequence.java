/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.gwc;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.primitives.Floats;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Envelope;

/**
 * A {@link CoordinateSequence} that grows as needed when coordinates are {@link #add(double,
 * double) added}, and stores all coordinates in a single {@code float[]}
 *
 * <p>A subset of this coordinate sequence can be obtained as a view sharing internal state using
 * {@link #subSequence(int, int)}
 */
class GrowableCoordinateSequence implements CoordinateSequence {

    private float[] ordinates;

    private int size;

    public GrowableCoordinateSequence() {
        this(new float[100], 0);
    }

    private GrowableCoordinateSequence(float[] ordinates, int size) {
        this.ordinates = ordinates;
        this.size = size;
    }

    /**
     * Adds the {@code x,y} coordinate to this coordinate sequence, increasing the internal buffer
     * as necessary to acommodate for the expanded size if needed.
     */
    public void add(double x, double y) {
        final int index = this.size;
        this.size++;
        ensureCapacity(size);
        setOrdinate(index, 0, x);
        setOrdinate(index, 1, y);
    }

    /**
     * @return a "view" of this coordinate sequence limited by {@code fromIndex} and {@code toIndex}
     */
    public CoordinateSequence subSequence(int fromIndex, int toIndex) {
        return new SubSequence(this, fromIndex, toIndex);
    }

    private void ensureCapacity(int size) {
        ordinates = Floats.ensureCapacity(ordinates, 2 * size, 100);
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public Coordinate getCoordinate(int i) {
        return new Coordinate(getX(i), getY(i));
    }

    @Override
    public Coordinate getCoordinateCopy(int i) {
        return getCoordinate(i);
    }

    @Override
    public void getCoordinate(int index, Coordinate coord) {
        coord.x = getX(index);
        coord.y = getY(index);
        coord.z = 0D;
    }

    @Override
    public double getX(int index) {
        return getOrdinate(index, 0);
    }

    @Override
    public double getY(int index) {
        return getOrdinate(index, 1);
    }

    @Override
    public double getOrdinate(int index, int ordinateIndex) {
        return ordinates[index * getDimension() + ordinateIndex];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void setOrdinate(int index, int ordinateIndex, double value) {
        if (index >= size) {
            throw new IndexOutOfBoundsException();
        }
        ordinates[index * getDimension() + ordinateIndex] = (float) value;
    }

    @Override
    public Coordinate[] toCoordinateArray() {
        Coordinate[] coords = new Coordinate[size()];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = getCoordinate(i);
        }
        return coords;
    }

    @Override
    public Envelope expandEnvelope(Envelope env) {
        final int size = size();
        final int dimension = getDimension();
        for (int i = 0; i < size; i += dimension) {
            env.expandToInclude(getX(i), getY(i));
        }
        return env;
    }

    @Override
    public GrowableCoordinateSequence clone() {
        return new GrowableCoordinateSequence(ordinates.clone(), size);
    }

    private static class SubSequence implements CoordinateSequence {

        private CoordinateSequence orig;

        private int toIndex;

        private int fromIndex;

        public SubSequence(CoordinateSequence orig, int fromIndex, int toIndex) {
            checkArgument(fromIndex > -1);
            checkArgument(toIndex >= fromIndex);
            checkArgument(toIndex < orig.size());
            this.orig = orig;
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }

        @Override
        public int getDimension() {
            return orig.getDimension();
        }

        @Override
        public Coordinate getCoordinate(int i) {
            return orig.getCoordinate(fromIndex + i);
        }

        @Override
        public Coordinate getCoordinateCopy(int i) {
            return orig.getCoordinateCopy(fromIndex + i);
        }

        @Override
        public void getCoordinate(int index, Coordinate coord) {
            orig.getCoordinate(fromIndex + index, coord);
        }

        @Override
        public double getX(int index) {
            return orig.getX(fromIndex + index);
        }

        @Override
        public double getY(int index) {
            return orig.getY(fromIndex + index);
        }

        @Override
        public double getOrdinate(int index, int ordinateIndex) {
            return orig.getOrdinate(fromIndex + index, ordinateIndex);
        }

        @Override
        public int size() {
            return 1 + (toIndex - fromIndex);
        }

        @Override
        public void setOrdinate(int index, int ordinateIndex, double value) {
            orig.setOrdinate(fromIndex + index, ordinateIndex, value);
        }

        @Override
        public Coordinate[] toCoordinateArray() {
            final int size = size();
            Coordinate[] coords = new Coordinate[size];
            for (int i = 0; i < size; i++) {
                coords[i] = getCoordinate(i);
            }
            return coords;
        }

        @Override
        public Envelope expandEnvelope(Envelope env) {
            final int size = size();
            for (int i = 0; i < size; i++) {
                env.expandToInclude(getOrdinate(i, 0), getOrdinate(i, 1));
            }
            return env;
        }

        @Override
        public Object clone() {
            return new SubSequence(orig, fromIndex, toIndex);
        }

        @Override
        public CoordinateSequence copy() {
            return new SubSequence(orig.copy(), fromIndex, toIndex);
        }
    }

    @Override
    public CoordinateSequence copy() {
        return new GrowableCoordinateSequence(ordinates.clone(), size());
    }
}
