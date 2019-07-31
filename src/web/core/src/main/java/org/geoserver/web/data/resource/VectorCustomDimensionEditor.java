/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.resource;

import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.ResourceInfo;

/**
 * Dimension Editor Component for Custom dimensions on Vector.
 *
 * @author Fernando Mino - Geosolutions
 */
public class VectorCustomDimensionEditor extends DimensionEditorBase<VectorCustomDimensionEntry> {

    private TextField<String> nameInput;

    public VectorCustomDimensionEditor(
            String id,
            IModel<VectorCustomDimensionEntry> model,
            ResourceInfo resource,
            Class<?> type,
            boolean editNearestMatch) {
        super(id, model, resource, type, editNearestMatch);
    }

    public VectorCustomDimensionEditor(
            String id,
            IModel<VectorCustomDimensionEntry> model,
            ResourceInfo resource,
            Class<?> type) {
        super(id, model, resource, type);
    }

    @Override
    protected void initComponents() {
        // name model
        final PropertyModel<String> nameModel =
                new PropertyModel<String>(getModel(), "keyNoPrefixed");
        // add name dimension input text
        nameInput = new TextField<String>("customDimName", nameModel);
        add(nameInput);

        // ajax button for removing
        final AjaxSubmitLink removeButton =
                new AjaxSubmitLink("removeButton") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        send(
                                getWebPage(),
                                Broadcast.BREADTH,
                                new RemoveEvent(
                                        target, VectorCustomDimensionEditor.this.getModelObject()));
                    }
                };
        removeButton.setDefaultFormProcessing(false);
        add(removeButton);
    }

    @Override
    protected boolean resetDimensionDataOnDisabled() {
        return false;
    }

    @Override
    protected void convertInputExtensions(VectorCustomDimensionEntry info) {
        super.convertInputExtensions(info);
        nameInput.processInput();
        info.setKeyNoPrefixed(nameInput.getModelObject());
    }

    @Override
    protected VectorCustomDimensionEntry infoOf() {
        return new VectorCustomDimensionEntry();
    }

    @Override
    protected VectorCustomDimensionEntry infoOf(VectorCustomDimensionEntry info) {
        return new VectorCustomDimensionEntry(info);
    }

    @Override
    protected void initializeEndAttributesValues(List<String> endAttributes) {
        // no data to add
    }

    /** Custom dimension remove Event data holder. */
    public static class RemoveEvent {
        private final AjaxRequestTarget target;
        private final VectorCustomDimensionEntry entry;

        public RemoveEvent(AjaxRequestTarget target, VectorCustomDimensionEntry entry) {
            this.target = target;
            this.entry = entry;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public VectorCustomDimensionEntry getEntry() {
            return entry;
        }
    }
}
