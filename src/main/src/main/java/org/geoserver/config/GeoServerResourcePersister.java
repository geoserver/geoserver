/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

/**
 * Handles the persistence of addition resources when changes happen to the catalog, such as rename,
 * remove and change of workspace.
 */
public class GeoServerResourcePersister implements CatalogListener {

    /** logging instance */
    static Logger LOGGER = Logging.getLogger("org.geoserver.config");

    Catalog catalog;
    GeoServerResourceLoader rl;
    GeoServerDataDirectory dd;

    public GeoServerResourcePersister(Catalog catalog) {
        this.catalog = catalog;
        this.rl = catalog.getResourceLoader();
        this.dd = new GeoServerDataDirectory(rl);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {}

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {}

    @Override
    public void reloaded() {}

    public void handleModifyEvent(CatalogModifyEvent event) {
        Object source = event.getSource();

        try {
            // handle the case of a style changing workspace
            if (source instanceof StyleInfo) {
                int i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    WorkspaceInfo oldWorkspace = (WorkspaceInfo) event.getOldValues().get(i);
                    WorkspaceInfo newWorkspace =
                            ResolvingProxy.resolve(
                                    catalog, (WorkspaceInfo) event.getNewValues().get(i));
                    Resource oldDir = dd.getStyles(oldWorkspace);
                    Resource newDir = dd.getStyles(newWorkspace);
                    URI oldDirURI = new URI(oldDir.path());

                    // look for any resource files (image, etc...) and copy them over, don't move
                    // since they could be shared among other styles
                    for (Resource old : dd.additionalStyleResources((StyleInfo) source)) {
                        if (old.getType() != Type.UNDEFINED) {
                            URI oldURI = new URI(old.path());
                            final URI relative = oldDirURI.relativize(oldURI);
                            final Resource target = newDir.get(relative.getPath()).parent();
                            copyResToDir(old, target);
                        }
                    }

                    // move over the config file and the sld
                    for (Resource old : baseResources((StyleInfo) source)) {
                        if (old.getType() != Type.UNDEFINED) {
                            moveResToDir(old, newDir);
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleRemoveEvent(CatalogRemoveEvent event) {
        Object source = event.getSource();
        try {
            if (source instanceof StyleInfo) {
                removeStyle((StyleInfo) source);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeStyle(StyleInfo s) throws IOException {
        Resource sld = dd.style(s);
        if (Resources.exists(sld)) {
            Resource sldBackup = dd.get(sld.path() + ".bak");
            int i = 1;
            while (Resources.exists(sldBackup)) {
                sldBackup = dd.get(sld.path() + ".bak." + i++);
            }
            LOGGER.fine("Removing the SLD as well but making backup " + sldBackup.name());
            sld.renameTo(sldBackup);
        }
    }

    /*
     * returns the SLD file as well
     */
    private List<Resource> baseResources(StyleInfo s) throws IOException {
        List<Resource> list = Arrays.asList(dd.config(s), dd.style(s));
        return list;
    }

    private void moveResToDir(Resource r, Resource newDir) {
        rl.move(r.path(), newDir.get(r.name()).path());
    }

    private void copyResToDir(Resource r, Resource newDir) throws IOException {
        Resource newR = newDir.get(r.name());
        try (InputStream in = r.in();
                OutputStream out = newR.out()) {
            IOUtils.copy(in, out);
        }
    }
}
