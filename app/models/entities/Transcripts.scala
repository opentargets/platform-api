package models.entities

import play.api.libs.json.Reads.*
import play.api.libs.json.*
import slick.jdbc.GetResult
import utils.OTLogging
import utils.db.DbJsonParser.fromPositionedResult

case class Flag(
    label: Option[String],
    value: Option[String]
)

case class Exon(
    exonId: String,
    chromosome: String,
    start: Int,
    end: Int,
    strand: Strand
)

case class Transcript(
    transcriptId: String,
    biotype: String,
    proteinId: Option[String],
    uniprotSwissprotIds: Seq[String],
    uniprotTremblIds: Seq[String],
    uniprotIsoformIds: Seq[String],
    alphafoldIds: Seq[String],
    isEnsemblCanonical: Boolean,
    chromosome: String,
    start: Int,
    end: Int,
    strand: Strand,
    transcriptionStartSite: Int,
    flags: Seq[Flag],
    exons: Seq[Exon]
)

case class Transcripts(
    count: Long,
    rows: IndexedSeq[Transcript]
)

object Transcripts extends OTLogging {
  def empty: Transcripts = Transcripts(0, IndexedSeq.empty)
  implicit val getTranscripts: GetResult[Transcripts] = GetResult(fromPositionedResult[Transcripts])
  implicit val transcriptsImp: OFormat[Transcripts] = Json.format[Transcripts]
  implicit val transcriptImp: OFormat[Transcript] = Json.format[Transcript]
  implicit val flagImp: OFormat[Flag] = Json.format[Flag]
  implicit val exonImp: OFormat[Exon] = Json.format[Exon]
  implicit val strandWrites: Writes[Strand] = Writes(s => JsString(s.value))
  implicit val strandReads: Reads[Strand] = Reads {
    case JsString(s) =>
      Strand.values.find(_.value == s) match {
        case Some(v) => JsSuccess(v)
        case None    => JsError(s"Invalid Strand value: $s")
      }
    case _ => JsError("Strand must be a string")
  }
}
