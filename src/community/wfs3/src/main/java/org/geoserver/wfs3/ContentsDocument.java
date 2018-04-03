package org.geoserver.wfs3;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.Filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A class representing the WFS3 server "contents" in a way that Jackson can easily translate to JSON/YAML (and
 * can be used as a Freemarker template model)
 */
public class ContentsDocument {
    
    public static final String REL_SELF = "self";
    public static final String REL_ALTERNATE = "alternate";
    public static final String REL_SERVICE = "service";
    public static final String REL_ABOUT = "about";
    public static final String REL_DESCRIBEDBY = "describedBy";

    static class Link {
        String href;
        String rel;
        String type;
        String title;

        public Link() {
        }

        public Link(String href, String rel, String type, String title) {
            this.href = href;
            this.rel = rel;
            this.type = type;
            this.title = title;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public String getRel() {
            return rel;
        }

        public void setRel(String rel) {
            this.rel = rel;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }
    
    static class Collection {
        String collectionId;
        String title;
        String description;
        double[] extent;
        List<Link> links = new ArrayList<>();

        public String getCollectionId() {
            return collectionId;
        }

        public void setCollectionId(String collectionId) {
            this.collectionId = collectionId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public double[] getExtent() {
            return extent;
        }

        public void setExtent(double[] extent) {
            this.extent = extent;
        }

        public List<Link> getLinks() {
            return links;
        }

        public void addLink(Link link) {
            links.add(link);
        }

    }
    
    private final Catalog catalog;
    private final WFSInfo wfs;
    private final List<Link> links = new ArrayList<>();
    private final ContentRequest request;
    
    public ContentsDocument(ContentRequest request, WFSInfo wfs, Catalog catalog) {
        this.wfs = wfs;
        this.catalog = catalog;
        this.request = request;

        // TODO: make this pluggable based on the available responses

        // setting up the contents link
        String baseUrl = request.getBaseUrl();
        String contentsUrl = ResponseUtils.buildURL(baseUrl, "wfs3/", null, URLMangler.URLType.SERVICE);
        addLink(new ContentsDocument.Link(contentsUrl, ContentsDocument.REL_SELF, BaseRequest.JSON_MIME, "This" +
                " document"));
        // uncomment when HTML format is supported
//        String contentsHtmlUrl = ResponseUtils.buildURL(baseUrl, "wfs3/", Collections.singletonMap("f", "html"),
//                URLMangler.URLType.SERVICE);
//        addLink(new ContentsDocument.Link(contentsHtmlUrl, ContentsDocument.REL_ALTERNATE, BaseRequest
//                .HTML_MIME, "This document as HTML"));
        String contentsYamlUrl = ResponseUtils.buildURL(baseUrl, "wfs3/", Collections.singletonMap("f", "yaml"),
                URLMangler.URLType.SERVICE);
        addLink(new ContentsDocument.Link(contentsYamlUrl, ContentsDocument.REL_ALTERNATE, BaseRequest
                .YAML_MIME, "This document as YAML"));

        // setting up the API links
        String apiUrl = ResponseUtils.buildURL(baseUrl, "wfs3/api", null, URLMangler.URLType.SERVICE);
        addLink(new ContentsDocument.Link(apiUrl, ContentsDocument.REL_SERVICE, BaseRequest.JSON_MIME, "The " +
                "OpenAPI definition as JSON"));
//        String apiHtmlUrl = ResponseUtils.buildURL(baseUrl, "wfs3/api", Collections.singletonMap("f", "html"),
//                URLMangler.URLType.SERVICE);
//        addLink(new ContentsDocument.Link(apiHtmlUrl, ContentsDocument.REL_SERVICE, BaseRequest.HTML_MIME,
//                "The OpenAPI definition as HTML"));
        String apiYamlUrl = ResponseUtils.buildURL(baseUrl, "wfs3/api", Collections.singletonMap("f", "yaml"),
                URLMangler.URLType.SERVICE);
        addLink(new ContentsDocument.Link(apiYamlUrl, ContentsDocument.REL_SERVICE, BaseRequest.YAML_MIME,
                "The OpenAPI definition as YAML"));
    }
    
    public void addLink(Link link) {
        links.add(link);
    }

    public List<Link> getLinks() {
        return links;
    }
    
    public Iterator<Collection> getCollections() {
        CloseableIterator<FeatureTypeInfo> featureTypes = catalog.list(FeatureTypeInfo.class, Filter.INCLUDE);
        return new Iterator<Collection>() {
            
            Collection next;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                
                boolean hasNext = featureTypes.hasNext();
                if (!hasNext) {
                    featureTypes.close();
                    return false;
                } else {
                    try {
                        FeatureTypeInfo featureType = featureTypes.next();
                        next = mapToCollection(featureType);
                        return true;
                    } catch(Exception e) {
                        featureTypes.close();
                        throw new ServiceException("Failed to iterate over the feature types in the catalog", e);
                    }
                }
            }

            @Override
            public Collection next() {
                Collection result = next;
                this.next = null;
                return result;
            }
        };
    }

    private Collection mapToCollection(FeatureTypeInfo featureType) {
        Collection collection = new Collection();
        
        // basic info
        String collectionId = NCNameResourceCodec.encode(featureType);
        collection.setCollectionId(collectionId);
        collection.setTitle(featureType.getTitle());
        collection.setDescription(featureType.getDescription());
        ReferencedEnvelope bbox = featureType.getLatLonBoundingBox();
        collection.setExtent(new double[] {bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(), bbox.getMaxY()});
        
        // links
        List<String> formats = DefaultWebFeatureService30.getAvailableFormats();
        String baseUrl = request.getBaseUrl();
        for (String format : formats) {
            String apiUrl = ResponseUtils.buildURL(baseUrl, "wfs3/" + collectionId, Collections.singletonMap("f", format), URLMangler.URLType.SERVICE);
            collection.addLink(new ContentsDocument.Link(apiUrl, ContentsDocument.REL_ABOUT, format, collectionId + " as " + format));
        }

        return collection;
    }

}
