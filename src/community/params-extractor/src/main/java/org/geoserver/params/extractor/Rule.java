/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import org.geotools.util.logging.Logging;

import java.net.URLDecoder;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Rule {

    private static final Logger LOGGER = Logging.getLogger(Rule.class);

    private final String id;
    private final Integer position;
    private final String match;
    private final String activation;
    private final String parameter;
    private final String transform;
    private final Integer remove;
    private final String combine;

    private final Pattern matchPattern;
    private final Pattern activationPattern;

    public Rule(String id, Integer position, String match, String activation, String parameter,
                String transform, Integer remove, String combine, Pattern matchPattern, Pattern activationPattern) {
        this.id = id;
        this.position = position;
        this.match = match;
        this.activation = activation;
        this.parameter = parameter;
        this.transform = transform;
        this.remove = remove;
        this.combine = combine;
        this.matchPattern = matchPattern;
        this.activationPattern = activationPattern;
    }

    public UrlTransform apply(UrlTransform urlTransform) {
        Utils.debug(LOGGER, "Start applying rule %s to URL '%s'.", id, urlTransform);
        if (activationPattern != null) {
            if (!activationPattern.matcher(urlTransform.getOriginalRequestUri()).matches()) {
                Utils.debug(LOGGER, "Rule %s doesn't apply to URL '%s'.", id, urlTransform);
                return urlTransform;
            }
        }
        Matcher matcher = matchPattern.matcher(urlTransform.getOriginalRequestUri());
        if (!matcher.matches()) {
            Utils.debug(LOGGER, "Rule %s doesn't match URL '%s'.", id, urlTransform);
            return urlTransform;
        }
        urlTransform.removeMatch(matcher.group(Utils.withDefault(remove, 1)));
        urlTransform.addParameter(parameter, URLDecoder.decode(matcher.replaceAll(transform)), combine);
        return urlTransform;
    }

    public String getId() {
        return id;
    }

    public Integer getPosition() {
        return position;
    }

    public String getMatch() {
        return match;
    }

    public String getActivation() {
        return activation;
    }

    public String getParameter() {
        return parameter;
    }

    public String getTransform() {
        return transform;
    }

    public Integer getRemove() {
        return remove;
    }

    public String getCombine() {
        return combine;
    }
}
