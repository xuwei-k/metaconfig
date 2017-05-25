package metaconfig

import metaconfig.Configured.NotOk
import metaconfig.Configured.Ok
import org.scalameta.logger
import org.scalatest.FunSuite

class DerivationTest extends FunSuite {
  type Result[T] = Either[Throwable, T]

  @DeriveConfDecoder
  case class Inner(nest: Int)

  @DeriveConfDecoder
  case class Outer(i: Int, inner: Inner) {
    implicit val innerReader: ConfDecoder[Inner] = inner.reader
  }

  @DeriveConfDecoder
  case class OuterRecurse(i: Int, @metaconfig.Recurse inner: Inner)

  @DeriveConfDecoder
  case class Bar(i: Int, b: Boolean, s: String)

  @DeriveConfDecoder
  case class HasList(i: Seq[Int])

  @DeriveConfDecoder
  case class HasMap(i: Map[String, Int])

  val b = Bar(0, true, "str")
  test("invalid field") {
    assert(
      b.reader
        .read(Conf.Obj("is" -> Conf.Num(2), "var" -> Conf.Num(3)))
        .isNotOk)
  }

  test("read OK") {
    assert(
      b.reader.read(Conf.Obj("i" -> Conf.Num(2))) ==
        Configured.Ok(b.copy(i = 2)))
    assert(
      b.reader.read(Conf.Obj("s" -> Conf.Str("str"))) ==
        Configured.Ok(b.copy(s = "str")))
    assert(
      b.reader.read(Conf.Obj("b" -> Conf.Bool(true))) ==
        Configured.Ok(b.copy(b = true)))
    assert(
      b.reader.read(
        Conf.Obj(
          "i" -> Conf.Num(3),
          "b" -> Conf.Bool(true),
          "s" -> Conf.Str("rand")
        )) == Configured.Ok(b.copy(i = 3, s = "rand", b = true)))
  }
  test("unexpected type") {
    val msg =
      "Error reading field 'i'. Expected argument of type int. Obtained value '\"str\"' of type String."
    assert(b.reader.read(Conf.Obj("i" -> Conf.Str("str"))).isNotOk)
  }

  test("write OK") {
    assert(
      b.fields == Map(
        "i" -> 0,
        "b" -> true,
        "s" -> "str"
      ))
  }
  test("nested OK") {
    val m = Conf.Obj(
      "i" -> Conf.Num(4),
      "inner" -> Conf.Obj(
        "nest" -> Conf.Num(5)
      )
    )
    val Configured.Ok(n) = OuterRecurse(2, Inner(3)).reader.read(m)
    val Configured.Ok(o) = Outer(2, Inner(3)).reader.read(m)
    assert(o == Outer(4, Inner(5)))
    assert(n == OuterRecurse(4, Inner(5)))
  }

  test("Seq") {
    val lst = HasList(List(1, 2, 3))
    assert(
      lst.reader
        .read(Conf.Obj("i" -> Conf.Lst(Conf.Num(666), Conf.Num(777)))) ==
        Configured.Ok(HasList(Seq(666, 777))))
  }

  test("Conf.Obj") {
    val lst = HasMap(Map("1" -> 2))
    assert(
      lst.reader.read(Conf.Obj("i" -> Conf.Obj("666" -> Conf.Num(777)))) ==
        Configured.Ok(HasMap(Map("666" -> 777))))
  }

  case object Kase
  @DeriveConfDecoder
  case class Ob(kase: Kase.type) {
    implicit val KaseReader: ConfDecoder[Kase.type] =
      ConfDecoder.stringConfDecoder.flatMap { x =>
        ???
      }
  }

  test("Runtime ???") {
    val m = Conf.Obj(
      "kase" -> Conf.Str("string")
    )
    intercept[NotImplementedError] { Ob(Kase).reader.read(m).isNotOk }
  }

  @DeriveConfDecoder
  case class HasExtra(@ExtraName("b") @metaconfig.ExtraName("c") a: Int)
  test("@ExtraName") {
    val x = HasExtra(1)
    val Configured.Ok(HasExtra(2)) =
      x.reader.read(Conf.Obj("b" -> Conf.Num(2)))
    val Configured.Ok(HasExtra(3)) =
      x.reader.read(Conf.Obj("c" -> Conf.Num(3)))
    val Configured.NotOk(_) = x.reader.read(Conf.Obj("d" -> Conf.Num(3)))
  }

  @DeriveConfDecoder
  case class HasTypeParam[T: ConfDecoder](x: T)

  test("type param") {
    val int: HasTypeParam[Int] = HasTypeParam(1)
    implicit val confDecoderDecoderHasTypeParam =
      ConfDecoder.instance[HasTypeParam[Int]] {
        case Conf.Str("abb") => Ok(HasTypeParam(123))
      }
    val tparamInt: HasTypeParam[HasTypeParam[Int]] = HasTypeParam(int)

    val Configured.Ok(HasTypeParam(2)) =
      int.reader.read(Conf.Obj("x" -> Conf.Num(2)))
    val Configured.Ok(HasTypeParam(HasTypeParam(123))) =
      tparamInt.reader.read(Conf.Obj("x" -> Conf.Str("abb")))
  }

}