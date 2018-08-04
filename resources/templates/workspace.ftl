<#macro component c>
  <#if c.name = c.interface>
  <div class="component">
    <div class="component-impl">${c.name}</div>
    <div class="pass-through-ifc-empty">&nbsp;</div>
  </div>
  <#else>
  <div class="component">
    <div class="component-impl">${c.name}</div>
    <div class="pass-through-ifc">${c.interface}</div>
  </div>
  </#if>
</#macro>

<!DOCTYPE html>
<html>
<head>
<title>${workspace}</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<img src="../../logo.png" alt="Polylith" style="width:200px;">

<h1>${workspace}</h1>

<h3>Libraries</h3>
<#list libraries as library>
<div class="library" title="${library.version}">${library.name}</div>
</#list>
<p class="clear"/>

<h3>Interfaces</h3>
<#list interfaces as interface>
<div class="interface">${interface}</div>
</#list>
<p class="clear"/>

<h3>Components</h3>
<#list components as comp>
  <@component c=comp/>
</#list>
<p class="clear"/>

<h3>Bases</h3>
<#list bases as base>
<div class="base">${base}</div>
</#list>
<p class="clear"/>

<h3>Environments</h3>
<div class="environments">
<#list environments as environment>
  <h4>${environment.name}:</h4>
  <#list environment.entities as entity>
    <#if entity.type = "base">
    <div class="base">${entity.name}</div>
    <#else>
      <@component c=entity/>
    </#if>
  </#list>
  <p class="clear"/>
</#list>
</div>

<h3>Systems</h3>
<div class="systems">
<#list systems as system>
  <h4>${system.name}:</h4>
  <#list system.entities as entity>
    <@component c=entity/>
  </#list>
  <p class="clear"/>
  <table class="system-table">
  <#list system.table as row>
    <tr>
    <#list row as col>
      <#if col.type = "spc">
      <td class="spc"></td>
      <#else>
        <#assign class><#if col.type = "base">tbase<#else>tcomponent</#if></#assign>
        <#assign colspan><#if col.columns != 1> colspan=${col.columns}</#if></#assign>
      <td class="${class}"${colspan}>${col.entity}</td>
      </#if>
    </#list>
    </tr>
  </#list>
  </table>
</#list>
</div>
</body>
</html>
