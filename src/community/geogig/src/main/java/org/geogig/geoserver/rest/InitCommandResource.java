package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.getGeogig;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.api.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.rest.repository.CommandResource;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class InitCommandResource extends CommandResource {
	
	@Override
	protected String getCommandName() {
        return "init";
    }
	
	@Override
	protected Representation runCommand(Variant variant, Request request) {
		Representation representation = super.runCommand(variant, request);

		if (getResponse().getStatus() == Status.SUCCESS_CREATED) {
			Catalog catalog = RepositoryManager.get().getCatalog();
			setUpDataStore(catalog, geogig.get().command(ResolveRepositoryName.class).call());
		}
		return representation;
	}
	
    public DataStoreInfo setUpDataStore(Catalog catalog, String storeName) {
    	NamespaceInfo ns = catalog.getDefaultNamespace();
    	WorkspaceInfo ws = catalog.getDefaultWorkspace();
        DataStoreInfo ds = catalog.getFactory().createDataStore();
        ds.setEnabled(true);
        ds.setDescription("GeoGIG repository");
        ds.setName(storeName);
        ds.setType(GeoGigDataStoreFactory.DISPLAY_NAME);
        ds.setWorkspace(ws);
        Map<String, Serializable> connParams = ds.getConnectionParameters();

        Optional<URI> GeogigDir = geogig.get().command(ResolveGeogigURI.class).call();
        File repositoryUrl = new File(GeogigDir.get()).getParentFile();

        connParams.put(GeoGigDataStoreFactory.REPOSITORY.key, repositoryUrl.getAbsolutePath());
        connParams.put(GeoGigDataStoreFactory.DEFAULT_NAMESPACE.key, ns.getURI());
        catalog.add(ds);

        try {
            DataStoreInfo dsInfo = catalog.getDataStoreByName(ws, storeName);
            String repoId = (String) dsInfo.getConnectionParameters()
                    .get(GeoGigDataStoreFactory.REPOSITORY.key);
        	RepositoryInfo info = RepositoryManager.get().get(repoId);
        } catch (IOException e) {
        	Throwables.propagate(e);
        }
        return ds;
    }
}
