]]>
</summary>
<link ${identifierLink}/>
<link ${metadataLink}/>
<link ${quicklookLink}/>
<media:group>
    <media:content ${mediaContent}>
        <media:category scheme="http://www.opengis.net/spec/EOMPOM/1.0">THUMBNAIL</media:category>
    </media:content>
</media:group>
<#list offerings as offer>
    <owc:offering ${offer.offeringCode}>
        <#list offer.offeringDetailList as detailList>
            <owc:operation ${detailList}/>
        </#list>
    </owc:offering>
</#list>
<#if downloadLink??>
    <link ${downloadLink}/>
</#if>
</entry>