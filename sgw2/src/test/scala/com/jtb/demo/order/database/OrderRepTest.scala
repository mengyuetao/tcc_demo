package com.jtb.demo.order.database

import com.jtb.common.database.Profile
import com.twitter.inject.Test
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration._


class OrderRepTest extends Test {


  import com.jtb.demo.order.database.OrderRep._




  val db = Database.forConfig("chapter01")
  val databaseLayer = new DatabaseLayer(slick.jdbc.H2Profile , db)
  class DatabaseLayer(val profile: JdbcProfile ,
                      val   db: slick.jdbc.JdbcBackend.Database ) extends Profile with OrderRepJdbc

  override def beforeAll(): Unit = {
    databaseLayer.createSchema()
  }

  test("test create order and cancel then cancel again") {

     Await.result(databaseLayer.createOrder("tranId01","orderId01","franchiserId01","productId01",10), 2.seconds)
     .orderId  should equal ("orderId01")

     Await.result(
      databaseLayer.cancel("tranId01"), 2.seconds).tranId should equal ("tranId01")

    Await.result(
      databaseLayer.cancel("tranId01"), 2.seconds).tranId should equal ("tranId01")

    Await.result(databaseLayer.getOrder("orderId01"),2.seconds)
      .shouldEqual(Order("orderId01","franchiserId01","productId01",10,"tranId01",OrderStatus.cancel))

  }



  test("test create order and comfirm and comfirm again") {
    Await.result(databaseLayer.createOrder("tranId02","orderId02","franchiserId02","productId02",10), 2.seconds)
      .orderId  should equal ("orderId02")

    Await.result(
      databaseLayer.comfirm("tranId02"), 2.seconds).tranId should equal ("tranId02")

    Await.result(
      databaseLayer.comfirm("tranId02"), 2.seconds).tranId should equal ("tranId02")

    Await.result(databaseLayer.getOrder("orderId02"),2.seconds)
      .shouldEqual(Order("orderId02","franchiserId02","productId02",10,"tranId02",OrderStatus.comfirm))

  }


  test("test create order and comfirm and cancel") {
    Await.result(databaseLayer.createOrder("tranId03","orderId03","franchiserId03","productId03",10), 2.seconds)
      .orderId  should equal ("orderId03")

    Await.result(
      databaseLayer.comfirm("tranId03"), 2.seconds).tranId should equal ("tranId03")

    val caught =
      intercept[RuntimeException] {
        Await.result(
          databaseLayer.cancel("tranId03"), 2.seconds)
      }
    assert(caught.getMessage.indexOf("tanId tranId03 in comfirm status!") != -1)

    Await.result(databaseLayer.getOrder("orderId03"),2.seconds)
      .shouldEqual(Order("orderId03","franchiserId03","productId03",10,"tranId03",OrderStatus.comfirm))

  }


  test("test create order and cancel and comfirm") {
    Await.result(databaseLayer.createOrder("tranId04","orderId04","franchiserId04","productId04",10), 2.seconds)
      .orderId  should equal ("orderId04")

    Await.result(
      databaseLayer.cancel("tranId04"), 2.seconds).tranId should equal ("tranId04")

    val caught =
      intercept[RuntimeException] {
        Await.result(
          databaseLayer.comfirm("tranId04"), 2.seconds)
      }
    assert(caught.getMessage.indexOf("tanId tranId04 in comfirm status!") != -1)

    Await.result(databaseLayer.getOrder("orderId04"),2.seconds)
      .shouldEqual(Order("orderId04","franchiserId04","productId04",10,"tranId04",OrderStatus.cancel))


  }

  test("test comfirm not exist") {

    val caught =
      intercept[RuntimeException] {
        Await.result(databaseLayer.comfirm("tranId1000"),2.seconds)
      }
    assert(caught.getMessage.indexOf("tanId tranId1000 not exist!") != -1)


  }

  test("test cancel not exist") {
       assert( Await.result(databaseLayer.cancel("tranId2000"),2.seconds).tranId == "tranId2000")

    Await.result(databaseLayer.getOrderTx("tranId2000"),2.seconds)
      .shouldEqual(OrderTr("","tranId2000",OrderStatus.cancel))

    val caught =
      intercept[RuntimeException] {
        Await.result(databaseLayer.comfirm("tranId2000"),2.seconds)
      }
    assert(caught.getMessage.indexOf("comfirm failed") != -1)
  }
}
