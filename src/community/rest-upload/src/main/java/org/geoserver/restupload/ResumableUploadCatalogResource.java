/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.restupload;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;
import org.restlet.util.Series;

/**
 * The main feature of the following module is the ability to resume the upload process of a file
 * via REST.
 *
 * <p>An upload URL is generated at the first REST POST request and saved in order to execute the
 * other upload steps.</br> Successive PUT request on the URL created before allow partial or full
 * upload of binary file.</br> RANGE parameter in PUT request/response allow handshake of the number
 * of bytes currently uploaded.</br> GET request can be used to retrieve informations about upload
 * status.
 *
 * <p>The uploaded resource is stored to temporary folder until the upload is not completed or the
 * {@link ResumableUploadResourceCleaner#expirationDelay} time is elapsed.</br> When the upload is
 * terminated the file is moved to REST main folder by {@link ResumableUploadPathMapper}
 *
 * @author Nicola Lagomarsini
 */
public class ResumableUploadCatalogResource extends Resource {

    private static final Logger LOGGER = Logging.getLogger(ResumableUploadCatalogResource.class);

    /** Manager for the Resumable REST upload */
    private ResumableUploadResourceManager resumableUploadResourceManager;

    /**
     * If the server has successfully received all bytes from the operation, it responds with a
     * final status code; otherwise it responds with a 308 (Resume Incomplete), indicating which
     * bytes of the operation it has successfully received.
     */
    public static final Status RESUME_INCOMPLETE = new Status(308);

    public ResumableUploadCatalogResource(
            Context context,
            Request request,
            Response response,
            Catalog catalog,
            ResumableUploadResourceManager resumableUploadResourceManager) {
        super(context, request, response);
        this.resumableUploadResourceManager = resumableUploadResourceManager;
    }

    @Override
    public boolean allowPost() {
        return true;
    }

    /** PUT request is allow only if at least one upload is in progress */
    @Override
    public boolean allowPut() {
        return resumableUploadResourceManager.hasAnyResource();
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    /**
     * POST request returns upload URL with uploadId to call with successive PUT request.</br> The
     * body of POST request must contains the desired final file path, it can be relative path with
     * subfolder.
     */
    @Override
    public void handlePost() {
        try {
            String filePath = getRequest().getEntity().getText();
            if (filePath == null || filePath.isEmpty()) {
                getResponse()
                        .setStatus(
                                new Status(
                                        Status.CLIENT_ERROR_BAD_REQUEST,
                                        "POST data must contains upload file path"));
                return;
            }
            Reference ref = getRequest().getResourceRef();
            String baseURL = ref.getIdentifier();

            String uploadId = resumableUploadResourceManager.createUploadResource(filePath);

            Representation output =
                    new StringRepresentation(
                            "-----TO USE IN PUT-----\n"
                                    + baseURL
                                    + "/"
                                    + uploadId
                                    + "\n-----------------------\n",
                            MediaType.TEXT_PLAIN);
            Response response = getResponse();

            Series<Parameter> headers = new Form();
            headers.add("Location", baseURL + uploadId);
            getResponse().getAttributes().put("org.restlet.http.headers", headers);
            response.setEntity(output);
            response.setStatus(Status.SUCCESS_CREATED);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            getResponse().setStatus(new Status(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
            return;
        }
    }

    /**
     * PUT request is used to uploads file. </br> The request must contains the uploadId attribute
     * with the value returned by previous POST request. </br> If the PUT request is the first, it
     * must contains the header parameters "Content-Length: {total file size in bytes}" Successive
     * resume PUT request must contains the header parameters:</br>
     *
     * <ul>
     *   <li>Content-Length:{total size of bytes which must be uploaded}
     *   <li>Content-Range:{resume byte start byte index}-{file end byte index}/{total file size in
     *       bytes}
     * </ul>
     *
     * If the upload is incomplete, the PUT return the RANGE header attribute:</br> Range:
     * 0-{uploded end byte index}. If the upload is complete, the uploaded file is moved to REST
     * root folder and the PUT return the relative path of the file.</br> Sidecar file is created in
     * temporary folder to mark the upload as ended and provide information to successive GET
     * requests.
     */
    @Override
    public void handlePut() {
        /*
         * Check required parameters: - uploadId - Content-Length
         */
        String uploadId = RESTUtils.getAttribute(getRequest(), "uploadId");
        if (uploadId == null || uploadId.isEmpty()) {
            getResponse()
                    .setStatus(new Status(Status.CLIENT_ERROR_BAD_REQUEST, "Missing upload ID"));
            return;
        }
        if (!resumableUploadResourceManager.resourceExists(uploadId)) {
            getResponse()
                    .setStatus(new Status(Status.CLIENT_ERROR_BAD_REQUEST, "Unknow upload ID"));
            return;
        }
        Long totalByteToUpload = getContentLength();
        Long startPosition = 0L;
        Long endPosition = (totalByteToUpload - 1);
        Long totalFileSize = totalByteToUpload;
        if (totalByteToUpload == 0) {
            getResponse()
                    .setStatus(
                            new Status(
                                    Status.CLIENT_ERROR_LENGTH_REQUIRED,
                                    "Not zero Content-Length header must be specified"));
            return;
        }
        HeaderRange headerRange = getHeaderRange();
        if (headerRange != null) {
            try {
                if (headerRange.getMinimum() > headerRange.getMaximum()
                        || (headerRange.getRange().longValue() != totalByteToUpload)) {
                    getResponse()
                            .setStatus(
                                    Status.CLIENT_ERROR_BAD_REQUEST,
                                    "Range parameter values are not valid");
                    return;
                }
                startPosition = headerRange.getMinimum().longValue();
                endPosition = headerRange.getMaximum().longValue();
                totalFileSize = headerRange.getTotalFileSize();
                /*
                 * Validate resume request If resume is requested existing file must contains the
                 * number of bytes matching startPosition
                 */
                Boolean validated =
                        resumableUploadResourceManager.validateUpload(
                                uploadId,
                                totalByteToUpload,
                                startPosition,
                                endPosition,
                                totalFileSize);
                if (!validated) {
                    getResponse()
                            .setStatus(
                                    Status.CLIENT_ERROR_REQUESTED_RANGE_NOT_SATISFIABLE,
                                    "Range parameter values not meets partial uploaded files size");
                    return;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
                return;
            }
        } else {
            // Clear previous file if exists
            resumableUploadResourceManager.clearUpload(uploadId);
        }

        /*
         * Start upload
         */
        Long writedBytes =
                resumableUploadResourceManager.handleUpload(
                        uploadId, getRequest().getEntity(), startPosition);
        if (writedBytes < totalFileSize) {
            getResponse().setStatus(new Status(RESUME_INCOMPLETE.getCode()));
            Series<Parameter> headers = new Form();
            headers.add("Content-Length", "0");
            headers.add("Range", "0-" + (writedBytes - 1));
            getResponse().getAttributes().put("org.restlet.http.headers", headers);
        } else {
            String mappedPath;
            try {
                mappedPath = resumableUploadResourceManager.uploadDone(uploadId);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage());
                return;
            }
            Representation output = new StringRepresentation(mappedPath, MediaType.TEXT_PLAIN);
            Response response = getResponse();
            response.setEntity(output);
            response.setStatus(Status.SUCCESS_OK);
        }
    }

    /**
     * GET request with uploadId is used to get the status of upload If the upload is incomplete,
     * the GET return the RANGE header attribute:</br> Range: 0-{uploded end byte index}.
     */
    @Override
    public void handleGet() {
        String uploadId = RESTUtils.getAttribute(getRequest(), "uploadId");
        if (uploadId == null || uploadId.isEmpty()) {
            getResponse()
                    .setStatus(new Status(Status.CLIENT_ERROR_BAD_REQUEST, "Missing upload ID"));
            return;
        }

        try {
            if (!resumableUploadResourceManager.isUploadDone(uploadId)) {
                Long writedBytes = resumableUploadResourceManager.getWrittenBytes(uploadId);
                getResponse().setStatus(new Status(RESUME_INCOMPLETE.getCode()));
                Series<Parameter> headers = new Form();
                headers.add("Content-Length", "0");
                headers.add("Range", "0-" + (writedBytes - 1));
                getResponse().getAttributes().put("org.restlet.http.headers", headers);
            } else {
                Response response = getResponse();
                response.setStatus(Status.SUCCESS_OK);
            }
        } catch (IllegalStateException e) {
            getResponse().setStatus(new Status(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage()));
            return;
        } catch (IOException e) {
            getResponse().setStatus(new Status(Status.SERVER_ERROR_INTERNAL, e.getMessage()));
            return;
        }
    }

    private Long getContentLength() {
        Long contentLength = 0L;
        Object oHeaders = getRequest().getAttributes().get("org.restlet.http.headers");
        if (oHeaders != null) {
            Series<Parameter> headers = (Series<Parameter>) oHeaders;
            Parameter contentLengthParam = headers.getFirst("Content-Length", true);
            if (contentLengthParam != null) {
                String contentLengthStr = contentLengthParam.getValue();
                if (!contentLengthStr.isEmpty() && StringUtils.isNumeric(contentLengthStr)) {
                    contentLength = Long.parseLong(contentLengthStr);
                }
            }
        }
        return contentLength;
    }

    private HeaderRange getHeaderRange() {
        HeaderRange headerRange = null;
        Object oHeaders = getRequest().getAttributes().get("org.restlet.http.headers");
        if (oHeaders != null) {
            Series<Parameter> headers = (Series<Parameter>) oHeaders;
            Parameter contentRangeParam = headers.getFirst("Content-Range", true);
            if (contentRangeParam != null) {
                String contentRangeStr = contentRangeParam.getValue();
                String range = contentRangeStr.substring(6);
                String[] rangeParts = range.split("/");
                Long startPosition = Long.parseLong(rangeParts[0].split("-")[0]);
                Long endPosition = Long.parseLong(rangeParts[0].split("-")[1]);
                Long totalFileSize = Long.parseLong(rangeParts[1]);
                headerRange = new HeaderRange(startPosition, endPosition, totalFileSize);
            }
        }
        return headerRange;
    }

    private class HeaderRange {
        public final NumberRange<Long> contentRange;

        public final Long totalFileSize;

        public HeaderRange(Long startPosition, Long endPosition, Long totalFileSize) {
            super();
            this.contentRange = new NumberRange<Long>(Long.class, startPosition, endPosition);
            this.totalFileSize = totalFileSize;
        }

        public Double getMinimum() {
            return contentRange.getMinimum();
        }

        public Double getMaximum() {
            return contentRange.getMaximum();
        }

        public Long getTotalFileSize() {
            return totalFileSize;
        }

        public Double getRange() {
            return (contentRange.getMaximum() - contentRange.getMinimum());
        }
    }
}
