package models.entities

import org.scalacheck.Gen
import org.scalacheck.Gen.alphaChar
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class DrugTest extends AnyFlatSpecLike with Matchers with ScalaCheckDrivenPropertyChecks {

  val mechanismOfActionRowGenerator: Gen[MechanismOfActionRow] = Gen.listOf(alphaChar).map(_.mkString).map(MechanismOfActionRow(_, None, None, None))
  val mechanismOfActionRowCollectionGenerator: Gen[List[MechanismOfActionRow]] = Gen.listOf(mechanismOfActionRowGenerator)

  "A collection of mechanismOfActionRows" should "support the distinct function on collections" in {
    forAll(mechanismOfActionRowCollectionGenerator) { moars => {
      val withDuplicates = moars :: moars
      withDuplicates.distinct.size == moars.distinct.size
    }
    }
  }

}
