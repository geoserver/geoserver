]]>
</summary>
<link ${identifierLink}/>
<link ${metadataLink}/>
<link ${osddLink}/>
<#list offerings as offer>
    <owc:offering ${offer.offeringCode}>
        <#list offer.offeringDetailList as detailList>
            <owc:operation ${detailList}/>
        </#list>
    </owc:offering>
</#list>
</entry>