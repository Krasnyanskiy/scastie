import com.typesafe.sbtscalariform.ScalariformPlugin._
import scala.Predef._
import scalariform.formatter.preferences._
import sbt._
import sbt.Keys._

object DefaultSettings {
  val sxrModule = "org.scala-tools.sxr" % "sxr" % "0.2.8-SNAPSHOT"
  def apply: Seq[Setting[_]] = scalariformSettings ++ Seq(
    resolvers += Resolver.url("olegych-repo",
      url("https://bitbucket.org/olegych/mvn/raw/default/ivy2/"))(Resolver.ivyStylePatterns)
    , resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
    , addSupportedCompilerPlugin(sxrModule)(pluginVersions)
    , addSupportedCompilerPlugin("com.foursquare.lint" % "linter" % "0.1-SNAPSHOT")(pluginVersions)
    , scalacOptions
    , ScalariformKeys.preferences := FormattingPreferences().
        setPreference(AlignParameters, true).
        setPreference(AlignSingleLineCaseStatements, true).
        setPreference(CompactControlReadability, true).
        setPreference(PreserveDanglingCloseParenthesis, true).
        setPreference(DoubleIndentClassDeclaration, true)
    , traceLevel := 1000
    , crossPaths := false
  )


  def scalacOptions: Project.Setting[Task[Seq[String]]] = {
    Keys.scalacOptions <++= (scalaSource in Compile, baseDirectory, scalaVersion) map {
      (scalaSource, baseDirectory, scalaVersion) =>
        val sxrOptions = if (sxrSupported(scalaVersion)) {
          Seq(
            "-P:sxr:base-directory:" + baseDirectory.getAbsolutePath,
            "-P:sxr:link-file:" + (baseDirectory / "sxr.links").getAbsolutePath)
        } else {
          Nil
        }
        val featureOptions = if (is210(scalaVersion)) Seq("-feature") else Nil
        Seq("-deprecation", "-unchecked", "-Ywarn-all", "-Xcheckinit") ++ sxrOptions ++ featureOptions
    }
  }


  def is210(scalaVersion: String) = scalaVersion.startsWith("2.10")

  def sxrSupported(scalaVersion: String) = pluginVersions.isDefinedAt(scalaVersion, sxrModule)

  val pluginVersions: PartialFunction[(String, ModuleID), ModuleID] = {
    case ("2.9.2", module) => module.cross(CrossVersion.full)
    case ("2.10.0", module) => module.cross(CrossVersion.binary)
  }

  def addSupportedCompilerPlugin(module: ModuleID)
                                (version: PartialFunction[(String, ModuleID), ModuleID]): Project.Setting[Seq[ModuleID]] =
    libraryDependencies <++= (scalaVersion) { scalaVersion =>
      version.lift(scalaVersion, module).map(compilerPlugin(_)).toList
    }
}

