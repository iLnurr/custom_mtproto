import Settings._
import sbt.Keys.version

lazy val fs2_mtproto = (project in file("."))
  .settings(commonSettings2_13())
  .settings(
    name    := "custom_mtproto",
    version := "0.1",
    resolvers += "Sonatype Public".at(
      "https://oss.sonatype.org/content/groups/public/"
    ),
    libraryDependencies ++= Seq(
      "co.fs2"     %% "fs2-core"      % "2.4.2",
      "co.fs2"     %% "fs2-io"        % "2.4.2",
      "org.scodec" %% "scodec-core"   % "1.11.7",
      "org.scodec" %% "scodec-bits"   % "1.1.17",
      "org.scodec" %% "scodec-stream" % "2.0.0"
    )
  )
