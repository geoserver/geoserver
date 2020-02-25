/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.crs.ReprojectFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Encodes a set of MapLayers in HTMLImageMap format.
 *
 * @author Mauro Bartolomeoli
 */
public class EncodeHTMLImageMap extends WebMap {
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.responses.wms.map");

    /** Filter factory for creating filters */
    private static final FilterFactory filterFactory =
            CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    /** Current writer. The writer is able to encode a single feature. */
    private HTMLImageMapWriter writer;

    private final int maxFilterSize = 15;

    /**
     * Creates a new EncodeHTMLImageMap object.
     *
     * @param mapContent current wms context
     */
    public EncodeHTMLImageMap(WMSMapContent mapContent) {
        super(mapContent);
    }

    /**
     * Encodes the current set of layers.
     *
     * @param out stream to write the produced map to.
     * @throws IOException if an error occurs in encoding map
     */
    public void encode(final OutputStream out) throws IOException {
        // initializes the writer
        this.writer = new HTMLImageMapWriter(out, mapContent);

        long t = System.currentTimeMillis();

        try {
            // encodes the different layers
            writeLayers();

            this.writer.flush();
            t = System.currentTimeMillis() - t;
            LOGGER.info("HTML ImageMap generated in " + t + " ms");
        } catch (AbortedException ex) {
            return;
        }
    }

    /**
     * Applies Filters from style rules to the given query, to optimize DataStore queries. Similar
     * to the method in StreamingRenderer.
     */
    private Filter processRuleForQuery(FeatureTypeStyle[] styles) {
        try {

            // first we check to see if there are >
            // "getMaxFiltersToSendToDatastore" rules
            // if so, then we dont do anything since no matter what there's too
            // many to send down.
            // next we check for any else rules. If we find any --> dont send
            // anything to Datastore
            // next we check for rules w/o filters. If we find any --> dont send
            // anything to Datastore
            //
            // otherwise, we're gold and can "or" together all the fiters then
            // AND it with the original filter.
            // ie. SELECT * FROM ... WHERE (the_geom && BBOX) AND (filter1 OR
            // filter2 OR filter3);

            final List<Filter> filtersToDS = new ArrayList<Filter>();

            final int stylesLength = styles.length;

            FeatureTypeStyle style;

            for (int t = 0; t < stylesLength; t++) // look at each
            // featuretypestyle
            {
                style = styles[t];

                for (Rule r : style.rules()) {
                    if (r.getFilter() == null) return null; // uh-oh has no filter (want all rows)
                    if (r.isElseFilter()) return null; // uh-oh has elseRule
                    filtersToDS.add(r.getFilter());
                }
            }

            Filter ruleFiltersCombined = null;
            Filter newFilter;
            // We're GOLD -- OR together all the Rule's Filters
            if (filtersToDS.size() == 1) // special case of 1 filter
            {
                ruleFiltersCombined = filtersToDS.get(0);
                // OR all filters if they are under maxFilterSize in number, else, do not filter
            } else if (filtersToDS.size() < maxFilterSize) {
                // build it up
                ruleFiltersCombined = filtersToDS.get(0);
                final int size = filtersToDS.size();
                for (int t = 1; t < size; t++) // NOTE: dont
                // redo 1st one
                {
                    newFilter = filtersToDS.get(t);
                    ruleFiltersCombined = filterFactory.or(ruleFiltersCombined, newFilter);
                }
            }
            return ruleFiltersCombined;
            /*
            // combine with the geometry filter (preexisting)
            ruleFiltersCombined = filterFactory.or(
            		q.getFilter(), ruleFiltersCombined);

            // set the actual filter
            q.setFilter(ruleFiltersCombined);
            */
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Filters the feature type styles of <code>style</code> returning only those that apply to
     * <code>featureType</code>
     *
     * <p>This methods returns feature types for which <code>featureTypeStyle.getFeatureTypeName()
     * </code> matches the name of the feature type of <code>featureType</code>, or matches the name
     * of any parent type of the feature type of <code>featureType</code>. This method returns an
     * empty array in the case of which no rules match.
     *
     * @param style The style containing the feature type styles.
     * @param featureType The feature type being filtered against.
     */
    protected FeatureTypeStyle[] filterFeatureTypeStyles(
            Style style, SimpleFeatureType featureType) {
        List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();

        if (featureTypeStyles.isEmpty()) {
            return new FeatureTypeStyle[0];
        }

        List<FeatureTypeStyle> filtered = new ArrayList<>(featureTypeStyles.size());

        for (FeatureTypeStyle featureTypeStyle : featureTypeStyles) {
            Rule[] rules = filterRules(featureTypeStyle.rules());
            featureTypeStyle.rules().clear();
            featureTypeStyle.rules().addAll(Arrays.asList(rules));

            // does this style apply to the feature collection
            if (featureTypeStyle.featureTypeNames().isEmpty()
                    || featureTypeStyle
                            .featureTypeNames()
                            .stream()
                            .anyMatch(tn -> FeatureTypes.matches(featureType, tn))) {
                filtered.add(featureTypeStyle);
            }
        }

        return filtered.toArray(new FeatureTypeStyle[filtered.size()]);
    }

    /**
     * Evaluates if the supplied scaleDenominator is congruent with a rule defined scale range.
     *
     * @param r current rule
     * @param scaleDenominator current value to verify
     * @return true if scaleDenominator is in the rule defined range
     */
    public static boolean isWithInScale(Rule r, double scaleDenominator) {
        return ((r.getMinScaleDenominator()) <= scaleDenominator)
                && ((r.getMaxScaleDenominator()) > scaleDenominator);
    }

    /** Filter given rules, to consider only the rules compatible with the current scale. */
    private Rule[] filterRules(List<Rule> rules) {
        List<Rule> result = new ArrayList<Rule>();
        for (Rule rule : rules) {
            double scaleDenominator;
            try {
                scaleDenominator =
                        RendererUtilities.calculateScale(
                                mapContent.getRenderingArea(),
                                mapContent.getMapWidth(),
                                mapContent.getMapHeight(),
                                90);

                // is this rule within scale?
                if (EncodeHTMLImageMap.isWithInScale(rule, scaleDenominator)) {
                    result.add(rule);
                }
            } catch (TransformException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (FactoryException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // TODO Auto-generated method stub
        return result.toArray(new Rule[result.size()]);
    }

    /**
     * Encodes the current set of layers.
     *
     * @throws IOException if an error occurs during encoding
     * @throws AbortedException if the encoding is aborted
     * @task TODO: respect layer filtering given by their Styles
     */
    @SuppressWarnings("unchecked")
    private void writeLayers() throws IOException, AbortedException {
        for (Layer layer : mapContent.layers()) {
            SimpleFeatureSource fSource;
            fSource = (SimpleFeatureSource) layer.getFeatureSource();
            SimpleFeatureType schema = fSource.getSchema();
            /*FeatureSource fSource = layer.getFeatureSource();
            FeatureType schema = fSource.getSchema();*/

            try {
                ReferencedEnvelope aoi = mapContent.getRenderingArea();

                CoordinateReferenceSystem sourceCrs =
                        schema.getGeometryDescriptor().getCoordinateReferenceSystem();

                boolean reproject =
                        (sourceCrs != null)
                                && !CRS.equalsIgnoreMetadata(
                                        aoi.getCoordinateReferenceSystem(), sourceCrs);
                if (reproject) {
                    aoi = aoi.transform(sourceCrs, true);
                }
                // apply filters.
                // 1) bbox filter
                BBOX bboxFilter =
                        filterFactory.bbox(
                                schema.getGeometryDescriptor().getLocalName(),
                                aoi.getMinX(),
                                aoi.getMinY(),
                                aoi.getMaxX(),
                                aoi.getMaxY(),
                                null);
                Query q = new Query(schema.getTypeName(), bboxFilter);

                String mapId = null;

                mapId = schema.getTypeName();

                writer.write("<map name=\"" + mapId + "\">\n");

                // 2) definition query filter
                Query definitionQuery = layer.getQuery();
                LOGGER.info("Definition Query: " + definitionQuery.toString());
                if (!definitionQuery.equals(Query.ALL)) {
                    if (q.equals(Query.ALL)) {
                        q = (Query) definitionQuery;
                    } else {
                        q =
                                (Query)
                                        DataUtilities.mixQueries(
                                                definitionQuery, q, "HTMLImageMapEncoder");
                    }
                }

                FeatureTypeStyle[] ftsList =
                        filterFeatureTypeStyles(layer.getStyle(), fSource.getSchema());
                // 3) rule filters
                Filter ruleFilter = processRuleForQuery(ftsList);
                if (ruleFilter != null) {
                    // combine with the geometry filter (preexisting)
                    ruleFilter = filterFactory.and(q.getFilter(), ruleFilter);

                    // set the actual filter
                    // q.setFilter(ruleFilter);
                    q = new Query(schema.getTypeName(), ruleFilter);
                    // q = (Query) DataUtilities.mixQueries(new
                    // Query(schema.getTypeName(),ruleFilter), q, "HTMLImageMapEncoder");
                }
                // ensure reprojection occurs, do not trust query, use the wrapper
                SimpleFeatureCollection fColl = null; // fSource.getFeatures(q);
                // FeatureCollection fColl=null;
                if (reproject) {
                    fColl =
                            new ReprojectFeatureResults(
                                    fSource.getFeatures(q),
                                    mapContent.getCoordinateReferenceSystem());
                } else fColl = fSource.getFeatures(q);

                // encodes the current layer, using the defined style
                writer.writeFeatures(fColl, ftsList);
                writer.write("</map>\n");

            } catch (IOException ex) {
                throw ex;
            } catch (AbortedException ae) {
                LOGGER.info("process aborted: " + ae.getMessage());
                throw ae;
            } catch (Throwable t) {
                LOGGER.warning("UNCAUGHT exception: " + t.getMessage());

                IOException ioe = new IOException("UNCAUGHT exception: " + t.getMessage());
                ioe.setStackTrace(t.getStackTrace());
                throw ioe;
            }
        }
    }
}
