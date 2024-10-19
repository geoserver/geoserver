/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat.web;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.Row;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.rat.CoverageRATs;
import org.geoserver.rat.RasterAttributeTable;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geotools.api.style.Style;
import org.geotools.util.logging.Logging;

public class RasterAttributeTableConfig extends PublishedConfigurationPanel<LayerInfo> {

    static final Logger LOGGER = Logging.getLogger(RasterAttributeTableConfig.class);
    private final CoverageRATs rats;
    private String name;
    private TextField<String> styleName;
    private List<String> classificationList;
    private DropDownChoice<String> classifications;
    private DropDownChoice<Integer> bandsSelector;
    private GeoServerTablePanel<Row> table;
    private RowProvider provider;

    public RasterAttributeTableConfig(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);

        // see if there is any attribute table
        final CoverageInfo coverage = (CoverageInfo) getPublishedInfo().getResource();
        rats = new CoverageRATs(GeoServerApplication.get().getCatalog(), coverage);
        PAMDataset dataset = rats.getPAMDataset();
        List<Integer> bands = getBandsWithRATs(dataset);
        if (bands.isEmpty()) {
            this.setVisible(false);
            return;
        }

        buildStyleToolar(bands, coverage);

        provider = new RowProvider(dataset, bands.get(0));
        table =
                new GeoServerTablePanel<>("rat", provider, true) {

                    @Override
                    protected Component getComponentForProperty(
                            String id,
                            IModel<Row> itemModel,
                            GeoServerDataProvider.Property<Row> property) {
                        return null;
                    }
                };
        table.setSelectable(false);
        table.setOutputMarkupId(true);
        add(table);
    }

    private void buildStyleToolar(List<Integer> bands, CoverageInfo coverage) {
        WebMarkupContainer styleToolbar = new WebMarkupContainer("styleToolbar");
        styleToolbar.setOutputMarkupId(true);
        add(styleToolbar);

        Button create =
                new AjaxButton("createStyles") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        createStyle();
                        target.add(styleToolbar);
                        Page page = getPage();
                        if (page instanceof GeoServerBasePage)
                            ((GeoServerBasePage) page).addFeedbackPanels(target);
                        page.visitChildren(
                                (c, v) -> {
                                    if (c.getClass().getSimpleName().equals("WMSLayerConfig")) {
                                        target.add(c);
                                    }
                                });
                    }
                };
        styleToolbar.add(create);
        create.setEnabled(false);

        bandsSelector =
                new DropDownChoice<>("bands", new Model<>(bands.get(0)), bands, new BandRenderer());
        bandsSelector.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Integer band = bandsSelector.getModelObject();
                        provider.setBand(band);
                        List<String> classifications =
                                new ArrayList<>(
                                        rats.getRasterAttributeTable(band).getClassifications());
                        Collections.sort(classifications);
                        classificationList.clear();
                        classificationList.addAll(classifications);

                        target.add(styleToolbar);
                    }
                });
        styleToolbar.add(bandsSelector);

        this.classificationList =
                new ArrayList<>(rats.getRasterAttributeTable(bands.get(0)).getClassifications());
        Collections.sort(classificationList);
        this.classifications =
                new DropDownChoice<>("classifications", new Model<>(), classificationList);
        classifications.add(
                new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        name =
                                rats.getDefaultStyleName(
                                        bandsSelector.getModelObject(),
                                        classifications.getModelObject());
                        create.setEnabled(true);
                        ajaxRequestTarget.add(styleToolbar);
                    }
                });
        styleToolbar.add(classifications);

        this.styleName = new TextField<>("styleName", new PropertyModel<>(this, "name"));
        styleToolbar.add(styleName);
    }

    private void createStyle() {
        Integer band = bandsSelector.getModelObject();
        RasterAttributeTable rat = rats.getRasterAttributeTable(band);
        if (rat == null) {
            error("No RAT found for band " + band);
            return;
        }

        String classification = classifications.getModelObject();
        if (classification == null) {
            error("No classification selected");
            return;
        }
        Style style = rat.classify(classification);
        if (style != null) {
            try {
                StyleInfo si = rats.saveStyle(style, name);

                Set<StyleInfo> styles = getPublishedInfo().getStyles();
                if (styles.contains(si)) {
                    info("Updated style " + si.prefixedName());
                } else {
                    getPublishedInfo().getStyles().add(si);
                    info("Created style " + si.prefixedName());
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to save style " + name, e);
                error(
                        "Failed to save style for band "
                                + band
                                + " and classification "
                                + classification
                                + ": "
                                + e.getMessage());
            }
        }
    }

    private List<Integer> getBandsWithRATs(PAMDataset dataset) {
        if (dataset == null) return Collections.emptyList();
        List<PAMDataset.PAMRasterBand> bands = dataset.getPAMRasterBand();
        if (bands == null) return Collections.emptyList();
        return bands.stream()
                .filter(b -> b.getGdalRasterAttributeTable() != null)
                .map(b -> b.getBand() - 1) // convert to 0-based
                .collect(Collectors.toList());
    }

    private static class BandRenderer implements IChoiceRenderer<Integer> {
        @Override
        public Object getDisplayValue(Integer integer) {
            return integer + 1;
        }

        @Override
        public String getIdValue(Integer integer, int i) {
            return String.valueOf(integer);
        }

        @Override
        public Integer getObject(String s, IModel<? extends List<? extends Integer>> iModel) {
            return Integer.parseInt(s);
        }
    }
}
