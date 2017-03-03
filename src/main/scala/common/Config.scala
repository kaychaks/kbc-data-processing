package common

import java.nio.file.{FileSystems, Path, Paths}

import pureconfig.ConfigConvert

import scala.util.Try

object Config {

  case class InputPath(value: Path) extends AnyVal
  case class PathConfig(inputPath: InputPath, outputPath: Path)
  case class DomainConfig(chat: PathConfig, faq: PathConfig)
  case class AdhocConfig(inputs: List[InputPath], outputPath: Path)
  case class AppConfig(associatekbc: PathConfig,
                       domain: DomainConfig,
                       adhoc: AdhocConfig)

  private implicit val inpathConvert: ConfigConvert[InputPath] =
    ConfigConvert.fromStringConvertTry[InputPath](
      s =>
        Try(
          InputPath(
            Paths.get(Config.getClass.getClassLoader.getResource(s).getPath)))
          .orElse(Try(InputPath(FileSystems.getDefault.getPath(s)))),
      _.value.toString
    )

  private implicit val pathConvert: ConfigConvert[Path] =
    ConfigConvert.fromStringConvertTry[Path](
      s => Try(FileSystems.getDefault.getPath(s)),
      _.toString)

  lazy val conf: AppConfig = pureconfig.loadConfigOrThrow[AppConfig]

}
