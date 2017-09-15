.. _s3_geotiff:

S3 Support for GeoTiff
====================================================================================================

Support for GeoTiffs hosted on Amazon S3, via a custom GeoTools GridFormat.


What's in the Box?
----------------------------------------------------------------------------------------------------

- `org.geotools.s3.geotiff`: S3 GeoTiff Format/FormatFactory/GridCoverage2dReader implementations
  based off of the GeoTiff versions. Only very minor changes to their parent classes.
- `org.geotools.s3.cache`: Very basic caching of images from S3 based off of EhCache.
- `S3ImageInputStreamImpl`: An implementation of ImageInputStream from JAI for reading imagery
  from S3. This class mainly contains the logic of stream position and chunking, while the cache
  package handles the actual S3 reads.
  
Configuration
----------------------------------------------------------------------------------------------------

Almost all configuration is currently done via system properties. For caching configuration, please
see the class `org.geotools.s3.cache.CacheConfig`. 


Usage
----------------------------------------------------------------------------------------------------
S3GeoTiff uses s3:// style URLs to operate. The only twist is that S3GeoTiff uses query string
parameters to configure certain parameters

- `awsRegion`: Controls the region to use when connecting. Needs to be in Java enum format eg. US_WEST_2
- `useAnon`: Controls whether to authenticate anonymously. This needs to be used to connect anonymous buckets

For example:

s3://landsat-pds/L8/001/002/LC80010022016230LGN00/LC80010022016230LGN00_B1.TIF?useAnon=true&awsRegion=US_WEST_2
  
Credentials
----------------------------------------------------------------------------------------------------
Unless `S3_USE_ANON` is set to true the 
[default AWS client credential chain](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html#using-the-default-credential-provider-chain) is used.