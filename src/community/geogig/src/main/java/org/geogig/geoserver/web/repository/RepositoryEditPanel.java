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
import org.locationtech.geogig.repository.RepositoryResolver;

public class RepositoryEditPanel extends FormComponentPanel<RepositoryInfo> {

    private static final long serialVersionUID = -870873448379832051L;

    private final GeoGigRepositoryInfoFormComponent config;

    public RepositoryEditPanel(final String wicketId, IModel<RepositoryInfo> model,
            final boolean isNew) {
        super(wicketId, model);
        
        config = new GeoGigRepositoryInfoFormComponent("repositoryConfig", model);
        config.setVisible(true);
        add(config);

        add(new IValidator<RepositoryInfo>() {

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
                if ("file".equals(location.getScheme())) {
                    File repoDir = new File(location);
                    final File parent = repoDir.getParentFile();
                    if (!parent.exists() || !parent.isDirectory()) {
                        error.addMessageKey("errParentDoesntExist");
                    }
                    if (!parent.canWrite()) {
                        error.addMessageKey("errParentReadOnly");
                    }
                    if (isNew) {
                        if (repoDir.exists()) {
                            error.addMessageKey("errDirectoryExists");
                        }
                    } else if (!isGeogigDirectory(repoDir)) {
                        error.addMessageKey("notAGeogigRepository");
                    }
                }
                if (!error.getKeys().isEmpty()) {
                    validatable.error(error);
                }
            }
        });
    }

    @Override
    protected void convertInput() {
        RepositoryInfo modelObject = getModelObject();
        setConvertedInput(modelObject);
    }
}
