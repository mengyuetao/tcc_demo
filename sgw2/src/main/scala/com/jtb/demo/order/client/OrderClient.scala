package com.jtb.demo.order.client

import java.util.concurrent.TimeUnit

import com.jtb.demo.warehouse.domain.http.OrderReq
import com.jtb.tcc.modules.TranPdtServiceModule
import com.twitter.finagle.Http
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Logging
import com.twitter.inject.app.App
import com.twitter.util.{Await, Duration}


object OrderClienttMain extends OrderClient with Logging

class OrderClient extends App {

  override val name = "sample-app"
  override val modules = Seq(FinatraJacksonModule, TranPdtServiceModule)

  def order1(mapper: FinatraObjectMapper, jsonclient: HttpClient) = {
    //OrderReq(orderId :String , franchiserId:String , productId:String, amount:Int)
    val orderReq = OrderReq("order001", "franchiser001", "product001", 1000)
    val orderString = mapper.writeValueAsString(orderReq)

    val request = RequestBuilder.post("/order/create")
      .header("xx-tranId", "tranId001")
      .body(orderString)

    val request2 = RequestBuilder.post("/order/comfirm")
      .header("xx-tranId", "tranId001")
      .body(orderString)

    val ret = jsonclient.execute(request)
    val r1 = Await.result(ret, Duration(100, TimeUnit.SECONDS))
    this.info(r1)
    val ret2 = jsonclient.execute(request2)
    val r2 = Await.result(ret2, Duration(100, TimeUnit.SECONDS))
    this.info(r2)
  }

  def order2(mapper: FinatraObjectMapper, jsonclient: HttpClient) = {
    //OrderReq(orderId :String , franchiserId:String , productId:String, amount:Int)
    val orderReq = OrderReq("order002", "franchiser001", "product001", -500)
    val orderString = mapper.writeValueAsString(orderReq)

    val request = RequestBuilder.post("/order/create")
      .header("xx-tranId", "tranId002")
      .body(orderString)

    val request2 = RequestBuilder.post("/order/cancel")
      .header("xx-tranId", "tranId002")
      .body(orderString)

    val ret = jsonclient.execute(request)
    val r1 = Await.result(ret, Duration(100, TimeUnit.SECONDS))
    this.info(r1)
    val ret2 = jsonclient.execute(request2)
    val r2 = Await.result(ret2, Duration(100, TimeUnit.SECONDS))
    this.info(r2)
  }


  override protected def run(): Unit = {

    //add， cancel， confirm
    val client = Http.client.newService("127.0.0.1:28888", "")
    val mapper = this.injector.instance[FinatraObjectMapper]
    val jsonclient = new HttpClient("hostname", client, None, Map(), mapper)

    try{
      order1(mapper, jsonclient)
    }catch {
      case e =>
        error(e)
    }

    order2(mapper, jsonclient)

  }

}
