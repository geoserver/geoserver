---
render_macros: true
---

# Installing the Keycloak Role Service module

## Download

1. Login to GeoServer and navigate to **About & Status → About GeoServer**. Check **Build Information** to find the exact version you are running.

2. Visit the [GeoServer download page](https://geoserver.org/download), open the **Development** tab, and locate the nightly build that matches your version.

   Follow the **Community Modules** link and download the `sec-keycloak` archive:

   - {{ snapshot }} example: [sec-keycloak](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-sec-keycloak-plugin.zip)

3. Extract the contents of the archive into the **`WEB-INF/lib`** directory of your GeoServer installation.

   !!! warning
       The version number in the filename must match the GeoServer version you are running (for example `geoserver-{{ snapshot }}-sec-keycloak-plugin.zip`).

4. Restart GeoServer.

## Build from source

To compile the module yourself, download the GeoServer source for your version and run:

``` bash
cd src/community/keycloak
mvn install -Passembly -DskipTests
```

The assembled ZIP is written to:

```
src/community/target/release/geoserver-<version>-sec-keycloak-plugin.zip
```

The ZIP contains exactly two JARs — `gs-sec-keycloak-core` and `gs-sec-keycloak-web` — both of which must be placed in `WEB-INF/lib`. All other required libraries (`httpclient5`, `jackson-databind`) are already bundled in the standard GeoServer WAR.

## Using with the GeoServer Docker container

Pass `sec-keycloak-plugin` as a community extension:

```bash
docker run -it -p 8080:8080 \
  --env INSTALL_EXTENSIONS=true \
  --env COMMUNITY_EXTENSIONS="sec-keycloak-plugin" \
  docker.osgeo.org/geoserver:{{ snapshot }}
```
