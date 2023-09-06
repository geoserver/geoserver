/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import java.util.HashSet;
import java.util.Set;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.styling.AbstractStyleVisitor;

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
