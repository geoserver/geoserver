/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.Variants.JSON;
import static org.locationtech.geogig.rest.Variants.XML;
import static org.locationtech.geogig.rest.repository.RESTUtils.repositoryProvider;

import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.format.FreemarkerFormat;
import org.locationtech.geogig.rest.JettisonRepresentation;
import org.locationtech.geogig.rest.RestletException;
import org.locationtech.geogig.rest.Variants;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Access point to a single repository.
 * <p>
 * Defines the following repository ends points:
 * <ul>
 * <li>{@code /manifest}
 * </ul>
 */
public class RepositoryResource extends Resource {

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
        List<MediaType> acceptedMediaTypes = Lists.transform(getRequest().getClientInfo()
                .getAcceptedMediaTypes(), new Function<Preference<MediaType>, MediaType>() {
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
            GeoServerRepositoryProvider repoFinder = (GeoServerRepositoryProvider) repositoryProvider(request);
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

    private static class RepositorytRepresentation extends JettisonRepresentation {

        private RepositoryInfo repo;

        public RepositorytRepresentation(MediaType mediaType, String baseURL, RepositoryInfo repo) {
            super(mediaType, baseURL);
            this.repo = repo;
        }

        @Override
        protected void write(XMLStreamWriter w) throws XMLStreamException {
            w.writeStartElement("repository");
            element(w, "id", repo.getId());
            element(w, "name", repo.getName());
            element(w, "location", repo.getLocation());
            w.writeEndElement();
        }
    }

}
