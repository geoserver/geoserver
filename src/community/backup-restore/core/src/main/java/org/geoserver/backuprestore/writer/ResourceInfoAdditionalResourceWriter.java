/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.template.GeoServerTemplateLoader;

/**
 * On a {@link CatalogFireWriter} firePostWrite event call, dumps additional resources related to
 * the {@link ResourceInfo}, like for instance the "ftl" Freemarker Templates definitions.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class ResourceInfoAdditionalResourceWriter
        implements CatalogAdditionalResourcesWriter<ResourceInfo> {

    /*
     *
     */
    static List<String> templates = new ArrayList<String>();

    /*
     *
     */
    static {
        templates.add("header.ftl");
        templates.add("footer.ftl");
        templates.add("title.ftl");
        templates.add("link.ftl");
        templates.add("content.ftl");
        templates.add("complex_content.ftl");
        templates.add("description.ftl");
        templates.add("height.ftl");
        templates.add("time.ftl");
        templates.add("shapezip.ftl");
        templates.add(
                "schema.xsd"); // http://docs.geoserver.org/latest/en/user/services/wfs/schemamapping.html
    }

    @Override
    public boolean canHandle(Object item) {
        return (item instanceof ResourceInfo);
    }

    @Override
    public void writeAdditionalResources(Backup backupFacade, Resource base, ResourceInfo item)
            throws IOException {

        final Resource rootDataDir = backupFacade.getGeoServerDataDirectory().getRoot(Paths.BASE);

        GeoServerTemplateLoader templateLoader =
                new GeoServerTemplateLoader(item.getClass(), backupFacade.getResourceLoader());
        templateLoader.setResource(item);

        if (templateLoader != null) {
            for (String template : templates) {
                final Object ftl = templateLoader.findTemplateSource(template);

                if (ftl != null && ftl instanceof File) {
                    Resource templateResource = Files.asResource((File) ftl);

                    if (Resources.exists(templateResource)) {
                        final String relative =
                                rootDataDir
                                        .dir()
                                        .toURI()
                                        .relativize(templateResource.file().toURI())
                                        .getPath();

                        Resource targetFtl = Resources.fromPath(relative, base.parent());

                        if (!targetFtl.parent().dir().exists()) {
                            targetFtl.parent().dir().mkdirs();
                        }

                        Resources.copy(templateResource.file(), targetFtl.parent());
                    }
                }
            }
        }
    }
}
