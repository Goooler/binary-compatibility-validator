[![Kotlin Alpha](https://kotl.in/badges/alpha.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/binary-compatibility-validator)](https://central.sonatype.com/search?q=org.jetbrains.kotlinx.binary-compatibility-validator)
[![License](https://img.shields.io/github/license/Kotlin/binary-compatibility-validator)](LICENSE.TXT)
[![KDoc link](https://img.shields.io/badge/API_reference-KDoc-blue)](https://kotlin.github.io/binary-compatibility-validator/)

# Support for this plugin has been discontinued

> [!WARNING]
> The development of a separate binary compatibility validator Gradle plugin has been discontinued, 
> and all its functionality will be moved to Kotlin Gradle Plugin starting from the [`2.2.0` release](https://kotlinlang.org/docs/whatsnew22.html#binary-compatibility-validation-included-in-kotlin-gradle-plugin).
> 
> As part of the migration, the code of the current plugin has been migrated to [the Kotlin repository](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/abi-validation), 
> as well as issues migrated to [the Kotlin project in YouTrack](https://youtrack.jetbrains.com/issues/KT?q=subsystems:%20%7BTools.%20BCV%7D,%20%7BTools.%20Gradle.%20BCV%7D).
> 
> This plugin is frozen from changes, no new features or minor bugfixes will be added to it.
> 
> The functionality of working with the ABI in Kotlin Gradle Plugin is in an experimental state now, 
> so it is recommended to continue using this plugin in production projects until KGP API stabilization.

# Binary compatibility validator

The tool allows dumping binary API of a JVM part of a Kotlin library that is public in the sense of Kotlin visibilities and ensures that the public binary API wasn't changed in a way that makes this change binary incompatible.

## Contents

* [Requirements](#requirements)
* [Setup](#setup)
  * [Tasks](#tasks)
  * [Optional parameters](#optional-parameters)
  * [Workflow](#workflow)
  * [Experimental KLib ABI validation support](#experimental-klib-abi-validation-support)
* [What constitutes the public API](#what-constitutes-the-public-api)
  * [Classes](#classes)
  * [Members](#members)
* [What makes an incompatible change to the public binary API](#what-makes-an-incompatible-change-to-the-public-binary-api)
  * [Class changes](#class-changes)
  * [Class member changes](#class-member-changes)
* [Building locally](#building-the-project-locally)

## Requirements

Binary compatibility validator plugin requires Gradle `6.1.1` or newer.

Kotlin version `1.6.20` or newer.

## Setup

Binary compatibility validator is a Gradle plugin that can be added to your build in the following way:

- in `build.gradle.kts`
```kotlin
plugins {
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.18.0"
}
```

- in `build.gradle`

```groovy
plugins {
    id 'org.jetbrains.kotlinx.binary-compatibility-validator' version '0.18.0'
}
```

It is enough to apply the plugin only to the root project build file; all sub-projects will be configured automatically.

### Tasks

The plugin provides two tasks:

  * `apiDump` — builds the project and dumps its public API in project `api` subfolder. 
  API is dumped in a human-readable format. If API dump already exists, it will be overwritten.
  * `apiCheck` — builds the project and checks that project's public API is the same as golden value
  in project `api` subfolder. This task is automatically inserted into `check` pipeline, so both `build` and `check`
  tasks will start checking public API upon their execution.

> For projects with multiple JVM targets, multiple subfolders will be created, e.g. `api/jvm` and `api/android`

### Optional parameters

Binary compatibility validator can be additionally configured with the following DSL:

Groovy
```groovy
apiValidation {
    /**
     * Packages that are excluded from public API dumps even if they
     * contain public API. 
     */
    ignoredPackages += ["kotlinx.coroutines.internal"]

    /**
     * Sub-projects that are excluded from API validation 
     */
    ignoredProjects += ["benchmarks", "examples"]

    /**
     * Classes (fully qualified) that are excluded from public API dumps even if they
     * contain public API.
     */
    ignoredClasses += ["com.company.BuildConfig"]

    /**
     * Set of annotations that exclude API from being public.
     * Typically, it is all kinds of `@InternalApi` annotations that mark 
     * effectively private API that cannot be actually private for technical reasons.
     */
    nonPublicMarkers += ["my.package.MyInternalApiAnnotation"]

    /**
     * Flag to programmatically disable compatibility validator
     */
    validationDisabled = true

    /**
     * A path to a subdirectory inside the project root directory where dumps should be stored.
     */
    apiDumpDirectory = "api"
}
```

Kotlin
```kotlin
apiValidation {
    /**
     * Packages that are excluded from public API dumps even if they
     * contain public API.
     */
    ignoredPackages.add("kotlinx.coroutines.internal")

    /**
     * Sub-projects that are excluded from API validation
     */
    ignoredProjects.addAll(listOf("benchmarks", "examples"))

    /**
     * Classes (fully qualified) that are excluded from public API dumps even if they
     * contain public API.
     */
    ignoredClasses.add("com.company.BuildConfig")
    
    /**
     * Set of annotations that exclude API from being public.
     * Typically, it is all kinds of `@InternalApi` annotations that mark
     * effectively private API that cannot be actually private for technical reasons.
     */
    nonPublicMarkers.add("my.package.MyInternalApiAnnotation")

    /**
     * Flag to programmatically disable compatibility validator
     */
    validationDisabled = false

    /**
     * A path to a subdirectory inside the project root directory where dumps should be stored.
     */
    apiDumpDirectory = "aux/validation"
}
```

### Producing dump of a jar

By default, binary compatibility validator analyzes project output class files from `build/classes` directory when building an API dump.
If you pack these classes into an output jar not in a regular way, for example, by excluding certain classes, applying `shadow` plugin, and so on,
the API dump built from the original class files may no longer reflect the resulting jar contents accurately.
In that case, it makes sense to use the resulting jar as an input of the `apiBuild` task:

Kotlin
```kotlin
tasks {
    apiBuild {
        // "jar" here is the name of the default Jar task producing the resulting jar file
        // in a multiplatform project it can be named "jvmJar"
        // if you applied the shadow plugin, it creates the "shadowJar" task that produces the transformed jar
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}
```


### Workflow

When starting to validate your library public API, we recommend the following workflow:

- Preparation phase (one-time action):
  * As the first step, apply the plugin, configure it and execute `apiDump`.
  * Validate your public API manually.
  * Commit `.api` files to your VCS.
  * At this moment, default `check` task will validate public API along with test run and will fail 
    the build if API differs.
 
- Regular workflow
  * When doing code changes that do not imply any changes in public API, no additional 
    actions should be performed. `check` task on your CI will validate everything.
  * When doing code changes that imply changes in public API, whether it is a new API or
    adjustments in existing one, `check` task will start to fail. `apiDump` should be executed manually,
    the resulting diff in `.api` file should be verified: only signatures you expected to change should be changed.
  * Commit the resulting `.api` diff along with code changes. 

### Experimental KLib ABI validation support

The KLib validation support is experimental and is a subject to change (applies to both an API and the ABI dump format).
A project has to use Kotlin 1.9.20 or newer to use this feature.

To validate public ABI of a Kotlin library (KLib) corresponding option should be enabled explicitly:
```kotlin
apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
    }
}
```

When enabled, KLib support adds additional dependencies to existing `apiDump` and `apiCheck` tasks.
Generate KLib ABI dumps are places alongside JVM dumps (in `api` subfolder, by default) 
in files named `<project name>.klib.api`.
The dump file combines all dumps generated for individual targets with declarations specific to some targets being
annotated with corresponding target names.
During the validation phase, that file is compared to the dump extracted from the latest version of the library, 
and any differences between these two files are reported as errors.

Generated ABI dumps include a library name,
so it's [recommended](https://docs.gradle.org/current/userguide/multi_project_builds.html#sec:naming_recommendations) 
to set Gradle's `rootProject.name` for your library. 
Without declaring the root project's name, Gradle defaults to using the project's directory name, which can lead to 
unstable validation behavior due to potential mismatches in naming.

Currently, all options described in [Optional parameters](#optional-parameters) section are supported for klibs too.
The only caveat here is that all class names should be specified in the JVM-format,
like `package.name.ClassName$SubclassName`.

Please refer to a [design document](docs/design/KLibSupport.md) for details on the format and rationale behind the 
current implementation.

#### KLib ABI dump generation and validation on Linux and Windows hosts

Currently, compilation to Apple-specific targets (like `iosArm64` or `watchosX86`) supported only on Apple hosts.
To ease the development on Windows and Linux hosts, binary compatibility validator does not validate ABI for targets
not supported on the current host, even if `.klib.api` file contains declarations for these targets.

This behavior could be altered to force an error when klibs for some targets could not be compiled:
```kotlin
apiValidation {
    @OptIn(kotlinx.validation.ExperimentalBCVApi::class)
    klib {
        enabled = true
        // treat a target being unsupported on a host as an error
        strictValidation = true
    }
}
```

When it comes to dump generation (`apiDump` task) on non-Apple hosts, binary compatibility validator attempts
to infer an ABI from dumps generated for supported targets and an old dump from project's `api` folder (if any).
Inferred dump may not match an actual dump,
and it is recommended to update a dump on hosts supporting all required targets, if possible. 

# What constitutes the public API

### Classes

A class is considered to be effectively public if all the following conditions are met:

 - it has public or protected JVM access (`ACC_PUBLIC` or `ACC_PROTECTED`)
 - it has one of the following visibilities in Kotlin:
    - no visibility (means no Kotlin declaration corresponds to this compiled class)
    - *public*
    - *protected*
    - *internal*, only in case if the class is annotated with `PublishedApi`
 - it isn't a local class
 - it isn't a synthetic class with mappings for `when` tableswitches (`$WhenMappings`)
 - it contains at least one effectively public member, in case if the class corresponds
   to a kotlin *file* with top-level members or a *multifile facade*
 - in case if the class is a member in another class, it is contained in the *effectively public* class
 - in case if the class is a protected member in another class, it is contained in the *non-final* class

### Members

A member of the class (i.e. a field or a method) is considered to be effectively public
if all the following conditions are met:

 - it has public or protected JVM access (`ACC_PUBLIC` or `ACC_PROTECTED`)
 - it has one of the following visibilities in Kotlin:
    - no visibility (means no Kotlin declaration corresponds to this class member)
    - *public*
    - *protected*
    - *internal*, only in case if the class is annotated with `PublishedApi`

    > Note that Kotlin visibility of a field exposed by `lateinit` property is the visibility of its setter.
 - in case if the member is protected, it is contained in *non-final* class
 - it isn't a synthetic access method for a private field

## What makes an incompatible change to the public binary API

### Class changes

For a class a binary incompatible change is:

 - changing the full class name (including package and containing classes)
 - changing the superclass, so that the class no longer has the previous superclass in
   the inheritance chain
 - changing the set of implemented interfaces so that the class
   no longer implements interfaces it had implemented before
 - changing one of the following access flags:
    - `ACC_PUBLIC`, `ACC_PROTECTED`, `ACC_PRIVATE` — lessening the class visibility
    - `ACC_FINAL` — making non-final class final
    - `ACC_ABSTRACT` — making non-abstract class abstract
    - `ACC_INTERFACE` — changing class to interface and vice versa
    - `ACC_ANNOTATION` — changing annotation to interface and vice versa

### Class member changes

For a class member a binary incompatible change is:

 - changing its name
 - changing its descriptor (erased return type and parameter types for methods);
   this includes changing field to method and vice versa
 - changing one of the following access flags:
    - `ACC_PUBLIC`, `ACC_PROTECTED`, `ACC_PRIVATE` — lessening the member visibility
    - `ACC_FINAL` — making non-final field or method final
    - `ACC_ABSTRACT` — making non-abstract method abstract
    - `ACC_STATIC` — changing instance member to static and vice versa


## Building the project locally

In order to build and run tests in the project in IDE, two prerequisites are required:

* Java 11 or above in order to use the latest ASM
* All build actions in the IDE should be delegated to Gradle

## Contributing

Read the [Contributing Guidelines](CONTRIBUTING.md).
