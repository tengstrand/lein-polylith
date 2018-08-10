
<#macro component c title="">
  <#if c.type = "interface">
    <div class="interface" title="${title}">${c.name}</div>
  <#else>
    <#if c.name = c.interface>
  <div class="component">
    <div class="component-impl" title="${title}">${c.name}</div>
    <div class="component-ifc-empty">&nbsp;</div>
  </div>
    <#else>
  <div class="component">
    <div class="component-impl" title="${title}">${c.name}</div>
    <div class="component-ifc">${c.interface}</div>
  </div>
    </#if>
  </#if>
</#macro>

<#macro table e>
  <table class="system-table">
  <#list e.table as row>
    <tr>
    <#list row as col>
      <#if col.type = "spc">
      <td class="spc"></td>
      <#else>
        <#assign top><#if col.top>-top</#if></#assign>
        <#assign bottom><#if col.bottom>-bottom</#if></#assign>
        <#assign class><#if col.type = "base">tbase<#elseif col.type = "interface">tinterface${top}${bottom}<#else>tcomponent${bottom}</#if></#assign>
        <#assign colspan><#if col.columns != 1> colspan=${col.columns}</#if></#assign>
      <td class="${class}"${colspan}>${col.entity}</td>
      </#if>
    </#list>
    </tr>
  </#list>
  </table>
</#macro>
