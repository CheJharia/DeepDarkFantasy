package com.thoughtworks.DDF.Bool

import com.thoughtworks.DDF.Arrow.{ArrowLoss, EvalArrow}
import com.thoughtworks.DDF.Combinators.{Comb, EvalComb}
import com.thoughtworks.DDF.{CommutativeMonoid, CommutativeMonoidUnit, Eval, EvalCase, Loss, LossCase}

import scalaz.Leibniz._

trait EvalBool extends BoolRepr[Loss, Eval] with EvalArrow {
  object BoolEC extends EvalCase[Boolean] {
    override type ret = Boolean
  }

  override def litB: Boolean => Eval[Boolean] = b => new Eval[Boolean] {
    override def eca: ec.ret = b

    override def eval: Boolean = b

    override val ec: EvalCase.Aux[Boolean, Boolean] = BoolEC

    override val loss: Loss[Boolean] = BoolInfo
  }

  def beval: Eval[Boolean] => Boolean = x => witness(x.ec.unique(BoolEC))(x.eca)

  private def comb: Comb[Loss, Eval] = EvalComb.apply

  override def ite[A](implicit ai: Loss[A]): Eval[Boolean => A => A => A] =
    arrowEval[Boolean, A => A => A, Unit, ArrowLoss[A, ArrowLoss[A, ai.loss]]](b =>
      (if(beval(b))
        comb.K[A, A](ai, ai) else
        app(comb.C[A, A, A](ai, ai, ai))(comb.K[A, A](ai, ai)), _ => ()))(BoolInfo, arrowInfo(ai, arrowInfo(ai, ai)))

  override implicit def BoolInfo: Loss.Aux[Boolean, Unit] = new Loss[Boolean] {
    override def convert = litB

    override def m: CommutativeMonoid[Unit] = CommutativeMonoidUnit.apply

    override def lca: lc.ret = ()

    override val lc: LossCase.Aux[Boolean, Unit] = new LossCase[Boolean] {
      override type ret = Unit
    }

    override type ret = Unit

    override def update(x: Boolean)(rate: Double)(l: loss): Boolean = x
  }
}

object EvalBool {
  implicit def apply = new EvalBool {}
}
