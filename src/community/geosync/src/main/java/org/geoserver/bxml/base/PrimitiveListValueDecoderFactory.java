package org.geoserver.bxml.base;

import org.gvsig.bxml.stream.EventType;

public class PrimitiveListValueDecoderFactory {

    public static PrimitiveListValueDecoder build(EventType type) {
        switch (type) {
        case VALUE_BOOL:
            return new PrimitiveListValueDecoder<boolean[]>(boolean[].class);
        case VALUE_DOUBLE:
            return new PrimitiveListValueDecoder<double[]>(double[].class);
        case VALUE_FLOAT:
            return new PrimitiveListValueDecoder<float[]>(float[].class);
        case VALUE_BYTE:
            return new PrimitiveListValueDecoder<byte[]>(byte[].class);
        case VALUE_INT:
            return new PrimitiveListValueDecoder<int[]>(int[].class);
        case VALUE_LONG:
            return new PrimitiveListValueDecoder<long[]>(long[].class);
        default:
            throw new IllegalArgumentException("Bad primitive array class: " + type);
        }
    }
}
