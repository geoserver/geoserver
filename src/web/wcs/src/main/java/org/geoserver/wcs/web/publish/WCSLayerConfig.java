/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.publish;

import java.io.Serial;
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
import org.geotools.util.SuppressFBWarnings;

/** A configuration panel for CoverageInfo properties that related to WCS publication */
public class WCSLayerConfig extends PublishedConfigurationPanel<LayerInfo> {

    @Serial
    private static final long serialVersionUID = 6120092654147588736L;

    private static final List<String> WCS_FORMATS =
            Arrays.asList("GIF", "PNG", "JPEG", "TIFF", "GEOTIFF", "IMAGEMOSAIC");
    private static final List<String> INTERPOLATIONS = Arrays.asList("nearest neighbor", "bilinear", "bicubic");

    @SuppressFBWarnings("NP_UNWRITTEN_FIELD") // wicket field reflection
    private List<String> selectedRequestSRSs;

    @SuppressFBWarnings("NP_UNWRITTEN_FIELD") // wicket field reflection
    private List<String> selectedResponseSRSs;

    private String newRequestSRS;
    private String newResponseSRS;

    public WCSLayerConfig(String id, IModel<LayerInfo> model) {
        super(id, model);

        final CoverageInfo coverage = (CoverageInfo) getPublishedInfo().getResource();
        add(new ListMultipleChoice<>(
                "requestSRS", new PropertyModel<>(this, "selectedRequestSRSs"), coverage.getRequestSRS()));

        add(new TextField<>("newRequestSRS", new PropertyModel<>(this, "newRequestSRS")));

        add(new Button("deleteSelectedRequestSRSs") {
            @Serial
            private static final long serialVersionUID = 8363252127939759315L;

            @Override
            public void onSubmit() {
                coverage.getRequestSRS().removeAll(selectedRequestSRSs);
                selectedRequestSRSs.clear();
            }
        });

        add(new Button("addNewRequestSRS") {
            @Serial
            private static final long serialVersionUID = -3493317500980471055L;

            @Override
            public void onSubmit() {
                coverage.getRequestSRS().add(newRequestSRS);
                newRequestSRS = "";
            }
        });

        add(new ListMultipleChoice<>(
                "responseSRS", new PropertyModel<>(this, "selectedResponseSRSs"), coverage.getResponseSRS()));

        add(new TextField<>("newResponseSRS", new PropertyModel<>(this, "newResponseSRS")));

        add(new Button("deleteSelectedResponseSRSs") {
            @Serial
            private static final long serialVersionUID = -8727831157546262491L;

            @Override
            public void onSubmit() {
                coverage.getResponseSRS().removeAll(selectedResponseSRSs);
                selectedResponseSRSs.clear();
            }
        });

        add(new Button("addNewResponseSRS") {
            @Serial
            private static final long serialVersionUID = -2888152896129259019L;

            @Override
            public void onSubmit() {
                coverage.getResponseSRS().add(newResponseSRS);
                newResponseSRS = "";
            }
        });

        add(new DropDownChoice<>(
                "defaultInterpolationMethod",
                new PropertyModel<>(coverage, "defaultInterpolationMethod"),
                new WCSInterpolationModel()));

        Palette<String> interpolationMethods =
                new Palette<>(
                        "interpolationMethods",
                        LiveCollectionModel.list(new PropertyModel<>(coverage, "interpolationMethods")),
                        new WCSInterpolationModel(),
                        new SimpleChoiceRenderer<>(),
                        7,
                        false) {
                    @Serial
                    private static final long serialVersionUID = 6815545819673802290L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("InterpolationMethodsPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("InterpolationMethodsPalette.availableHeader"));
                    }
                };
        interpolationMethods.add(new DefaultTheme());
        add(interpolationMethods);

        // don't allow editing the native format
        TextField<String> nativeFormat = new TextField<>("nativeFormat", new PropertyModel<>(coverage, "nativeFormat"));
        nativeFormat.setEnabled(false);
        add(nativeFormat);

        Palette<String> formatPalette =
                new Palette<>(
                        "formatPalette",
                        LiveCollectionModel.list(new PropertyModel<>(coverage, "supportedFormats")),
                        new WCSFormatsModel(),
                        new SimpleChoiceRenderer<>(),
                        10,
                        false) {
                    @Serial
                    private static final long serialVersionUID = -2463012775305597908L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("FormatsPalette.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(componentId, new ResourceModel("FormatsPalette.availableHeader"));
                    }
                };
        formatPalette.add(new DefaultTheme());
        add(formatPalette);
    }

    static class WCSFormatsModel extends LoadableDetachableModel<ArrayList<String>> {

        @Serial
        private static final long serialVersionUID = 1802421566341456007L;

        WCSFormatsModel() {
            super(new ArrayList<>(WCS_FORMATS));
        }

        @Override
        protected ArrayList<String> load() {
            return new ArrayList<>(WCS_FORMATS);
        }
    }

    static class WCSInterpolationModel extends LoadableDetachableModel<ArrayList<String>> {

        @Serial
        private static final long serialVersionUID = 7328612985196203413L;

        WCSInterpolationModel() {
            super(new ArrayList<>(INTERPOLATIONS));
        }

        @Override
        protected ArrayList<String> load() {
            return new ArrayList<>(INTERPOLATIONS);
        }
    }
}
