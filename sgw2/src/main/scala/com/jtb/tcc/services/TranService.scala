package com.jtb.tcc.services

import com.jtb.sgw2.SnowFlake
import com.jtb.common.convert.FutureConvert._
import com.jtb.tcc.database.TccRep
import com.jtb.tcc.database.TccRep.{TccLv1, TccLv2, TccLv3}
import com.jtb.tcc.domain.{Tcc, Tran}
import com.twitter.util.Future

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future => ScalaFuture}

trait TranService {

  /**
    * 启动母事务，生成并返回唯一的Id + ModId
    *
    *
    *
    */
  def startTran(modId: String): Future[Tran]

  /**
    * 启动子事务，Id + modId
    *
    * 相同Id的母事只有一个
    *
    * 必选，这一步是必须被调用，相关的事务才能被 comfirm 或 cancel
    * 因此，在调用子模块前调用，调用成功后，才能继续调用子模块
    *
    *
    */
  def startSubTran(tranId: String, modId: String): Future[Tran]

  /**
    * 结束子事务 Id + modId
    *
    * 可选方法：
    * 调用并为suc时，事务采用被提交
    * 调用并为suc时，其他子事务失败，事务取消
    * 调用并为fail时， 事务取消
    * 不调用，事务取消 （超时促发）
    *
    */
  def closeSubTran(tranId: String, modId: String , tryCommit:Boolean ): Future[Unit]

  /**
    * 关闭母事务
    *
    * 关闭后，处理结束，子事务不能提交结果。
    *
    * 调用并为suc时，事务采用被提交
    * 调用并为suc时，其他子事务失败，事务取消
    * 调用并为fail时， 事务取消
    * 不调用，事务取消 （超时促发）
    *
    */
  def closeTran(tranId: String, modId: String , tryCommit:Boolean ): Future[Unit]



  def cancelSub(tranId: String, modId: String ): Future[Unit]
  def comfirmSub(tranId: String, modId: String ): Future[Unit]


  /**
    * list
    * 超时处理，占时不实现
    * 异常处理，设置为 hangup
    * 对于已经关闭的子事务，关闭main事务
    */

  def list():Future[Seq[Tcc]]

}


abstract class TranServiceImpl extends TranService {

  val snowFlake: SnowFlake = new SnowFlake(1, 1)
  val tccRep: TccRep

  override def startTran(modId: String): Future[Tran] = {
    val id: Long = snowFlake.nextId()
    val ret: ScalaFuture[Tran] =
      tccRep.Reg(id.toString, modId)
        .flatMap { x: TccLv1 =>
          ScalaFuture[Tran](new Tran(x.tranId, x.modId, Tran.Processing))
        }
    ret.asTwitter
  }

  override def startSubTran(tranId: String, modId: String): Future[Tran] = {
    val ret: ScalaFuture[Tran] =
      tccRep.RegSub(tranId, modId)
        .flatMap { x: TccLv2 =>
          ScalaFuture[Tran](new Tran(x.tranId, x.modId, Tran.Processing))
        }
    ret.asTwitter
  }

  override def closeSubTran(tranId: String, modId: String , tryCommit:Boolean ): Future[Unit] = {
    val ret = tccRep.ReqCloseSub(tranId, modId, tryCommit)
      .flatMap(_ =>
        ScalaFuture[Unit](Unit)
      )
    ret.asTwitter
  }

  override def closeTran(tranId: String, modId: String , tryCommit:Boolean): Future[Unit] = {
    val ret = tccRep.ReqClose(tranId, modId, tryCommit)
      .flatMap(_ =>
        ScalaFuture[Unit](Unit)
      )
    ret.asTwitter
  }


  def cancelSub(tranId: String, modId: String ): Future[Unit] ={
    val ret = tccRep.cancel(tranId, modId )
      .flatMap(_ =>
        ScalaFuture[Unit](Unit)
      )
    ret.asTwitter
  }

  def comfirmSub(tranId: String, modId: String ): Future[Unit] = {
    val ret = tccRep.comfirmSub(tranId, modId )
      .flatMap(_ =>
        ScalaFuture[Unit](Unit)
      )
    ret.asTwitter

  }

  /**
    * list
    * 超时处理，占时不实现
    * 异常处理，设置为 hangup
    * 对于已经关闭的子事务，关闭main事务
    */

  def list():Future[Seq[Tcc]] = {

    //关闭已经处理
    val x=tccRep.closeMainTran().asTwitter





    val ret  =    tccRep.tccList()

    val ret2=  ret.flatMap { x: Seq[TccLv3] =>
        ScalaFuture( x.map  { z => Tcc(z.tranId,z.modId,z.action)})
      }

    x.flatMap( _ =>    ret2.asTwitter)


  }
}