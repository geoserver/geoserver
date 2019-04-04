/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.QosData;
import org.geoserver.qos.xml.OperatingInfo;
import org.geoserver.qos.xml.OperatingInfoTime;
import org.geoserver.qos.xml.ReferenceType;

public class OperatingInfoModalPanel extends Panel {
    private static final long serialVersionUID = 1L;

    protected ModalWindow timeModal;
    // protected SelectionModel<OperatingInfoTime> timeSelection;
    protected WebMarkupContainer timeListContainer;
    protected SerializableConsumer<OperatingInfo> onSaveOperatingInfo = o -> {};
    protected SerializableConsumer<AjaxRequestTarget> onAjaxSave = a -> {};
    protected SerializableConsumer<AjaxTargetAndModel<OperatingInfo>> onDelete;

    public OperatingInfoModalPanel(String id, IModel<OperatingInfo> model) {
        super(id, model);

        final WebMarkupContainer form = new WebMarkupContainer("opInfoForm");
        form.setOutputMarkupId(true);
        add(form);

        // pre init non nullable objects
        if (model.getObject().getOperationalStatus() == null)
            model.getObject().setOperationalStatus(new ReferenceType());
        if (model.getObject().getByDaysOfWeek() == null)
            model.getObject().setByDaysOfWeek(new ArrayList<>());

        final DropDownChoice<String> titleSelect =
                new DropDownChoice<String>(
                        "titleSelect",
                        new PropertyModel<String>(model, "operationalStatus.href"),
                        QosData.instance().getOperationalStatusList(),
                        new ChoiceRenderer<String>() {
                            @Override
                            public Object getDisplayValue(String object) {
                                if (object == null) return null;
                                return object.split("#")[1];
                            }

                            @Override
                            public String getIdValue(String object, int index) {
                                return object;
                            }
                        }) {
                    @Override
                    protected void onSelectionChanged(String newSelection) {
                        super.onSelectionChanged(newSelection);
                    }
                };
        form.add(titleSelect);

        final TextField<String> titleInput =
                new TextField<String>(
                        "titleInput", new PropertyModel<>(model, "operationalStatus.title"));
        // titleInput.setRequired(true);
        form.add(titleInput);

        timeListContainer = new WebMarkupContainer("timeListContainer");
        timeListContainer.setOutputMarkupId(true);
        form.add(timeListContainer);

        // ModalWindow timeModal
        timeModal = new ModalWindow("timeModal");
        add(timeModal);

        final ListView<OperatingInfoTime> timeListView =
                new ListView<OperatingInfoTime>("timeList", model.getObject().getByDaysOfWeek()) {
                    @Override
                    protected void populateItem(ListItem<OperatingInfoTime> item) {
                        OperatingInfoTimeModalPanel timePanel =
                                new OperatingInfoTimeModalPanel("timePanel", item.getModel());
                        // set on delete callback
                        timePanel.setOnDelete(
                                x -> {
                                    model.getObject().getByDaysOfWeek().remove(x.getModel());
                                    x.getTarget().add(timeListContainer);
                                });
                        item.add(timePanel);
                    }
                };
        timeListView.setOutputMarkupId(true);
        timeListContainer.add(timeListView);

        final AjaxButton addTimeLink = new AjaxButton("addTime") {};
        timeListContainer.add(addTimeLink);
        addTimeLink.add(
                new AjaxFormSubmitBehavior("click") {
                    @Override
                    protected void onAfterSubmit(final AjaxRequestTarget target) {
                        model.getObject().getByDaysOfWeek().add(new OperatingInfoTime());
                        target.add(timeListContainer);
                    }
                });

        final AjaxSubmitLink deleteLink =
                new AjaxSubmitLink("deleteLink") {
                    @Override
                    public void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        delete(target);
                    }
                };
        form.add(deleteLink);
    }

    public void setOnSaveOperatingInfo(SerializableConsumer<OperatingInfo> onSaveOperatingInfo) {
        this.onSaveOperatingInfo = onSaveOperatingInfo;
    }

    public void setOnAjaxSave(SerializableConsumer<AjaxRequestTarget> onAjaxSave) {
        this.onAjaxSave = onAjaxSave;
    }

    protected void delete(AjaxRequestTarget target) {
        if (onDelete == null) return;
        AjaxTargetAndModel<OperatingInfo> chain =
                new AjaxTargetAndModel<>((OperatingInfo) this.getDefaultModelObject(), target);
        onDelete.accept(chain);
    }

    public void setOnDelete(SerializableConsumer<AjaxTargetAndModel<OperatingInfo>> onDelete) {
        this.onDelete = onDelete;
    }

    class OperatingInfoTimePanel extends Panel {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("rawtypes")
        public OperatingInfoTimePanel(String id, IModel<OperatingInfoTime> model) {
            super(id, model);

            add(
                    new Link("edit") {
                        @Override
                        public void onClick() {}
                    });

            add(
                    new Link("delete") {
                        @Override
                        public void onClick() {
                            OperatingInfoTime todelete =
                                    (OperatingInfoTime) getParent().getDefaultModelObject();
                            OperatingInfo info =
                                    (OperatingInfo)
                                            OperatingInfoModalPanel.this.getDefaultModelObject();
                            info.getByDaysOfWeek().remove(todelete);
                        }
                    });
        }
    }
}
