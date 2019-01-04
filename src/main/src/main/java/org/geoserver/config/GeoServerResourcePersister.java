/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
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

    private static final int MAX_RENAME_ATTEMPTS = 100;

    /** logging instance */
    static Logger LOGGER = Logging.getLogger("org.geoserver.config");

    GeoServerResourceLoader rl;
    GeoServerDataDirectory dd;

    public GeoServerResourcePersister(GeoServerResourceLoader rl) {
        this.rl = rl;
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
            // here we handle name changes
            int i = event.getPropertyNames().indexOf("name");
            if (i > -1) {
                String newName = (String) event.getNewValues().get(i);

                if (source instanceof StyleInfo) {
                    renameStyle((StyleInfo) source, newName);
                }
            }

            // handle the case of a style changing workspace
            if (source instanceof StyleInfo) {
                i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get(i);
                    Resource newDir = dd.getStyles(newWorkspace);

                    // look for any resource files (image, etc...) and copy them over, don't move
                    // since they could be shared among other styles
                    for (Resource old : dd.additionalStyleResources((StyleInfo) source)) {
                        if (old.getType() != Type.UNDEFINED) {
                            copyResToDir(old, newDir);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void renameStyle(StyleInfo s, String newName) throws IOException {
        // rename style definition file
        Resource style = dd.style(s);
        StyleHandler format = Styles.handler(s.getFormat());

        Resource target = uniqueResource(style, newName, format.getFileExtension());
        renameRes(style, target.name());
        s.setFilename(target.name());

        // rename generated sld if appropriate
        if (!SLDHandler.FORMAT.equals(format.getFormat())) {
            Resource sld = style.parent().get(FilenameUtils.getBaseName(style.name()) + ".sld");
            if (sld.getType() == Type.RESOURCE) {
                LOGGER.fine("Renaming style resource " + s.getName() + " to " + newName);

                Resource generated = uniqueResource(sld, newName, "sld");
                renameRes(sld, generated.name());
            }
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

    /**
     * Determine unique name of the form <code>newName.extension</code>. newName will have a number
     * appended as required to produce a unique resource name.
     *
     * @param resource Resource being renamed
     * @param newName proposed name to use as a template
     * @param extension extension
     * @return New UNDEFINED resource suitable for use with rename
     * @throws IOException If unique resource cannot be produced
     */
    private Resource uniqueResource(Resource resource, String newName, String extension)
            throws IOException {
        Resource target = resource.parent().get(newName + "." + extension);

        int i = 0;
        while (target.getType() != Type.UNDEFINED && ++i <= MAX_RENAME_ATTEMPTS) {
            target = resource.parent().get(newName + i + "." + extension);
        }
        if (i > MAX_RENAME_ATTEMPTS) {
            throw new IOException(
                    "All target files between "
                            + newName
                            + "1."
                            + extension
                            + " and "
                            + newName
                            + MAX_RENAME_ATTEMPTS
                            + "."
                            + extension
                            + " are in use already, giving up");
        }
        return target;
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

    private void renameRes(Resource r, String newName) {
        rl.move(r.path(), r.parent().get(newName).path());
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
