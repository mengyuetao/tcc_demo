package com.jtb.demo.order.database

import com.jtb.common.database.{DatabaseX, LogDbFail, Profile}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object OrderRep {

  object OrderStatus {

    val processing = "processing"
    val cancel = "cancel"
    val comfirm = "comfirm"
  }

  final case class Order
  (
    orderId: String,
    franchiserId: String,
    productId: String,
    amount: Int,
    tranId: String,
    status: String
  )

  final case class OrderTr
  (
    orderId: String,
    tranId: String,
    status: String
  )

}


/**
  * 对数据库进行操作
  */

trait OrderRep {

  import OrderRep._


  def getOrder(orderId:String): Future[Order]
  def getOrderTx(tranId:String): Future[OrderTr]


  /**
    * 按照创 tranId 创建一个新的订单,状态为处理中
    *
    * 如果 tranId 存在，创建失败
    * 如果 orderId 存在，创建失败
    *
    * @param tranId       事务Id
    * @param orderId      订单Id
    * @param franchiserId 经销商Id
    * @param productId    产品Id
    * @param amount       产品数量
    */
  def createOrder(
                   tranId: String,
                   orderId: String,
                   franchiserId: String,
                   productId: String,
                   amount: Int,

                 ): Future[Order]




  /**
    *
    * 修改订单的状态为确认
    *
    * 如果 tranId 不存在，失败
    * 如果 orderId 不存在，失败
    *
    * 如果 tranId 存在且状态为取消，失败
    * 如果 tranId 存在且状态为确认，成功
    * 如果 tranId 存在且状态为处理中，修改状态到成功
    *
    * @param tranId  事务Id
    * @return
    */
  def comfirm(tranId: String): Future[OrderTr]


  /**
    * 取消事务，回滚订单
    *
    * 如果tranId 存在，comfirm，那么异常
    * 如果tranId 存在，proceeing，那么cancel订单
    * 如果tranId 存在，cancel，直接返回
    * 如果tranId 不存在，插入cancel
    *
    * @param tranId  事务Id
    * @return
    */
  def cancel(tranId: String): Future[OrderTr]

}


trait OrderRepJdbc extends OrderRep with DatabaseX with LogDbFail {
  self: Profile =>

  import profile.api._
  import OrderRep._


  final class OrderTable(tag: Tag) extends Table[Order](tag, "order") {

    def orderId = column[String]("order_id" ,  O.PrimaryKey)

    def franchierId = column[String]("franchiser_id")

    def productId = column[String]("product_id")

    def amount = column[Int]("product_amount")

    def tranId = column[String]("tran_id")

    def status = column[String]("status")

    override def * = (orderId, franchierId, productId, amount, tranId, status).mapTo[Order]
  }


  final class OrderTrTable(tag: Tag) extends Table[OrderTr](tag, "order_tr") {

    def orderId = column[String]("order_id")

    def tranId = column[String]("tran_id" ,  O.PrimaryKey )

    def status = column[String]("status")

    override def * = (orderId, tranId, status).mapTo[OrderTr]
  }


  val orders = TableQuery[OrderTable]
  val orderTrs = TableQuery[OrderTrTable]

  private def updateOrderStatus(tranId: String, orderId: String, status: String): DBIO[Int] = {
    val act1 = orderTrs.filter { ot: OrderTrTable =>
      ot.orderId === orderId && ot.tranId === tranId && ot.status === OrderStatus.processing
    }.map(_.status).update(status).flatMap {
      case 1 => DBIO.successful(1)
      case _ => dbIoFail(s"updateOrderStatus of orderTrs tranId: $tranId  orderId: $orderId , status:$status failed")
    }

    val act2 = orders.filter { ot: OrderTable =>
      ot.orderId === orderId && ot.status === OrderStatus.processing
    }.map(_.status).update(status).flatMap {
      case 1 => DBIO.successful(1)
      case _ => dbIoFail(s"updateOrderStatus of orders tranId: $tranId  orderId: $orderId , status:$status failed")
    }

    act1.andThen(act2)
  }


  def createOrder(tranId: String, orderId: String, franchiserId: String, productId: String, amount: Int): Future[OrderRep.Order] = {

    val order = Order(orderId, franchiserId, productId, amount, tranId, OrderStatus.processing)
    val step1 = orderTrs += OrderTr(orderId, tranId, OrderStatus.processing)
    val step2 = orders += order
    val result = db.run(step1.andThen(step2).transactionally)
    result.flatMap(_ => Future.successful(order))
  }

  def comfirm(tranId: String): Future[OrderRep.OrderTr] = {
    val action1: DBIO[OrderRep.OrderTr] = orderTrs
      .filter { ot: OrderTrTable => ot.tranId === tranId }
      .forUpdate
      .result
      .flatMap { orders: Seq[OrderTr] =>
        orders match {
          case m +: Nil if m.status == OrderStatus.processing =>
            updateOrderStatus(m.tranId, m.orderId, OrderStatus.comfirm).flatMap { _ => DBIO.successful(m) }
          case m +: Nil if m.status == OrderStatus.comfirm => DBIO.successful(m)
          case m +: Nil if m.status == OrderStatus.cancel => dbIoFail(s"comfirm failed,tanId $tranId in comfirm status!")
          case Nil => dbIoFail(s"comfirm failed,tanId $tranId not exist!")
          case _ => dbIoFail(s"comfirm failed,tanId $tranId failed reason ,unknown!")
        }
      }
    db.run(action1.transactionally)
  }


  def cancel(tranId: String): Future[OrderRep.OrderTr] = {
    val action: DBIO[OrderRep.OrderTr] = orderTrs
      .filter { ot: OrderTrTable => ot.tranId === tranId }
      .forUpdate
      .result
      .flatMap { orders: Seq[OrderTr] =>
        orders match {
          case m +: Nil if m.status == OrderStatus.processing =>
            updateOrderStatus(m.tranId, m.orderId, OrderStatus.cancel).flatMap { _ => DBIO.successful(m) }
          case m +: Nil if m.status == OrderStatus.cancel => DBIO.successful(m)
          case m +: Nil if m.status == OrderStatus.comfirm => dbIoFail(s"cancel failed,tanId $tranId in comfirm status!")
          case Nil => val o = OrderTr("", tranId, OrderStatus.cancel)
            (orderTrs += o).flatMap { _ => DBIO.successful(o) }
          case _ => dbIoFail(s"comfirm failed,tanId $tranId failed reason ,unknown!")
        }
      }
    db.run(action.transactionally)
  }

  def getOrder(orderId:String): Future[Order] = {

    val action: DBIO[OrderRep.Order] = orders
      .filter { ot: OrderTable => ot.orderId === orderId }
      .result
      .flatMap { orders: Seq[Order] =>
        orders match {
          case m +: Nil   =>  DBIO.successful(m)
          case _ => dbIoFail(s"getOrder,orderId $orderId failed!")
        }
      }
    db.run(action)
  }

  def getOrderTx(tranId:String): Future[OrderTr] = {

    val action: DBIO[OrderRep.OrderTr] = orderTrs
      .filter { ot: OrderTrTable => ot.tranId=== tranId }
      .result
      .flatMap { orders: Seq[OrderTr] =>
        orders match {
          case m +: Nil   =>  DBIO.successful(m)
          case _ => dbIoFail(s"getOrder,tranId $tranId failed!")
        }
      }
    db.run(action)
  }

  def createSchema(): Unit ={
    val action = orders.schema.create
    val action2 = orderTrs .schema.create
    val future: Future[Unit]=db.run(action.andThen(action2))
    Await.result(future, 2.seconds)
  }

}

