package com.jtb.tcc.integrate

import com.jtb.common.database.Profile

import com.jtb.tcc.controllers.TranController
import com.jtb.tcc.database.{TccRep, TccRepJdbc}
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.inject.Injector
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile
import com.jtb.tcc.modules.TranServiceModule

object TestTccServerMain extends TestTccServer


object TranTestServiceModule extends TranServiceModule {

  class DatabaseLayer(val profile: JdbcProfile,
                      val db: slick.jdbc.JdbcBackend.Database) extends Profile with TccRepJdbc

  override def configure() {
      val db = Database.forConfig("chapter02")
      val databaseLayer = new DatabaseLayer(slick.jdbc.H2Profile, db)
      databaseLayer.createSchema()

      bind[TccRep].toInstance( databaseLayer)


  }

}

class TestTccServer extends HttpServer {

  override val modules = Seq(TranTestServiceModule)

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .add[TranController]
  }
}
