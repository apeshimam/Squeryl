package org.squeryl.test

import org.squeryl.Schema
import org.squeryl.framework.{SchemaTester, RunTestsInsideTransaction}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl._
import adapters.H2Adapter
import dsl.ast._
import dsl.{OneToMany, CompositeKey2}
import java.sql.{Savepoint}
import org.squeryl.h2.H2_Connection


class User(val userId: Long, val firstName: String, val lastName: String)

object ShardingTestDb extends Schema with H2_Connection {

  implicit object HashingFunction extends ShardingFunction[Long] {
    def selectShard(k: Long): Session = {
      val connect = connectToDb()
      connect.get.apply
    }

  }

  val users = table[User].shardedOn(_.userId)
}

class TestData {

  import ShardingTestDb._

}

class ShardingTest extends SchemaTester with H2_Connection {

  val schema = ShardingTestDb

  test("test insert and select via sharded lookup") {
    schema.users.shardedTransaction(1) {
      table =>
        table.insert(new User(1, "Mickey", "Mouse"))
    }

    val result = schema.users.shardedLookup(1)
    println(result)
  }


}