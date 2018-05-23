package com.jtb.demo.recovery

import com.jtb.demo.recovery.controller.RecoveryController

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

object RecoveryServerMain extends RecoveryServer
class RecoveryServer extends HttpServer {

  override def defaultFinatraHttpPort: String = ":58888"
  override def defaultAdminPort: Int = 59990

  override val modules = Seq()

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[RecoveryController]
  }
}
