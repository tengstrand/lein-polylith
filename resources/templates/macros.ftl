
<#macro link e type>
onclick="window.location='#${e}-${type}';"
</#macro>

<#macro hasLibrary libs lib>
  <#local result = "">
  <#list libs as library>
    <#if lib.name = library.name && lib.version = library.version>
      <#local result = "&#10003;">
    </#if>
  </#list>
${result}
</#macro>

<#macro hasEntity entities entity>
  <#local result = "">
  <#list entities as ent>
    <#if entity.name = ent.name>
      <#local result = "&#10003;">
    </#if>
  </#list>
${result}
</#macro>

<#function dashify string>
  <#return string?replace("-", "&#8209;")>
</#function>

<#macro libRows entities type>
  <#list entities as entity>
    <tr>
      <td class="${type}-row-header">${dashify(entity.name)}</td>
    <#list libraries as lb>
      <td class="center ${type}-row"><@hasLibrary libs=entity.libraries lib=lb/></td>
    </#list>
    </tr>
  </#list>
</#macro>

<#macro entityRows entities type>
<#list entities as e>
  <tr>
    <td class="${type}-row-header">${e.name}</td>
  <#list environments as env>
    <td class="center ${type}-row"><@hasEntity entities=env.entities entity=e/></td>
  </#list>
  <#list systems as sys>
    <td class="center ${type}-row"><@hasEntity entities=sys.entities entity=e/></td>
  </#list>
  </tr>
</#list>
</#macro>

<#macro doc dir entity size=20>
  <#if githomeurl = "">
    <h3>${entity.name}</h3>
  <#else>
    <div>
      <div style="font-size: ${size}px; font-weight: bold; margin-right: 10px; float: left;">${entity.name}</div>
      <#assign path><#if dir != "">/${dir}/${entity.name}</#if></#assign>
      <a target="_blank" rel="noopener noreferrer" href="${githomeurl}${path}" style="font-size: ${size - 6}px;">(src)</a>
      <p class="tiny-clear"/>
    </div>
  </#if>
  <div style="margin-left: 10px;">${entity.description}<br></div>
  <p class="tiny-clear"/>
</#macro>

<#macro listLibraries libs>
<#list libs as lib>
<div class="library" title="${lib.version}">${lib.name}</div>
</#list>
<p class="clear"/>
</#macro>

<#macro compdiv c i t>
  <div class="component">
    <div class="component-impl" title="${t}" <@link e=c type="component"/>>${c}</div>
    <div class="component-ifc" title="${t}" <@link e=c type="interface"/>>${i}</div>
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
        <#assign entityName><#if col.samename>&nbsp;<#else>${col.entity}</#if></#assign>
      <td class="${class}"${colspan} <@link e=col.entity type=col.type/>>${entityName}</td>
      </#if>
    </#list>
    </tr>
  </#list>
  </table>
</#macro>
