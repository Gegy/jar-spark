
# Jar Spark
Jar Spark is a tool that wraps the launch process of a given jar file, with support for plugins and native injection.
Plugins are loaded from a `plugins` directory, and injected onto the classpath at launch. Plugins perform functionality such as transforming class bytes as they are loaded, allowing for bytecode manipulation without requiring the static target jar to be modified.

Additionally, any `.jar` files or native dependency files (`.dll`, `.so`, etc) contained in the target jar file will be extracted into a local `dependencies` directory, and injected onto the classpath at runtime.

## Dependency
Jar Spark can be easily added as a dependency through Gradle:

```groovy
repositories {
    maven { url = 'https://maven.gegy1000.net' } 
}

dependencies {
    compile 'com.hrznstudio:jar-spark:{VERSION}'
}
```
... where `{VERSION}` would be replaced with the desired version.

## Usage
Jar Spark's primary use to to launch an external jar file based on given arguments. This is ideal for a modding situation, where you cannot modify the target jar file.

Jar Spark can be downloaded from [GitHub Releases](https://github.com/gegy1000/jar-spark/releases), or alternatively be manually built from sources.

The Spark jar takes 3 optional arguments:
 - `launchJar`: Path to the target jar file to be launched. This can be relative or absolute, and defaults to the current jar file. Unless Jar Spark is bundled, a target jar should be specified.
 - `dependencyDir`: The directory to extract dependencies to. This defaults to `/dependencies`, relative to the current directory.
 - `mainClass`: The fully qualified name of the main class to be invoked by Jar Spark. This defaults to the main class specified in the jar manifest.

An example external invocation of the Spark jar may look like:
```
java -jar jar-spark.jar --launchJar "Equilinox.jar" --mainClass "main.MainApp"
```

## Bundling
Jar Spark can also be bundled within a jar, and launch a main class from itself. This is preferable in the case that you want your jar to support launch plugins natively, and not require it to be launched from an external source.

To bundle Jar Spark, you will want to add it as a dependency to your project. This can be done with Gradle, as mentioned above.

In the case that Jar Spark is bundled, `com.hrznstudio.spark.SparkLauncher` should be configured as your main class, and your jar invoked with the desired `mainClass` argument.

## Creating a Plugin
Two types of plugins are supported: Patch Plugins and Launch Plugins. Patch Plugins are used to apply transformers to classes as they are being loaded, while launch plugins are used for any hackery that may be required before the target jar is launched.

Plugins may be loaded from the `plugins` directory, but are also accepted if already on the current classpath. This is supported through Java 8 `ServiceLoader`s.

First, you will want to set up a workspace with Jar Spark. This can be done with Gradle as mentioned above. You will then want to create a class that implements `com.hrznstudio.spark.patch.IPatchPlugin` or `com.hrznstudio.spark.plugin.ILaunchPlugin` respectively.

To get Jar Spark to pick up these plugins, they need to be visible to the `ServiceLoader`. Services are added to `META-INF/services` under the name of the interface they implement. For example, with a Patch Plugin, a service would be defined with a file `META-INF/services/com.hrznstudio.spark.patch.IPatchPlugin` containing a list of fully qualified class names to your classes implementing the relevant interface (separated by newlines)

If set up correctly, your plugin should be invoked when running from within your development environment, or when added via the `plugins` directory.

## Testing a Plugin
While plugins can be tested by building and placing in the `plugins` folder, they can also be tested from a development environment. 
This can be done by creating a launch configuration targeting `com.hrznstudio.spark.SparkLauncher` with a `--launchJar` specified for the jar that you want to test on. The plugin should be picked up from the classpath.

