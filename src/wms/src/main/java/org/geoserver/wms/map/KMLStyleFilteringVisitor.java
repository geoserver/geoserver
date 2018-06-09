/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import org.geoserver.wms.SymbolizerFilteringVisitor;
import org.geotools.styling.PointSymbolizer;

/**
 * A style visitor that copies styles but removes all point and text symbolizers
 *
 * @author Andrea Aime - OpenGeo
 */
public class KMLStyleFilteringVisitor extends SymbolizerFilteringVisitor {

    public void visit(PointSymbolizer ps) {
        pages.push(null);
    }

    public void visit(org.geotools.styling.TextSymbolizer ts) {
        pages.push(null);
    }
}
