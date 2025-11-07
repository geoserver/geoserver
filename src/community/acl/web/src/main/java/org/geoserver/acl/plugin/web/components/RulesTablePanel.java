/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (factored out from org.geoserver.geofence.server.web.GeofenceServerAdminPage)
 */
package org.geoserver.acl.plugin.web.components;

import java.io.Serializable;
import lombok.NonNull;
import lombok.Setter;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.acl.plugin.web.support.SerializableBiConsumer;
import org.geoserver.acl.plugin.web.support.SerializableConsumer;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import wicketdnd.DragSource;
import wicketdnd.DropTarget;
import wicketdnd.Location;
import wicketdnd.Operation;
import wicketdnd.Transfer;
import wicketdnd.theme.WebTheme;

/**
 * {@link GeoServerTablePanel} for an {@link RulesDataProvider}
 *
 * @param <R>
 */
@SuppressWarnings("serial")
public final class RulesTablePanel<R extends Serializable> extends GeoServerTablePanel<R> {

    /** @see #buttons() */
    private static final Property<?> BUTTONS = new PropertyPlaceholder<>("buttons");

    private @NonNull @Setter SerializableConsumer<AjaxRequestTarget> onSelectionUpdate = t -> {};

    private @NonNull @Setter SerializableConsumer<R> onEdit = r -> {};

    private @NonNull @Setter SerializableBiConsumer<R, R> onDrop = (moved, target) -> {};

    @SuppressWarnings("unchecked")
    public static <T> Property<T> buttons() {
        return (Property<T>) BUTTONS;
    }

    private final DragSource dragSource;
    private final DropTarget dropTarget;

    public RulesTablePanel(String id, RulesDataProvider<R> dataProvider) {
        super(id, dataProvider, true /* selectable */);
        setOutputMarkupId(true);

        // re-create items, data is reloaded on each mutation like DnD, remove selected, etc
        DataView<?> items = (DataView<?>) super.get("listContainer:items");
        items.setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());

        add(new WebTheme());
        dragSource = dragSourceBehavior();
        dropTarget = dropTargetBehavior();
        enableDnD(true);
    }

    public void setFilteredMode(boolean filtered) {
        setPageable(!filtered);
        setSelectable(!filtered);
        setFilterable(!filtered);
        enableDnD(!filtered);
        modelChanged();
    }

    void enableDnD(boolean enable) {
        if (enable) {
            super.add(dragSource, dropTarget);
        } else {
            super.remove(dragSource, dropTarget);
        }
    }

    private DragSource dragSourceBehavior() {
        return new DragSource(Operation.MOVE).drag("tr");
    }

    /** @return {@link DropTarget} that calls {@link #onDrop onDrop(movedRule, targetRule)} */
    private DropTarget dropTargetBehavior() {
        return new DropTarget(Operation.MOVE) {

            private final @NonNull Class<R> dropTargetType = getDataProvider().getModelClass();

            @Override
            public void onDrop(AjaxRequestTarget target, Transfer transfer, Location location) {
                if (location == null) {
                    return;
                }
                Object targetObject = location.getComponent().getDefaultModel().getObject();
                if (dropTargetType.isInstance(targetObject)) {
                    R movedRule = transfer.getData();
                    R targetRule = dropTargetType.cast(targetObject);
                    onDrop.accept(movedRule, targetRule);
                }
            }
        }.dropCenter("tr");
    }

    @Override
    public RulesDataProvider<R> getDataProvider() {
        return (RulesDataProvider<R>) super.getDataProvider();
    }

    @Override
    protected Component getComponentForProperty(String id, IModel<R> itemModel, Property<R> property) {
        if (property == BUTTONS) {
            return new UpDownButtonsPanel(id, itemModel.getObject());
        }

        return new TableData(id, property.getModel(itemModel));
    }

    static class TableData extends Panel {

        public TableData(String id, IModel<?> itemModel) {
            super(id);
            add(new Label("label", itemModel));
        }
    }

    @Override
    protected void onSelectionUpdate(AjaxRequestTarget target) {
        onSelectionUpdate.accept(target);
    }

    public class UpDownButtonsPanel extends Panel {

        private R rule;

        public UpDownButtonsPanel(String id, final R rule) {
            super(id);
            this.rule = rule;
            this.setOutputMarkupId(true);

            add(moveUpLink());
            add(moveDownLink());
            add(editLink());
        }

        private ImageAjaxLink<Object> moveUpLink() {
            ImageAjaxLink<Object> upLink = new ImageAjaxLink<>("up", imageRef("arrow_up.png")) {

                protected @Override void onClick(AjaxRequestTarget target) {
                    RulesTablePanel.this.getDataProvider().moveUp(rule);
                    target.add(RulesTablePanel.this);
                }

                protected @Override void onComponentTag(ComponentTag tag) {
                    if (RulesTablePanel.this.getDataProvider().canUp(rule)) {
                        tag.put("style", "visibility:visible");
                    } else {
                        tag.put("style", "visibility:hidden");
                    }
                }
            };
            upLink.setOutputMarkupId(true);
            setImgAlt(upLink, "RulesTablePanel.buttons.up");
            return upLink;
        }

        private ImageAjaxLink<Object> moveDownLink() {
            ImageAjaxLink<Object> downLink = new ImageAjaxLink<>("down", imageRef("arrow_down.png")) {

                protected @Override void onClick(AjaxRequestTarget target) {
                    RulesTablePanel.this.getDataProvider().moveDown(rule);
                    target.add(RulesTablePanel.this);
                }

                protected @Override void onComponentTag(ComponentTag tag) {
                    if (RulesTablePanel.this.getDataProvider().canDown(rule)) {
                        tag.put("style", "visibility:visible");
                    } else {
                        tag.put("style", "visibility:hidden");
                    }
                }
            };
            downLink.setOutputMarkupId(true);
            setImgAlt(downLink, "RulesTablePanel.buttons.down");
            return downLink;
        }

        private Component editLink() {
            ImageAjaxLink<Object> editLink = new ImageAjaxLink<>("edit", imageRef("edit.png")) {

                protected @Override void onClick(AjaxRequestTarget target) {
                    onEdit.accept(rule);
                }
            };
            editLink.setOutputMarkupId(true);
            setImgAlt(editLink, "RulesTablePanel.buttons.edit");
            return editLink;
        }

        private void setImgAlt(ImageAjaxLink<Object> link, String resourceKey) {
            link.getImage().add(new AttributeModifier("alt", new ParamResourceModel(resourceKey, link)));
        }

        private PackageResourceReference imageRef(String imageName) {
            return new PackageResourceReference(RulesTablePanel.class, imageName);
        }
    }
}
