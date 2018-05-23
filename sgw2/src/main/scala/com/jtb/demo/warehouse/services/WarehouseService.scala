package com.jtb.demo.warehouse.services.services

import com.jtb.demo.warehouse.database.WarehouseRep
import com.jtb.demo.warehouse.domain.Order
import com.twitter.util.Future
import com.jtb.common.convert.FutureConvert._
import scala.concurrent.ExecutionContext.Implicits.global

trait WarehouseService {

  def addWareHouse(order: Order): Future[Unit]

  def comfirmWareHouse(tranId: String): Future[Unit]

  def cancelWareHouse(tranId: String): Future[Unit]
}

abstract class WarehouseServiceImpl extends WarehouseService {

  def warehouseRep: WarehouseRep

  def addWareHouse(o: Order): Future[Unit] = {
    warehouseRep
      .addToWarehouse(o.tranId, o.franchiserId, o.productId, o.amount, o.orderId)
      .asTwitter.flatMap(_ => Future.Done)
  }

  def comfirmWareHouse(tranId: String): Future[Unit] = {
    warehouseRep
      .comfirm(tranId)
      .asTwitter.flatMap(_ => Future.Done)
  }

  def cancelWareHouse(tranId: String): Future[Unit] = {
    warehouseRep
      .cancel(tranId)
      .asTwitter.flatMap(_ => Future.Done)
  }
}