/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.plumbing.TransactionBegin;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigAction;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigScope;
import org.locationtech.geogig.remotes.RemoteAddOp;
import org.locationtech.geogig.remotes.RemoteListOp;
import org.locationtech.geogig.remotes.RemoteRemoveOp;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.IndexInfo;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.impl.GeogigTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public abstract class RepositoryEditFormPanel extends Panel {

    private static final long serialVersionUID = -9001389048321749075L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryEditFormPanel.class);

    private RemotesListPanel remotes;

    private ConfigListPanel localConfig;

    private ConfigListPanel globalConfig;

    private IndexListPanel indexes;

    private ModalWindow popupWindow;

    private Form<RepositoryInfo> form;

    private boolean isNew = false;

    public RepositoryEditFormPanel(final String id) {
        this(id, null);
    }

    public RepositoryEditFormPanel(final String id, @Nullable IModel<RepositoryInfo> repoInfo) {
        super(id);
        isNew = repoInfo == null;
        if (isNew) {
            repoInfo = new Model<>(new RepositoryInfo());
        }
        setDefaultModel(repoInfo);

        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);

        form = new Form<>("repoForm", repoInfo);
        form.add(new RepositoryEditPanel("repo", repoInfo, isNew));

        form.add(addRemoteLink());
        List<RemoteInfo> remoteInfos = loadRemoteInfos(repoInfo.getObject());
        form.add(remotes = new RemotesListPanel("remotes", remoteInfos));

        form.add(addConfigLink(false));
        Map<String, String> localConfigMap = loadConfig(repoInfo.getObject(), false);
        form.add(localConfig = new ConfigListPanel("localConfig", localConfigMap));

        form.add(addConfigLink(true));
        Map<String, String> globalConfigMap = loadConfig(repoInfo.getObject(), true);
        form.add(globalConfig = new ConfigListPanel("globalConfig", globalConfigMap));

        List<IndexInfo> indexInfos = loadIndexes(repoInfo.getObject());
        form.add(indexes = new IndexListPanel("indexes", indexInfos));

        add(form);
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        form.add(feedback);

        form.add(
                new AjaxLink<Void>("cancel") {
                    private static final long serialVersionUID = 6220299771769708060L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        cancelled(target);
                    }
                });

        form.add(
                new AjaxSubmitLink("save", form) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        super.onError(target, form);
                        target.add(form);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        try {
                            RepositoryInfo repoInfo = (RepositoryInfo) form.getModelObject();
                            onSave(repoInfo, target);
                        } catch (IllegalArgumentException e) {
                            form.error(e.getMessage());
                            target.add(form);
                        }
                    }
                });
    }

    private List<RemoteInfo> loadRemoteInfos(RepositoryInfo repo) {
        String repoId = repo.getId();
        if (null == repoId) {
            return new ArrayList<>();
        }

        ArrayList<RemoteInfo> list = new ArrayList<>();
        Repository geogig;
        try {
            geogig = RepositoryManager.get().getRepository(repoId);
            if (geogig != null) {
                ImmutableList<Remote> geogigRemotes = geogig.command(RemoteListOp.class).call();
                list = RemoteInfo.fromList(geogigRemotes);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load Remotes for repository", e);
        }

        return list;
    }

    private Map<String, String> loadConfig(RepositoryInfo repo, boolean global) {
        String repoId = repo.getId();
        if (null == repoId) {
            return Maps.newHashMap();
        }

        Repository geogig;
        try {
            geogig = RepositoryManager.get().getRepository(repoId);
            if (geogig != null) {
                Optional<Map<String, String>> config =
                        geogig.command(ConfigOp.class) //
                                .setAction(ConfigAction.CONFIG_LIST) //
                                .setScope(global ? ConfigScope.GLOBAL : ConfigScope.LOCAL) //
                                .call();
                if (config.isPresent()) {
                    return config.get();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load config for repository", e);
        }

        return Maps.newHashMap();
    }

    private List<IndexInfo> loadIndexes(RepositoryInfo repo) {
        String repoId = repo.getId();
        if (null == repoId) {
            return Lists.newArrayList();
        }

        Repository geogig;
        try {
            geogig = RepositoryManager.get().getRepository(repoId);
            return geogig.indexDatabase().getIndexInfos();
        } catch (Exception e) {
            LOGGER.warn("Failed to load indexes for repository", e);
        }

        return Lists.newArrayList();
    }

    private Component addRemoteLink() {
        return new AjaxLink<Void>("addRemote") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                RemoteInfo ri = new RemoteInfo();
                IModel<RemoteInfo> model = new Model<>(ri);
                RemotesListPanel table = RepositoryEditFormPanel.this.remotes;
                RemoteEditPanel editPanel =
                        new RemoteEditPanel(popupWindow.getContentId(), model, popupWindow, table);

                popupWindow.setContent(editPanel);
                popupWindow.setTitle(new ResourceModel("RemoteEditPanel.title"));
                popupWindow.show(target);
            }
        };
    }

    private Component addConfigLink(boolean global) {
        return new AjaxLink<Void>(global ? "addGlobalConfig" : "addLocalConfig") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ConfigEntry config = new ConfigEntry();
                IModel<ConfigEntry> model = new Model<>(config);
                ConfigListPanel table =
                        global
                                ? RepositoryEditFormPanel.this.globalConfig
                                : RepositoryEditFormPanel.this.localConfig;
                ConfigEditPanel editPanel =
                        new ConfigEditPanel(popupWindow.getContentId(), model, popupWindow, table);

                popupWindow.setContent(editPanel);
                popupWindow.setTitle(new ResourceModel("ConfigEditPanel.title"));
                popupWindow.show(target);
            }
        };
    }

    private void onSave(RepositoryInfo repoInfo, AjaxRequestTarget target) {
        RepositoryManager manager = RepositoryManager.get();
        // update remotes
        Repository geogig;
        try {
            repoInfo = manager.save(repoInfo);
            geogig = manager.getRepository(repoInfo.getId());
        } catch (Exception e) {
            form.error(
                    "Unable to connect to repository "
                            + repoInfo.getLocation()
                            + "\n"
                            + e.getMessage());
            target.add(form);
            return;
        }

        Function<RemoteInfo, Integer> keyFunction =
                new Function<RemoteInfo, Integer>() {
                    @Override
                    public Integer apply(RemoteInfo r) {
                        return r.getId();
                    }
                };

        Map<Integer, RemoteInfo> currentRemotes =
                new HashMap<>(Maps.uniqueIndex(loadRemoteInfos(repoInfo), keyFunction));

        Set<RemoteInfo> newRemotes = Sets.newHashSet(remotes.getRemotes());
        if (!currentRemotes.isEmpty() || !newRemotes.isEmpty()) {
            GeogigTransaction tx = geogig.command(TransactionBegin.class).call();
            try {
                updateRemotes(tx, currentRemotes, newRemotes);
                tx.commit();
            } catch (Exception e) {
                try {
                    tx.abort();
                } finally {
                    form.error(e.getMessage());
                    target.add(form);
                }
                return;
            }
        }
        Map<String, String> currentLocalConfig = loadConfig(repoInfo, false);
        updateConfig(
                geogig.context(),
                currentLocalConfig,
                Lists.newArrayList(localConfig.getConfigs()),
                false);
        Map<String, String> currentGlobalConfig = loadConfig(repoInfo, true);
        updateConfig(
                geogig.context(),
                currentGlobalConfig,
                Lists.newArrayList(globalConfig.getConfigs()),
                true);
        saved(repoInfo, target);
    }

    private void updateRemotes(
            Context geogig, Map<Integer, RemoteInfo> currentRemotes, Set<RemoteInfo> newRemotes)
            throws Exception {

        // handle deletes first, in case a remote was deleted in the table and then a new one added
        // with the same name
        {
            Map<Integer, RemoteInfo> remaining = new HashMap<>();
            for (RemoteInfo ri : newRemotes) {
                if (ri.getId() != null) {
                    remaining.put(ri.getId(), ri);
                }
            }

            for (RemoteInfo deleted : currentRemotes.values()) {
                if (!remaining.containsKey(deleted.getId())) {
                    String name = deleted.getName();
                    try {
                        geogig.command(RemoteRemoveOp.class).setName(name).call();
                    } catch (RuntimeException e) {
                        throw new RuntimeException("Error deleting remote " + name, e);
                    }
                }
            }
        }

        for (RemoteInfo ri : newRemotes) {
            if (ri.getId() == null) { // its a new one
                RemoteAddOp cmd = geogig.command(RemoteAddOp.class);
                cmd.setName(ri.getName());
                cmd.setURL(ri.getURL());
                cmd.setUserName(ri.getUserName());
                cmd.setPassword(ri.getPassword());
                try {
                    cmd.call();
                } catch (RuntimeException e) {
                    throw new RuntimeException(
                            "Error adding remote " + ri.getName() + ": " + e.getMessage(), e);
                }
            } else {
                // handle upadtes
                RemoteInfo old = currentRemotes.remove(ri.getId());
                Preconditions.checkNotNull(old);
                if (old.equals(ri)) {
                    continue; // didn't change
                }
                try {
                    geogig.command(RemoteRemoveOp.class).setName(old.getName()).call();
                    RemoteAddOp addop = geogig.command(RemoteAddOp.class);
                    addop.setName(ri.getName())
                            .setURL(ri.getURL())
                            .setUserName(ri.getUserName())
                            .setPassword(ri.getPassword());
                    addop.call();
                } catch (RuntimeException e) {
                    throw new RuntimeException(
                            "Error updating remote " + ri.getName() + ": " + e.getMessage(), e);
                }
            }
        }
    }

    private void updateConfig(
            Context geogig,
            Map<String, String> currentConfig,
            List<ConfigEntry> newConfig,
            boolean global) {
        ConfigScope scope = global ? ConfigScope.GLOBAL : ConfigScope.LOCAL;
        for (ConfigEntry entry : newConfig) {
            if (currentConfig.containsKey(entry.getName())) {
                if (!currentConfig.get(entry.getName()).equals(entry.getValue())) {
                    // entry changed
                    geogig.command(ConfigOp.class) //
                            .setAction(ConfigAction.CONFIG_SET) //
                            .setName(entry.getName()) //
                            .setValue(entry.getValue()) //
                            .setScope(scope) //
                            .call();
                }
                currentConfig.remove(entry.getName());
            } else {
                // new entry
                geogig.command(ConfigOp.class) //
                        .setAction(ConfigAction.CONFIG_SET) //
                        .setName(entry.getName()) //
                        .setValue(entry.getValue()) //
                        .setScope(scope) //
                        .call();
            }
        }
        if (!isNew) {
            for (Entry<String, String> entry : currentConfig.entrySet()) {
                // removed entry
                geogig.command(ConfigOp.class) //
                        .setAction(ConfigAction.CONFIG_UNSET) //
                        .setName(entry.getKey()) //
                        .setScope(scope) //
                        .call();
            }
        }
    }

    protected abstract void saved(RepositoryInfo info, AjaxRequestTarget target);

    protected abstract void cancelled(AjaxRequestTarget target);
}
