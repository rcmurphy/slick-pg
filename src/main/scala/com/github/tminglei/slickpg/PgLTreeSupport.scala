package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedResult, JdbcType}

/** simple ltree wrapper */
case class LTree(value: List[String]) {
  override def toString = value.mkString(".")
}

object LTree {
  def apply(value: String): LTree = LTree(value.split("\\.").toList)
}

/**
 * simple ltree support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgLTreeSupport extends ltree.PgLTreeExtensions with utils.PgCommonJdbcTypes with array.PgArrayJdbcTypes { driver: PostgresDriver =>
  import driver.api._

  /// alias
  trait LTreeImplicits extends SimpleLTreeImplicits

  trait SimpleLTreeImplicits {
    implicit val simpleLTreeTypeMapper: JdbcType[LTree] =
      new GenericJdbcType[LTree]("ltree",
        (v) => LTree(v),
        (v) => v.toString,
        hasLiteralForm = false
      )
    implicit val simpleLTreeListTypeMapper: JdbcType[List[LTree]] =
      new AdvancedArrayJdbcType[LTree]("ltree",
        fromString = utils.SimpleArrayUtils.fromString(LTree.apply)(_).map(_.toList).orNull,
        mkString = utils.SimpleArrayUtils.mkString[LTree](_.toString)(_)
      ).to(_.toList)

    implicit def simpleLTreeColumnExtensionMethods(c: Rep[LTree]) = {
        new LTreeColumnExtensionMethods[LTree, LTree](c)
      }
    implicit def simpleLTreeOptionColumnExtensionMethods(c: Rep[Option[LTree]]) = {
        new LTreeColumnExtensionMethods[LTree, Option[LTree]](c)
      }

    implicit def simpleLTreeListColumnExtensionMethods(c: Rep[List[LTree]]) = {
        new LTreeListColumnExtensionMethods[LTree, List[LTree]](c)
      }
    implicit def simpleLTreeListOptionColumnExtensionMethods(c: Rep[Option[List[LTree]]]) = {
        new LTreeListColumnExtensionMethods[LTree, Option[List[LTree]]](c)
      }
  }

  trait SimpleLTreePlainImplicits {
    import scala.reflect.classTag
    import utils.PlainSQLUtils._
    // to support 'nextArray[T]/nextArrayOption[T]' in PgArraySupport
    {
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(LTree.apply)(r.nextString()))
    }

    // used to support code gen
    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("ltree", classTag[LTree])
    }

    implicit class PgLTreePositionedResult(r: PositionedResult) {
      def nextLTree() = nextLTreeOption().orNull
      def nextLTreeOption() = r.nextStringOption().map(LTree.apply)
    }

    ///////////////////////////////////////////////////////////
    implicit val getLTree = mkGetResult(_.nextLTree())
    implicit val getLTreeOption = mkGetResult(_.nextLTreeOption())
    implicit val setLTree = mkSetParameter[LTree]("ltree")
    implicit val setLTreeOption = mkOptionSetParameter[LTree]("ltree")
    ///
    implicit val setLTreeArray = mkArraySetParameter[LTree]("ltree")
    implicit val setLTreeArrayOption = mkArrayOptionSetParameter[LTree]("ltree")
  }
}
