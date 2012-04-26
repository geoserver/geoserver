package org.geoserver.bxml.base.wrapper;

public class ArrayWrapperFactory {

    @SuppressWarnings("rawtypes")
    public static ArrayWrapper buildArrayFactory(Class type) {
        if (type.equals(boolean[].class)) {
            return new BooleanArrayWrapper();
        }
        if (type.equals(byte[].class)) {
            return new ByteArrayWrapper();
        }
        if (type.equals(int[].class)) {
            return new IntArrayWrapper();
        }
        if (type.equals(long[].class)) {
            return new LongArrayWrapper();
        }
        if (type.equals(float[].class)) {
            return new FloatArrayWrapper();
        }
        if (type.equals(double[].class)) {
            return new DoubleArrayWrapper();
        }
        throw new IllegalArgumentException("Bad primitive array class: " + type);
    }

}
