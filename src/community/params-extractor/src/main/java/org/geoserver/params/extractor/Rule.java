/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.net.URLDecoder;
import java.util.Optional;
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
    private final Optional<String> activation;
    private final String parameter;
    private final String transform;
    private final Optional<Integer> remove;
    private final Optional<String> combine;

    private final Pattern matchPattern;
    private final Optional<Pattern> activationPattern;

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
            Pattern matchPattern,
            Pattern activationPattern) {
        this.id = id;
        this.activated = activated;
        this.position = position;
        this.match = match;
        this.activation = Optional.ofNullable(activation);
        this.parameter = parameter;
        this.transform = transform;
        this.remove = Optional.ofNullable(remove);
        this.combine = Optional.ofNullable(combine);
        this.matchPattern = matchPattern;
        this.activationPattern = Optional.ofNullable(activationPattern);
    }

    public UrlTransform apply(UrlTransform urlTransform) {
        if (!activated) {
            Utils.debug(LOGGER, "Rule %s is deactivated.", id, urlTransform);
            return urlTransform;
        }
        Utils.debug(LOGGER, "Start applying rule %s to URL '%s'.", id, urlTransform);
        if (activationPattern.isPresent()) {
            if (!activationPattern.get().matcher(urlTransform.getOriginalRequestUri()).matches()) {
                Utils.debug(LOGGER, "Rule %s doesn't apply to URL '%s'.", id, urlTransform);
                return urlTransform;
            }
        }
        Matcher matcher = matchPattern.matcher(urlTransform.getOriginalRequestUri());
        if (!matcher.matches()) {
            Utils.debug(LOGGER, "Rule %s doesn't match URL '%s'.", id, urlTransform);
            return urlTransform;
        }
        urlTransform.removeMatch(matcher.group(remove.orElse(1)));
        urlTransform.addParameter(
                parameter, URLDecoder.decode(matcher.replaceAll(transform)), combine);
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
        return activation.orElse(null);
    }

    public String getParameter() {
        return parameter;
    }

    public String getTransform() {
        return transform;
    }

    public Integer getRemove() {
        return remove.orElse(null);
    }

    public String getCombine() {
        return combine.orElse(null);
    }
}
