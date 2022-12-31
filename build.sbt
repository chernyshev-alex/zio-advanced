name := "zio-advanced"
version := "0.1"
scalaVersion := "2.13.8"

val zioVersion = "2.0.5"
val zioConfigVersion = "3.0.6"
val zioSqlVersion = "0.1.1"

libraryDependencies ++= Seq(
  "dev.zio"       %% "zio"            % zioVersion,
  "dev.zio"       %% "zio-sql-postgres" % zioSqlVersion,
  "dev.zio"       %% "zio-streams"    % zioVersion,
  "dev.zio"       %% "zio-test"       % zioVersion,
  "dev.zio"       %% "zio-test"       % zioVersion % "test",
  "dev.zio"       %% "zio-test-sbt"   % zioVersion % "test",
  "dev.zio"       %% "zio-json"       % "0.4.2",
  "io.d11"        %% "zhttp"          % "2.0.0-RC10",
  "io.d11"        %% "zhttp-test"     % "2.0.0-RC9" % Test,
  //config
  "dev.zio" %% "zio-config" % zioConfigVersion,
  "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
  "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
  /*
  "io.getquill"   %% "quill-zio"      % "4.3.0",
  "io.getquill"   %% "quill-jdbc-zio" % "4.3.0",
  "com.h2database" % "h2"             % "2.1.214",
  */
)

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))

//scalacOptions in Compile in console := Seq(
//  "-Ypartial-unification",
//  "-language:higherKinds",
//  "-language:existentials",
//  "-Yno-adapted-args",
//  "-Xsource:2.13",
//  "-Yrepl-class-based",
//  "-deprecation",
//  "-encoding",
//  "UTF-8",
//  "-explaintypes",
//  "-Yrangepos",
//  "-feature",
//  "-Xfuture",
//  "-unchecked",
//  "-Xlint:_,-type-parameter-shadow",
//  "-Ywarn-numeric-widen",
//  "-Ywarn-value-discard",
//  "-opt-warnings",
//  "-Ywarn-extra-implicit",
//  "-Ywarn-unused:_,imports",
//  "-Ywarn-unused:imports",
//  "-opt:l:inline",
//  "-opt-inline-from:<source>",
//  "-Ypartial-unification",
//  "-Yno-adapted-args",
//  "-Ywarn-inaccessible",
//  "-Ywarn-infer-any",
//  "-Ywarn-nullary-override",
//  "-Ywarn-nullary-unit"
//)