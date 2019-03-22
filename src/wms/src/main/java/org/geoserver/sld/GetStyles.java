/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sld;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMS;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;

/**
 * DescribeLayer WMs operation.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetStyles {

    private final WMS wms;

    public GetStyles(WMS wms) {
        this.wms = wms;
    }

    public StyledLayerDescriptor run(final GetStylesRequest request) throws ServiceException {

        if (request.getSldVer() != null
                && "".equals(request.getSldVer())
                && !"1.0.0".equals(request.getSldVer()))
            throw new ServiceException("SLD version " + request.getSldVer() + " not supported");

        try {
            StyleFactory factory = CommonFactoryFinder.getStyleFactory(null);
            List<StyledLayer> layers = new ArrayList<StyledLayer>();
            for (String layerName : request.getLayers()) {
                NamedLayer namedLayer = factory.createNamedLayer();
                layers.add(namedLayer);
                namedLayer.setName(layerName);
                LayerGroupInfo group = wms.getLayerGroupByName(layerName);
                LayerInfo layer = wms.getLayerByName(layerName);
                if (group == null) {
                    // groups have no style, check other cases
                    if (layer != null) {
                        Style style = layer.getDefaultStyle().getStyle();
                        // add the default style first
                        style = cloneStyle(style);
                        style.setDefault(true);
                        style.setName(layer.getDefaultStyle().getName());
                        namedLayer.styles().add(style);
                        // add alternate styles
                        for (StyleInfo si : layer.getStyles()) {
                            style = cloneStyle(si.getStyle());
                            style.setName(si.getName());
                            namedLayer.styles().add(style);
                        }
                    } else {
                        // we should really add a code and a locator...
                        throw new ServiceException("Unknown layer " + layerName);
                    }
                }
            }

            StyledLayerDescriptor sld = factory.createStyledLayerDescriptor();
            sld.setStyledLayers((StyledLayer[]) layers.toArray(new StyledLayer[layers.size()]));

            return sld;
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    private Style cloneStyle(Style style) {
        DuplicatingStyleVisitor cloner = new DuplicatingStyleVisitor();
        style.accept(cloner);
        style = (Style) cloner.getCopy();
        return style;
    }
}
