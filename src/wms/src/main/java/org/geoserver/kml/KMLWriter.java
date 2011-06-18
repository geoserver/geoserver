/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import javax.xml.transform.TransformerException;

import org.geoserver.template.FeatureWrapper;
import org.geoserver.template.GeoServerTemplateLoader;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.DataSourceException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.geometry.jts.JTS;
import org.geotools.gml.producer.GeometryTransformer;
import org.geotools.map.MapLayer;
import org.geotools.referencing.CRS;
import org.geotools.renderer.style.LineStyle2D;
import org.geotools.renderer.style.MarkStyle2D;
import org.geotools.renderer.style.PolygonStyle2D;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.renderer.style.Style2D;
import org.geotools.renderer.style.TextStyle2D;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.NumberRange;
import org.geotools.util.Range;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Writer for KML/KMZ (Keyhole Markup Language) files. Normaly controled by an
 * EncodeKML instance, this class handles the styling information and ensures
 * that the geometries produced match the pseudo GML expected by GE.
 * 
 * @REVISIT: Once this is fully working, revisit as an extention to
 *           TransformerBase
 * @author James Macgill
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $
 * @author Brent Owens
 * 
 * @deprecated use {@link KMLTransformer}.
 */
public class KMLWriter extends OutputStreamWriter {
    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(KMLWriter.class.getPackage().getName());

    /**
     * a number formatter set up to write KML legible numbers
     */
    private static DecimalFormat formatter;

    /**
     * The template configuration
     */
    private static Configuration templateConfig;

    /**
     * Resolves the FeatureTypeStyle info per feature into a Style2D object.
     */
    private SLDStyleFactory styleFactory = new SLDStyleFactory();

    // TODO: calcuate a real value based on image size to bbox ratio, as image
    // size has no meanining for KML yet this is a fudge.
    private double scaleDenominator = 1;

    /** Tolerance used to compare doubles for equality */
    private static final double TOLERANCE = 1e-6;

    /**
     * The CRS of the data we are querying. It is a bit of a hack because
     * sometimes when we grab the CRS from the feature itself, we get null. This
     * variable is paired with setSourceCrs() so EncodeKML can can use the
     * feature type's schema to set the CRS.
     */
    private CoordinateReferenceSystem sourceCrs;

    /**
     * Handles the outputing of geometries as GML
     */
    private GeometryTransformer transformer;

    static {
        Locale locale = new Locale("en", "US");

        DecimalFormatSymbols decimalSymbols = new DecimalFormatSymbols(locale);
        decimalSymbols.setDecimalSeparator('.');
        formatter = new DecimalFormat();
        formatter.setDecimalFormatSymbols(decimalSymbols);

        // do not group
        formatter.setGroupingSize(0);

        // do not show decimal separator if it is not needed
        formatter.setDecimalSeparatorAlwaysShown(false);
        formatter.setDecimalFormatSymbols(null);

        // set default number of fraction digits
        formatter.setMaximumFractionDigits(5);

        // minimun fraction digits to 0 so they get not rendered if not needed
        formatter.setMinimumFractionDigits(0);

        // initialize the template engine, this is static to maintain a cache
        // over instantiations of kml writer
        templateConfig = new Configuration();
        templateConfig.setObjectWrapper(new FeatureWrapper());
    }

    /** Holds the map layer set, styling info and area of interest bounds */
    private WMSMapContext mapContext;
    
    /**
     * Whether vector name and description should  be generated or not
     */
    protected final boolean vectorNameDescription;
    

    /**
     * Creates a new KMLWriter object.
     * 
     * @param out
     *            OutputStream to write the KML into
     * @param config
     *            WMSMapContext describing the map to be generated.
     */
    public KMLWriter(OutputStream out, WMSMapContext mapContext, WMS wms) {
        super(out, Charset.forName("UTF-8"));
        this.mapContext = mapContext;

        transformer = new GeometryTransformer();
        // transformer.setUseDummyZ(true);
        transformer.setOmitXMLDeclaration(true);
        transformer.setNamespaceDeclarationEnabled(true);

        transformer.setNumDecimals(wms.getNumDecimals());
        
        this.vectorNameDescription = KMLUtils.getKMAttr(mapContext.getRequest(), wms);
    }

    /**
     * Sets the maximum number of digits allowed in the fraction portion of a
     * number.
     * 
     * @param numDigits
     * @see NumberFormat#setMaximumFractionDigits
     */
    public void setMaximunFractionDigits(int numDigits) {
        formatter.setMaximumFractionDigits(numDigits);
    }

    /**
     * Gets the maximum number of digits allowed in the fraction portion of a
     * number.
     * 
     * @return int numDigits
     * @see NumberFormat#getMaximumFractionDigits
     */
    public int getMaximunFractionDigits() {
        return formatter.getMaximumFractionDigits();
    }

    /**
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number.
     * 
     * @param numDigits
     * @see NumberFormat#setMinimumFractionDigits
     */
    public void setMinimunFractionDigits(int numDigits) {
        formatter.setMinimumFractionDigits(numDigits);
    }

    /*
     * Sets the minimum number of digits allowed in the fraction portion of a
     * number.
     * 
     * @param numDigits
     * 
     * @see NumberFormat#getMinimumFractionDigits
     */
    public int getMinimunFractionDigits() {
        return formatter.getMinimumFractionDigits();
    }

    public void setRequestedScale(double scale) {
        scaleDenominator = scale;
    }

    public void setSourceCrs(CoordinateReferenceSystem crs) {
        sourceCrs = crs;
    }

    /**
     * Formated version of standard write double
     * 
     * @param d
     *            The double to format and write out.
     * 
     * @throws IOException
     */
    public void write(double d) throws IOException {
        write(formatter.format(d));
    }

    /**
     * Convinience method to add a newline char to the output
     * 
     * @throws IOException
     */
    public void newline() throws IOException {
        super.write('\n');
    }

    public void writeFeaturesAsRaster(
            final SimpleFeatureCollection features,
            final MapLayer layer, final int order) throws IOException,
            AbortedException {
        Style style = layer.getStyle();

        try {
            SimpleFeatureType featureType = features.getSchema();

            setUpWriterHandler(featureType);

            FeatureTypeStyle[] fts = style.getFeatureTypeStyles();
            processStylersRaster(features, fts, layer, order);
            LOGGER.fine("encoded " + featureType.getTypeName().toString());
        } catch (NoSuchElementException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        } catch (IllegalAttributeException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        }
    }

    public void writeFeaturesAsVectors(
            final SimpleFeatureCollection features,
            final MapLayer layer) throws IOException, AbortedException {
        Style style = layer.getStyle();

        try {
            SimpleFeatureType featureType = features.getSchema();

            setUpWriterHandler(featureType);

            FeatureTypeStyle[] fts = style.getFeatureTypeStyles();
            processStylersVector(features, fts, layer);
            LOGGER.fine("encoded " + featureType.getTypeName().toString());
        } catch (NoSuchElementException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        } catch (IllegalAttributeException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        }
    }

    public void writeCoverages(
            final SimpleFeatureCollection features,
            final MapLayer layer) throws IOException, AbortedException {
        Style style = layer.getStyle();

        try {
            SimpleFeatureType featureType = features.getSchema();

            setUpWriterHandler(featureType);

            FeatureTypeStyle[] fts = style.getFeatureTypeStyles();
            processStylersCoverage(features, fts, layer);
            LOGGER.fine("encoded " + featureType.getTypeName().toString());
        } catch (NoSuchElementException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        } catch (IllegalAttributeException ex) {
            throw new DataSourceException(ex.getMessage(), ex);
        }
    }

    /**
     * Write all the features in a collection which pass the rules in the
     * provided Style object.
     * 
     * @TODO: support Name and Description information
     */

    /*
     * public void writeFeatures(final FeatureCollection features, final
     * MapLayer layer, final int order, final boolean kmz, final boolean
     * vectorResult) throws IOException, AbortedException { Style style =
     * layer.getStyle();
     * 
     * try { FeatureType featureType = features.getSchema();
     * 
     * setUpWriterHandler(featureType); FeatureTypeStyle[] fts =
     * style.getFeatureTypeStyles(); if (!kmz) processStylers(features, fts,
     * layer, order); else processStylersKMZ(features, fts, layer, order,
     * vectorResult);
     * 
     * 
     * LOGGER.fine(new StringBuffer("encoded
     * ").append(featureType.getTypeName()).toString()); } catch
     * (NoSuchElementException ex) { throw new
     * DataSourceException(ex.getMessage(), ex); } catch
     * (IllegalAttributeException ex) { throw new
     * DataSourceException(ex.getMessage(), ex); } }
     */

    /**
     * Start a new KML folder. From the spec 2.0: A top-level, optional tag used
     * to structure hierarchical arrangement of other folders, placemarks,
     * ground overlays, and screen overlays. Use this tag to structure and
     * organize your information in the Google Earth client.
     * 
     * In this context we should be using a Folder per map layer.
     * 
     * @param name
     *            A String to label this folder with, if null the name tag will
     *            be ommited
     * @param description
     *            Supplies descriptive information. This description appears in
     *            the Places window when the user clicks on the folder or ground
     *            overlay, and in a pop-up window when the user clicks on either
     *            the Placemark name in the Places window, or the placemark
     *            icon. The description element supports plain text as well as
     *            HTML formatting. A valid URL string for the World Wide Web is
     *            automatically converted to a hyperlink to that URL (e.g.
     *            http://www.google.com). if null the description tag will be
     *            ommited
     */
    public void startFolder(String name, String description) throws IOException {
        write("<Folder>");

        if (name != null) {
            write("<name>" + name + "</name>");
        }

        if (description != null) {
            write("<description>" + description + "</description>");
        }
    }

    public void startDocument(String name, String description)
            throws IOException {
        write("<Document>");

        if (name != null) {
            write("<name>" + name + "</name>");
        }

        if (description != null) {
            write("<description>" + description + "</description>");
        }
    }

    public void endFolder() throws IOException {
        write("</Folder>");
    }

    public void endDocument() throws IOException {
        write("</Document>");
    }

    /**
     * Gather any information needed to write the KML document.
     * 
     * @TODO: support writing of 'Schema' tags based on featureType
     */
    private void setUpWriterHandler(SimpleFeatureType featureType)
            throws IOException {
        String typeName = featureType.getTypeName();

        /*
         * REVISIT: To use attributes properly we need to be using the 'schema'
         * part of KML to contain custom data..
         */
        List atts = new ArrayList(0); // config.getAttributes(typeName);
    }

    /**
     * Write out the geometry. Contains workaround for the fact that KML2.0 does
     * not support multipart geometries in the same way that GML does.
     * 
     * @param geom
     *            The Geometry to be encoded, multi part geometries will be
     *            written as a sequence.
     * @param trans
     *            A GeometryTransformer to produce the gml output, its output is
     *            post processed to remove gml namespace prefixes.
     */
    protected void writeGeometry(Geometry geom, GeometryTransformer trans)
            throws IOException, TransformerException {
        if (isMultiPart(geom)) {
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                writeGeometry(geom.getGeometryN(i), trans);
            }
        } else {
            // remove gml prefixing as KML does not accept them
            StringWriter tempWriter = new StringWriter();
            // trans.setNumDecimals(config.getNumDecimals());
            trans.transform(geom, tempWriter);

            String tempBuffer = tempWriter.toString();
            // @REVISIT: should check which prefix is being used, this will only
            // work for the default (99.9%) of cases.
            write(tempBuffer.replaceAll("gml:", ""));
        }
    }

    protected void writeLookAt(Geometry geom, GeometryTransformer trans)
            throws IOException, TransformerException {
        final Coordinate[] coordinates = getCentroid(geom).getCoordinates();
        write("<LookAt>");
        write("<longitude>" + coordinates[0].x + "</longitude>");
        write("<latitude>" + coordinates[0].y + "</latitude>");
        write("<heading>10.0</heading>");
        write("<tilt>10.0</tilt>");
        write("<range>700</range>");
        write("</LookAt>");
    }

    protected void writePlaceMarkPoint(Geometry geom, GeometryTransformer trans)
            throws IOException, TransformerException {
        final Coordinate[] coordinates = getCentroid(geom).getCoordinates();
        write("<Point><coordinates>" + coordinates[0].x + ","
                + coordinates[0].y + "," + coordinates[0].z
                + "</coordinates></Point>");
    }

    /**
     * Test to see if the geometry is a Multi geometry
     * 
     * @return true if geom instance of MultiPolygon, MultiPoint or
     *         MultiLineString
     */
    protected boolean isMultiPart(Geometry geom) {
        Class geomClass = geom.getClass();

        return (geomClass.equals(MultiPolygon.class)
                || geomClass.equals(MultiPoint.class) || geomClass
                .equals(MultiLineString.class));
    }

    /**
     * Applies each feature type styler in turn to all of the features.
     * 
     * @param features
     *            A FeatureCollection contatining the features to be rendered
     * @param featureStylers
     *            An array of feature stylers to be applied
     * @throws IOException
     * @throws IllegalAttributeException
     * @TODO: multiple features types result in muliple data passes, could be
     *        split into separate tempory files then joined.
     */
    private void processStylersVector(
            final SimpleFeatureCollection features,
            final FeatureTypeStyle[] featureStylers, final MapLayer layer)
            throws IOException, IllegalAttributeException {
        final int ftsLength = featureStylers.length;

        for (int i = 0; i < ftsLength; i++) {
            FeatureTypeStyle fts = featureStylers[i];
            final String typeName = features.getSchema().getTypeName();

            if ((typeName != null)
                    && (FeatureTypes.isDecendedFrom(features.getSchema(), null,
                            fts.getFeatureTypeName()) || typeName
                            .equalsIgnoreCase(fts.getFeatureTypeName()))) {
                // get applicable rules at the current scale
                Rule[] rules = fts.getRules();
                List ruleList = new ArrayList();
                List elseRuleList = new ArrayList();
                populateRuleLists(rules, ruleList, elseRuleList, false);

                if ((ruleList.size() == 0) && (elseRuleList.size() == 0)) {
                    return; // bail out early if no rules made it (because of
                    // scale denominators)
                }

                // REVISIT: once scaleDemominator can actualy be determined
                // re-evaluate sensible ranges for GE
                NumberRange scaleRange = new NumberRange(scaleDenominator,
                        scaleDenominator);
                SimpleFeatureIterator reader = features.features();

                while (true) {
                    try {
                        if (!reader.hasNext()) {
                            break;
                        }

                        boolean doElse = true;

                        SimpleFeature feature = reader.next();
                        StringBuffer featureLabel = new StringBuffer(""); // this
                        // gets filled in if there is a textsymbolizer
                        String id = feature.getID();
                        id = id.replaceAll("&", "");
                        id = id.replaceAll(">", "");
                        id = id.replaceAll("<", "");
                        id = id.replaceAll("%", "");
                        startDocument(id, layer.getTitle());

                        // start writing out the styles
                        write("<Style id=\"GeoServerStyle" + feature.getID()
                                + "\">");

                        // applicable rules
                        for (Iterator it = ruleList.iterator(); it.hasNext();) {
                            Rule r = (Rule) it.next();
                            LOGGER.finer(new StringBuffer("applying rule: ")
                                    .append(r.toString()).toString());

                            Filter filter = r.getFilter();

                            // if there is no filter or the filter says to do
                            // the feature anyways, render it
                            if ((filter == null) || filter.evaluate(feature)) {
                                doElse = false;
                                LOGGER.finer("processing Symobolizer ...");

                                Symbolizer[] symbolizers = r.getSymbolizers();
                                processVectorSymbolizers(feature, symbolizers,
                                        scaleRange, featureLabel);
                            }
                        }

                        if (doElse) {
                            // rules with an else filter
                            LOGGER.finer("rules with an else filter");

                            for (Iterator it = elseRuleList.iterator(); it
                                    .hasNext();) {
                                Rule r = (Rule) it.next();
                                Symbolizer[] symbolizers = r.getSymbolizers();
                                LOGGER.finer("processing Symobolizer ...");
                                processVectorSymbolizers(feature, symbolizers,
                                        scaleRange, featureLabel);
                            }
                        }

                        write("</Style>"); // close off styles

                        // we have written out the style, so now lets write out
                        // the geometry
                        String fTitle = featureLabel.toString();

                        if (fTitle.equals("")) {
                            fTitle = feature.getID();
                        }

                        write("<Placemark>");
                        write("<name><![CDATA[" + featureLabel + "]]></name>"); // CDATA
                        // needed
                        // for
                        // ampersands

                        final SimpleFeatureType schema = features.getSchema();

                        // if there are supposed to be detailed descriptions,
                        // write them out
                        write("<description><![CDATA[");
                        writeDescription(feature, schema);
                        write("]]></description>");

                        writeLookAt(findGeometry(feature), transformer);
                        write("<styleUrl>#GeoServerStyle" + feature.getID()
                                + "</styleUrl>");
                        write("<MultiGeometry>");
                        writePlaceMarkPoint(findGeometry(feature), transformer);
                        writeGeometry(findGeometry(feature), transformer);
                        write("</MultiGeometry>");
                        write("</Placemark>");
                        newline();

                        endDocument(); // </Document>
                    } catch (Exception e) {
                        // that feature failed but others may still work
                        // REVISIT: don't like eating exceptions, even with a
                        // log.
                        // e.printStackTrace();
                        LOGGER.warning(new StringBuffer(
                                "KML transform for feature failed ").append(
                                e.getMessage()).toString());
                    }
                }

                // FeatureIterators may be backed by a stream so this tidies
                // things up.
                features.close(reader);
            }
        }
    }

    /**
     * Applies each feature type styler in turn to all of the features.
     * 
     * @param features
     *            A FeatureCollection contatining the features to be rendered
     * @param featureStylers
     *            An array of feature stylers to be applied
     * @throws IOException
     * @throws IllegalAttributeException
     * @TODO: multiple features types result in muliple data passes, could be
     *        split into separate tempory files then joined.
     */
    private void processStylersCoverage(
            final SimpleFeatureCollection features,
            final FeatureTypeStyle[] featureStylers, final MapLayer layer)
            throws IOException, IllegalAttributeException {
        final int ftStylesLength = featureStylers.length;

        for (int i = 0; i < ftStylesLength; i++) { // for each style

            FeatureTypeStyle fts = featureStylers[i];
            String typeName = features.getSchema().getTypeName();

            if ((typeName != null)
                    && (FeatureTypes.isDecendedFrom(features.getSchema(), null,
                            fts.getFeatureTypeName()) || typeName
                            .equalsIgnoreCase(fts.getFeatureTypeName()))) {
                // get applicable rules at the current scale
                Rule[] rules = fts.getRules();
                List ruleList = new ArrayList();
                List elseRuleList = new ArrayList();
                populateRuleLists(rules, ruleList, elseRuleList, false);

                if ((ruleList.size() == 0) && (elseRuleList.size() == 0)) {
                    return;
                }

                SimpleFeatureIterator reader = features.features();

                // we aren't going to iterate through the features because we
                // just need to prepare
                // the kml document for one feature; it is a raster result.
                try {
                    if (!reader.hasNext()) {
                        continue; // no features, so move on
                    }

                    boolean doElse = true;
                    SimpleFeature feature = reader.next();

                    // applicable rules
                    for (Iterator it = ruleList.iterator(); it.hasNext();) {
                        Rule r = (Rule) it.next();
                        LOGGER.finer(new StringBuffer("applying rule: ")
                                .append(r.toString()).toString());

                        Filter filter = r.getFilter();

                        if ((filter == null) || filter.evaluate(feature)) {
                            doElse = false;
                            LOGGER
                                    .finer("processing raster-result Symobolizer ...");

                            Symbolizer[] symbolizers = r.getSymbolizers();

                            processRasterSymbolizersForCoverage(feature,
                                    symbolizers, layer);
                        }
                    }

                    if (doElse) {
                        // rules with an else filter
                        LOGGER.finer("rules with an else filter");

                        for (Iterator it = elseRuleList.iterator(); it
                                .hasNext();) {
                            Rule r = (Rule) it.next();
                            Symbolizer[] symbolizers = r.getSymbolizers();
                            LOGGER
                                    .finer("processing raster-result Symobolizer ...");

                            processRasterSymbolizersForCoverage(feature,
                                    symbolizers, layer);
                        }
                    }
                } catch (Exception e) {
                    // that feature failed but others may still work
                    // REVISIT: don't like eating exceptions, even with a log.
                    LOGGER.warning(new StringBuffer(
                            "KML transform for feature failed ").append(
                            e.getMessage()).toString());
                }

                // FeatureIterators may be backed by a stream so this tidies
                // things up.
                features.close(reader);
            } // end if
        } // end for loop
    }

    /**
     * 
     * @param features
     * @param featureStylers
     * @param layer
     * @param order
     * @throws IOException
     * @throws IllegalAttributeException
     */
    private void processStylersRaster(
            final SimpleFeatureCollection features,
            final FeatureTypeStyle[] featureStylers, final MapLayer layer,
            final int order) throws IOException, IllegalAttributeException {
        startFolder("layer_" + order, layer.getTitle());

        int layerCounter = order;

        final int ftStylesLength = featureStylers.length;

        for (int i = 0; i < ftStylesLength; i++) { // for each style

            FeatureTypeStyle fts = featureStylers[i];
            String typeName = features.getSchema().getTypeName();

            if ((typeName != null)
                    && (FeatureTypes.isDecendedFrom(features.getSchema(), null,
                            fts.getFeatureTypeName()) || typeName
                            .equalsIgnoreCase(fts.getFeatureTypeName()))) {
                // get applicable rules at the current scale
                Rule[] rules = fts.getRules();
                List ruleList = new ArrayList();
                List elseRuleList = new ArrayList();
                populateRuleLists(rules, ruleList, elseRuleList, true);

                if ((ruleList.size() == 0) && (elseRuleList.size() == 0)) {
                    return;
                }

                SimpleFeatureIterator reader = features.features();

                // we aren't going to iterate through the features because we
                // just need to prepare
                // the kml document for one feature; it is a raster result.
                try {
                    if (!reader.hasNext()) {
                        continue; // no features, so move on
                    }

                    boolean doElse = true;
                    SimpleFeature feature = reader.next();

                    // applicable rules
                    for (Iterator it = ruleList.iterator(); it.hasNext();) {
                        Rule r = (Rule) it.next();
                        LOGGER.finer(new StringBuffer("applying rule: ")
                                .append(r.toString()).toString());

                        Filter filter = r.getFilter();

                        if ((filter == null) || filter.evaluate(feature)) {
                            doElse = false;
                            LOGGER.finer("processing raster-result Symobolizer ...");

                            Symbolizer[] symbolizers = r.getSymbolizers();

                            processRasterSymbolizers(feature, symbolizers,
                                    order);
                            layerCounter++;
                        }
                    }

                    if (doElse) {
                        // rules with an else filter
                        LOGGER.finer("rules with an else filter");

                        for (Iterator it = elseRuleList.iterator(); it
                                .hasNext();) {
                            Rule r = (Rule) it.next();
                            Symbolizer[] symbolizers = r.getSymbolizers();
                            LOGGER.finer("processing raster-result Symobolizer ...");

                            processRasterSymbolizers(feature, symbolizers,
                                    order);
                            layerCounter++;
                        }
                    }
                } catch (Exception e) {
                    // that feature failed but others may still work
                    // REVISIT: don't like eating exceptions, even with a log.
                    LOGGER.warning(new StringBuffer(
                            "KML transform for feature failed ").append(
                            e.getMessage()).toString());
                }

                // FeatureIterators may be backed by a stream so this tidies
                // things up.
                features.close(reader);
            } // end if
        } // end for loop

        endFolder(); // close the folder </Folder>
    }

    /**
     * Sorts the rules into "If" rules and "Else" rules. The rules are sorted
     * into their respective lists.
     * 
     * @param rules
     * @param ruleList
     * @param elseRuleList
     * @param ignoreScale
     *            ignore the scale denominator
     */
    private void populateRuleLists(Rule[] rules, List ruleList,
            List elseRuleList, boolean ignoreScale) {
        final int rulesLength = rules.length;

        for (int j = 0; j < rulesLength; j++) {
            Rule r = rules[j];

            if (ignoreScale) {
                if (r.hasElseFilter()) {
                    elseRuleList.add(r);
                } else {
                    ruleList.add(r);
                }
            } else {
                if (isWithinScale(r)) {
                    if (r.hasElseFilter()) {
                        elseRuleList.add(r);
                    } else {
                        ruleList.add(r);
                    }
                }
            }
        }
    }

    private void writeDescription(SimpleFeature feature,
            final SimpleFeatureType schema) throws IOException {
        if (vectorNameDescription) {
            // descriptions are "templatable" by users, so see if there is a
            // template available for use
            GeoServerTemplateLoader templateLoader = new GeoServerTemplateLoader(
                    getClass());
            templateLoader.setFeatureType(schema);

            Template template = null;

            // Configuration is not thread safe
            synchronized (templateConfig) {
                templateConfig.setTemplateLoader(templateLoader);
                template = templateConfig.getTemplate("kmlDescription.ftl");
            }

            try {
                template.setEncoding("UTF-8");
                template.process(feature, this);
            } catch (TemplateException e) {
                String msg = "Error occured processing template.";
                throw (IOException) new IOException(msg).initCause(e);
            }
        }
    }

    private void processVectorSymbolizers(final SimpleFeature feature,
            final Symbolizer[] symbolizers, Range scaleRange,
            StringBuffer featureLabel) throws IOException, TransformerException {
        final int length = symbolizers.length;

        // for each Symbolizer (text, polygon, line etc...)
        for (int m = 0; m < length; m++) {
            LOGGER.finer(new StringBuffer("applying symbolizer ").append(
                    symbolizers[m]).toString());

            if (symbolizers[m] instanceof TextSymbolizer) {
                TextSymbolizer ts = (TextSymbolizer) symbolizers[m];
                Expression ex = ts.getLabel();
                featureLabel.append((String) ex.evaluate(feature, String.class)); // attach
                // the lable title

                Style2D style = styleFactory.createStyle(feature,
                        symbolizers[m], scaleRange);
                writeStyle(style, feature.getID(), symbolizers[m]);
                
            } else { // all other symbolizers
                Style2D style = styleFactory.createStyle(feature,
                        symbolizers[m], scaleRange);
                writeStyle(style, feature.getID(), symbolizers[m]);
            }
        } // end for loop
    }

    /**
     * Writes out the KML for a ground overlay. The image is processed later on.
     * 
     * This will style the KML for raster output. There are no descriptions with
     * vector output, as that would make the result really large (assuming that
     * they chose raster output because a lot of features were requested).
     * 
     * 
     * @param feature
     * @param symbolizers
     * @param order
     * @throws IOException
     * @throws TransformerException
     */
    private void processRasterSymbolizers(final SimpleFeature feature,
            final Symbolizer[] symbolizers, final int order)
            throws IOException, TransformerException {
        if (symbolizers.length < 1) {
            return; // no symbolizers so return
        }

        LOGGER.finer("applying one symbolizer: " + symbolizers[0].toString());

        com.vividsolutions.jts.geom.Envelope envelope = this.mapContext
                .getRequest().getBbox();
        write(new StringBuffer("<GroundOverlay>")
                .append("<name>").append(feature.getID()).append("</name>")
                .append("<drawOrder>").append(order).append("</drawOrder>")
                .append("<Icon>").toString());

        final double[] BBOX = new double[] { envelope.getMinX(),
                envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY() };
        write(new StringBuffer("<href>layer_").append(order).append(".png</href>")
                .append("<viewRefreshMode>never</viewRefreshMode>")
                .append("<viewBoundScale>0.75</viewBoundScale>")
                .append("</Icon>")
                .append("<LatLonBox>")
                .append("<north>").append(BBOX[3]).append("</north>")
                .append("<south>").append(BBOX[1]).append("</south>")
                .append("<east>").append(BBOX[2]).append("</east>")
                .append("<west>").append(BBOX[0]).append("</west>")
                .append("</LatLonBox>")
                .append("</GroundOverlay>").toString());
    }

    /**
     * Writes out the KML for a ground overlay. The image is processed later on.
     * 
     * @param feature
     * @param symbolizers
     * @param order
     * @throws IOException
     * @throws TransformerException
     */
    private void processRasterSymbolizersForCoverage(
            final SimpleFeature feature, final Symbolizer[] symbolizers,
            final MapLayer layer) throws IOException, TransformerException {
        if (symbolizers.length < 1) {
            return; // no symbolizers so return
        }

        LOGGER.finer("applying one symbolizer: " + symbolizers[0].toString());

        final AbstractGridCoverage2DReader gcReader = 
            (AbstractGridCoverage2DReader) feature.getAttribute("grid");

        // TODO add read parameters feature.getAttribute("params")
        final String baseURL = mapContext.getRequest().getBaseUrl();

        com.vividsolutions.jts.geom.Envelope envelope = this.mapContext
                .getRequest().getBbox();
        write(new StringBuffer("<GroundOverlay>").append("<name>").append(
                feature.getID()).append("</name>").append("<Icon>").toString());

        final double[] BBOX = new double[] { envelope.getMinX(),
                envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY() };

        final StringBuffer getMapRequest = new StringBuffer(baseURL)
                .append("wms?bbox=")
                .append(BBOX[0])
                .append(",")
                .append(BBOX[1])
                .append(",")
                .append(BBOX[2])
                .append(",")
                .append(BBOX[3])
                .append("&amp;styles=")
                .append(layer.getStyle().getName())
                .append("&amp;Format=image/png&amp;request=GetMap&amp;layers=")
                .append(layer.getTitle())
                .append("&amp;width="+ this.mapContext.getMapWidth()
                                + "&amp;height="
                                + this.mapContext.getMapHeight()
                                + "&amp;srs=EPSG:4326&amp;transparent=true&amp;");

        write(new StringBuffer("<href>").append(getMapRequest).append("</href>")
                .append("<viewRefreshMode>never</viewRefreshMode>")
                .append("<viewBoundScale>0.75</viewBoundScale>")
                .append("</Icon>")
                .append("<LatLonBox>")
                .append("<north>").append(BBOX[3]).append("</north>")
                .append("<south>").append(BBOX[1]).append("</south>")
                .append("<east>").append(BBOX[2]).append("</east>")
                .append("<west>").append(BBOX[0]).append("</west>")
                .append("</LatLonBox>")
                .append("</GroundOverlay>").toString());
    }

    /**
     * Applies each of a set of symbolizers in turn to a given feature.
     * <p>
     * This is an internal method and should only be called by processStylers.
     * </p>
     * 
     * The KML color tag: The order of expression is alpha, blue, green, red
     * (ABGR). The range of values for any one color is 0 to 255 (00 to ff). For
     * opacity, 00 is fully transparent and ff is fully opaque.
     * 
     * @param feature
     *            The feature to be rendered
     * @param symbolizers
     *            An array of symbolizers which actually perform the rendering.
     * @param scaleRange
     *            The scale range we are working on... provided in order to make
     *            the style factory happy
     */
    private boolean processSymbolizers(
            final SimpleFeatureCollection features,
            final SimpleFeature feature, final Symbolizer[] symbolizers,
            Range scaleRange, final MapLayer layer, final int order,
            final int layerCounter, StringBuffer title, boolean vectorResult)
            throws IOException, TransformerException {
        boolean res = false;

        // String title=null;
        final int length = symbolizers.length;

        // for each Symbolizer (text, polygon, line etc...)
        for (int m = 0; m < length; m++) {
            LOGGER.finer(new StringBuffer("applying symbolizer ").append(
                    symbolizers[m]).toString());

            if (symbolizers[m] instanceof RasterSymbolizer) {
                // LOGGER.info("Removed by bao for testing");
                /*
                 * final GridCoverage gc = (GridCoverage)
                 * feature.getAttribute("grid"); final HttpServletRequest
                 * request =
                 * this.mapContext.getRequest().getHttpServletRequest(); final
                 * String baseURL =
                 * org.vfny.geoserver.util.Requests.getBaseUrl(request);
                 * com.vividsolutions.jts.geom.Envelope envelope =
                 * this.mapContext.getRequest().getBbox();
                 */

                /**
                 * EXAMPLE OUTPUT: <GroundOverlay> <name>Google Earth - New
                 * Image Overlay</name> <Icon>
                 * <href>http://localhost:8081/geoserver
                 * /wms?bbox=-130,24,-66,50&
                 * amp;styles=raster&amp;Format=image/tiff
                 * &amp;request=GetMap&amp
                 * ;layers=nurc:Img_Sample&amp;width=550&amp
                 * ;height=250&amp;srs=EPSG:4326&amp;</href>
                 * <viewRefreshMode>never</viewRefreshMode>
                 * <viewBoundScale>0.75</viewBoundScale> </Icon> <LatLonBox>
                 * <north>50.0</north> <south>24.0</south> <east>-66.0</east>
                 * <west>-130.0</west> </LatLonBox> </GroundOverlay>
                 */

                /*
                 * write(new StringBuffer("<GroundOverlay>").
                 * append("<name>").append
                 * (((GridCoverage2D)gc).getName()).append("</name>").
                 * append("<drawOrder>").append(order).append("</drawOrder>").
                 * append("<Icon>").toString()); final double[] BBOX = new
                 * double[] { envelope.getMinX(), envelope.getMinY(),
                 * envelope.getMaxX(), envelope.getMaxY() }; if (layerCounter<0)
                 * { final StringBuffer getMapRequest = new
                 * StringBuffer(baseURL)
                 * .append("wms?bbox=").append(BBOX[0]).append(",").
                 * append(BBOX[
                 * 1]).append(",").append(BBOX[2]).append(",").append
                 * (BBOX[3]).append("&amp;styles=").
                 * append(layer.getStyle().getName
                 * ()).append("&amp;Format=image/png&amp;request=GetMap&amp;layers="
                 * ).
                 * append(layer.getTitle()).append("&amp;width="+this.mapContext
                 * .getMapWidth()+"&amp;height="+this.mapContext.getMapHeight()+
                 * "&amp;srs=EPSG:4326&amp;");
                 * write("<href>"+getMapRequest.toString()+"</href>"); } else {
                 * write("<href>layer_"+order+".png</href>"); } write(new
                 * StringBuffer("<viewRefreshMode>never</viewRefreshMode>").
                 * append("<viewBoundScale>0.75</viewBoundScale>").
                 * append("</Icon>"). append("<LatLonBox>").
                 * append("<north>").append(BBOX[3]).append("</north>").
                 * append("<south>").append(BBOX[1]).append("</south>").
                 * append("<east>").append(BBOX[2]).append("</east>").
                 * append("<west>").append(BBOX[0]).append("</west>").
                 * append("</LatLonBox>").
                 * append("</GroundOverlay>").toString()); //Geometry g =
                 * findGeometry(feature, symbolizers[m]);
                 * //writeRasterStyle(getMapRequest.toString(),
                 * feature.getID());
                 */
                res = true;
            } else if (vectorResult) {
                // TODO: come back and sort out crs transformation
                // CoordinateReferenceSystem crs = findGeometryCS(feature,
                // symbolizers[m]);
                if (symbolizers[m] instanceof TextSymbolizer) {
                    TextSymbolizer ts = (TextSymbolizer) symbolizers[m];
                    Expression ex = ts.getLabel();
                    String value = (String) ex.evaluate(feature, String.class);
                    title.append(value);

                    Style2D style = styleFactory.createStyle(feature,
                            symbolizers[m], scaleRange);
                    writeStyle(style, feature.getID(), symbolizers[m]);
                } else {
                    Style2D style = styleFactory.createStyle(feature,
                            symbolizers[m], scaleRange);
                    writeStyle(style, feature.getID(), symbolizers[m]);
                }
            } else if (!vectorResult) {
                com.vividsolutions.jts.geom.Envelope envelope = this.mapContext
                        .getRequest().getBbox();
                write(new StringBuffer("<GroundOverlay>").append("<name>")
                        .append(feature.getID()).append("</name>").append(
                                "<drawOrder>").append(order).append(
                                "</drawOrder>").append("<Icon>").toString());

                final double[] BBOX = new double[] { envelope.getMinX(),
                        envelope.getMinY(), envelope.getMaxX(),
                        envelope.getMaxY() };
                write(new StringBuffer("<href>layer_").append(order).append(".png</href>")
                        .append("<viewRefreshMode>never</viewRefreshMode>")
                        .append("<viewBoundScale>0.75</viewBoundScale>")
                        .append("</Icon>")
                        .append("<LatLonBox>")
                        .append("<north>").append(BBOX[3]).append("</north>")
                        .append("<south>").append(BBOX[1]).append("</south>")
                        .append("<east>").append(BBOX[2]).append("</east>")
                        .append("<west>").append(BBOX[0]).append("</west>")
                        .append("</LatLonBox>")
                        .append("</GroundOverlay>").toString());
            } else {
                LOGGER.info("KMZ processSymbolizerz unknown case. Please report error.");
            }
        }

        return res;
    }

    /**
     * Adds the <style> tag to the KML document.
     * 
     * @param style
     * @param id
     * @throws IOException
     */
    private void writeStyle(final Style2D style, final String id, Symbolizer sym)
            throws IOException {
        if (style instanceof PolygonStyle2D && sym instanceof PolygonSymbolizer) {
            if ((((PolygonStyle2D) style).getFill() == null)
                    && (((PolygonStyle2D) style).getStroke() == null)) {
                LOGGER.info("Empty PolygonSymbolizer, using default fill and stroke.");
            }

            final StringBuffer styleString = new StringBuffer();

            PolygonSymbolizer polySym = (PolygonSymbolizer) sym;

            // ** LABEL **
            styleString.append("<IconStyle>");

            if (!vectorNameDescription) { // if they don't want
                // attributes
                styleString.append("<color>#00ffffff</color>"); // fully
                // transparent
            }

            styleString.append("<Icon><href>root://icons/palette-3.png</href><x>224</x><w>32</w><h>32</h></Icon>");
            styleString.append("</IconStyle>");

            // ** FILL **
            styleString.append("<PolyStyle><color>");

            if (polySym.getFill() != null) // if they specified a fill
            {
                int opacity = 255; // default to full opacity

                if (polySym.getFill().getOpacity() != null) {
                    float op = getOpacity(polySym.getFill().getOpacity());
                    opacity = (new Float(255 * op)).intValue();
                }

                Paint p = ((PolygonStyle2D) style).getFill();

                if (p instanceof Color) {
                    styleString.append("#").append(intToHex(opacity)).append(
                            colorToHex((Color) p)); // transparancy needs to
                    // come from the opacity
                    // value.
                } else {
                    styleString.append("#ffaaaaaa"); // should not occure in
                    // normal parsing
                }
            } else { // no fill specified, make transparent
                styleString.append("#00aaaaaa");
            }

            // if there is an outline, specify that we have one, then style it
            styleString.append("</color>");

            if (polySym.getStroke() != null) {
                styleString.append("<outline>1</outline>");
            } else {
                styleString.append("<outline>0</outline>");
            }

            styleString.append("</PolyStyle>");

            // ** OUTLINE **
            if (polySym.getStroke() != null) // if there is an outline
            {
                styleString.append("<LineStyle><color>");

                int opacity = 255; // default to full opacity

                if (polySym.getStroke().getOpacity() != null) {
                    float op = getOpacity(polySym.getStroke().getOpacity());
                    opacity = (new Float(255 * op)).intValue();
                }

                Paint p = ((PolygonStyle2D) style).getContour();

                if (p instanceof Color) {
                    styleString.append("#").append(intToHex(opacity)).append(
                            colorToHex((Color) p)); // transparancy needs to
                    // come from the opacity
                    // value.
                } else {
                    styleString.append("#ffaaaaaa"); // should not occure in
                    // normal parsing
                }

                styleString.append("</color>");

                // stroke width
                if (polySym.getStroke().getWidth() != null) {
                    int width = getWidth(polySym.getStroke().getWidth());
                    styleString.append("<width>").append(width).append(
                            "</width>");
                }

                styleString.append("</LineStyle>");
            }

            write(styleString.toString());
        } else if (style instanceof LineStyle2D
                && sym instanceof LineSymbolizer) {
            if (((LineStyle2D) style).getStroke() == null) {
                LOGGER.info("Empty LineSymbolizer, using default stroke.");
            }

            LineSymbolizer lineSym = (LineSymbolizer) sym;

            // ** LABEL **
            final StringBuffer styleString = new StringBuffer();
            styleString.append("<IconStyle>");

            if (!vectorNameDescription) { // if they don't want
                // attributes
                styleString.append("<color>#00ffffff</color>"); // fully
                // transparent
            }

            styleString.append("</IconStyle>");

            // ** LINE **
            styleString.append("<LineStyle><color>");

            if (lineSym.getStroke() != null) {
                int opacity = 255;

                if (lineSym.getStroke().getOpacity() != null) {
                    float op = getOpacity(lineSym.getStroke().getOpacity());
                    opacity = (new Float(255 * op)).intValue();
                }

                Paint p = ((LineStyle2D) style).getContour();

                if (p instanceof Color) {
                    styleString.append("#").append(intToHex(opacity)).append(
                            colorToHex((Color) p)); // transparancy needs to
                    // come from the opacity
                    // value.
                } else {
                    styleString.append("#ffaaaaaa"); // should not occure in
                    // normal parsing
                }

                styleString.append("</color>");

                // stroke width
                if (lineSym.getStroke().getWidth() != null) {
                    int width = getWidth(lineSym.getStroke().getWidth());
                    styleString.append("<width>").append(width).append(
                            "</width>");
                }
            } else // no style defined, so use default
            {
                styleString.append("#ffaaaaaa");
                styleString.append("</color><width>1</width>");
            }

            styleString.append("</LineStyle>");

            write(styleString.toString());
        } else if (style instanceof TextStyle2D
                && sym instanceof TextSymbolizer) {
            final StringBuffer styleString = new StringBuffer();
            TextSymbolizer textSym = (TextSymbolizer) sym;

            styleString.append("<LabelStyle><color>");

            if (textSym.getFill() != null) {
                int opacity = 255;

                if (textSym.getFill().getOpacity() != null) {
                    float op = getOpacity(textSym.getFill().getOpacity());
                    opacity = (new Float(255 * op)).intValue();
                }

                Paint p = ((TextStyle2D) style).getFill();

                if (p instanceof Color) {
                    styleString.append("#").append(intToHex(opacity)).append(
                            colorToHex((Color) p)); 
                    // transparancy needs to come from the opacity value.
                } else {
                    styleString.append("#ffaaaaaa"); // should not occure in
                    // normal parsing
                }

                styleString.append("</color></LabelStyle>");
            } else {
                styleString.append("#ffaaaaaa");
                styleString.append("</color></LabelStyle>");
            }

            write(styleString.toString());
        } else if (style instanceof MarkStyle2D
                && sym instanceof PointSymbolizer) {
            // we can sorta style points. Just with color however.
            final StringBuffer styleString = new StringBuffer();
            PointSymbolizer pointSym = (PointSymbolizer) sym;

            styleString.append("<IconStyle><color>");

            if ((pointSym.getGraphic() != null)
                    && (pointSym.getGraphic().getMarks() != null)) {
                Mark[] marks = pointSym.getGraphic().getMarks();

                if ((marks.length > 0) && (marks[0] != null)) {
                    Mark mark = marks[0];

                    int opacity = 255;

                    if (mark.getFill().getOpacity() != null) {
                        float op = getOpacity(mark.getFill().getOpacity());
                        opacity = (new Float(255 * op)).intValue();
                    }

                    Paint p = ((MarkStyle2D) style).getFill();

                    if (p instanceof Color) {
                        styleString.append("#").append(intToHex(opacity))
                                .append(colorToHex((Color) p)); 
                        // transparancy needs to comefrom the opacity value.
                    } else {
                        styleString.append("#ffaaaaaa"); // should not occure
                        // in normal parsing
                    }
                } else {
                    styleString.append("#ffaaaaaa");
                }
            } else {
                styleString.append("#ffaaaaaa");
            }

            styleString.append("</color>");
            styleString.append("<colorMode>normal</colorMode>");
            styleString.append("<Icon><href>root://icons/palette-4.png</href>");
            styleString.append("<x>32</x><y>128</y><w>32</w><h>32</h></Icon>");

            styleString.append("</IconStyle>");

            write(styleString.toString());
        }
    }

    /**
     * @param href
     * @param id
     * @throws IOException
     */
    private void writeRasterStyle(final String href, final String id)
            throws IOException {
        final StringBuffer styleString = new StringBuffer();
        styleString.append("<Style id=\"GeoServerStyle").append(id).append(
                "\">");
        styleString.append("<IconStyle><Icon><href>").append(href).append(
                "</href><viewRefreshMode>never</viewRefreshMode>").append(
                "<viewBoundScale>0.75</viewBoundScale><w>").append(
                this.mapContext.getMapWidth()).append("</w><h>").append(
                this.mapContext.getMapHeight()).append(
                "</h></Icon></IconStyle>");
        styleString
                .append("<PolyStyle><fill>0</fill><outline>0</outline></PolyStyle>");
        styleString.append("</Style>");

        write(styleString.toString());
    }

    private boolean isWithinScale(Rule r) {
        double min = r.getMinScaleDenominator();
        double max = r.getMaxScaleDenominator();

        if (((min - TOLERANCE) <= scaleDenominator)
                && ((max + TOLERANCE) >= scaleDenominator)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Finds the geometric attribute requested by the symbolizer
     * 
     * @param f
     *            The feature
     * @param s
     *            The symbolizer
     * @return The geometry requested in the symbolizer, or the default geometry
     *         if none is specified
     */
    private com.vividsolutions.jts.geom.Geometry findGeometry(SimpleFeature f,
            Symbolizer s) {
        String geomName = getGeometryPropertyName(s);

        // get the geometry
        Geometry geom;

        if (geomName == null) {
            geom = (Geometry) f.getDefaultGeometry();
        } else {
            geom = (com.vividsolutions.jts.geom.Geometry) f
                    .getAttribute(geomName);
        }

        // if the symbolizer is a point symbolizer generate a suitable location
        // to place the
        // point in order to avoid recomputing that location at each rendering
        // step
        if (s instanceof PointSymbolizer) {
            geom = getCentroid(geom); // djb: major simpificatioN
        }

        return geom;
    }

    /**
     * Returns the default geometry in the feature.
     * 
     * @param f
     *            feature to find the geometry in
     * @return
     */
    private com.vividsolutions.jts.geom.Geometry findGeometry(SimpleFeature f) {
        // get the geometry
        Geometry geom = (Geometry) f.getDefaultGeometry();

        // CoordinateReferenceSystem sourceCRS =
        // f.getFeatureType().getDefaultGeometry().getCoordinateSystem();
        if (!CRS.equalsIgnoreMetadata(sourceCrs, this.mapContext
                .getCoordinateReferenceSystem())) {
            try {
                MathTransform transform = CRS.findMathTransform(sourceCrs,
                        this.mapContext.getCoordinateReferenceSystem(), true);
                geom = JTS.transform(geom, transform);
            } catch (MismatchedDimensionException e) {
                LOGGER.severe(e.getLocalizedMessage());
            } catch (TransformException e) {
                LOGGER.severe(e.getLocalizedMessage());
            } catch (FactoryException e) {
                LOGGER.severe(e.getLocalizedMessage());
            }
        }

        return geom;
    }

    /**
     * Finds the centroid of the input geometry if input = point, line, polygon
     * --> return a point that represents the centroid of that geom if input =
     * geometry collection --> return a multipoint that represents the centoid
     * of each sub-geom
     * 
     * @param g
     * @return
     */
    public Geometry getCentroid(Geometry g) {
        if (g instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) g;
            Coordinate[] pts = new Coordinate[gc.getNumGeometries()];

            for (int t = 0; t < gc.getNumGeometries(); t++) {
                pts[t] = gc.getGeometryN(t).getCentroid().getCoordinate();
            }

            return g.getFactory().createMultiPoint(pts);
        } else {
            return g.getCentroid();
        }
    }

    /**
     * Finds the geometric attribute coordinate reference system
     * 
     * @param f
     *            The feature
     * @param s
     *            The symbolizer
     * @return The geometry requested in the symbolizer, or the default geometry
     *         if none is specified
     */
    private org.opengis.referencing.crs.CoordinateReferenceSystem findGeometryCS(
            SimpleFeature f, Symbolizer s) {
        String geomName = getGeometryPropertyName(s);

        if (geomName != null) {
            return ((GeometryDescriptor) f.getFeatureType().getDescriptor(
                    geomName)).getCoordinateReferenceSystem();
        } else {
            return f.getFeatureType().getCoordinateReferenceSystem();
        }
    }

    /**
     * Utility method to find which geometry property is referenced by a given
     * symbolizer.
     * 
     * @param s
     *            The symbolizer
     * @TODO: this is c&p from lite renderer code as the method was private
     *        consider moving to a public unility class.
     */
    private String getGeometryPropertyName(Symbolizer s) {
        String geomName = null;

        // TODO: fix the styles, the getGeometryPropertyName should probably be
        // moved into an
        // interface...
        if (s instanceof PolygonSymbolizer) {
            geomName = ((PolygonSymbolizer) s).getGeometryPropertyName();
        } else if (s instanceof PointSymbolizer) {
            geomName = ((PointSymbolizer) s).getGeometryPropertyName();
        } else if (s instanceof LineSymbolizer) {
            geomName = ((LineSymbolizer) s).getGeometryPropertyName();
        } else if (s instanceof TextSymbolizer) {
            geomName = ((TextSymbolizer) s).getGeometryPropertyName();
        }

        return geomName;
    }

    /**
     * Utility method to convert an int into hex, padded to two characters.
     * handy for generating colour strings.
     * 
     * @param i
     *            Int to convert
     * @return String a two character hex representation of i NOTE: this is a
     *         utility method and should be put somewhere more useful.
     */
    protected static String intToHex(int i) {
        String prelim = Integer.toHexString(i);

        if (prelim.length() < 2) {
            prelim = "0" + prelim;
        }

        return prelim;
    }

    /**
     * Utility method to convert a Color into a KML color ref
     * 
     * @param c
     *            The color to convert
     * @return A string in BBGGRR format - note alpha must be prefixed seperatly
     *         before use.
     */
    private String colorToHex(Color c) {
        return intToHex(c.getBlue()) + intToHex(c.getGreen())
                + intToHex(c.getRed());
    }

    /**
     * Borrowed from StreamingRenderer
     * 
     * @param sym
     * @return
     */
    private float getOpacity(final Symbolizer sym) {
        float alpha = 1.0f;
        Expression exp = null;

        if (sym instanceof PolygonSymbolizer) {
            exp = ((PolygonSymbolizer) sym).getFill().getOpacity();
        } else if (sym instanceof LineSymbolizer) {
            exp = ((LineSymbolizer) sym).getStroke().getOpacity();
        } else if (sym instanceof PointSymbolizer) {
            exp = ((PointSymbolizer) sym).getGraphic().getOpacity();
        } else if (sym instanceof TextSymbolizer) {
            exp = ((TextSymbolizer) sym).getFill().getOpacity();
        } else {
            LOGGER.info("Symbolizer not matched; was of class: " + sym);
        }

        if (exp == null) {
            LOGGER.info("Could not determine proper symbolizer opacity.");

            return alpha;
        }

        Float number = (Float) exp.evaluate(null, Float.class);

        if (number == null) {
            return alpha;
        }

        return number.floatValue();
    }

    private float getOpacity(final Expression exp) {
        float alpha = 1.0f;

        Float number = (Float) exp.evaluate(null, Float.class);

        if (number == null) {
            return alpha;
        }

        return number.floatValue();
    }

    private int getWidth(final Expression exp) {
        int defaultWidth = 1;

        Integer number = (Integer) exp.evaluate(null, Integer.class);

        if (number == null) {
            return defaultWidth;
        }

        return number.intValue();
    }
}
