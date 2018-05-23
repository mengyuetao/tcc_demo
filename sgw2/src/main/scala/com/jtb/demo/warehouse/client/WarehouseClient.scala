package com.jtb.demo.warehouse.client

import java.util.concurrent.TimeUnit

import com.jtb.demo.warehouse.domain.http.OrderReq
import com.twitter.finagle.Http
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.finatra.httpclient.HttpClient
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.finatra.json.modules.FinatraJacksonModule
import com.twitter.inject.Logging
import com.twitter.inject.app.App
import com.twitter.util.{Await, Duration}


object WarehouseClientMain extends WarehouseClient with Logging

class WarehouseClient extends App {

  override val name = "sample-app"
  override val modules = Seq(FinatraJacksonModule)


  def add1(mapper:FinatraObjectMapper,jsonclient:HttpClient) = {
    //OrderReq(orderId :String , franchiserId:String , productId:String, amount:Int)
    val orderReq = OrderReq("order001", "franchiser001", "product001", 1000)
    val orderString = mapper.writeValueAsString(orderReq)

    val request = RequestBuilder.post("/warehouse/add")
      .header("xx-tranId","tranId001")
      .body(orderString)

    val request2 = RequestBuilder.post("/warehouse/comfirm")
      .header("xx-tranId","tranId001")
      .body(orderString)

    val ret = jsonclient.execute(request)

    val r1 = Await.result(ret,  Duration(100,TimeUnit.SECONDS))
    this.info(r1)

    val ret2 = jsonclient.execute(request2)


    val r2 = Await.result(ret2,  Duration(100,TimeUnit.SECONDS))

    this.info(r2)

  }

  def redu1(mapper:FinatraObjectMapper,jsonclient:HttpClient) = {
    //OrderReq(orderId :String , franchiserId:String , productId:String, amount:Int)
    val orderReq = OrderReq("order002", "franchiser001", "product001", -500)
    val orderString = mapper.writeValueAsString(orderReq)

    val request = RequestBuilder.post("/warehouse/add")
      .header("xx-tranId","tranId002")
      .body(orderString)

    val request2 = RequestBuilder.post("/warehouse/comfirm")
      .header("xx-tranId","tranId002")
      .body(orderString)

    val ret = jsonclient.execute(request)

    val r1 = Await.result(ret,  Duration(100,TimeUnit.SECONDS))
    this.info(r1)

    val ret2 = jsonclient.execute(request2)


    val r2 = Await.result(ret2,  Duration(100,TimeUnit.SECONDS))

    this.info(r2)

  }

  def redu2(mapper:FinatraObjectMapper,jsonclient:HttpClient) = {
    //OrderReq(orderId :String , franchiserId:String , productId:String, amount:Int)
    val orderReq = OrderReq("order003", "franchiser001", "product001", -500)
    val orderString = mapper.writeValueAsString(orderReq)

    val request = RequestBuilder.post("/warehouse/add")
      .header("xx-tranId","tranId003")
      .body(orderString)

    val request2 = RequestBuilder.post("/warehouse/cancel")
      .header("xx-tranId","tranId003")
      .body(orderString)

    val ret = jsonclient.execute(request)

    val r1 = Await.result(ret,  Duration(100,TimeUnit.SECONDS))
    this.info(r1)

    val ret2 = jsonclient.execute(request2)


    val r2 = Await.result(ret2,  Duration(100,TimeUnit.SECONDS))

    this.info(r2)

  }

  def redu3(mapper:FinatraObjectMapper,jsonclient:HttpClient) = {
    //OrderReq(orderId :String , franchiserId:String , productId:String, amount:Int)
    val orderReq = OrderReq("order004", "franchiser001", "product001", -600)
    val orderString = mapper.writeValueAsString(orderReq)

    val request = RequestBuilder.post("/warehouse/add")
      .header("xx-tranId","tranId004")
      .body(orderString)

    val request2 = RequestBuilder.post("/warehouse/cancel")
      .header("xx-tranId","tranId004")
      .body(orderString)

    val ret = jsonclient.execute(request)

    val r1 = Await.result(ret,  Duration(100,TimeUnit.SECONDS))
    this.info(r1)

    //val ret2 = jsonclient.execute(request2)


    //val r2 = Await.result(ret2,  Duration(100,TimeUnit.SECONDS))

    //this.info(r2)

  }


  override protected def run(): Unit = {

    //add， cancel， confirm
    val client = Http.client.newService("127.0.0.1:18888", "")
    val mapper = this.injector.instance[FinatraObjectMapper]
    val jsonclient = new HttpClient("hostname", client, None, Map(), mapper)

    //添加库存

    add1(mapper,jsonclient)   // add 1000 comfirm
    redu1(mapper,jsonclient)  // sub 500  comfirm
    redu2(mapper,jsonclient)   //sub 500 cancel

    //500 here

    redu3(mapper,jsonclient)   //sub 600 fail

    //500 here


  }

}
