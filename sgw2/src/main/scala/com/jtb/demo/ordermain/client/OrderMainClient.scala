package com.jtb.demo.ordermain.client

import java.util.concurrent.TimeUnit

import com.jtb.demo.warehouse.domain.http.OrderReq
import com.twitter.finagle.{Http}
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Logging
import com.twitter.inject.app.App
import com.twitter.util.{Await, Duration}


object OrderClienttMain extends OrderMainClient with Logging

class OrderMainClient extends App {

  override val name = "sample-app"
  override val modules = Seq(FinatraJacksonModule)

  def order1(mapper: FinatraObjectMapper, jsonclient: HttpClient , mun:Int) = {
    //OrderReq(orderId :String , franchiserId:String , productId:String, amount:Int)
    val orderReq = OrderReq("order001", "franchiser001", "product001", mun)
    val orderString = mapper.writeValueAsString(orderReq)
    val request = RequestBuilder.post("/order/main/order")
      .body(orderString)
    val ret = jsonclient.execute(request)
    val r1 = Await.result(ret, Duration(100, TimeUnit.SECONDS))
    this.info(r1)
  }




  override protected def run(): Unit = {

    //add， cancel， confirm
    val client = Http.client.newService("127.0.0.1:38888", "")
    val mapper = this.injector.instance[FinatraObjectMapper]
    val jsonclient = new HttpClient("hostname", client, None, Map(), mapper)

    order1(mapper, jsonclient,1000)
    //order1(mapper, jsonclient,2000)


  }

}