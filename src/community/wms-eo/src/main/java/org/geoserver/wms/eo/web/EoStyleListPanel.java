/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layergroup.StyleListPanel;
import org.geoserver.wms.eo.EoLayerType;
import org.geoserver.wms.eo.EoStyles;

/**
 * A style picker that takes into account which styles make sense for specific types of layers in a
 * EO group
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class EoStyleListPanel extends StyleListPanel {

    /** */
    private static final long serialVersionUID = -8296533423160059140L;

    public EoStyleListPanel(String contentId, final EoLayerType layerType) {
        super(
                contentId,
                new StyleListProvider() {
                    private static final long serialVersionUID = -6645387722215242978L;

                    @Override
                    protected List<StyleInfo> getItems() {
                        Catalog catalog = GeoServerApplication.get().getCatalog();
                        if (layerType == EoLayerType.BITMASK
                                || layerType == EoLayerType.COVERAGE_OUTLINE) {
                            List<StyleInfo> styles = new ArrayList<StyleInfo>();
                            for (String name : EoStyles.EO_STYLE_NAMES) {
                                StyleInfo si = catalog.getStyleByName(name);
                                if (si != null) {
                                    styles.add(si);
                                }
                            }

                            return styles;
                        } else {
                            // TODO: limit the styles to the ones in the current workspace and
                            // global ones
                            return catalog.getStyles();
                        }
                    }
                });
    }
}
