package org.squeryl.test

import org.squeryl.Schema
import org.squeryl.framework.{SchemaTester, RunTestsInsideTransaction}
import org.squeryl.PrimitiveTypeMode._
import org.squeryl._
import dsl.ast._
import dsl.{OneToMany, CompositeKey2}
import java.sql.{Savepoint}

class User(val userId: Long, val firstName: String, val lastName: String)

object ShardingTestDb extends Schema {

  val users = table[User]
  on(users)(
  (u) => declare (
    u.userId is (sharded)
  ))
}

class TestData {
  import ShardingTestDb._
  users.insert(new User(1, "Mickey", "Mouse"))
  users.insert(new User(2, "Donald", "Duck"))
}

abstract class ShardingTest extends SchemaTester with RunTestsInsideTransaction {
  val schema = ShardingTestDb

  test("sharded column attribute is correctly true") {
    val metadata = ShardingTestDb.users.posoMetaData.findFieldMetaDataForProperty("userId").
      getOrElse(fail("metadata for userId should exist"))
    metadata.isSharded should be (true)
  }
  test("sharded column attribute is correctly false") {
    val metadata = ShardingTestDb.users.posoMetaData.findFieldMetaDataForProperty("firstName").
      getOrElse(fail("metadata for firstName should exist"))
    metadata.isSharded should be (false)
  }
}
