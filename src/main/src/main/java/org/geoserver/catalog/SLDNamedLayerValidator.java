package org.geoserver.catalog;

import org.geoserver.platform.ServiceException;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tbarsballe on 2017-08-21.
 */
public class SLDNamedLayerValidator extends SLDVisitorAdapter {

    public List<Exception> validationErrors = new ArrayList<>();

    public SLDNamedLayerValidator(Catalog catalog, CoordinateReferenceSystem fallbackCrs) {
        super(catalog, fallbackCrs);
    }

    public List<Exception> getValidationErrors() {
        return validationErrors;
    }

    @Override
    public SLDNamedLayerValidator apply(StyledLayerDescriptor sld) {
        try {
            super.apply(sld);
        } catch (Exception e) {
            validationErrors.add(e);
        }
        return this;
    }

    @Override
    public PublishedInfo visitNamedLayer(StyledLayer namedLayer) {
        PublishedInfo p =  catalog.getLayerGroupByName(namedLayer.getName());
        if (p == null) {
            p = catalog.getLayerByName(namedLayer.getName());
        }
        if (p == null) {
            validationErrors.add(new Exception("No layer or layer group named '" + namedLayer.getName()+ "' found in the catalog"));
        }
        return p;
    }

    @Override
    public Style visitNamedStyle(StyledLayer layer, NamedStyle namedStyle, LayerInfo info) throws IOException {
        StyleInfo s = catalog.getStyleByName(namedStyle.getName());
        if (s == null) {
            validationErrors.add(new Exception("No style named '" + namedStyle.getName()+ "' found in the catalog"));
        }
        return s.getStyle();
    }

    public static List<Exception> validate(Catalog catalog, StyledLayerDescriptor sld) {
        return new SLDNamedLayerValidator(catalog, null).apply(sld).getValidationErrors();
    }
}
