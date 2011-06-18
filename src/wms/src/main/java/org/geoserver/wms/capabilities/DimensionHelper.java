/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.factory.GeoTools;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.geotools.util.logging.Logging;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Helper class avoiding to duplicate the time/elevation management code between WMS 1.1 and 1.3
 * 
 * @author Andrea Aime - GeoSolutions
 */
abstract class DimensionHelper {

    static final Logger LOGGER = Logging.getLogger(DimensionHelper.class);

    enum Mode {
        WMS11, WMS13
    }

    Mode mode;
	WMS wms;

    public DimensionHelper(Mode mode, WMS wms) {
        this.mode = mode;
        this.wms = wms;
    }

    /**
     * Implement to write out an element
     */
    protected abstract void element(String element, String content);

    /**
     * Implement to write out an element
     */
    protected abstract void element(String element, String content, Attributes atts);

    void handleVectorLayerDimensions(LayerInfo layer) {
        // do we have time and elevation?
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) layer.getResource();
        DimensionInfo timeInfo = typeInfo.getMetadata().get(ResourceInfo.TIME,
                DimensionInfo.class);
        DimensionInfo elevInfo = typeInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        boolean hasTime = timeInfo != null && timeInfo.isEnabled();
        boolean hasElevation = elevInfo != null && elevInfo.isEnabled();

        // skip if no need
        if (!hasTime && !hasElevation) {
            return;
        }

        if (mode == Mode.WMS11) {
            declareWMS11Dimensions(hasTime, hasElevation);
        }

        // Time dimension
        if (hasTime) {
            try {
                handleTimeDimensionVector(typeInfo);
            } catch (IOException e) {
                throw new RuntimeException("Failed to handle time attribute for layer " + e);
            }
        }
        // elevation dimension
        if (hasElevation) {
            try {
                handleElevationDimensionVector(typeInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Writes down the raster layer dimensions, if any
     * 
     * @param layer
     * @throws RuntimeException
     */
    void handleRasterLayerDimensions(final LayerInfo layer) throws RuntimeException {
        // do we have time and elevation?
        CoverageInfo cvInfo = (CoverageInfo) layer.getResource();
        DimensionInfo timeInfo = cvInfo.getMetadata()
                .get(ResourceInfo.TIME, DimensionInfo.class);
        DimensionInfo elevInfo = cvInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        boolean hasTime = timeInfo != null && timeInfo.isEnabled();
        boolean hasElevation = elevInfo != null && elevInfo.isEnabled();

        // skip if nothing is configured
        if (!hasTime && !hasElevation) {
            return;
        }
        
        if (cvInfo == null)
            throw new ServiceException("Unable to acquire coverage resource for layer: "
                    + layer.getName());

        Catalog catalog = cvInfo.getCatalog();
        if (catalog == null)
            throw new ServiceException("Unable to acquire catalog resource for layer: "
                    + layer.getName());

        CoverageStoreInfo csinfo = cvInfo.getStore();
        if (csinfo == null)
            throw new ServiceException("Unable to acquire coverage store resource for layer: "
                    + layer.getName());

        AbstractGridCoverage2DReader reader = null;
        try {
            reader = (AbstractGridCoverage2DReader) catalog.getResourcePool()
                    .getGridCoverageReader(csinfo, GeoTools.getDefaultHints());
        } catch (Throwable t) {
        	 LOGGER.log(Level.SEVERE, "Unable to acquire a reader for this coverage with format: "
        			 + csinfo.getFormat().getName(), t);
        }

        if (reader == null)
            throw new ServiceException("Unable to acquire a reader for this coverage with format: "
                    + csinfo.getFormat().getName());
        
        if (mode == Mode.WMS11) {
            declareWMS11Dimensions(hasTime, hasElevation);
        }

        // timeDimension
        String hasTimeDomain = reader.getMetadataValue("HAS_TIME_DOMAIN");
        hasTime = hasTime & "true".equalsIgnoreCase(hasTimeDomain);
        if (hasTime) {
            handleTimeDimensionRaster(timeInfo, reader);
        }

        // elevationDomain
        String haselevationDomain = reader.getMetadataValue("HAS_ELEVATION_DOMAIN");
        hasElevation = hasElevation & "true".equalsIgnoreCase(haselevationDomain);
        if (hasElevation) {
            handleElevationDimensionRaster(elevInfo, reader);
        }
    }

    private void handleElevationDimensionRaster(DimensionInfo elevInfo,
            AbstractGridCoverage2DReader reader) {
        // parse the values from the reader, they are exposed as strings...
        String[] elevationValues = reader.getMetadataValue("ELEVATION_DOMAIN").split(",");
        List<Double> elevations = new LinkedList<Double>();
        for (String val : elevationValues) {
            try {
                elevations.add(Double.parseDouble(val));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
        String elevationMetadata = getZDomainRepresentation(elevInfo, elevations);

        writeElevationDimension(elevations, elevationMetadata);
    }

    private void handleTimeDimensionRaster(DimensionInfo timeInfo,
            AbstractGridCoverage2DReader reader) {
        // parse the dates coming from the readers, they come out as strings... ugh...
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String[] timeInstants = reader.getMetadataValue("TIME_DOMAIN").split(",");
        Set<Date> values = new TreeSet<Date>();
        for (String tp : timeInstants) {
            try {
                values.add(df.parse(tp));
            } catch (ParseException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
        String timeMetadata = getTemporalDomainRepresentation(timeInfo, (TreeSet<Date>) values);

        writeTimeDimension(timeMetadata);
    }

    /**
     * Writes WMS 1.1.1 conforming dimensions (WMS 1.3 squashed dimensions and extent in the same tag instead)
     * @param hasTime
     * @param hasElevation
     */
    private void declareWMS11Dimensions(boolean hasTime, boolean hasElevation) {
        // we have to declare time and elevation before the extents
        if (hasTime) {
            AttributesImpl timeDim = new AttributesImpl();
            timeDim.addAttribute("", "name", "name", "", "time");
            timeDim.addAttribute("", "units", "units", "", "ISO8601");
            element("Dimension", null, timeDim);
        }
        if (hasElevation) {
            AttributesImpl elevDim = new AttributesImpl();
            elevDim.addAttribute("", "name", "name", "", "elevation");
            elevDim.addAttribute("", "units", "units", "", "EPSG:5030");
            element("Dimension", null, elevDim);
        }
    }

    protected String getZDomainRepresentation(DimensionInfo dimension, List<Double> values) {
        String elevationMetadata = null;

        final StringBuilder buff = new StringBuilder();

        if (DimensionPresentation.LIST == dimension.getPresentation()) {
            for (Double val : values) {
                buff.append(val);
                buff.append(",");
            }
            elevationMetadata = buff.substring(0, buff.length() - 1).toString().replaceAll("\\[",
                    "").replaceAll("\\]", "").replaceAll(" ", "");
        } else if (DimensionPresentation.CONTINUOUS_INTERVAL == dimension.getPresentation()) {
            buff.append(values.get(0));
            buff.append("/");

            buff.append(values.get(values.size() - 1));
            buff.append("/");

            Double resolution = values.get(values.size() - 1) - values.get(0);
            buff.append(resolution);

            elevationMetadata = buff.toString();
        } else if (DimensionPresentation.DISCRETE_INTERVAL == dimension.getPresentation()) {
            buff.append(values.get(0));
            buff.append("/");

            buff.append(values.get(values.size() - 1));
            buff.append("/");

            BigDecimal resolution = dimension.getResolution();
            if (resolution != null) {
                buff.append(resolution.doubleValue());
            } else {
                if (values.size() >= 2) {
                    int count = 2, i = 2;
                    Double[] zPositions = new Double[count];
                    for (Double val : values) {
                        zPositions[count - i--] = val;
                        if (i == 0)
                            break;
                    }
                    double span = zPositions[count - 1] - zPositions[count - 2];
                    buff.append(span);
                } else {
                    buff.append(0.0);
                }
            }

            elevationMetadata = buff.toString();
        }

        return elevationMetadata;
    }

    /**
     * Builds the proper presentation given the current
     * 
     * @param resourceInfo
     * @param values
     * @return
     */
    String getTemporalDomainRepresentation(DimensionInfo dimension, TreeSet<Date> values) {
        String timeMetadata = null;

        final StringBuilder buff = new StringBuilder();
        final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (DimensionPresentation.LIST == dimension.getPresentation()) {
            for (Date date : values) {
                buff.append(df.format(date)).append("Z");// ZULU
                buff.append(",");
            }
            timeMetadata = buff.substring(0, buff.length() - 1).toString().replaceAll("\\[", "")
                    .replaceAll("\\]", "").replaceAll(" ", "");
        } else if (DimensionPresentation.CONTINUOUS_INTERVAL == dimension.getPresentation()) {
            buff.append(df.format(((TreeSet<Date>) values).first())).append("Z");// ZULU
            buff.append("/");

            buff.append(df.format(((TreeSet<Date>) values).last())).append("Z");// ZULU
            buff.append("/");

            long durationInMilliSeconds = ((TreeSet<Date>) values).last().getTime()
                    - ((TreeSet<Date>) values).first().getTime();
            buff.append(new DefaultPeriodDuration(durationInMilliSeconds).toString());

            timeMetadata = buff.toString();
        } else if (DimensionPresentation.DISCRETE_INTERVAL == dimension.getPresentation()) {
            buff.append(df.format(((TreeSet<Date>) values).first())).append("Z");// ZULU
            buff.append("/");

            buff.append(df.format(((TreeSet<Date>) values).last())).append("Z");// ZULU
            buff.append("/");

            final BigDecimal resolution = dimension.getResolution();
            if (resolution != null) {
                buff.append(new DefaultPeriodDuration(resolution.longValue()).toString());
            } else {
                if (values.size() >= 2) {
                    int count = 2, i = 2;
                    Date[] timePositions = new Date[count];
                    for (Date date : values) {
                        timePositions[count - i--] = date;
                        if (i == 0)
                            break;
                    }
                    long durationInMilliSeconds = timePositions[count - 1].getTime()
                            - timePositions[count - 2].getTime();
                    buff.append(new DefaultPeriodDuration(durationInMilliSeconds).toString());
                } else {
                    buff.append(new DefaultPeriodDuration(0).toString());
                }
            }

            timeMetadata = buff.toString();
        }

        return timeMetadata;
    }

    /**
     * Writes out metadata for the time dimension
     * 
     * @param typeInfo
     * @param source
     * @param timeAttribute
     * @throws IOException
     */
    private void handleTimeDimensionVector(FeatureTypeInfo typeInfo) throws IOException {
        // build the time dim representation
        TreeSet<Date> values = wms.getFeatureTypeTimes(typeInfo);
        DimensionInfo timeInfo = typeInfo.getMetadata().get(ResourceInfo.TIME,
                DimensionInfo.class);
        String timeMetadata = getTemporalDomainRepresentation(timeInfo, (TreeSet<Date>) values);

        writeTimeDimension(timeMetadata);
    }
    
    private void handleElevationDimensionVector(FeatureTypeInfo typeInfo) throws IOException {
        
        List<Double> elevations = new ArrayList<Double>(wms.getFeatureTypeElevations(typeInfo));
        DimensionInfo di = typeInfo.getMetadata().get(ResourceInfo.ELEVATION,
                DimensionInfo.class);
        final String elevationMetadata = getZDomainRepresentation(di, elevations);

        writeElevationDimension(elevations, elevationMetadata);
    }

    private void writeTimeDimension(String timeMetadata) {
        AttributesImpl timeDim = new AttributesImpl();
        if (mode == Mode.WMS11) {
            timeDim.addAttribute("", "name", "name", "", "time");
            timeDim.addAttribute("", "default", "default", "", "current");
            element("Extent", timeMetadata, timeDim);
        } else {
            timeDim.addAttribute("", "name", "name", "", "time");
            timeDim.addAttribute("", "default", "default", "", "current");
            timeDim.addAttribute("", "units", "units", "", "ISO8601");
            element("Dimension", timeMetadata, timeDim);
        }
    }

    private void writeElevationDimension(List<Double> elevations, final String elevationMetadata) {
        AttributesImpl elevDim = new AttributesImpl();
        if (mode == Mode.WMS11) {
            elevDim.addAttribute("", "name", "name", "", "elevation");
            elevDim.addAttribute("", "default", "default", "", Double.toString(elevations.get(0)));
            element("Extent", elevationMetadata, elevDim);
        } else {
            elevDim.addAttribute("", "name", "name", "", "elevation");
            elevDim.addAttribute("", "default", "default", "", Double.toString(elevations.get(0)));
            elevDim.addAttribute("", "units", "units", "", "EPSG:5030");
            elevDim.addAttribute("", "unitSymbol", "unitSymbol", "", "m");
            element("Dimension", elevationMetadata, elevDim);
        }
    }

}
