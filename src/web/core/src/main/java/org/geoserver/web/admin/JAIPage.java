/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.JAIInfo.PngEncoderType;
import org.geoserver.web.GeoserverAjaxSubmitLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.PercentageTextField;

/** Edits the JAI configuration parameters */
// TODO WICKET8 - Verify this page works OK
public class JAIPage extends ServerAdminPage {
    @Serial
    private static final long serialVersionUID = -1184717232184497578L;

    private final IModel<GeoServer> geoServerModel;
    private final IModel<JAIInfo> jaiModel;

    public JAIPage() {
        geoServerModel = getGeoServerModel();

        // this invokation will trigger a clone of the JAIInfo
        // which will allow the modification proxy seeing changes on the
        // Jai page with respect to the original JAIInfo object
        jaiModel = getJAIModel();

        // form and submit
        Form<JAIInfo> form = new Form<>("form", new CompoundPropertyModel<>(jaiModel));
        add(form);

        // All the fields
        // ... memory capacity and threshold are percentages
        RangeValidator<Double> percentageValidator = RangeValidator.range(0.0, 1.0);
        TextField<Double> memoryCapacity = new PercentageTextField("memoryCapacity");
        memoryCapacity.add(percentageValidator);
        form.add(memoryCapacity);
        TextField<Double> memoryThreshold = new PercentageTextField("memoryThreshold");
        memoryThreshold.add(percentageValidator);
        form.add(memoryThreshold);
        TextField<Integer> tileThreads = new TextField<>("tileThreads");
        tileThreads.add(RangeValidator.minimum(0));
        form.add(tileThreads);
        TextField<Integer> tilePriority = new TextField<>("tilePriority");
        tilePriority.add(RangeValidator.minimum(0));
        form.add(tilePriority);
        form.add(new CheckBox("recycling"));
        addPngEncoderEditor(form);

        Button submit = new Button("submit") {
            @Serial
            private static final long serialVersionUID = -2842881187264147131L;

            @Override
            public void onSubmit() {
                save(true);
            }
        };
        form.add(submit);

        form.add(applyLink(form));

        Button cancel = new Button("cancel") {
            @Serial
            private static final long serialVersionUID = 7917847596581898225L;

            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
    }

    private void save(boolean doReturn) {
        GeoServer gs = geoServerModel.getObject();
        GeoServerInfo global = gs.getGlobal();
        global.setJAI(jaiModel.getObject());
        gs.save(global);
        if (doReturn) doReturn();
    }

    private GeoserverAjaxSubmitLink applyLink(Form form) {
        return new GeoserverAjaxSubmitLink("apply", form, this) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(form);
            }

            @Override
            protected void onSubmitInternal(AjaxRequestTarget target) {
                try {
                    save(false);
                } catch (IllegalArgumentException e) {
                    getForm().error(e.getMessage());
                    target.add(getForm());
                }
            }
        };
    }

    private void addPngEncoderEditor(Form<JAIInfo> form) {
        // get the list of available encoders
        List<PngEncoderType> encoders = new ArrayList<>(Arrays.asList(JAIInfo.PngEncoderType.values()));
        // create the editor, eventually set a default value
        DropDownChoice<JAIInfo.PngEncoderType> editor =
                new DropDownChoice<>("pngEncoderType", encoders, new ChoiceRenderer<>() {
                    @Serial
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(PngEncoderType type) {
                        return new ParamResourceModel("pngEncoder." + type.name(), JAIPage.this).getString();
                    }

                    @Override
                    public String getIdValue(PngEncoderType type, int index) {
                        return type.name();
                    }
                });
        form.add(editor);
        if (!encoders.contains(editor.getModelObject())) {
            editor.setModelObject(PngEncoderType.PNGJ);
        }
    }
}
