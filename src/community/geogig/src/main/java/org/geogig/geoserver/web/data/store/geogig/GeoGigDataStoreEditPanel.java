/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.data.store.geogig;

import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.BRANCH;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.RESOLVER_CLASS_NAME;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geogig.geoserver.config.GeoServerStoreRepositoryResolver;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.web.repository.DirectoryChooser;
import org.geogig.geoserver.web.repository.RepositoryEditFormPanel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

public class GeoGigDataStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 330172801498702374L;

    private final DropDownChoice<String> repository;

    private final BranchSelectionPanel branch;

    private final ModalWindow modalWindow;

    private IModel<String> repositoryIdModel;

    private IModel<String> branchNameModel;

    private String originalRepo, originalBranch;

    /**
     * @param componentId the wicket component id
     * @param storeEditForm the data store edit form, as provided by {@link DataAccessEditPage} and
     *        {@link DataAccessNewPage}
     */
    @SuppressWarnings("unchecked")
    public GeoGigDataStoreEditPanel(final String componentId,
            final Form<DataStoreInfo> storeEditForm) {
        super(componentId, storeEditForm);
        final IModel<DataStoreInfo> model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, Serializable>> paramsModel = new PropertyModel<>(model,
                "connectionParameters");
        paramsModel.getObject().put(RESOLVER_CLASS_NAME.key,
                GeoServerStoreRepositoryResolver.class.getName());

        this.repositoryIdModel = new MapModel(paramsModel, REPOSITORY.key);
        this.branchNameModel = new MapModel(paramsModel, BRANCH.key);

        this.originalRepo = repositoryIdModel.getObject();
        this.originalBranch = branchNameModel.getObject();

        add(repository = createRepositoryPanel());
        add(importExistingLink(storeEditForm));
        add(addNewLink(storeEditForm));

        add(branch = createBranchNameComponent(storeEditForm));

        add(modalWindow = new ModalWindow("modalWindow"));
    }

    private DropDownChoice<String> createRepositoryPanel() {

        IModel<List<String>> choices = new RepositoryListDettachableModel();

        RepoInfoChoiceRenderer choiceRenderer = new RepoInfoChoiceRenderer();
        DropDownChoice<String> choice = new DropDownChoice<String>("geogig_repository",
                repositoryIdModel, choices, choiceRenderer);
        choice.setLabel(new ResourceModel("repository"));
        choice.setNullValid(true);
        choice.setRequired(true);
        choice.setOutputMarkupId(true);

        choice.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            private static final long serialVersionUID = 6182000388125500580L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String branchName = null;
                // do not lose the original branch if the user is moving around the repo choices
                if (Objects.equal(originalRepo, repositoryIdModel.getObject())) {
                    branchName = originalBranch;
                }
                branchNameModel.setObject(branchName);
                branch.updateChoices(true, GeoGigDataStoreEditPanel.this.storeEditForm);
                target.addComponent(branch);
            }
        });
        return choice;
    }

    private BranchSelectionPanel createBranchNameComponent(Form<DataStoreInfo> form) {

        final String panelId = "branch";
        BranchSelectionPanel selectionPanel;
        selectionPanel = new BranchSelectionPanel(panelId, repositoryIdModel, branchNameModel, form);
        selectionPanel.setOutputMarkupId(true);
        return selectionPanel;
    }

    private Component addNewLink(final Form<DataStoreInfo> storeEditForm) {
        AjaxSubmitLink link = new AjaxSubmitLink("addNew", storeEditForm) {

            private static final long serialVersionUID = 1242472443848716943L;

            @Override
            public boolean getDefaultFormProcessing() {
                return false;
            }

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                final RepositoryEditFormPanel panel;
                panel = new RepositoryEditFormPanel(modalWindow.getContentId()) {
                    private static final long serialVersionUID = -2629733074852452891L;

                    @SuppressWarnings("unchecked")
                    @Override
                    protected void saved(final RepositoryInfo info, final AjaxRequestTarget target) {
                        modalWindow.close(target);
                        updateRepository((Form<DataStoreInfo>) form, target, info);
                    }

                    @Override
                    protected void cancelled(AjaxRequestTarget target) {
                        modalWindow.close(target);
                    }
                };

                modalWindow.setContent(panel);
                modalWindow.setTitle(new ResourceModel(
                        "GeoGigDirectoryFormComponent.chooser.browseTitle"));
                modalWindow.show(target);
            }

        };
        return link;
    }

    protected Component importExistingLink(Form<DataStoreInfo> storeEditForm) {

        AjaxSubmitLink link = new AjaxSubmitLink("importExisting", storeEditForm) {

            private static final long serialVersionUID = 1242472443848716943L;

            @Override
            public boolean getDefaultFormProcessing() {
                return false;
            }

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                DirectoryChooser chooser;
                chooser = new DirectoryChooser(modalWindow.getContentId(), new Model<File>()) {

                    private static final long serialVersionUID = 1L;

                    @SuppressWarnings("unchecked")
                    @Override
                    protected void geogigDirectoryClicked(final File file, AjaxRequestTarget target) {
                        // clear the raw input of the field won't show the new model value
                        RepositoryManager manager = RepositoryManager.get();
                        RepositoryInfo info = new RepositoryInfo();
                        info.setLocation(file.getAbsolutePath());
                        info = manager.save(info);
                        modalWindow.close(target);
                        updateRepository((Form<DataStoreInfo>) form, target, info);
                    }
                };
                chooser.setFileTableHeight(null);
                modalWindow.setContent(chooser);
                modalWindow.setTitle(new ResourceModel(
                        "GeoGigDirectoryFormComponent.chooser.browseTitle"));
                modalWindow.show(target);
            }

        };
        return link;
    }

    @SuppressWarnings("unchecked")
    private void updateRepository(final Form<DataStoreInfo> form, AjaxRequestTarget target,
            RepositoryInfo info) {
        repository.setModelObject(info.getId());
        branchNameModel.setObject(null);
        branch.updateChoices(true, form);
        target.addComponent(repository);
        target.addComponent(branch);

        IModel<DataStoreInfo> storeModel = form.getModel();
        String name = storeModel.getObject().getName();
        if (Strings.isNullOrEmpty(name)) {
            Component namePanel = form.get("dataStoreNamePanel");
            if (namePanel != null && namePanel instanceof TextParamPanel) {
                TextParamPanel paramPanel = (TextParamPanel) namePanel;
                paramPanel.getFormComponent().setModelObject(info.getName());
                target.addComponent(form);
            }
        }
    }

    private static class RepositoryListDettachableModel extends
            LoadableDetachableModel<List<String>> {
        private static final long serialVersionUID = 6664339867388245896L;

        @Override
        protected List<String> load() {
            List<RepositoryInfo> all = RepositoryManager.get().getAll();
            List<String> ids = new ArrayList<>(all.size());
            for (RepositoryInfo info : all) {
                ids.add(info.getId());
            }
            Collections.sort(ids);
            return ids;
        }
    }

    private static class RepoInfoChoiceRenderer implements IChoiceRenderer<String> {
        private static final long serialVersionUID = -7350304450283044479L;

        @Override
        public Object getDisplayValue(String id) {
            RepositoryInfo info;
            try {
                info = RepositoryManager.get().get(id);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            return info.getName() + " (" + info.getLocation() + ")";
        }

        @Override
        public String getIdValue(String id, int index) {
            return id;
        }
    }

}
