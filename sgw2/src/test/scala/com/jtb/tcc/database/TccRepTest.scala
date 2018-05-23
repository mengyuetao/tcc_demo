package com.jtb.tcc.database

import com.jtb.common.database.Profile
import com.twitter.inject.Test
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration._

class TccRepTest extends Test {


  val db = Database.forConfig("chapter01")
  val databaseLayer = new DatabaseLayer(slick.jdbc.H2Profile, db)

  class DatabaseLayer(val profile: JdbcProfile,
                      val db: slick.jdbc.JdbcBackend.Database) extends Profile with TccRepJdbc

  override def beforeAll(): Unit = {
    databaseLayer.createSchema()
  }

  test("test reg regClose and comfirm ") {


    Await.result(databaseLayer.Reg("tranId01", "modId01"), 2.seconds)
      .tranId should equal("tranId01")


    Await.result(databaseLayer.RegSub("tranId01", "sub-modId01"), 2.seconds)
      .tranId should equal("tranId01")


    Await.result(databaseLayer.ReqCloseSub("tranId01", "sub-modId01", true), 2.seconds)
      .tranId should equal("tranId01")


    Await.result(databaseLayer.RegSub("tranId01", "sub-modId02"), 2.seconds)
      .tranId should equal("tranId01")


    Await.result(databaseLayer.ReqCloseSub("tranId01", "sub-modId02", false), 2.seconds)
      .tranId should equal("tranId01")


    val ret = Await.result(databaseLayer.tccList(), 2.seconds)



    Await.result(databaseLayer.ReqClose("tranId01", "modId01", true), 2.seconds)
      .tranId should equal("tranId01")

    val ret2 = Await.result(databaseLayer.tccList(), 2.seconds)

    Await.result(databaseLayer.comfirmSub("tranId01", "sub-modId01"), 2.seconds)
      .tranId should equal("tranId01")

    Await.result(databaseLayer.comfirm("tranId01", "modId01"), 2.seconds)
      .tranId should equal("tranId01")





  }


  test("test regSub without reg  or after regClose  ") {




    val caught =
      intercept[RuntimeException] {
        Await.result(databaseLayer.RegSub("tranId02", "sub-modId01"), 2.seconds)

      }
    assert(caught.getMessage.indexOf("ReqSub fail") != -1)


    Await.result(databaseLayer.Reg("tranId02", "modId01"), 2.seconds)
      .tranId should equal("tranId02")



    //afterclose
    Await.result(databaseLayer.ReqClose("tranId02", "modId01", true), 2.seconds)
      .tranId should equal("tranId02")



    val caught2 =
      intercept[RuntimeException] {
        Await.result(databaseLayer.RegSub("tranId02", "sub-modId01"), 2.seconds)

      }
    assert(caught2.getMessage.indexOf("ReqSub fail") != -1)






  }

  test("test cancel after regClose  ") {



    Await.result(databaseLayer.Reg("tranId03", "modId01"), 2.seconds)
      .tranId should equal("tranId03")

    Await.result(databaseLayer.RegSub("tranId03", "sub-modId01"), 2.seconds)
      .tranId should equal("tranId03")

    Await.result(databaseLayer.ReqCloseSub("tranId03", "sub-modId01", false), 2.seconds)
      .tranId should equal("tranId03")


    Await.result(databaseLayer.ReqClose("tranId03", "modId01",false), 2.seconds)
      .tranId should equal("tranId03")


    Await.result(databaseLayer.cancelSub("tranId03", "sub-modId01"), 2.seconds)
      .tranId should equal("tranId03")



  }


  test("test close Main tran  ") {



    Await.result(databaseLayer.Reg("tranId04", "modId01"), 2.seconds)
      .tranId should equal("tranId04")

    Await.result(databaseLayer.RegSub("tranId04", "sub-modId01"), 2.seconds)
      .tranId should equal("tranId04")

    Await.result(databaseLayer.ReqCloseSub("tranId04", "sub-modId01", false), 2.seconds)
      .tranId should equal("tranId04")


    Await.result(databaseLayer.ReqClose("tranId04", "modId01",false), 2.seconds)
      .tranId should equal("tranId04")


    Await.result(databaseLayer.cancelSub("tranId04", "sub-modId01"), 2.seconds)
      .tranId should equal("tranId04")


    Await.result(databaseLayer.closeMainTran(), 20.seconds)

    val t=Await.result(databaseLayer.getTran("tranId04", "modId01"), 20.seconds)

      t.status should equal("cancel")
  }


}
