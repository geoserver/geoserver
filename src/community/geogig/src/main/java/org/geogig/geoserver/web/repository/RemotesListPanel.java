/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.DefaultItemReuseStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.SymRef;

public class RemotesListPanel extends GeoServerTablePanel<RemoteInfo> {

    private static final long serialVersionUID = 5957961031378924960L;

    private static final PackageResourceReference REMOVE_ICON =
            new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/delete.png");

    private final ModalWindow popupWindow;

    private final GeoServerDialog dialog;

    private final RemotesProvider provider;

    private final FeedbackPanel pingFeedbackPanel;

    public RemotesListPanel(final String id, final List<RemoteInfo> remotes) {
        super(id, new RemotesProvider(remotes), false);
        super.setFilterable(false);
        this.provider = (RemotesProvider) super.getDataProvider();
        this.setOutputMarkupId(true);
        // set the reuse strategy, otherwise the list does not get updated when the popup window
        // closes
        this.setItemReuseStrategy(DefaultItemReuseStrategy.getInstance());
        // the popup window for messages
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);

        add(dialog = new GeoServerDialog("dialog"));
        add(pingFeedbackPanel = new FeedbackPanel("feedback"));
        pingFeedbackPanel.setOutputMarkupId(true);
    }

    public void add(RemoteInfo remote) {
        this.provider.getItems().add(remote);
    }

    public Iterable<RemoteInfo> getRemotes() {
        return provider.getItems();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected Component getComponentForProperty(
            String id, IModel<RemoteInfo> itemModel, Property<RemoteInfo> property) {

        IModel<RemoteInfo> model = itemModel;

        if (property == RemotesProvider.NAME) {
            return nameLink(id, itemModel);
        } else if (property == RemotesProvider.URL) {
            String location = (String) RemotesProvider.URL.getModel(itemModel).getObject();
            Label label = new Label(id, location);
            // label.add(new SimpleAttributeModifier("style", "word-wrap:break-word;"));
            return label;
        } else if (property == RemotesProvider.PINGLINK) {
            return new RemotePingLink(id, model);
        } else if (property == RemotesProvider.REMOVELINK) {
            return removeLink(id, itemModel);
        }
        return null;
    }

    private Component nameLink(String id, IModel<RemoteInfo> itemModel) {
        @SuppressWarnings("unchecked")
        IModel<?> nameModel = RemotesProvider.NAME.getModel(itemModel);

        SimpleAjaxLink<RemoteInfo> link =
                new SimpleAjaxLink<RemoteInfo>(id, itemModel, nameModel) {
                    private static final long serialVersionUID = -18292070541084372L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<RemoteInfo> model = this.getModel();
                        RemotesListPanel table = RemotesListPanel.this;
                        RemoteEditPanel editPanel =
                                new RemoteEditPanel(
                                        popupWindow.getContentId(), model, popupWindow, table);

                        popupWindow.setContent(editPanel);
                        popupWindow.setTitle(new ResourceModel("RemoteEditPanel.title"));
                        popupWindow.show(target);
                    }
                };
        return link;
    }

    protected Component removeLink(final String id, final IModel<RemoteInfo> itemModel) {

        return new ImageAjaxLink(id, REMOVE_ICON) {

            private static final long serialVersionUID = -3061812114487970427L;

            private final IModel<RemoteInfo> model = itemModel;

            @Override
            public void onClick(AjaxRequestTarget target) {
                GeoServerDialog dialog = RemotesListPanel.this.dialog;
                dialog.setTitle(
                        new ParamResourceModel("RemotesListPanel.confirmRemoval.title", this));

                dialog.showOkCancel(
                        target,
                        new GeoServerDialog.DialogDelegate() {
                            private static final long serialVersionUID = -450822090965263894L;

                            @Override
                            protected Component getContents(String id) {
                                return new ConfirmRemovePanel(id, model);
                            }

                            @Override
                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                boolean closeConfirmDialog = true;

                                final RemoteInfo remote = model.getObject();
                                List<RemoteInfo> items = RemotesListPanel.this.provider.getItems();
                                items.remove(remote);
                                target.add(RemotesListPanel.this);
                                return closeConfirmDialog;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target) {
                                target.add(RemotesListPanel.this);
                            }
                        });
            }
        };
    }

    static class ConfirmRemovePanel extends Panel {

        private static final long serialVersionUID = 653769682579422516L;

        public ConfirmRemovePanel(String id, IModel<RemoteInfo> remote) {
            super(id);

            add(
                    new Label(
                            "aboutRemoveMsg",
                            new ParamResourceModel(
                                    "RemotesListPanel$ConfirmRemovePanel.aboutRemove",
                                    this,
                                    remote.getObject().getName())));
        }
    }

    static class RemotesProvider extends GeoServerDataProvider<RemoteInfo> {

        private static final long serialVersionUID = 4883560661021761394L;

        static final Property<RemoteInfo> NAME = new BeanProperty<>("name", "name");

        static final Property<RemoteInfo> URL = new BeanProperty<>("URL", "URL");

        static final Property<RemoteInfo> PINGLINK =
                new AbstractProperty<RemoteInfo>("") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getPropertyValue(RemoteInfo item) {
                        return Boolean.TRUE;
                    }

                    @Override
                    public boolean isSearchable() {
                        return false;
                    }
                };

        static final Property<RemoteInfo> REMOVELINK =
                new AbstractProperty<RemoteInfo>("remove") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getPropertyValue(RemoteInfo item) {
                        return Boolean.TRUE;
                    }

                    @Override
                    public boolean isSearchable() {
                        return false;
                    }
                };

        final List<Property<RemoteInfo>> PROPERTIES =
                Arrays.asList(NAME, URL, PINGLINK, REMOVELINK);

        private final ArrayList<RemoteInfo> items;

        public RemotesProvider(final List<RemoteInfo> remotes) {
            this.items = new ArrayList<>(remotes);
        }

        @Override
        protected List<RemoteInfo> getItems() {
            return items;
        }

        @Override
        protected List<Property<RemoteInfo>> getProperties() {
            return PROPERTIES;
        }

        @Override
        protected Comparator<RemoteInfo> getComparator(SortParam sort) {
            return super.getComparator(sort);
        }

        @Override
        public IModel<RemoteInfo> newModel(RemoteInfo object) {
            return new Model<>(object);
        }
    }

    private final class RemotePingLink extends SimpleAjaxLink<RemoteInfo> {
        private static final long serialVersionUID = 1L;

        private RemotePingLink(String id, IModel<RemoteInfo> model) {
            super(id, model, new Model<>("ping"));
        }

        @Override
        protected void onClick(AjaxRequestTarget target) {
            RemoteInfo remoteInfo = getModelObject();
            String location = remoteInfo.getURL();
            String username = remoteInfo.getUserName();
            String pwd = remoteInfo.getPassword();
            try {
                Ref head = RepositoryManager.pingRemote(location, username, pwd);
                String headTarget;
                if (head instanceof SymRef) {
                    headTarget = ((SymRef) head).getTarget();
                } else {
                    headTarget = head.getObjectId().toString();
                }
                pingFeedbackPanel.info("Connection suceeded. HEAD is at " + headTarget);
            } catch (Exception e) {
                pingFeedbackPanel.error(e.getMessage());
            }
            target.add(pingFeedbackPanel);
        }
    }
}
