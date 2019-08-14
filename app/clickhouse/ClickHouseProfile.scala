package clickhouse

import scala.concurrent.ExecutionContext
import slick.relational.RelationalCapabilities
import slick.sql.SqlCapabilities
import slick.jdbc._
import slick.basic.Capability
import slick.util.MacroSupport.macroSupportInterpolation
import slick.compiler.CompilerState
import slick.jdbc.meta._
import slick.lifted.{Query, Rep, _}

import scala.language.{higherKinds, implicitConversions}
import slick.ast._
import FunctionSymbolExtensionMethods._
import ScalaBaseType._
import slick.ast.Library.SqlAggregateFunction

object CHLibrary {
  val Uniq = new SqlAggregateFunction("uniq")
  val Any = new SqlAggregateFunction("any")
}

trait CHColumnExtensionMethods[B1, P1] extends Any with ExtensionMethods[B1, P1] {
  def uniq = CHLibrary.Uniq.column[Long](n)
}

final class BaseCHColumnExtensionMethods[P1](val c: Rep[P1]) extends AnyVal with CHColumnExtensionMethods[P1, P1] with BaseExtensionMethods[P1]

final class OptionCHColumnExtensionMethods[B1](val c: Rep[Option[B1]]) extends AnyVal with CHColumnExtensionMethods[B1, Option[B1]] with OptionExtensionMethods[B1]


/** Extension methods for Queries of a single column */
final class CHSingleColumnQueryExtensionMethods[B1, P1, C[_]](val q: Query[Rep[P1], _, C]) extends AnyVal {
  type OptionTM =  TypedType[Option[B1]]
  def uniq(implicit tm: OptionTM) = CHLibrary.Uniq.column[Option[Long]](q.toNode)
  def any(implicit tm: OptionTM) = CHLibrary.Any.column[Option[B1]](q.toNode)
}

trait ClickHouseProfile extends JdbcProfile {
  override protected def computeCapabilities: Set[Capability] = (super.computeCapabilities
    - RelationalCapabilities.foreignKeyActions
    - RelationalCapabilities.functionUser
    - RelationalCapabilities.typeBigDecimal
    - RelationalCapabilities.typeBlob
    - RelationalCapabilities.typeLong
    - RelationalCapabilities.zip
    - SqlCapabilities.sequence
    - JdbcCapabilities.forUpdate
    - JdbcCapabilities.forceInsert
    - JdbcCapabilities.insertOrUpdate
    - JdbcCapabilities.mutable
    - JdbcCapabilities.returnInsertKey
    - JdbcCapabilities.returnInsertOther
    - JdbcCapabilities.supportsByte
    )

  class ModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit ec: ExecutionContext)
    extends JdbcModelBuilder(mTables, ignoreInvalidDefaults)

  override val columnTypes = new JdbcTypes
  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilder = new QueryBuilder(n, state)
  override def createUpsertBuilder(node: Insert): super.InsertBuilder = new UpsertBuilder(node)
  override def createInsertBuilder(node: Insert): super.InsertBuilder = new InsertBuilder(node)
  override def createTableDDLBuilder(table: Table[_]): TableDDLBuilder = new TableDDLBuilder(table)
  override def createColumnDDLBuilder(column: FieldSymbol, table: Table[_]): ColumnDDLBuilder =
    new ColumnDDLBuilder(column)
  override def createInsertActionExtensionMethods[T](compiled: CompiledInsert): InsertActionExtensionMethods[T] =
    new CountingInsertActionComposerImpl[T](compiled)

  class QueryBuilder(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) {
      // override protected val concatOperator = Some("||")
      override protected val alwaysAliasSubqueries = false
      override protected val supportsLiteralGroupBy = true
      override protected val quotedJdbcFns = Some(Nil)

      override protected def buildFetchOffsetClause(fetch: Option[Node], offset: Option[Node]): Unit = {
        (fetch, offset) match {
          case (Some(t), Some(d)) => b"\nlimit $d , $t"
          case (Some(t), None) => b"\nlimit $t"
          case (None, Some(d)) =>
          case _ =>
        }
      }

    override def expr(c: Node, skipParens: Boolean = false): Unit = c match {
      case Library.UCase(ch) => b"upper($ch)"
      case Library.LCase(ch) => b"lower($ch)"
      case Library.User() => b"''"
      case Library.Database() => b"currentDatabase()"
      case _ => super.expr(c, skipParens)
    }
  }

  class UpsertBuilder(ins: Insert) extends super.InsertBuilder(ins)
  class InsertBuilder(ins: Insert) extends super.InsertBuilder(ins)
  class TableDDLBuilder(table: Table[_]) extends super.TableDDLBuilder(table)
  class ColumnDDLBuilder(column: FieldSymbol) extends super.ColumnDDLBuilder(column)
  class CountingInsertActionComposerImpl[U](compiled: CompiledInsert)
    extends super.CountingInsertActionComposerImpl[U](compiled)

  trait ClickHouseAPI extends API {
    // nice page to read about extending profile apis
    // https://virtuslab.com/blog/smooth-operator-with-slick-3/

    implicit def chSingleColumnQueryExtensionMethods[B1 : BaseTypedType, C[_]](q: Query[Rep[B1], _, C]): CHSingleColumnQueryExtensionMethods[B1, B1, C] = new CHSingleColumnQueryExtensionMethods[B1, B1, C](q)
    implicit def chSingleOptionColumnQueryExtensionMethods[B1 : BaseTypedType, C[_]](q: Query[Rep[Option[B1]], _, C]): CHSingleColumnQueryExtensionMethods[B1, Option[B1], C] = new CHSingleColumnQueryExtensionMethods[B1, Option[B1], C](q)
    implicit def chColumnExtensionMethods[B1](c: Rep[B1])(implicit tm: BaseTypedType[B1]/* with NumericTypedType*/): BaseCHColumnExtensionMethods[B1] = new BaseCHColumnExtensionMethods[B1](c)
    implicit def chOptionColumnExtensionMethods[B1](c: Rep[Option[B1]])(implicit tm: BaseTypedType[B1]/* with NumericTypedType*/): OptionCHColumnExtensionMethods[B1] = new OptionCHColumnExtensionMethods[B1](c)
  }

  override val api: ClickHouseAPI = new ClickHouseAPI {}
}

object ClickHouseProfile extends ClickHouseProfile