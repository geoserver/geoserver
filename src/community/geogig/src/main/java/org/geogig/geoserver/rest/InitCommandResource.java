/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.geogig.geoserver.rest.InitRequestHandler.AUTHOR_EMAIL;
import static org.geogig.geoserver.rest.InitRequestHandler.AUTHOR_NAME;
import static org.locationtech.geogig.porcelain.ConfigOp.ConfigAction.CONFIG_SET;
import static org.locationtech.geogig.porcelain.ConfigOp.ConfigScope.LOCAL;

import java.io.File;
import java.net.URI;
import java.util.Map;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.rest.repository.CommandResource;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class InitCommandResource extends CommandResource {

    @Override
    protected String getCommandName() {
        return "init";
    }

    @Override
    protected Representation runCommand(Variant variant, Request request) {
        Representation representation = super.runCommand(variant, request);

        if (getResponse().getStatus() == Status.SUCCESS_CREATED) {
            // set the Author name and email from the Init request
            setAuthor(request);
            // save the repo in the Manager
            RepositoryInfo repoInfo = saveRepository();
        }
        return representation;
    }

    private void setAuthor(Request request) {
        // get request attributes. If author info was requested, it will be stored there.
        Map<String, Object> attributes = request.getAttributes();
        if (attributes.containsKey(AUTHOR_NAME)) {
            // set the author name
            geogig.get().command(ConfigOp.class).setAction(CONFIG_SET).setScope(LOCAL)
                    .setName("user.name").setValue(attributes.get(AUTHOR_NAME).toString()).call();
        }
        if (attributes.containsKey(AUTHOR_EMAIL)) {
            // set the author email
            geogig.get().command(ConfigOp.class).setAction(CONFIG_SET).setScope(LOCAL)
                    .setName("user.email").setValue(attributes.get(AUTHOR_EMAIL).toString()).call();
        }
    }

    private RepositoryInfo saveRepository() {
        // repo was just created, need to register it with an ID in the manager
        // cretae a RepositoryInfo object
        RepositoryInfo repoInfo = new RepositoryInfo();
        URI location = geogig.get().getLocation();
        if ("file".equals(location.getScheme())) {
            // need the parent
            File parentDir = new File(location).getParentFile();
            location = parentDir.toURI();
        }
        // set the URI
        repoInfo.setLocation(location);
        // save the repo, this will set a UUID
        return RepositoryManager.get().save(repoInfo);
    }
}
