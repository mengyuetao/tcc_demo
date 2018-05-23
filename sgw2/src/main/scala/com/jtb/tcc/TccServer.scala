package com.jtb.tcc

import java.util.concurrent.TimeUnit

import com.jtb.demo.warehouse.domain.http.OrderReq
import com.jtb.tcc.client.filters.TranCloseRequest
import com.jtb.tcc.domain.Tcc
import com.jtb.tcc.domain.http.TranRequest
import com.jtb.tcc.modules.TranPdtServiceModule
import com.jtb.tcc.services.TranService
import com.twitter.finagle.Http
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Logging
import com.twitter.inject.app.App
import com.twitter.util.{Await, Duration}


object TccServerMain extends TccServer

class TccServer extends App with Logging {

  override val name = "sample-app"
  override val modules = Seq(FinatraJacksonModule, TranPdtServiceModule)

  def cancelorcomfirm(mapper: FinatraObjectMapper, jsonclient: HttpClient, jsonclientTcc: HttpClient, url: String, tranId: String, modId: String, urltcc: String) = {


    val request2 = RequestBuilder.post(url)
      .header("xx-tranId", tranId)
      .header("xx-modId",modId)
      .body("[]")

    val ret2 = jsonclient.execute(request2)
    val r2 = Await.result(ret2, Duration(100, TimeUnit.SECONDS))
    this.info(r2)


    val r = RequestBuilder.post(urltcc).body(mapper.writeValueAsString(
      TranRequest(tranId, modId)))
    val ret3 = jsonclientTcc.execute(r)
    val r3 = Await.result(ret3, Duration(100, TimeUnit.SECONDS))
    this.info(r3)
  }


  override protected def run(): Unit = {

    //add， cancel， confirm
    val mapper = this.injector.instance[FinatraObjectMapper]

    val client = Http.client.newService("127.0.0.1:58888", "")
    val jsonclient = new HttpClient("hostname", client, None, Map(), mapper)


    val client2 = Http.client.newService("127.0.0.1:48888", "")
    val jsonclient2 = new HttpClient("hostname", client2, None, Map(), mapper)


    val tranService = this.injector.instance[TranService]


    val trans = tranService.list()

    val r1: Seq[Tcc] = Await.result(trans, Duration(100, TimeUnit.SECONDS))


    //代码可以重入，不用考虑太多异常？

    r1.foreach(z => {

      debug(z)

      val url = "/tran/" + z.action
      val urltcc = "/tran/sub/" + z.action
      cancelorcomfirm(mapper, jsonclient, jsonclient2, url, z.tranId, z.modId, urltcc)


    }
    )

    //查询所有待处理的tran

    //cancel or comfirm


  }

}
