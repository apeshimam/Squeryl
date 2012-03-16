package org.squeryl

import dsl.ast.TypedExpressionNode
import dsl.QueryDsl
import internals.FieldMetaData
import internals.FieldReferenceLinker
import internals.Utils

trait ShardingFunction[K] {
  def selectShard(k: K): Session
}

class ShardedTable[A,K](table: Table[A],
                        invokeShardedFieldGetter: A => AnyRef,
                        shardedFieldMetaData: FieldMetaData,
                        f: ShardingFunction[K]) {

  def shardedLookup(k: K)(implicit dsl: QueryDsl) = {

    import dsl._

    using(f.selectShard(k)) {
      val q = from(table)(a => dsl.where {
        FieldReferenceLinker.createEqualityExpressionWithLastAccessedFieldReferenceAndConstant(invokeShardedFieldGetter(a), k)
      } select(a))
      q.single
    }
  }

  def shardedTransaction[U](shardKey: K)(block: Table[A] => U)(implicit dsl: QueryDsl) =
    dsl.transaction(f.selectShard(shardKey)) {
      block(table)
    }
}

class ShardedTablePrecursor[A](table: Table[A]) {

   def shardedOn[K,T](f: A => T)(implicit dsl: QueryDsl, ev: T => TypedExpressionNode[K], sf: ShardingFunction[K]) = {

     import dsl._

     val q = from(table)(a=> select(a)) : Query[A]

     val n =
       Utils.mapSampleObject(q, (a0:A) => ev(f(a0)))

     val invoker = f.asInstanceOf[A=>AnyRef]

     new ShardedTable[A,K](table, invoker, n._fieldMetaData, sf)
   }
}