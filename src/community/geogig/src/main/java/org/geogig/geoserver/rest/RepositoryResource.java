/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.Variants.JSON;
import static org.locationtech.geogig.rest.Variants.XML;
import static org.locationtech.geogig.web.api.RESTUtils.repositoryProvider;

import java.util.List;
import java.util.Map;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.format.FreemarkerFormat;
import org.locationtech.geogig.rest.RestletException;
import org.locationtech.geogig.rest.StreamingWriterRepresentation;
import org.locationtech.geogig.rest.Variants;
import org.locationtech.geogig.rest.repository.DeleteRepository;
import org.locationtech.geogig.web.api.StreamWriterException;
import org.locationtech.geogig.web.api.StreamingWriter;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Access point to a single repository. Provides a Repository information response for <b>GET</b>
 * requests. Performs a Repository delete operation for <b>DELETE</b> requests.
 */
public class RepositoryResource extends DeleteRepository {

    private static final Variant HTML = new Variant(MediaType.TEXT_HTML);

    @Override
    public void init(Context context, Request request, Response response) {
        super.init(context, request, response);
        List<Variant> variants = getVariants();
        variants.add(HTML);
        variants.add(XML);
        variants.add(JSON);
    }

    @Override
    public Variant getPreferredVariant() {
        Optional<Variant> byExtension = Variants.getVariantByExtension(getRequest(), getVariants());
        if (byExtension.isPresent()) {
            return byExtension.get();
        }
        List<MediaType> acceptedMediaTypes = Lists.transform(
                getRequest().getClientInfo().getAcceptedMediaTypes(),
                new Function<Preference<MediaType>, MediaType>() {
                    @Override
                    public MediaType apply(Preference<MediaType> input) {
                        return input.getMetadata();
                    }
                });
        if (acceptedMediaTypes.contains(MediaType.TEXT_HTML)) {
            return HTML;
        }
        if (acceptedMediaTypes.contains(MediaType.TEXT_XML)) {
            return XML;
        }
        if (acceptedMediaTypes.contains(MediaType.APPLICATION_JSON)) {
            return JSON;
        }

        return XML;
    }

    @Override
    public Representation getRepresentation(Variant variant) {
        Representation representation;
        if (HTML.equals(variant)) {
            String templateName = "RepositoryResource.ftl";
            FreemarkerFormat format = new FreemarkerFormat(templateName, getClass(),
                    MediaType.TEXT_HTML);
            representation = format.toRepresentation(getMap());
        } else {
            Request request = getRequest();
            GeoServerRepositoryProvider repoFinder = (GeoServerRepositoryProvider) repositoryProvider(
                    request);
            Optional<RepositoryInfo> repository = repoFinder.findRepository(request);
            if (!repository.isPresent()) {
                throw new RestletException("not found", Status.CLIENT_ERROR_NOT_FOUND);
            }
            final String baseURL = getRequest().getRootRef().toString();
            RepositoryInfo repoInfo = repository.get();
            representation = new RepositorytRepresentation(variant.getMediaType(), baseURL,
                    repoInfo);
        }
        return representation;
    }

    private Map<String, Object> getMap() {

        Map<String, Object> map = Maps.newHashMap();
        PageInfo pageInfo = getPageInfo();
        map.put("page", pageInfo);

        map.put("Manifest", "manifest");
        return map;
    }

    protected PageInfo getPageInfo() {
        return (PageInfo) getRequest().getAttributes().get(PageInfo.KEY);
    }

    private static class RepositorytRepresentation extends StreamingWriterRepresentation {

        private final RepositoryInfo repo;

        public RepositorytRepresentation(MediaType mediaType, String baseURL, RepositoryInfo repo) {
            super(mediaType, baseURL);
            this.repo = repo;
        }

        @Override
        public void write(StreamingWriter w) throws StreamWriterException {
            w.writeStartElement("repository");
            w.writeElement("id", repo.getId());
            w.writeElement("name", repo.getRepoName());
            w.writeElement("location", repo.getMaskedLocation());
            w.writeEndElement();
        }
    }

}
