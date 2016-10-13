package com.thoughtworks.DDF.Product

import com.thoughtworks.DDF.Arrow.{ArrowLoss, EvalArrow}
import com.thoughtworks.DDF.{CommutativeMonoid, Eval, EvalCase, Loss, LossCase}

import scalaz.Leibniz._

trait EvalProduct extends ProductRepr[Loss, Eval] with EvalArrow {
  trait ProductLCRet[A, B] {
    def zeroth: Loss[A]

    def first: Loss[B]
  }

  case class ProductEC[A, B]() extends EvalCase[(A, B)] {
    override type ret = (Eval[A], Eval[B])
  }

  case class ProductLC[A, B]() extends LossCase[(A, B)] {
    override type ret = ProductLCRet[A, B]
  }

  def peval[A, B](ab: Eval[(A, B)]): (Eval[A], Eval[B]) = witness(ab.ec.unique(ProductEC[A, B]()))(ab.eca)

  def productEval[A, B](a: Eval[A], b: Eval[B])(implicit al: Loss[A], bl: Loss[B]) = new Eval[(A, B)] {
    override val loss: Loss[(A, B)] = productInfo(al, bl)

    override def eval: (A, B) = (a.eval, b.eval)

    override val ec: EvalCase.Aux[(A, B), (Eval[A], Eval[B])] = ProductEC()

    override def eca: ec.ret = (a, b)
  }

  override def zeroth[A, B](implicit at: Loss[A], bt: Loss[B]): Eval[((A, B)) => A] =
    arrowEval[(A, B), A, (at.loss, bt.loss), at.loss](p => (peval(p)._1, al => (al, bt.m.zero)))(productInfo(at, bt), at)

  override def first[A, B](implicit at: Loss[A], bt: Loss[B]): Eval[((A, B)) => B] =
    arrowEval[(A, B), B, (at.loss, bt.loss), bt.loss](p => (peval(p)._2, bl => (at.m.zero, bl)))(productInfo(at, bt), bt)

  override def mkProduct[A, B](implicit ai: Loss[A], bi: Loss[B]): Eval[(A) => (B) => (A, B)] =
    arrowEval[A, B => (A, B), ai.loss, ArrowLoss[B, (ai.loss, bi.loss)]](a =>
      (arrowEval[B, (A, B), bi.loss, (ai.loss, bi.loss)](b =>
        (productEval(a, b), _._2))(bi, productInfo(ai, bi)),
        _.mapReduce(_ => _._1)(ai.m)))(
      ai, arrowInfo(bi, productInfo(ai, bi)))

  override implicit def productInfo[A, B](implicit ai: Loss[A], bi: Loss[B]): Loss.Aux[(A, B), (ai.loss, bi.loss)] =
    new Loss[(A, B)] {
      override def convert: ((A, B)) => Eval[(A, B)] = p => productEval(ai.convert(p._1), bi.convert(p._2))

      override val lc: LossCase.Aux[(A, B), ProductLCRet[A, B]] = ProductLC[A, B]()

      override def lca: lc.ret = new ProductLCRet[A, B] {
        override def zeroth: Loss[A] = ai

        override def first: Loss[B] = bi
      }

      override type ret = (ai.loss, bi.loss)

      override def m: CommutativeMonoid[(ai.loss, bi.loss)] = new CommutativeMonoid[(ai.loss, bi.loss)] {

        override def zero: (ai.loss, bi.loss) = (ai.m.zero, bi.m.zero)

        override def append(f1: (ai.loss, bi.loss), f2: => (ai.loss, bi.loss)): (ai.loss, bi.loss) =
          (ai.m.append(f1._1, f2._1), bi.m.append(f1._2, f2._2))
      }

      override def update(x: (A, B))(rate: Double)(l: (ai.loss, bi.loss)): (A, B) =
        (ai.update(x._1)(rate)(l._1), bi.update(x._2)(rate)(l._2))
    }

  override def productZerothInfo[A, B]: Loss[(A, B)] => Loss[A] = p => witness(p.lc.unique(ProductLC[A, B]()))(p.lca).zeroth

  override def productFirstInfo[A, B]: Loss[(A, B)] => Loss[B] = p => witness(p.lc.unique(ProductLC[A, B]()))(p.lca).first

  def curry[A, B, C](implicit ai: Loss[A], bi: Loss[B], ci: Loss[C]): Eval[(((A, B)) => C) => A => B => C] =
    arrowEval[((A, B)) => C, A => B => C, ArrowLoss[(A, B), ci.loss], ArrowLoss[A, ArrowLoss[B, ci.loss]]](abc =>
      (arrowEval[A, B => C, ai.loss, ArrowLoss[B, ci.loss]](a =>
        (arrowEval[B, C, bi.loss, ci.loss](b => {
          val c = aeval(abc).forward(productEval(a, b))
          (c.eb, l => c.backward(l)._2)
        })(bi, ci), l => l.mapReduce(b => l => {
          val c = aeval(abc).forward(productEval(a, b))
          c.backward(l)._1
        })(ai.m)))
        (ai, arrowInfo(bi, ci)),
        _.mapReduce(a => _.mapReduce(b => l => ArrowLoss(productEval(a, b))(l))(
          arrowInfo(productInfo(ai, bi), ci).m))(
          arrowInfo(productInfo(ai, bi), ci).m)))

  def uncurry[A, B, C](implicit ai: Loss[A], bi: Loss[B], ci: Loss[C]): Eval[(A => B => C) => ((A, B)) => C] =
    arrowEval[A => B => C, ((A, B)) => C, ArrowLoss[A, ArrowLoss[B, ci.loss]], ArrowLoss[(A, B), ci.loss]](abc =>
      (arrowEval[(A, B), C, (ai.loss, bi.loss), ci.loss](ab => {
        val bc = aeval(abc).forward(peval(ab)._1)
        val c = aeval(bc.eb).forward(peval(ab)._2)
        (c.eb, l => (bc.backward(ArrowLoss(peval(ab)._2)(l)), c.backward(l)))
      })(productInfo(ai, bi), ci),
        _.mapReduce(p => l => ArrowLoss(peval(p)._1)(ArrowLoss(peval(p)._2)(l)))(arrowInfo(ai, arrowInfo(bi, ci)).m)))
}
