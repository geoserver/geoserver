/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.taskmanager.util;

import com.thoughtworks.xstream.io.xml.JDomWriter;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import it.geosolutions.geoserver.rest.encoder.GSStyleEncoder;
import it.geosolutions.geoserver.rest.encoder.authorityurl.GSAuthorityURLInfoEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;
import it.geosolutions.geoserver.rest.encoder.identifier.GSIdentifierInfoEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.util.io.IOUtils;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.Style;
import org.geotools.referencing.CRS;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.util.logging.Logging;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CatalogUtil {

    private static final Logger LOGGER = Logging.getLogger(CatalogUtil.class);

    private static final String[] IGNORE_METADATA = {
        "custom-derived-attributes", // metadata module
        CoverageView.COVERAGE_VIEW // coverage view
    };

    @Autowired protected GeoServerDataDirectory geoServerDataDirectory;

    @Autowired protected XStreamPersisterFactory persisterFactory;

    private CatalogUtil() {}

    public void syncMetadata(LayerInfo layer, GSLayerEncoder layerEncoder) {
        for (AuthorityURLInfo authorityURL : layer.getAuthorityURLs()) {
            GSAuthorityURLInfoEncoder authorityURLEncoder = new GSAuthorityURLInfoEncoder();
            authorityURLEncoder.setHref(authorityURL.getHref());
            authorityURLEncoder.setName(authorityURL.getName());
            layerEncoder.addAuthorityURL(authorityURLEncoder);
        }

        for (LayerIdentifierInfo layerIdentifier : layer.getIdentifiers()) {
            GSIdentifierInfoEncoder identifierEncoder = new GSIdentifierInfoEncoder();
            identifierEncoder.setAuthority(layerIdentifier.getAuthority());
            identifierEncoder.setIdentifier(layerIdentifier.getIdentifier());
            layerEncoder.addIdentifier(identifierEncoder);
        }
    }

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
            if (!ArrayUtils.contains(IGNORE_METADATA, entry.getKey())) {
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
                    CRS.toSRS(resource.getNativeBoundingBox().getCoordinateReferenceSystem()));
        }
        if (resource.getLatLonBoundingBox() != null) {
            re.setLatLonBoundingBox(
                    resource.getLatLonBoundingBox().getMinX(),
                    resource.getLatLonBoundingBox().getMinY(),
                    resource.getLatLonBoundingBox().getMaxX(),
                    resource.getLatLonBoundingBox().getMaxY(),
                    CRS.toSRS(resource.getLatLonBoundingBox().getCoordinateReferenceSystem()));
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

    public GSStyleEncoder syncStyle(StyleInfo styleInfo) {
        GSStyleEncoder encoder = new GSStyleEncoder();
        if (styleInfo.getLegend() != null) {
            encoder.setLegendGraphic(
                    styleInfo.getLegend().getOnlineResource(),
                    styleInfo.getLegend().getFormat(),
                    styleInfo.getLegend().getWidth(),
                    styleInfo.getLegend().getHeight());
        }
        encoder.setFormat(styleInfo.getFormat());
        encoder.setLanguageVersion(styleInfo.getFormatVersion().toString());
        return encoder;
    }

    public static String wsName(WorkspaceInfo ws) {
        return ws == null ? null : ws.getName();
    }

    public File createStyleZipFile(StyleInfo style) throws TaskException {
        try {
            Style parsedStyle = geoServerDataDirectory.parsedStyle(style);
            Resource resStyle = geoServerDataDirectory.style(style);
            Set<String> pictures = new HashSet<String>();
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

                            String picturePath = null;
                            try {
                                picturePath = uriToPath(uri, resStyle);
                                if (picturePath == null) {
                                    LOGGER.info(
                                            "While synchronizing style "
                                                    + style.getName()
                                                    + ", ignoring external image URI: "
                                                    + uri);
                                } else {
                                    pictures.add(picturePath);
                                }
                            } catch (IllegalArgumentException | MalformedURLException e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Error attemping to process SLD resource for style "
                                                + style.getName(),
                                        e);
                            }
                        }
                    });
            if (style.getLegend() != null) {
                String legendGraphic = style.getLegend().getOnlineResource();
                try {
                    if (!new URI(legendGraphic).isAbsolute()) {
                        pictures.add(legendGraphic);
                    }
                } catch (URISyntaxException e) {
                    LOGGER.log(Level.WARNING, e.getMessage(), e);
                }
            }
            // subdirectories for pictures
            Set<String> dirs = new HashSet<>();
            for (String picturePath : pictures) {
                Path parent = Paths.get(picturePath).getParent();
                if (parent != null) {
                    dirs.add(parent.toString());
                }
            }

            File zipFile = File.createTempFile("style", ".zip");
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
                ZipEntry zipEntry = new ZipEntry(resStyle.name());
                out.putNextEntry(zipEntry);
                try (InputStream in = resStyle.in()) {
                    IOUtils.copy(in, out);
                }
                out.closeEntry();
                // dirs
                for (String dir : dirs) {
                    out.putNextEntry(new ZipEntry(dir + "/"));
                }
                // pictures
                for (String picturePath : pictures) {
                    Resource resPicture = resStyle.parent().get(picturePath);
                    if (!Resources.exists(resPicture)) {
                        LOGGER.warning(
                                "While synchronizing style "
                                        + style.getName()
                                        + ", couldn't find picture : "
                                        + picturePath);
                    } else {
                        zipEntry = new ZipEntry(picturePath);
                        out.putNextEntry(zipEntry);
                        try (InputStream in = resPicture.in()) {
                            IOUtils.copy(in, out);
                        }
                        out.closeEntry();
                    }
                }
                return zipFile;
            }
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }

    private String uriToPath(URI uri, Resource styleRes) throws MalformedURLException {
        if (uri.getScheme() != null && !uri.getScheme().equals("file")) {
            return null;
        } else {
            Path styleDirPath = Paths.get(styleRes.parent().dir().getAbsolutePath());
            Path imagePath = Paths.get(uri);
            Path result = styleDirPath.relativize(imagePath).normalize();
            if (result.startsWith("..")) {
                return null;
            } else {
                return result.toString();
            }
        }
    }
}
