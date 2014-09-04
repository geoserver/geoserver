/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.NumberValidator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.JAIInfo.PngEncoderType;
import org.geoserver.web.wicket.ParamResourceModel;

import com.sun.media.imageioimpl.common.PackageUtil;

/**
 * Edits the JAI configuration parameters
 */
public class JAIPage extends ServerAdminPage {
    private static final long serialVersionUID = -1184717232184497578L;

    public JAIPage(){
        final IModel geoServerModel = getGeoServerModel();
        
        // this invokation will trigger a clone of the JAIInfo
        // which will allow the modification proxy seeing changes on the
        // Jai page with respect to the original JAIInfo object
        final IModel jaiModel = getJAIModel();

        // form and submit
        Form form = new Form("form", new CompoundPropertyModel(jaiModel));
        add( form );

        // All the fields
        // ... memory capacity and threshold are percentages
        NumberValidator percentageValidator = NumberValidator.range(0, 1);
        TextField memoryCapacity = new TextField("memoryCapacity");
        memoryCapacity.add(percentageValidator);
        form.add(memoryCapacity);
        TextField memoryThreshold = new TextField("memoryThreshold");
        memoryThreshold.add(percentageValidator);
        form.add(memoryThreshold);
        TextField tileThreads = new TextField("tileThreads");
        tileThreads.add(NumberValidator.POSITIVE);
        form.add(tileThreads);
        TextField tilePriority = new TextField("tilePriority");
        tilePriority.add(NumberValidator.POSITIVE);
        form.add(tilePriority);
        form.add(new CheckBox("recycling"));
        form.add(new CheckBox("jpegAcceleration"));
        addPngEncoderEditor(form);
        form.add(new CheckBox("allowNativeMosaic"));
        form.add(new CheckBox("allowNativeWarp"));

        Button submit = new Button("submit", new StringResourceModel("submit", this, null)) {
            @Override
            public void onSubmit() {
                GeoServer gs = (GeoServer) geoServerModel.getObject();
                GeoServerInfo global = gs.getGlobal();
                global.setJAI( (JAIInfo)jaiModel.getObject());
                gs.save( global );
                doReturn();
            }
        };
        form.add(submit);
        
        Button cancel = new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
    }

    private void addPngEncoderEditor(Form form) {
        // get the list of available encoders
        List<PngEncoderType> encoders = new ArrayList(Arrays.asList(JAIInfo.PngEncoderType.values()));
        if(!PackageUtil.isCodecLibAvailable()) {
            encoders.remove(PngEncoderType.NATIVE);
        }
        // create the editor, eventually set a default value
        DropDownChoice<JAIInfo.PngEncoderType> editor = new DropDownChoice<JAIInfo.PngEncoderType>("pngEncoderType", encoders, new IChoiceRenderer<JAIInfo.PngEncoderType>() {

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
        if(!encoders.contains(editor.getModelObject())) {
            editor.setModelObject(PngEncoderType.PNGJ);
        }
    }

}
