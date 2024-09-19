package clickhouse

import scala.concurrent.ExecutionContext
import slick.relational.RelationalCapabilities
import slick.sql.SqlCapabilities
import slick.basic.Capability
import slick.compiler.CompilerState
import slick.jdbc.meta._
import slick.lifted.{Query, Rep, _}
import FunctionSymbolExtensionMethods._
import slick.ast.Library.SqlAggregateFunction
import slick.ast.ScalaBaseType.longType
import slick.ast.{BaseTypedType, FieldSymbol, Insert, Library, Node, TypedType}
import slick.jdbc.{JdbcActionComponent, JdbcCapabilities, JdbcModelBuilder, JdbcProfile}

import scala.language.implicitConversions

object CHLibrary {
  val Uniq = new SqlAggregateFunction("uniq")
  val Any = new SqlAggregateFunction("any")
}

trait CHColumnExtensionMethods[B1, P1] extends Any with ExtensionMethods[B1, P1] {
  def uniq: Rep[Long] = CHLibrary.Uniq.column[Long](n)
}

final class BaseCHColumnExtensionMethods[P1](val c: Rep[P1])
    extends AnyVal
    with CHColumnExtensionMethods[P1, P1]
    with BaseExtensionMethods[P1]

final class OptionCHColumnExtensionMethods[B1](val c: Rep[Option[B1]])
    extends AnyVal
    with CHColumnExtensionMethods[B1, Option[B1]]
    with OptionExtensionMethods[B1]

/** Extension methods for Queries of a single column */
final class CHSingleColumnQueryExtensionMethods[B1, P1, C[_]](val q: Query[Rep[P1], ?, C])
    extends AnyVal {
  type OptionTM = TypedType[Option[B1]]

  def uniq(implicit tm: OptionTM): Rep[Option[Long]] = CHLibrary.Uniq.column[Option[Long]](q.toNode)

  def any(implicit tm: OptionTM): Rep[Option[B1]] = CHLibrary.Any.column[Option[B1]](q.toNode)
}

trait ClickHouseProfile
    extends JdbcProfile
    with JdbcActionComponent.MultipleRowsPerStatementSupport {
  override protected def computeCapabilities: Set[Capability] =
    (super.computeCapabilities
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
      - JdbcCapabilities.supportsByte)

  class ModelBuilder(mTables: Seq[MTable], ignoreInvalidDefaults: Boolean)(implicit
      ec: ExecutionContext
  ) extends JdbcModelBuilder(mTables, ignoreInvalidDefaults)

  override val columnTypes = new JdbcTypes

  override def createQueryBuilder(n: Node, state: CompilerState): QueryBuilderCH =
    new QueryBuilderCH(n, state)

  override def createUpsertBuilder(node: Insert): super.InsertBuilder = new UpsertBuilderCH(node)

  override def createInsertBuilder(node: Insert): super.InsertBuilder = new InsertBuilderCH(node)

  override def createTableDDLBuilder(table: Table[?]): TableDDLBuilderCH = new TableDDLBuilderCH(
    table
  )

  override def createColumnDDLBuilder(column: FieldSymbol, table: Table[?]): ColumnDDLBuilderCH =
    new ColumnDDLBuilderCH(column)

  override def createInsertActionExtensionMethods[T](
      compiled: CompiledInsert
  ): InsertActionExtensionMethods[T] =
    new CountingInsertActionComposerImplCH[T](compiled)

  class QueryBuilderCH(tree: Node, state: CompilerState) extends super.QueryBuilder(tree, state) {
    // override protected val concatOperator = Some("||")
    override protected val alwaysAliasSubqueries = false
    override protected val supportsLiteralGroupBy = true
    override protected val quotedJdbcFns: Some[Nil.type] = Some(Nil)
  }

  class UpsertBuilderCH(ins: Insert) extends super.InsertBuilder(ins)

  class InsertBuilderCH(ins: Insert) extends super.InsertBuilder(ins)

  class TableDDLBuilderCH(table: Table[?]) extends super.TableDDLBuilder(table)

  class ColumnDDLBuilderCH(column: FieldSymbol) extends super.ColumnDDLBuilder(column)

  class CountingInsertActionComposerImplCH[U](compiled: CompiledInsert)
      extends super.CountingInsertActionComposerImpl[U](compiled)

  trait ClickHouseAPI extends JdbcAPI {
    // nice page to read about extending profile apis
    // https://virtuslab.com/blog/smooth-operator-with-slick-3/

    implicit def chSingleColumnQueryExtensionMethods[B1: BaseTypedType, C[_]](
        q: Query[Rep[B1], ?, C]
    ): CHSingleColumnQueryExtensionMethods[B1, B1, C] =
      new CHSingleColumnQueryExtensionMethods[B1, B1, C](q)

    implicit def chSingleOptionColumnQueryExtensionMethods[B1: BaseTypedType, C[_]](
        q: Query[Rep[Option[B1]], ?, C]
    ): CHSingleColumnQueryExtensionMethods[B1, Option[B1], C] =
      new CHSingleColumnQueryExtensionMethods[B1, Option[B1], C](q)

    implicit def chColumnExtensionMethods[B1](c: Rep[B1])(implicit
        tm: BaseTypedType[B1] /* with NumericTypedType*/
    ): BaseCHColumnExtensionMethods[B1] = new BaseCHColumnExtensionMethods[B1](c)

    implicit def chOptionColumnExtensionMethods[B1](c: Rep[Option[B1]])(implicit
        tm: BaseTypedType[B1] /* with NumericTypedType*/
    ): OptionCHColumnExtensionMethods[B1] = new OptionCHColumnExtensionMethods[B1](c)
  }

  override val api: ClickHouseAPI = new ClickHouseAPI {}
}

object ClickHouseProfile extends ClickHouseProfile
