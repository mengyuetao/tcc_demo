package com.jtb.demo.order.controllers

import javax.inject.Inject

import com.jtb.demo.order.domain.http.{OrderReq, OrderTranId}
import com.jtb.demo.order.services.OrderServcie
import com.jtb.tcc.client.filters.{TranId, TranInfo}
import com.twitter.finatra.http.Controller
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.{Future, Throw}
import com.jtb.demo.order.domain.Order
import com.twitter.finagle.http.Request

class OrderController @Inject()(
                                 orderServcie: OrderServcie, mapper: FinatraObjectMapper) extends Controller {

  //创建发货单
  post("/order/create") { r: OrderReq =>

    val tranId = TranInfo.tranId
    if (tranId == TranId.empty) {
      Future(Throw(new RuntimeException("/warehouse/add need tranId")))
    } else {
      orderServcie.createOrder(Order(r.orderId,r.franchiserId,r.productId,r.amount,tranId.id) ).onFailure(
        e => error(e)
      )
    }

  }

  //确认
  post("/order/comfirm") { r:Request =>

    val tranId = TranInfo.tranId
    if (tranId == TranId.empty) {
      Future(Throw(new RuntimeException("/order/comfirm need tranId")))
    } else {
      orderServcie.comfirmOrder(tranId.id)

    }


  }

  //取消
  post("/order/cancel") { r:Request =>
    val tranId = TranInfo.tranId
    if (tranId == TranId.empty) {
      Future(Throw(new RuntimeException("/order/comfirm need tranId")))
    } else {
      orderServcie.cancelOrder(tranId.id)
    }
  }

}
