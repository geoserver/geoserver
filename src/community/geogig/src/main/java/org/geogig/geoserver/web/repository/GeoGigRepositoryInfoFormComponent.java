/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.io.Serializable;
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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geogig.geoserver.config.PostgresConfigBean;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.model.PGBeanModel;
import org.geogig.geoserver.model.RepoDirModel;
import org.geogig.geoserver.model.RepoNameModel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;

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

    static final String DIRECTORY_CONFIG = "Directory";
    static final String PG_CONFIG = "PostgreSQL";
    static final String DEFAULT_CONFIG = DIRECTORY_CONFIG;
    static final List<String> CONFIG_LIST = Arrays.asList(DIRECTORY_CONFIG, PG_CONFIG);

    private final TextParamPanel repoNamePanel;
    private final DropDownChoiceParamPanel dropdownPanel;
    private final GeoGigDirectoryFormComponent directoryComponent;
    private final PostgresConfigFormPanel pgPanel;

    private WebMarkupContainer settingsContainer;

    public GeoGigRepositoryInfoFormComponent(String id, IModel<RepositoryInfo> model) {
        super(id, model);

        IModel<String> nameModel = new RepoNameModel(model);
        repoNamePanel = new TextParamPanel("repositoryNamePanel", nameModel,
                new ResourceModel("GeoGigRepositoryInfoFormComponent.repositoryName",
                        "Repository Name"), true);
        add(repoNamePanel);

        // add the dropdown to switch between configurations
        dropdownPanel = new DropDownChoiceParamPanel("configChoicePanel", new DropDownModel(model),
                new ResourceModel("GeoGigRepositoryInfoFormComponent.repositoryType",
                        "Repository Type"), CONFIG_LIST, true);
        final DropDownChoice<Serializable> dropDownChoice = dropdownPanel.getFormComponent();
        dropDownChoice.add(new AjaxFormComponentUpdatingBehavior("onChange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                final String value = dropDownChoice.getModelObject().toString();
                directoryComponent.setVisible(DIRECTORY_CONFIG.equals(value));
                pgPanel.setVisible(PG_CONFIG.equals(value));
                target.addComponent(settingsContainer);
            }
        });
        add(dropdownPanel);

        settingsContainer = new WebMarkupContainer("settingsContainer");
        settingsContainer.setOutputMarkupId(true);
        add(settingsContainer);

        pgPanel = new PostgresConfigFormPanel("pgPanel", new PGBeanModel(model));
        pgPanel.setVisible(PG_CONFIG.equals(dropDownChoice.getModelObject().toString()));
        settingsContainer.add(pgPanel);

        directoryComponent = new GeoGigDirectoryFormComponent("parentDirectory",
                new RepoDirModel(model));
        directoryComponent.setOutputMarkupId(true);
        directoryComponent
                .setVisible(DIRECTORY_CONFIG.equals(dropDownChoice.getModelObject().toString()));
        settingsContainer.add(directoryComponent);

    }

    @Override
    protected void convertInput() {
        RepositoryInfo modelObject = getModelObject();
        final String repoTypeChoice = dropdownPanel.getFormComponent().getConvertedInput()
                .toString();
        if (null != repoTypeChoice) {
            switch (repoTypeChoice) {
                case PG_CONFIG:
                    // PG config used
                    PostgresConfigBean bean = pgPanel.getConvertedInput();
                    // build a URI out of the config
                    URI uri = bean.toUri(repoNamePanel.getFormComponent().getConvertedInput()
                            .toString().trim());
                    modelObject.setLocation(uri);
                    break;
                case DIRECTORY_CONFIG:
                    // local directory used
                    String path = directoryComponent.getConvertedInput().trim();
                    String repoId = repoNamePanel.getFormComponent().getConvertedInput().toString()
                            .trim();
                    Path uriPath = Paths.get(path, repoId);
                    modelObject.setLocation(uriPath.toUri());
                    break;
                default:
                    throw new IllegalStateException(
                            String.format("Unknown repositry type '%s', expected one of %s, %s",
                                    repoTypeChoice, PG_CONFIG, DIRECTORY_CONFIG));
            }
        }
        setConvertedInput(modelObject);
    }

    class DropDownModel implements IModel<Serializable> {

        private final IModel<RepositoryInfo> repoModel;
        private String type;

        public DropDownModel(IModel<RepositoryInfo> repoModel) {
            this.repoModel = repoModel;
            if (null == repoModel || null == repoModel.getObject() || null == repoModel.getObject()
                    .getLocation()) {
                type = DEFAULT_CONFIG;
            }
        }

        @Override
        public Serializable getObject() {
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
                                type = DIRECTORY_CONFIG;
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
        public void setObject(Serializable object) {
            type = object.toString();
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
