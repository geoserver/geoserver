/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.ArrayList;
import java.util.List;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/** GeoServerSLDVisitor for collecting assorted validation errors and reporting them */
public class SLDNamedLayerValidator extends GeoServerSLDVisitorAdapter {

    public List<Exception> validationErrors = new ArrayList<>();

    public SLDNamedLayerValidator(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
        super(catalog, fallbackCrs);
    }

    public List<Exception> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public void visit(StyledLayerDescriptor sld) {
        try {
            super.visit(sld);
        } catch (Exception e) {
            validationErrors.add(e);
        }
    }

    @Override
    public PublishedInfo visitNamedLayerInternal(StyledLayer namedLayer) {
        PublishedInfo p = catalog.getLayerGroupByName(namedLayer.getName());
        if (p == null) {
            p = catalog.getLayerByName(namedLayer.getName());
        }
        if (p == null) {
            validationErrors.add(
                    new Exception(
                            "No layer or layer group named '"
                                    + namedLayer.getName()
                                    + "' found in the catalog"));
        }
        return p;
    }

    @Override
    public StyleInfo visitNamedStyleInternal(NamedStyle namedStyle) {
        StyleInfo s = catalog.getStyleByName(namedStyle.getName());
        if (s == null) {
            validationErrors.add(
                    new Exception(
                            "No style named '" + namedStyle.getName() + "' found in the catalog"));
        }
        return s;
    }

    public static List<Exception> validate(Catalog catalog, StyledLayerDescriptor sld) {
        SLDNamedLayerValidator validator = new SLDNamedLayerValidator(catalog, null);
        sld.accept(validator);
        return validator.getValidationErrors();
    }
}
