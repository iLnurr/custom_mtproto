import Settings._
import sbt.Keys.version

val tsecV = "0.2.1"
lazy val fs2_mtproto = (project in file("."))
  .settings(commonSettings2_13())
  .settings(
    name    := "custom_mtproto",
    version := "0.1",
    resolvers += "Sonatype Public".at(
      "https://oss.sonatype.org/content/groups/public/"
    ),
    libraryDependencies ++= Seq(
      "co.fs2"             %% "fs2-core"           % "2.4.2",
      "co.fs2"             %% "fs2-io"             % "2.4.2",
      "org.scodec"         %% "scodec-core"        % "1.11.7",
      "org.scodec"         %% "scodec-bits"        % "1.1.17",
      "org.scodec"         %% "scodec-stream"      % "2.0.0",
      "dev.zio"            %% "zio"                % "1.0.0-RC21-2",
      "dev.zio"            %% "zio-interop-cats"   % "2.1.4.0-RC17",
      "io.github.jmcardon" %% "tsec-common"        % tsecV,
      "io.github.jmcardon" %% "tsec-password"      % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-jca"    % tsecV,
      "io.github.jmcardon" %% "tsec-cipher-bouncy" % tsecV,
      "io.github.jmcardon" %% "tsec-mac"           % tsecV,
      "io.github.jmcardon" %% "tsec-signatures"    % tsecV,
      "io.github.jmcardon" %% "tsec-hash-jca"      % tsecV,
      "io.github.jmcardon" %% "tsec-hash-bouncy"   % tsecV,
      "io.github.jmcardon" %% "tsec-jwt-mac"       % tsecV,
      "io.github.jmcardon" %% "tsec-jwt-sig"       % tsecV,
      "io.github.jmcardon" %% "tsec-http4s"        % tsecV,
      "org.scalatest"      %% "scalatest"          % "3.2.0" % Test
    )
  )
