/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Logger;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.util.IOUtils;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.legendgraphic.JSONLegendGraphicBuilder;

/** @author ImranR */
// GetLegendGraphicRequest

public class CascadedLegendRequest extends LegendRequest {

    Logger LOGGER = Logger.getLogger(CascadedLegendRequest.class.getName());

    GetLegendGraphicRequest request;

    org.geotools.ows.wms.request.GetLegendGraphicRequest remoteLegendGraphicRequest;

    JSONObject jsonLegend;

    public CascadedLegendRequest(GetLegendGraphicRequest request) {
        this.request = request;
        LegendInfo info = new LegendInfoImpl();

        info.setFormat(this.request.getFormat());
        info.setWidth(this.request.getWidth());
        info.setHeight(this.request.getHeight());

        super.setLegendInfo(info);
    }

    public org.geotools.ows.wms.request.GetLegendGraphicRequest getRemoteLegendGraphicRequest() {
        return remoteLegendGraphicRequest;
    }

    public void setRemoteLegendGraphicRequest(
            org.geotools.ows.wms.request.GetLegendGraphicRequest remoteLegendGraphicRequest) {
        // copying params to remote request
        this.remoteLegendGraphicRequest = remoteLegendGraphicRequest;
        Map<String, String> params = request.getRawKvp();
        // relaying request params
        params.keySet()
                .forEach(
                        k -> {
                            // layer param has already been read from WMSStore
                            // we need to use the layer name on remote server
                            if (!k.equalsIgnoreCase("layer"))
                                this.remoteLegendGraphicRequest.setProperty(
                                        k, String.valueOf(params.get(k)));
                        });
        // generating URL

        super.getLegendInfo()
                .setOnlineResource(this.remoteLegendGraphicRequest.getFinalURL().toExternalForm());
        LOGGER.fine(
                String.format(
                        "Cascaded GetLegend Request URL:%s",
                        super.getLegendInfo().getOnlineResource()));
    }

    public GetLegendGraphicRequest getDelegate() {
        return request;
    }

    public void setDelegate(GetLegendGraphicRequest delegate) {
        this.request = delegate;
    }

    public JSONArray getCascadedJSONRules() {
        InputStream is = null;
        BufferedReader bufferedReader = null;
        try {
            // execute the request and fetch JSON
            is = new URL(super.getLegendInfo().getOnlineResource()).openStream();
            bufferedReader =
                    new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = bufferedReader.read()) != -1) {
                sb.append((char) cp);
            }
            String jsonText = sb.toString();
            LOGGER.fine(String.format("Cascaded GetLegend Request JSON Response:%s", jsonText));
            JSONObject jsonLegend = JSONObject.fromObject(jsonText);
            JSONArray layerLegends = jsonLegend.getJSONArray(JSONLegendGraphicBuilder.LEGEND);
            JSONArray cascadedRules =
                    layerLegends.getJSONObject(0).getJSONArray(JSONLegendGraphicBuilder.RULES);
            return cascadedRules;
            // return jsonLegend;
        } catch (Exception e) {
            IOUtils.closeQuietly(bufferedReader);
            IOUtils.closeQuietly(is);
        }

        return null;
    }
}
