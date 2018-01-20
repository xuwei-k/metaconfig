package metaconfig

import metaconfig.Conf._
import metaconfig.internal.Macros
import org.scalatest.FunSuite

case class AllTheAnnotations(
    @SettingDescription("descriptioon")
    @ExampleValue("value")
    @ExampleValue("value2")
    @ExtraSettingName("extraName")
    @ExtraSettingName("extraName2")
    @DeprecatedSettingName("deprecatedName")
    @DeprecatedSettingName("deprecatedName2")
    @SinceVersion("2.1")
    @SettingDescription("Description")
    @DeprecatedSetting("Use newFeature instead", "2.1")
    setting: Int = 2,
    setting2: String
)

object AllTheAnnotations {
  implicit lazy val fields: Fields[AllTheAnnotations] =
    Macros.deriveFields[AllTheAnnotations]
  implicit lazy val decoder: ConfDecoder[AllTheAnnotations] =
    new ConfDecoder[AllTheAnnotations] {
      override def read(conf: Conf): Configured[AllTheAnnotations] = {
        fields.fields.map { field =>
          pprint.log(field)
        }
        ConfError.empty.notOk
      }
    }
}

class MacrosTest extends FunSuite {

  def checkError(name: String, obj: Conf, expected: String): Unit = {
    test(name) {
      ConfDecoder.decode[AllTheAnnotations](obj) match {
        case Configured.NotOk(err) =>
          assert(expected == err.toString)
        case Configured.Ok(obtained) =>
          fail(s"Expected error, obtained=$obtained")
      }
    }
  }

  private val setting = "setting" -> Num(42)
  private val setting2 = "setting2" -> Str("42")

  checkError(
    "typo",
    Obj(setting, "setting3" -> Str("42")),
    ""
  )

  test("ConfDecoder[T] ok") {
    val obj = Obj("setting" -> Num(42), "setting2" -> Str("42"))
    val expected = AllTheAnnotations(42, "42")
    val obtained = ConfDecoder.decode[AllTheAnnotations](obj).get
    pprint.log(obtained)
    assert(obtained == expected)
  }

  test("Settings[T]") {
    val List(s1, s2) = Settings[AllTheAnnotations].settings
    assert(s1.name == "setting")
    assert(
      s1.extraNames == List(
        "extraName",
        "extraName2"
      )
    )
    assert(
      s1.deprecatedNames ==
        List("deprecatedName", "deprecatedName2")
    )
    assert(
      s1.exampleValues ==
        List("value", "value2")
    )
    assert(s1.description.contains("descriptioon"))
    assert(s1.sinceVersion.contains("2.1"))
    assert(
      s1.deprecated.contains(DeprecatedSetting("Use newFeature instead", "2.1"))
    )

    assert(s2.name == "setting2")
    assert(s2.annotations.isEmpty)
  }
}