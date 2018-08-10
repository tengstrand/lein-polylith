
<#include "macros.ftl">

<!DOCTYPE html>
<html>
<head>
<title>${workspace} - Components</title>

<link rel="stylesheet" type="text/css" href="style.css">

</head>
<body>

<img src="../logo.png" alt="Polylith" style="width:200px;">

<h1>Components</h1>

<#list components as component>
<h3>${component.name}</h3>
  <@table e=component/>
</#list>

</body>
</html>