package com.jtb.tcc


import com.jtb.tcc.controllers.TranController

import com.jtb.tcc.modules.TranPdtServiceModule
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter



object TranServerMain extends TranServer




class TranServer extends HttpServer {


  override def defaultFinatraHttpPort: String = ":48888"
  override def defaultAdminPort: Int = 49990

  override val modules = Seq(TranPdtServiceModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[TranController]
  }
}
