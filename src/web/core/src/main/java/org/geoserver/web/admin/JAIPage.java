/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import com.sun.media.imageioimpl.common.PackageUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.geoserver.config.JAIEXTInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.JAIInfo.PngEncoderType;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.PercentageTextField;
import org.geotools.image.ImageWorker;

/** Edits the JAI configuration parameters */
public class JAIPage extends ServerAdminPage {
    private static final long serialVersionUID = -1184717232184497578L;

    public JAIPage() {
        final IModel<GeoServer> geoServerModel = getGeoServerModel();

        // this invokation will trigger a clone of the JAIInfo
        // which will allow the modification proxy seeing changes on the
        // Jai page with respect to the original JAIInfo object
        final IModel<JAIInfo> jaiModel = getJAIModel();

        // form and submit
        Form<JAIInfo> form =
                new Form<JAIInfo>("form", new CompoundPropertyModel<JAIInfo>(jaiModel));
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
        TextField<Integer> tileThreads = new TextField<Integer>("tileThreads");
        tileThreads.add(RangeValidator.minimum(0));
        form.add(tileThreads);
        TextField<Integer> tilePriority = new TextField<Integer>("tilePriority");
        tilePriority.add(RangeValidator.minimum(0));
        form.add(tilePriority);
        form.add(new CheckBox("recycling"));
        form.add(new CheckBox("jpegAcceleration"));
        addPngEncoderEditor(form);
        CheckBox checkBoxMosaic = new CheckBox("allowNativeMosaic");
        CheckBox checkBoxWarp = new CheckBox("allowNativeWarp");
        JAIInfo info = (JAIInfo) jaiModel.getObject();
        JAIEXTInfo je = null;
        boolean isJAIExtEnabled = ImageWorker.isJaiExtEnabled();
        if (isJAIExtEnabled) {
            je = info.getJAIEXTInfo();
        }
        boolean mosaicEnabled = je != null && !je.getJAIEXTOperations().contains("Mosaic");
        boolean warpEnabled = je != null && !je.getJAIEXTOperations().contains("Warp");
        checkBoxMosaic.setEnabled(mosaicEnabled);
        checkBoxWarp.setEnabled(warpEnabled);
        form.add(checkBoxMosaic);
        form.add(checkBoxWarp);
        JAIEXTPanel jaiExtPanel = new JAIEXTPanel("jaiext", jaiModel);
        if (!isJAIExtEnabled) {
            jaiExtPanel.setVisible(false);
        }
        form.add(jaiExtPanel);

        Button submit =
                new Button("submit") {
                    private static final long serialVersionUID = -2842881187264147131L;

                    @Override
                    public void onSubmit() {
                        GeoServer gs = (GeoServer) geoServerModel.getObject();
                        GeoServerInfo global = gs.getGlobal();
                        global.setJAI((JAIInfo) jaiModel.getObject());
                        gs.save(global);
                        doReturn();
                    }
                };
        form.add(submit);

        Button cancel =
                new Button("cancel") {
                    private static final long serialVersionUID = 7917847596581898225L;

                    @Override
                    public void onSubmit() {
                        doReturn();
                    }
                };
        form.add(cancel);
    }

    private void addPngEncoderEditor(Form<JAIInfo> form) {
        // get the list of available encoders
        List<PngEncoderType> encoders =
                new ArrayList<PngEncoderType>(Arrays.asList(JAIInfo.PngEncoderType.values()));
        if (!PackageUtil.isCodecLibAvailable()) {
            encoders.remove(PngEncoderType.NATIVE);
        }
        // create the editor, eventually set a default value
        DropDownChoice<JAIInfo.PngEncoderType> editor =
                new DropDownChoice<JAIInfo.PngEncoderType>(
                        "pngEncoderType",
                        encoders,
                        new ChoiceRenderer<JAIInfo.PngEncoderType>() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object getDisplayValue(PngEncoderType type) {
                                return new ParamResourceModel(
                                                "pngEncoder." + type.name(), JAIPage.this)
                                        .getString();
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
