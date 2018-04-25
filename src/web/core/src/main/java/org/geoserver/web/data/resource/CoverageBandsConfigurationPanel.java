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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import si.uom.NonSI;
import si.uom.SI;
import tec.uom.se.format.SimpleUnitFormat;

import javax.measure.Unit;
import javax.measure.format.UnitFormat;

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
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.DecimalListTextField;
import org.geoserver.web.wicket.DecimalTextField;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.GeoTools;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.SampleDimensionType;

@SuppressWarnings("serial")
public class CoverageBandsConfigurationPanel extends ResourceConfigurationPanel {
    static final Logger LOGGER = Logging.getLogger(CoverageBandsConfigurationPanel.class);

    private GeoServerTablePanel<CoverageDimensionInfo> bands;


    public CoverageBandsConfigurationPanel(String id, final IModel model) {
        super(id, model);
        bands = new GeoServerTablePanel<CoverageDimensionInfo>("bands",
                new CoverageDimensionsProvider(), true) {

            @Override
            protected Component getComponentForProperty(String id, IModel<CoverageDimensionInfo> itemModel, 
                Property<CoverageDimensionInfo> property) {
                if ("band".equals(property.getName())) {
                    Fragment f = new Fragment(id, "bandtext", CoverageBandsConfigurationPanel.this);
                    Component text = new TextField<>("bandtext", (IModel<String>) property.getModel(itemModel));
                    f.add(text);
                    return f;
                }
                if ("nullValues".equals(property.getName())) {
                    Fragment f = new Fragment(id, "nulltext", CoverageBandsConfigurationPanel.this);
                    Component text = new DecimalListTextField("nulltext", (IModel<List>) property.getModel(itemModel));
                    f.add(text);
                    return f;
                }
                if ("unit".equals(property.getName())) {
                    Fragment f = new Fragment(id, "text", CoverageBandsConfigurationPanel.this);
                    Component text = buildUnitField("text", property.getModel(itemModel));
                    f.add(text);
                    return f;
                }
               if ("minRange".equals(property.getName())) {
                    Fragment f = new Fragment(id, "minRange", CoverageBandsConfigurationPanel.this);
                    Component min = new DecimalTextField("minRange", (IModel<Double>) property.getModel(itemModel));
                    f.add(min);
                    return f;
                }
                if ("maxRange".equals(property.getName())) {
                    Fragment f = new Fragment(id, "maxRange", CoverageBandsConfigurationPanel.this);
                    Component max = new DecimalTextField("maxRange", (IModel<Double>) property.getModel(itemModel));                    
                    f.add(max);
                    return f;
                }

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

        GeoServerAjaxFormLink reload = new GeoServerAjaxFormLink("reload") {
            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                GeoServerApplication app = (GeoServerApplication) getApplication();

                try {
                    CoverageInfo ci = (CoverageInfo) getResourceInfo();
                    String nativeName = ci.getNativeCoverageName();
                    Catalog catalog = app.getCatalog();
                    CatalogBuilder cb = new CatalogBuilder(catalog);
                    cb.setStore(ci.getStore());
                    MetadataMap metadata = ci.getMetadata();
                    CoverageInfo rebuilt = null;
                    if (metadata != null && metadata.containsKey(CoverageView.COVERAGE_VIEW)) {
                        GridCoverage2DReader reader = (GridCoverage2DReader) catalog.getResourcePool().getGridCoverageReader(ci, nativeName, GeoTools.getDefaultHints());
                        rebuilt = cb.buildCoverage(reader, nativeName, null);    
                    } else {
                        rebuilt = cb.buildCoverage(nativeName);
                    }
                    ci.getDimensions().clear();
                    ci.getDimensions().addAll(rebuilt.getDimensions());
                    target.add(bands);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failure updating the bands list", e);
                    error(e.toString());
                }
            }
        };
        add(reload);
    }

    protected Component buildUnitField(String id, IModel model) {
        return new AutoCompleteTextField<String>(id, model) {
            @Override
            protected Iterator<String> getChoices(String input) {
                if (Strings.isEmpty(input)) {
                    List<String> emptyList = Collections.emptyList();
                    return emptyList.iterator();
                }

                List<Unit<?>> units = new ArrayList<Unit<?>>();
                units.addAll(SI.getInstance().getUnits());
                units.addAll(NonSI.getInstance().getUnits());

                List<String> unitNames = new ArrayList<String>();
                // adding radiance as it's the most common, but it's not part of the standard units
                unitNames.add("W.m-2.Sr-1");
                UnitFormat format = SimpleUnitFormat.getInstance();
                for (Unit<?> unit : units) {
                    unitNames.add(format.format(unit));
                }

                List<String> choices = new ArrayList<String>();
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
            List<Property<CoverageDimensionInfo>> result = new ArrayList<Property<CoverageDimensionInfo>>();
            result.add(new BeanProperty<CoverageDimensionInfo>("band", "name"));
            result.add(new AbstractProperty<CoverageDimensionInfo>("dimensionType") {

                @Override
                public Object getPropertyValue(CoverageDimensionInfo item) {
                    SampleDimensionType type = item.getDimensionType();
                    if (type == null) {
                        return "-";
                    } else {
                        String name = type.name();
                        try {
                            String key = CoverageBandsConfigurationPanel.class.getSimpleName() + "." +  name;
                            ParamResourceModel rm = new ParamResourceModel(key, null);
                            return rm.getString();
                        } catch(Exception e) {
                            return name;
                        }
                        
                    }
                }

            });
            result.add(new AbstractProperty<CoverageDimensionInfo>("nullValues") {

                @Override
                public Object getPropertyValue(CoverageDimensionInfo item) {
                    return new IModel<List<Double>>() {

                        @Override
                        public void detach() {
                            // TODO Auto-generated method stub
                            
                        }

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
            result.add(new AbstractProperty<CoverageDimensionInfo>("minRange") {

                @Override
                public Object getPropertyValue(final CoverageDimensionInfo item) {
                    return new IModel<Double>() {

                        @Override
                        public void detach() {
                            // nothing to do
                        }

                        @Override
                        public Double getObject() {
                            if(item.getRange() == null) {
                                return null;
                            }
                            return item.getRange().getMinimum(true);
                        }

                        @Override
                        public void setObject(Double min) {
                            if(min != null) {
                                NumberRange range = item.getRange();
                                NumberRange<Double> newRange = NumberRange.create(min, range != null ? range.getMaximum() : min);
                                item.setRange(newRange);
                            }
                        }
                    };
                }

            });
            result.add(new AbstractProperty<CoverageDimensionInfo>("maxRange") {

                @Override
                public Object getPropertyValue(final CoverageDimensionInfo item) {
                    return new IModel<Double>() {

                        @Override
                        public void detach() {
                            // nothing to do
                        }

                        @Override
                        public Double getObject() {
                            if(item.getRange() == null) {
                                return null;
                            }
                            return item.getRange().getMaximum();
                        }

                        @Override
                        public void setObject(Double max) {
                            if(max != null) {
                                NumberRange range = item.getRange();
                                NumberRange<Double> newRange = NumberRange.create(range != null ? range.getMinimum() : max, max);
                                item.setRange(newRange);
                            }
                        }
                    };
                }

            });
            result.add(new BeanProperty<CoverageDimensionInfo>("unit", "unit"));
            return result;
        }

        @Override
        protected List<CoverageDimensionInfo> getItems() {
            CoverageInfo ci = (CoverageInfo) CoverageBandsConfigurationPanel.this
                    .getDefaultModelObject();
            if (ci.getDimensions() != null) {
                return ci.getDimensions();
            } else {
                return Collections.emptyList();
            }
        }
    }
}
