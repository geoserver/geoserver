/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.taskmanager.util;

import com.thoughtworks.xstream.io.xml.JDomWriter;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.wicket.util.io.IOUtils;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geotools.referencing.CRS;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CatalogUtil {

    private static final Logger LOGGER = Logging.getLogger(CatalogUtil.class);

    @Autowired protected GeoServerDataDirectory geoServerDataDirectory;

    @Autowired protected XStreamPersisterFactory persisterFactory;

    private CatalogUtil() {}

    public GSResourceEncoder syncMetadata(ResourceInfo resource) {
        return syncMetadata(resource, resource.getName());
    }

    public GSResourceEncoder syncMetadata(ResourceInfo resource, String name) {
        final GSResourceEncoder re;
        if (resource instanceof CoverageInfo) {
            CoverageInfo coverage = (CoverageInfo) resource;
            final GSCoverageEncoder coverageEncoder = new GSCoverageEncoder();
            for (String format : coverage.getSupportedFormats()) {
                coverageEncoder.addSupportedFormats(format);
            }
            for (String srs : coverage.getRequestSRS()) {
                coverageEncoder.setRequestSRS(srs); // wrong: should be add
            }
            for (String srs : coverage.getResponseSRS()) {
                coverageEncoder.setResponseSRS(srs); // wrong: should be add
            }
            coverageEncoder.setNativeFormat(coverage.getNativeFormat());
            re = coverageEncoder;
        } else {
            re = new GSFeatureTypeEncoder();
            if (resource.getNativeCRS() != null) {
                re.setNativeCRS(CRS.toSRS(resource.getNativeCRS()));
            }
            re.setNativeName(resource.getNativeName());
        }
        re.setName(name);
        re.setTitle(resource.getTitle());
        re.setAbstract(resource.getAbstract());
        re.setDescription(resource.getAbstract());
        re.setSRS(resource.getSRS());
        for (KeywordInfo ki : resource.getKeywords()) {
            re.addKeyword(ki.getValue(), ki.getLanguage(), ki.getVocabulary());
        }
        for (MetadataLinkInfo mdli : resource.getMetadataLinks()) {
            re.addMetadataLinkInfo(mdli.getType(), mdli.getMetadataType(), mdli.getContent());
        }
        for (Map.Entry<String, Serializable> entry : resource.getMetadata().entrySet()) {
            if (entry.getValue() instanceof String) {
                re.addMetadataString(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() != null) {
                JDomWriter writer = new JDomWriter();
                persisterFactory
                        .createXMLPersister()
                        .getXStream()
                        .marshal(entry.getValue(), writer);
                re.addMetadata(entry.getKey(), (Element) writer.getTopLevelNodes().get(0));
            }
        }
        re.setProjectionPolicy(
                resource.getProjectionPolicy() == null
                        ? ProjectionPolicy.NONE
                        : ProjectionPolicy.valueOf(resource.getProjectionPolicy().toString()));
        if (resource.getNativeBoundingBox() != null) {
            re.setNativeBoundingBox(
                    resource.getNativeBoundingBox().getMinX(),
                    resource.getNativeBoundingBox().getMinY(),
                    resource.getNativeBoundingBox().getMaxX(),
                    resource.getNativeBoundingBox().getMaxY(),
                    resource.getSRS());
        }
        if (resource.getLatLonBoundingBox() != null) {
            re.setLatLonBoundingBox(
                    resource.getLatLonBoundingBox().getMinX(),
                    resource.getLatLonBoundingBox().getMinY(),
                    resource.getLatLonBoundingBox().getMaxX(),
                    resource.getLatLonBoundingBox().getMaxY(),
                    resource.getSRS());
        }

        // dimensions, must happen after setName or strange things happen (gs-man bug)
        if (resource instanceof CoverageInfo) {
            CoverageInfo coverage = (CoverageInfo) resource;
            for (CoverageDimensionInfo di : coverage.getDimensions()) {
                ((GSCoverageEncoder) re)
                        .addCoverageDimensionInfo(
                                di.getName(),
                                di.getDescription(),
                                Double.toString(di.getRange().getMinimum()),
                                Double.toString(di.getRange().getMaximum()),
                                di.getUnit(),
                                di.getDimensionType() == null
                                        ? null
                                        : di.getDimensionType().identifier());
            }
        }

        return re;
    }

    public static String wsName(WorkspaceInfo ws) {
        return ws == null ? null : ws.getName();
    }

    public File createStyleZipFile(StyleInfo style) throws TaskException {
        try {
            Style parsedStyle = geoServerDataDirectory.parsedStyle(style);
            Set<Resource> pictures = new HashSet<Resource>();
            parsedStyle.accept(
                    new AbstractStyleVisitor() {
                        @Override
                        public void visit(ExternalGraphic exgr) {
                            if (exgr.getOnlineResource() == null) {
                                return;
                            }

                            URI uri = exgr.getOnlineResource().getLinkage();
                            if (uri == null) {
                                return;
                            }

                            Resource resPicture = null;
                            try {
                                resPicture = uriToResource(uri);
                                if (resPicture != null && resPicture.getType() != Type.UNDEFINED) {
                                    pictures.add(resPicture);
                                }
                            } catch (IllegalArgumentException | MalformedURLException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Error attemping to process SLD resource",
                                        e);
                            }
                        }
                    });

            File zipFile = File.createTempFile("style", ".zip");
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile)); ) {
                Resource resStyle = geoServerDataDirectory.style(style);
                ZipEntry zipEntry = new ZipEntry(resStyle.name());
                out.putNextEntry(zipEntry);
                try (InputStream in = resStyle.in()) {
                    IOUtils.copy(in, out);
                }
                out.closeEntry();
                for (Resource resPicture : pictures) {
                    zipEntry = new ZipEntry(resPicture.name());
                    out.putNextEntry(zipEntry);
                    try (InputStream in = resPicture.in()) {
                        IOUtils.copy(in, out);
                    }
                    out.closeEntry();
                }
                return zipFile;
            }
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }

    private Resource uriToResource(URI uri) throws MalformedURLException {
        if (uri.getScheme() != null && !uri.getScheme().equals("file")) {
            return null;
        } else if (uri.getScheme().equals("file") && uri.isAbsolute() && !uri.isOpaque()) {
            return Files.asResource(new File(uri.toURL().getFile()));
        } else {
            return geoServerDataDirectory.get(uri.getSchemeSpecificPart());
        }
    }
}
