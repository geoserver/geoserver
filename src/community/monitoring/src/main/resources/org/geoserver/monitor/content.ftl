<#escape x as x?xml>
<Request id="${id!""}">
   <Service>${service!""}</Service> 
   <Version>${owsVersion!""}</Version>
   <Operation>${operation!""}</Operation> 
   <SubOperation>${subOperation!""}</SubOperation>
   <Resources>${resourcesList!""}</Resources>
   <Path>${path!""}</Path>
   <QueryString>${queryString!""}</QueryString>
   <#if bodyAsString??>
   <Body>
   ${bodyAsString}
   </Body>
   </#if>
   <HttpMethod>${httpMethod!""}</HttpMethod>
   <StartTime>${startTime?datetime?iso_utc_ms}</StartTime> 
   <EndTime>${endTime?datetime?iso_utc_ms}</EndTime>
   <TotalTime>${totalTime}</TotalTime> 
   <RemoteAddr>${remoteAddr!""}</RemoteAddr>
   <RemoteHost>${remoteHost!""}</RemoteHost>
   <Host>${host}</Host> 
   <RemoteUser>${remoteUser!""}</RemoteUser>
   <ResponseStatus>${responseStatus!""}</ResponseStatus>
   <ResponseLength>${responseLength?c}</ResponseLength>
   <ResponseContentType>${responseContentType!""}</ResponseContentType>
   <#if bbox?? && bbox.minX()<=bbox.maxX()>
   <BoundingBox><Min x="${bbox.minX()}" y="${bbox.minY()}"><Max x="${bbox.maxX()}" y="${bbox.maxY()}"></BoundingBox>
   </#if>
   <#if error??>
   <Failed>true</Failed>
   <ErrorMessage>${errorMessage!""}</ErrorMessage>
   <#else>
   <Failed>false</Failed>
   </#if>
</Request>
</#escape>