package com.jtb.demo.ordermain.controller

import javax.inject.Inject

import com.jtb.demo.ordermain.anno.{Order, Warehouse}
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.Logging
import com.jtb.demo.ordermain.domain.OrderReq
import com.twitter.finatra.json.FinatraObjectMapper

/**
  * 提供入库，发货操作服务
  *
  * 调用订单，库存子服务
  *
  *
  */
class OrderMainController @Inject()(@Order orderService: Service[Request, Response],
                                    @Warehouse warehouseService: Service[Request, Response],
                                    mapper: FinatraObjectMapper
                                   ) extends Controller with Logging {

  post("/order/main/order") { r: OrderReq =>


    debugResult("/order/main/order request: %s ") {
      r
    }


    val orderString = mapper.writeValueAsString(r)

    val r1 = RequestBuilder.post("/order/create").body(orderString)
    val orderRep = orderService(r1)
    orderRep.onFailure(e => error(e))


    val r2 = RequestBuilder.post("/warehouse/add").body(orderString)
    val warehouseRep = warehouseService(r2)
    warehouseRep.onFailure(e => error(e))

    debugFutureResult("invoke orderService response %s")(orderRep)
    debugFutureResult("invoke warehouseRep response %s")(warehouseRep)


    orderRep.flatMap(_ => warehouseRep)


  }

}
