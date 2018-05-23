package com.jtb.demo.warehouse

import com.jtb.demo.warehouse.controllers.WarehouseController
import com.jtb.demo.warehouse.modules.WarehouseModule
import com.jtb.tcc.client.filters.TccSubMainServerFilter
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter


object WareHouseServerMain extends WarehouseServer


class WarehouseServer extends HttpServer {

  override def defaultFinatraHttpPort: String = ":18888"
  override def defaultAdminPort: Int = 19990

  override val modules = Seq(WarehouseModule)

  override def configureHttp(router: HttpRouter): Unit = {

    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .filter[TccSubMainServerFilter]
      .add[WarehouseController]
  }

}