/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static org.geogig.geoserver.config.RepositoryManager.isGeogigDirectory;

import java.io.File;
import java.net.URI;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geogig.geoserver.util.PostgresConnectionErrorHandler;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryEditPanel extends FormComponentPanel<RepositoryInfo> {

    private static final long serialVersionUID = -870873448379832051L;
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryEditPanel.class);

    private final GeoGigRepositoryInfoFormComponent config;

    public RepositoryEditPanel(
            final String wicketId, IModel<RepositoryInfo> model, final boolean isNew) {
        super(wicketId, model);

        config = new GeoGigRepositoryInfoFormComponent("repositoryConfig", model, isNew);
        config.setVisible(true);
        add(config);

        add(
                new IValidator<RepositoryInfo>() {

                    private static final long serialVersionUID = 224160688160723504L;

                    @Override
                    public void validate(IValidatable<RepositoryInfo> validatable) {
                        if (isNew) {
                            config.processInput();
                        }
                        ValidationError error = new ValidationError();
                        RepositoryInfo repo = validatable.getValue();
                        final URI location = repo.getLocation();
                        final RepositoryResolver resolver = RepositoryResolver.lookup(location);
                        final String scheme = location.getScheme();
                        final boolean nameExists =
                                RepositoryManager.get().repoExistsByName(repo.getRepoName());
                        if (isNew && nameExists) {
                            error.addKey("errRepositoryNameExists");
                        } else if ("file".equals(scheme)) {
                            File repoDir = new File(location);
                            final File parent = repoDir.getParentFile();
                            if (!parent.exists() || !parent.isDirectory()) {
                                error.addKey("errParentDoesntExist");
                            }
                            if (!parent.canWrite()) {
                                error.addKey("errParentReadOnly");
                            }
                            if (isNew) {
                                if (repoDir.exists()) {
                                    error.addKey("errDirectoryExists");
                                }
                            } else if (!isGeogigDirectory(repoDir)) {
                                error.addKey("notAGeogigRepository");
                            }
                        } else if ("postgresql".equals(scheme)) {
                            try {
                                if (isNew) {
                                    if (resolver.repoExists(location)) {
                                        error.addKey("errRepositoryExists");
                                    }
                                } else {
                                    // try to connect
                                    resolver.open(location);
                                }
                            } catch (Exception ex) {
                                // likely failed to connect
                                LOGGER.error("Failed to connect to PostgreSQL database", ex);
                                error.addKey("errCannotConnectToDatabase");
                                // find root cause
                                error.setVariable(
                                        "message", PostgresConnectionErrorHandler.getMessage(ex));
                            }
                        }
                        if (!error.getKeys().isEmpty()) {
                            validatable.error(error);
                        }
                    }
                });
    }

    @Override
    public void convertInput() {
        RepositoryInfo modelObject = getModelObject();
        setConvertedInput(modelObject);
    }
}
