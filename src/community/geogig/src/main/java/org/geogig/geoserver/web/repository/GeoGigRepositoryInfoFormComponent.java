/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.model.PGBeanModel;
import org.geogig.geoserver.model.RepoDirModel;
import org.geogig.geoserver.model.RepoNameModel;

/**
 * Wicket form panel to hold form components for configuring a GeoGig repository. This panel allows
 * for configuring a GeoGig repository URI via multiple child panels, one for each type of
 * configuration. Currently, only File/Directory based config and PostgreSQL backend configurations
 * are supported. The DropDownChoice switches the configuration types and hides/exposes fields
 * specific to the choice selected. Each choice should introduce a wrapping IModel implementation
 * that transforms the repository URI location field of {@link RepositoryInfo} into a data model
 * that maps configuration fields into UI components.
 */
class GeoGigRepositoryInfoFormComponent extends FormComponentPanel<RepositoryInfo> {

    private static final long serialVersionUID = 1L;

    static final String FILE_CONFIG = "File";
    static final String PG_CONFIG = "PostgreSQL";
    static final String DEFAULT_CONFIG = FILE_CONFIG;
    static final List<String> CONFIG_LIST = Arrays.asList(FILE_CONFIG, PG_CONFIG);

    private final TextField<String> name;
    private final DropDownChoice<String> dropdown;
    private final GeoGigDirectoryFormComponent directoryComponent;
    private final PostgresConfigFormPanel pgPanel;

    private WebMarkupContainer settingsContainer;

    public GeoGigRepositoryInfoFormComponent(String id, IModel<RepositoryInfo> model) {
        super(id, model);

        RepositoryInfo repoInfo = model.getObject();

        IModel<String> nameModel = new RepoNameModel(model);
        name = new TextField<>("repositoryName", nameModel);
        name.setRequired(true);
        //name.setLabel(new ResourceModel("name", "Repo name"));
        if (repoInfo.getRepoName() != null) {
            name.setModelObject(repoInfo.getRepoName());
        }
        add(name);

        // add the dropdown to switch between configurations
        dropdown = new DropDownChoice<>("configChoice", new DropDownModel(model), CONFIG_LIST);
        dropdown.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                final String value = dropdown.getModelObject();
                directoryComponent.setVisible(FILE_CONFIG.equals(value));
                pgPanel.setVisible(PG_CONFIG.equals(value));
                target.addComponent(settingsContainer);
            }
        });
        dropdown.setOutputMarkupId(true);
        dropdown.setNullValid(true);
        dropdown.setRequired(true);
        add(dropdown);

        settingsContainer = new WebMarkupContainer("settingsContainer");
        settingsContainer.setOutputMarkupId(true);
        add(settingsContainer);

        pgPanel = new PostgresConfigFormPanel("pgPanel", new PGBeanModel(model));
        pgPanel.setVisible(PG_CONFIG.equals(dropdown.getModelObject()));
        settingsContainer.add(pgPanel);

        directoryComponent = new GeoGigDirectoryFormComponent("parentDirectory",
                new RepoDirModel(model));
        directoryComponent.setOutputMarkupId(true);
        directoryComponent.setVisible(FILE_CONFIG.equals(dropdown.getModelObject()));
        settingsContainer.add(directoryComponent);

    }

    @Override
    protected void convertInput() {
        RepositoryInfo modelObject = getModelObject();
        if (PG_CONFIG.equals(dropdown.getModelValue())) {
            // PG config used
            PostgresConfigBean bean = pgPanel.getConvertedInput();
            // build a URI out of the config
            URI uri = bean.toUri(name.getConvertedInput().trim());
            modelObject.setLocation(uri);
        } else if (FILE_CONFIG.equals(dropdown.getModelValue())) {
            // local directory used
            String path = directoryComponent.getConvertedInput().trim();
            String repoId = name.getConvertedInput().trim();
            Path uriPath = Paths.get(path, repoId);
            modelObject.setLocation(uriPath.toUri());
        }
        setConvertedInput(modelObject);
    }

    class DropDownModel implements IModel<String> {

        private final IModel<RepositoryInfo> repoModel;
        private String type;

        public DropDownModel(IModel<RepositoryInfo> repoModel) {
            this.repoModel = repoModel;
        }

        @Override
        public String getObject() {
            if (type == null) {
                // get the type from the model
                RepositoryInfo repo = repoModel.getObject();
                URI location = repo != null ? repo.getLocation() : null;
                if (location != null) {
                    if (null != location.getScheme()) // if the URI is Postgres...
                    {
                        switch (location.getScheme()) {
                            case "postgresql":
                                type = PG_CONFIG;
                                break;
                            case "file":
                                type = FILE_CONFIG;
                                break;
                            default:
                                type = DEFAULT_CONFIG;
                                break;
                        }
                    }
                }
            }
            return type;
        }

        @Override
        public void setObject(String object) {
            type = object;
        }

        @Override
        public void detach() {
            if (repoModel != null) {
                repoModel.detach();
            }
            type = null;
        }

    }
}
