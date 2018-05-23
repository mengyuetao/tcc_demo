package com.jtb.demo.warehouse.database

import com.jtb.common.database.{DatabaseX, LogDbFail, Profile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object WarehouseRep {


  object WarehouseStatus {

    val processing = "processing"
    val cancel = "cancel"
    val comfirm = "comfirm"
  }

  final case class WarehouseTr
  (
    orderId: String,
    franchiserId: String,
    productId: String,
    amount: Int,
    tranId: String,
    status: String
  )

  final case class Warehouse
  (

    franchiserId: String,
    productId: String,
    amount: Int,
    lastTrId: String,

  )

}

trait WarehouseRep {

  import WarehouseRep._

  /**
    * 添加库存,状态为 Processing
    *
    * @param tranId       事务Id, 如果事务Id已经存在，那么抛出异常
    * @param franchiserId 经销商Id, 非空字符串
    * @param productId    商品Id, 非空字符串
    * @param amount       商品数量, 大于0小于1000
    * @param orderId      订单Id
    */
  def addToWarehouse(tranId: String, franchiserId: String, productId: String, amount: Int, orderId: String): Future[WarehouseTr]


  /**
    *
    * 增加锁定并库存数量
    * 如果库存不存在，那么添加库存
    *
    * @param tranId
    */
  def comfirm(tranId: String): Future[WarehouseTr]


  /**
    * 略
    *
    * @param tranId
    */
  def cancel(tranId: String): Future[WarehouseTr]

}


trait WarehouseRepJdbc extends WarehouseRep with DatabaseX with LogDbFail {
  self: Profile =>

  import WarehouseRep._
  import profile.api._


  final class WarehouseTrTable(tag: Tag) extends Table[WarehouseTr](tag, "warehouse_tr") {

    def orderId = column[String]("order_id")

    def franchierId = column[String]("franchiser_id")

    def productId = column[String]("product_id")

    def amount = column[Int]("product_amount")

    def tranId = column[String]("tran_id" ,  O.PrimaryKey)

    def status = column[String]("status")

    override def * = (orderId, franchierId, productId, amount, tranId, status).mapTo[WarehouseTr]
  }

  final class WarehouseTable(tag: Tag) extends Table[Warehouse](tag, "warehouse") {


    def franchierId = column[String]("franchiser_id")

    def productId = column[String]("product_id")

    def amount = column[Int]("product_amount")

    def lastTrId = column[String]("last_tr_id")

    def pk = primaryKey("warehouse_pk", (franchierId, productId))

    override def * = (franchierId, productId, amount, lastTrId).mapTo[Warehouse]
  }

  val wrTrs = TableQuery[WarehouseTrTable]
  val wrs = TableQuery[WarehouseTable]


  private def updateWhMountEvenNotExist(mtr: WarehouseTr): DBIO[Warehouse] = {
    //库存存在，更新数据库的库存数量，库存必须大于0
    def updateWhMount(m: Warehouse, mtr: WarehouseTr): DBIO[Warehouse] = {

      val finalct = m.amount + mtr.amount
      if (finalct < 0) {
        return dbIoFail(s"updateWhMount failed finalct < 0 ")
      }

      val act1 = wrs.filter { ot: WarehouseTable =>
        ot.franchierId === m.franchiserId && ot.productId === m.productId
      }
        .map(m=>(m.amount,m.lastTrId)).update((finalct,mtr.tranId)).flatMap {
        case 1 => DBIO.successful(1)
        case _ => dbIoFail(s"updateWhMount failed finalct = $finalct ")
      }

      act1.flatMap(_ => DBIO.successful(m.copy(amount = finalct)))

    }

    val updateWr: DBIO[Warehouse] =
      wrs.filter { ot: WarehouseTable =>
        ot.franchierId === mtr.franchiserId && ot.productId === mtr.productId
      }.forUpdate
        .result
        .flatMap {
          case mx +: Nil => updateWhMount(mx, mtr) //1.1 库存条目存在，直接更新
          case Nil if mtr.amount > 0 => //1.2 不存在，插入新库存
            val newAdd = Warehouse(mtr.franchiserId, mtr.productId, mtr.amount, mtr.tranId)
            (wrs += newAdd).flatMap(_ => DBIO.successful(newAdd))
          case _ => dbIoFail(s"comfirm failed,tanId ${mtr.tranId} failed reason ,unknown!")
        }
    updateWr
  }


  private def updateTx(mtr: WarehouseTr, status: String): DBIO[WarehouseTr] = {

    val act1 = wrTrs.filter { ot: WarehouseTrTable =>
      ot.tranId === mtr.tranId && ot.status === WarehouseStatus.processing
    }.map(_.status).update(status).flatMap {
      case 1 => DBIO.successful(1)
      case _ => dbIoFail(s"$status failed tranId = ${mtr.tranId} ")
    }
    act1.flatMap(_ => DBIO.successful(mtr))

  }

  def getWarehouse(franchiserId: String, productId: String)  : Future[Warehouse] = {
    val action1: DBIO[Warehouse] = wrs
      .filter { ot: WarehouseTable => ot.franchierId === franchiserId && ot.productId === productId }
      .result
      .flatMap {
        //1.如果存在待确认的库存，那么更新库存
        case m +: Nil  => DBIO.successful(m)
        case Nil =>    DBIO.successful(Warehouse(franchiserId,productId,0,""))
        case _ => dbIoFail(s"getwarehouse failed, failed reason ,unknown!")
      }
    db.run(action1.transactionally)
  }

  def addToWarehouse(tranId: String, franchiserId: String, productId: String, amount: Int, orderId: String): Future[WarehouseTr] = {

    val wrTr = WarehouseTr(orderId, franchiserId, productId, amount, tranId, WarehouseStatus.processing)
    val step1 = wrTrs += wrTr

    //如果数量大于零，那么添加到数据库
    //如果数量小于零，那么添加到数据库，并且减去库存
    val step2 = if (amount < 0) {
      updateWhMountEvenNotExist(wrTr).flatMap(_ => step1)
    } else {
      step1
    }

    db.run(step2.flatMap(_ => DBIO.successful(wrTr)).transactionally)
  }


  def comfirm(tranId: String): Future[WarehouseTr] = {

    def updateWhAdd(mtr: WarehouseTr): DBIO[WarehouseTr] = {
      if (mtr.amount < 0) {
        updateTx(mtr, WarehouseStatus.comfirm) //如果是出库，直接更新状态，应为库存已经扣除
      } else { //如果是入库，直接更新状态还要加入库存
        updateWhMountEvenNotExist(mtr).flatMap(_ => updateTx(mtr, WarehouseStatus.comfirm))
      }
    }

    val action1: DBIO[WarehouseTr] = wrTrs
      .filter { ot: WarehouseTrTable => ot.tranId === tranId }
      .forUpdate
      .result
      .flatMap {
        //1.如果存在待确认的库存，那么更新库存
        case m +: Nil if m.status == WarehouseStatus.processing => updateWhAdd(m)
        case m +: Nil if m.status == WarehouseStatus.comfirm => DBIO.successful(m)
        case m +: Nil if m.status == WarehouseStatus.cancel => dbIoFail(s"comfirm failed,tanId $tranId in cancel status!")
        case Nil => dbIoFail(s"comfirm failed,tanId $tranId not exist!")
        case _ => dbIoFail(s"comfirm failed,tanId $tranId failed reason ,unknown!")
      }
    db.run(action1.transactionally)

  }


  def cancel(tranId: String): Future[WarehouseTr] = {

    def updateWhAdd(mtr: WarehouseTr): DBIO[WarehouseTr] = {
      if (mtr.amount > 0) { //如果是入库，直接更新状态
        updateTx(mtr, WarehouseStatus.cancel)
      } else { //如果是出库，直接更新状态，归还库存
        updateWhMountEvenNotExist(mtr.copy(amount = mtr.amount * -1)).flatMap(_ => updateTx(mtr, WarehouseStatus.cancel))
      }
    }

    val action1: DBIO[WarehouseTr] = wrTrs
      .filter { ot: WarehouseTrTable => ot.tranId === tranId }
      .forUpdate
      .result
      .flatMap {
        //1.如果存在待确认的库存，那么更新库存
        case m +: Nil if m.status == WarehouseStatus.processing => updateWhAdd(m)
        case m +: Nil if m.status == WarehouseStatus.cancel => DBIO.successful(m)
        case m +: Nil if m.status == WarehouseStatus.comfirm => dbIoFail(s"cancel failed,tanId $tranId in comfirm status!")
        case Nil => val o = WarehouseTr("", "", "", 0, tranId, WarehouseStatus.cancel)
          (wrTrs += o).flatMap { _ => DBIO.successful(o) }
        case _ => dbIoFail(s"cancel failed,tanId $tranId failed reason ,unknown!")
      }
    db.run(action1.transactionally)

  }

  def createSchema(): Unit = {
    val action = wrTrs.schema.create
    val action2 = wrs.schema.create
    val future: Future[Unit] = db.run(action.andThen(action2))
    Await.result(future, 2.seconds)
  }

}

