/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.TJSInfo;
import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.web.dataset.DatasetPage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.validation.validator.UrlValidator;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.wicket.FileExistsValidator;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;


@SuppressWarnings("serial")
public class TJSAdminPage extends BaseServiceAdminPage<TJSInfo> {

    protected Class<TJSInfo> getServiceClass() {
        return TJSInfo.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void build(final IModel info, Form form) {

        form.add(new TextField("tjsServerBaseURL").add(new UrlValidator()));

        AjaxLink makeJoins = new AjaxLink("makeJoins") {
            @Override
            public void onClick(AjaxRequestTarget target) {

                TJSCatalog catalog = TJSExtension.getTJSCatalog();

                for(DatasetInfo dsi : catalog.getDatasets(null)) {
                    if (dsi.getAutoJoin()) {
                        joinMap(dsi);
                    }
                }

                setResponsePage(DatasetPage.class);
            }
        };
        form.add(makeJoins);
    }

    private void joinMap(DatasetInfo dsi) {

            final ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
            final String url = servletWebRequest.getHttpServletRequest().getRequestURL().toString();
            final String baseURL = url.substring(0, url.indexOf("/web/") + 1);//+1 para dejarle el / final

            StringBuilder attributes = new StringBuilder("");
            for (ColumnInfo ci : dsi.getColumns()) {
                attributes.append(ci.getName());
                attributes.append(",");
            }
            if (!attributes.equals("")) {
                attributes.deleteCharAt(attributes.length() - 1);//borrar la coma del final
            }

            final String getDataURL = baseURL +
                    "ows?Service=TJS&Version=1.0&Request=GetData&FrameworkURI=" + dsi.getFramework().getUri() +
                    "&DatasetURI=" + dsi.getDatasetUri() +
                    "&attributes=" + attributes.toString();

            String encodedGetDataURL = "";
            try {
                encodedGetDataURL = URLEncoder.encode(getDataURL, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                ;//no dería acurrir nunca
            }

            final String request = baseURL + "ows?Service=TJS&Version=1.0&Request=JoinData"
                    + "&FrameworkURI=" + dsi.getFramework().getUri()
                    + "&GetDataURL=" + encodedGetDataURL;

            try {
                final URL reqURL = new URL(request);
                final InputStream inputStream = reqURL.openStream();
                LOGGER.info("Respuesta de join Data:");
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null)
                    LOGGER.info(line);
                inputStream.close();
            } catch (MalformedURLException e) {
                ;
            } catch (IOException e) {
                ;
            }

        }


    protected String getServiceName() {
        return "TJS";
    }
}
