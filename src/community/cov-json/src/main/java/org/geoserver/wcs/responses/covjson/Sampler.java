/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses.covjson;

import java.awt.image.DataBuffer;
import javax.media.jai.iterator.RandomIter;

/**
 * Class used to return samples from an underlying randomIter, returning proper type of number
 * (integer, float, double), calling the proper getSampleXXX method.
 */
abstract class Sampler {

    protected RandomIter randomIter;

    Sampler(RandomIter randomIter) {
        this.randomIter = randomIter;
    }

    void done() {
        randomIter.done();
    }

    abstract Number getSample(int x, int y, int b);

    public static Sampler create(int dataType, RandomIter iter) {
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                return new IntSampler(iter);
            case DataBuffer.TYPE_FLOAT:
                return new FloatSampler(iter);
            case DataBuffer.TYPE_DOUBLE:
                return new DoubleSampler(iter);
            default:
                throw new IllegalArgumentException("dataType not supported: " + dataType);
        }
    }

    static class FloatSampler extends Sampler {
        public FloatSampler(RandomIter iter) {
            super(iter);
        }

        @Override
        Number getSample(int x, int y, int b) {
            return randomIter.getSampleFloat(x, y, b);
        }
    };

    static class IntSampler extends Sampler {
        public IntSampler(RandomIter iter) {
            super(iter);
        }

        @Override
        Number getSample(int x, int y, int b) {
            return randomIter.getSample(x, y, b);
        }
    };

    static class DoubleSampler extends Sampler {
        public DoubleSampler(RandomIter iter) {
            super(iter);
        }

        @Override
        Number getSample(int x, int y, int b) {
            return randomIter.getSampleDouble(x, y, b);
        }
    };
}
