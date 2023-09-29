# Security Policy

## Supported Versions

Each GeoServer release is supported with bug fixes for a year, with releases made approximately every two months.

| Version     | Supported          | Available               |
| ----------- | ------------------ |------------------------ |
| stable      | :white_check_mark: | six months              |
| maintenance | :white_check_mark: | twelve months           |
| archived    | :x:                |                         |

This approach provides ample time for upgrading ensuring you are always working with a supported GeoServer release.

If your organisation is making use of a GeoServer version that is no longer in use by the community all is not lost.
You can volunteer on the developer list to make additional releases, or engage with one of our [Commercial Support](http://geoserver.org/support/) providers.

## Reporting a Vulnerability

If you encounter a security vulnerability in GeoServer please take care to report in a responsible fashion:

* Keep exploit details out of mailing list and issue tracker
* Send details to geoserver-security@lists.osgeo.org which is monitored by volunteers
* GitHub [security](https://github.com/geoserver/geoserver/security) page for *Private vulnerability reporting*
* Be prepared to work with Project Steering Committee (PSC) members on a solution
* Keep in mind PSC members are volunteers and an extensive fix may require fundraising / resources

Please send a mail directly to geoserver-security@lists.osgeo.org (moderated list with no possibility to subscribe, please just send directly to the address, the mail will be evaluated and eventually posted) and provide information about the security issue you might have found there.

For more information see [Community Support](http://geoserver.org/comm/).

## Coordinated vulnerability disclosure

Disclosure policy:

1. Vulnerability has been verified by working with the geoserver-security list
2. GitHub [security advisory](https://github.com/geoserver/geoserver/security) used to assign a CVE number
3. Fix or documentation clarification is avialable and has been backported to both the stable and maintenance branches
4. Fix is available for download (either by waiting for for natrual release of the stable and matinenace branches, or by issuing an emergency update)
5. The CVE vulnerability released with mitigation and patch instructions

This represents a balance between transparency and particpation that does not overwhelm particpants.  Those seeking greater visibility are encouraged to volunteer with the geoserver-security list; or work with a one of the excellent commercial support providers that will do so on their behalf.
