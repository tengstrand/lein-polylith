# <img src="images/logo.png" width="50%" alt="Polylith" id="logo">
A leiningen plugin used to develop Polylith based architectures.

[![CircleCI](https://circleci.com/gh/tengstrand/lein-polylith/tree/master.svg?style=shield)](https://circleci.com/gh/tengstrand/lein-polylith/tree/master)

Welcome to the wonderful world of Polylith!

Polylith is a new way of thinking around system architecture, that puts the developer in the driving seat and the code in the center.

Polylith is a way of organising code into reusable building blocks that are used to create systems. To better understand the principles and ideas behind it, we recommend you first read the Polylith [documentation](https://polylith.gitbook.io).

Organising code as a Polylith can be done manually, which was actually how it all began. With that said, there is no magic behind this way of organising the code. It's not a framework nor a library, just a simple yet powerful way to work with code at the system level.

The reason we built this [Leiningen](https://leiningen.org) plugin was to make life easier for you as a developer by making the work more efficient and fun.

Enjoy the ride!

## Table of Contents

- [Installation](#installation)
- [Content](#content)
- [RealWorld Example](#realworld-example)
- [Workspace](#workspace)
- [System](#system)
- [Base](#base)
- [Component](#component)
- [Component Interface](#component-interface)
- [Workspace Interface](#workspace-interface)
- [Development](#development)
- [Dependencies](#dependencies)
- [Libraries](#libraries)
- [Context](#context)
- [Testing](#testing)
- [Design](#design)
- [Versioning and branching](#versioning-and-branching)
- [Mix languages](#mix-languages)
- [In practice](#in-practice)
- [Continuous Integration](#continuous-integration)
- [Commands](#commands)
- [What's next?](#whats-next)
- [Thanks](#thanks)
- [Contact](#contact)
- [License](#license)

## Installation

If you haven't done it already, start by installing [Leiningen](https://leiningen.org).

The next thing to do is to add the Polylith plugin to `~/.lein/profiles.clj`. After editing the file it should look something like this:
```clojure
{:user {:plugins [[polylith/lein-polylith "LATEST"]]}}
```

This ensures that the Polylith plugin can be called from anywhere in the file system and not just from the *workspace root* where the *project.clj* file with the Polylith declaration resides:
```clojure
...
:plugins [[polylith/lein-polylith "0.2.2"]]
```

If called from the workspace root then it will use *0.2.2* in this case, otherwise it will use the latest version of the plugin.

### Latest version
[![Clojars Project](https://clojars.org/polylith/lein-polylith/latest-version.svg)](http://clojars.org/polylith/lein-polylith)

## Content
This documentation aims to be a practical guide to this plugin with lots of code examples. We encourage you to follow the code examples and try it out yourself. We will guide you through the steps of creating a workspace with systems composed of bases, components and libraries.

You will get an understanding of how the Polylith workspace is structured and how systems and environments are your views to components and bases by using [symbolic links](https://en.wikipedia.org/wiki/Symbolic_link).

You will also learn how to test your building blocks frequently by using the [test](#test) and [build](#build) commands and how to keep libraries, systems and interfaces syncronized with the [sync](#sync) command.

We will walk you through how dependencies are detected and how the plugin keeps track of the time for the last successful test or build. We will explain how this information is used to build, compile and test your systems incrementally to shorten the feedback loop, how the dependencies can be listed with the [deps](#deps) command and used from several commands to stop you from introducing circular dependencies in your code.

We will explain the value of components and how they bring context to your development experience, which will help you build decoupled and scalable systems from day one.

Finally we will show what's next in the pipeline and how the extra power and flexibility will take your development experience to the next level.

## Help

Go to [Commands](#commands) to read how to use the built-in help.

## Realworld Example

If you want to have a look at a full-blown system, go to the [RealWorld](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app) project where you can compare it with [implementations made in other languages](https://github.com/gothinkster/realworld).

## Workspace
The workspace is the top-level container for all your code and everything you need to create Polylith systems.

Let’s start by creating the *example* workspace with the top namespace *se.example*:
```bash
$ lein polylith create w example se.example
```

By default, the [create](#create) workspace command adds a top-level folder that’s version controlled by git.
If you don’t use git, then you can disabled this feature by adding the `-git` parameter to the command:

```bash
$ lein polylith create w example se.example -git
```

The workspace directory structure will end up like this:
```bash
example                         # root dir
  bases                         # empty dir
  components                    # empty dir
  environments
    development                 # the development environment
      ...
  interfaces
    project.clj                 # the workspace interfaces project file
    src                         # source dir
      se                        # top namespace: se.example
        example                 # empty dir (no source code added yet)
  images
    logo.png                    # used by readme.md
  project.clj                   # project file for the workspace
  readme.md                     # documentation
  systems                       # empty dir
```

When you get used to it, you will love this structure because everything lives where you expect to find it. The bases live in bases, components in components, systems in systems, development environments in environments and workspace interfaces in the interfaces’ src directory beneath the top namespace *se.example*.

If you, for example, have the top namespace *com.a.b.c* and the component *user* then all its namespaces will live under the namespace path *com.a.b.c.user*. If the *user* component has a core namespace then it will automatically get the namespace *com.a.b.c.user.core*.

Right now the plugin doesn’t support changing the name of the top namespace. The advice is therefore to think carefully when deciding the name of the top namespace.

The plugin doesn’t use configuration files, annotations, dependency injection or similar to assemble its parts. Instead it uses [symbolic links](https://en.wikipedia.org/wiki/Symbolic_link) directly in the file system. So just look in the file system or use the [info](#info) command to inspect all the parts of the workspace.

If you want to [mix different languages](#mix-languages) without the risk of introducing name conflicts,
we recommend you to include the name of the language in the top namespace, e.g.: *com.a.b.c.clojure*, *com.a.b.c.java* or *com.a.b.c.scala*.
If you always include it, it allows you to safely work with several languages (one per workspace) in the future.

The *development environment* contains these files:
```bash
      docs                      # empty dir
      interfaces                # link to interfaces/src
      project-files
        bases                   # empty dir
        components              # empty dir
        interfaces-project.clj  # link to interfaces/project.clj
        systems                 # empty dir
        workspace-project.clj   # link to project.clj
      project.clj
      resources                 # empty dir
      src                       # source dir
        se                      # top namespace: se.example
          example               # empty dir (no source code added yet)
      test                      # source test dir
        se                      # top namespace: se.example
          example               # empty dir (no source code added yet)
```

The *development* directory will be the root directory when you work with the code in your development environment. Whenever you create a new *component* or *base*, the development environment will be updated automatically, letting you concentrate on writing code!

To make sure that [git](https://git-scm.com) doesn’t remove empty directories, the plugin will put a .keep file in the empty ones. It’s important that they are not removed when you clone a repository (workspace) otherwise the plugin will not work properly.

Let’s have a look at some files in the workspace so we get an idea of what’s in there.

The [Leiningen](https://leiningen.org) *project.clj* file defines which version of Polylith plugin to use, the name of the top namespace and the Clojure version to use:
```clojure
(defproject se.example/example "1.0"
  :description "A Polylith workspace."
  :plugins [[polylith/lein-polylith "0.2.2"]]
  :polylith {:top-namespace "se.example"
             :clojure-version "1.9.0"})
```

The *interfaces/project.clj* file describes how to compile all the *workspace interfaces* and looks like this:
```clojure
(defproject se.example/interfaces "1.0"
  :description "Component interfaces"
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :aot :all)
```

The *environments/development/project.clj* file is the project file for the development environment and looks like this:
```clojure
(defproject se.example/development "1.0"
  :description "The main development environment."
  :dependencies [[org.clojure/clojure "1.9.0"]])
```

We will talk more about these files later.

Go to the newly created workspace:
```
$ cd example
```

If we now run the [info](#info) command, it will list the contents of the workspace:
```
$ lein polylith info

interfaces:
components:
bases:
systems:
environments:
  development
```

...which is empty at the moment because we haven’t added anything yet.

### The prompt
A nice way of working with the plugin is to start a [prompt](#prompt):
```
$ lein polylith prompt
```

This will enter a prompt within the *example* namespace:
```
example$>
```

The benefits are that commands will execute instantly and that you don’t need to repeat “lein polylith” before every command.
All commands can be executed from any directory within the workspace root during the prompt session.

If you haven't started a prompt, all commands except [help](#help) and [create](#create) (workspace) must be executed from the workspace root.

We can execute the [info](#info) command again by just typing *info*:
```
example$> info
interfaces:
components:
bases:
systems:
environments:
  development
```

Type *quit* or *exit* to exit:
```
example$> exit
```

Everywhere in this documentation where we say you should type something like `lein polylith command` we just mean `command` if you are inside a prompt.

Feel free to start a prompt, it will save you some time and typing!

## System

A system consists of a base at the bottom with components and libraries above:<br>
<img src="images/system.png" width="30%" alt="System">

This is well explained in the Polylith [documentation](https://polylith.gitbook.io). The idea is to compose systems with smaller building blocks instead of putting everything into one place as a monolith. To describe a system we need to start with the base.

## Base

The base is the foundation of a system:<br>
<img src="images/base.png" width="30%" alt="Base">

A system can only have one base. The base exposes a public [API](https://en.wikipedia.org/wiki/Application_programming_interface) to the outer world, illustrated as sockets in the metaphor. In the *cmd-line* base that we will soon create, the public API will consist of a single `main` function that prints out “Hello world!”:
```clojure
(defn -main [& args]
  (println "Hello world!"))
```

Let’s [create](#create) the *cmd-line* system:
```
$ lein polylith create s cmd-line cmd-line
```

This will create a system with the name *cmd-line* that has a base with the name *cmd-line*. The *development* environment and the *cmd-line* system will now both contain the *cmd-line* base (in blue):<br>
<img src="images/env-systems-01.png" width="50%">

Run the [info](#info) command again to see the result:
```
$ lein polylith info

interfaces:
components:
bases:
  cmd-line *
systems:
  cmd-line *
    cmd-line *   -> base
environments:
  development
    cmd-line *   -> base
```

We can see that the *cmd-line base* and the *cmd-line system* were created. The cmd-line *base* is automatically added to the cmd-line *system* and to the *development* environment. The “-> base” means that they both link to a *base*. The * symbols indicate that something has changed since *the last successful test or build* (more on that later).

The workspace now looks like this on disk:
```bash
example
  bases
    cmd-line                # new 
      project.clj           # new (cmd-line build file)
      readme.md             # new (almost empty documentation)
      resources
        cmd-line            # new (empty dir - to put cmd-line resources)
      src/se/example
        cmd-line            # new
          core.clj          # new (cmd-line core namespace)
      test/se/example
        cmd-line            # new
          core_test.clj     # new (cmd-line core-test namespace)
  environments
    development
      docs
        cmd-line-readme.md  # new (link to bases/cmd-line/readme.md)
      project-files
        bases
          cmd-line-project.clj  # new (link to bases/cmd-line/project.clj)
        systems
          cmd-line-project.clj  # new (link to systems/cmd-line/project.clj)
      resources
        cmd-line     # new (link to bases/cmd-line/resources/cmd-line)
      src/se/example
        cmd-line     # new (link to bases/cmd-line/src/se/example/cmd-line)
      test/se/example
        cmd-line     # new (link to bases/cmd-line/test/se/example/cmd-line)
  systems
    cmd-line
      build.sh
      project.clj
      readme.md
      resources
        cmd-line  # link to bases/cmd-line/resources/cmd-line
      src/se/example
        cmd-line  # link to bases/cmd-line/src/se/example/cmd-line
```

If we have a look at some of the generated files, we first find *bases/cmd-line/src/se/example/cmd_line/core.clj*:
```clojure
(ns se.example.cmd-line.core
  (:gen-class))

;; A stand alone base example. Change to the right type of base.
(defn -main [& args]
  (println "Hello world!"))
```

...with its corresponding test *bases/cmd-line/test/se/example/cmd_line/core_test.clj*:
```clojure
(ns se.example.cmd-line.core-test
  (:require [clojure.test :refer :all]
            [se.example.cmd-line.core :as core]))

;; Add tests here...
(deftest hello-world-example-test
  (let [output (with-out-str (core/-main))]
    (is (= "Hello world!\n"
           output))))
```

...then we also have *systems/cmd-line/project.clj* which is the project file for the *cmd-line* system:
```clojure
(defproject se.example/cmd-line "0.1"
  :description "A cmd-line system."
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :aot :all
  :main se.example.cmd-line.core)
```

...and *systems/cmd-line/build.sh* which is the script that performs the final step to build an artifact for the system, in this case an uberjar:
```bash
#!/usr/bin/env bash
set -e

lein uberjar
```

Let’s run the [build](#build) command:
```
$ lein polylith build
updated: bases/cmd-line/project.clj

Changed components:
Changed bases: cmd-line
Changed systems: cmd-line

Compiling workspace interfaces
Created /Users/joakimtengstrand/IdeaProjects/example/interfaces/target/interfaces-1.0.jar
Wrote /Users/joakimtengstrand/IdeaProjects/example/interfaces/pom.xml
Installed jar and pom into local repo.

Compiling bases/cmd-line

Compiling systems/cmd-line

Start execution of tests in 1 namespaces:
lein test se.example.cmd-line.core-test

lein test se.example.cmd-line.core-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

Building systems/cmd-line
Created /Users/joakimtengstrand/IdeaProjects/example/systems/cmd-line/target/cmd-line-0.1.jar
Created /Users/joakimtengstrand/IdeaProjects/example/systems/cmd-line/target/cmd-line-0.1-standalone.jar

set :last-success in .polylith/time.edn

Execution time: 31.2 seconds
```

A build performs these steps:
1. Checks for circular dependencies and quits if found.
2. Calculates the components and bases to build, based on what has changed since the last successful test or build.
3. Calls *sync* and makes sure that:
   - all library dependencies in project.clj files are in sync
   - the workspace interfaces are in sync with the component interfaces
   - all systems have the libraries and components they need
4. [AOT-compiles](https://clojure.org/reference/compilation) changed components, bases and systems to check that they compile against the workspace interfaces.
5. Runs tests for all bases and components that have been affected by the changes.
6. Executes build.sh for all changed systems to make sure they have a working build script and no missing components or libraries.
7. If the entire build is successful, then execute the success command that updates the time for the last successful test or build.

We can now execute the jar and see if it works:
```
$ java -jar systems/cmd-line/target/cmd-line-0.1-standalone.jar
Hello world!
```

Yes it did!

If we run the [info](#info) command again:
```
$ lein polylith info

interfaces:
components:
bases:
  cmd-line
systems:
  cmd-line
    cmd-line   -> base
environments:
  development
    cmd-line   -> base
```

...we can see that all the *s are gone. The reason is that no files have been changed since the *successful build* that we just did. Let’s have a look at the file where the time of the last successful test or build is stored:
```
cat .polylith/time.edn
{:last-success 1529166522000}
```

1529166522000 is the number of milliseconds since 1970. You can see a more readable version of it by executing the [settings](#settings) command:
```
$ lein polylith settings
...
bookmarks:
  2018-06-16 18:28:42 last-success (1529166522000)
```

The plugin uses the date format: yyyy-mm-dd hh:mm:ss.

## Component

Components are the main building blocks in the Polylith world which are used to compose systems:<br>
<img src="images/component.png" width="20%" alt="Component">

A component consists of an *implementation*, an *interface* and dependencies to other components and libraries.

The *component interface* will be explained in the next section.

Let’s [create](#create) the *user* component:
```
$ lein polylith create c user
```

If you leave out the third argument of [create](#create), the *component interface* will get the same name as the component, which is *user* in this case.

The *user* component was created and added to the *development* environment:<br>
<img src="images/env-systems-02.png" width="50%">

Run the [info](#info) command again to see the result:
```
$ lein polylith info

interfaces:
  user *
components:
  user *
bases:
  cmd-line
systems:
  cmd-line
    cmd-line   -> base
environments:
  development
    user *     -> component
    cmd-line   -> base
```

We can see that the *user* interface and the *user* component were created (and are marked as changed). The *user* component was also added to the *development* environment where “-> component” indicates that it links to the *user* component. 

The *user*-related parts in the workspace now look like this on disk:

```bash
example
  components
    user                   # new (user component dir)
      readme.md            # new (empty documentation)
      project.clj          # new (user build file)
      resources
        user               # new (empty dir - to put user resources)
      src/se/example
        user               # new
          core.clj         # new (user core namespace)
          interface.clj    # new (user interface namespace)
      test/se/example
        user               # new
          core_test.clj    # new (user core test namespace)
  environments
    development
      docs
        user-readme.md     # new (link to components/user/readme.md)
      project-files
        components
          user-project.clj  # new (link to components/user/project.clj)
      resources
        user                # new (link to components/user/resources/user)
      src/se/example
        user             # new (link to components/user/src/se/example/user)
      test/se/example
        user             # new (link to components/user/test/se/example/user)
  interfaces
    src/se/example
      user                 # new (public user interface dir)
        interface.clj      # new (user interface namespace)
```

The symbolic links in *environments/development* form a view to your components and bases that makes it possible to work with the Polylith code from one place.

Some example code was created in *components/user/src/se/example/user/core.clj*:
```clojure
(ns se.example.user.core)

;; add your functions here...
(defn add-two [x]
  (+ 2 x))
```

...and its test in *components/user/test/se/example/user/core_test.clj*:
```clojure
(ns se.example.user.core-test
  (:require [clojure.test :refer :all]
            [se.example.user.interface :as user]))

;; add your tests here...
(deftest test-add-two
  (is (= 42 (user/add-two 40))))
```
You are not forced to put your code in the *core* namespace. It’s just an example and can be changed to some other name and/or divided up into several namespaces.

If we go to the *development* directory, we can now execute all the tests (the *cmd-line* base test and the *user* component test):
```
$ cd environments/development
$ lein test

lein test se.example.cmd-line.core-test

lein test se.example.user.core-test

Ran 2 tests containing 2 assertions.
0 failures, 0 errors.
```

An alternative way of running the tests is:
```
$ cd ../..
$ lein polylith test
updated: components/user/project.clj

Changed components: user
Changed bases:
Changed systems:

Compiling workspace interfaces
Created /Users/joakimtengstrand/IdeaProjects/example/interfaces/target/interfaces-1.0.jar
Wrote /Users/joakimtengstrand/IdeaProjects/example/interfaces/pom.xml
Installed jar and pom into local repo.

Compiling components/user

Start execution of tests in 1 namespaces:
lein test se.example.user.core-test

lein test se.example.user.core-test

Ran 1 tests containing 1 assertions.
0 failures, 0 errors.

set :last-success in .polylith/time.edn

Execution time: 13.3 seconds
```

You may notice that the *cmd-line* base wasn’t compiled and that its test wasn’t executed. The reason is that it hasn’t changed since the last successful test or build, which is something that the plugin detects to speed up testing.

The project file *components/user/project.cj* was updated when the *sync* step was executed. When we ran the build command for the first time it updated *bases/cmd-line/project.clj*. The is because the formatting and/or spacing differs between the project file and the generated file from the [sync](#sync) command. This will only happen the first time you run [sync](#sync).

If you execute the [sync](#sync) again or any of the other commands that include the *sync* as a step ([build](#build), [compile](#compile) and [test](#test)) the project file will be left untouched:
```
$ lein polylith sync
```

## Component interface

If you have read the Polylith [documentation](https://polylith.gitbook.io/polylith/why-polylith) you may already have an idea of what a *component interface* is in the Polylith world, shown in light green here:<br>
<img src="images/component-interface.png" width="20%">

When we created the user component, the user’s *interface* was also created in *components/user/src/se/example/user/interface.clj*:
```clojure
(ns se.example.user.interface
  (:require [se.example.user.core :as core]))

;; delegate to the implementations...
(defn add-two [x]
  (core/add-two x))
```

As you can see, the function in the *interface* delegates to the actual implementation, which lives in another namespace.

What you often do is to delegate calls to other namespaces in the component to keep the interface clean and tidy. There are no restrictions here, it’s up to you how to arrange the code the way you want and you are free to put all the code in the interface namespace if that is what you think is best. Most often you will have hundreds of lines of code in a component and then it’s best to just delegate.

Interfaces are there for a reason and they solve a number of problems:
* Guarantee isolation for each component by only exposing the interface.
* Make components interchangeable within a system so that you can replace them with other components that conform to the same interface.
* Encourage reuse by sharing the workspace interface for all components within the workspace.
* Enable navigation between components in the development environment.

A nice side-effect of using components is that you can leave all the functions public in the Clojure code. Normally you want to protect some functions by declaring them as private but with components only the interface is exposed anyway and all other functions are hidden automatically.
This can also be handy when you stop at a breakpoint to evaluate a function. If it’s private then you need to use a special syntax to access it but if all functions are public, you don’t have that annoying problem.

## Workspace interface
When we created the user component, the workspace interface *interfaces/src/se/example/user/interface.clj* was also created:
```clojure
(ns se.example.user.interface)

;; add your function signatures here...
(defn add-two [x])
```

An important difference between a *component interface* and a *workspace interface* is that the latter only contains empty functions. 

The lack of dependencies results in a flat roof:
<p>
<img src="images/interface-top.png" width="20%">
</p>

<p>
...while the signatures reside at the bottom:
</p>
<img src="images/interface-bottom.png" width="20%">

The *workspace interfaces* are used to guarantee that all the components and bases only depend on functions (or variables and macros) in other component's interface. Each base and component have their own project build file with dependencies to their libraries + the empty *workspace interfaces*. If you try to access the implementation of a component from another component or base, it will result in compilation errors when executing the [compile](#compile), [test](#test) or [build](#build) commands.

An *interface* is a specification and a contract, similar to interfaces in the [object oriented](https://en.wikipedia.org/wiki/Object-oriented_programming) world, and defines how a base or component can connect to them.

The functions in the *workspace interface* should be empty but the *component interface* serves as a bridge between the *interface signature* and its own implementation:
```clojure
...
(defn add-two [x]
  (core/add-two x))  ; delegates to the implementation
```

Note that the signature of the *component interface* and its corresponding *workspace interface* must match exactly, otherwise you will get compilation errors when compiling the code.

You are free to arrange your interface namespaces in any way, even to use other names than *interface*.
Instead of one namespace you may want to split them up like: *mycomponent/interface*, *mycomponent/x/interface* and *mycomponent/y/interface*.
The recommendation is to have one namespace with the name *interface*, but if you have good reasons not to, there are no restrictions.

### Compose a system

Now we have a *cmd-line* base and a *user* component:
```
$ lein polylith info

interfaces:
  user
components:
  user
bases:
  cmd-line
systems:
  cmd-line
    cmd-line   -> base
environments:
  development
    user       -> component
    cmd-line   -> base
```

## Development

So far we have used the console when interacting with the system and the code but very often you want more than that, like an [IDE](https://en.wikipedia.org/wiki/Integrated_development_environment) or an advanced editor.

The *development* project is just an ordinary Leiningen project and can be edited from any development environment of your choice. Here we will use [IntelliJ IDEA](https://www.jetbrains.com/idea/) with the [Cursive plugin](https://cursive-ide.com/) as an example.

Open *example/environments/development* from your preferable text editor or IDE or follow the instructions here if you use Cursive.

How to create a project in Cursive:
1. Select *File > New > Project from Existing Sources*
2. Open the directory example/environments/development
3. Select *Import project from external model* + Leiningen
4. Keep the suggested root directory
5. Keep suggested projects to import: *se.example/development*:1.0
6. Select project SDK
7. Select project name and location:
   - Project name: example
   - Project file location (e.g.): *~/examples/example/environments/development*
8. Select *File > Project Structure… > Modules > Development*. Select the *interfaces* directory and press *Test* to make it a test directory.

The reason we marked the *interfaces* directory as a test directory is that we want the IDE to treat its content as source code so that we can get the colouring when we edit it. We don’t want to put it in the *src* folder because then it can shadow the interfaces of the components (that have the same namespaces and signatures).

Now we have the *example* project created and we are ready to work with it from the IDE:<br>
<img src="images/project-01.png" width="50%">

You may notice that the *cmd-line* and the *user* namespace that live under the *se.example* top namespace are marked with an arrow. This is because they are [symbolic links](https://en.wikipedia.org/wiki/Symbolic_link). This is just a trick that enables us to work with the codebase as if it was a single system similar to a monolith. 

With this setup we can start the [REPL](https://en.wikipedia.org/wiki/Read%E2%80%93eval%E2%80%93print_loop) once and keep on working without restarting it, even though we are working with physically and logically separated components and bases. It also enables refactoring support between components and all the other benefits you get when everything resides in one place, like searching and highlighting unused code.

When working with a Polylith environment in your IDE, you’ll lose the git integration features that you would have with a monolith (the same is true for Microservices). That’s due to the symbolic links confusing the git plugin in your IDE. There are two solutions to this issue: 1) open the individual component and base projects in separate IDE windows, and manage git from there, or 2) use git from the command line or an external GUI tool (such as [SourceTree](https://www.sourcetreeapp.com/)).

An improvement would be to create a Polylith plugin for each of the popular IDEs, which includes VCS support.

#### Emacs/CIDER Support

If you use [Emacs/CIDER](https://github.com/clojure-emacs/cider) you should set `vc-follow-symlinks` to `nil` and ensure that `find-file-visit-truename` is also `nil`. It can be done with [per-directory local variables](https://www.gnu.org/software/emacs/manual/html_node/emacs/Directory-Variables.html) - just create `.dir-locals.el` file in the *workspace root* directory with this content:
``` elisp
((nil . ((find-file-visit-truename . nil)
         (vc-follow-symlinks       . nil))))
```
You might want to white-list `vc-follow-symlinks` as [safe file variable](https://www.gnu.org/software/emacs/manual/html_node/emacs/Safe-File-Variables.html) by adding `(put 'vc-follow-symlinks 'safe-local-variable (lambda (x) (memq x '(t nil ask))))` to your configs.

To see why this is necessary, please check [this issue](https://github.com/clojure-emacs/cider/issues/2505).

By default Emacs opens(visits) same file with the same buffer even if you use a different path (symlink). if it's too inconvenient you can set `find-file-existing-other-name` to `nil`:
```
find-file-existing-other-name documentation:
Non-nil means find a file under alternative names, in existing buffers.
This means if any existing buffer is visiting the file you want
under another name, you get the existing buffer instead of a new buffer.
```
Note that it can lead to data loss if buffers get out of sync.

### Working with the code

Let’s add some code to the user component and the *cmd-line* base and let *cmd-line* base use the component.

Change the user interface *user/interface.clj* in *interfaces* to:
```clojure
(ns se.example.user.interface)

;; We "forgot" to add the hello! function.
```

...and the implementation *user/core.clj* in *src* to:
```clojure
(ns se.example.user.core

(defn hello! [user]
  (println (str "Hello " user "!")))
```

...and its interface *user/interface.clj* in *src* to:
```clojure
(ns se.example.user.interface
  (:require [se.example.user.core :as core]))

(defn hello! [user]
  (core/hello! user))
```

...and the user test *user/core-test* in *test* to:
```clojure
(ns se.example.user.core-test
  (:require [clojure.test :refer :all]
            [se.example.user.interface :as user]))

(deftest print-user-test
  (let [output (with-out-str (user/hello! "Bill"))]
    (is (= "Hello Bill!\n"
           output))))
```

...and the base *cmd-line/core.clj* in *src* to:
```clojure
(ns se.example.cmd-line.core
  (:require [se.example.user.interface :as user])
  (:gen-class))

(defn -main [& args]
  (user/hello! "Victoria"))
```

...and its test *cmd-line/core-test.clj* in *test* to:
```clojure
(ns se.example.cmd-line.core-test
  (:require [clojure.test :refer :all]
            [se.example.cmd-line.core :as core]))

(deftest hello-world-example-test
  (let [output (with-out-str (core/-main))]
    (is (= "Hello Victoria!\n"
           output))))
```

Now we can run all the tests from the IDE or the console:
```
$ cd environments/development
$ lein test

lein test se.example.cmd-line.core-test

lein test se.example.user.core-test

Ran 2 tests containing 2 assertions.
0 failures, 0 errors.
```

The *cmd-line* system only contains the base.

We can now either [add](#add) *user* to *cmd-line* with:
```
$ lein polylith add user cmd-line
```

...or let the [sync](#sync) command detect the missing component and add it for us:
```
$ lein polylith sync
Added these definitions to 'interfaces/src/se/example/user/interface.clj':
  (defn hello! [user])
Added component 'user' to system 'cmd-line'.
```

This will add the *user* component to the *cmd-line* system:<br>
<img src="images/env-systems-03.png" width="50%">

It also detected the missing user workspace interface and added it:
```
(ns se.example.user.interface)

;; We "forgot" to add the hello! function.

(defn hello! [user])
```

Let’s look again:
```
$ lein polylith info

interfaces:
  user *
components:
  user *
bases:
  cmd-line
systems:
  cmd-line
    user *     -> component
    cmd-line   -> base
environments:
  development
    user *     -> component
    cmd-line   -> base
```

## Dependencies

In our example, the *cmd-line* base has a dependency to the *user*
component via the `:require` statement in *cmd-line/core.clj*.

We can list all existing dependencies to other components by executing the [deps](#deps) command from the workspace root. By default, the name of the component interfaces are listed:
```
$ cd ../..
$ lein polylith deps

cmd-line:
  user
user: 
```

This means that *cmd-line* depends on *user* and that user doesn’t depend on other components at all.

We can also list function dependencies with:
```
$ lein polylith deps +f

cmd-line:
  se.example.user.interface/hello!
user:
```

For the plugin to work correctly, you need to use the `:as` syntax in the `:require` part of the `ns` statement, for example:
```clojure
(ns se.example.standalone.core
  (:require [se.example.user.interface :as user])
  …)
```

The plugin can then detect dependencies to the *user* in function calls like this:
```clojure
(user/hello! “Victoria”)
```

It’s not only possible to expose functions and macros defined by `defn` and `defmacro` but also values defined by `def`. The `def` statements need to be added to both the *workspace interface* and all *component interfaces* in the same way as with `defn`. It can make the code that accesses the interface look a bit cleaner if you have constants like this:
```clojure
(def con-string database/connection-string)
```

...instead of:
```clojure
(def con-string (database/connection-string))
```

The use of `def` statements can be especially useful when declaring [Clojure spec](https://clojure.org/guides/spec) definitions.
The use of `defmacro` can be handy if you delegate to other macros like an *info* macro in a logging library.

### Indirect dependencies

The [success](#success) command is the last step to be executed in the [build](#build) command if everything went ok. We can execute it to fake a successful build:
```
$ lein polylith success
set :last-success in .polylith/time.edn
$ lein polylith info

interfaces:
  user
components:
  user
bases:
  cmd-line
systems:
  cmd-line
    user       -> component
    cmd-line   -> base
environments:
  development
    user       -> component
    cmd-line   -> base
```

Now all * have disappeared. 

Now make a small change to *user/core.clj* for example by executing the [touch](https://en.wikipedia.org/wiki/Touch_(Unix)) command:
```
$ touch components/user/src/se/example/user/core.clj 
```

Now run the [info](#info) command again:
```
$ lein polylith info

interfaces:
  user
components:
  user *
bases:
  cmd-line (*)
systems:
  cmd-line
    user *        -> component
    cmd-line (*)  -> base
environments:
  development
    user *        -> component
    cmd-line (*)  -> base
```

We can see that *user* is changed and that *cmd-line* is marked with an _(\*)_. The reason is that it depends on something that has changed but is unchanged itself. This will ensure that its tests are executed when running the [test](#test) or [build](#build) command.

Now let’s create the *address* component:
```
$ lein polylith create c address
```

This creates the *address* component and adds it to the *development* environment:<br>
<img src="images/env-systems-04.png" width="50%">

### Circular dependencies

Change the interface *address/interface.clj* in *interfaces* to:
```clojure
(ns se.example.address.interface)

(defn process! [address])
```

...and the interface *address/interface.clj* in *src* to:
```clojure
(ns se.example.address.interface
  (:require [se.example.address.core :as core]))

(defn process! [address]
  (core/process! address))
```

Now let’s see what happens if we depend on *user* in *address/core.clj*:
```clojure
(ns se.example.address.core
  (:require [se.example.user.interface :as user]))

(defn process! [address]
  (user/hello! "Carl"))
```

...and if we depend back on *address* in *user/core.clj*:
```clojure
(ns se.example.user.core
  (:require [se.example.address.interface :as address]))

(defn hello! [user]
  (address/process! "first street"))
```

Let’s execute the *info* command again:
```
$ lein polylith info
interfaces:
  address *
  user
components:
  address *
  user *   
bases:
  cmd-line (*)
systems:
  cmd-line
    user *        -> component
    cmd-line (*)  -> base
environments:
  development
    address *     -> component  (circular deps: address > user > address)
    user *        -> component  (circular deps: user > address > user)
    cmd-line (*)  -> base       (circular deps: cmd-line > user > address > user)
```

The circular dependencies are detected so that we can take appropriate actions. If you run the *cmd-line/core-test* from the IDE, it will complain:<br>
<img src="images/ide-circular-dependencies.png">

If you run the tests from the console, you will get a similar error:
```
$ cd environments/development
$ lein test

java.lang.Exception: Cyclic load dependency: [ /se/example/address/interface ]->/se/example/user/core->/se/example/user/interface->/se/example/address/core->[ /se/example/address/interface ]->/se/example/address/core_test
```

If you don’t have test coverage of these components (although that will of course never happen!) then it can slip through to the production code. To avoid that, the check for circular dependencies is also executed as a step in the [build](#build) command.

Circular dependencies will not only detect simple cases like a > b > a, but also a > b > c > d > a.

Now restore *user/core.clj* in *src*:
```clojure
(ns se.example.user.core

(defn hello! [user]
  (println (str "Hello " user "!")))
```

...and change *address/core.clj* in *src* to:
```clojure
(ns se.example.address.core)

(defn process! [address]
  (println "process address:" address))
```

...and change its test *address/core-test.clj* in *test* to:
```clojure
(ns se.example.address.core-test
  (:require [clojure.test :refer :all]
            [se.example.address.interface :as address]))

(deftest process-address-test
  (let [output (with-out-str (address/process! "first street"))]
    (is (= "process address: first street\n"
           output))))
```

Now we should be able to run the tests again:
```
$ lein polylith test

lein test se.example.address.core-test

lein test se.example.cmd-line.core-test

lein test se.example.user.core-test

Ran 3 tests containing 3 assertions.
0 failures, 0 errors.
```

## Context

When you have worked with the Polylith for a while, we believe you will soon find it very pleasant to work with. One reason is that you can start the [REPL](https://en.wikipedia.org/wiki/Read%E2%80%93eval%E2%80%93print_loop) once without the need to restart it. Another reason is that you have direct access to everything from one place, the *development environment*.
 
Another slightly subtle detail is that the components via their interfaces give you *context*. This might sound like a small thing, but it is very helpful. It not just helps with the design of the system but it also improves the way you work with the code. 

Object oriented languages give you context by using objects. Let’s say you work in an object oriented language and that you want to save the object *userToBeSaved*. If you type *userToBeSaved* followed by a “.”, the intellisense in the IDE will show you a list of available methods for that object, for example `persist`:
```bash
userToBeSaved.persist(db)
```

...or use a service:
```bash
userService.persist(db, userToBeSaved)
```

With the Polylith you actually get the same level of support from the IDE. Here you would instead import the *user interface* and then write:
```clojure
(user/
```

Here the IDE will list all available functions in the *user interface* and one of them would be `persist!`:
```clojure
(user/persist! db user-to-be-saved)
```

## Libraries

In the Polylith world, each library uses the same version everywhere within a workspace. So if you have a system called *backend* and another system called *backend2* and both have the *user* component that uses the library `[clj-time “0.14.2”]` both *backend* and *backend2* will now use the same version of that library.

The plugin can help you keep all your libraries in sync just by calling the *sync* command. The [sync](#sync) is also called as a step in the [test](#test), [compile](#compile) and [build](#build) command to make sure you don’t forget it.

Now open *project-files/components/user-project.clj* from the development environment. This is a symbolic link to *example/components/user/project.clj*.

Add the `[clj-time “0.14.2”]` library:
```clojure
(defproject se.example/user "0.1"
  :description "A user component"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [se.example/interfaces "1.0"]
                 [clj-time "0.14.2"]]
  :aot :all)
```

If we look in *project-files/systems/cmd-line-project.clj*, which is a symbolic link to *systems/cmd-line/project.clj*, it looks like this:
```clojure
(defproject se.example/cmd-line "0.1"
  :description "A cmd-line system."
  :dependencies ([org.clojure/clojure "1.9.0"])
  :aot :all
  :main se.example.cmd-line.core)
```
...and the *project.clj* file in the root of the development project looks like this:
```clojure
(defproject se.example/development "1.0"
  :description "The main development environment."
  :dependencies [[org.clojure/clojure "1.9.0"]])
```

### Syncing dependencies

To be able to work with the *user* component from the development environment, the *clj-time* library now needs to be added to the development *project file*. The good thing is that the *sync* command can help us with that (make sure you are at the workspace root):
```
$ lein polylith sync
updated: environments/development/project.clj
updated: components/user/project.clj
updated: components/address/project.clj
updated: systems/cmd-line/project.clj
```

The *environments/development/project.clj* file now looks like this:
```clojure
(defproject se.example/development "1.0"
  :description "The main development environment."
  :dependencies [[clj-time "0.14.2"]
                 [org.clojure/clojure "1.9.0"]])
```

...and *systems/cmd-line/project.clj* like this:
```clojure
  (defproject se.example/cmd-line "0.1"
    :description "A cmd-line system."
    :dependencies ([clj-time "0.14.2"]
                   [org.clojure/clojure "1.9.0"])
    :aot :all
    :main se.example.cmd-line.core)
```

The [sync](#sync) command not only helps you add the missing libraries, it also sorts the dependency list in alphabetical order. You can also have keywords like `:extensions` in a dependency.

The *development environment* project file (*environments/development/project.clj*) is the master for controlling library versions. However, the individual project files in each component or base are where you add new libraries.

The rules of thumb are:
* add new libraries to component or base project files.
* update library versions in the development project file.

Let’s update *clj-time* to *0.14.4* in *environments/development/project.clj* to:
```clojure
(defproject se.example/development "1.0"
  :description "The main development environment."
  :dependencies [[clj-time "0.14.4"]
                 [org.clojure/clojure "1.9.0"]])
```

...and run [sync](#sync) to see what happens:
```
$ lein polylith sync
  updated: components/user/project.clj
  updated: systems/cmd-line/project.clj
```

The *clj-time* version was updated for the *user* component:
```clojure
$ cat components/user/project.clj
(defproject se.example/user "0.1"
  :description "A user component"
  :dependencies [[clj-time "0.14.4"]
                 [org.clojure/clojure "1.9.0"]                 
                 [se.example/interfaces "1.0"]]
  :aot :all)
```

...and added to the *cmd-line* system:
```clojure
$ cat systems/cmd-line/project.clj
(defproject se.example/cmd-line "0.1"
  :description "A cmd-line system."
  :dependencies [[clj-time "0.14.4"]
                 [org.clojure/clojure "1.9.0"]]
  :aot :all
  :main se.example.cmd-line.core)
```

To release often in a controlled way is a good thing and to keep the code and its libraries in sync is also a good thing. The best thing is that the plugin helps you with both!

## Testing

This plugin encourages a test-centric approach when working with your code. The introduction of components (that are well isolated and more manageable in size compared to systems) makes testing less complex, faster and more fun.

The [test](#test) and [build](#build) commands, that only compiles and runs what has changed since the last successful test or build, makes testing an iterative process that allows you to grow the software in small controlled steps.

### Workflow

A natural way of working with the Polylith is this:
1. Change code and/or add tests.
2. Run tests from your development environment (e.g. IDE).
3. Repeat 1 and 2 till you become confident.
4. Execute `lein polylith test`
   - if fails => go to 1.
   - if ok => commit to local git repo
5. Go to 1 or push to global git repo.

This gives a really fast feedback loop from the REPL (1 and 2).
The [test](#test) command (4) will only compile and run tests for affected components since the last time you executed the [test](#test) command.
This will save more and more time the bigger your codebase grows compared to having to compile everything every time.
Finally you push your changes (5) and then the same plugin code will be executed on the CI server so if it worked locally you can be quite confident that it will also work on the server
(the [sync](#sync) command makes sure the code also passes the last build step).

### Test changes

To understand how the plugin figures out what components and bases that have changed we can start by looking at the [diff](#diff) command:
```
$ lein polylith diff
  interfaces/src/se/example/address/interface.clj
  systems/cmd-line/src/se/example/user/core.clj
  systems/cmd-line/project.clj
  components/address/src/se/example/address/interface.clj
  components/address/src/se/example/address/core.clj
  components/address/readme.md
  components/address/test/se/example/address/core_test.clj
  components/address/project.clj
  components/user/src/se/example/user/core.clj
  components/user/project.clj
  environments/development/src/se/example/address/interface.clj
  environments/development/src/se/example/address/core.clj
  environments/development/src/se/example/user/core.clj
  environments/development/interfaces/se/example/address/interface.clj
  environments/development/docs/address-readme.md
  environments/development/project-files/systems/cmd-line-project.clj
  environments/development/project-files/components/address-project.clj
  environments/development/project-files/components/user-project.clj
  environments/development/test/se/example/address/core_test.clj
  environments/development/project.clj
```

To calculate this list, the plugin starts with going through all the files in the workspace recursively. Then it takes the value of the *:last-success* key in *.polylith/time.edn* and uses that timestamp to filter out all files with a later timestamp.

If the *CI* variable is set, it’s an indication that the code is executed by the continuous integration server. In that case it takes the SHA1 value from the *:last-success* key in *.polylith/git.edn* and then performs a `git diff --name-only last-successful-hash current-hash`. This approach covers one special case that the local *time.edn* solution doesn’t cover, namely when a file or directory has been deleted but nothing else was changed.

Commands like [build](#build), [changes](#changes), [info](#info) and [test](#test) all start by calling [diff](#diff) internally to calculate changed components and bases to figure out what components and bases to operate on. They all support sending in a *bookmark* or a *timestamp*.

The timestamp is the time in milliseconds since 1970, e.g. *1529504135000* instead of *2018-06-20 16:15:35* (the 20th of April 2018, 4:15 pm and 35 seconds). The bookmarks are found in *.polylith/time.edn* or *.polylith/git.edn* depending on environment.

So if we figure out when the last successful test or build was:
```
$ cat .polylith/time.edn
{:last-success 1529504135000}
```

...then these three commands are equivalent: 
```
$ lein polylith diff
$ lein polylith diff 1529504135000
$ lein polylith diff last-success
```

To see all changed files since 1970-01-01, simply execute:
```
$ lein polylith diff 0
```

To test everything since 1970-01-01, execute:
```
$ lein polylith test 0
```

## Design

The Polylith makes it easier to compose well-designed systems by providing decoupled building blocks in manageable sizes. It’s just code that directly connects with other code and can therefore easily be used everywhere, like lego bricks.

Let’s take the [RealWorld](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app) system as an example:<br>
<img src="images/real-world-system.png">

Here we have the rest-api base at the bottom followed by components and libraries on top (not shown here). This design ensures that all dependencies always point in the same direction (up) which is a property it shares with the [layered architecture](https://en.wikipedia.org/wiki/Multitier_architecture), the [hexagonal architecture](http://www.dossier-andreas.net/software_architecture/ports_and_adapters.html), and others, but with more flexibility built in.

The design helps you focus on building useful building blocks that are easy to combine like
functions without introducing circular dependencies.
It also makes it easier to find things and to reason about your systems.

> “All problems in computer science can be solved by another level of indirection”<br>
  Butler Lampson

The statement means something like “It is easier to move a problem around than it is to solve it” (see [indirections](https://en.wikipedia.org/wiki/Indirection)).
With the Polylith design you get decoupling “for free” via the component’s interfaces which reduces the need for layers to a minimum. It also makes reuse and replacement of code a natural part of your daily work.

### Test doubles

If we also add address to the cmd-line system:
```
$ lein polylith add address cmd-line
```

Part of the workspace will now look like this:<br>
<img src="images/env-systems-05.png" width="50%">

It’s possible to have more than one component that conforms to an interface. This can be useful if we want to create a separate test system where one of the components is replaced by a fake implementation (see [test doubles](https://books.google.se/books?id=Csz3DAAAQBAJ&pg=PT248&lpg=PT248&dq=fake+test+double+alexander+tarlinder&source=bl&ots=LQ8VcKUdCF&sig=UuASn3TBj48EznuYvnTUBzlnlMY&hl=sv&sa=X&ved=0ahUKEwjrjprJ6f_bAhWGApoKHUDDB78Q6AEIXDAK#v=onepage&q&f=false)).

Let’s create a fake implementation of the *address* interface:
```
$ lein polylith create c address-fake address
FYI: the component address-fake was created but not added to development because it's interface address was already used by address.
```

The workspace will now look like this:<br>
<img src="images/env-systems-06.png" width="50%">

The reason why *address-fake* wasn’t added to the *development* environment was that it already contained a component that conformed to the *address* interface, namely the *address* component. This is not allowed by the plugin because the JVM would then only pick one of them anyway (the one that comes first in the classpath) while the other component would be shadowed and ignored.

If we run the [info](#info) command again we can see that the *address-fake* component has an interface *(address)* with a different name than the component itself *(address-fake)*:
```
$ lein polylith info
interfaces:
  address *
  user
components:
  address *     
  address-fake *   > address
  user *        
bases:
  cmd-line (*)
systems:
  cmd-line *
    address *     -> component
    user *        -> component
    cmd-line (*)  -> base
environments:
  development
    address *     -> component
    user *        -> component
    cmd-line (*)  -> base
```

Now create the *cmd-line-test* system using the *cmd-line* base and then add *user* and *address-fake* to it:
```
$ lein polylith create s cmd-line-test cmd-line
$ lein polylith add user cmd-line-test
$ lein polylith add address-fake cmd-line-test
```

Now we have one more system, the *cmd-line-test*:<br>
<img src="images/env-systems-07.png" width="92%">

In this first release of the Polylith we only support having one development environment and we don’t support adding or removing bases against the development environment either. The reason is that we wanted to release an [MVP](https://en.wikipedia.org/wiki/Minimum_viable_product). A consequence of these decisions is that it’s harder to edit and test components that are not part of the *development* environment (like *address-fake* in this case).

What you can do in the meanwhile is to edit *address-fake* either from its own project or from the *cmd-line-test* system project.

You can now either add unit tests to *address-fake* or create system-level tests by manually creating a *test/se/example* directory structure under *example/systems/cmd-line-test*, adding your tests there and making sure you execute these from your [CI](https://en.wikipedia.org/wiki/Continuous_integration) build.

## Versioning and branching

The polylith architecture gives you plenty of alternatives to avoid branching.
You may need to make extensive changes to a component that is hard to do without
being affected or affect other team members or teams. One way of doing that
is to add a new component that fulfills the same interface. Now you can work with that
component in isolation and keep up with changes in the corresponding *workspace interface*.
When the new component is ready to use you can just switch it in to replace the old one.

When the plugin supports more than one environment, this will be even smother,
but in the meantime you can still do it by editing the component from the component
project itself, as you would do if it was a Microservice architecture.

Another situation that can arise is when you have different teams working on new functionality
that takes a long time to implement and probably will break existing code.
In these situations an alternative
is to temporarily introduce new versions in the interfaces of the components you want to change.
If you for example have the interface *com.abc.user.interface* containing your function
signatures, then you can introduce the namespace *com.abc.user.v2.interface* containing
the changes that otherwise would break your code.
When all development has finished, you can either choose to keep that interface or merging
it back to *com.abc.user.interface* and remove *com.abc.user.v2.interface*.

## Mix languages
The Polylith allows us to run multiple languages side by side where each language lives in their own workspace.
This will work especially well if we run different languages on top of the same platform, e.g. the JVM
(see list of [JVM languages](https://en.wikipedia.org/wiki/List_of_JVM_languages)).

Let's say we have the languages A, B and C. The first thing to remember is to have different
names of the top namespace for each language, so that we don't run into name conflicts.
We would end up with top namespaces like: *com.mycompany.a*, *com.mycompany.b* and *com.mycompany.c*.
Each language will have their own workspace and they will compile each component to its own library,
alternatively compile all components into one big jar like a.jar, b.jar or c.jar.

So if component *com.mycompany.a.authentication* is used by *com.mycompany.b.user*,
then *com.mycompany.b.user* will include either *a-authentication.jar* or *a.jar*
in its library list and delegate to functions in *authentication* from the *user* component interface.

With this setup, all components can be used between the languages by first compiling them into libraries.
We could also use the [Java Native Interface](https://en.wikipedia.org/wiki/Java_Native_Interface) to share code between languages
that don't run on top of the JVM, or use something like [Neanderthal](https://neanderthal.uncomplicate.org)
if we want to integrate with the [GPU](https://en.wikipedia.org/wiki/Graphics_processing_unit).

An alternative approach would be to use the [GraalVM](https://www.graalvm.org) or similar.

## In practice
Here are some tips on how to think when working with the Polylith architecture based on our first two years' experience working with the Polylith in production.

A good principle is to keep your components small. In our experience a size between 100 and 1000 lines of code is a good rule of thumb in Clojure. But as long as you keep the interface cohesive and manage to divide the implementation into well named namespaces, you will be fine even with larger components.

Another advantage of keeping the components small is that it also reduces the build time. The reason is that all the components and bases that depend on a changed component (directly or indirectly) have to execute their tests when building the workspace.
If you for example have a huge *common* component, you run the risk of a majority of your components depending on that component and therefore have to run their tests during build time every time that component changes. If you instead keep the components small, less code will be affected by a change which will in turn reduce the number of executed tests.

To give good names to your systems, bases and components is well spend time. It will affect how you think, reason and communicate about your system. It also makes it easier to find what you are looking for.

It's recommended to wrap API's with components. If you have a REST API to an external system "x" then you should wrap it with the component "x-api" that exposes a nice interface. Then you may have another component "x" where you put all the business logic. To keep them separate is a good thing.

We created a dedicated base, called *migration*, that was not part of any system but used from the development environment.
In the beginning we used it to migrate an old relational database to [Datomic](https://www.datomic.com)
by executing code in the *migration* base from our development environment.
It turned out that we could put other type of scripts here too, like cleaning data in the database
or to execute "jobs". Basically everything that was executed once and didn't need its own server.
We kept all these scripts in the version control systems to keep track of what changes had been done
and to steal code from when it was time to create another similar script.

## Continuous Integration

As we mentioned before, Polylith uses timestamps to decide if a file has changed since the last success. This works fine on your local machine, however when you push your code to a git repository, all the timestamps will be reset. Once your Continuous Integration (CI) system pulls your code, it will assume everything has changed since the last build.

Don’t worry, the Polylith tool solves this too.

Most popular CI products have an environment variable named ```CI```. The Polylith tool uses that variable to detect if the commands are running on your local machine or on your CI. On your CI, instead of looking at the timestamps, it uses git commit SHA1s to figure out what has changed. Every time the CI runs the [success](#success) command, it saves the current SHA1 to ```.polylith/git.edn``` file. You can cache this file for the next build so the Polylith tool can detect changes since the last build. You can also test this locally if you set the ```CI``` environment variable on your computer before running any commands.

You can find a complete [CircleCI](https://circleci.com) configuration file that handles Polylith builds in the [RealWorld example](https://github.com/furkan3ayraktar/clojure-polylith-realworld-example-app#continuous-integration).

## Commands

The goal for this documentation has so far been to give an overall understanding of what problems the plugin tries to solve and how to use it. This section zooms in and explains each command separately. The individual help texts listed here are taken from the built-in *help* command described here.

**Commands**
- [add](#add)
- [build](#build)
- [changes](#changes)
- [compile](#compile)
- [create](#create)
- [delete](#delete)
- [deps](#deps)
- [diff](#diff)
- [info](#info)
- [prompt](#prompt)
- [remove](#remove)
- [settings](#settings)
- [success](#success)
- [sync](#sync)
- [test](#test)

To list all available commands, write:
```
$ lein polylith help
```

Or just:
```
$ lein polylith
```

```
  Polylith 0.2.3 (2020-05-09) - https://github.com/tengstrand/lein-polylith

  lein polylith CMD [ARGS]  - where CMD [ARGS] are:

    add C S               Adds a component to a system.
    build N [A] [S]       Builds changed systems and create artifacts.
    changes E P [A]       Lists changed components, bases or systems.
    compile P [A] [S]     Compiles changed components, bases and systems.
    create X N [F]        Creates a component, system or workspace.
    delete c N [B]        Deletes a component, base or system.
    deps [A]              Lists dependencies.
    diff P [A] [F]        Lists all changes since a specific point in time.
    help [C]              Show this help or help for specified command.
    info P [A]            Lists interfaces, components, bases, systems and environments.
    prompt                Starts a prompt for current workspace.
    remove C S            Removes a component from a system.
    settings              Shows polylith settings.
    success [B]           Sets last-success or given bookmark.
    sync [F]              Syncs library dependencies, system components and workspace interfaces.
    test P [A] [S]        Executes affected tests in components and bases.

  Examples:
    lein polylith add mycomponent targetsystem
    lein polylith build
    lein polylith build -sync -compile -test -success
    lein polylith build 1523649477000
    lein polylith build mybookmark
    lein polylith changes b
    lein polylith changes c 1523649477000
    lein polylith changes s mybookmark
    lein polylith compile
    lein polylith compile -sync
    lein polylith compile 1523649477000
    lein polylith compile mybookmark
    lein polylith create c mycomponent
    lein polylith create c mycomponent myinterface
    lein polylith create s mysystem mybase
    lein polylith create w myworkspace -
    lein polylith create w myworkspace com.my.company
    lein polylith create w myworkspace com.my.company -git
    lein polylith delete c mycomponent
    lein polylith delete b mybase
    lein polylith delete s mysystem
    lein polylith delete s mysystem mybase
    lein polylith deps
    lein polylith deps +c
    lein polylith deps +f
    lein polylith deps development
    lein polylith deps mycomponent +f
    lein polylith deps myenvironment +c
    lein polylith diff
    lein polylith diff 1523649477000
    lein polylith diff mybookmark
    lein polylith diff mybookmark +
    lein polylith help
    lein polylith help info
    lein polylith info
    lein polylith info 1523649477000
    lein polylith info mybookmark
    lein polylith prompt
    lein polylith remove mycomponent mysystem
    lein polylith settings
    lein polylith success
    lein polylith success mybookmark
    lein polylith sync
    lein polylith test
    lein polylith test -compile
    lein polylith test 1523649477000
    lein polylith test mybookmark
    lein polylith test -compile -success
```

As described above, you can read more about a specific command, e.g. *prompt*, by typing:
```
$ lein polylith help prompt
  Starts a prompt for current workspace.
  ...
```

If you plan to execute the code from a script, here comes some useful information.
If a command halts because of a problem, it will perform a [(System/exit 1)](https://docs.oracle.com/javase/10/docs/api/java/lang/System.html#exit(int)) call.
The return code can therefore be used to check if something went wrong.
Possible problems could for example be that an unsolvable interface declaration or a circular dependency was detected.

### add
```
  Adds a component to a system.

  lein polylith add COMPONENT SYSTEM
     COMPONENT = Component to add.
     SYSTEM    = Add COMPONENT to SYSTEM.

  example:
    lein polylith add mycomponent mysystem
```

### build
```
  Builds system artifacts.

  The following steps are performed:
    - checks for circular dependencies and quits if found.
    - calculates what components and bases to build based on what has
      changed since the last successful test or build.
    - calls 'sync' and makes sure that all dependencies in project.clj
      files are in sync and that all systems have all components they need.
    - AOT-compiles changed components, bases and systems to check that they compile
      against the workspace interfaces and have all the libraries they need.
    - runs tests for all bases and components that have been affected by the changes.
    - executes build.sh for all changed systems to make sure they have a working
      build script and no missing components or libraries.
    - if the entire build is successful, then execute the success command
      that updates the time for the last successful test or build.

  lein polylith build [ARG] [SKIP]
    ARG = (omitted) -> Since last successful test or build, stored in bookmark
                       :last-success in WS-ROOT/.polylith/time.edn
                       or :last-success in WS-ROOT/.polylith/git.edn
                       if you have CI variable set to something on the machine.
          timestamp -> Since the given timestamp (milliseconds since 1970).
          git-hash  -> Since the given git hash if the CI variable is set.
          bookmark  -> Since the timestamp for the given bookmark in
                       WS-ROOT/.polylith/time.edn or since the git hash
                       for the given bookmark in WS-ROOT/.polylith/git.edn
                       if the CI variable is set.

    SKIP = (omitted)      -> Executes all steps.
           -circular-deps -> Skips checking for circular dependencies step
           -sync          -> Skips dependency sync step
           -compile       -> Skips compilation step
           -test          -> Skips test step
           -success       -> Skips success step

  'lein polylith build 0' can be used to build all files in the workspace
  (or at least changes since 1970-01-01).

  examples:
    lein polylith build
    lein polylith build -compile
    lein polylith build 1523649477000
    lein polylith build mybookmark
    lein polylith build 1523649477000 -compile -test
```

### changes

```
  Shows what has been changed since a specific point in time.

  lein polylith changes ENTITY [ARG]
    ENTITY = i[nterface] -> Shows changed interfaces
             c[omponent] -> Shows changed components
             b[ase]      -> Shows changed bases
             s[ystem]    -> Shows changed systems

    ARG = (omitted) -> Since last successful test or build, stored in bookmark
                       :last-success in WS-ROOT/.polylith/time.edn. or
                       :last-success in WS-ROOT/.polylith/git.edn if
                       you have the CI variable set to something on the machine.
          timestamp -> Since the given timestamp (milliseconds since 1970).
          git-hash  -> Since the given git hash if the CI variable is set.
          bookmark  -> Since the timestamp for the given bookmark in
                       WS-ROOT/.polylith/time.edn or since the git hash
                       for the given bookmark in WS-ROOT/.polylith/git.edn
                       if the CI variable is set.

  'lein polylith changes 0' can be used to build all files in the workspace
  (or at least changes since 1970-01-01).

  example:
    lein polylith changes i
    lein polylith changes c
    lein polylith changes component
    lein polylith changes b
    lein polylith changes s 1523649477000
    lein polylith changes s mybookmark
```

### compile
```
  Compiles changes since a specific point in time.

  The following steps are performed:
    - checks for circular dependencies and stops if found.
    - calculates what components and bases to process based on what has
      changed since the last successful test or build.
    - calls 'sync' and makes sure that all dependencies in project.clj
      files are in sync and that all systems have all components they need.
    - AOT compile changed components, bases and systems to check that they compile
      and fulfill workspace interfaces and have all libraries they need.

  lein polylith compile [ARG]
    ARG = (omitted) -> Since last successful test or build, stored in bookmark
                       :last-success in WS-ROOT/.polylith/time.edn
                       or :last-success in WS-ROOT/.polylith/git.edn
                       if you have the CI variable set to something on the machine.
          timestamp -> Since the given timestamp (milliseconds since 1970).
          git-hash  -> Since the given git hash if the CI variable is set.
          bookmark  -> Since the timestamp for the given bookmark in
                       WS-ROOT/.polylith/time.edn or since the git hash
                       for the given bookmark in WS-ROOT/.polylith/git.edn
                       if the CI variable is set.

    SKIP = (omitted)      -> Executes all steps.
           -circular-deps -> Skips checking for circular dependencies step.
           -sync           -> Skips dependency sync step.

  'lein polylith compile 0' can be used to compile all files in the workspace
  (or at least changes since 1970-01-01).

  example:
    lein polylith compile
    lein polylith compile -sync
    lein polylith compile 1523649477000
    lein polylith compile mybookmark
    lein polylith compile mybookmark -sync
```

### create
```
  Creates a component:

  lein polylith create c[omponent] NAME [INTERFACE]
    NAME = Component name
    INTERFACE = Interface name. Same as component name if omitted.
  --------------------------------------------------------
  Creates a system:

  lein polylith create s[ystem] NAME BASE
    NAME = System name.
    BASE = Base name.
  --------------------------------------------------------
  Creates a workspace:

  lein polylith create w[orkspace] WS NS [FLAG]
    WS = Workspace name.
    NS = Namespace name or '-' to omit it.
         It's recommended and good practice to give a namespace.
    FLAG = (omitted) -> version control the workspace with git.
           -git      -> don't version control the workspace
                        (may occur in any order).

  example:
    lein polylith create c mycomponent
    lein polylith create c mycomponent myinterface
    lein polylith create component mycomponent
    lein polylith create component mycomponent myinterface
    lein polylith create s mysystem mybase
    lein polylith create system mysystem mybase
    lein polylith create w myworkspace -
    lein polylith create w myworkspace com.my.company
    lein polylith create w myworkspace com.my.company -git
    lein polylith create workspace myworkspace com.my.company
```

### delete
```
  Deletes a component and its interface if no other components use it:

  lein polylith delete c[omponent] NAME
    NAME = Component name
  --------------------------------------------------------
  Deletes a base:

  lein polylith delete b[ase] NAME
    NAME = Base name.
  --------------------------------------------------------
  Deletes a system (and its base if given):

  lein polylith delete s[ystem] NAME [BASE]
    NAME = System name.
    BASE = Base name.

  example:
    lein polylith delete c mycomponent
    lein polylith delete component mycomponent
    lein polylith delete b mybase
    lein polylith create base mybase
    lein polylith delete s mysystem
    lein polylith delete system mysystem
    lein polylith delete s mysystem mybase
    lein polylith delete system mysystem mybase
```

### deps
```
  Lists dependencies used in at least one environment or system.

  lein polylith deps [NAME] [FLAG]
    NAME = (omitted) -> Lists all dependencies.
           else      -> Filters dependencies by given system, environment
                        base or component name.
    FLAG = (omitted)    -> Lists interface dependencies.
           +c[omponent] -> Lists component dependencies.
           +f[unction]  -> Lists function dependencies.

  To work correctly, :require with a corresponding :as alias
  must be used to specify dependencies in each namespace, e.g.:
    (ns my.namespace.core
      (:require [my.namespace.user.interface :as user]
                [my.namespace.email.interface :as email]))

  example:
    lein polylith deps
    lein polylith deps +c
    lein polylith deps +component
    lein polylith deps +f
    lein polylith deps +function
    lein polylith deps development
    lein polylith deps mycomponent +f
    lein polylith deps myenvironment +c
```

### diff
```
  Lists all files and directories that have changed in the workspace
  since a specific point in time. Deleted files and directories
  are not detected locally (when the CI environment variable is not set).

  lein polylith diff [ARG] [FLAG]
    ARG = (omitted) -> Since last successful test or build, stored in bookmark
                       :last-success in WS-ROOT/.polylith/time.edn
                       or :last-success in WS-ROOT/.polylith/git.edn
                       if you have the CI variable set to something on the machine.
          timestamp -> Since the given timestamp (milliseconds since 1970).
          git-hash  -> Since the given git hash if the CI variable is set.
          bookmark  -> Since the timestamp for the given bookmark in
                       WS-ROOT/.polylith/time.edn or since the git hash
                       for the given bookmark in WS-ROOT/.polylith/git.edn
                       if the CI variable is set.

    FLAG = +        -> Show time information.
                       (the + sign may occur in any order in the argument list).

  'lein polylith diff 0' can be used to list all files in the workspace
  (or at least changes since 1970-01-01).

  example:
    lein polylith diff
    lein polylith diff +
    lein polylith diff + 1523649477000
    lein polylith diff 1523649477000
    lein polylith diff 1523649477000 +
    lein polylith diff mybookmark
```

### info
```
  Shows the content of a Polylith workspace and its changes since
  the last successful test or build or a given point in time.

  Each row is followed by an * if something has changed.
  Each row is followed by a (*) if nothing has changed but it
  depends on one or more components that have changed.

  lein polylith info [ARG]
    ARG = (omitted) -> Since last successful test or build, stored in bookmark
                       :last-success in WS-ROOT/.polylith/time.edn
                       or :last-success in WS-ROOT/.polylith/git.edn
                       if you have the CI variable set to something on the machine.
          timestamp -> Since the given timestamp (milliseconds since 1970).
          git-hash  -> Since the given git hash if the CI variable is set.
          bookmark  -> Since the timestamp for the given bookmark in
                       WS-ROOT/.polylith/time.edn or since the git hash
                       for the given bookmark in WS-ROOT/.polylith/git.edn
                       if the CI variable set.

  example:
    lein polylith info
    lein polylith info 1523649477000
    lein polylith info mybookmark
```

### prompt
```
  Starts a prompt for current workspace.

  Allows faster execution of all the commands from any directory.

  Type 'exit' or 'quit' to exit the prompt.

  examples:
    lein polylith prompt
```

### remove
```
  Removes a component from a system.

  lein polylith remove COMPONENT SYSTEM
    COMPONENT = Name of the component
    SYSTEM    = Name of the system

  example:
    lein polylith remove mycomponent mysystem
```

### settings
```
  Shows workspace settings with various information.

  examples:
    lein polylith settings
```

### success
```
  Depending on whether the environment variable CI is set or not:

    If CI is not set (when executed from the local development environment):
      Sets the BOOKMARK in WS-ROOT/.polylith/time.edn to current time
      in milliseconds.

    If CI is set (when executed from the continuous integration server):
      Sets the BOOKMARK in WS-ROOT/.polylith/git.edn to current Git SHA1.

  lein polylith success [BOOKMARK]
    BOOKMARK = (omitted) -> last-success
               name of the bookmark

  examples:
    lein polylith success
    lein polylith success mybookmark
```

### sync
```
  These steps are performed:

  1. Adds missing libraries to the development environment.
     The way it does that is to first check which components and bases
     are part of the development environment. Then it goes through
     those components and bases and collects a list of all their dependencies
     from each project.clj file. That list is compared with the dependencies
     in environments/development/project.clj and missing libraries are added.

  2. Makes sure that the library versions for all components
     and bases are in sync with the library versions in
     environments/development/project.clj.

  3. Makes sure that each system has a library list that reflects
     the sum of all libraries of its components and bases.

  4. Adds missing components to systems if possible/needed.
     This can be performed only if each interface belongs to exactly
     one component, otherwise an error message is displayed.

  5. Adds missing def/defn/defmacro definitions to workspace interfaces.
     All namespaces for each interface under the interfaces directory are
     parsed and all def/defn/defmacro definitions are collected into a set.
     Then all def/defn/defmacro definitions, that exist in the corresponding
     component interface(s) but not in the workspace interface, are added.
     If a new arity of a function or macro has been added or changed,
     then an error message is shown, informing you to update it manually.

  examples:
    lein polylith sync
```

### test
```
  Executes component and base tests.

  The following steps are performed:
    - checks for circular dependencies and stops if found.
    - calculates what components and bases to process based on what has
      changed since the last successful test or build.
    - calls 'sync' and makes sure that all dependencies in project.clj
      files are in sync and that all systems have all components they need.
    - AOT compile changed components, bases and systems to check that they compile
      and fulfill workspace interfaces and have all libraries they need.
    - runs tests for all bases and components that have been affected by the changes.
    - if all the tests are successful, then execute the success command
      that updates the time for the last successful test or build.

  lein polylith test [ARG] [SKIP]
    ARG = (omitted) -> Since last successful test or build, stored in bookmark
                       :last-success in WS-ROOT/.polylith/time.edn
                       or :last-success in WS-ROOT/.polylith/git.edn
                       if you have the CI variable set to something on the machine.
          timestamp -> Since the given timestamp (milliseconds since 1970).
          git-hash  -> Since the given git hash if the CI variable is set.
          bookmark  -> Since the timestamp for the given bookmark in
                       WS-ROOT/.polylith/time.edn or since the git hash
                       for the given bookmark in WS-ROOT/.polylith/git.edn
                       if CI variable set.

    SKIP = (omitted)      -> Executes all steps.
           -circular-deps -> Skips checking for circular dependencies step.
           -sync          -> Skips dependency sync step.
           -compile       -> Skips compilation step.
           -success       -> Skips success step

  'lein polylith test 0' can be used to test all files in the workspace
  (or at least changes since 1970-01-01).

  examples:
    lein polylith test
    lein polylith test -compile
    lein polylith test 1523649477000
    lein polylith test mybookmark
    lein polylith test mybookmark -compile
    lein polylith test -compile -success
    lein polylith test -compile test
```

## What’s next

Here are some of the planned features for the plugin:
* Support for more than one environment.
* Support for adding and removing components and bases to/from environments.
* Support for adding and removing bases to/from systems.
* Support for renaming interfaces, components, bases and systems.
* Support for creating a base only used by an environment.
* Support for executing tests in other environments than development.
* Introduce the 'doc' command that produces interactive HTML documentation where you can visualise your systems, bases, components, interfaces and how everything fits together:

<img src="images/whats-next-doc.png" width="100%" alt="System">

## Thanks

A big thanks goes to James Trunk for your support during the recent years and for always having time to listen to my sometimes crazy ideas. Thanks also for your excellent work with the Polylith presentation and documentation and the amazing level of quality that you put into everything you do.

Another big thanks goes to Furkan Bayraktar not just because you are a nice person but also because you are the fastest and most talented developer I have worked with during my 20+ years in this industry. Thanks for your contribution to the Polylith plugin and the RealWorld example.

Thanks to Kim Kinnear, the creator of [zprint](https://github.com/kkinnear/zprint) whose library helps us to update the dependencies in project.clj files.

I also want to thank my son Mattias Tengstrand for his excellent work with the [Polylith logo](#logo).

My last thanks goes to Rachel for the help with the English texts.

Joakim Tengstrand

## Contact

Feel free to contact me:<br><br>
Twitter: @jtengstrand<br>
Email: joakim[dot]tengstrand[at]gmail[dot]com

You can also get in touch with us in the [Polylith forum](https://polylith.freeflarum.com).

## License

Distributed under the Eclipse Public License, the same as Clojure.
