/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import java.util.HashSet;
import java.util.Set;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.TextSymbolizer;
import org.opengis.style.Symbolizer;

/** Collects the types of symbolizers found in the style */
public class SymbolizerTypeVisitor extends AbstractStyleVisitor {

    Set<Class<? extends Symbolizer>> symbolizerTypes = new HashSet<>();

    @Override
    public void visit(TextSymbolizer text) {
        symbolizerTypes.add(text.getClass());
    }

    @Override
    public void visit(PointSymbolizer point) {
        symbolizerTypes.add(point.getClass());
    }

    @Override
    public void visit(LineSymbolizer line) {
        symbolizerTypes.add(line.getClass());
    }

    @Override
    public void visit(PolygonSymbolizer poly) {
        symbolizerTypes.add(poly.getClass());
    }

    @Override
    public void visit(RasterSymbolizer raster) {
        symbolizerTypes.add(raster.getClass());
    }

    public Set<Class<? extends Symbolizer>> getSymbolizerTypes() {
        return symbolizerTypes;
    }
}
