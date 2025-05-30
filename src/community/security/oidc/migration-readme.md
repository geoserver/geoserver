# GeoServer OpenID Connect Rewrite

**Status:** January 2025

## Current Status

- The implementation is ready for review and public testing.
- Existing unit tests have been ported and supplemented, except for functionality that has been dropped (see details below). Test coverage (excluding "Resource Server" UCS, see below)
  - 82% of instruction for gs-sec-oidc-core
  - 93% of instruction for gs-sec-oidc-web

## Features

- Working with Google, GitHub, Microsoft Azure, and one custom OIDC provider.
- The "Resource Server" functionality (i.e., "OpenID Connect With Attached Access Bearer Tokens") was originally available. However, it was decided not to support this feature for the time being, as a separate extension (~ `gs-sec-jwt`) already provides similar functionality.
  - The current extension still contains code for the "Resource Server" use case, but it is in an initial status.
  - A first test of the feature was successful, but the functionality is currently disabled (commented out in `application.xml`).
  - Consider removing this code if it remains unused (though this might be unfortunate).

## Design & Goals

- Leverage Spring’s public API to configure Spring filters and associated classes for a future-proof solution.
  - Filters are created by Spring and integrated into GeoServer, minimizing custom setup code.
- While Spring is not inherently designed for the dynamic behavior required by GeoServer, the approach still appears reasonable.
- Avoid "fishing" for request parameters and headers.

### References

- For further information, refer to JavaDocs:
  - `org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationProvider`
  - `org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter`
- See the class diagram in `gs-sec-oidc/doc/diagrams` for orientation.

## Installation

- The project includes an assembly module. Place the JAR files in `WEB-INF/lib`.

## Configuration

1. Create a filter named **"openid-connect"** using the Admin Web UI.
2. Add the **"openid-connect"** filter to the `web` filter chain.
3. Create a new filter chain named **"oauth-callback"** with the following configuration:
   - **Ant Patterns:** `/oauth2/authorization/**,/login/oauth2/code/**` (Attention: no space between)
   - Assign the **"openid-connect"** filter to this chain.
   - Use default settings for everything else.
   - Ensure the chain is positioned before `webLogin`.
4. If using a user group service as the role source, create users with roles that correspond to the identity provider's users.
5. Set up the identity provider (Google, GitHub, Microsoft, or custom OIDC provider) as usual.

## Open Tasks

### High Relevance

- Update the user guide.
  - generally
  - specifics:
    - special cases for `tokenRolesClaim` (`scope` for source `access token` and `authorities` for source `userInfo`, see `GeoServerOAuth2RoleResolver`)
- Validation: Previously `checkTokenEndpointUrl` or `jwkURI` was required, see prior OpenIdConnectFilterConfigValidator. Situation is a little confusing.
  - What I understand:
    - `checkTokenEndpointUrl` refers to `userInfoUri` (according to Spring naming). API purpose: Exchange an access token into userInfo data.
    - `jwkURI` refers to `jwkSetUri` (according to Spring naming). API purpose: Load IDP public keys for signature validation.
    - I suppose the prior code is using one of those methods to validate the access token.
  - The current Spring approach is different:
    - Spring uses the `jwkSetUri` for validation by default, which is determined by the selected JwsAlgorithm, which defaults to RS256. This algorithm belongs to the family of signature algorithms using asymetric encryption and public/private keys. Therefor the public keys are loaded from the `jwkSetUri`.
      - If an algorithm of the MAC family is configured instead (regarding configuration: see below) the signature is validated using a pre-shared secret instead (here: the client secret).
    - so in neither case the `userInfoUri` is used for token validation. Even the `jwkSetUri` is not necessarily used - it depends on the algorithm.
    - however the `userInfoUri` is used by Spring in the OAuth2 case (not OIDC) to load userInfo to enrich the `OAuth2User` with attributes and authorities. GeoServer uses it if `UserInfo` is the selected role source.
    - I think this is only affecting the validations which currently do not reflect this. Also, this should be explained in the user guide. I suppose:
      - the config UI should be extended to select an algorithm, RS256 by default (see below)
      - if the algorithm belongs to the signature family the `jwkSetUri` is mandatory, otherwise it is optional
      - if `UserInfo` is selected as role source the `userInfoUri` is mandatory, otherwise it is optional
- Implement GEOS-11635: _"Add support for opaque auth tokens in OpenID Connect"_.
  - I suppose this works out of the box for login. Introspection endpoint is not used by Spring in case of OAuth2 login, but some support is contained in the resource-server Spring lib. Maybe the GEOS-11635 adressed the Resource Server use case, which is not supported here?

### Medium Relevance

- Verify compatibility with Keycloak and GeoNode
  - if they were using GS as "Resource Server" (i.e., "OpenID Connect With Attached Access Bearer Tokens") this is not supported anymore, as mentioned above. Consider using the respective extension instead (~ `gs-sec-jwt`).
- Create integration tests:
  - Consider using [Spring Authorization Server](https://spring.io/projects/spring-authorization-server) as an OIDC provider. While simple to use, it requires newer Spring versions, making the setup currently more complex.
  - An example setup is available in the [gs-sec-oidc-integration-tests repository](https://github.com/awaterme/gs-sec-oidc-integration-tests). This setup is also useful as a lightweight development OIDC provider.

### Lower Relevance

- Improve unit test coverage
- Some validators have been removed:
  - `AudienceAccessTokenValidator`: Likely replaced by `OidcIdTokenValidator`.
  - `SubjectTokenValidator`: Previously used only for the Resource Server use case?
- Consider implementing Andrea’s suggestion for a dropdown in the UI to "add provider xy." However, in the current implementation deactivated providers are hidden, reducing the relevance of this feature.

## Compatibility with Prior Implementation

- **Role Sources:**
  - Previous implementations may have supported nested JSON paths. It’s unclear if this was intentional or necessary.
- **Token Validation:**
  - Previously, invalid signatures may have been accepted if "enforce token validation" was set to `false`.
  - Now, invalid signatures are always rejected (a reasonable change).
  - "Enforce token validation" now only tolerates invalid claims.
- Other potential differences in behavior may exist.

## Next Steps

1. Conduct a thorough review.
   - Note: As mentioned earlier, I am not a trained security specialist.

## Future Ideas

- Introduce additional OIDC protocol configurations, such as:
  - Allowing users to specify the `JwsAlgorithm` (this has been newly introduced and is now part of the configuration but currently without a UI counterpart).
  - I wonder if using the wrong algorithm in the past led to the requirement to make "force token validation" optional.
- Improve parsing of the `.wellknown-operations` endpoint:
  - The `JwsAlgorithm` and other details might be automatically detectable.
- Provide an "Apply Preset" UI action for certain identity providers (e.g., ADFS), which pre-fills settings for those providers.
- Document available claims and their semantics for each identity provider as thoroughly as possible.
