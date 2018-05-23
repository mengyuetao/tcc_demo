package com.jtb.demo.warehouse.services

import com.jtb.common.database.Profile
import com.jtb.demo.warehouse.database.WarehouseRepJdbc
import com.twitter.inject.Test
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration._


class WarehouseRepTest extends Test {


  val db = Database.forConfig("chapter01")
  val databaseLayer = new DatabaseLayer(slick.jdbc.H2Profile, db)

  class DatabaseLayer(val profile: JdbcProfile,
                      val db: slick.jdbc.JdbcBackend.Database) extends Profile with WarehouseRepJdbc

  override def beforeAll(): Unit = {
    databaseLayer.createSchema()
  }

  test("test add rm  warehouse and cancel then cancel again") {


    //1 添加库存 10
    Await.result(databaseLayer.addToWarehouse("tr01", "f01", "p01", 10, "order01"), 2.seconds)
      .tranId should equal("tr01")

    //2 取消
    Await.result(
      databaseLayer.cancel("tr01"), 2.seconds).tranId should equal("tr01")

    Await.result(
      databaseLayer.cancel("tr01"), 2.seconds).tranId should equal("tr01")

    //3 库存应该为 0
    Await.result(
      databaseLayer.getWarehouse("f01", "p01"), 2.seconds).amount should equal(0)



    //1 添加库存 10
    Await.result(databaseLayer.addToWarehouse("tr02", "f01", "p01", 10, "ord02"), 2.seconds)
      .tranId should equal("tr02")

    //1.1 未确认库存应该为 0
    Await.result(
      databaseLayer.getWarehouse("f01", "p01"), 2.seconds).amount should equal(0)

    //2 确认
    Await.result(
      databaseLayer.comfirm("tr02"), 2.seconds).tranId should equal("tr02")

    //3 库存应该为 10
    Await.result(
      databaseLayer.getWarehouse("f01", "p01"), 2.seconds).amount should equal(10)

    //4 减去 5件，还有5件
    Await.result(databaseLayer.addToWarehouse("tr03", "f01", "p01", -5, "ord03"), 2.seconds)
      .tranId should equal("tr03")

    //3 库存应该为 5
    Await.result(
      databaseLayer.getWarehouse("f01", "p01"), 2.seconds).amount should equal(5)

    //4 取消，取消，应该还是10件
    //2 确认
    Await.result(
      databaseLayer.cancel("tr03"), 2.seconds).tranId should equal("tr03")

    Await.result(
      databaseLayer.cancel("tr03"), 2.seconds).tranId should equal("tr03")

    //3 库存应该为 10
    Await.result(
      databaseLayer.getWarehouse("f01", "p01"), 2.seconds).amount should equal(10)

    //4 减去 100件，失败
    val caught =
      intercept[RuntimeException] {
        Await.result(databaseLayer.addToWarehouse("tr04", "f01", "p01", -100, "ord04"), 2.seconds)
          .tranId should equal("tr04")
      }
    assert(caught.getMessage.indexOf("updateWhMount failed finalct < 0") != -1)

    //3 库存应该为 10
    Await.result(
      databaseLayer.getWarehouse("f01", "p01"), 2.seconds).amount should equal(10)

  }

  test("test add rm warehouse order and comfirm and comfirm again and cancel") {

    //1 添加库存 10
    Await.result(databaseLayer.addToWarehouse("tr10", "f02", "p02", 100, "ord100"), 2.seconds)
      .tranId should equal("tr10")

    Await.result(
      databaseLayer.comfirm("tr10"), 2.seconds).tranId should equal("tr10")

    Await.result(
      databaseLayer.comfirm("tr10"), 2.seconds).tranId should equal("tr10")

    //3 库存应该为 10
    Await.result(
      databaseLayer.getWarehouse("f02", "p02"), 2.seconds).amount should equal(100)

    val caught =
      intercept[RuntimeException] {
        Await.result(
          databaseLayer.cancel("tr10"), 2.seconds)
      }
    assert(caught.getMessage.indexOf("cancel failed") != -1)

  }


  test("test add rm  warehouse and cancel and comfirm") {

    //1 添加库存 10
    Await.result(databaseLayer.addToWarehouse("tr20", "f02", "p02", 100, "ord100"), 2.seconds)
      .tranId should equal("tr20")

    Await.result(
      databaseLayer.cancel("tr20"), 2.seconds).tranId should equal("tr20")

    val caught =
      intercept[RuntimeException] {
        Await.result(
          databaseLayer.comfirm("tr20"), 2.seconds)
      }
    assert(caught.getMessage.indexOf("comfirm failed") != -1)
  }

  test("test rm warehouse not have enought rep first time ") {
    val caught =
      intercept[RuntimeException] {
        Await.result(databaseLayer.addToWarehouse("tr30", "f03", "p03", -100, "ord100"), 2.seconds)
      }
    assert(caught.getMessage.indexOf("comfirm failed") != -1)
  }

  test("test comfirm not exist") {
    val caught =
      intercept[RuntimeException] {
        Await.result(
          databaseLayer.comfirm("tr40"), 2.seconds)
      }
    assert(caught.getMessage.indexOf("comfirm failed") != -1)
  }

  test("test cancel not exist") {
    Await.result(
      databaseLayer.cancel("tr50"), 2.seconds).tranId should equal("tr50")
    val caught =
      intercept[RuntimeException] {
        Await.result(
          databaseLayer.comfirm("tr50"), 2.seconds)
      }
    assert(caught.getMessage.indexOf("comfirm failed") != -1)
  }

}
