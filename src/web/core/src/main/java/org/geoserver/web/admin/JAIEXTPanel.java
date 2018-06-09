/* (c) 2015 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.extensions.markup.html.form.palette.theme.DefaultTheme;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.config.JAIEXTInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.wicket.LiveCollectionModel;
import org.geoserver.web.wicket.ParamResourceModel;

public class JAIEXTPanel extends Panel {
    private static final long serialVersionUID = -4274061927074052166L;

    public JAIEXTPanel(String id, IModel<JAIInfo> model) {
        super(id, model);

        PropertyModel<JAIEXTInfo> jaiextModel = new PropertyModel<JAIEXTInfo>(model, "JAIEXTInfo");

        @SuppressWarnings({"rawtypes", "unchecked"})
        Palette jaiextSelector =
                new Palette<String>(
                        "jaiextOps",
                        LiveCollectionModel.set(new PropertyModel(jaiextModel, "JAIOperations")),
                        LiveCollectionModel.set(new PropertyModel(jaiextModel, "JAIEXTOperations")),
                        new JAChoiceRenderer(),
                        7,
                        false) {
                    private static final long serialVersionUID = -4665147378650094510L;

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newSelectedHeader(final String componentId) {
                        return new Label(
                                componentId, new ResourceModel("JAIEXTPanel.selectedHeader"));
                    }

                    /** Override otherwise the header is not i18n'ized */
                    @Override
                    public Component newAvailableHeader(final String componentId) {
                        return new Label(
                                componentId, new ResourceModel("JAIEXTPanel.availableHeader"));
                    }
                };
        jaiextSelector.add(new DefaultTheme());
        add(jaiextSelector);
    }

    static class JAChoiceRenderer extends ChoiceRenderer<String> {
        private static final long serialVersionUID = -1978519626641784908L;

        private static final String ALGEBRIC = "algebric";
        private static final String OPERATION_CONST = "operationConst";
        private static final String STATS = "Stats";

        @Override
        public Object getDisplayValue(String object) {
            if (object.equalsIgnoreCase(STATS)) {
                return new ParamResourceModel("JAIEXTPanel." + STATS, null, "").getString();
            } else if (object.equalsIgnoreCase(OPERATION_CONST)) {
                return new ParamResourceModel("JAIEXTPanel." + OPERATION_CONST, null, "")
                        .getString();
            } else if (object.equalsIgnoreCase(ALGEBRIC)) {
                return new ParamResourceModel("JAIEXTPanel." + ALGEBRIC, null, "").getString();
            } else {
                return object;
            }
        }

        @Override
        public String getIdValue(String object, int index) {
            return object;
        }
    }
}
