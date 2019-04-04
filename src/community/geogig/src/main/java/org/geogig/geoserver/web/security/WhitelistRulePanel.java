/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.security;

import static com.google.common.collect.Lists.newArrayList;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geogig.geoserver.config.ConfigStore;
import org.geogig.geoserver.config.WhitelistRule;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;

public class WhitelistRulePanel extends GeoServerTablePanel<WhitelistRule> {

    private static final long serialVersionUID = 6946747039214324528L;

    private final ModalWindow window;

    private final WhitelistRulesProvider provider;

    public WhitelistRulePanel(String id, ModalWindow window) {
        super(id, new WhitelistRulesProvider());
        super.setOutputMarkupId(true);
        super.setSelectable(false);
        super.setSortable(true);
        super.setFilterable(true);
        super.setFilterVisible(true);
        super.setPageable(true);
        this.provider = (WhitelistRulesProvider) super.getDataProvider();
        this.window = window;
    }

    public void save() {
        provider.save();
    }

    public void add(WhitelistRule rule) {
        provider.add(rule);
    }

    public List<WhitelistRule> getRules() {
        return provider.getItems();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Component getComponentForProperty(
            String id, final IModel<WhitelistRule> model, Property<WhitelistRule> property) {
        if (property == WhitelistRulesProvider.NAME) {
            return new Label(id, property.getModel(model));
        } else if (property == WhitelistRulesProvider.PATTERN) {
            return new Label(id, property.getModel(model));
        } else if (property == WhitelistRulesProvider.REQUIRE_SSL) {
            final WhitelistRule rule = (WhitelistRule) model.getObject();
            @SuppressWarnings("deprecation")
            Fragment fragment = new Fragment(id, "image.cell", this);
            if (rule.isRequireSSL()) {
                fragment.add(
                        new Image(
                                "display",
                                new PackageResourceReference(getClass(), "../lock.png")));
            } else {
                fragment.add(
                        new Image(
                                "display",
                                new PackageResourceReference(getClass(), "../lock_open.png")));
            }
            return fragment;
        } else if (property == WhitelistRulesProvider.EDIT) {
            ImageAjaxLink link =
                    new ImageAjaxLink(
                            id,
                            new PackageResourceReference(
                                    GeoServerApplication.class, "img/icons/silk/pencil.png")) {

                        private static final long serialVersionUID = 4467715973193154831L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            window.setInitialHeight(360);
                            window.setInitialWidth(400);
                            window.setTitle(new Model<>("Edit whitelist rule"));
                            window.setContent(
                                    new WhitelistRuleEditor(
                                            window.getContentId(),
                                            model,
                                            window,
                                            WhitelistRulePanel.this,
                                            false));
                            window.show(target);
                        }
                    };
            return link;
        } else if (property == WhitelistRulesProvider.REMOVE) {
            final WhitelistRule rule = (WhitelistRule) model.getObject();
            ImageAjaxLink link =
                    new ImageAjaxLink(
                            id,
                            new PackageResourceReference(
                                    GeoServerApplication.class, "img/icons/silk/delete.png")) {

                        private static final long serialVersionUID = 9069782618988848563L;

                        @Override
                        protected void onClick(AjaxRequestTarget target) {
                            provider.remove(rule);
                            provider.save();
                            target.add(WhitelistRulePanel.this);
                        }
                    };
            // link.getImage().add(new AttributeModifier("alt", true, new
            // ParamResourceModel("AbstractLayerGroupPage.th.remove", link)));
            return link;
        } else {
            throw new IllegalArgumentException(
                    "Property " + property + " is not associated with this component.");
        }
    }

    private static class WhitelistRulesProvider extends GeoServerDataProvider<WhitelistRule> {

        private static final long serialVersionUID = 7545140184962032147L;

        private List<WhitelistRule> rules;

        @Override
        protected List<Property<WhitelistRule>> getProperties() {
            return PROPERTIES;
        }

        public void add(WhitelistRule rule) {
            getItems().add(rule);
        }

        public void remove(WhitelistRule rule) {
            getItems().remove(rule);
        }

        public void save() {
            final ConfigStore configStore;
            configStore = (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
            // make sure rules is set
            getItems();
            configStore.saveWhitelist(rules);
        }

        @Override
        protected List<WhitelistRule> getItems() {
            if (rules == null) {
                ConfigStore configStore =
                        (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
                try {
                    rules = new ArrayList<>(configStore.getWhitelist());
                } catch (IOException e) {
                    rules = newArrayList();
                }
            }
            return rules;
        }

        private static final Property<WhitelistRule> EDIT = new PropertyPlaceholder<>("");

        private static final Property<WhitelistRule> REMOVE = new PropertyPlaceholder<>("");

        private static final Property<WhitelistRule> NAME = new BeanProperty<>("Name", "name");

        private static final Property<WhitelistRule> PATTERN =
                new BeanProperty<>("Pattern", "pattern");

        private static final Property<WhitelistRule> REQUIRE_SSL =
                new BeanProperty<>("Require SSL", "requireSSL");

        private static final List<Property<WhitelistRule>> PROPERTIES =
                ImmutableList.of(NAME, PATTERN, REQUIRE_SSL, EDIT, REMOVE);
    }
}
