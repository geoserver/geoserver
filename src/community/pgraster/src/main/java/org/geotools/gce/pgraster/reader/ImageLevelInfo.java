/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.pgraster.reader;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Java Bean for pyramid level information. For each pyramid and the original image, there is one
 * ImageLevelInfo object
 *
 * @author mcr
 */
public class ImageLevelInfo implements Comparable<ImageLevelInfo> {

    /** Flag if ImageIO.read(InputStream in) does not return a null pointer */
    private boolean canImageIOReadFromInputStream = true;

    /** The Coordinate Reference System stored in the sql database (if supported) */
    private CoordinateReferenceSystem crs;

    /** The Spatial Reference System Id if the used database supports it */
    private Integer srsId;

    /** The name of the coverage, stored in the master table */
    private String coverageName;

    /** minimum X value of the covered extent */
    private Double extentMinX;

    /** minimum Y value of the covered extent */
    private Double extentMinY;

    /** maximum X value of the covered extent */
    private Double extentMaxX;

    /** maximu Y value of the covered extent */
    private Double extentMaxY;

    /** resolution of the x axis */
    private Double resX;

    /** resolution of the y axis */
    private Double resY;

    /** table name where to find the images */
    private String tileTableName;

    /** table name where to find georeferencing information */
    private String spatialTableName;

    /** the number of entries in the spatial table */
    private Integer countFeature;

    /** the number of entries in the tiles table */
    private Integer countTiles;

    /** storing resolutionX and resolution Y as array, for convinience */
    private double[] resolution = null;

    /** storing the extent as envelope, for convinience */
    private Envelope envelope = null;

    private Number noDataValue;

    public String getCoverageName() {
        return coverageName;
    }

    public void setCoverageName(String coverageName) {
        this.coverageName = coverageName;
    }

    public Double getExtentMaxX() {
        return extentMaxX;
    }

    public void setExtentMaxX(Double extentMaxX) {
        this.extentMaxX = extentMaxX;
        envelope = null;
    }

    public Double getExtentMaxY() {
        return extentMaxY;
    }

    public void setExtentMaxY(Double extentMaxY) {
        this.extentMaxY = extentMaxY;
        envelope = null;
    }

    public Double getExtentMinX() {
        return extentMinX;
    }

    public void setExtentMinX(Double extentMinX) {
        this.extentMinX = extentMinX;
        envelope = null;
    }

    public Double getExtentMinY() {
        return extentMinY;
    }

    public void setExtentMinY(Double extentMinY) {
        this.extentMinY = extentMinY;
        envelope = null;
    }

    public Double getResX() {
        return resX;
    }

    public void setResX(Double resX) {
        this.resX = resX;
        resolution = null;
    }

    public Double getResY() {
        return resY;
    }

    public void setResY(Double resY) {
        this.resY = resY;
        resolution = null;
    }

    public String getSpatialTableName() {
        return spatialTableName;
    }

    public void setSpatialTableName(String spatialTableName) {
        this.spatialTableName = spatialTableName;
    }

    public String getTileTableName() {
        return tileTableName;
    }

    public void setTileTableName(String tileTableName) {
        this.tileTableName = tileTableName;
    }

    @Override
    public String toString() {
        return "Coverage: "
                + getCoverageName()
                + ":"
                + getSpatialTableName()
                + ":"
                + getTileTableName();
    }

    @Override
    public int compareTo(ImageLevelInfo other) {
        int res = 0;

        if ((res = getCoverageName().compareTo(other.getCoverageName())) != 0) {
            return res;
        }

        if ((res = getResX().compareTo(other.getResX())) != 0) {
            return res;
        }

        if ((res = getResY().compareTo(other.getResY())) != 0) {
            return res;
        }

        return 0;
    }

    public double[] getResolution() {
        if (resolution != null) {
            return resolution;
        }

        resolution = new double[2];

        if (getResX() != null) {
            resolution[0] = getResX().doubleValue();
        }

        if (getResY() != null) {
            resolution[1] = getResY().doubleValue();
        }

        return resolution;
    }

    public Envelope getEnvelope() {
        if (envelope != null) {
            return envelope;
        }

        if ((getExtentMaxX() == null)
                || (getExtentMaxY() == null)
                || (getExtentMinX() == null)
                || (getExtentMinY() == null)) {
            return null;
        }

        envelope =
                new Envelope(
                        getExtentMinX().doubleValue(),
                        getExtentMaxX().doubleValue(),
                        getExtentMinY().doubleValue(),
                        getExtentMaxY().doubleValue());

        return envelope;
    }

    public Integer getCountFeature() {
        return countFeature;
    }

    public void setCountFeature(Integer countFeature) {
        this.countFeature = countFeature;
    }

    public Integer getCountTiles() {
        return countTiles;
    }

    public void setCountTiles(Integer countTiles) {
        this.countTiles = countTiles;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    public void setCrs(CoordinateReferenceSystem crs) {
        this.crs = crs;
    }

    public boolean calculateResolutionNeeded() {
        return (getResX() == null) || (getResY() == null);
    }

    public boolean calculateExtentsNeeded() {
        return (getExtentMaxX() == null)
                || (getExtentMaxY() == null)
                || (getExtentMinX() == null)
                || (getExtentMinY() == null);
    }

    public String infoString() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintWriter w = new PrintWriter(bout);
        w.print("Coveragename: ");
        w.println(getCoverageName());

        if (getCrs() != null) {
            w.print("CoordinateRefernceSystem: ");
            w.println(getCrs().getName());
        }

        if (getSrsId() != null) {
            w.print("SRS_ID: ");
            w.println(getSrsId());
        }

        w.print("Envelope: ");
        w.println(getEnvelope());

        w.print("Resolution X: ");
        w.println(getResX());

        w.print("Resolution Y: ");
        w.println(getResY());

        w.print("Tiletable: ");
        w.print(getTileTableName());

        if (getCountTiles() != null) {
            w.print(" #tiles: ");
            w.println(getCountTiles());
        }

        w.print(" Spatialtable: ");
        w.print(getSpatialTableName());

        if (getCountFeature() != null) {
            w.print(" #geometries: ");
            w.println(getCountFeature());
        }

        w.close();

        return bout.toString();
    }

    public Integer getSrsId() {
        return srsId;
    }

    public void setSrsId(Integer srsId) {
        this.srsId = srsId;
    }

    public boolean isImplementedAsTableSplit() {
        return getSpatialTableName().equals(getTileTableName()) == false;
    }

    public boolean getCanImageIOReadFromInputStream() {
        return canImageIOReadFromInputStream;
    }

    public void setCanImageIOReadFromInputStream(boolean canImageIOReadFromInputStream) {
        this.canImageIOReadFromInputStream = canImageIOReadFromInputStream;
    }

    public Number getNoDataValue() {
        return noDataValue;
    }

    public void setNoDataValue(Number noDataValue) {
        this.noDataValue = noDataValue;
    }
}
