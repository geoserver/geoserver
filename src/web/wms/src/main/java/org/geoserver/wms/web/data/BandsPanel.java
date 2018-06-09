/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.SampleDimensionType;

/** Panel for listing band data of a CoverageInfo resource. */
@SuppressWarnings("serial")
public class BandsPanel extends Panel {
    static final Logger LOGGER = Logging.getLogger(BandsPanel.class);

    private GeoServerTablePanel<CoverageDimensionInfo> bands;

    public BandsPanel(String id, CoverageInfo coverage) {
        super(id, new Model<CoverageInfo>(coverage));

        // the parameters table
        bands =
                new GeoServerTablePanel<CoverageDimensionInfo>(
                        "bands", new CoverageDimensionsProvider(), true) {

                    @Override
                    protected GeoServerTablePanel<CoverageDimensionInfo> getComponentForProperty(
                            String id,
                            IModel<CoverageDimensionInfo> itemModel,
                            Property<CoverageDimensionInfo> property) {
                        return null;
                    }
                };
        bands.setFilterVisible(false);
        bands.setSortable(false);
        bands.getTopPager().setVisible(false);
        bands.getBottomPager().setVisible(false);
        bands.setOutputMarkupId(true);
        bands.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        bands.setFilterable(false);
        bands.setSelectable(false);
        add(bands);
    }

    class CoverageDimensionsProvider extends GeoServerDataProvider<CoverageDimensionInfo> {

        @Override
        protected List<Property<CoverageDimensionInfo>> getProperties() {
            List<Property<CoverageDimensionInfo>> result =
                    new ArrayList<Property<CoverageDimensionInfo>>();
            result.add(new BeanProperty<CoverageDimensionInfo>("band", "name"));
            result.add(
                    new AbstractProperty<CoverageDimensionInfo>("dimensionType") {

                        @Override
                        public Object getPropertyValue(CoverageDimensionInfo item) {
                            SampleDimensionType type = item.getDimensionType();
                            if (type == null) {
                                return "-";
                            } else {
                                String name = type.name();
                                try {
                                    String key = BandsPanel.class.getSimpleName() + "." + name;
                                    ParamResourceModel rm = new ParamResourceModel(key, null);
                                    return rm.getString();
                                } catch (Exception e) {
                                    return name;
                                }
                            }
                        }
                    });
            result.add(
                    new AbstractProperty<CoverageDimensionInfo>("nullValues") {

                        @Override
                        public Object getPropertyValue(CoverageDimensionInfo item) {
                            List<Double> values = item.getNullValues();
                            if (values == null || values.isEmpty()) {
                                return "-";
                            } else {
                                StringBuilder sb = new StringBuilder();
                                final int size = values.size();
                                for (int i = 0; i < size; i++) {
                                    sb.append(values.get(i));
                                    if (i < size - 1) {
                                        sb.append(", ");
                                    }
                                }

                                return sb.toString();
                            }
                        }
                    });
            result.add(new BeanProperty<CoverageDimensionInfo>("unit", "unit"));
            return result;
        }

        @Override
        protected List<CoverageDimensionInfo> getItems() {
            CoverageInfo ci = (CoverageInfo) BandsPanel.this.getDefaultModelObject();
            if (ci.getDimensions() != null) {
                return ci.getDimensions();
            } else {
                return Collections.emptyList();
            }
        }
    }
}
