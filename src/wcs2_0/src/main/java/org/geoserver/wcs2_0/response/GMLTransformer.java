/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.measure.Unit;
import javax.media.jai.PlanarImage;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs2_0.GetCoverage;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.TypeMap;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.DateRange;
import org.geotools.util.GeoToolsUnitFormat;
import org.geotools.util.NumberRange;
import org.geotools.util.Utilities;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.opengis.coverage.SampleDimension;
import org.opengis.coverage.SampleDimensionType;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.operation.MathTransform2D;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;
import tec.uom.se.format.SimpleUnitFormat;

/**
 * Internal Base {@link GMLTransformer} for DescribeCoverage and GMLCoverageEncoding
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
class GMLTransformer extends TransformerBase {

    protected final EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    protected FileReference fileReference;

    protected String mimeType;

    public GMLTransformer(EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
        this.envelopeDimensionsMapper = envelopeDimensionsMapper;
    }

    public void setFileReference(FileReference fileReference) {
        this.fileReference = fileReference;
    }

    /** Set of custom TAGs for Metadata elements */
    static class TAG {
        private static final String RANGE = "wcsgs:Range";

        private static final String INTERVAL_START = "wcsgs:start";

        private static final String INTERVAL_END = "wcsgs:end";

        private static final String INTERVAL_PERIOD = "wcsgs:Interval";

        private static final String SINGLE_VALUE = "wcsgs:SingleValue";

        private static final String ADDITIONAL_DIMENSION = "wcsgs:DimensionDomain";

        private static final String TIME_DOMAIN = "wcsgs:TimeDomain";

        private static final String ELEVATION_DOMAIN = "wcsgs:ElevationDomain";
    }

    class GMLTranslator extends TranslatorSupport {

        protected List<WCS20CoverageMetadataProvider> extensions;
        private WCS20CoverageMetadataProvider.Translator translator =
                new WCS20CoverageMetadataProvider.Translator() {

                    @Override
                    public void start(String element, Attributes attributes) {
                        GMLTranslator.this.start(element, attributes);
                    }

                    @Override
                    public void start(String element) {
                        GMLTranslator.this.start(element);
                    }

                    @Override
                    public void end(String element) {
                        GMLTranslator.this.end(element);
                    }

                    @Override
                    public void chars(String text) {
                        GMLTranslator.this.chars(text);
                    }
                };
        protected TranslatorHelper helper = new TranslatorHelper();

        public GMLTranslator(ContentHandler contentHandler) {
            super(contentHandler, null, null);
            this.extensions = GeoServerExtensions.extensions(WCS20CoverageMetadataProvider.class);
        }

        @Override
        public void encode(Object o) throws IllegalArgumentException {
            // register namespaces provided by extended capabilities
            NamespaceSupport namespaces = getNamespaceSupport();
            namespaces.declarePrefix(
                    "wcscrs", "http://www.opengis.net/wcs/service-extension/crs/1.0");
            namespaces.declarePrefix(
                    "int", "http://www.opengis.net/WCS_service-extension_interpolation/1.0");
            namespaces.declarePrefix("gml", "http://www.opengis.net/gml/3.2");
            namespaces.declarePrefix("gmlcov", "http://www.opengis.net/gmlcov/1.0");
            namespaces.declarePrefix("swe", "http://www.opengis.net/swe/2.0");
            namespaces.declarePrefix("xlink", "http://www.w3.org/1999/xlink");
            namespaces.declarePrefix("xsi", "http://www.w3.org/2001/XMLSchema-instance");

            for (WCS20CoverageMetadataProvider cp : extensions) {
                cp.registerNamespaces(namespaces);
            }

            // is this a GridCoverage?
            if (!(o instanceof GridCoverage2D)) {
                throw new IllegalArgumentException(
                        "Provided object is not a GridCoverage2D:"
                                + (o != null ? o.getClass().toString() : "null"));
            }
            final GridCoverage2D gc2d = (GridCoverage2D) o;
            // we are going to use this name as an ID
            final String gcName = gc2d.getName().toString(Locale.getDefault());

            // get the crs and look for an EPSG code
            final CoordinateReferenceSystem crs = gc2d.getCoordinateReferenceSystem2D();
            List<String> axesNames =
                    GMLTransformer.this.envelopeDimensionsMapper.getAxesNames(
                            gc2d.getEnvelope2D(), true);

            // lookup EPSG code
            Integer EPSGCode = null;
            try {
                EPSGCode = CRS.lookupEpsgCode(crs, false);
            } catch (FactoryException e) {
                throw new IllegalStateException(
                        "Unable to lookup epsg code for this CRS:" + crs, e);
            }
            if (EPSGCode == null) {
                throw new IllegalStateException("Unable to lookup epsg code for this CRS:" + crs);
            }
            final String srsName = GetCoverage.SRS_STARTER + EPSGCode;
            // handle axes swap for geographic crs
            final boolean axisSwap = !CRS.getAxisOrder(crs).equals(AxisOrder.EAST_NORTH);

            final AttributesImpl attributes = new AttributesImpl();
            helper.registerNamespaces(getNamespaceSupport(), attributes);

            // using Name as the ID
            attributes.addAttribute(
                    "", "gml:id", "gml:id", "", gc2d.getName().toString(Locale.getDefault()));
            start("gml:RectifiedGridCoverage", attributes);

            // handle domain
            final StringBuilder builder = new StringBuilder();
            for (String axisName : axesNames) {
                builder.append(axisName).append(" ");
            }
            String axesLabel = builder.substring(0, builder.length() - 1);
            try {
                GeneralEnvelope envelope = new GeneralEnvelope(gc2d.getEnvelope());
                handleBoundedBy(envelope, axisSwap, srsName, axesLabel, null);
            } catch (IOException ex) {
                throw new WCS20Exception(ex);
            }

            // handle domain
            builder.setLength(0);
            axesNames =
                    GMLTransformer.this.envelopeDimensionsMapper.getAxesNames(
                            gc2d.getEnvelope2D(), false);
            for (String axisName : axesNames) {
                builder.append(axisName).append(" ");
            }
            axesLabel = builder.substring(0, builder.length() - 1);
            handleDomainSet(gc2d.getGridGeometry(), gc2d.getDimension(), gcName, srsName, axisSwap);

            // handle rangetype
            handleRangeType(gc2d);

            // handle coverage function
            final GridEnvelope ge2D = gc2d.getGridGeometry().getGridRange();
            handleCoverageFunction(ge2D, axisSwap);

            // handle range
            handleRange(gc2d);

            // handle metadata OPTIONAL
            try {
                handleMetadata(null, null);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            end("gml:RectifiedGridCoverage");
        }

        /**
         * Encode the coverage function or better the GridFunction as per clause 19.3.12 of GML
         * 3.2.1 which helps us with indicating in which way we traverse the data.
         *
         * <p>Notice that we use the axisOrder to actually <strong>always</strong> encode data il
         * easting,northing, hence in case of a northing,easting crs we use a reversed order to
         * indicate that we always walk on the raster columns first.
         *
         * <p>In cases where the coordinates increases in the opposite order ho our walk the
         * offsetVectors of the RectifiedGrid will do the rest.
         */
        public void handleCoverageFunction(GridEnvelope gridRange, boolean axisSwap) {
            start("gml:coverageFunction");
            start("gml:GridFunction");

            // build the fragment
            final AttributesImpl gridAttrs = new AttributesImpl();
            gridAttrs.addAttribute("", "axisOrder", "axisOrder", "", axisSwap ? "+2 +1" : "+1 +2");
            element("gml:sequenceRule", "Linear", gridAttrs); // minOccurs 0, default Linear
            element(
                    "gml:startPoint",
                    gridRange.getLow(0)
                            + " "
                            + gridRange.getLow(
                                    1)); // we start at minx, miny (this is optional though)

            end("gml:GridFunction");
            end("gml:coverageFunction");
        }

        /**
         * Encoding eventual metadata that come along with this coverage
         *
         * <pre>{@code
         * <gmlcov:metadata>
         *    <gmlcov:Extension>
         *       <myNS:metadata>Some metadata ...</myNS:metadata>
         *    </gmlcov:Extension>
         * </gmlcov:metadata>
         * }</pre>
         *
         * @param context Can be either a {@link GridCoverage2DReader} or a {@link GridCoverage2D},
         *     depending on how the method is invoked
         */
        public void handleMetadata(Object context, WCSDimensionsHelper dimensionsHelper)
                throws IOException {
            start("gmlcov:metadata");
            start("gmlcov:Extension");

            handleAdditionalMetadata(context);
            if (dimensionsHelper != null) {

                // handle time if necessary
                handleTimeMetadata(dimensionsHelper);

                // handle elevation if necessary
                handleElevationMetadata(dimensionsHelper);

                // handle additional dimensions if necessary
                handleAdditionalDimensionMetadata(dimensionsHelper);
            }

            for (WCS20CoverageMetadataProvider extension : extensions) {
                extension.encode(translator, context);
            }

            end("gmlcov:Extension");
            end("gmlcov:metadata");
        }

        protected void handleAdditionalMetadata(Object context) {
            // Override to do something.
        }

        /**
         * Look for additional dimensions in the dimensionsHelper and put additional domains to the
         * metadata
         */
        private void handleAdditionalDimensionMetadata(final WCSDimensionsHelper helper)
                throws IOException {
            Utilities.ensureNonNull("helper", helper);
            final Map<String, DimensionInfo> additionalDimensions =
                    helper.getAdditionalDimensions();
            final Set<String> dimensionsName = additionalDimensions.keySet();
            final Iterator<String> dimensionsIterator = dimensionsName.iterator();
            int index = 0;
            while (dimensionsIterator.hasNext()) {
                final String dimensionName = dimensionsIterator.next();
                final DimensionInfo customDimension = additionalDimensions.get(dimensionName);
                if (customDimension != null) {
                    setAdditionalDimensionMetadata(dimensionName, customDimension, index, helper);
                    index++;
                }
            }
        }

        /**
         * Set additional dimension metadata to the DescribeCoverage element
         *
         * @param name the custom dimension name
         * @param dimension the custom dimension related {@link DimensionInfo} instance
         * @param index the custom dimension index to ensure unique GML ids
         * @param helper the {@link WCSDimensionsHelper} instance to be used to parse domains
         */
        private void setAdditionalDimensionMetadata(
                final String name,
                final DimensionInfo dimension,
                int index,
                WCSDimensionsHelper helper)
                throws IOException {
            Utilities.ensureNonNull("helper", helper);
            final String startTag =
                    initStartMetadataTag(TAG.ADDITIONAL_DIMENSION, name, dimension, helper);

            start(startTag);
            // Custom dimension only supports List presentation
            final List<String> domain = helper.getDomain(name);
            // TODO: check if we are in the list of instants case, or in the list of periods case

            // list case
            for (int i = 0; i < domain.size(); i++) {
                String item = domain.get(i);
                Date date = WCSDimensionsValueParser.parseAsDate(item);
                if (date != null) {
                    final String dimensionId = helper.getCoverageId() + "_dd_" + index + "_" + i;
                    encodeDate(date, helper, dimensionId);
                    continue;
                }

                Double number = WCSDimensionsValueParser.parseAsDouble(item);
                if (number != null) {
                    element(TAG.SINGLE_VALUE, item.toString());
                    continue;
                }

                NumberRange<Double> range = WCSDimensionsValueParser.parseAsDoubleRange(item);
                if (range != null) {
                    encodeInterval(
                            range.getMinValue().toString(),
                            range.getMaxValue().toString(),
                            null,
                            null);
                    continue;
                }

                // TODO: Add support for date Ranges
                if (item instanceof String) {
                    element(TAG.SINGLE_VALUE, item.toString());
                }
                //                else if (item instanceof DateRange) {
                //                    final String dimensionId = helper.getCoverageId() + "_dd_" +
                // i;
                //                    encodeDateRange((DateRange) item, helper, dimensionId);
                //                }

                // TODO: Add more cases
            }
            end(TAG.ADDITIONAL_DIMENSION);
        }

        /**
         * Initialize the metadata start tag for a custom dimension, setting dimension name,
         * checking for UOM, defaultValue, ...
         *
         * @param dimensionTag the TAG referring to type of dimension (Time, Elevation, Additional
         *     ,...)
         * @param name the name of the custom dimension
         * @param dimension the custom dimension {@link DimensionInfo} instance
         * @param helper the {@link WCSDimensionsHelper} instance used to parse default values
         */
        private String initStartMetadataTag(
                final String dimensionTag,
                final String name,
                final DimensionInfo dimension,
                final WCSDimensionsHelper helper)
                throws IOException {
            final String uom = dimension.getUnitSymbol();
            String defaultValue = null;
            String prolog = null;
            if (dimensionTag.equals(TAG.ADDITIONAL_DIMENSION)) {
                prolog = TAG.ADDITIONAL_DIMENSION + " name = \"" + name + "\"";
                defaultValue = helper.getDefaultValue(name);
            } else if (dimensionTag.equals(TAG.ELEVATION_DOMAIN)) {
                prolog = TAG.ELEVATION_DOMAIN;
                defaultValue = helper.getBeginElevation();
            } else if (dimensionTag.equals(TAG.TIME_DOMAIN)) {
                prolog = TAG.TIME_DOMAIN;
                defaultValue = helper.getEndTime();
            }
            return prolog
                    + (uom != null ? (" uom=\"" + uom + "\"") : "")
                    + (defaultValue != null ? (" default=\"" + defaultValue + "\"") : "");
        }

        /** Set the timeDomain metadata in case the dimensionsHelper instance has a timeDimension */
        private void handleTimeMetadata(WCSDimensionsHelper helper) throws IOException {
            Utilities.ensureNonNull("helper", helper);
            final DimensionInfo timeDimension = helper.getTimeDimension();
            if (timeDimension != null) {
                start(initStartMetadataTag(TAG.TIME_DOMAIN, null, timeDimension, helper));
                final DimensionPresentation presentation = timeDimension.getPresentation();
                final String id = helper.getCoverageId();
                switch (presentation) {
                    case CONTINUOUS_INTERVAL:
                        encodeTimePeriod(
                                helper.getBeginTime(),
                                helper.getEndTime(),
                                id + "_tp_0",
                                null,
                                null);
                        break;
                    case DISCRETE_INTERVAL:
                        encodeTimePeriod(
                                helper.getBeginTime(),
                                helper.getEndTime(),
                                id + "_tp_0",
                                helper.getTimeResolutionUnit(),
                                helper.getTimeResolutionValue());
                        break;
                    default:
                        // TODO: check if we are in the list of instants case, or in the list of
                        // periods case

                        // list case
                        final TreeSet<Object> domain = helper.getTimeDomain();
                        int i = 0;
                        for (Object item : domain) {
                            // gml:id is mandatory for time instant...
                            if (item instanceof Date) {
                                encodeDate((Date) item, helper, id + "_td_" + i);
                            } else if (item instanceof DateRange) {
                                encodeDateRange((DateRange) item, helper, id + "_td_" + i);
                            }
                            i++;
                        }
                        break;
                }
                end(TAG.TIME_DOMAIN);
            }
        }

        /**
         * Set the elevationDomain metadata in case the dimensionsHelper instance has an
         * elevationDimension
         */
        private void handleElevationMetadata(WCSDimensionsHelper helper) throws IOException {
            // Null check has been performed in advance
            final DimensionInfo elevationDimension = helper.getElevationDimension();
            if (elevationDimension != null) {
                start(initStartMetadataTag(TAG.ELEVATION_DOMAIN, null, elevationDimension, helper));
                final DimensionPresentation presentation = elevationDimension.getPresentation();
                switch (presentation) {
                        // Where _er_ means elevation range
                    case CONTINUOUS_INTERVAL:
                        encodeInterval(
                                helper.getBeginElevation(), helper.getEndElevation(), null, null);
                        break;
                    case DISCRETE_INTERVAL:
                        encodeInterval(
                                helper.getBeginElevation(),
                                helper.getEndElevation(),
                                helper.getElevationResolutionUnit(),
                                helper.getElevationResolutionValue());
                        break;
                    default:
                        // TODO: check if we are in the list of instants case, or in the list of
                        // periods case

                        // list case
                        final TreeSet<Object> domain = helper.getElevationDomain();
                        for (Object item : domain) {
                            if (item instanceof Number) {
                                element(TAG.SINGLE_VALUE, item.toString());
                            } else if (item instanceof NumberRange) {
                                NumberRange range = (NumberRange) item;
                                encodeInterval(
                                        range.getMinValue().toString(),
                                        range.getMaxValue().toString(),
                                        null,
                                        null);
                            }
                        }
                        break;
                }
                end(TAG.ELEVATION_DOMAIN);
            }
        }

        /** Encode a DateRange item as a GML TimePeriod */
        private void encodeDateRange(
                final DateRange range, final WCSDimensionsHelper helper, final String id) {
            encodeTimePeriod(
                    helper.format(range.getMinValue()),
                    helper.format(range.getMaxValue()),
                    id,
                    null,
                    null);
        }

        /** Encode a Date item as a GML TimeInstant */
        private void encodeDate(
                final Date item, final WCSDimensionsHelper helper, final String id) {
            final AttributesImpl atts = new AttributesImpl();
            atts.addAttribute("", "gml:id", "gml:id", "", id);
            start("gml:TimeInstant", atts);
            element("gml:timePosition", helper.format(item));
            end("gml:TimeInstant");
        }

        /** Encode a GML time period */
        public void encodeTimePeriod(
                String beginPosition,
                String endPosition,
                String timePeriodId,
                String intervalUnit,
                Long intervalValue) {
            AttributesImpl atts = new AttributesImpl();
            atts.addAttribute("", "gml:id", "gml:id", "", timePeriodId);
            start("gml:TimePeriod", atts);
            element("gml:beginPosition", beginPosition);
            element("gml:endPosition", endPosition);
            if (intervalUnit != null && intervalValue != null) {
                atts = new AttributesImpl();
                atts.addAttribute("", "unit", "unit", "", intervalUnit);
                element("gml:timeInterval", intervalValue.toString(), atts);
            }
            end("gml:TimePeriod");
        }

        /** Encode Interval */
        public void encodeInterval(
                String beginPosition,
                String endPosition,
                String intervalUnit,
                Double intervalValue) {
            AttributesImpl atts = new AttributesImpl();
            start(TAG.RANGE, atts);
            element(TAG.INTERVAL_START, beginPosition);
            element(TAG.INTERVAL_END, endPosition);
            if (intervalUnit != null && intervalValue != null) {
                atts = new AttributesImpl();
                atts.addAttribute("", "unit", "unit", "", intervalUnit);
                element(TAG.INTERVAL_PERIOD, intervalValue.toString(), atts);
            }
            end(TAG.RANGE);
        }
        /**
         * Encodes the boundedBy element
         *
         * <p>e.g.:
         *
         * <pre>{@code
         * <gml:boundedBy>
         *    <gml:Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326" axisLabels="Lat Long" uomLabels="deg deg" srsDimension="2">
         *       <gml:lowerCorner>1 1</gml:lowerCorner>
         *       <gml:upperCorner>5 3</gml:upperCorner>
         *    </gml:Envelope>
         * </gml:boundedBy>
         * }</pre>
         */
        public void handleBoundedBy(
                final GeneralEnvelope envelope,
                boolean axisSwap,
                String srsName,
                String axisLabels,
                WCSDimensionsHelper dimensionHelper)
                throws IOException {
            final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
            final CoordinateSystem cs = crs.getCoordinateSystem();

            // TODO time
            String uomLabels =
                    extractUoM(crs, cs.getAxis(axisSwap ? 1 : 0).getUnit())
                            + " "
                            + extractUoM(crs, cs.getAxis(axisSwap ? 0 : 1).getUnit());

            // time and elevation dimensions management
            boolean hasElevation = false;
            boolean hasTime = false;
            if (dimensionHelper != null) {
                if (dimensionHelper.getElevationDimension() != null) {
                    uomLabels = uomLabels + " m"; // TODO: Check elevation uom
                    hasElevation = true;
                }
                if (dimensionHelper.getTimeDimension() != null) {
                    uomLabels = uomLabels + " s";
                    hasTime = true;
                }
            }
            final int srsDimension = cs.getDimension() + (hasElevation ? 1 : 0);

            // Setting up envelope bounds (including elevation)
            final String lower =
                    new StringBuilder()
                            .append(envelope.getLowerCorner().getOrdinate(axisSwap ? 1 : 0))
                            .append(" ")
                            .append(envelope.getLowerCorner().getOrdinate(axisSwap ? 0 : 1))
                            .append(hasElevation ? " " + dimensionHelper.getBeginElevation() : "")
                            .toString();

            final String upper =
                    new StringBuilder()
                            .append(envelope.getUpperCorner().getOrdinate(axisSwap ? 1 : 0))
                            .append(" ")
                            .append(envelope.getUpperCorner().getOrdinate(axisSwap ? 0 : 1))
                            .append(hasElevation ? " " + dimensionHelper.getEndElevation() : "")
                            .toString();

            // build the fragment
            final AttributesImpl envelopeAttrs = new AttributesImpl();
            envelopeAttrs.addAttribute("", "srsName", "srsName", "", srsName);
            envelopeAttrs.addAttribute("", "axisLabels", "axisLabels", "", axisLabels);
            envelopeAttrs.addAttribute("", "uomLabels", "uomLabels", "", uomLabels);
            envelopeAttrs.addAttribute(
                    "", "srsDimension", "srsDimension", "", String.valueOf(srsDimension));
            start("gml:boundedBy");
            String envelopeName = hasTime ? "gml:EnvelopeWithTimePeriod" : "gml:Envelope";
            start(envelopeName, envelopeAttrs);

            element("gml:lowerCorner", lower);
            element("gml:upperCorner", upper);

            if (hasTime) {
                element("gml:beginPosition", dimensionHelper.getBeginTime());
                element("gml:endPosition", dimensionHelper.getEndTime());
            }

            end(envelopeName);
            end("gml:boundedBy");
        }

        /** Returns a beautiful String representation for the provided {@link Unit} */
        public String extractUoM(CoordinateReferenceSystem crs, Unit<?> uom) {
            // special handling for Degrees
            if (crs instanceof GeographicCRS) {
                return "Deg";
            }
            return GeoToolsUnitFormat.getInstance().format(uom);
        }

        /**
         * Encodes the Range as per the GML spec of the provided {@link GridCoverage2D}
         *
         * @param gc2d the {@link GridCoverage2D} for which to encode the Range.
         */
        public void handleRange(GridCoverage2D gc2d) {
            // preamble
            start("gml:rangeSet");

            if (fileReference != null) {
                encodeFileReference(fileReference);
            } else {
                encodeAsDataBlocks(gc2d);
            }
            end("gml:rangeSet");
        }

        private void encodeFileReference(FileReference fileReference) {
            start("gml:File");

            final AttributesImpl atts = new AttributesImpl();
            atts.addAttribute("", "xlink:arcrole", "xlink:arcrole", "", "fileReference");
            atts.addAttribute(
                    "", "xlink:href", "xlink:href", "", "cid:" + fileReference.getReference());
            atts.addAttribute(
                    "", "xlink:role", "xlink:role", "", fileReference.getConformanceClass());
            element("gml:rangeParameters", "", atts);
            element("gml:fileReference", "cid:" + fileReference.getReference());
            element("gml:fileStructure", "");
            element("gml:mimeType", fileReference.getMimeType());

            end("gml:File");
        }

        private void encodeAsDataBlocks(GridCoverage2D gc2d) {
            start("gml:DataBlock");
            start("gml:rangeParameters");
            end("gml:rangeParameters");

            start("tupleList");
            // walk through the coverage and spit it out!
            final RenderedImage raster = gc2d.getRenderedImage();
            final int numBands = raster.getSampleModel().getNumBands();
            final int dataType = raster.getSampleModel().getDataType();
            final double[] valuesD = new double[numBands];
            final int[] valuesI = new int[numBands];
            RectIter iterator =
                    RectIterFactory.create(
                            raster, PlanarImage.wrapRenderedImage(raster).getBounds());

            iterator.startLines();
            while (!iterator.finishedLines()) {
                iterator.startPixels();
                while (!iterator.finishedPixels()) {
                    switch (dataType) {
                        case DataBuffer.TYPE_BYTE:
                        case DataBuffer.TYPE_INT:
                        case DataBuffer.TYPE_SHORT:
                        case DataBuffer.TYPE_USHORT:
                            iterator.getPixel(valuesI);
                            for (int i = 0; i < numBands; i++) {
                                // spit out
                                chars(String.valueOf(valuesI[i]));
                                if (i + 1 < numBands) {
                                    chars(",");
                                }
                            }
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                        case DataBuffer.TYPE_FLOAT:
                            iterator.getPixel(valuesD);
                            for (int i = 0; i < numBands; i++) {
                                // spit out
                                chars(String.valueOf(valuesD[i]));
                                if (i + 1 < numBands) {
                                    chars(",");
                                }
                            }
                            break;
                        default:
                            break;
                    }
                    // space as sample separator
                    chars(" ");
                    iterator.nextPixel();
                }
                iterator.nextLine();
                chars("\n");
            }

            end("tupleList");
            end("gml:DataBlock");
        }

        /**
         * Encodes the RangeType as per the GML spec of the provided {@link GridCoverage2D}
         *
         * <p>e.g.:
         *
         * <pre>{@code
         * <gmlcov:rangeType>
         *    <swe:DataRecord>
         *        <swe:field name="singleBand">
         *           <swe:Quantity definition="http://www.opengis.net/def/property/OGC/0/Radiance">
         *               <gml:description>Panchromatic Channel</gml:description>
         *               <gml:name>single band</gml:name>
         *               <swe:uom code="W/cm2"/>
         *               <swe:constraint>
         *                   <swe:AllowedValues>
         *                       <swe:interval>0 255</swe:interval>
         *                       <swe:significantFigures>3</swe:significantFigures>
         *                   </swe:AllowedValues>
         *               </swe:constraint>
         *           </swe:Quantity>
         *        </swe:field>
         *    </swe:DataRecord>
         * </gmlcov:rangeType>
         * }</pre>
         *
         * @param gc2d the {@link GridCoverage2D} for which to encode the RangeType.
         */
        public void handleRangeType(GridCoverage2D gc2d) {
            start("gml:rangeType");
            start("swe:DataRecord");

            // get bands
            final SampleDimension[] bands = gc2d.getSampleDimensions();

            // handle bands
            for (SampleDimension sd : bands) {
                final AttributesImpl fieldAttr = new AttributesImpl();
                fieldAttr.addAttribute(
                        "",
                        "name",
                        "name",
                        "",
                        sd.getDescription()
                                .toString()); // TODO NCNAME? TODO Use Band[i] convention?
                start("swe:field", fieldAttr);

                start("swe:Quantity");

                // Description
                start("swe:description");
                chars(sd.getDescription().toString()); // TODO can we make up something better??
                end("swe:description");

                // UoM
                final AttributesImpl uomAttr = new AttributesImpl();
                final Unit<?> uom = sd.getUnits();
                uomAttr.addAttribute(
                        "",
                        "code",
                        "code",
                        "",
                        uom == null ? "W.m-2.Sr-1" : SimpleUnitFormat.getInstance().format(uom));
                start("swe:uom", uomAttr);
                end("swe:uom");

                // constraint on values
                start("swe:constraint");
                start("swe:AllowedValues");
                handleSampleDimensionRange(sd); // TODO make this generic
                end("swe:AllowedValues");
                end("swe:constraint");

                // nil values
                handleSampleDimensionNilValues(gc2d, sd.getNoDataValues());

                end("swe:Quantity");
                end("swe:field");
            }

            end("swe:DataRecord");
            end("gml:rangeType");
        }

        /** @param sd */
        public void handleSampleDimensionNilValues(GridCoverage2D gc2d, GridSampleDimension sd) {
            handleSampleDimensionNilValues(gc2d, sd != null ? sd.getNoDataValues() : null);
        }

        public void handleSampleDimensionNilValues(GridCoverage2D gc2d, double[] nodataValues) {
            start("swe:nilValues");
            start("swe:NilValues");

            if (nodataValues != null && nodataValues.length > 0) {
                for (double nodata : nodataValues) {
                    final AttributesImpl nodataAttr = new AttributesImpl();
                    nodataAttr.addAttribute(
                            "",
                            "reason",
                            "reason",
                            "",
                            "http://www.opengis.net/def/nil/OGC/0/unknown");
                    element("swe:nilValue", String.valueOf(nodata), nodataAttr);
                }
            } else if (gc2d != null) {
                // do we have already a a NO_DATA value at hand?
                NoDataContainer noDataProperty = CoverageUtilities.getNoDataProperty(gc2d);
                if (noDataProperty != null) {
                    String nodata =
                            Double.valueOf(noDataProperty.getAsSingleValue())
                                    .toString(); // TODO test me
                    final AttributesImpl nodataAttr = new AttributesImpl();
                    nodataAttr.addAttribute(
                            "",
                            "reason",
                            "reason",
                            "",
                            "http://www.opengis.net/def/nil/OGC/0/unknown");
                    element("swe:nilValue", nodata, nodataAttr);
                } else {
                    // let's suggest some meaningful value from the data type of the underlying
                    // image
                    Number nodata =
                            CoverageUtilities.suggestNoDataValue(
                                    gc2d.getRenderedImage().getSampleModel().getDataType());
                    final AttributesImpl nodataAttr = new AttributesImpl();
                    nodataAttr.addAttribute(
                            "",
                            "reason",
                            "reason",
                            "",
                            "http://www.opengis.net/def/nil/OGC/0/unknown");
                    element("swe:nilValue", nodata.toString(), nodataAttr);
                }
            }

            end("swe:NilValues");
            end("swe:nilValues");
        }

        /**
         * Tries to encode a meaningful range for a {@link SampleDimension}.
         *
         * @param sd the {@link CoverageDimensionInfo} to encode a meaningful range for.
         */
        public void handleSampleDimensionRange(CoverageDimensionInfo sd) {
            if (!setRange(sd.getRange())) {
                SampleDimensionType sdType = sd.getDimensionType();
                handleSampleDimensionType(sdType);
            }
        }

        private void handleSampleDimensionType(SampleDimensionType sdType) {
            // old data dirs upgrading will have this empty
            if (sdType == null) {
                // pick the one with the largest domain and be done with it
                sdType = SampleDimensionType.REAL_64BITS;
            }
            final NumberRange<? extends Number> indicativeRange = TypeMap.getRange(sdType);
            setRange(indicativeRange);
        }

        /** Encode the interval range */
        private boolean setRange(NumberRange<? extends Number> range) {
            if (range != null
                    && !Double.isInfinite(range.getMaximum())
                    && !Double.isInfinite(range.getMinimum())) {
                start("swe:interval");
                chars(range.getMinValue() + " " + range.getMaxValue());
                end("swe:interval");
                return true;
            }
            return false;
        }

        /**
         * Tries to encode a meaningful range for a {@link SampleDimension}.
         *
         * @param sd the {@link SampleDimension} to encode a meaningful range for.
         */
        public void handleSampleDimensionRange(SampleDimension sd) {
            // look for ranges on the sample dimension
            boolean setRange = false;
            if (sd instanceof GridSampleDimension) {
                GridSampleDimension gridSd = ((GridSampleDimension) sd);
                setRange = setRange(gridSd.getRange());
            }
            if (!setRange) {
                // fallback on sampleDimensionType
                SampleDimensionType sdType = sd.getSampleDimensionType();
                handleSampleDimensionType(sdType);
            }
        }

        /**
         * Encodes the DomainSet as per the GML spec of the provided {@link GridCoverage2D}
         *
         * <p>e.g.:
         *
         * <pre>{@code
         * <gml:domainSet>
         *    <gml:Grid gml:id="gr0001_C0001" dimension="2">
         *       <gml:limits>
         *          <gml:GridEnvelope>
         *             <gml:low>1 1</gml:low>
         *             <gml:high>5 3</gml:high>
         *          </gml:GridEnvelope>
         *       </gml:limits>
         *       <gml:axisLabels>Lat Long</gml:axisLabels>
         *    </gml:Grid>
         * </gml:domainSet>
         * }</pre>
         */
        public void handleDomainSet(
                GridGeometry2D gg2D,
                int gridDimension,
                String gcName,
                String srsName,
                boolean axesSwap) {
            // setup vars
            final String gridId = "grid00__" + gcName;

            // Grid Envelope
            final GridEnvelope gridEnvelope = gg2D.getGridRange();

            final StringBuilder lowSb = new StringBuilder();
            for (int i : gridEnvelope.getLow().getCoordinateValues()) {
                lowSb.append(i).append(' ');
            }
            final StringBuilder highSb = new StringBuilder();
            for (int i : gridEnvelope.getHigh().getCoordinateValues()) {
                highSb.append(i).append(' ');
            }

            // build the fragment
            final AttributesImpl gridAttrs = new AttributesImpl();
            gridAttrs.addAttribute("", "gml:id", "gml:id", "", gridId);
            gridAttrs.addAttribute("", "dimension", "dimension", "", String.valueOf(gridDimension));

            start("gml:domainSet");
            start("gml:RectifiedGrid", gridAttrs);
            start("gml:limits");

            // GridEnvelope
            start("gml:GridEnvelope");
            element("gml:low", lowSb.toString().trim());
            element("gml:high", highSb.toString().trim());
            end("gml:GridEnvelope");

            end("gml:limits");

            // Axis Label
            element("gml:axisLabels", "i j");

            final MathTransform2D transform = gg2D.getGridToCRS2D(PixelOrientation.CENTER);
            if (!(transform instanceof AffineTransform2D)) {
                throw new IllegalStateException(
                        "Invalid grid to worl provided:" + transform.toString());
            }
            final AffineTransform2D g2W = (AffineTransform2D) transform;

            // Origin
            // we use ULC as per our G2W transformation
            final AttributesImpl pointAttr = new AttributesImpl();
            pointAttr.addAttribute("", "gml:id", "gml:id", "", "p00_" + gcName);
            pointAttr.addAttribute("", "srsName", "srsName", "", srsName);
            start("gml:origin");
            start("gml:Point", pointAttr);
            element(
                    "gml:pos",
                    axesSwap
                            ? g2W.getTranslateY() + " " + g2W.getTranslateX()
                            : g2W.getTranslateX() + " " + g2W.getTranslateY());
            end("gml:Point");
            end("gml:origin");

            // Offsets
            final AttributesImpl offsetAttr = new AttributesImpl();
            offsetAttr.addAttribute("", "srsName", "srsName", "", srsName);

            // notice the orientation of the transformation I create. The origin of the coordinates
            // in this grid is not at UPPER LEFT like in our grid to world but at LOWER LEFT !!!
            element(
                    "gml:offsetVector",
                    Double.valueOf(axesSwap ? g2W.getShearX() : g2W.getScaleX())
                            + " "
                            + Double.valueOf(axesSwap ? g2W.getScaleX() : g2W.getShearX()),
                    offsetAttr);
            element(
                    "gml:offsetVector",
                    Double.valueOf(axesSwap ? g2W.getScaleY() : g2W.getShearY())
                            + " "
                            + Double.valueOf(axesSwap ? g2W.getShearY() : g2W.getScaleY()),
                    offsetAttr);
            end("gml:RectifiedGrid");
            end("gml:domainSet");
        }
    }

    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new GMLTranslator(handler);
    }
}
