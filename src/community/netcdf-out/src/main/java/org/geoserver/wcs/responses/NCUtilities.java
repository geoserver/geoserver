/*
 *  GeoBatch - Open Source geospatial batch processing system
 *  http://geobatch.geo-solutions.it/
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.wcs.responses;

import java.awt.image.DataBuffer;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;

/**
 * A NetCDF Utilities class 
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 *
 */
public class NCUtilities {

    public final static String LAT = "lat";

    public final static String LON = "lon";

    public final static String LATITUDE = "latitude";

    public final static String LONGITUDE = "longitude";

    public final static String UNITS = "units";

    public final static String LONGNAME = "long_name";

    public final static String STANDARD_NAME = "standard_name";

    public final static String DESCRIPTION = "description";

    public final static String BOUNDS = "bounds";

    public final static String BOUNDS_SUFFIX = "_bnds";

    public final static String FILLVALUE = "_FillValue";

    public final static String MISSING_VALUE = "missing_value";

    public final static String LON_UNITS = "degrees_east";

    public final static String LAT_UNITS = "degrees_north";

    public final static String NO_COORDS = "NoCoords";

    public final static String TIME_ORIGIN = "seconds since 1970-01-01 00:00:00 UTC";

    public final static long START_TIME;

    public final static String BOUNDARY_DIMENSION = "nv";

    public final static TimeZone UTC;

    static {
        // Setting the LINUX Epoch as start time
        final GregorianCalendar calendar = new GregorianCalendar(1970, 00, 01, 00, 00, 00);
        UTC = TimeZone.getTimeZone("UTC");
        calendar.setTimeZone(UTC);
        START_TIME = calendar.getTimeInMillis();
    }

    final static Set<String> EXCLUDED_ATTRIBUTES = new HashSet<String>();

    static {
        EXCLUDED_ATTRIBUTES.add(UNITS);
        EXCLUDED_ATTRIBUTES.add(LONGNAME);
        EXCLUDED_ATTRIBUTES.add(DESCRIPTION);
        EXCLUDED_ATTRIBUTES.add(STANDARD_NAME);
    }

    /**
     * Transcode a NetCDF data type into a java2D  DataBuffer type.
     * 
     * @param type the {@link DataType} to transcode.
     * @param unsigned if the original data is unsigned or not
     * @return an int representing the correct DataBuffer type.
     */
    public static int transcodeNetCDFDataType(final DataType type, final boolean unsigned) {
        if (DataType.BOOLEAN.equals(type) || DataType.BYTE.equals(type)) {
            return DataBuffer.TYPE_BYTE;
        }
        if (DataType.CHAR.equals(type)) {
            return DataBuffer.TYPE_USHORT;
        }
        if (DataType.SHORT.equals(type)) {
            return unsigned ? DataBuffer.TYPE_USHORT: DataBuffer.TYPE_SHORT;
        }
        if (DataType.INT.equals(type)) {
            return DataBuffer.TYPE_INT;
        }
        if (DataType.FLOAT.equals(type)) {
            return DataBuffer.TYPE_FLOAT;
        }
        if (DataType.LONG.equals(type) || DataType.DOUBLE.equals(type)) {
            return DataBuffer.TYPE_DOUBLE;
        }
        return DataBuffer.TYPE_UNDEFINED;
        }

    /**
     * Get an Array of proper size and type.
     * 
     * @param dimensions the dimensions
     * @param varDataType the DataType of the required array 
     * @return
     */
    public static Array getArray(int[] dimensions, DataType varDataType) {
        if (dimensions == null)
            throw new IllegalArgumentException("Illegal dimensions");
        final int nDims = dimensions.length;
        switch (nDims) {
        case 6:
            // 6D Arrays
            if (varDataType == DataType.FLOAT) {
                return new ArrayFloat.D6(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4], dimensions[5]);
            } else if (varDataType == DataType.DOUBLE) {
                return new ArrayDouble.D6(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4], dimensions[5]);
            } else if (varDataType == DataType.BYTE) {
                return new ArrayByte.D6(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4], dimensions[5]);
            } else if (varDataType == DataType.SHORT) {
                return new ArrayShort.D6(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4], dimensions[5]);
            } else if (varDataType == DataType.INT) {
                return new ArrayInt.D6(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4], dimensions[5]);
            } else
                throw new IllegalArgumentException("unsupported Datatype");
        case 5:
            // 5D Arrays
            if (varDataType == DataType.FLOAT) {
                return new ArrayFloat.D5(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4]);
            } else if (varDataType == DataType.DOUBLE) {
                return new ArrayDouble.D5(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4]);
            } else if (varDataType == DataType.BYTE) {
                return new ArrayByte.D5(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4]);
            } else if (varDataType == DataType.SHORT) {
                return new ArrayShort.D5(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4]);
            } else if (varDataType == DataType.INT) {
                return new ArrayInt.D5(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3], dimensions[4]);
            } else
                throw new IllegalArgumentException("unsupported Datatype");
        case 4:
            // 4D Arrays
            if (varDataType == DataType.FLOAT) {
                return new ArrayFloat.D4(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3]);
            } else if (varDataType == DataType.DOUBLE) {
                return new ArrayDouble.D4(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3]);
            } else if (varDataType == DataType.BYTE) {
                return new ArrayByte.D4(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3]);
            } else if (varDataType == DataType.SHORT) {
                return new ArrayShort.D4(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3]);
            } else if (varDataType == DataType.INT) {
                return new ArrayInt.D4(dimensions[0], dimensions[1],
                        dimensions[2], dimensions[3]);
            } else
                throw new IllegalArgumentException("unsupported Datatype");
        case 3:
            // 3D Arrays
            if (varDataType == DataType.FLOAT) {
                return new ArrayFloat.D3(dimensions[0], dimensions[1],
                        dimensions[2]);
            } else if (varDataType == DataType.DOUBLE) {
                return new ArrayDouble.D3(dimensions[0], dimensions[1],
                        dimensions[2]);
            } else if (varDataType == DataType.BYTE) {
                return new ArrayByte.D3(dimensions[0], dimensions[1],
                        dimensions[2]);
            } else if (varDataType == DataType.SHORT) {
                return new ArrayShort.D3(dimensions[0], dimensions[1],
                        dimensions[2]);
            } else if (varDataType == DataType.INT) {
                return new ArrayInt.D3(dimensions[0], dimensions[1],
                        dimensions[2]);
            } else
                throw new IllegalArgumentException("unsupported Datatype");
        case 2:
            // 2D Arrays
            if (varDataType == DataType.FLOAT) {
                return new ArrayFloat.D2(dimensions[0], dimensions[1]);
            } else if (varDataType == DataType.DOUBLE) {
                return new ArrayDouble.D2(dimensions[0], dimensions[1]);
            } else if (varDataType == DataType.BYTE) {
                return new ArrayByte.D2(dimensions[0], dimensions[1]);
            } else if (varDataType == DataType.SHORT) {
                return new ArrayShort.D2(dimensions[0], dimensions[1]);
            } else if (varDataType == DataType.INT) {
                return new ArrayInt.D2(dimensions[0], dimensions[1]);
            } else
                throw new IllegalArgumentException("unsupported Datatype");
        case 1:
            // 1D Arrays
            if (varDataType == DataType.FLOAT) {
                return new ArrayFloat.D1(dimensions[0]);
            } else if (varDataType == DataType.DOUBLE) {
                return new ArrayDouble.D1(dimensions[0]);
            } else if (varDataType == DataType.BYTE) {
                return new ArrayByte.D1(dimensions[0]);
            } else if (varDataType == DataType.SHORT) {
                return new ArrayShort.D1(dimensions[0]);
            } else if (varDataType == DataType.INT) {
                return new ArrayInt.D1(dimensions[0]);
            } else
                throw new IllegalArgumentException("unsupported Datatype");
        }
        throw new IllegalArgumentException("Unable to create a proper array unsupported Datatype");
    }

    static boolean isLatLon(String bandName) {
        return bandName.equalsIgnoreCase(LON) || bandName.equalsIgnoreCase(LAT);
    }

    /** 
     * Return the propery NetCDF dataType for the input datatype class
     * 
     * @param classDataType
     * @return
     */
    public static DataType getNetCDFDataType(String classDataType) {
        if (isATime(classDataType)) {
            return DataType.DOUBLE;
        } else if (classDataType.endsWith("Integer")) {
            return DataType.INT;
        } else if (classDataType.endsWith("Double")) {
            return DataType.DOUBLE;
        } else if (classDataType.endsWith("String")) {
            return DataType.STRING;
        }
        return DataType.STRING;
    }

    /**
     * Transcode a DataBuffer type into a NetCDF DataType .
     * 
     * @param type the beam {@link ProductData} type to transcode.
     * @return an NetCDF DataType type.
     */
    public static DataType transcodeImageDataType(final int dataType) {
        switch (dataType) {
        case DataBuffer.TYPE_BYTE:
            return DataType.BYTE;
        case DataBuffer.TYPE_SHORT:
            return DataType.SHORT;
        case DataBuffer.TYPE_INT:
            return DataType.INT;
        case DataBuffer.TYPE_DOUBLE:
            return DataType.DOUBLE;
        case DataBuffer.TYPE_FLOAT:
            return DataType.FLOAT;
        case DataBuffer.TYPE_UNDEFINED:
        default:
            throw new IllegalArgumentException("Invalid input data type:" + dataType);
        }
    }

    /** 
     * Return true in case that dataType refers to something which need to be handled 
     * as a Time (TimeStamp, Date)
     * @param classDataType
     * @return
     */
    public final static boolean isATime(String classDataType) {
        return (classDataType.endsWith("Timestamp") || classDataType.endsWith("Date"));
    }

}
