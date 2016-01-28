/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.rest.GeoServerServletConverter;
import org.geoserver.rest.PageInfo;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.rest.RestletException;
import org.locationtech.geogig.rest.TaskStatusResource;
import org.locationtech.geogig.rest.osm.OSMRouter;
import org.locationtech.geogig.rest.repository.CommandResource;
import org.locationtech.geogig.rest.repository.FixedEncoder;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.rest.repository.RepositoryRouter;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.springframework.beans.BeansException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.google.common.base.Charsets;
import com.noelios.restlet.application.Decoder;
import com.noelios.restlet.ext.servlet.ServletConverter;

/**
 * Simple AbstractController implementation that does the translation between Spring requests and
 * Restlet requests.
 */
public class GeogigDispatcher extends AbstractController {
    /** HTTP method "PUT" */
    public static final String METHOD_PUT = "PUT";

    /** HTTP method "DELETE" */
    public static final String METHOD_DELETE = "DELETE";

    /**
     * logger
     */
    static Logger LOG = Logging.getLogger(GeogigDispatcher.class);

    private GeoServerRepositoryProvider repositoryProvider;

    /**
     * converter for turning servlet requests into resetlet requests.
     */
    private ServletConverter converter;

    /**
     * the root restlet router
     */
    private Restlet root;

    public GeogigDispatcher() {
        this.repositoryProvider = new GeoServerRepositoryProvider();
        setSupportedMethods(new String[] { METHOD_GET, METHOD_POST, METHOD_PUT, METHOD_DELETE,
                METHOD_HEAD });
    }

    @Override
    protected void initApplicationContext() throws BeansException {
        super.initApplicationContext();

        converter = new GeoServerServletConverter(getServletContext());
        Router router = createInboundRoot();

        org.restlet.Context context = null;// getContext();
        FixedEncoder encoder = new FixedEncoder(context);
        // needed for the Encoder to wrap the incoming requests if they come with
        // "Content-Type: gzip"
        encoder.setEncodeRequest(false);
        encoder.setEncodeResponse(true);
        encoder.setNext(router);

        Decoder decoder = new Decoder(context);
        decoder.setDecodeRequest(true);
        decoder.setDecodeResponse(false);
        decoder.setNext(encoder);

        root = decoder;
        converter.setTarget(root);
    }

    public Router createInboundRoot() {
        Router router = createRoot();

        router.attach("", RepositoryListResource.class);
        router.attach("/", RepositoryListResource.class);

        router.attach("/tasks.{extension}", TaskStatusResource.class);
        router.attach("/tasks", TaskStatusResource.class);
        router.attach("/tasks/{taskId}.{extension}", TaskStatusResource.class);
        router.attach("/tasks/{taskId}", TaskStatusResource.class);

        Router osm = new OSMRouter();
        router.attach("/{repository}/osm", osm);

        router.attach("/{repository}.{extension}", RepositoryResource.class);
        router.attach("/{repository}", RepositoryResource.class);
        router.attach("/{repository}/repo", makeRepoRouter());
        router.attach("/{repository}/{command}.{extension}", CommandResource.class);
        router.attach("/{repository}/{command}", CommandResource.class);

        return router;
    }

    public static Router makeRepoRouter() {
        return new RepositoryRouter();
    }

    @Override
    public ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {

        try {
            converter.service(req, resp);
        } catch (Exception e) {
            if (e instanceof CommandSpecException) {
                String msg = e.getMessage();
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                if (msg != null) {
                    resp.getOutputStream().write(msg.getBytes(Charsets.UTF_8));
                }
                return null;
            }
            RestletException re = null;
            if (e instanceof RestletException) {
                re = (RestletException) e;
            }
            if (re == null && e.getCause() instanceof RestletException) {
                re = (RestletException) e.getCause();
            }

            if (re != null) {
                resp.setStatus(re.getStatus().getCode());

                String reStr = re.getRepresentation().getText();
                if (reStr != null) {
                    LOG.severe(reStr);
                    resp.setContentType("text/plain");
                    resp.getOutputStream().write(reStr.getBytes());
                }

                // log the full exception at a higher level
                LOG.log(Level.SEVERE, "", re);
            } else {
                LOG.log(Level.SEVERE, "", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                if (e.getMessage() != null) {
                    resp.getOutputStream().write(e.getMessage().getBytes());
                }
            }
            resp.getOutputStream().flush();
        }

        return null;
    }

    public Router createRoot() {
        Router router = new Router() {

            @Override
            protected synchronized void init(Request request, Response response) {
                super.init(request, response);
                if (!isStarted()) {
                    return;
                }
                request.getAttributes().put(RepositoryProvider.KEY, repositoryProvider);
                // set the page uri's

                // http://host:port/appName
                String baseURL = request.getRootRef().getParentRef().toString();
                String rootPath = request.getRootRef().toString().substring(baseURL.length());
                String pagePath = request.getResourceRef().toString().substring(baseURL.length());
                String basePath = null;
                if (request.getResourceRef().getBaseRef() != null) {
                    basePath = request.getResourceRef().getBaseRef().toString()
                            .substring(baseURL.length());
                }

                // strip off the extension
                String extension = ResponseUtils.getExtension(pagePath);
                if (extension != null) {
                    pagePath = pagePath.substring(0, pagePath.length() - extension.length() - 1);
                }

                // trim leading slash
                if (pagePath.endsWith("/")) {
                    pagePath = pagePath.substring(0, pagePath.length() - 1);
                }
                // create a page info object and put it into a request attribute
                PageInfo pageInfo = new PageInfo();
                pageInfo.setBaseURL(baseURL);
                pageInfo.setRootPath(rootPath);
                pageInfo.setBasePath(basePath);
                pageInfo.setPagePath(pagePath);
                pageInfo.setExtension(extension);
                request.getAttributes().put(PageInfo.KEY, pageInfo);
            }

        };
        return router;
    }
}
