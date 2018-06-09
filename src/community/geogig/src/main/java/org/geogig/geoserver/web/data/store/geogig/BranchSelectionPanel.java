/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.data.store.geogig;

import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.DataStoreInfo;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.repository.RepositoryResolver;

public class BranchSelectionPanel extends FormComponentPanel<String> {
    private static final long serialVersionUID = 1L;

    private final DropDownChoice<String> choice;

    private final IModel<String> repositoryUriModel;

    private transient Supplier<RepositoryManager> manager = () -> RepositoryManager.get();

    public BranchSelectionPanel(
            String id,
            IModel<String> repositoryUriModel,
            IModel<String> branchNameModel,
            Form<DataStoreInfo> storeEditForm) {
        super(id, branchNameModel);
        this.repositoryUriModel = repositoryUriModel;

        final List<String> choices = new ArrayList<String>();
        choice = new DropDownChoice<String>("branchDropDown", branchNameModel, choices);
        choice.setOutputMarkupId(true);
        choice.setNullValid(true);
        choice.setRequired(false);
        add(choice);
        updateChoices(false, null);

        final AjaxSubmitLink refreshLink =
                new AjaxSubmitLink("refresh", storeEditForm) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        onSubmit(target, form);
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        updateChoices(true, form);
                        target.add(BranchSelectionPanel.this.choice);
                    }
                };
        add(refreshLink);
    }

    @Override
    public void convertInput() {
        choice.processInput();
        String branch = choice.getConvertedInput();
        setModelObject(branch);
        setConvertedInput(branch);
    }

    @VisibleForTesting
    void setRepositoryManager(Supplier<RepositoryManager> supplier) {
        this.manager = supplier;
    }

    public void updateChoices(boolean reportError, Form<?> form) {
        final String repoUriStr = repositoryUriModel.getObject();
        if (REPOSITORY.sample != null && REPOSITORY.sample.equals(repoUriStr)) {
            return;
        }
        List<String> branchNames = new ArrayList<>();
        if (repoUriStr != null) {
            try {
                RepositoryManager manager = this.manager.get();
                URI repoURI = new URI(repoUriStr);
                RepositoryResolver resolver = RepositoryResolver.lookup(repoURI);
                String repoName = resolver.getName(repoURI);
                RepositoryInfo repoInfo = manager.getByRepoName(repoName);
                if (repoInfo != null) {
                    String repoId = repoInfo.getId();
                    List<Ref> branchRefs = manager.listBranches(repoId);
                    for (Ref branch : branchRefs) {
                        branchNames.add(branch.localName());
                    }
                }
            } catch (IOException | URISyntaxException e) {
                if (reportError) {
                    form.error("Could not list branches: " + e.getMessage());
                }
            }
            String current = (String) choice.getModelObject();
            if (current != null && !branchNames.contains(current)) {
                branchNames.add(0, current);
            }
        }
        choice.setChoices(branchNames);
    }
}
