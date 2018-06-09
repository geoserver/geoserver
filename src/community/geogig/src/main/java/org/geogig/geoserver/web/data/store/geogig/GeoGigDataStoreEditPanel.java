/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.data.store.geogig;

import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.AUTO_INDEXING;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.BRANCH;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geogig.geoserver.config.GeoServerGeoGigRepositoryResolver;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.web.repository.RepositoryEditFormPanel;
import org.geogig.geoserver.web.repository.RepositoryImportFormPanel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.locationtech.geogig.repository.RepositoryResolver;

public class GeoGigDataStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 330172801498702374L;

    private final DropDownChoice<String> repository;

    private final BranchSelectionPanel branch;

    private final ModalWindow modalWindow;

    private final IModel<String> repositoryUriModel;

    private final IModel<String> branchNameModel;

    private final String originalRepo, originalBranch;

    private final IModel<Boolean> autoIndexingModel;
    /**
     * @param componentId the wicket component id
     * @param storeEditForm the data store edit form, as provided by {@link DataAccessEditPage} and
     *     {@link DataAccessNewPage}
     */
    @SuppressWarnings("unchecked")
    public GeoGigDataStoreEditPanel(
            final String componentId, final Form<DataStoreInfo> storeEditForm) {
        super(componentId, storeEditForm);
        final IModel<DataStoreInfo> model = storeEditForm.getModel();
        setDefaultModel(model);

        final IModel<Map<String, Serializable>> paramsModel =
                new PropertyModel<>(model, "connectionParameters");

        this.repositoryUriModel = new MapModel(paramsModel, REPOSITORY.key);
        this.branchNameModel = new MapModel(paramsModel, BRANCH.key);
        this.autoIndexingModel = new MapModel(paramsModel, AUTO_INDEXING.key);

        this.originalRepo = repositoryUriModel.getObject();
        this.originalBranch = branchNameModel.getObject();

        add(repository = createRepositoryPanel());
        add(importExistingLink(storeEditForm));
        add(addNewLink(storeEditForm));

        add(branch = createBranchNameComponent(storeEditForm));
        add(createAutoIndexingPanel());
        add(modalWindow = new ModalWindow("modalWindow"));
    }

    private CheckBoxParamPanel createAutoIndexingPanel() {
        CheckBoxParamPanel checkBoxParamPanel =
                new CheckBoxParamPanel(
                        "autoIndexing",
                        autoIndexingModel,
                        new ResourceModel("autoIndexing", "Auto-Indexing"));
        checkBoxParamPanel.setOutputMarkupId(true);
        return checkBoxParamPanel;
    }

    private DropDownChoice<String> createRepositoryPanel() {

        IModel<List<String>> choices = new RepositoryListDettachableModel();

        RepoInfoChoiceRenderer choiceRenderer = new RepoInfoChoiceRenderer();
        DropDownChoice<String> choice =
                new DropDownChoice<>(
                        "geogig_repository", repositoryUriModel, choices, choiceRenderer);
        choice.setLabel(new ResourceModel("repository"));
        choice.setNullValid(true);
        choice.setRequired(true);
        choice.setOutputMarkupId(true);

        choice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    private static final long serialVersionUID = 6182000388125500580L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        String branchName = null;
                        // do not lose the original branch if the user is moving around the repo
                        // choices
                        if (Objects.equal(originalRepo, repositoryUriModel.getObject())) {
                            branchName = originalBranch;
                        }
                        branchNameModel.setObject(branchName);
                        branch.updateChoices(true, GeoGigDataStoreEditPanel.this.storeEditForm);
                        target.add(branch);
                    }
                });
        return choice;
    }

    private BranchSelectionPanel createBranchNameComponent(Form<DataStoreInfo> form) {

        final String panelId = "branch";
        BranchSelectionPanel selectionPanel;
        selectionPanel =
                new BranchSelectionPanel(panelId, repositoryUriModel, branchNameModel, form);
        selectionPanel.setOutputMarkupId(true);
        return selectionPanel;
    }

    private Component addNewLink(final Form<DataStoreInfo> storeEditForm) {
        AjaxSubmitLink link =
                new AjaxSubmitLink("addNew", storeEditForm) {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {

                        final RepositoryEditFormPanel panel;
                        panel =
                                new RepositoryEditFormPanel(modalWindow.getContentId()) {
                                    private static final long serialVersionUID =
                                            -2629733074852452891L;

                                    @SuppressWarnings("unchecked")
                                    @Override
                                    protected void saved(
                                            final RepositoryInfo info,
                                            final AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                        updateRepository((Form<DataStoreInfo>) form, target, info);
                                    }

                                    @Override
                                    protected void cancelled(AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                    }
                                };

                        modalWindow.setContent(panel);
                        modalWindow.setTitle(
                                new ResourceModel(
                                        "GeoGigDirectoryFormComponent.chooser.browseTitle"));
                        modalWindow.show(target);
                    }
                };
        return link;
    }

    private Component importExistingLink(Form<DataStoreInfo> storeEditForm) {

        AjaxSubmitLink link =
                new AjaxSubmitLink("importExisting", storeEditForm) {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public boolean getDefaultFormProcessing() {
                        return false;
                    }

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        final RepositoryImportFormPanel panel;
                        panel =
                                new RepositoryImportFormPanel(modalWindow.getContentId()) {
                                    private static final long serialVersionUID = 1L;

                                    @SuppressWarnings("unchecked")
                                    @Override
                                    protected void saved(
                                            final RepositoryInfo info,
                                            final AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                        updateRepository((Form<DataStoreInfo>) form, target, info);
                                    }

                                    @Override
                                    protected void cancelled(AjaxRequestTarget target) {
                                        modalWindow.close(target);
                                    }
                                };

                        modalWindow.setContent(panel);
                        modalWindow.setTitle(
                                new ResourceModel(
                                        "GeoGigDirectoryFormComponent.chooser.browseTitle"));
                        modalWindow.show(target);
                    }
                };
        return link;
    }

    @SuppressWarnings("unchecked")
    private void updateRepository(
            final Form<DataStoreInfo> form, AjaxRequestTarget target, RepositoryInfo info) {
        repository.setModelObject(GeoServerGeoGigRepositoryResolver.getURI(info.getRepoName()));
        branchNameModel.setObject(null);
        branch.updateChoices(true, form);
        target.add(repository);
        target.add(branch);

        IModel<DataStoreInfo> storeModel = form.getModel();
        String dataStoreName = storeModel.getObject().getName();
        if (Strings.isNullOrEmpty(dataStoreName)) {
            Component namePanel = form.get("dataStoreNamePanel");
            if (namePanel != null && namePanel instanceof TextParamPanel) {
                TextParamPanel paramPanel = (TextParamPanel) namePanel;
                paramPanel.getFormComponent().setModelObject(info.getRepoName());
                target.add(form);
            }
        }
    }

    private static class RepositoryListDettachableModel
            extends LoadableDetachableModel<List<String>> {
        private static final long serialVersionUID = 6664339867388245896L;

        @Override
        protected List<String> load() {
            List<RepositoryInfo> all = RepositoryManager.get().getAll();
            List<String> uris = new ArrayList<>(all.size());
            for (RepositoryInfo info : all) {
                uris.add(GeoServerGeoGigRepositoryResolver.getURI(info.getRepoName()));
            }
            Collections.sort(uris);
            return uris;
        }
    }

    private static class RepoInfoChoiceRenderer extends ChoiceRenderer<String> {
        private static final long serialVersionUID = -7350304450283044479L;

        @Override
        public Object getDisplayValue(String repoUriStr) {
            try {
                URI repoUri = new URI(repoUriStr);
                RepositoryResolver resolver = RepositoryResolver.lookup(repoUri);
                RepositoryInfo info =
                        RepositoryManager.get().getByRepoName(resolver.getName(repoUri));
                return info.getRepoName() + " (" + info.getMaskedLocation() + ")";
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public String getIdValue(String id, int index) {
            return id;
        }
    }
}
