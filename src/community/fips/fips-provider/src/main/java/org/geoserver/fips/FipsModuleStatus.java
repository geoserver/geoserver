/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fips;

import java.util.Optional;
import org.geoserver.platform.ModuleStatus;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;

/** Reports the FIPS module, so an admin can tell from /rest/about/status that this is a FIPS deployment. */
public class FipsModuleStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-fips-provider";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.of("crypto-provider");
    }

    @Override
    public String getName() {
        return "GeoServer FIPS 140-3 support";
    }

    @Override
    public Optional<String> getVersion() {
        Version version = GeoTools.getVersion(FipsModuleStatus.class);
        return Optional.ofNullable(version).map(Version::toString);
    }

    /** The module is on the classpath, so the FIPS jars are too. The self tests say whether they work. */
    @Override
    public boolean isAvailable() {
        return FipsSetup.isFipsModuleReady();
    }

    /** Enabled means non-approved algorithms throw rather than silently run. */
    @Override
    public boolean isEnabled() {
        return FipsSetup.isApprovedOnlyRequested();
    }

    /** The same content as the FIPS tab of the status page, so an admin can read it over REST. */
    @Override
    public Optional<String> getMessage() {
        StringBuilder message = new StringBuilder();
        for (FipsSetup.Fact fact : FipsSetup.getFacts()) {
            message.append(fact.label()).append(": ").append(fact.value()).append("\n");
        }
        for (FipsSetup.AlgorithmStatus algorithm : FipsSetup.getRequiredAlgorithms()) {
            message.append(algorithm.type())
                    .append(" ")
                    .append(algorithm.algorithm())
                    .append(": ")
                    .append(algorithm.available() ? "yes" : "no")
                    .append("\n");
        }
        return Optional.of(message.toString());
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.of("https://docs.geoserver.org/latest/en/user/community/fips/index.html");
    }

    @Override
    public Category getCategory() {
        return Category.COMMUNITY;
    }
}
