// ---------------------------------------------------------------------------
// Basic settings

name := "mc-random-teleport"
version := "0.9.0-SNAPSHOT"
organization := "org.clapper"
licenses := Seq("BSD" -> url("http://software.clapper.org/grizzled-scala/license.html"))
homepage := None
description := "Spigot random teleport Minecraft"
scalaVersion := "2.11.8"

// Incremental compilation performance improvement. See
// http://scala-lang.org/news/2014/04/21/release-notes-2.11.0.html

incOptions := incOptions.value.withNameHashing(true)

ivyScala := ivyScala.value.map { _.copy(overrideScalaVersion = true) }

// ---------------------------------------------------------------------------
// Additional compiler options and plugins

autoCompilerPlugins := true
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// ---------------------------------------------------------------------------
// Deps

resolvers ++= Seq(
  "Spigot" at "https://hub.spigotmc.org/nexus/content/groups/public",
  "vault" at "http://nexus.hc.to/content/repositories/pub_releases"
)

libraryDependencies ++= Seq(
  "org.bukkit"          % "bukkit"          % "1.11.2-R0.1-SNAPSHOT" % Provided,
  "org.clapper"        %% "mc-scala-plugin" % "0.11.0-SNAPSHOT"      % Provided,
  "org.clapper"        %% "grizzled-scala"  % "4.2.0"                % Provided,
  "net.milkbowl.vault"  % "VaultAPI"        % "1.6"                  % Provided
)

// ---------------------------------------------------------------------------
// sbt-editsource

sources in EditSource <++= baseDirectory.map { d =>
  (d / "templates" ** "*.yml").get
}
targetDirectory in EditSource <<= baseDirectory(_ / "src" / "main" / "resources")
flatten in EditSource := true

variables in EditSource <+= name { n => "name" -> n }
variables in EditSource += ("version", version.value)
variables in EditSource += ("date", (new java.util.Date).toString)

// Wire it into the compilation phase.
compile in Compile <<= (compile in Compile) dependsOn (edit in EditSource)

// ---------------------------------------------------------------------------
// sbt-assembly and package settings

// Exclude the same files from the assembly jar.
assemblyMergeStrategy in assembly := { path =>
  val oldStrategy = (assemblyMergeStrategy in assembly).value

  path match {
    case PathList("scala", _ @ _*) =>
      MergeStrategy.discard
    case PathList("org", "clapper", "bukkit", "randomteleport", _ @ _*)  =>
      MergeStrategy.first
    case PathList(f) if f endsWith ".yml" =>
      MergeStrategy.first
    case PathList("rootdoc.txt") /* scala */ =>
      MergeStrategy.discard
    case PathList("library.properties") /* scala */ =>
      MergeStrategy.discard
    case x =>
      oldStrategy(x)
  }
}

// ---------------------------------------------------------------------------
// install task

addCommandAlias("install", ";editsource:clean;compile;assembly;installJar")

val installJar = taskKey[Unit]("Copy the jar to the directory specified by $MC_PLUGIN_DIR")
installJar := {
  import grizzled.file.Implicits.GrizzledFile

  val installDir = Option(System.getenv("MC_PLUGIN_DIR")).getOrElse {
    throw new Exception("MC_PLUGIN_DIR environment variable isn't set.")
  }

  val jar = (assemblyOutputPath in assembly).value
  if (! jar.exists) {
    throw new Exception(s"$jar does not exist.")
  }

  println(s"cp $jar $target")
  jar.copyTo(installDir).get
}
