
<#include "macros.ftl">

<!DOCTYPE html>
<html>
<head>
<title>${workspace.name} (workspace)</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<script>
function viewSmallTree(system) {
    document.getElementById(system + "-medium").style.display = "none";
    document.getElementById(system + "-large").style.display = "none";
    document.getElementById(system + "-small").style.display = "block";
    document.getElementById(system + "-small-ref").style.fontWeight = "bold";
    document.getElementById(system + "-medium-ref").style.fontWeight = "normal";
    document.getElementById(system + "-large-ref").style.fontWeight = "normal";
}

function viewMediumTree(system) {
    document.getElementById(system + "-small").style.display = "none";
    document.getElementById(system + "-large").style.display = "none";
    document.getElementById(system + "-medium").style.display = "block";
    document.getElementById(system + "-small-ref").style.fontWeight = "normal";
    document.getElementById(system + "-medium-ref").style.fontWeight = "bold";
    document.getElementById(system + "-large-ref").style.fontWeight = "normal";
}

function viewLargeTree(system) {
    document.getElementById(system + "-small").style.display = "none";
    document.getElementById(system + "-medium").style.display = "none";
    document.getElementById(system + "-large").style.display = "block";
    document.getElementById(system + "-small-ref").style.fontWeight = "normal";
    document.getElementById(system + "-medium-ref").style.fontWeight = "normal";
    document.getElementById(system + "-large-ref").style.fontWeight = "bold";
}
</script>

<img src="../logo.png" alt="Polylith" style="width:200px;">

<p class="clear"/>
<@doc dir = "" entity = workspace size=32/>

<h1>Libraries</h1>
<table class="entity-table">
  <tr>
    <td></td>
<#list libraries as lib>
    <td class="library-cell"><span class="library-text">${dashify(lib.name)}&nbsp;&nbsp;${lib.version}&nbsp;</div></td>
</#list>
  </tr>
<@libRows entities=components type="component"/>
<@libRows entities=bases type="base"/>
<@libRows entities=environments type="environment"/>
<@libRows entities=systems type="system"/>
</table>

<h1>Components and bases</h1>

<table class="entity-table">
  <tr>
    <td></td>
<#list environments as env>
    <td><div class="environment">${env.name}</div></td>
</#list>
<#list systems as sys>
    <td><div class="system">${sys.name}</div></td>
</#list>
  </tr>
<@entityRows entities=components type="component"/>
<@entityRows entities=bases type="base"/>
</table>

<h3>Interfaces</h3>
<#list interfaces as interface>
<div class="interface" <@link e=interface type="interface"/>>${interface}</div>
</#list>
<p class="clear"/>

<h3>Components</h3>
<#list components as comp>
  <@component c=comp/>
</#list>
<p class="clear"/>

<h3>Bases</h3>
<#list bases as base>
<div class="base" <@link e=base.name type="base"/>>${base.name}</div>
</#list>
<p class="clear"/>

<h3>Environments</h3>
<div class="environments">
<#list environments as environment>
  <@doc dir = "environments" entity = environment size=18/>
  <#list environment.entities as entity>
    <#if entity.type = "base">
    <div class="base" <@link e=entity.name type="base"/>>${entity.name}</div>
    <#else>
      <@component c=entity/>
    </#if>
  </#list>
  <p class="clear"/>
  <@listLibraries libs=environment.libraries/>
</#list>
</div>

<h3>Systems</h3>
<div class="systems">
<#list systems as system>
  <@doc dir = "systems" entity = system/>

  <p class="clear"/>
  <a nohref id="${system.name}-small-ref" style="cursor:pointer;color:blue;margin-left:10px;" onClick="viewSmallTree('${system.name}')">S</a>
  <a nohref id="${system.name}-medium-ref" style="cursor:pointer;color:blue;margin-left:5px;font-weight:bold;" onClick="viewMediumTree('${system.name}')">M</a>
  <a nohref id="${system.name}-large-ref" style="cursor:pointer;color:blue;margin-left:5px;" onClick="viewLargeTree('${system.name}')">L</a>
  <p class="tiny-clear"/>

  <#list system.unreferencedComponents as entity>
    <#if entity.name = "&nbsp;">
    <@component c=entity title="The interface '${entity.name}' is referenced from '${system.name}' but a component that implements the '${entity.name}' interface also needs to be added to ${system.name}', otherwise it will not compile."/>
    <#else>
    <@component c=entity title="The component '${entity.name}' was added to '${system.name}' but has no references to it in the source code."/>
    </#if>
  </#list>
  <#if system.entities?has_content>
  <p class="tiny-clear"/>
  </#if>
  <@table name=system.name table=system.smalltable size="small"/>
  <@table name=system.name table=system.mediumtable size="medium"/>
  <@table name=system.name table=system.largetable size="large"/>
  <@listLibraries libs=system.libraries/>
</#list>
</div>

<h2>Interfaces</h1>
<#list interfaces as interface>
  <a id="${interface}-interface"/>
  <h3>${interface}</h3>
  <div class="interface">${interface}</div>
  <p class="clear"/>
</#list>

<h2>Components</h2>
<#list components as component>
  <a id="${component.name}-component"/>
  <@doc dir = "components" entity = component/>
  <@table name=component.name table=component.table/>
  <@listLibraries libs=component.libraries/>
  <p class="tiny-clear"/>
</#list>

<h2>Bases</h2>
<#list bases as base>
  <a id="${base.name}-base"/>
  <@doc dir = "bases" entity = base/>
  <@table name=base.name table=base.table/>
  <@listLibraries libs=base.libraries/>
  <p class="tiny-clear"/>
</#list>

</body>
</html>
