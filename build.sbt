// ---------------------------------------------------------------------------
// Basic settings

name := "mc-random-teleport"
version := "0.5.1"
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

resolvers += "Spigot" at "https://hub.spigotmc.org/nexus/content/groups/public"

libraryDependencies ++= Seq(
  "org.bukkit"   % "bukkit"          % "1.11.2-R0.1-SNAPSHOT" % "provided",
  "org.clapper" %% "mc-scala-plugin" % "0.2.1"                % "provided",
  "org.clapper" %% "grizzled-scala"  % "4.2.0"                % "provided"
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
