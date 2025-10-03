/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.io.Serial;
import java.io.Serializable;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.TileCache;
import org.geoserver.config.ImageProcessingInfo;

public class ImageProcessingInfoImpl implements Serializable, ImageProcessingInfo {

    public static final String KEY = "jai.info";

    @Serial
    private static final long serialVersionUID = 7121137497699361776L;

    boolean allowInterpolation;

    public static final boolean DEFAULT_Recycling = false;
    boolean recycling = DEFAULT_Recycling;

    public static final int DEFAULT_TilePriority = Thread.NORM_PRIORITY;
    int tilePriority = DEFAULT_TilePriority;

    public static final int DEFAULT_TileThreads = 7;
    int tileThreads = DEFAULT_TileThreads;

    public static final double DEFAULT_MemoryCapacity = 0.5;
    double memoryCapacity = DEFAULT_MemoryCapacity;

    public static final double DEFAULT_MemoryThreshold = 0.75;
    double memoryThreshold = DEFAULT_MemoryThreshold;

    public static final boolean DEFAULT_ImageIOCache = false;
    boolean imageIOCache = DEFAULT_ImageIOCache;

    PngEncoderType pngEncoderType = PngEncoderType.PNGJ;

    /** @uml.property name="allowInterpolation" */
    @Override
    public boolean getAllowInterpolation() {
        return allowInterpolation;
    }

    /** @uml.property name="allowInterpolation" */
    @Override
    public void setAllowInterpolation(boolean allowInterpolation) {
        this.allowInterpolation = allowInterpolation;
    }

    /** @uml.property name="recycling" */
    @Override
    public boolean isRecycling() {
        return recycling;
    }

    /** @uml.property name="recycling" */
    @Override
    public void setRecycling(boolean recycling) {
        this.recycling = recycling;
    }

    /** @uml.property name="tilePriority" */
    @Override
    public int getTilePriority() {
        return tilePriority;
    }

    /** @uml.property name="tilePriority" */
    @Override
    public void setTilePriority(int tilePriority) {
        this.tilePriority = tilePriority;
    }

    /** @uml.property name="tileThreads" */
    @Override
    public int getTileThreads() {
        return tileThreads;
    }

    /** @uml.property name="tileThreads" */
    @Override
    public void setTileThreads(int tileThreads) {
        this.tileThreads = tileThreads;
    }

    /** @uml.property name="memoryCapacity" */
    @Override
    public double getMemoryCapacity() {
        return memoryCapacity;
    }

    /** @uml.property name="memoryCapacity" */
    @Override
    public void setMemoryCapacity(double memoryCapacity) {
        this.memoryCapacity = memoryCapacity;
    }

    /** @uml.property name="memoryThreshold" */
    @Override
    public double getMemoryThreshold() {
        return memoryThreshold;
    }

    /** @uml.property name="memoryThreshold" */
    @Override
    public void setMemoryThreshold(double memoryThreshold) {
        this.memoryThreshold = memoryThreshold;
    }

    public void setImageIOCache(boolean imageIOCache) {
        this.imageIOCache = imageIOCache;
    }

    public boolean isImageIOCache() {
        return imageIOCache;
    }

    @Override
    public ImageN getJAI() {
        return ImageN.getDefaultInstance();
    }

    @Override
    public void setJAI(ImageN imagen) {
        // do nothing. REVISIT: we're using the singleton ImageN instance and guess there's no way to
        // get a non singleton one, so does this method make sense at all? In any case, this class
        // is meant to be serializable, hence the change in getJAI() to return the singleton
        // directly and avoid NPE's
    }

    @Override
    public TileCache getTileCache() {
        return getJAI().getTileCache();
    }

    @Override
    public void setTileCache(TileCache tileCache) {
        // do nothing. REVISIT: we're using the singleton ImageN instance and guess there's no way to
        // get a non singleton one, so does this method make sense at all? In any case, this class
        // is meant to be serializable, hence the change in getTileCache() to return the singleton
        // directly and avoid NPE's
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (allowInterpolation ? 1231 : 1237);
        result = prime * result + (imageIOCache ? 1231 : 1237);
        long temp = Double.doubleToLongBits(memoryCapacity);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(memoryThreshold);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (recycling ? 1231 : 1237);
        result = prime * result + tilePriority;
        result = prime * result + tileThreads;
        result = prime * result + getPngEncoderType().hashCode();
        result = prime * result + getPngEncoderType().hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ImageProcessingInfoImpl other = (ImageProcessingInfoImpl) obj;
        if (allowInterpolation != other.allowInterpolation) return false;
        if (imageIOCache != other.imageIOCache) return false;
        if (Double.doubleToLongBits(memoryCapacity) != Double.doubleToLongBits(other.memoryCapacity)) return false;
        if (Double.doubleToLongBits(memoryThreshold) != Double.doubleToLongBits(other.memoryThreshold)) return false;
        if (recycling != other.recycling) return false;
        if (tilePriority != other.tilePriority) return false;
        if (tileThreads != other.tileThreads) return false;
        if (getPngEncoderType() != other.getPngEncoderType()) return false;
        return true;
    }

    @Override
    public ImageProcessingInfoImpl clone() {
        try {
            return (ImageProcessingInfoImpl) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PngEncoderType getPngEncoderType() {
        if (pngEncoderType == null) {
            return PngEncoderType.PNGJ;
        } else {
            return pngEncoderType;
        }
    }

    @Override
    public void setPngEncoderType(PngEncoderType pngEncoderType) {
        this.pngEncoderType = pngEncoderType;
    }
}
