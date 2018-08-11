
<#include "macros.ftl">

<!DOCTYPE html>
<html>
<head>
<title>${workspace} (entities)</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<img src="../logo.png" alt="Polylith" style="width:200px;">

<h1>Interfaces</h1>
<#list interfaces as interface>
  <a id="${interface}-interface"/>
  <h3>${interface}</h3>
  <div class="interface">${interface}</div>
  <p class="clear"/>
</#list>

<h1>Components</h1>
<#list components as component>
  <a id="${component.name}-component"/>
  <h3>${component.name}</h3>
  <@table e=component/>
</#list>

<h1>Bases</h1>
<#list bases as base>
  <a id="${base.name}-base"/>
  <h3>${base.name}</h3>
  <@table e=base/>
</#list>

</body>
</html>
