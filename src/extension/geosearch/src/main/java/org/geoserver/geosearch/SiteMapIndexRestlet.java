package org.geoserver.geosearch;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class SiteMapIndexRestlet extends GeoServerProxyAwareRestlet {

    private Catalog myCatalog;
    private String GEOSERVER_ROOT;
    protected static Namespace SITEMAP = Namespace.getNamespace("http://www.sitemaps.org/schemas/sitemap/0.9");

    public Catalog getData(){
        return myCatalog;
    }

    public void setCatalog(Catalog d){
        myCatalog = d;
    }

    public void handle(Request request, Response response){
        GEOSERVER_ROOT = getBaseURL(request);

        if (request.getMethod().equals(Method.GET)){
            doGet(request, response);
        } else { 
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }
    }

    /**
     * Creates a "sitemap.xml" with all the namespaces that contain
     * one or more featuretypes.
     * 
     * @param request
     * @param response
     */
    public void doGet(Request request, Response response){
        Document d = new Document();
        Element sitemapindex = new Element("sitemapindex", SITEMAP);
	//urlset.addNamespaceDeclaration(GEOSITEMAP);
        d.setRootElement(sitemapindex);

        buildGlobalSiteMap(sitemapindex);

        response.setEntity(new JDOMRepresentation(d));
    }
    
    private void buildGlobalSiteMap(Element sitemapindex)  {
        for (FeatureTypeInfo ft : getData().getFeatureTypes()) {
            try {
                if ((Boolean)ft.getMetadata().get("indexingEnabled")) {
                    String ftSitemap = 
                        GEOSERVER_ROOT + "/layers/" + ft.getName() + "/sitemap.xml";

                    addSitemap(sitemapindex, ftSitemap);
                }
            } catch( Exception e ) {
                // Do nothing ?
            }
        }
    }

    protected static void addSitemap(Element sitemapindex, String url){
        Element sitemapElement = new Element("sitemap", SITEMAP);
        Element loc = new Element("loc", SITEMAP);
        loc.setText(url);
        sitemapElement.addContent(loc);

        sitemapindex.addContent(sitemapElement);
    }

    public static String getParentUrl(String url){
        while (url.endsWith("/")){
            url = url.substring(0, url.length() - 1);
        }

        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1){
            url = url.substring(0, lastSlash);
        }

        return url;
    }
}
