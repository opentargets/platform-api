package models.entities

import org.scalatestplus.play.PlaySpec
import play.api.Logging

class DrugTest extends PlaySpec with Logging {

  "Mechanism of action rows" should {
       "have uniqueActionTypes" in {
          // given
         val input = Seq(
           MechanismOfActionRaw(Seq("id1, id2"), None, "moa", Some("testAction"), None, None, None),
           MechanismOfActionRaw(Seq("id1, id2"), None, "moa", Some("testAction"), None, None, None),
         )
          // when
         val results = Drug.mechanismOfActionRaw2MechanismOfAction(input)
          // then
         results.uniqueActionTypes.length must be(1)
       }
  }
}
