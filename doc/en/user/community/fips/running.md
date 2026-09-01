# Running GeoServer under FIPS

## Start from a new data directory

A FIPS GeoServer cannot open a data directory written by a normal one, and it fails on the first
start. Two things in it use algorithms that approved-only mode refuses:

- The keystore holding the configuration keys is a JCEKS file, whose password protection is MD5 and
  DES.
- Stored passwords name the encryption that wrote them: `crypt1` for user and group
  passwords, `crypt2` for store connection passwords. Neither can be read under FIPS.

There is no migration tool yet. Point the deployment at an empty directory, let GeoServer fill it,
then configure the services, stores, layers and users again. Create that directory before you start:
GeoServer ignores a `GEOSERVER_DATA_DIR` that does not exist and builds one inside the web
application instead. You can copy everything unencrypted from the old directory, such as the layer
and service configuration, the styles and the access rules. The `security` directory stays behind.

A directory GeoServer creates under FIPS differs from a normal one in three places. All three are
visible in the **FIPS** tab of **About & Status > Server Status**:

| what | normal GeoServer | under FIPS |
| --- | --- | --- |
| keystore holding the configuration keys | JCEKS | BCFKS, BouncyCastle's own format |
| encryption of stored passwords | `crypt1` and `crypt2` | `crypt3`, AES-GCM with a key derived by PBKDF2 |
| storage of the master password | password based encryption in a file | AES-GCM in a file |

!!! note
    The keystore file is named after its format. A normal install keeps `security/geoserver.jceks`,
    a FIPS one gets `security/geoserver.bcfks`. GeoServer checks the first bytes of the file against
    the format it expects and refuses to start when they disagree, rather than creating an empty
    keystore and losing the keys your passwords were encrypted with.

The `crypt3` encryption is not FIPS specific. It uses algorithms every Java runtime offers, so a
normal GeoServer with the same keystore reads a value written under FIPS. Only the other direction
fails, and that is the gap a migration tool would close.

As on any new data directory, the administrator account is the standard one, `admin` with password
`geoserver`. Change it on first login, from **Security > Users, Groups, Roles**.

## Reading the FIPS tab

The tab has two tables. The first one reports the cryptography in force, with short values:

| Item | Value in a FIPS deployment | Meaning |
| --- | --- | --- |
| Crypto module | `READY` | The validated module passed its own self tests when GeoServer started. Any other value means the deployment is not FIPS compliant, whatever the rest of the page says. |
| Approved-only mode | `on` | A request for a non-approved algorithm fails. `off` means non-approved algorithms are allowed to run. |
| Operating system FIPS mode | `yes` | The kernel FIPS flag. `no` means the machine is not in FIPS mode, `unknown` that it does not say. The module cannot set it, see [Installing the FIPS module](installing.md). |
| Crypto provider | `BCFIPS` and its version | The validated module registered with Java. `not installed` means GeoServer is not using it. |
| Provider position | `first` | Java uses the first provider that offers an algorithm. `behind <name>` means another provider answers first and does the work outside the validated module. |
| Keystore format | `BCFKS` | Format of the keystore holding the configuration keys. Anything else means the data directory was created without FIPS. |
| Config password encoder | `AES-GCM` | How passwords in the catalog are encrypted, such as store connection parameters. |
| User password encoder | `AES-GCM` | How passwords held by user group services are encrypted. |
| Master password storage | `AES-GCM file` | How the master password is kept. |
| Random source | generator and provider | The generator serving random bytes, with the provider in round brackets. A provider other than `BCFIPS` means the bytes come from outside the validated module. |

The keystore, encoder and master password values are what this data directory was created with, and
cannot be changed on an existing one.

The second table lists the four algorithms GeoServer cannot work without, with a yes or no each: the
keystore format, `AES/GCM/NoPadding`, `PBKDF2WithHmacSHA256` and `SHA-256`. A `no` on any of them
means the security subsystem will fail somewhere, and this table says which piece is missing.

## Reading the same values over REST

The FIPS module reports itself among the module statuses, so the same values can be read without the
user interface. Ask for the HTML form of the module status list:

```
curl -u admin:geoserver "http://localhost:8080/geoserver/rest/about/status.html"
```

The entry to look for is the one whose **Module** is `gs-fips-provider`. Its **Message** holds one
line per item of the first table, then one line per required algorithm:

```
Crypto module: READY
Approved-only mode: on
Operating system FIPS mode: yes
Crypto provider: BCFIPS 2.0102
Provider position: first
Keystore format: BCFKS
Config password encoder: AES-GCM
User password encoder: AES-GCM
Master password storage: AES-GCM file
Random source: DEFAULT (BCFIPS)
KeyStore BCFKS: yes
Cipher AES/GCM/NoPadding: yes
SecretKeyFactory PBKDF2WithHmacSHA256: yes
MessageDigest SHA-256: yes
```

The same entry has two boolean fields: **Available** is true when the module self tests passed,
**Enabled** when approved-only mode is on.

!!! note
    The `json` and `xml` forms of `rest/about/status` list module names and links only, with no
    message, so none of the values above can be read there.

## Limitations

**HTTP Digest authentication cannot be used.** MD5 is in the protocol itself, both ends have to
compute the same value, and the stored password format is MD5 too. Move those configurations to
another authentication method.

**Database passwords need at least 14 characters.** PostgreSQL authenticates with SCRAM, which
derives a key from the password, and approved-only mode refuses a derivation from a password shorter
than 112 bits. A shorter one makes the store fail to connect with `password must be at least 112
bits` in the log. The same holds for any other database that derives a key from the password rather
than sending it. Command line tools such as `psql` do not use Java cryptography, so the same account
can work fine outside GeoServer.

**A library may ask for a random source that a FIPS system does not have.** Nothing in GeoServer
does, but a third party extension calling `SecureRandom.getInstanceStrong()` fails on a Red Hat
family FIPS system: both generators named as strong there belong to a provider the policy has
emptied. Only a change to the Java security configuration fixes that.

## Log messages that are not problems

- `The default SHA1PRNG algorithm for SecureRandom is not supported by this JVM. Using the platform
  default.`, from Tomcat at startup. The platform default on a FIPS machine is a FIPS generator, so
  the session identifiers are compliant.
- `SecureRandom algorithm 'DRBG' is not available`, at `FINE` level. `DRBG` is a stock Java name
  owned by a provider that a FIPS system empties, not a forbidden algorithm. The validated module's
  own generator takes over.
