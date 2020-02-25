/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.net.URLDecoder;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotools.util.logging.Logging;

public final class Rule {

    private static final Logger LOGGER = Logging.getLogger(Rule.class);

    private final String id;
    private final Boolean activated;
    private final Integer position;
    private final String match;
    private final String activation;
    private final String parameter;
    private final String transform;
    private final Integer remove;
    private final String combine;

    private final Pattern matchPattern;
    private final Pattern activationPattern;

    private final Boolean repeat;

    public Rule(
            String id,
            Boolean activated,
            Integer position,
            String match,
            String activation,
            String parameter,
            String transform,
            Integer remove,
            String combine,
            Boolean repeat,
            Pattern matchPattern,
            Pattern activationPattern) {
        this.id = id;
        this.activated = activated;
        this.position = position;
        this.match = match;
        this.activation = activation;
        this.parameter = parameter;
        this.transform = transform;
        this.remove = remove;
        this.combine = combine;
        this.repeat = repeat;
        this.matchPattern = matchPattern;
        this.activationPattern = activationPattern;
    }

    public UrlTransform apply(UrlTransform urlTransform) {
        if (!activated) {
            Utils.debug(LOGGER, "Rule %s is deactivated.", id, urlTransform);
            return urlTransform;
        }
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
        urlTransform.removeMatch(matcher.group(remove != null ? remove : 1));
        urlTransform.addParameter(
                parameter, URLDecoder.decode(matcher.replaceAll(transform)), combine, repeat);
        return urlTransform;
    }

    public String getId() {
        return id;
    }

    public Boolean getActivated() {
        return activated;
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

    public Boolean getRepeat() {
        return repeat;
    }
}
