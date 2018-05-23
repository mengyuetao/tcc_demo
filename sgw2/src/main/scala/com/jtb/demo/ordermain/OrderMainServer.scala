package com.jtb.demo.ordermain

import com.jtb.demo.ordermain.controller.OrderMainController
import com.jtb.demo.ordermain.filter.OrderTccMainFilter
import com.jtb.demo.ordermain.modules.OrderMainModule
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

object OrderMainServerMain extends OrderMainServer
class OrderMainServer extends HttpServer {


  override def defaultFinatraHttpPort: String = ":38888"
  override def defaultAdminPort: Int = 39990


  override val modules = Seq(OrderMainModule)

  override protected def configureHttp(router: HttpRouter): Unit = {

    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .filter[OrderTccMainFilter]
      .add[OrderMainController]

  }
}
