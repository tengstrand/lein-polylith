
<#include "macros.ftl">

<!DOCTYPE html>
<html>
<head>
<title>${workspace}</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<img src="../logo.png" alt="Polylith" style="width:200px;">

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
    <#if entity.name = "&nbsp;">
    <@component c=entity title="The interface '${entity.name}' is referenced from '${system.name}' but a component that implements the '${entity.name}' interface also needs to be added to ${system.name}', otherwise it will not compile."/>
    <#else>
    <@component c=entity title="The component '${entity.name}' was added to '${system.name}' but has no references to it in the source code."/>
    </#if>
  </#list>
  <#if system.entities?has_content>
  <p class="clear"/>
  </#if>
  <@table e=system/>
</#list>
</div>
</body>
</html>
