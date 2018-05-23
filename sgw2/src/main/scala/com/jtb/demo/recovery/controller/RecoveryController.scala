package com.jtb.demo.recovery.controller

import javax.inject.Inject

import com.jtb.tcc.client.filters.TranContext
import com.twitter.finagle.Http
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging

class RecoveryController @Inject() (mapper: FinatraObjectMapper) extends Controller with Logging {


  lazy val warehouseClient = new HttpClient("hostname", Http.client.newService("127.0.0.1:18888", ""), None, Map(), mapper)
  lazy val orderClient = new HttpClient("hostname", Http.client.newService("127.0.0.1:28888", ""), None, Map(), mapper)

  post("/tran/cancel") { req: Request =>

    val modId = req.headerMap.get("xx-modId").getOrElse("")


    val (r, c) = modId match {
      case "/order/create" => (RequestBuilder.post("/order/cancel").body("""[]"""), orderClient)
      case " /warehouse/add" => (RequestBuilder.post(" /warehouse/cancel").body("""[]"""), warehouseClient)
    }

    c.execute(r.header("xx-tranId", req.headerMap.get("xx-tranId").getOrElse("")))

  }

  post("/tran/comfirm") { req: Request =>

    val modId = req.headerMap.get("xx-modId").getOrElse("")


    val (r, c) = modId match {
      case "/order/create" => (RequestBuilder.post("/order/comfirm").body("""[]"""), orderClient)
      case "/warehouse/add" => (RequestBuilder.post("/warehouse/comfirm").body("""[]"""), warehouseClient)
    }

    c.execute(r.header("xx-tranId", req.headerMap.get("xx-tranId").getOrElse("")))

  }

}
