package com.jtb.demo.order.services

import com.jtb.demo.order.domain.Order
import com.twitter.util.Future

import scala.concurrent.ExecutionContext.Implicits.global
import com.jtb.common.convert.FutureConvert._
import com.jtb.demo.order.database.OrderRep
import com.twitter.inject.Logging

trait OrderServcie {
  def createOrder(order: Order): Future[Unit]
  def comfirmOrder(tranId: String): Future[Unit]
  def cancelOrder(tranId: String): Future[Unit]
}

abstract class OrderServcieImpl extends OrderServcie  with Logging {

  def orderRep: OrderRep

  def createOrder(o: Order): Future[Unit] = {
    val r = orderRep.createOrder(o.tranId, o.orderId, o.franchiserId, o.productId, o.amount)
    r.asTwitter.flatMap(_ => Future.Done)
  }

  def comfirmOrder(tranId: String): Future[Unit] = {
    val r = orderRep.comfirm(tranId)
    r.onComplete( x => {
      error(x)
    })
    r.asTwitter.flatMap(_ => Future.Done)
  }

  def cancelOrder(tranId: String): Future[Unit] = {
    val r = orderRep.cancel(tranId)
    r.asTwitter.flatMap(_ => Future.Done)
  }
}