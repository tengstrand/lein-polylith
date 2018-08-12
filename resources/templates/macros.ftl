
<#macro link e type>
onclick="window.location='entities.html#${e}-${type}';"
</#macro>

<#macro compdiv c i t>
  <div class="component">
    <#assign ifc><#if c=i>i<#else>c</#if></#assign>
    <div class="component-impl" title="${t}" <@link e=c type="component"/>>${c}</div>
    <div class="component-ifc" title="${t}" <@link e=ifc type="interface"/>>${i}</div>
  </div>
</#macro>

<#macro component c title="">
  <#if c.type = "interface">
    <div class="interface" title="${title}">${c.name}</div>
  <#else>
    <#if c.name = c.interface>
    <@compdiv c=c.name i="&nbsp;" t=title/>
    <#else>
    <@compdiv c=c.name i=c.interface t=title/>
    </#if>
  </#if>
</#macro>

<#macro table name table size="medium">
  <#assign hide><#if size != "medium"> style="display:none"</#if></#assign>
  <table id="${name}-${size}" class="system-table"${hide}>
  <#list table as row>
    <tr>
    <#list row as col>
      <#if col.type = "spc">
      <td class="spc"></td>
      <#else>
        <#assign top><#if col.top>-top</#if></#assign>
        <#assign topclass><#if col.top> top</#if></#assign>
        <#assign bottom><#if col.bottom>-bottom</#if></#assign>
        <#assign class><#if col.type = "base">tbase<#elseif col.type = "interface">tinterface${top}${bottom}<#else>tcomponent${bottom}${topclass}</#if></#assign>
        <#assign colspan><#if col.columns != 1> colspan=${col.columns}</#if></#assign>
      <td class="${class}"${colspan} <@link e=col.entity type=col.type/>>${col.entity}</td>
      </#if>
    </#list>
    </tr>
  </#list>
  </table>
</#macro>
