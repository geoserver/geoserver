/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import javax.servlet.http.HttpServletResponse;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/gsuser")
public class GsUserController {

    @GetMapping()
    @ResponseBody
    public String handleGet(HttpServletResponse response) {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Function function = ff.function("env", ff.literal("GSUSER"), ff.literal("USER_NOT_FOUND"));
        String result = function.evaluate(null, String.class);
        response.setContentType("text/plain");
        return result;
    }
}
