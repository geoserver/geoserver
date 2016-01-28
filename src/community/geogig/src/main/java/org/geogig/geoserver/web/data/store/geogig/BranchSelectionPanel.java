/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.data.store.geogig;

import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.DataStoreInfo;
import org.locationtech.geogig.api.Ref;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;

public class BranchSelectionPanel extends FormComponentPanel<String> {
    private static final long serialVersionUID = 1L;

    private final DropDownChoice<String> choice;

    private final IModel<String> repositoryIdModel;

    private Supplier<RepositoryManager> manager = RepositoryManager.supplier();

    public BranchSelectionPanel(String id, IModel<String> repositoryIdModel,
            IModel<String> branchNameModel, Form<DataStoreInfo> storeEditForm) {
        super(id, branchNameModel);
        this.repositoryIdModel = repositoryIdModel;

        final List<String> choices = new ArrayList<String>();
        choice = new DropDownChoice<String>("branchDropDown", branchNameModel, choices);
        choice.setOutputMarkupId(true);
        choice.setNullValid(true);
        choice.setRequired(false);
        add(choice);
        updateChoices(false, null);

        final AjaxSubmitLink refreshLink = new AjaxSubmitLink("refresh", storeEditForm) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                onSubmit(target, form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateChoices(true, form);
                target.addComponent(BranchSelectionPanel.this.choice);
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
        final String repository = repositoryIdModel.getObject();
        if (REPOSITORY.sample != null && REPOSITORY.sample.equals(repository)) {
            return;
        }
        List<String> branchNames = new ArrayList<>();
        if (repository != null) {
            try {
                RepositoryManager manager = this.manager.get();
                List<Ref> branchRefs = manager.listBranches(repository);
                for (Ref branch : branchRefs) {
                    branchNames.add(branch.localName());
                }
            } catch (IOException e) {
                if (reportError) {
                    form.error("Could not list branches: " + e.getMessage());
                }
                branchNames = new ArrayList<String>();
            } catch (RuntimeException e) {
                if (reportError) {
                    form.error("Could not list branches: " + e.getMessage());
                }
                branchNames = new ArrayList<String>();
            }
            String current = (String) choice.getModelObject();
            if (current != null && !branchNames.contains(current)) {
                branchNames.add(0, current);
            }
        }
        choice.setChoices(branchNames);
    }
}
