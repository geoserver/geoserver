/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.publish;

import static org.geoserver.wcs.responses.AscCoverageResponseDelegate.ARCGRID_COVERAGE_FORMAT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.publish.PublishedConfigurationPanel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SimpleChoiceRenderer;

/** A configuration panel for CoverageInfo properties that related to WCS publication */
public class WCSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    private static final long serialVersionUID = 6120092654147588736L;

    private static final List<String> WCS_FORMATS =
            Arrays.asList(
                    "GIF",
                    "PNG",
                    "JPEG",
                    "TIFF",
                    "GEOTIFF",
                    "IMAGEMOSAIC",
                    ARCGRID_COVERAGE_FORMAT);
    private static final List<String> INTERPOLATIONS =
            Arrays.asList("nearest neighbor", "bilinear", "bicubic");

    private List<String> selectedRequestSRSs;
    private List<String> selectedResponseSRSs;
    private String newRequestSRS;
    private String newResponseSRS;

    public WCSLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        final CoverageInfo coverage = (CoverageInfo) getPublishedInfo().getResource();
        add(
                new ListMultipleChoice<String>(
                        "requestSRS",
                        new PropertyModel<List<String>>(this, "selectedRequestSRSs"),
                        coverage.getRequestSRS()));

        add(
                new TextField<String>(
                        "newRequestSRS", new PropertyModel<String>(this, "newRequestSRS")));

        add(
                new Button("deleteSelectedRequestSRSs") {
                    private static final long serialVersionUID = 8363252127939759315L;

                    public void onSubmit() {
                        coverage.getRequestSRS().removeAll(selectedRequestSRSs);
                        selectedRequestSRSs.clear();
                    }
                });

        add(
                new Button("addNewRequestSRS") {
                    private static final long serialVersionUID = -3493317500980471055L;

                    public void onSubmit() {
                        coverage.getRequestSRS().add(newRequestSRS);
                        newRequestSRS = "";
                    }
                });

        add(
                new ListMultipleChoice<String>(
                        "responseSRS",
                        new PropertyModel<List<String>>(this, "selectedResponseSRSs"),
                        coverage.getResponseSRS()));

        add(
                new TextField<String>(
                        "newResponseSRS", new PropertyModel<String>(this, "newResponseSRS")));

        add(
                new Button("deleteSelectedResponseSRSs") {
                    private static final long serialVersionUID = -8727831157546262491L;

                    public void onSubmit() {
                        coverage.getResponseSRS().removeAll(selectedResponseSRSs);
                        selectedResponseSRSs.clear();
                    }
                });

        add(
                new Button("addNewResponseSRS") {
                    private static final long serialVersionUID = -2888152896129259019L;

                    public void onSubmit() {
                        coverage.getResponseSRS().add(newResponseSRS);
                        newResponseSRS = "";
                    }
                });

        add(
                new DropDownChoice<String>(
                        "defaultInterpolationMethod",
                        new PropertyModel<String>(coverage, "defaultInterpolationMethod"),
                        new WCSInterpolationModel()));

        Palette<String> interpolationMethods =
                new Palette<String>(
                        "interpolationMethods",
                        LiveCollectionModel.list(
                                new PropertyModel<List<String>>(coverage, "interpolationMethods")),
                        new WCSInterpolationModel(),
                        new SimpleChoiceRenderer(),
                        7,
                        false) {
                    private static final long serialVersionUID = 6815545819673802290L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("InterpolationMethodsPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId,
                                new ResourceModel("InterpolationMethodsPalette.availableHeader"));
                    }
                };
        interpolationMethods.add(new DefaultTheme());
        add(interpolationMethods);

        // don't allow editing the native format
        TextField<String> nativeFormat =
                new TextField<String>(
                        "nativeFormat", new PropertyModel<String>(coverage, "nativeFormat"));
        nativeFormat.setEnabled(false);
        add(nativeFormat);

        Palette<String> formatPalette =
                new Palette<String>(
                        "formatPalette",
                        LiveCollectionModel.list(
                                new PropertyModel<List<String>>(coverage, "supportedFormats")),
                        new WCSFormatsModel(),
                        new SimpleChoiceRenderer(),
                        10,
                        false) {
                    private static final long serialVersionUID = -2463012775305597908L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId, new ResourceModel("FormatsPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId, new ResourceModel("FormatsPalette.availableHeader"));
                    }
                };
        formatPalette.add(new DefaultTheme());
        add(formatPalette);
    }

    static class WCSFormatsModel extends LoadableDetachableModel<ArrayList<String>> {

        private static final long serialVersionUID = 1802421566341456007L;

        WCSFormatsModel() {
            super(new ArrayList<String>(WCS_FORMATS));
        }

        @Override
        protected ArrayList<String> load() {
            return new ArrayList<String>(WCS_FORMATS);
        }
    }

    static class WCSInterpolationModel extends LoadableDetachableModel<ArrayList<String>> {

        private static final long serialVersionUID = 7328612985196203413L;

        WCSInterpolationModel() {
            super(new ArrayList<String>(INTERPOLATIONS));
        }

        @Override
        protected ArrayList<String> load() {
            return new ArrayList<String>(INTERPOLATIONS);
        }
    }
}
