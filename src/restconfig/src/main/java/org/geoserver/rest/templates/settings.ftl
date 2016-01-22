<#include "head.ftl">

Global Configuration:

<ul>
  <li>Global Services:  "${properties.globalServices}"</li>
  <li>Update Sequence:  "${properties.updateSequence}"</li>
  <li>Feature Type Cache Size: "${properties.featureTypeCacheSize}"</li>
  <li>XML POST request log buffer in characters: "${properties.xmlPostRequestLogBufferSize}"</li>
</ul>

Contact Information: 

<ul>
  <li>Contact:  "${properties.contactPerson}"</li>
  <li>Organization:  "${properties.contactOrganization}"</li>
  <li>Position:  "${properties.contactPosition}"</li>
  <li>Address Type:  "${properties.addressType}"</li>
  <li>Address:  "${properties.address}"</li>
  <li>City:  "${properties.addressCity}"</li>
  <li>State:  "${properties.addressState}"</li>
  <li>ZIP code:  "${properties.addressPostalCode}"</li>
  <li>Country:  "${properties.addressCountry}"</li>
  <li>Telephone:  "${properties.contactVoice}"</li>
  <li>Fax:  "${properties.contactFacsimile}"</li>
  <li>Email:  "${properties.contactEmail}"</li>
</ul>

Settings:

<ul>
  <li>Verbose Messages:  "${properties.verbose}"</li>
  <li>Verbose Exception Reporting:  "${properties.verboseExceptions}"</li>
  <li>Number of Decimals:  "${properties.numDecimals}"</li>
  <li>Character Set:  "${properties.charset}"</li>
  <li>Proxy Base URL:  "${properties.proxyBaseUrl}"</li>
  <li>Online Resource:  "${properties.onlineResource}"</li>
</ul>

JAI Configuration:

<ul>
  <li>Interpolation:  "${properties.allowInterpolation}"</li>
  <li>Recycling:  "${properties.recycling}"</li>
  <li>Tile Priority:  "${properties.tilePriority}"</li>
  <li>Tile Threads:  "${properties.tileThreads}"</li>
  <li>Memory Capacity:  "${properties.memoryCapacity}"</li>
  <li>Memory Threshold:  "${properties.memoryThreshold}"</li>
  <li>ImageIO Cache:  "${properties.imageIOCache}"</li>
  <li>PNG Encoder:  "${properties.pngEncoderType}"</li>
  <li>PNG Acceleration:  "${properties.pngAcceleration}"</li>
  <li>JPEG Acceleration:  "${properties.jpegAcceleration}"</li>
  <li>Allow Native Mosaic:  "${properties.allowNativeMosaic}"</li>
</ul>

Coverage Access Settings:

<ul>
  <li>Core Pool Size:  "${properties.corePoolSize}"</li>
  <li>Maximum Pool Size:  "${properties.maxPoolSize}"</li>
  <li>Keep Alive Time:  "${properties.keepAliveTime}"</li>
  <li>Queue Type:  "${properties.queueType}"</li>
  <li>ImageIO Cache Memory Threshold(KB):  "${properties.imageIOCacheThreshold}"</li>
</ul>

<#include "tail.ftl">