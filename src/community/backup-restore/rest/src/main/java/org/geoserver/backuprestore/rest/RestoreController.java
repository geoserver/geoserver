/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.logging.Level;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * REST Restore Controller
 *
 * <pre>/br/restore[/&lt;restoreId&gt;][.zip]</pre>
 *
 * @author "Alessio Fabiani" <alessio.fabiani@geo-solutions.it>, GeoSolutions
 */
@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/br/",
    produces = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE,
        MediaType.TEXT_HTML_VALUE
    }
)
public class RestoreController extends AbstractBackupRestoreController {

    @Autowired
    public RestoreController(@Qualifier("backupFacade") Backup backupFacade) {
        assert backupFacade != null;
        this.backupFacade = backupFacade;
    }

    @GetMapping(
        path = "restore{.+}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper restoreGet(@RequestParam(name = "format", required = false) String format) {

        Object lookup = lookupRestoreExecutionsContext(null, true, false);

        if (lookup != null) {
            if (lookup instanceof RestoreExecutionAdapter) {
                return wrapObject((RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
            } else {
                return wrapList(
                        (List<RestoreExecutionAdapter>) lookup, RestoreExecutionAdapter.class);
            }
        }

        return null;
    }

    @GetMapping(
        path = "restore/{restoreId:.+}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.ALL_VALUE
        }
    )
    public RestWrapper restoreGet(
            @RequestParam(name = "format", required = false) String format,
            @PathVariable String restoreId,
            HttpServletResponse response) {

        Object lookup =
                lookupRestoreExecutionsContext(getExecutionIdFilter(restoreId), true, false);

        if (lookup != null) {
            if (lookup instanceof RestoreExecutionAdapter) {
                if (restoreId.endsWith(".zip")) {
                    try {
                        // get your file as InputStream
                        File file = ((RestoreExecutionAdapter) lookup).getArchiveFile().file();
                        InputStream is = new FileInputStream(file);
                        // copy it to response's OutputStream
                        org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
                        response.flushBuffer();
                    } catch (IOException ex) {
                        LOGGER.log(Level.INFO, "Error writing file to output stream.", ex);
                        throw new RuntimeException("IOError writing file to output stream");
                    }
                } else {
                    return wrapObject(
                            (RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
                }
            } else {
                return wrapList(
                        (List<RestoreExecutionAdapter>) lookup, RestoreExecutionAdapter.class);
            }
        }

        return null;
    }

    @DeleteMapping(
        path = "restore/{restoreId:.+}",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper restoreDelete(
            @RequestParam(name = "format", required = false) String format,
            @PathVariable String restoreId)
            throws IOException {

        final String executionId = getExecutionIdFilter(restoreId);
        Object lookup = lookupRestoreExecutionsContext(executionId, true, false);

        if (lookup != null) {
            if (lookup instanceof RestoreExecutionAdapter) {
                try {
                    getBackupFacade().abandonExecution(Long.valueOf(executionId));
                } catch (Exception e) {
                    throw new IOException(e);
                }
                return wrapObject((RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
            } else {
                return wrapList(
                        (List<RestoreExecutionAdapter>) lookup, RestoreExecutionAdapter.class);
            }
        }

        return null;
    }

    @PostMapping(
        value = {"/restore"},
        consumes = {
            MediaType.TEXT_XML_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypeExtensions.TEXT_JSON_VALUE
        },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public RestWrapper restorePost(
            @RequestBody(required = true) RestoreExecutionAdapter restore,
            @RequestHeader("Content-Type") String contentType,
            UriComponentsBuilder builder)
            throws IOException {
        RestoreExecutionAdapter execution = null;

        if (restore.getId() != null) {
            Object lookup =
                    lookupBackupExecutionsContext(String.valueOf(restore.getId()), false, false);
            if (lookup != null) {
                // Restore instance already exists... trying to restart it.
                try {
                    getBackupFacade().restartExecution(restore.getId());

                    LOGGER.log(Level.INFO, "Restore restarted: " + restore.getArchiveFile());

                    return wrapObject(
                            (RestoreExecutionAdapter) lookup, RestoreExecutionAdapter.class);
                } catch (Exception e) {

                    LOGGER.log(
                            Level.WARNING,
                            "Could not restart the restore: " + restore.getArchiveFile());

                    throw new IOException(e);
                }
            }
        } else {
            // Start a new execution asynchronously. You will need to query for the status in order
            // to follow the progress.
            execution =
                    getBackupFacade()
                            .runRestoreAsync(
                                    restore.getArchiveFile(),
                                    restore.getWsFilter(),
                                    restore.getSiFilter(),
                                    restore.getLiFilter(),
                                    asParams(restore.getOptions()));

            LOGGER.log(Level.INFO, "Restore file: " + restore.getArchiveFile());

            return wrapObject((RestoreExecutionAdapter) execution, RestoreExecutionAdapter.class);
        }

        return null;
    }

    /**
     * From {@link RestBaseController}
     *
     * <p>... * Any extending classes which override {@link #configurePersister(XStreamPersister,
     * XStreamMessageConverter)}, and require this configuration for reading objects from incoming
     * requests must also be annotated with {@link
     * org.springframework.web.bind.annotation.ControllerAdvice} and override the {@link
     * #supports(MethodParameter, Type, Class)} method...
     */
    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return RestoreExecutionAdapter.class.isAssignableFrom(methodParameter.getParameterType());
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        intializeXStreamContext(xstream);
    }
}
