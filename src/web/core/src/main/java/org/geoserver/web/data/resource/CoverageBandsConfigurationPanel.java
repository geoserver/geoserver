/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.measure.Unit;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.ReuseIfModelsEqualStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.DecimalListTextField;
import org.geoserver.web.wicket.DecimalTextField;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.coverage.SampleDimensionType;
import org.geotools.measure.UnitFormat;
import org.geotools.measure.UnitFormatter;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import si.uom.NonSI;
import si.uom.SI;

@SuppressWarnings("serial")
public class CoverageBandsConfigurationPanel extends ResourceConfigurationPanel {
    static final Logger LOGGER = Logging.getLogger(CoverageBandsConfigurationPanel.class);

    private GeoServerTablePanel<CoverageDimensionInfo> bands;

    public CoverageBandsConfigurationPanel(String id, final IModel model) {
        super(id, model);
        bands = new GeoServerTablePanel<>("bands", new CoverageDimensionsProvider(), true) {

            @Override
            protected Component getComponentForProperty(
                    String id, IModel<CoverageDimensionInfo> itemModel, Property<CoverageDimensionInfo> property) {
                if ("band".equals(property.getName())) {
                    Fragment f = new Fragment(id, "bandtext", CoverageBandsConfigurationPanel.this);
                    @SuppressWarnings("unchecked")
                    Component text = new TextField<>("bandtext", (IModel<String>) property.getModel(itemModel));
                    f.add(text);
                    return f;
                }
                if ("nullValues".equals(property.getName())) {
                    Fragment f = new Fragment(id, "nulltext", CoverageBandsConfigurationPanel.this);
                    @SuppressWarnings("unchecked")
                    Component text = new DecimalListTextField("nulltext", (IModel<List>) property.getModel(itemModel));
                    f.add(text);
                    return f;
                }
                if ("unit".equals(property.getName())) {
                    Fragment f = new Fragment(id, "text", CoverageBandsConfigurationPanel.this);
                    @SuppressWarnings("unchecked")
                    Component text = buildUnitField("text", (IModel<String>) property.getModel(itemModel));
                    f.add(text);
                    return f;
                }
                if ("minRange".equals(property.getName())) {
                    Fragment f = new Fragment(id, "minRange", CoverageBandsConfigurationPanel.this);
                    @SuppressWarnings("unchecked")
                    Component min = new DecimalTextField("minRange", (IModel<Double>) property.getModel(itemModel));
                    f.add(min);
                    return f;
                }
                if ("maxRange".equals(property.getName())) {
                    Fragment f = new Fragment(id, "maxRange", CoverageBandsConfigurationPanel.this);
                    @SuppressWarnings("unchecked")
                    Component max = new DecimalTextField("maxRange", (IModel<Double>) property.getModel(itemModel));
                    f.add(max);
                    return f;
                }

                return null;
            }
        };
        bands.setFilterVisible(false);
        bands.setSortable(false);
        bands.setPageable(false);
        bands.setOutputMarkupId(true);
        bands.setItemReuseStrategy(ReuseIfModelsEqualStrategy.getInstance());
        bands.setFilterable(false);
        bands.setSelectable(false);
        add(bands);

        GeoServerAjaxFormLink reload = new GeoServerAjaxFormLink("reload") {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                try {
                    reloadBands();
                    target.add(bands);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failure updating the bands list", e);
                    error(e.toString());
                }
            }
        };
        add(reload);
    }

    private void reloadBands() throws Exception {
        GeoServerApplication app = (GeoServerApplication) getApplication();
        CoverageInfo ci = (CoverageInfo) getResourceInfo();
        Catalog catalog = app.getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.reloadDimensions(ci);
    }

    protected Component buildUnitField(String id, IModel<String> model) {
        return new AutoCompleteTextField<>(id, model) {
            @Override
            protected Iterator<String> getChoices(String input) {
                if (Strings.isEmpty(input)) {
                    List<String> emptyList = Collections.emptyList();
                    return emptyList.iterator();
                }

                List<Unit<?>> units = new ArrayList<>();
                units.addAll(SI.getInstance().getUnits());
                units.addAll(NonSI.getInstance().getUnits());

                List<String> unitNames = new ArrayList<>();
                // adding radiance as it's the most common, but it's not part of the standard units
                unitNames.add("W.m-2.Sr-1");
                UnitFormatter format = UnitFormat.getInstance();
                for (Unit<?> unit : units) {
                    unitNames.add(format.format(unit));
                }

                List<String> choices = new ArrayList<>();
                for (String name : unitNames) {
                    if (name.toLowerCase().startsWith(input.toLowerCase())) {
                        choices.add(name);
                    }
                }

                return choices.iterator();
            }
        };
    }

    class CoverageDimensionsProvider extends GeoServerDataProvider<CoverageDimensionInfo> {

        @Override
        protected List<Property<CoverageDimensionInfo>> getProperties() {
            List<Property<CoverageDimensionInfo>> result = new ArrayList<>();
            result.add(new BeanProperty<>("band", "name"));
            result.add(new AbstractProperty<>("dimensionType") {

                @Override
                public Object getPropertyValue(CoverageDimensionInfo item) {
                    SampleDimensionType type = item.getDimensionType();
                    if (type == null) {
                        return "-";
                    } else {
                        String name = type.name();
                        try {
                            String key = CoverageBandsConfigurationPanel.class.getSimpleName() + "." + name;
                            ParamResourceModel rm = new ParamResourceModel(key, null);
                            return rm.getString();
                        } catch (Exception e) {
                            return name;
                        }
                    }
                }
            });
            result.add(new AbstractProperty<>("nullValues") {

                @Override
                public Object getPropertyValue(CoverageDimensionInfo item) {
                    return new IModel<List<Double>>() {

                        @Override
                        public List<Double> getObject() {
                            return item.getNullValues();
                        }

                        @Override
                        public void setObject(List<Double> object) {
                            List<Double> values = item.getNullValues();
                            values.clear();
                            values.addAll(object);
                        }
                    };
                }
            });
            result.add(new AbstractProperty<>("minRange") {

                @Override
                public Object getPropertyValue(final CoverageDimensionInfo item) {
                    return new IModel<Double>() {

                        @Override
                        public Double getObject() {
                            if (item.getRange() == null) {
                                return null;
                            }
                            return item.getRange().getMinimum(true);
                        }

                        @Override
                        public void setObject(Double min) {
                            if (min != null) {
                                NumberRange range = item.getRange();
                                NumberRange<Double> newRange =
                                        NumberRange.create(min, range != null ? range.getMaximum() : min);
                                item.setRange(newRange);
                            }
                        }
                    };
                }
            });
            result.add(new AbstractProperty<>("maxRange") {

                @Override
                public Object getPropertyValue(final CoverageDimensionInfo item) {
                    return new IModel<Double>() {

                        @Override
                        public Double getObject() {
                            if (item.getRange() == null) {
                                return null;
                            }
                            return item.getRange().getMaximum();
                        }

                        @Override
                        public void setObject(Double max) {
                            if (max != null) {
                                NumberRange range = item.getRange();
                                NumberRange<Double> newRange =
                                        NumberRange.create(range != null ? range.getMinimum() : max, max);
                                item.setRange(newRange);
                            }
                        }
                    };
                }
            });
            result.add(new BeanProperty<>("unit", "unit"));
            return result;
        }

        @Override
        protected List<CoverageDimensionInfo> getItems() {
            CoverageInfo ci = (CoverageInfo) CoverageBandsConfigurationPanel.this.getDefaultModelObject();
            if (ci.getDimensions() != null) {
                return ci.getDimensions();
            } else {
                return Collections.emptyList();
            }
        }
    }
}
