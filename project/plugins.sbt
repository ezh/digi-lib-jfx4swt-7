resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo",
  "oss sonatype" at "https://oss.sonatype.org/content/groups/public/",
  "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/"
)

addSbtPlugin("org.digimead" % "sbt-application" % "0.2.0.0")

addSbtPlugin("org.digimead" % "sbt-osgi-manager" % "0.3.0.1-SNAPSHOT")
