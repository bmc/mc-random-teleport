libraryDependencies += "org.clapper" %% "grizzled-scala" % "4.2.0"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.1")

resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
      url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
          Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
addSbtPlugin("org.clapper" % "sbt-editsource" % "0.8.0")
