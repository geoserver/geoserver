package org.geoserver.sldservice.rest;

import javax.xml.transform.TransformerException;
import org.geoserver.catalog.Catalog;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;

public abstract class BaseSLDServiceController extends AbstractCatalogController {
    protected static final StyleFactory SF = CommonFactoryFinder.getStyleFactory();

    public BaseSLDServiceController(Catalog catalog) {
        super(catalog);
    }

    protected String sldAsString(StyledLayerDescriptor sld) throws TransformerException {
        SLDTransformer transform = new SLDTransformer();
        transform.setIndentation(2);
        return transform.transform(sld);
    }
}
