/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.include.Include;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geotools.util.logging.Logging;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class LoginFormHTMLInclude extends Include {

    protected static final Logger LOGGER = Logging.getLogger(LoginFormHTMLInclude.class);

    /** serialVersionUID */
    private static final long serialVersionUID = 2413413223248385722L;

    private PackageResourceReference resourceReference;

    public LoginFormHTMLInclude(String id, PackageResourceReference packageResourceReference) {
        super(id);
        this.resourceReference = packageResourceReference;
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        String content = importAsString();
        replaceComponentTagBody(markupStream, openTag, content);
    }

    /**
     * Imports the contents of the url of the model object.
     * 
     * @return the imported contents
     */
    @Override
    protected String importAsString() {
        try {
            InputStream inputStream = this.resourceReference.getResource().getResourceStream()
                    .getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
            br.close();

            return sb.toString();
        } catch (Exception ex) {
            LOGGER.log(Level.FINEST, "Problem reading resource contents.", ex);
        }

        return "";
    }

}
