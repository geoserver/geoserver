---
render_macros: true
---

# Installing the FIPS module

Installing this extension works like any other, with one addition: you also have to remove the
regular BouncyCastle jars that GeoServer ships. Do that in the war file, before you deploy it. A
stock GeoServer does not start on a machine that is already in FIPS mode.

## Installing

1.  Check the version you are going to install. A running GeoServer shows it in
    **About & Status > About GeoServer**, under **Build Information**.

2.  Open the [website download](https://geoserver.org/download) page, go to the **Development** tab
    and find the nightly build matching your version. Follow the **Community Modules** link and
    download the `fips` archive.

    - {{ snapshot }} example: [fips](https://build.geoserver.org/geoserver/main/community-latest/geoserver-{{ snapshot }}-fips-plugin.zip)

3.  Stop GeoServer.

4.  A war file is a zip file. Unpack it, replace the three regular BouncyCastle jars in
    `WEB-INF/lib` with the ones from the plugin archive, delete the data directory bundled in the
    war, then pack it again:

    ``` bash
    unzip -q geoserver.war -d geoserver
    rm geoserver/WEB-INF/lib/bcprov-lts8on-*.jar \
       geoserver/WEB-INF/lib/bcpkix-lts8on-*.jar \
       geoserver/WEB-INF/lib/bcutil-lts8on-*.jar
    unzip -q -o geoserver-<version>-fips-plugin.zip -d geoserver/WEB-INF/lib
    ls geoserver/WEB-INF/lib | grep -i '^bc'     # only the fips jars, nothing else
    rm -rf geoserver/data                        # a 2.x data directory, unusable under FIPS
    (cd geoserver && zip -q -r ../geoserver-fips.war .)
    ```

5.  Deploy `geoserver-fips.war` under the name the old one had.

6.  Point GeoServer at a **new** data directory, see [Running GeoServer under FIPS](running.md), and
    start it again.

7.  Log in and open **About & Status > Server Status**, tab **FIPS**. The tab exists only when the
    module is installed. Read it before configuring anything else, see
    [Reading the FIPS tab](running.md#reading-the-fips-tab).

## The operating system and the Java runtime

The module only changes GeoServer. It does not put the machine in FIPS mode: that is a boot time
setting of the kernel, and it covers TLS, OpenSSL and everything outside the Java runtime. Turn it on
with the tools of your distribution, for example on Red Hat family systems:

``` bash
sudo fips-mode-setup --enable
sudo reboot
cat /proc/sys/crypto/fips_enabled     # 1 when it is on
```

Use the Java runtime your distribution ships, not one you download yourself. On a Red Hat family
system only the packaged runtime reads the system cryptographic policy and applies it to Java.

## Approved-only mode

The module turns on approved-only mode. A request for a non-approved algorithm then fails instead
of running quietly. Leave it off and the deployment is not FIPS compliant, because the validated
module still serves MD5 and DES.

A system property turns it off, and the FIPS tab then reports that:

```
-Dorg.bouncycastle.fips.approved_only=false
```

Only do that to diagnose a startup failure. A deployment bound by a FIPS policy runs with it on.

