name := "zio-advanced"
version := "0.1"
scalaVersion := "2.13.8"

val zioVersion = "2.0.2"

libraryDependencies ++= Seq(
  "dev.zio"       %% "zio"            % zioVersion,
  "dev.zio"       %% "zio-streams"    % zioVersion,
  "dev.zio"       %% "zio-test"       % zioVersion,
  "dev.zio"       %% "zio-test"       % zioVersion % "test",
  "dev.zio"       %% "zio-test-sbt"   % zioVersion % "test",
  "dev.zio"       %% "zio-json"       % "0.3.0-RC11",
  "io.d11"        %% "zhttp"          % "2.0.0-RC10",
  "io.getquill"   %% "quill-zio"      % "4.3.0",
  "io.getquill"   %% "quill-jdbc-zio" % "4.3.0",
  "com.h2database" % "h2"             % "2.1.214"
)