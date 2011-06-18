/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.sld;

import org.geoserver.wms.WMS;
import org.vfny.geoserver.Request;


public class PutStylesRequest extends Request {
    private MandatoryParameters mandatoryParameters = new MandatoryParameters();
    private OptionalParameters optionalParameters = new OptionalParameters();

    public PutStylesRequest(WMS wms) {
        super("SLD", "PutStyles", wms.getServiceInfo());
    }

    public void setMode(String mode) {
        this.mandatoryParameters.mode = mode;
    }

    public String getMode() {
        return this.mandatoryParameters.mode;
    }

    public void setSLD(String sld) {
        this.optionalParameters.sld = sld;
    }

    public String getSLD() {
        return this.optionalParameters.sld;
    }

    public void setSldBody(String sld_body) {
        this.optionalParameters.sld_body = sld_body;
    }

    public String getSldBody() {
        return this.optionalParameters.sld_body;
    }

    private class MandatoryParameters {
        /**
         * This gives the mode of the ?put?: either ?InsertAndReplace? or
         * ?ReplaceAll?. In InsertAndReplace mode, all new styles for
         * a layer are inserted and all existing styles which are defined in the
         * SLD are replaced. In ReplaceAll mode, all existing styles for a
         * layer are logically deleted, and then the SLD-defined styles are
         * inserted. This is similar to InsertAndReplace mode, except
         * that all styles not in the SLD are deleted.
         */
        String mode = ""; // either 'InsertAndReplace' or 'ReplaceAll'
    }

    private class OptionalParameters {
        /**
         * This parameter specifies a reference to an external SLD document.
         * It works in the same way as the SLD= parameter of the WMS
         * GetMap operation.
         */
        String sld = null;

        /**
         * This parameter allows an SLD document to be included directly in
         * an HTTP-GET request. It works in the same way as the
         * SLD_BODY= parameter of the WMS GetMap operation.
         */
        String sld_body = "";
    }
}
