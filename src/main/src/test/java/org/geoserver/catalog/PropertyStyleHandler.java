/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.UserLayer;
import org.geotools.util.Version;
import org.opengis.filter.FilterFactory;
import org.xml.sax.EntityResolver;

/** Test style handler based on properties format. */
public class PropertyStyleHandler extends StyleHandler {

    public static final String FORMAT = "psl";
    public static final String MIMETYPE = "application/prs.gs.psl";

    StyleFactory styleFactory;
    FilterFactory filterFactory;

    public PropertyStyleHandler() {
        super("Property", FORMAT);
        styleFactory = CommonFactoryFinder.getStyleFactory();
        filterFactory = CommonFactoryFinder.getFilterFactory();
    }

    @Override
    public String getFileExtension() {
        return "properties";
    }

    @Override
    public String mimeType(Version version) {
        return MIMETYPE;
    }

    @Override
    public StyledLayerDescriptor parse(
            Object input,
            Version version,
            ResourceLocator resourceLocator,
            EntityResolver enityResolver)
            throws IOException {
        Properties p = new Properties();
        try (Reader reader = toReader(input)) {
            p.load(reader);
        }

        Color color = color(p.getProperty("color"), Color.BLACK);
        Symbolizer sym = null;

        String type = p.getProperty("type");
        if ("line".equalsIgnoreCase(type)) {
            LineSymbolizer ls = styleFactory.createLineSymbolizer();
            ls.setStroke(
                    styleFactory.createStroke(
                            filterFactory.literal(color), filterFactory.literal(2)));

            sym = ls;
        } else if ("polygon".equalsIgnoreCase(type)) {
            PolygonSymbolizer ps = styleFactory.createPolygonSymbolizer();
            ps.setFill(styleFactory.createFill(filterFactory.literal(color)));

            sym = ps;
        } else if ("raster".equalsIgnoreCase(type)) {
            RasterSymbolizer rs = styleFactory.createRasterSymbolizer();
            sym = rs;
        } else {
            Mark mark = styleFactory.createMark();
            mark.setFill(styleFactory.createFill(filterFactory.literal(color)));

            PointSymbolizer ps = styleFactory.createPointSymbolizer();
            ps.setGraphic(styleFactory.createDefaultGraphic());
            ps.getGraphic().graphicalSymbols().add(mark);

            sym = ps;
        }

        Rule r = styleFactory.createRule();
        r.symbolizers().add(sym);

        FeatureTypeStyle fts = styleFactory.createFeatureTypeStyle();
        fts.rules().add(r);

        Style s = styleFactory.createStyle();
        s.featureTypeStyles().add(fts);

        UserLayer l = styleFactory.createUserLayer();
        l.userStyles().add(s);

        StyledLayerDescriptor sld = styleFactory.createStyledLayerDescriptor();
        sld.layers().add(l);
        return sld;
    }

    Color color(String color, Color def) {
        if (color == null) {
            return def;
        }

        return new Color(
                Integer.valueOf(color.substring(0, 2), 16),
                Integer.valueOf(color.substring(2, 4), 16),
                Integer.valueOf(color.substring(4, 6), 16));
    }

    @Override
    public void encode(
            StyledLayerDescriptor sld, Version version, boolean pretty, OutputStream output)
            throws IOException {
        Properties props = new Properties();
        for (Symbolizer sym : SLD.symbolizers(Styles.style(sld))) {
            if (sym instanceof PointSymbolizer) {
                props.put("type", "point");
            } else if (sym instanceof LineSymbolizer) {
                props.put("type", "line");
            } else if (sym instanceof PolygonSymbolizer) {
                props.put("type", "polygon");
            } else if (sym instanceof RasterSymbolizer) {
                props.put("type", "raster");
            }
        }

        props.store(output, null);
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {
        return Collections.emptyList();
    }

    @Override
    public boolean supportsEncoding(Version version) {
        return true;
    }
}
