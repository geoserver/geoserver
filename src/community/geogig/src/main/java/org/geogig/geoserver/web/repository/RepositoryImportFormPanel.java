/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;

/**
 * The main Wicket panel for importing an existing GeoGig repository. This panel is very similar to
 * {@link RepositoryEditFormPanel}, but it is not concerned with repository remotes. Remotes of an
 * imported GeoGig repository will be included with the repository import. Once imported, users can
 * navigate to the Edit page to edit remotes.
 */
public abstract class RepositoryImportFormPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private Form<RepositoryInfo> form;

    public RepositoryImportFormPanel(String id) {
        this(id, null);
    }

    public RepositoryImportFormPanel(String id, IModel<RepositoryInfo> repoInfo) {
        super(id);

        if (repoInfo == null) {
            repoInfo = new Model<>(new RepositoryInfo());
        }
        setDefaultModel(repoInfo);

        form = new Form<>("repoForm", repoInfo);
        form.add(new RepositoryImportPanel("repo", repoInfo));
        add(form);
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        form.add(feedback);

        form.add(
                new AjaxLink<Void>("cancel") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        cancelled(target);
                    }
                });

        form.add(
                new AjaxSubmitLink("import", form) {
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

    private void onSave(RepositoryInfo repoInfo, AjaxRequestTarget target) {
        RepositoryManager manager = RepositoryManager.get();
        repoInfo = manager.save(repoInfo);
        saved(repoInfo, target);
    }

    protected abstract void saved(RepositoryInfo info, AjaxRequestTarget target);

    protected abstract void cancelled(AjaxRequestTarget target);
}
