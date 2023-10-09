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
You can volunteer on the developer list to make additional releases, or engage with one of our
[Commercial Support](http://geoserver.org/support/) providers.

## Reporting a Vulnerability

If you encounter a security vulnerability in GeoServer please take care to report in a responsible fashion:

* Keep exploit details out of public mailing lists and issue tracker
* Send details to volunteers on private geoserver-security@lists.osgeo.org mailing list; or via
  GitHub [security](https://github.com/geoserver/geoserver/security) page using *Private vulnerability reporting*
* Be prepared to work with geoserver-security email list volunteers on a solution
* Keep in mind participants are volunteering their time, an extensive fix may require fundraising/resources

Please send a mail directly to geoserver-security@lists.osgeo.org  and provide information
about the security vulnerability you might have found there. This is a moderated list:
send directly to the address; your email will be moderated; and eventually shared with volunteers.
The volunteer nature of geoserver-security list means that there is no expected response time. 

For more information see [Community Support](http://geoserver.org/comm/).

## Coordinated vulnerability disclosure

Disclosure policy:

1. The reported vulnerability has been verified by working with the geoserver-security list
2. GitHub [security advisory](https://github.com/geoserver/geoserver/security) is used to reserve a CVE number
3. A fix or documentation clarification is accepted and backported to both the "stable" and "maintenance" branches
4. A fix is included for the "stable" and "maintenance" downloads (released as scheduled, or issued via emergency update)
6. The CVE vulnerability is published with mitigation and patch instructions

This represents a balance between transparency and particpation that does not overwhelm particpants. 
Those seeking greater visibility are encouraged to volunteer with the geoserver-security list;
or work with one of the commercial support providers who participate on behalf of their customers.
