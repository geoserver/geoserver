/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import net.opengis.tjs10.DescribeKeyType;
import org.geoserver.ows.util.RequestUtils;
import org.geotools.xml.transform.TransformerBase;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Web Feature Service DescribeFeatureType operation.
 */
public class DescribeKey {
    /**
     * Catalog reference
     */
    private TJSCatalog catalog;

    /**
     * TJS service
     */
    private TJSInfo tjs;

    public DescribeKey(TJSInfo tjs, TJSCatalog catalog) {
        this.catalog = catalog;
        this.tjs = tjs;
    }

    public TJSInfo getTJS() {
        return tjs;
    }

    public void setTJS(TJSInfo tjs) {
        this.tjs = tjs;
    }

    public TJSCatalog getCatalog() {
        return catalog;
    }

    public void setCatalog(TJSCatalog catalog) {
        this.catalog = catalog;
    }


    public TransformerBase run(DescribeKeyType request)
            throws TJSException {

        List<String> provided = new ArrayList<String>();
        provided.add("1.0.0");
//        provided.add("1.1.0");
        List<String> requested = new ArrayList<String>();

        if (request.getVersion() != null)
            requested.add(request.getVersion()._100_LITERAL.getLiteral());
        else {
            requested.add("1.0.0");
        }
        String version = RequestUtils.getVersionPreOws(provided, requested);

        final DescribeKeyTransformer dkTransformer;
        if ("1.0.0".equals(version)) {
            dkTransformer = new DescribeKeyTransformer.TJS1_0(tjs, catalog);
        } else {
            throw new TJSException("Could not understand version:" + version);
        }
        if (request.getFrameworkURI() == null) {
            throw new TJSException("frameworkURI is not defined");
        }
        try {
            dkTransformer.setEncoding(Charset.forName(tjs.getGeoServer().getGlobal().getCharset()));
        } catch (Exception ex) {
            Logger.getLogger(GetCapabilities.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        return dkTransformer;
    }
}
