package com.jtb.tcc.database

import java.text.SimpleDateFormat
import java.util.Date

import com.jtb.common.database.{DatabaseX, LogDbFail, Profile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


object TccRep {

  object TccLv1Status {

    val processing = "processing"
    val processingSuc = "processingSuc"
    val processingFail = "processingFail"
    val cancel = "cancel"
    val comfirm = "comfirm"
    val hang = "hang"
  }

  object TccLv2Status {

    val processing = "processing"
    val processingSuc = "processingSuc"
    val processingFail = "processingFail"
    val cancel = "cancel"
    val comfirm = "comfirm"
    val hang = "hang"
  }

  final case class TccLv1
  (
    tranId: String,
    modId: String,
    status: String,
    createTime: String,
    updateTime: String,
  )

  final case class TccLv2
  (
    tranId: String,
    modId: String,
    status: String,
    createTime: String,
    updateTime: String,
  )

  object TccLv3Status {
    val cancel = "cancel"
    val comfirm = "comfirm"
  }

  final case class TccLv3
  (
    tranId: String,
    modId: String,
    action: String,
  )

}


trait TccRep {

  import TccRep._

  /**
    * 使用唯一的Id注册
    *
    * 如果Id存在，那么失败，否则返回成功
    *
    */
  def Reg(tranId: String, modId: String): Future[TccLv1]

  /**
    * 请求关闭事务
    */
  def ReqClose(tranId: String, modId: String, isSuc: Boolean): Future[TccLv1]

  /**
    * 注册子事务，关闭后不能注册
    */
  def RegSub(tranId: String, subModId: String): Future[TccLv2]

  /**
    * 关闭子事务，关闭后不能关闭
    */
  def ReqCloseSub(tranId: String, modId: String, isSuc: Boolean): Future[TccLv2]


  /**
    *
    */
  def comfirmSub(tranId: String, modId: String): Future[TccLv2]


  /**
    *
    */
  def cancelSub(tranId: String, modId: String): Future[TccLv2]


  /**
    *
    */
  def comfirm(tranId: String, modId: String): Future[TccLv1]


  /**
    *
    */
  def cancel(tranId: String, modId: String): Future[TccLv1]


  /**
    * for test
    */
  def getTran(tranId: String, modId: String): Future[TccLv1]

  /**
    * 列出所有未关闭的子事务
    */
  def tccList(): Future[Seq[TccLv3]]


  /**
    * close main
    *
    */
  def closeMainTran(): Future[Seq[Unit]]

}

trait TccRepJdbc extends TccRep with DatabaseX with LogDbFail {
  self: Profile =>

  import TccRep._
  import profile.api._


  final class TccLv1Table(tag: Tag) extends Table[TccLv1](tag, "tcc_lv1") {

    def tranId = column[String]("tran_id")

    def modId = column[String]("mod_id")

    def status = column[String]("status")

    def createTime = column[String]("create_time")

    def updateTime = column[String]("update_time")

    def pk = primaryKey("tcc_lv1__pk", (tranId, modId))

    override def * = (tranId, modId, status, createTime, updateTime).mapTo[TccLv1]

  }

  final class TccLv2Table(tag: Tag) extends Table[TccLv2](tag, "tcc_lv2") {

    def tranId = column[String]("tran_id")

    def modId = column[String]("mod_id")

    def status = column[String]("status")

    def createTime = column[String]("create_time")

    def updateTime = column[String]("update_time")

    def pk = primaryKey("tcc_lv2__pk", (tranId, modId))

    override def * = (tranId, modId, status, createTime, updateTime).mapTo[TccLv2]

  }

  val tccLv1s = TableQuery[TccLv1Table]
  val tccLv2s = TableQuery[TccLv2Table]

  private def getCurrentFormatTime: String = {

    val sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    sdf.format(new Date())
  }

  private def updateTccLv1Status(tcclv1: TccLv1, newStatus: String): DBIO[TccLv1] = {
    val time = getCurrentFormatTime

    val action = tccLv1s.filter { t: TccLv1Table =>
      t.tranId === tcclv1.tranId &&
        t.modId === tcclv1.modId
    }
      .map(t => (t.status, t.updateTime))
      .update((newStatus, time))
      .flatMap {
        case 1 => DBIO.successful(tcclv1.copy(updateTime = time))
        case _ => dbIoFail(s"update fail tcclv1 = $tcclv1 , newStatus = $newStatus ")
      }
    action

  }


  private def lockAndupdateTccLv2Status(tranId: String, modId: String, status: String, newStatus: String): DBIO[TccLv2] = {

    val s1: DBIO[TccLv2] = tccLv2s
      .filter { t: TccLv2Table =>
        t.tranId === tranId &&
          t.modId === modId &&
          t.status === status
      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil => updateTccLv2Status(m, newStatus)
        case p@_ => dbIoFail(s"lockAndupdateTccLv2Status fail lvs= $p")
      }
    s1
  }


  private def lockAndupdateTccLv2StatusComfirm(tranId: String, modId: String, status1: String, status2: String, newStatus: String): DBIO[TccLv2] = {

    val s1: DBIO[TccLv2] = tccLv2s
      .filter { t: TccLv2Table =>
        t.tranId === tranId &&
          t.modId === modId &&
          (t.status === status1 || t.status === status2)
      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil => updateTccLv2Status(m, newStatus)
        case p@_ => dbIoFail(s"lockAndupdateTccLv2Status fail lvs= $p")
      }
    s1
  }

  private def lockAndupdateTccLv2StatusComfirm2(tranId: String, modId: String, status: Seq[String], newStatus: String): DBIO[TccLv2] = {

    val head +: tail = status;

    val s1: DBIO[TccLv2] = tccLv2s
      .filter { t: TccLv2Table =>
        t.tranId === tranId && t.modId === modId &&
          tail.foldLeft(t.status === head)(
            (x, y) => x || t.status === y)

      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil => updateTccLv2Status(m, newStatus)
        case p@_ => dbIoFail(s"lockAndupdateTccLv2Status fail lvs= $p")
      }
    s1
  }


  private def updateTccLv2Status(tcclv2: TccLv2, newStatus: String): DBIO[TccLv2] = {
    val time = getCurrentFormatTime

    val action = tccLv2s.filter { t: TccLv2Table =>
      t.tranId === tcclv2.tranId &&
        t.modId === tcclv2.modId
    }
      .map(t => (t.status, t.updateTime))
      .update((newStatus, time))
      .flatMap {
        case 1 => DBIO.successful(tcclv2.copy(updateTime = time))
        case _ => dbIoFail(s"update fail tcclv1 = $tcclv2 , newStatus = $newStatus ")
      }
    action

  }

  def Reg(tranId: String, modId: String): Future[TccLv1] = {

    val time = getCurrentFormatTime
    val o = TccLv1(tranId, modId, TccLv1Status.processing,
      time, time)

    val insert = tccLv1s += o
    val result = db.run(insert.transactionally)
    result.flatMap(_ => Future.successful(o))

  }

  def ReqClose(tranId: String, modId: String, isSuc: Boolean): Future[TccLv1] = {

    val status = if (isSuc) TccLv1Status.processingSuc else TccLv1Status.processingFail

    val s1: DBIO[TccLv1] = tccLv1s
      .filter { t: TccLv1Table =>
        t.tranId === tranId &&
          t.modId === modId
      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil if m.status == TccLv1Status.processing =>
          tccLv2s.filter { t2: TccLv2Table =>
            t2.tranId === m.tranId
          }.result
            .flatMap { l2r =>
              if (l2r.forall(_.status == TccLv2Status.processingSuc)) {
                updateTccLv1Status(m, status)
              } else {
                updateTccLv1Status(m, TccLv1Status.processingFail)
              }

            }

        case p@_ => dbIoFail(s"ReqClose fail lvs= $p")
      }
    db.run(s1.transactionally)
  }

  def RegSub(tranId: String, modId: String): Future[TccLv2] = {

    val time = getCurrentFormatTime
    val ttlv2 = TccLv2(tranId, modId, TccLv2Status.processing, time, time)
    val insert = tccLv2s += ttlv2

    val s1: DBIO[TccLv2] = tccLv1s
      .filter { t: TccLv1Table =>
        t.tranId === tranId &&
          t.status === TccLv1Status.processing
      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil if m.status == TccLv1Status.processing =>
          insert.flatMap { case 1 => DBIO.successful(ttlv2)
          case _ => dbIoFail(s"ReqSub fail lvs= $m")
          }
        case p@_ => dbIoFail(s"ReqSub fail lvs= $p")
      }
    db.run(s1.transactionally)
  }

  def ReqCloseSub(tranId: String, modId: String, isSuc: Boolean): Future[TccLv2] = {

    val status = if (isSuc) TccLv2Status.processingSuc else TccLv2Status.processingFail

    //    val time = getCurrentFormatTime
    //    val ttlv2 = TccLv2(tranId, modId, TccLv2Status.processing, time, time)
    //    val insert = tccLv2s += ttlv2

    val s1: DBIO[TccLv2] = tccLv1s
      .filter { t: TccLv1Table =>
        t.tranId === tranId &&
          t.status === TccLv1Status.processing
      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil => lockAndupdateTccLv2Status(tranId, modId, TccLv2Status.processing, status)
        case p@_ => dbIoFail(s"ReqSub fail lvs= $p")
      }
    db.run(s1.transactionally)


  }

  def comfirmOrCancelSub(tranId: String, modId: String, status: String): Future[TccLv2] = {


    //    val time = getCurrentFormatTime
    //    val ttlv2 = TccLv2(tranId, modId, TccLv2Status.processing, time, time)
    //val insert = tccLv2s += ttlv2

    val s1: DBIO[TccLv2] = tccLv1s
      .filter { t: TccLv1Table =>
        t.tranId === tranId &&
          (t.status === TccLv1Status.processingFail || t.status === TccLv1Status.processingSuc)
      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil => lockAndupdateTccLv2StatusComfirm2(tranId, modId,
          Seq(TccLv2Status.processing, TccLv2Status.processingFail, TccLv2Status.processingSuc, TccLv2Status.hang), status)
        case p@_ => dbIoFail(s"comfirmOrCancelSub fail lvs= $p")
      }
    db.run(s1.transactionally)

  }

  def comfirmSub(tranId: String, modId: String): Future[TccLv2] = comfirmOrCancelSub(tranId, modId, TccLv2Status.comfirm)

  def cancelSub(tranId: String, modId: String): Future[TccLv2] = comfirmOrCancelSub(tranId, modId, TccLv2Status.cancel)


  def comfirmOrCancel(tranId: String, modId: String, status: String): Future[TccLv1] = {


    db.run(comfirmOrCancelDBIO(tranId: String, modId: String, status: String).transactionally)

  }

  def comfirmOrCancelDBIO(tranId: String, modId: String, status: String): DBIO[TccLv1] = {


    val s1: DBIO[TccLv1] = tccLv1s
      .filter { t: TccLv1Table =>
        t.tranId === tranId &&
          t.modId === modId && (t.status === TccLv1Status.processingFail || t.status === TccLv1Status.processingSuc)
      }
      .forUpdate
      .result
      .flatMap {
        case m +: Nil => updateTccLv1Status(m, status)
        case p@_ => dbIoFail(s"ReqClose fail lvs= $p")
      }
    s1
  }

  def comfirm(tranId: String, modId: String): Future[TccLv1] = comfirmOrCancel(tranId, modId, TccLv1Status.comfirm)

  def cancel(tranId: String, modId: String): Future[TccLv1] = comfirmOrCancel(tranId, modId, TccLv1Status.cancel)

  def createSchema(): Unit = {
    val action = tccLv1s.schema.create
    val action2 = tccLv2s.schema.create
    val future: Future[Unit] = db.run(action.andThen(action2))
    Await.result(future, 2.seconds)
  }

  def tccList(): Future[Seq[TccLv3]] = {

    val e = for {

      l1 <- tccLv1s
      if l1.status === TccLv1Status.processingSuc ||
        l1.status === TccLv1Status.processingFail
      l2 <- tccLv2s

      if l2.tranId === l1.tranId && l2.status =!= TccLv1Status.comfirm && l2.status =!= TccLv1Status.cancel


    } yield {
      (l2.tranId, l2.modId, l1.status)
    }

    val e2 = e.result.flatMap(v =>

      DBIO.successful(v.map { r => {

        if (r._3 == TccLv1Status.processingSuc)
          TccLv3(r._1, r._2, TccLv3Status.comfirm)
        else

          TccLv3(r._1, r._2, TccLv3Status.cancel)
      }
      }))

    db.run(e2.transactionally)

  }

  private def updateifallcomfirmorcancel(r: (String, String, String)): DBIO[Unit] = {

    val (tranId, modId, status) = r

    val comfirmorcancel = if (status == TccLv1Status.processingFail) TccLv3Status.cancel else TccLv3Status.comfirm
    val comfirmorcancel2 = if (status == TccLv1Status.processingFail) TccLv2Status.cancel else TccLv2Status.comfirm

    val s1: DBIO[Seq[TccLv2]] = tccLv2s
      .filter { t: TccLv2Table =>
        t.tranId === tranId && t.status =!= comfirmorcancel2
      }.result


    s1.flatMap {
      case Nil => comfirmOrCancelDBIO(tranId, modId, comfirmorcancel).flatMap(_ => DBIO.successful())
      case _ => DBIO.successful()
    }


  }

  def closeMainTran(): Future[Seq[Unit]] = {

    val e = for {
      l1 <- tccLv1s
      if l1.status === TccLv1Status.processingSuc || l1.status === TccLv1Status.processingFail

    } yield {
      (l1.tranId, l1.modId, l1.status)
    }


    val e2 = e.result.flatMap(rr =>
      DBIO.sequence(rr.map(updateifallcomfirmorcancel)))

    db.run(e2.transactionally)

  }


  def getTran(tranId: String, modId: String): Future[TccLv1] = {

    val e =  tccLv1s
      .filter { l1=>
        l1.tranId === tranId && l1.modId === modId

    }.result
      .flatMap {
        case m +: Nil =>  DBIO.successful(m)
        case p@_ => dbIoFail(s"getTran fail lvs= $p")

      }

    db.run(e )
  }
}