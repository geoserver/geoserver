/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.image.Image;
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

public class ModuleStatusPanel extends Panel {

    private static final long serialVersionUID = 3892224318224575781L;

    final CatalogIconFactory icons = CatalogIconFactory.get();

    ModalWindow popup;

    AjaxLink msgLink;

    public ModuleStatusPanel(String id, AbstractStatusPage parent) {
        super(id);
        initUI();
    }

    public void initUI() {

        final WebMarkupContainer wmc = new WebMarkupContainer("listViewContainer");
        wmc.setOutputMarkupId(true);
        this.add(wmc);

        popup = new ModalWindow("popup");
        add(popup);

        // get the list of ModuleStatuses
        List<ModuleStatus> applicationStatus =
                GeoServerExtensions.extensions(ModuleStatus.class)
                        .stream()
                        .filter(status -> !status.getModule().matches("\\A[system-](.*)"))
                        .map(ModuleStatusImpl::new)
                        .sorted(Comparator.comparing(ModuleStatus::getModule))
                        .collect(Collectors.toList());

        final ListView<ModuleStatus> moduleView =
                new ListView<ModuleStatus>("modules", applicationStatus) {
                    private static final long serialVersionUID = 235576083712961710L;

                    @Override
                    protected void populateItem(ListItem<ModuleStatus> item) {
                        item.add(new Label("module", new PropertyModel(item.getModel(), "module")));
                        item.add(getIcons("available", item.getModelObject().isAvailable()));
                        item.add(getIcons("enabled", item.getModelObject().isEnabled()));
                        item.add(
                                new Label(
                                        "component",
                                        new Model(
                                                item.getModelObject().getComponent().orElse(""))));
                        item.add(
                                new Label(
                                        "version",
                                        new Model(item.getModelObject().getVersion().orElse(""))));
                        msgLink =
                                new AjaxLink("msg") {
                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        popup.setInitialHeight(325);
                                        popup.setInitialWidth(525);
                                        popup.setContent(
                                                new MessagePanel(popup.getContentId(), item));
                                        popup.setTitle("Module Info");
                                        popup.show(target);
                                    }
                                };
                        msgLink.setEnabled(true);
                        msgLink.add(
                                new Label("nameLink", new PropertyModel(item.getModel(), "name")));
                        item.add(msgLink);
                    }
                };
        wmc.add(moduleView);
    }

    final Fragment getIcons(String id, boolean status) {
        PackageResourceReference icon = status ? icons.getEnabledIcon() : icons.getDisabledIcon();
        Fragment f = new Fragment(id, "iconFragment", this);
        f.add(new Image("statusIcon", icon));
        return f;
    };

    class MessagePanel extends Panel {

        private static final long serialVersionUID = -3200098674603724915L;

        public MessagePanel(String id, ListItem<ModuleStatus> item) {
            super(id);

            Label name = new Label("name", new PropertyModel(item.getModel(), "name"));
            Label module = new Label("module", new PropertyModel(item.getModel(), "module"));
            Label component =
                    new Label(
                            "component",
                            new Model(item.getModelObject().getComponent().orElse("")));
            Label version =
                    new Label("version", new Model(item.getModelObject().getVersion().orElse("")));
            MultiLineLabel msgLabel =
                    new MultiLineLabel("msg", item.getModelObject().getMessage().orElse(""));

            add(name);
            add(module);
            add(component);
            add(version);
            add(msgLabel);
        }
    }
}
