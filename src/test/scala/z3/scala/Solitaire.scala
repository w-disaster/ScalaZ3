package z3.scala
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import z3.scala.dsl.{And, *, given}

import scala.language.implicitConversions

object HeuleEncodings:

  private def atMostOneNp(v: Seq[Tree[BoolSort]]): Seq[Tree[BoolSort]] =
    val r = for
      p <- v.combinations(2)
    yield Not(And(p.head, p.tail.head))
    r.toSeq

  def atLeastOne(v: Seq[Tree[BoolSort]]): Or[BoolSort] = Or(v: _*)

  def atMostOne(v: Seq[Tree[BoolSort]]): Tree[BoolSort] =
    if v.size <= 4 then
      And(atMostOneNp(v): _*)
    else
      val y = BoolVar()
      And(
        And(atMostOneNp(v.drop(3) ++ Seq(y)): _*),
        And(atMostOne(v.take(3) ++ Seq(Not(y))))
      )

  def exactlyOne(v: Seq[Tree[BoolSort]]): Tree[BoolSort] =
    And(atMostOne(v), atLeastOne(v))


class Solitaire extends AnyFunSuite with Matchers {
  import dsl._

  test("Solitaire") {

    val width = 5
    val height = 5
    val offsets: Seq[(Int, Int)] = Seq((-3, 0), (3, 0), (2, -2), (2, 2),
      (-2, 2), (-2, -2), (0, 3), (0, -3))
    val ctx = new Z3Context("MODEL" -> true)

    def render(solution: Seq[(Int, Int)], width: Int, height: Int): String =
      val rows =
        for y <- 0 until height
            row = for x <- 0 until width
                      number = solution.indexOf((x, y)) + 1
            yield if number > 0 then "%-2d ".format(number) else "X  "
        yield row.mkString
      rows.mkString("\n")

    def computePrevPos(r: Int, c: Int): Seq[(Int, Int)] =
      offsets
        .map((or, oc) => (or + r, oc + c))
        .filter((or, oc) => or >= 0 && or < height &&
          oc >= 0 && oc < width)

    /* Declaring column variables */
    val cells = (0 until height).map(_ => (0 until width).map(_ => IntVar()))

    /* Constraints */

    // Each placing is on a different cell
    val diffConstraint = Distinct(cells.flatten: _*)
    // The numbers are between 0 to (width * size)
    val boundsConstraint =
      for
        c <- 0 until width
        r <- 0 until height
      yield cells(r)(c) > 0 && cells(r)(c) <= width * height

    /*
      Given a cell of position (r, c) and its value i, there must be
      exactly one position between the previous possible ones with value i-1.
      There's an exception on the central position: its value is 1 and there's no 0.
    */
    val prevConstraint =
      for
        c <- 0 until width
        r <- 0 until height
        if !(c == width / 2 && r == height / 2)
      yield
        HeuleEncodings.exactlyOne(computePrevPos(r, c)
          .map((nr, nc) => Eq(cells(nr)(nc), cells(r)(c) - 1)))

    // The first placing must be done on the central position
    val firstPosCnstr = Eq(cells(height / 2)(width / 2), 1)

    // Solver
    val solver = ctx.mkSolver()

    // Add constraints
    solver.assertCnstr(diffConstraint)
    boundsConstraint foreach (solver.assertCnstr(_))
    solver.assertCnstr(firstPosCnstr)
    prevConstraint foreach solver.assertCnstr

    // Check if it's SAT
    val (testModels, renderModels) = solver.checkAndGetAllModels().duplicate

    // Must be SAT with 352 possible solutions
    solver.check() should equal (Some(true))
    testModels.size should equal (352)

    // Print all the solutions
    /* renderModels.foreach(m =>
      val eval = for
        c <- 0 until width
        r <- 0 until height
      yield (r, c, m.evalAs[Int](cells(r)(c).ast(ctx)).get)
      println(render(eval.sortBy((_, _, e) => e).map((r, c, _) => (r, c)), width, height) + '\n')
    ) */
    ctx.delete()
  }
}
