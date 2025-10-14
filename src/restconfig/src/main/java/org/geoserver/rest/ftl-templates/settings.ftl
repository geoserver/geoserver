<#include "head.ftl">

Global Configuration:

<ul>
  <li>Global Services:  "${properties.globalServices!}"</li>
  <li>Update Sequence:  "${properties.updateSequence!}"</li>
  <li>Feature Type Cache Size: "${properties.featureTypeCacheSize!}"</li>
  <li>XML POST request log buffer in characters: "${properties.xmlPostRequestLogBufferSize!}"</li>
</ul>

Contact Information: 

<ul>
  <li>Contact:  "${properties.contactPerson!}"</li>
  <li>Organization:  "${properties.contactOrganization!}"</li>
  <li>Position:  "${properties.contactPosition!}"</li>
  <li>Address Type:  "${properties.addressType!}"</li>
  <li>Address:  "${properties.address!}"</li>
  <li>City:  "${properties.addressCity!}"</li>
  <li>State:  "${properties.addressState!}"</li>
  <li>ZIP code:  "${properties.addressPostalCode!}"</li>
  <li>Country:  "${properties.addressCountry!}"</li>
  <li>Telephone:  "${properties.contactVoice!}"</li>
  <li>Fax:  "${properties.contactFacsimile!}"</li>
  <li>Email:  "${properties.contactEmail!}"</li>
</ul>

Settings:

<ul>
  <li>Verbose Messages:  "${properties.settings.verbose!}"</li>
  <li>Verbose Exception Reporting:  "${properties.settings.verboseExceptions!}"</li>
  <li>Number of Decimals:  "${properties.settings.numDecimals!}"</li>
  <li>Character Set:  "${properties.settings.charset!}"</li>
  <li>Proxy Base URL:  "${properties.settings.proxyBaseUrl!}"</li>
  <li>Online Resource:  "${properties.settings.onlineResource!}"</li>
</ul>

Image Processing Configuration:

<ul>
  <li>Interpolation:  "${properties.imageProcessing.allowInterpolation}"</li>
  <li>Recycling:  "${properties.imageProcessing.recycling}"</li>
  <li>Tile Priority:  "${properties.imageProcessing.tilePriority}"</li>
  <li>Tile Threads:  "${properties.imageProcessing.tileThreads}"</li>
  <li>Memory Capacity:  "${properties.imageProcessing.memoryCapacity}"</li>
  <li>Memory Threshold:  "${properties.imageProcessing.memoryThreshold}"</li>
  <li>PNG Encoder:  "${properties.imageProcessing.pngEncoderType}"</li>
</ul>

Coverage Access Settings:

<ul>
  <li>Core Pool Size:  "${properties.coverageAccess.corePoolSize}"</li>
  <li>Maximum Pool Size:  "${properties.coverageAccess.maxPoolSize}"</li>
  <li>Keep Alive Time:  "${properties.coverageAccess.keepAliveTime}"</li>
  <li>Queue Type:  "${properties.coverageAccess.queueType}"</li>
  <li>ImageIO Cache Memory Threshold(KB):  "${properties.coverageAccess.imageIOCacheThreshold}"</li>
</ul>

<#include "tail.ftl">