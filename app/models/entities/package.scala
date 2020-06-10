package models

package object entities {
  sealed trait SortEntity {
    val name: String
  }
  object SortEntity {
    case object ByTarget extends SortEntity {
      val name: String = "approvedSymbol.keyword"
    }
    case object ByDisease extends SortEntity {
      val name: String = "label.keyword"
    }
    case object ByDrug extends SortEntity {
      val name: String = "prefName.keyword"
    }

    def TARGET = ByTarget
    def DISEASE  = ByDisease
    def DRUG  = ByDrug
  }
}
