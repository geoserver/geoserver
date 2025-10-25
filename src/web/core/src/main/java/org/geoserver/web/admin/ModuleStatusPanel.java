/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.geoserver.platform.ModuleStatusImpl;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.wicket.CachingImage;
import org.geoserver.web.wicket.GSModalWindow;
import org.geoserver.web.wicket.ParamResourceModel;

public class ModuleStatusPanel extends Panel {

    @Serial
    private static final long serialVersionUID = 3892224318224575781L;

    final CatalogIconFactory icons = CatalogIconFactory.get();

    GSModalWindow popup;

    AjaxLink msgLink;

    public ModuleStatusPanel(String id, AbstractStatusPage parent) {
        super(id);
        initUI();
    }

    public void initUI() {

        final WebMarkupContainer wmc = new WebMarkupContainer("listViewContainer");
        wmc.setOutputMarkupId(true);
        this.add(wmc);

        popup = new GSModalWindow("popup");
        add(popup);

        // get the list of ModuleStatuses
        Comparator<String> nullSafeStringComparator = Comparator.nullsFirst(String::compareToIgnoreCase);
        Comparator<ModuleStatus.Category> nullSafeEnumComparator =
                Comparator.nullsFirst(ModuleStatus.Category::compareTo);
        List<ModuleStatus> applicationStatus = GeoServerExtensions.extensions(ModuleStatus.class).stream()
                .map(ModuleStatusImpl::new)
                .sorted(Comparator.comparing(ModuleStatus::getCategory, nullSafeEnumComparator)
                        .thenComparing(ModuleStatus::getName, nullSafeStringComparator))
                .collect(Collectors.toList());

        final ListView<ModuleStatus> moduleView = new ListView<>("modules", applicationStatus) {
            @Serial
            private static final long serialVersionUID = 235576083712961710L;

            @Override
            protected void populateItem(ListItem<ModuleStatus> item) {
                item.add(new Label("module", new PropertyModel<>(item.getModel(), "module")));
                item.add(getIcons("available", item.getModelObject().isAvailable()));
                item.add(getIcons("enabled", item.getModelObject().isEnabled()));
                item.add(new Label(
                        "version",
                        new Model<>(item.getModelObject().getVersion().orElse(""))));
                msgLink = new AjaxLink<>("msg") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        popup.setInitialHeight(325);
                        popup.setInitialWidth(525);
                        popup.setContent(new MessagePanel(popup.getContentId(), item));
                        popup.setTitle("Module Info");
                        popup.show(target);
                    }
                };
                msgLink.setEnabled(true);
                msgLink.add(new Label("nameLink", new PropertyModel<>(item.getModel(), "name")));
                item.add(msgLink);
                item.add(new Label(
                        "category",
                        new ParamResourceModel(
                                        item.getModelObject().getCategory().name(), this)
                                .getString()));
            }
        };
        wmc.add(moduleView);
    }

    final Fragment getIcons(String id, boolean status) {
        PackageResourceReference icon = status ? icons.getEnabledIcon() : icons.getDisabledIcon();
        Fragment f = new Fragment(id, "iconFragment", this);
        f.add(new CachingImage("statusIcon", icon));
        return f;
    }

    static class MessagePanel extends Panel {

        @Serial
        private static final long serialVersionUID = -3200098674603724915L;

        public MessagePanel(String id, ListItem<ModuleStatus> item) {
            super(id);

            Label name = new Label("name", new PropertyModel<>(item.getModel(), "name"));
            Label module = new Label("module", new PropertyModel<>(item.getModel(), "module"));
            ModuleStatus modelObject = item.getModelObject();
            Label component = new Label(
                    "component", new Model<>(modelObject.getComponent().orElse("")));
            Label version =
                    new Label("version", new Model<>(modelObject.getVersion().orElse("")));
            MultiLineLabel msgLabel =
                    new MultiLineLabel("msg", modelObject.getMessage().orElse(""));
            Label category =
                    new Label("category", new Model<>(modelObject.getCategory().name()));
            Label contact = new Label("contact", new Model<>(modelObject.getContact()));

            add(name);
            add(module);
            add(component);
            add(version);
            add(msgLabel);
            add(category);
            add(contact);
        }
    }
}
