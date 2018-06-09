/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import ucar.ma2.DataType;

/**
 * Class used to identify the DataPacking to be applied to NetCDF values to be stored. DataPacking
 * on write is used to reduce the disk usage (As an instance, storing 64 bit Double data as 16 bit
 * Shorts). Data values are rescaled (packed) using a formula:
 *
 * <p>packed_value = nearestint((original_value - add_offset) / scale_factor)
 *
 * <p>add_offset and scale_factor attributes are added to the Variable when data packing occurs.
 * They are computed on top of the dataset's min and max.
 *
 * <p>add_offset = (max + min) / 2 scale_factor = (max - min) / (2^n - 2)
 *
 * <p>[where n is the number of bits of the output packed data type.] Supported packing types are
 * Byte, Short, Integer.
 *
 * <p>Data can be read back with the formula: unpacked_value = packed_value * scale_factor +
 * add_offset
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public enum DataPacking {
    NONE {
        @Override
        public DataType getDataType() {
            // No packing required
            return null;
        }

        @Override
        public DataPacker getDataPacker(DataStats stats) {
            // No packing required
            return null;
        }

        @Override
        public Integer getDenominator() {
            // No packing required
            return null;
        }

        @Override
        public Integer getReservedValue() {
            // No packing required
            return null;
        }
    },
    BYTE {
        @Override
        public DataType getDataType() {
            return DataType.BYTE;
        }

        @Override
        public Integer getDenominator() {
            return BYTE_DENOMINATOR;
        }

        @Override
        public Integer getReservedValue() {
            return ZERO;
        }

        @Override
        public DataPacker getDataPacker(DataStats stats) {
            double min = stats.getMin();
            double max = stats.getMax();
            double scale = (max - min) / getDenominator();
            double offset = (min - scale);
            return new DataPacker(offset, scale, getReservedValue());
        }
    },
    SHORT {
        @Override
        public DataType getDataType() {
            return DataType.SHORT;
        }

        @Override
        public Integer getDenominator() {
            return SHORT_DENOMINATOR;
        }

        @Override
        public Integer getReservedValue() {
            return SHORT_RESERVED;
        }
    },
    INT {
        @Override
        public DataType getDataType() {
            return DataType.INT;
        }

        @Override
        public Integer getDenominator() {
            return INT_DENOMINATOR;
        }

        @Override
        public Integer getReservedValue() {
            return INT_RESERVED;
        }
    },
    LONG {
        @Override
        public DataType getDataType() {
            return DataType.LONG;
        }

        @Override
        public Integer getDenominator() {
            return LONG_DENOMINATOR;
        }

        @Override
        public Integer getReservedValue() {
            return LONG_RESERVED;
        }
    };

    public static DataPacking getDefault() {
        return NONE;
    }

    private static final Integer ZERO = 0;

    /**
     * Constants to be used for dataPacking formula, depending on the dataPacking dataType.
     * scale_factor = (max - min) / (2^n - 2).
     *
     * <p>(2^n - 2) is a precomputed denominator.
     */
    // this one uses only half range in order to encode positive bytes, some clients
    // do not handle negative ones well
    private static final Integer BYTE_DENOMINATOR = ((int) Math.pow(2, 7)) - 2;

    private static final Integer SHORT_DENOMINATOR = ((int) Math.pow(2, 16)) - 2;

    private static final Integer INT_DENOMINATOR = ((int) Math.pow(2, 32)) - 2;

    private static final Integer LONG_DENOMINATOR = ((int) Math.pow(2, 64)) - 2;

    /**
     * Reserved special values dataType related. They will be used to remap a specific input value
     * which can't be represented in the output type. As an instance, a -1E-20 input fillValue can't
     * be represented as a short, so we need a reserved value to represent it. The reserved value
     * formula is: -(2^(n-1)) where n is the output dataType's number of bits.
     */
    private static final Integer SHORT_RESERVED = -((int) Math.pow(2, 15));

    private static final Integer INT_RESERVED = -((int) Math.pow(2, 31));

    private static final Integer LONG_RESERVED = -((int) Math.pow(2, 63));

    /** Return the denominator to be used in computing the scale_factor */
    public abstract Integer getDenominator();

    /** Return a reserved value (it can be used to represent fillValue) */
    public abstract Integer getReservedValue();

    /** Return the Variable's {@link DataType} for the specific packing */
    public abstract DataType getDataType();

    /** Get the default DataPacker */
    public DataPacker getDataPacker(DataStats stats) {
        double min = stats.getMin();
        double max = stats.getMax();
        double offset = (min + max) / 2;
        double scale = (max - min) / getDenominator();
        return new DataPacker(offset, scale, getReservedValue());
    }

    /** A {@link DataPacker} instance used to pack data. */
    public static class DataPacker {
        private double offset;
        private double scale;
        private int reservedValue;

        public DataPacker(double offset, double scale, int reservedValue) {
            this.offset = offset;
            this.scale = scale;
            this.reservedValue = reservedValue;
        }

        public double getScale() {
            return scale;
        }

        public double getOffset() {
            return offset;
        }

        /** Return the reservedValue. */
        public int getReservedValue() {
            return reservedValue;
        }

        /**
         * Pack the sample using the dataPacking formula:
         *
         * <p>packed_value = nearestint((original_value - add_offset) / scale_factor)
         *
         * @param sample the sample to be packed
         * @return the packed value
         */
        public int pack(double sample) {
            if (Double.isNaN(sample)) {
                return reservedValue;
            }
            double unrounded = ((sample - offset) / scale);
            return (int) (unrounded + 0.5);
        }
    }

    /**
     * Stores and update data stats which will be used to get a specific {@link DataPacker}
     * instance. Indeed, computed offset and scale depend on the data statistics (min and max)
     */
    public static class DataStats {
        public DataStats() {
            min = Double.POSITIVE_INFINITY;
            max = Double.NEGATIVE_INFINITY;
        }

        public double getMin() {
            return min;
        }

        public void setMin(double min) {
            this.min = min;
        }

        public double getMax() {
            return max;
        }

        public void setMax(double max) {
            this.max = max;
        }

        private double min;

        private double max;

        public void update(double updatingMin, double updatingMax) {
            if (!Double.isNaN(updatingMin)) {
                min = min > updatingMin ? updatingMin : min;
            }
            if (!Double.isNaN(updatingMax)) {
                max = max < updatingMax ? updatingMax : max;
            }
        }
    }

    public static String ADD_OFFSET = "add_offset";

    public static String SCALE_FACTOR = "scale_factor";
}
