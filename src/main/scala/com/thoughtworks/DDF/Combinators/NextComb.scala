package com.thoughtworks.DDF.Combinators

import com.thoughtworks.DDF.Arrow.NextArrow
import com.thoughtworks.DDF.NextBase

trait NextComb[Info[_], Repr[_], Arg] extends
  Comb[Lambda[X => Info[Arg => X]], Lambda[X => Either[Repr[X], Repr[Arg => X]]]] with
  NextBase[Info, Repr, Arg] with
  NextArrow[Info, Repr, Arg] {
  implicit def base: Comb[Info, Repr]

  override def S[A, B, C](implicit ai: Info[Arg => A], bi: Info[Arg => B], ci: Info[Arg => C]) =
    rconv(base.S(base.ArrRngInfo(ai), base.ArrRngInfo(bi), base.ArrRngInfo(ci)))

  override def B[A, B, C](implicit ai: Info[Arg => A], bi: Info[Arg => B], ci: Info[Arg => C]) =
    rconv(base.B(base.ArrRngInfo(ai), base.ArrRngInfo(bi), base.ArrRngInfo(ci)))

  override def C[A, B, C](implicit ai: Info[Arg => A], bi: Info[Arg => B], ci: Info[Arg => C]) =
    rconv(base.C(base.ArrRngInfo(ai), base.ArrRngInfo(bi), base.ArrRngInfo(ci)))

  override def Y[A, B](implicit ai: Info[Arg => A], bi: Info[Arg => B]) =
    rconv(base.Y(base.ArrRngInfo(ai), base.ArrRngInfo(bi)))

  override def K[A, B](implicit ai: Info[Arg => A], bi: Info[Arg => B]) =
    rconv(base.K(base.ArrRngInfo(ai), base.ArrRngInfo(bi)))

  override def I[A](implicit ai: Info[Arg => A]) = rconv(base.I(base.ArrRngInfo(ai)))

  override def W[A, B](implicit ai: Info[Arg => A], bi: Info[Arg => B]) =
    rconv(base.W(base.ArrRngInfo(ai), base.ArrRngInfo(bi)))

  override implicit def ski: SKIRepr[Info, Repr] = base
}

object NextComb {
  implicit def apply[Info[_], Repr[_], Arg](implicit comb: Comb[Info, Repr], arg: Info[Arg]) =
    new NextComb[Info, Repr, Arg] {
      override implicit def base: Comb[Info, Repr] = comb

      override implicit def argi: Info[Arg] = arg
  }
}