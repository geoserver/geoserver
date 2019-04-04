/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import com.sun.media.jai.util.DataBufferUtils;
import java.awt.Point;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.TileFactory;
import javax.media.jai.TileRecycler;
import org.apache.commons.beanutils.PropertyUtils;
import org.geotools.util.logging.Logging;

/**
 * A thread safe recycling tile factory that using Java 5 Concurrent data structures
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ConcurrentTileFactory implements TileFactory, TileRecycler {

    /** Cache of the tile recycled arrays */
    private volatile ArrayCache recycledArrays = new ArrayCache();

    /**
     * A concurrent multimap geared towards tile data array caching
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class ArrayCache
            extends ConcurrentHashMap<Long, ConcurrentLinkedQueue<SoftReference<?>>> {
        private static final long serialVersionUID = -6905685668738379653L;

        static final Logger LOGGER = Logging.getLogger(ConcurrentTileFactory.class);

        /** Retrieve an array of the specified type and length. */
        Object getRecycledArray(int arrayType, long numBanks, long arrayLength) {
            Long key = getKey(arrayType, numBanks, arrayLength);

            ConcurrentLinkedQueue<SoftReference<?>> arrays = get(key);

            if (arrays != null) {
                SoftReference<?> arrayRef;
                while ((arrayRef = arrays.poll()) != null) {
                    Object array = arrayRef.get();
                    if (array != null) {
                        if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.log(
                                    Level.FINER,
                                    "Recycling tile hit on type:{1}, banks: {2}, arrayLength: {3}",
                                    new Object[] {arrayType, numBanks, arrayLength});
                        }
                        return array;
                    }
                }
            }

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(
                        Level.FINER,
                        "Recycling tile miss on type:{1}, banks: {2}, arrayLength: {3}",
                        new Object[] {arrayType, numBanks, arrayLength});
            }
            return null;
        }

        private Long getKey(int arrayType, long numBanks, long arrayLength) {
            return Long.valueOf((((long) arrayType << 56) | numBanks << 32 | arrayLength));
        }

        public void recycleTile(Raster tile) {
            DataBuffer db = tile.getDataBuffer();

            Long key = getKey(db.getDataType(), db.getNumBanks(), db.getSize());
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(
                        Level.FINER,
                        "Recycling tile hit on type:{1}, banks: {2}, arrayLength: {3}",
                        new Object[] {db.getDataType(), db.getNumBanks(), db.getSize()});
            }

            ConcurrentLinkedQueue<SoftReference<?>> arrays = get(key);
            if (arrays == null) {
                arrays = new ConcurrentLinkedQueue<SoftReference<?>>();
                arrays.add(getBankReference(db));
                put(key, arrays);
                return;
            } else {
                arrays.add(getBankReference(db));
            }
        }

        /** Returns a <code>SoftReference</code> to the actual data stored into the DataBuffer */
        private static SoftReference<?> getBankReference(DataBuffer db) {
            try {
                Object array = PropertyUtils.getProperty(db, "bankData");
                return new SoftReference<Object>(array);
            } catch (Exception e) {
                throw new UnsupportedOperationException("Unknown data buffer type " + db);
            }
        }
    }

    /** Constructs a <code>RecyclingTileFactory</code>. */
    public ConcurrentTileFactory() {}

    /** Returns <code>true</code>. */
    public boolean canReclaimMemory() {
        return true;
    }

    /** Returns <code>true</code>. */
    public boolean isMemoryCache() {
        return true;
    }

    /** Always returns -1, does not do used memory accounting */
    public long getMemoryUsed() {
        return -1;
    }

    /** Clean up the cache */
    public void flush() {
        recycledArrays.clear();
    }

    /** Builds a new tile, eventually recycling the data array backing it */
    public WritableRaster createTile(SampleModel sampleModel, Point location) {
        // sanity checks
        if (sampleModel == null) {
            throw new NullPointerException("sampleModel cannot be null");
        }
        if (location == null) {
            location = new Point(0, 0);
        }

        DataBuffer db = null;

        // get the three elements making the key into the recycled array map
        int type = sampleModel.getTransferType();
        long numBanks = 0;
        long size = 0;
        if (sampleModel instanceof ComponentSampleModel) {
            ComponentSampleModel csm = (ComponentSampleModel) sampleModel;
            numBanks = getNumBanksCSM(csm);
            size = getBufferSizeCSM(csm);
        } else if (sampleModel instanceof MultiPixelPackedSampleModel) {
            MultiPixelPackedSampleModel mppsm = (MultiPixelPackedSampleModel) sampleModel;
            numBanks = 1;
            int dataTypeSize = DataBuffer.getDataTypeSize(type);
            size =
                    mppsm.getScanlineStride() * mppsm.getHeight()
                            + (mppsm.getDataBitOffset() + dataTypeSize - 1) / dataTypeSize;
        } else if (sampleModel instanceof SinglePixelPackedSampleModel) {
            SinglePixelPackedSampleModel sppsm = (SinglePixelPackedSampleModel) sampleModel;
            numBanks = 1;
            size = sppsm.getScanlineStride() * (sppsm.getHeight() - 1) + sppsm.getWidth();
        }

        if (size > 0) {
            // try to build a new data buffer starting from
            Object array = recycledArrays.getRecycledArray(type, numBanks, size);
            if (array != null) {
                switch (type) {
                    case DataBuffer.TYPE_BYTE:
                        {
                            byte[][] bankData = (byte[][]) array;
                            for (int i = 0; i < numBanks; i++) {
                                Arrays.fill(bankData[i], (byte) 0);
                            }
                            db = new DataBufferByte(bankData, (int) size);
                        }
                        break;
                    case DataBuffer.TYPE_USHORT:
                        {
                            short[][] bankData = (short[][]) array;
                            for (int i = 0; i < numBanks; i++) {
                                Arrays.fill(bankData[i], (short) 0);
                            }
                            db = new DataBufferUShort(bankData, (int) size);
                        }
                        break;
                    case DataBuffer.TYPE_SHORT:
                        {
                            short[][] bankData = (short[][]) array;
                            for (int i = 0; i < numBanks; i++) {
                                Arrays.fill(bankData[i], (short) 0);
                            }
                            db = new DataBufferShort(bankData, (int) size);
                        }
                        break;
                    case DataBuffer.TYPE_INT:
                        {
                            int[][] bankData = (int[][]) array;
                            for (int i = 0; i < numBanks; i++) {
                                Arrays.fill(bankData[i], 0);
                            }
                            db = new DataBufferInt(bankData, (int) size);
                        }
                        break;
                    case DataBuffer.TYPE_FLOAT:
                        {
                            float[][] bankData = (float[][]) array;
                            for (int i = 0; i < numBanks; i++) {
                                Arrays.fill(bankData[i], 0.0F);
                            }
                            db = DataBufferUtils.createDataBufferFloat(bankData, (int) size);
                        }
                        break;
                    case DataBuffer.TYPE_DOUBLE:
                        {
                            double[][] bankData = (double[][]) array;
                            for (int i = 0; i < numBanks; i++) {
                                Arrays.fill(bankData[i], 0.0);
                            }
                            db = DataBufferUtils.createDataBufferDouble(bankData, (int) size);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown array type");
                }
            }
        }

        if (db == null) {
            db = sampleModel.createDataBuffer();
        }

        return Raster.createWritableRaster(sampleModel, db, location);
    }

    static long getBufferSizeCSM(ComponentSampleModel csm) {
        int[] bandOffsets = csm.getBandOffsets();
        int maxBandOff = bandOffsets[0];
        for (int i = 1; i < bandOffsets.length; i++)
            maxBandOff = Math.max(maxBandOff, bandOffsets[i]);

        long size = 0;
        if (maxBandOff >= 0) size += maxBandOff + 1;
        int pixelStride = csm.getPixelStride();
        if (pixelStride > 0) size += pixelStride * (csm.getWidth() - 1);
        int scanlineStride = csm.getScanlineStride();
        if (scanlineStride > 0) size += scanlineStride * (csm.getHeight() - 1);
        return size;
    }

    private static long getNumBanksCSM(ComponentSampleModel csm) {
        int[] bankIndices = csm.getBankIndices();
        int maxIndex = bankIndices[0];
        for (int i = 1; i < bankIndices.length; i++) {
            int bankIndex = bankIndices[i];
            if (bankIndex > maxIndex) {
                maxIndex = bankIndex;
            }
        }
        return maxIndex + 1;
    }

    /** Recycle the given tile. */
    public void recycleTile(Raster tile) {
        if (tile.getWidth() != tile.getHeight() || tile.getWidth() > 512) {
            return;
        }
        recycledArrays.recycleTile(tile);
    }
}
