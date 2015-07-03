/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.FreemarkerFormat;
import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.Route;
import org.restlet.Router;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;


/**
 * The IndexResource class lists the paths available for a Router.
 * Specifically, it auto-generates an index page containing all 
 * non-templated paths relative to the router root.
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
class IndexRestlet extends Finder{
    private Router myRouter;

    public IndexRestlet(Router r){
        myRouter = r;
    }

    public IndexRestlet(Context con, Router router) {
        super(con);
        myRouter = router;
    }

    public Resource findTarget(Request req, Response resp){
        Resource r = new IndexResource(getContext(),req,resp);
        r.init(getContext(), req, resp);
        return r;
    }

    private class IndexResource extends MapResource{

        public IndexResource(Context context, Request request, Response response) {
            super(context, request, response);
        }

        @Override
        protected List<DataFormat> createSupportedFormats(
                Request request, Response response) {
            ArrayList l = new ArrayList();

            l.add(new FreemarkerFormat("templates/index.ftl", getClass(), MediaType.TEXT_HTML));
            //l.add(null, m.get(MediaType.TEXT_HTML));

            return l;
        }

        public Map getMap() {
            Map m = new HashMap();
            m.put("links", getLinkList());
            m.put("page", getPageInfo());

            return m;
        }

        private List getLinkList() {
            List l = new ArrayList();

            Iterator it = myRouter.getRoutes().iterator();

            while (it.hasNext()) {
                Route r = (Route) it.next();
                String pattern = r.getTemplate().getPattern();

                if (!pattern.contains("{") && (pattern.length() > 1)) {
                    l.add(pattern.substring(1)); // trim leading slash
                }
            }

            return l;
        }
    }
}
