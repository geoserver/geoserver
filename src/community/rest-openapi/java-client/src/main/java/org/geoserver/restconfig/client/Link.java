package org.geoserver.restconfig.client;

import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.Value;
import org.geoserver.openapi.v1.model.NamedLink;

/**
 * Value class to unify the mismatch in the geoserver REST API regarding mixed usage of "href" and
 * "link" attributes when returning a list of {@link NamedLink named links}
 */
public @Value class Link {

    private String name;
    private String link;

    public static List<Link> map(@NonNull List<NamedLink> namedLinks) {
        return namedLinks.stream().map(Link::map).collect(Collectors.toList());
    }

    public static Link map(@NonNull NamedLink namedLink) {
        String linkAtt = namedLink.getHref() == null ? namedLink.getLink() : namedLink.getHref();
        return new Link(namedLink.getName(), linkAtt);
    }
}
