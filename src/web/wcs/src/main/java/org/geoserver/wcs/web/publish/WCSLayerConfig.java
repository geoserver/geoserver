/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.publish;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
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
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.SimpleChoiceRenderer;

/**
 * A configuration panel for CoverageInfo properties that related to WCS publication
 */
@SuppressWarnings("serial")
public class WCSLayerConfig extends LayerConfigurationPanel {

    private static final List<String> WCS_FORMATS = Arrays.asList("GIF","PNG","JPEG","TIFF","GTOPO30","GEOTIFF","IMAGEMOSAIC","ARCGRID");
    private static final List<String> INTERPOLATIONS = Arrays.asList("nearest neighbor","bilinear","bicubic");
    
    private List<String> selectedRequestSRSs;
    private List<String> selectedResponseSRSs;
    private List<String> selectedInterpolationMethods;
    private String newRequestSRS;
    private String newResponseSRS;
    private String newInterpolationMethod;

    public WCSLayerConfig(String id, IModel model){
        super(id, model);

        final CoverageInfo coverage = (CoverageInfo) getLayerInfo().getResource();
        add(new ListMultipleChoice("requestSRS", 
                    new PropertyModel(this, "selectedRequestSRSs"), 
                    coverage.getRequestSRS())
        );

        add(new TextField("newRequestSRS", new PropertyModel(this, "newRequestSRS")));

        add(new Button("deleteSelectedRequestSRSs"){
            public void onSubmit(){
                coverage.getRequestSRS().removeAll(selectedRequestSRSs);
                selectedRequestSRSs.clear();
            }
        });

        add(new Button("addNewRequestSRS"){
            public void onSubmit(){
                coverage.getRequestSRS().add(newRequestSRS);
                newRequestSRS = "";
            }
        });

        add (new ListMultipleChoice("responseSRS", 
                    new PropertyModel(this, "selectedResponseSRSs"),
                    coverage.getResponseSRS())
        );

        add(new TextField("newResponseSRS", new PropertyModel(this, "newResponseSRS")));

        add(new Button("deleteSelectedResponseSRSs"){
            public void onSubmit(){
                coverage.getResponseSRS().removeAll(selectedResponseSRSs);
                selectedResponseSRSs.clear();
            }
        });

        add(new Button("addNewResponseSRS"){
            public void onSubmit(){
                coverage.getResponseSRS().add(newResponseSRS);
                newResponseSRS = "";
            }
        });

        add(new DropDownChoice("defaultInterpolationMethod", new PropertyModel(coverage, "defaultInterpolationMethod"),
               new WCSInterpolationModel()));
 
        Palette interpolationMethods = new Palette("interpolationMethods", LiveCollectionModel
                .list(new PropertyModel(coverage, "interpolationMethods")),
                new WCSInterpolationModel(), new SimpleChoiceRenderer(), 7, false) {
            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newSelectedHeader(final String componentId) {
                return new Label(componentId, new ResourceModel(
                        "InterpolationMethodsPalette.selectedHeader"));
            }

            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newAvailableHeader(final String componentId) {
                return new Label(componentId, new ResourceModel(
                        "InterpolationMethodsPalette.availableHeader"));
            }
        };
        add(interpolationMethods);

        // don't allow editing the native format
        TextField nativeFormat = new TextField("nativeFormat", new PropertyModel(coverage, "nativeFormat"));
        nativeFormat.setEnabled(false);
        add(nativeFormat);

        Palette formatPalette = new Palette("formatPalette", LiveCollectionModel
                .list(new PropertyModel(coverage, "supportedFormats")), new WCSFormatsModel(),
                new SimpleChoiceRenderer(), 10, false) {
            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newSelectedHeader(final String componentId) {
                return new Label(componentId, new ResourceModel(
                        "FormatsPalette.selectedHeader"));
            }

            /**
             * Override otherwise the header is not i18n'ized
             */
            @Override
            public Component newAvailableHeader(final String componentId) {
                return new Label(componentId, new ResourceModel(
                        "FormatsPalette.availableHeader"));
            }
        };
        add(formatPalette);
   }
    
    
    static class WCSFormatsModel extends LoadableDetachableModel {

        WCSFormatsModel() {
            super(new ArrayList(WCS_FORMATS));
        }

        @Override
        protected Object load() {
            return new ArrayList(WCS_FORMATS);
        }
    }
    
    static class WCSInterpolationModel extends LoadableDetachableModel {

        WCSInterpolationModel() {
            super(new ArrayList(INTERPOLATIONS));
        }

        @Override
        protected Object load() {
            return new ArrayList(INTERPOLATIONS);
        }
    }
}
