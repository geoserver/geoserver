/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.rest.schema;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.featurestemplating.configuration.schema.SchemaFileManager;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.featurestemplating.configuration.schema.SchemaService;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.IOUtils;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class SchemaRestController extends AbstractCatalogController {

    private static final Logger LOGGER = Logging.getLogger(SchemaRestController.class);

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("schemaInfo", SchemaInfoList.class);
        xstream.registerConverter(new SchemaInfoConverter());
        xstream.allowTypes(new Class[] {SchemaInfoList.class});
    }

    @Autowired
    public SchemaRestController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @PostMapping(
            value = {
                "/schemaoverrides",
                "/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides",
                "/workspaces/{ws}/schemaoverrides"
            },
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XHTML_XML_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE,
            })
    public ResponseEntity<String> schemaPost(
            InputStream inputStream,
            @PathVariable(required = false) String featureType,
            @PathVariable(required = false) String ws,
            @RequestParam String schemaName,
            @RequestHeader("Content-Type") String contentType,
            UriComponentsBuilder builder) {

        if (ws != null) {
            checkWorkspaceName(ws);
            checkFullAdminRequired(ws);
        }
        String ftName = null;
        if (featureType != null) {
            FeatureTypeInfo ft = checkFeatureType(ws, featureType);
            ftName = ft.getName();
            ws = ft.getStore().getWorkspace().getName();
        }
        SchemaInfoDAO dao = SchemaInfoDAO.get();
        if (dao.findByFullName(schemaName) != null) {
            throw new RestException("Schema " + schemaName + " already exists.", HttpStatus.FORBIDDEN);
        }
        SchemaInfo info = new SchemaInfo();
        info.setSchemaName(schemaName);
        info.setExtension(getExtensionByContentType(contentType));
        if (ws != null) info.setWorkspace(ws);
        if (ftName != null) info.setFeatureType(ftName);
        saveOrUpdateSchema(info, inputStream);
        dao.saveOrUpdate(info);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(getUri(schemaName, ws, ftName, builder));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(schemaName, headers, HttpStatus.CREATED);
    }

    private String getExtensionByContentType(String contentType) {
        String mediaType = getMimeTypeFromContentType(contentType);
        String extension = getExtensionByMediaType(mediaType);
        if (extension == null) {
            throw new RestException("Unable to determine the schema file extension.", HttpStatus.BAD_REQUEST);
        }
        return extension;
    }

    private String getExtensionByMediaType(String mediaType) {
        if (mediaType != null) {
            if (mediaType.equals(MediaType.APPLICATION_JSON_VALUE)
                    || mediaType.equals(MediaTypeExtensions.TEXT_JSON_VALUE)) return "json";
            else if (mediaType.equals(MediaType.APPLICATION_XML_VALUE) || mediaType.equals(MediaType.TEXT_XML_VALUE))
                return "xml";
            else if (mediaType.equals(MediaType.APPLICATION_XHTML_XML_VALUE)) return "xhtml";
        }
        return null;
    }

    private String getMimeTypeFromContentType(String contentType) {
        if (contentType != null) {
            return contentType.split(";")[0];
        }
        return null;
    }

    @PostMapping(
            value = {
                "/schemaoverrides",
                "/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides",
                "/workspaces/{ws}/schemaoverrides"
            },
            consumes = {MediaTypeExtensions.APPLICATION_ZIP_VALUE})
    public ResponseEntity<String> schemaZipPost(
            InputStream is,
            @PathVariable(required = false) String featureType,
            @PathVariable(required = false) String ws,
            @RequestParam(required = false) String schemaName,
            UriComponentsBuilder builder) {

        if (ws != null) {
            checkWorkspaceName(ws);
            checkFullAdminRequired(ws);
        }
        String ftName = null;
        if (featureType != null) {
            FeatureTypeInfo ft = checkFeatureType(ws, featureType);
            ftName = ft.getName();
            ws = ft.getStore().getWorkspace().getName();
        }

        SchemaInfo info = new SchemaInfo();
        if (ws != null) info.setWorkspace(ws);
        if (ftName != null) info.setFeatureType(ftName);
        File directory = unzip(is);
        try {
            File schemaFile = getSchemaFileFromDirectory(directory);
            if (schemaName == null) schemaName = FilenameUtils.removeExtension(schemaFile.getName());
            String extension = FilenameUtils.getExtension(schemaFile.getName());
            SchemaInfoDAO dao = SchemaInfoDAO.get();
            if (dao.findByFullName(schemaName) != null) {
                throw new RestException("Schema " + schemaName + " already exists.", HttpStatus.FORBIDDEN);
            }
            info.setSchemaName(schemaName);
            info.setExtension(extension);
            try (InputStream inputStream = new FileInputStream(schemaFile)) {
                saveOrUpdateSchema(info, inputStream);
            } catch (IOException e) {
                throw new RestException("Error while processing the schema", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        } finally {
            try {
                FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(getUri(schemaName, ws, ftName, builder));
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(schemaName, headers, HttpStatus.CREATED);
    }

    @PutMapping(
            value = {
                "/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/schemaoverrides/{schemaName}"
            },
            consumes = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XHTML_XML_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public ResponseEntity<String> schemaPut(
            InputStream inputStream,
            @PathVariable(required = false) String featureType,
            @PathVariable(required = false) String ws,
            @PathVariable String schemaName,
            @RequestHeader("Content-Type") String contentType,
            UriComponentsBuilder builder) {

        if (ws != null) {
            checkWorkspaceName(ws);
            checkFullAdminRequired(ws);
        }
        FeatureTypeInfo ft;
        if (featureType != null) {
            ft = checkFeatureType(ws, featureType);
            ws = ft.getStore().getWorkspace().getName();
            featureType = ft.getName();
        }
        String fullName = buildFullName(ws, featureType, schemaName);
        SchemaInfo info = checkSchemaInfo(fullName);
        String extension = getExtensionByContentType(contentType);
        if (!info.getExtension().equals(extension)) {
            info.setExtension(extension);
            SchemaInfoDAO.get().saveOrUpdate(info);
        }
        saveOrUpdateSchema(info, inputStream);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(schemaName, headers, HttpStatus.CREATED);
    }

    @PutMapping(
            value = {
                "/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/schemaoverrides/{schemaName}"
            },
            consumes = {MediaTypeExtensions.APPLICATION_ZIP_VALUE})
    public ResponseEntity<String> schemaZipPut(
            InputStream is,
            @PathVariable String schemaName,
            @PathVariable(required = false) String ws,
            @PathVariable(required = false) String featureType) {

        if (ws != null) {
            checkWorkspaceName(ws);
            checkFullAdminRequired(ws);
        }
        FeatureTypeInfo ft;
        if (featureType != null) {
            ft = checkFeatureType(ws, featureType);
            ws = ft.getStore().getWorkspace().getName();
            featureType = ft.getName();
        }
        String fullName = buildFullName(ws, featureType, schemaName);
        SchemaInfo info = checkSchemaInfo(fullName);
        File directory = unzip(is);
        try {
            File schemaFile = getSchemaFileFromDirectory(directory);
            String extension = FilenameUtils.getExtension(schemaFile.getName());
            if (!info.getExtension().equals(extension)) info.setExtension(extension);
            try (InputStream inputStream = new FileInputStream(schemaFile)) {
                saveOrUpdateSchema(info, inputStream);
            } catch (IOException e) {
                throw new RestException("Error while processing the schema", HttpStatus.INTERNAL_SERVER_ERROR, e);
            }
        } finally {
            try {
                FileUtils.deleteDirectory(directory);
            } catch (IOException e) {
                throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(schemaName, headers, HttpStatus.CREATED);
    }

    private SchemaInfo checkSchemaInfo(String fullName) {
        SchemaInfoDAO dao = SchemaInfoDAO.get();
        SchemaInfo info = dao.findByFullName(fullName);
        if (info == null) throw new RestException("Schema " + fullName + " doesn't exist.", HttpStatus.FORBIDDEN);
        return info;
    }

    private URI getUri(String name, String workspace, String featureType, UriComponentsBuilder builder) {
        UriComponents uriComponents;
        builder = builder.cloneBuilder();
        if (featureType != null) {
            uriComponents = builder.path("/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides/{schemaName}")
                    .buildAndExpand(workspace, featureType, name);
        } else if (workspace != null) {
            uriComponents = builder.path("/workspaces/{ws}/schemaoverrides/{schemaName}")
                    .buildAndExpand(workspace, name);
        } else {
            uriComponents = builder.path("/schemaoverrides/{schemaName}").buildAndExpand(name);
        }
        return uriComponents.toUri();
    }

    private void saveOrUpdateSchema(SchemaInfo info, InputStream inputStream) {
        try {
            byte[] rawData = IOUtils.toByteArray(inputStream);
            String content = new String(rawData, Charset.defaultCharset());
            new SchemaService().saveOrUpdate(info, content);
        } catch (IOException e) {
            throw new RestException("Error while writing the schema", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(
            value = {
                "/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/schemaoverrides/{schemaName}"
            },
            produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchema(
            @PathVariable(required = false) String featureType,
            @PathVariable(required = false) String ws,
            @PathVariable String schemaName) {

        if (ws != null) {
            checkWorkspaceName(ws);
            checkFullAdminRequired(ws);
        }
        FeatureTypeInfo ft;
        if (featureType != null) {
            ft = checkFeatureType(ws, featureType);
            ws = ft.getStore().getWorkspace().getName();
            featureType = ft.getName();
        }
        String fullName = buildFullName(ws, featureType, schemaName);
        SchemaInfoDAO dao = SchemaInfoDAO.get();
        SchemaInfo info = dao.findByFullName(fullName);
        new SchemaService().delete(info);
        LOGGER.info("Deleted schema with name " + info.getFullName());
    }

    @GetMapping(
            value = {
                "/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides/{schemaName}",
                "/workspaces/{ws}/schemaoverrides/{schemaName}"
            },
            produces = {
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_XHTML_XML_VALUE,
            })
    public void schemaGet(
            @PathVariable(required = false) String ws,
            @PathVariable(required = false) String featureType,
            @PathVariable String schemaName,
            HttpServletResponse response) {

        if (ws != null) {
            checkWorkspaceName(ws);
            checkFullAdminRequired(ws);
        }
        FeatureTypeInfo ft;
        if (featureType != null) {
            ft = checkFeatureType(ws, featureType);
            ws = ft.getStore().getWorkspace().getName();
            featureType = ft.getName();
        }
        String fullName = buildFullName(ws, featureType, schemaName);
        SchemaInfo info = SchemaInfoDAO.get().findByFullName(fullName);
        Resource resource = SchemaFileManager.get().getSchemaResource(info);
        if (resource.getType() != Resource.Type.RESOURCE) {
            throw new RestException("Schema with fullName " + info.getFullName() + " not found", HttpStatus.NOT_FOUND);
        }
        byte[] bytes;
        try {
            bytes = resource.getContents();

            response.setContentType(getMediaType(info));
            response.setContentLength(bytes.length);
            try (ServletOutputStream output = response.getOutputStream()) {
                output.write(bytes);
                output.flush();
            }
        } catch (IOException e) {
            throw new RestException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @GetMapping(
            value = {
                "/schemaoverrides",
                "/workspaces/{ws}/featuretypes/{featureType}/schemaoverrides",
                "/workspaces/{ws}/schemaoverrides"
            },
            produces = {
                MediaType.TEXT_XML_VALUE,
                MediaType.APPLICATION_XML_VALUE,
                MediaType.APPLICATION_JSON_VALUE,
                MediaTypeExtensions.TEXT_JSON_VALUE
            })
    public RestWrapper<SchemaInfoList> schemaGetAll(
            @PathVariable(required = false) String ws,
            @PathVariable(required = false) String featureType,
            UriComponentsBuilder builder) {

        if (ws != null) {
            checkWorkspaceName(ws);
            checkFullAdminRequired(ws);
        }
        FeatureTypeInfo ft;
        if (featureType != null) {
            ft = checkFeatureType(ws, featureType);
            ws = ft.getStore().getWorkspace().getName();
            featureType = ft.getName();
        }
        List<SchemaInfo> infos = SchemaInfoDAO.get().findAll().stream()
                .filter(getPredicate(ws, featureType))
                .collect(Collectors.toList());
        return wrapObject(new SchemaInfoList(infos, builder), SchemaInfoList.class);
    }

    private Predicate<SchemaInfo> getPredicate(String ws, String featureType) {
        Predicate<SchemaInfo> predicate;
        if (featureType != null) {
            predicate = t -> t.getFeatureType() != null && t.getFeatureType().equals(featureType);
        } else if (ws != null) {
            predicate = t -> t.getWorkspace() != null && t.getWorkspace().equals(ws) && t.getFeatureType() == null;
        } else {
            predicate = t -> t.getWorkspace() == null && t.getFeatureType() == null;
        }
        return predicate;
    }

    private File unzip(InputStream object) {
        try {
            File tempDir = Files.createTempDirectory("_schema").toFile();

            org.geoserver.util.IOUtils.decompress(object, tempDir);

            return tempDir;
        } catch (Exception e) {
            LOGGER.severe("Error processing the schema zip (PUT): " + e.getMessage());
            throw new RestException("Error processing the schema", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private File getSchemaFileFromDirectory(File directory) throws RestException {
        try {
            File[] matchingFiles = directory.listFiles(
                    (dir, name) -> name.endsWith("json") || name.endsWith("xml") || name.endsWith("xhtml"));

            if (matchingFiles == null || matchingFiles.length == 0) {
                throw new RestException("No schema file provided:", HttpStatus.FORBIDDEN);
            }

            LOGGER.fine("getting schema file from directory: " + matchingFiles[0].getAbsolutePath());

            return matchingFiles[0];
        } catch (Exception e) {
            LOGGER.severe("Error while searching the schema in unzipped directory (PUT): " + e.getMessage());
            throw new RestException("Error processing the schema", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private String getMediaType(SchemaInfo info) {
        String result;
        if (info.getExtension().equals("json")) result = MediaType.APPLICATION_JSON_VALUE;
        else if (info.getExtension().equals("xhtml")) result = MediaType.APPLICATION_XHTML_XML_VALUE;
        else result = MediaType.APPLICATION_XML_VALUE;
        return result;
    }

    private String buildFullName(String ws, String ft, String schemaName) {
        StringBuilder fullName = new StringBuilder("");
        if (ws != null) fullName.append(ws).append(":");
        if (ft != null) fullName.append(ft).append(":");
        fullName.append(schemaName);
        return fullName.toString();
    }

    private void checkWorkspaceName(String workspaceName) throws RestException {
        if (workspaceName != null && catalog.getWorkspaceByName(workspaceName) == null) {
            throw new ResourceNotFoundException("Workspace " + workspaceName + " not found");
        }
    }

    private FeatureTypeInfo checkFeatureType(String workspace, String featureTypeName) {
        FeatureTypeInfo featureType = catalog.getFeatureTypeByName(new NameImpl(workspace, featureTypeName));
        if (featureType == null) {
            throw new ResourceNotFoundException("FeatureType " + featureTypeName + " not found");
        }
        return featureType;
    }

    class SchemaInfoConverter implements Converter {

        @Override
        public void marshal(
                Object o, HierarchicalStreamWriter hierarchicalStreamWriter, MarshallingContext marshallingContext) {
            if (o instanceof SchemaInfoList) {
                SchemaInfoList list = (SchemaInfoList) o;
                for (SchemaInfo info : list.getInfos()) {
                    hierarchicalStreamWriter.startNode("schemas");
                    hierarchicalStreamWriter.startNode("name");
                    hierarchicalStreamWriter.setValue(info.getSchemaName());
                    hierarchicalStreamWriter.endNode();
                    hierarchicalStreamWriter.startNode("fileType");
                    hierarchicalStreamWriter.setValue(info.getExtension());
                    hierarchicalStreamWriter.endNode();
                    hierarchicalStreamWriter.startNode("location");
                    String uri = getUri(
                                    info.getSchemaName(),
                                    info.getWorkspace(),
                                    info.getFeatureType(),
                                    list.getUriBuilder())
                            .toString();
                    hierarchicalStreamWriter.setValue(uri);
                    hierarchicalStreamWriter.endNode();
                    hierarchicalStreamWriter.endNode();
                }
            }
        }

        @Override
        public Object unmarshal(
                HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
            return null;
        }

        @Override
        public boolean canConvert(Class aClass) {
            return aClass.isAssignableFrom(SchemaInfoList.class);
        }
    }

    class SchemaInfoList {
        private List<SchemaInfo> infos;

        private UriComponentsBuilder builder;

        SchemaInfoList(List<SchemaInfo> infos, UriComponentsBuilder builder) {
            this.infos = infos;
            this.builder = builder;
        }

        List<SchemaInfo> getInfos() {
            return infos;
        }

        UriComponentsBuilder getUriBuilder() {
            return builder.cloneBuilder();
        }
    }
}
