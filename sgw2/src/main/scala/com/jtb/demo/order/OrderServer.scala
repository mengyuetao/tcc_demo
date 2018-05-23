package com.jtb.demo.order

import com.jtb.demo.order.controllers.OrderController
import com.jtb.demo.order.modules.OrderModule

import com.jtb.tcc.client.filters.TccSubMainServerFilter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter


object OrderServerMain extends OrderServer


class OrderServer extends HttpServer {

  override def defaultFinatraHttpPort: String = ":28888"
  override def defaultAdminPort: Int = 29990

  override val modules = Seq(OrderModule)

  override def configureHttp(router: HttpRouter): Unit = {

    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .filter[TccSubMainServerFilter]
      .add[OrderController]
  }

}