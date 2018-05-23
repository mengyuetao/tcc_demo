package com.jtb.demo.warehouse.controllers

import javax.inject.Inject

import com.fasterxml.jackson.databind.JsonNode
import com.jtb.demo.warehouse.domain.Order
import com.jtb.demo.warehouse.domain.http.OrderReq
import com.jtb.demo.warehouse.services.services.WarehouseService
import com.jtb.tcc.client.filters.{TranId, TranInfo}
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.inject.Logging
import com.twitter.util.{Future, Throw}

import scala.util.parsing.json.JSONObject

class WarehouseController @Inject()(warehouse: WarehouseService, mapper: FinatraObjectMapper) extends Controller with Logging {


  post("/warehouse/add") { r: OrderReq =>

    val tranId = TranInfo.tranId
    if (tranId == TranId.empty) {
      Future(Throw(new RuntimeException("/warehouse/add need tranId")))
    } else {
      warehouse.addWareHouse(Order(r.orderId,r.franchiserId,r.productId,r.amount,tranId.id) ).onFailure(
        e => error(e)
      )
    }
  }



  post("/warehouse/comfirm") { r:Request =>
    val tranId = TranInfo.tranId
    if (tranId == TranId.empty) {
      Future(Throw(new RuntimeException("/warehouse/comfirm need tranId")))
    } else {
      warehouse.comfirmWareHouse(tranId.id)
    }

  }


  post("/warehouse/cancel") { r:Request =>
    val tranId = TranInfo.tranId
    if (tranId == TranId.empty) {
      Future(Throw(new RuntimeException("/warehouse/cancel need tranId")))
    } else {
      warehouse.cancelWareHouse(tranId.id)
    }

  }
}

