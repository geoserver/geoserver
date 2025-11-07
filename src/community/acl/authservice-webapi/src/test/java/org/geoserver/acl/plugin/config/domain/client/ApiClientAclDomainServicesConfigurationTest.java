/* (c) 2023  Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.config.domain.client;

// import static org.assertj.core.api.Assertions.assertThat;

// import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ApiClientAclDomainServicesConfigurationTest {
    //
    //    ApplicationContextRunner runner =
    //            new ApplicationContextRunner().withUserConfiguration(ApiClientAclDomainServicesConfiguration.class);
    //
    //    @Test
    //    void testMissingConfig() {
    //        runner.run(context -> {
    //            assertThat(context)
    //                    .hasFailed()
    //                    .getFailure()
    //                    .hasMessageContaining(
    //                            "Authorization service target URL not provided through config property
    // geoserver.acl.client.basePath");
    //        });
    //    }
    //
    //    @Test
    //    void testConfiguredThroughConfigProperties() {
    //        runner.withPropertyValues(
    //                        "geoserver.acl.client.basePath=http://localhost:8181/acl/api",
    //                        "geoserver.acl.client.username=testme",
    //                        "geoserver.acl.client.password=s3cr3t",
    //                        "geoserver.acl.client.startupCheck=false")
    //                .run(context -> {
    //                    assertThat(context).hasNotFailed();
    //                    assertThat(context).hasSingleBean(RuleAdminService.class);
    //                    assertThat(context).hasSingleBean(AdminRuleAdminService.class);
    //                    assertThat(context).hasSingleBean(AuthorizationService.class);
    //                    assertThat(context).hasSingleBean(AuthorizationServiceClientAdaptor.class);
    //                });
    //    }
    //
    //    @Test
    //    void testStartupCheck() {
    //        runner.withPropertyValues(
    //                        "geoserver.acl.client.basePath=http://localhost:8181/acl/api",
    //                        "geoserver.acl.client.username=testme",
    //                        "geoserver.acl.client.password=s3cr3t",
    //                        "geoserver.acl.client.startupCheck=true",
    //                        "geoserver.acl.client.initTimeout=2")
    //                .run(context -> {
    //                    assertThat(context)
    //                            .hasFailed()
    //                            .getFailure()
    //                            .hasMessageContaining("Unable to connect to ACL after 2 seconds");
    //                });
    //    }
}
