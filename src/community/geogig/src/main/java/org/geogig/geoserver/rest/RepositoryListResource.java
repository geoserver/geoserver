/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.Variants.JSON;
import static org.locationtech.geogig.rest.Variants.XML;
import static org.locationtech.geogig.rest.repository.RESTUtils.repositoryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.format.FreemarkerFormat;
import org.locationtech.geogig.rest.JettisonRepresentation;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RepositoryListResource extends Resource {

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
            String templateName = "RepositoryListResource.ftl";
            FreemarkerFormat format = new FreemarkerFormat(templateName, getClass(),
                    MediaType.TEXT_HTML);
            representation = format.toRepresentation(getMap());
        } else {

            List<RepositoryInfo> repos = getRepositories();
            final String baseURL = getRequest().getRootRef().toString();
            representation = new RepositoryListRepresentation(variant.getMediaType(), baseURL,
                    repos);
        }
        return representation;
    }

    public Map<String, Object> getMap() {
        List<RepositoryInfo> repositories = getRepositories();

        Map<String, Object> map = Maps.newHashMap();
        map.put("repositories", repositories);
        map.put("page", getPageInfo());
        return map;
    }

    protected PageInfo getPageInfo() {
        return (PageInfo) getRequest().getAttributes().get(PageInfo.KEY);
    }

    private List<RepositoryInfo> getRepositories() {
        Request request = getRequest();
        GeoServerRepositoryProvider repoFinder = (GeoServerRepositoryProvider) repositoryProvider(request);

        List<RepositoryInfo> repos = new ArrayList<>(repoFinder.findRepositories());

        return repos;
    }

    private static class RepositoryListRepresentation extends JettisonRepresentation {

        private List<RepositoryInfo> repos;

        public RepositoryListRepresentation(MediaType mediaType, String baseURL,
                List<RepositoryInfo> repos) {
            super(mediaType, baseURL);
            this.repos = repos;
        }

        @Override
        protected void write(XMLStreamWriter w) throws XMLStreamException {
            w.writeStartElement("repositories");
            for (RepositoryInfo repo : repos) {
                write(w, repo);
            }
            w.writeEndElement();
        }

        private void write(XMLStreamWriter w, RepositoryInfo repo) throws XMLStreamException {
            w.writeStartElement("repository");
            element(w, "id", repo.getId());
            element(w, "name", repo.getName());
            encodeAlternateAtomLink(w, repo.getId());
            w.writeEndElement();
        }

    }
}
