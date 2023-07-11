/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.CoverageReadingTransformation;
import org.geotools.feature.FeatureTypes;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.ImageOutline;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.OverlapBehavior;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.ShadedRelief;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.UserLayer;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.parameter.Parameter;

/**
 * Extracts the active raster symbolizers, as long as there are some, and only raster symbolizers
 * are available, without rendering transformations in place. In the case of mixed symbolizers it
 * will return null TODO: extend this class so that it handles the case of other symbolizers applied
 * after a raster symbolizer one (e.g., to draw a rectangle around a coverage)
 *
 * @author Andrea Aime
 */
public class RasterSymbolizerVisitor implements StyleVisitor {

    /**
     * Boolean value allowing to control whether rendering transformations should be evaluated when
     * performing WMS GetFeatureInfo requests. This option applies only to transformations with a
     * raster source (i.e., raster-to-raster and raster-to-vector). The default value can be
     * configured by administrators in the global and workspace-specific WMS service settings
     * (transformations will be evaluated by default) and this SLD vendor option can be used to
     * override the service setting for a specific FeatureTypeStyle element within the SLD document.
     */
    public static final String TRANSFORM_FEATURE_INFO = "transformFeatureInfo";

    double scaleDenominator;

    FeatureType featureType;

    List<RasterSymbolizer> symbolizers = new ArrayList<>();

    boolean otherSymbolizers = false;

    Expression rasterTransformation = null;

    CoverageReadingTransformation coverageReadingTransformation = null;

    List<Expression> otherRenderingTransformations = new ArrayList<>();

    Boolean transformFeatureInfo = null;

    public RasterSymbolizerVisitor(double scaleDenominator, FeatureType featureType) {
        this(scaleDenominator, featureType, null);
    }

    public RasterSymbolizerVisitor(
            double scaleDenominator, FeatureType featureType, Boolean transformFeatureInfo) {
        this.scaleDenominator = scaleDenominator;
        this.featureType = featureType;
        this.transformFeatureInfo = transformFeatureInfo;
    }

    public void reset() {
        symbolizers.clear();
        otherSymbolizers = false;
        rasterTransformation = null;
        otherRenderingTransformations = new ArrayList<>();
    }

    public List<RasterSymbolizer> getRasterSymbolizers() {
        if (otherSymbolizers || !otherRenderingTransformations.isEmpty())
            return Collections.emptyList();
        else return symbolizers;
    }

    public Expression getRasterRenderingTransformation() {
        return rasterTransformation;
    }

    public CoverageReadingTransformation getCoverageReadingTransformation() {
        return coverageReadingTransformation;
    }

    public List<Expression> getOtherRenderingTransformations() {
        return otherRenderingTransformations;
    }

    @Override
    public void visit(StyledLayerDescriptor sld) {
        for (StyledLayer sl : sld.getStyledLayers()) {
            if (sl instanceof UserLayer) {
                ((UserLayer) sl).accept(this);
            } else if (sl instanceof NamedLayer) {
                ((NamedLayer) sl).accept(this);
            }
        }
    }

    @Override
    public void visit(NamedLayer layer) {
        for (Style s : layer.getStyles()) s.accept(this);
    }

    @Override
    public void visit(UserLayer layer) {
        for (Style s : layer.getUserStyles()) s.accept(this);
    }

    @Override
    public void visit(FeatureTypeConstraint ftc) {
        // nothing to do
    }

    @Override
    public void visit(Style style) {
        for (FeatureTypeStyle fts : style.featureTypeStyles()) {
            fts.accept(this);
        }
    }

    @Override
    public void visit(Rule rule) {
        if (rule.getMinScaleDenominator() < scaleDenominator
                && rule.getMaxScaleDenominator() > scaleDenominator) {
            for (Symbolizer s : rule.symbolizers()) s.accept(this);
        }
    }

    @Override
    public void visit(FeatureTypeStyle fts) {
        // use the same logic as streaming renderer to decide if a fts is active
        if (featureType == null
                || (featureType.getName().getLocalPart() != null)
                        && (fts.featureTypeNames().isEmpty()
                                || fts.featureTypeNames().stream()
                                        .anyMatch(tn -> FeatureTypes.matches(featureType, tn)))) {
            if (activeRules(fts)) {
                Expression tx = isTransformFeatureInfo(fts) ? fts.getTransformation() : null;
                if (tx != null) {
                    boolean rasterTransformation = false;
                    if (tx instanceof CoverageReadingTransformation)
                        this.coverageReadingTransformation = (CoverageReadingTransformation) tx;
                    else if (tx instanceof Function) {
                        rasterTransformation = isRasterTransformation(tx, rasterTransformation);
                    }
                    if (!rasterTransformation) otherRenderingTransformations.add(tx);
                }
                for (Rule r : fts.rules()) {
                    r.accept(this);
                }
            }
        }
    }

    private boolean isTransformFeatureInfo(FeatureTypeStyle fts) {
        // ignore this setting if not a GetFeatureInfo request
        if (this.transformFeatureInfo == null) {
            return true;
        }
        // check the vendor option first, then the WMS settings
        String option = fts.getOptions().get(TRANSFORM_FEATURE_INFO);
        return option != null ? Boolean.parseBoolean(option) : this.transformFeatureInfo;
    }

    private boolean activeRules(FeatureTypeStyle fts) {
        for (Rule rule : fts.rules()) {
            if (rule.getMinScaleDenominator() < scaleDenominator
                    && rule.getMaxScaleDenominator() > scaleDenominator) return true;
        }

        return false;
    }

    private boolean isRasterTransformation(Expression tx, boolean rasterTransformation) {
        Function f = (Function) tx;
        FunctionName name = f.getFunctionName();
        if (name != null) {
            Parameter<?> result = name.getReturn();
            if (result != null) {
                if (GridCoverage2D.class.isAssignableFrom(result.getType())) {
                    rasterTransformation = true;
                    this.rasterTransformation = tx;
                }
            }
        }
        return rasterTransformation;
    }

    @Override
    public void visit(Fill fill) {
        // nothing to do

    }

    @Override
    public void visit(Stroke stroke) {
        // nothing to do
    }

    @Override
    public void visit(Symbolizer sym) {
        if (sym instanceof RasterSymbolizer) {
            visit((RasterSymbolizer) sym);
        } else {
            otherSymbolizers = true;
        }
    }

    @Override
    public void visit(PointSymbolizer ps) {
        otherSymbolizers = true;
    }

    @Override
    public void visit(LineSymbolizer line) {
        otherSymbolizers = true;
    }

    @Override
    public void visit(PolygonSymbolizer poly) {
        otherSymbolizers = true;
    }

    @Override
    public void visit(TextSymbolizer text) {
        otherSymbolizers = true;
    }

    @Override
    public void visit(RasterSymbolizer raster) {
        this.symbolizers.add(raster);
    }

    @Override
    public void visit(Graphic gr) {
        // nothing to do

    }

    @Override
    public void visit(Mark mark) {
        // nothing to do

    }

    @Override
    public void visit(ExternalGraphic exgr) {
        // nothing to do

    }

    @Override
    public void visit(PointPlacement pp) {
        // nothing to do

    }

    @Override
    public void visit(AnchorPoint ap) {
        // nothing to do
    }

    @Override
    public void visit(Displacement dis) {
        // nothing to do
    }

    @Override
    public void visit(LinePlacement lp) {
        // nothing to do
    }

    @Override
    public void visit(Halo halo) {
        // nothing to do
    }

    @Override
    public void visit(ColorMap colorMap) {
        // nothing to do
    }

    @Override
    public void visit(ColorMapEntry colorMapEntry) {
        // nothing to do
    }

    @Override
    public void visit(ContrastEnhancement contrastEnhancement) {
        // nothing to do
    }

    @Override
    public void visit(ImageOutline outline) {
        // nothing to do
    }

    @Override
    public void visit(ChannelSelection cs) {
        // nothing to do
    }

    @Override
    public void visit(OverlapBehavior ob) {
        // nothing to do
    }

    @Override
    public void visit(SelectedChannelType sct) {
        // nothing to do
    }

    @Override
    public void visit(ShadedRelief sr) {
        // nothing to do
    }
}
