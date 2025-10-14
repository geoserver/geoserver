/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.CoverageAccessInfo.QueueType;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoserverAjaxSubmitLink;
import org.geoserver.web.wicket.ParamResourceModel;

/** Edits the Coverage configuration parameters */
// TODO WICKET8 - Verify this page works OK
public class CoverageAccessPage extends ServerAdminPage {
    @Serial
    private static final long serialVersionUID = -5028265196560034398L;

    private IModel<?> geoServerModel;
    private IModel<CoverageAccessInfo> coverageModel;

    class PoolSizeValidator extends AbstractFormValidator {

        @Serial
        private static final long serialVersionUID = -3435198454570572665L;

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return null;
        }

        @Override
        public void validate(Form<?> form) {
            // only validate on final submit
            if (!form.findSubmitter().getInputName().equals("submit")) {
                return;
            }

            // Getting pool components
            final TextField<?> maxPoolField = (TextField<?>) form.get("maxPoolSize");
            final TextField<?> corePoolField = (TextField<?>) form.get("corePoolSize");

            int maxPool;
            int corePool;

            // checking limits are properly set
            if (maxPoolField != null && corePoolField != null) {
                final String mp = maxPoolField.getValue();
                final String cp = corePoolField.getValue();
                if (!(mp == null
                        || cp == null
                        || mp.trim().isEmpty()
                        || cp.trim().isEmpty())) {
                    try {
                        maxPool = Integer.valueOf(mp);
                    } catch (NumberFormatException nfe) {
                        // The MinimumValidator(1) should already deal with that
                        return;
                    }

                    try {
                        corePool = Integer.valueOf(cp);
                    } catch (NumberFormatException nfe) {
                        // The MinimumValidator(1) should already deal with that
                        return;
                    }

                    if (maxPool >= 1 && corePool >= 1 && maxPool < corePool) {
                        form.error(new ParamResourceModel("poolSizeCheck", getPage()).getString());
                    }
                }
            }
        }
    }

    public CoverageAccessPage() {
        geoServerModel = getGeoServerModel();

        // this invocation will trigger a clone of the CoverageAccessInfo,
        // which will allow the modification proxy seeing changes on the
        // CoverageAccess page with respect to the original CoverageAccessInfo object
        coverageModel = getCoverageAccessModel();

        // form and submit
        Form<CoverageAccessInfo> form = new Form<>("form", new CompoundPropertyModel<>(coverageModel));
        add(form);
        form.add(new PoolSizeValidator());
        // All the fields
        NumberTextField<Integer> corePoolSize = new NumberTextField<>("corePoolSize", Integer.class);
        corePoolSize.setMinimum(1);
        form.add(corePoolSize);

        NumberTextField<Integer> maxPoolSize = new NumberTextField<>("maxPoolSize", Integer.class);
        maxPoolSize.add(RangeValidator.minimum(1));
        form.add(maxPoolSize);

        NumberTextField<Integer> keepAliveTime = new NumberTextField<>("keepAliveTime", Integer.class);
        keepAliveTime.add(RangeValidator.minimum(1));
        form.add(keepAliveTime);

        final DropDownChoice<QueueType> queueType = new DropDownChoice<>(
                "queueType", Arrays.asList(CoverageAccessInfo.QueueType.values()), new QueueTypeRenderer());
        form.add(queueType);

        TextField<String> imageIOCacheThreshold = new TextField<>("imageIOCacheThreshold");
        imageIOCacheThreshold.add(RangeValidator.minimum(0l));
        form.add(imageIOCacheThreshold);

        Button submit = new Button("submit") {
            @Serial
            private static final long serialVersionUID = 4149741045073254811L;

            @Override
            public void onSubmit() {
                save(true);
            }
        };
        form.add(submit);

        form.add(applyLink(form));

        Button cancel = new Button("cancel") {
            @Serial
            private static final long serialVersionUID = -57093747603810865L;

            @Override
            public void onSubmit() {
                doReturn();
            }
        };
        form.add(cancel);
    }

    public void save(boolean doReturn) {
        GeoServer gs = (GeoServer) geoServerModel.getObject();
        GeoServerInfo global = gs.getGlobal();
        global.setCoverageAccess(coverageModel.getObject());
        gs.save(global);
        if (doReturn) doReturn();
    }

    private GeoserverAjaxSubmitLink applyLink(Form form) {
        return new GeoserverAjaxSubmitLink("apply", form, this) {

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                target.add(getForm());
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

    /** Display and ID mapping adapter for QueueType. */
    // TODO: consider use of EnumChoiceRenderer<QueueType>
    private class QueueTypeRenderer extends ChoiceRenderer<QueueType> {
        @Serial
        private static final long serialVersionUID = -702911785346928083L;

        @Override
        public String getDisplayValue(QueueType type) {
            return new StringResourceModel(type.name(), CoverageAccessPage.this, null).getString();
        }

        @Override
        public String getIdValue(QueueType type, int index) {
            return type.name();
        }

        @Override
        public QueueType getObject(String id, IModel<? extends List<? extends QueueType>> choices) {
            return QueueType.valueOf(id);
        }
    }
}
