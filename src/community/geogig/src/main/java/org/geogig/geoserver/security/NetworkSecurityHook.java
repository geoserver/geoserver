/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.security;

import com.google.common.base.Optional;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geogig.geoserver.config.ConfigStore;
import org.geogig.geoserver.config.WhitelistRule;
import org.geoserver.platform.GeoServerExtensions;
import org.locationtech.geogig.hooks.CannotRunGeogigOperationException;
import org.locationtech.geogig.hooks.CommandHook;
import org.locationtech.geogig.remotes.CloneOp;
import org.locationtech.geogig.remotes.FetchOp;
import org.locationtech.geogig.remotes.LsRemoteOp;
import org.locationtech.geogig.remotes.PushOp;
import org.locationtech.geogig.repository.AbstractGeoGigOp;
import org.locationtech.geogig.repository.Remote;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Classpath {@link CommandHook hook} that catches remotes related commands before they are executed
 * and validates them against the {@link WhitelistRule whitelist rules} to let them process or not.
 */
public final class NetworkSecurityHook implements CommandHook {

    @Override
    public <C extends AbstractGeoGigOp<?>> C pre(C command)
            throws CannotRunGeogigOperationException {
        if (command instanceof LsRemoteOp) {
            LsRemoteOp lsRemote = (LsRemoteOp) command;
            Optional<Remote> remote = lsRemote.getRemote();
            if (remote.isPresent()) {
                String url = remote.get().getFetchURL();
                checkRestricted(url);
            }
        } else if (command instanceof CloneOp) {
            CloneOp cloneOp = (CloneOp) command;
            URI url = cloneOp.getRemoteURI();
            if (null != url) {
                checkRestricted(url.toString());
            }
        } else if (command instanceof FetchOp) {
            FetchOp fetchOp = (FetchOp) command;
            for (Remote r : fetchOp.getRemotes()) {
                checkRestricted(r.getFetchURL());
            }
        } else if (command instanceof PushOp) {
            PushOp pushOp = (PushOp) command;
            Optional<Remote> remote = pushOp.getRemote();
            if (remote.isPresent()) {
                String url = remote.get().getPushURL();
                checkRestricted(url);
            }
        }

        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T post(
            AbstractGeoGigOp<T> command, Object retVal, RuntimeException potentialException)
            throws Exception {
        return (T) retVal;
    }

    @Override
    public boolean appliesTo(Class<? extends AbstractGeoGigOp<?>> clazz) {
        return LsRemoteOp.class.equals(clazz)
                || CloneOp.class.equals(clazz)
                || FetchOp.class.equals(clazz)
                || PushOp.class.equals(clazz);
    }

    private void checkRestricted(String remoteUrl) throws CannotRunGeogigOperationException {

        ConfigStore configStore = (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
        List<WhitelistRule> rules;
        try {
            rules = configStore.getWhitelist();
        } catch (IOException e) {
            throw new CannotRunGeogigOperationException(
                    "Unable to obtain the remotes white list: " + e.getMessage(), e);
        }
        if (!rules.isEmpty()) {
            for (WhitelistRule rule : rules) {
                if (!ruleBlocks(rule, remoteUrl)) {
                    return; // break fast if any of the rules doesn't block the url
                }
            }

            String msg =
                    String.format(
                            "Remote %s does not pass any white list rule: %s",
                            remoteUrl, new ArrayList<>(rules));
            throw new CannotRunGeogigOperationException(msg);
        }
    }

    private boolean ruleBlocks(WhitelistRule rule, String url) {
        URL parsed;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        final String host = parsed.getHost();
        if (host == null || parsed.getProtocol() == null || parsed.getProtocol().equals("file")) {
            return false;
        }

        if (rule.isRequireSSL() && !parsed.getProtocol().equals("https")) {
            return true;
        }

        String pattern = rule.getPattern();
        if (pattern.startsWith("[.*]")) {
            final String effectivePattern = rule.getPattern().substring("[.*]".length());
            return !host.endsWith(effectivePattern);
        } else {
            Matcher matcher = IP_ADDRESS_OR_CIDR_RANGE.matcher(pattern);
            String effectiveHost;
            if (host.startsWith("[") && host.endsWith("]")) { // signifies ipv6 address
                effectiveHost = host.substring(1, host.length() - 1);
            } else {
                effectiveHost = host;
            }
            if (matcher.matches()) {
                try {
                    IpAddressMatcher ipMatcher = new IpAddressMatcher(matcher.group());
                    return !ipMatcher.matches(effectiveHost);
                } catch (IllegalArgumentException e) {
                    // still account for malformed addresses since the regex is too loose
                    return false;
                }
            } else {
                return !host.equalsIgnoreCase(pattern);
            }
        }
    }

    private static final Pattern IP_ADDRESS_OR_CIDR_RANGE =
            Pattern.compile("^(([:\\p{XDigit}]+)|([\\d\\.]+))(/\\d+)?$");
}
