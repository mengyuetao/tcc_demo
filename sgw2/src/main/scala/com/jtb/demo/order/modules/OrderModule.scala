package com.jtb.demo.order.modules

import com.google.inject.{Provides, Singleton}
import com.jtb.common.database.Profile
import com.jtb.demo.order.database.{OrderRep, OrderRepJdbc}
import com.jtb.demo.order.services.{OrderServcie, OrderServcieImpl}

import com.twitter.inject.TwitterModule
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile


class DatabaseLayer(val profile: JdbcProfile,
                    val db: slick.jdbc.JdbcBackend.Database) extends Profile with OrderRepJdbc

object OrderModule extends OrderModule

class OrderModule extends TwitterModule {

  @Singleton
  @Provides
  def provideOrderServcie(): OrderServcie = {
    val db = Database.forConfig("mysql")
    val databaseLayer = new DatabaseLayer(slick.jdbc.MySQLProfile, db)
    new OrderServcieImpl {
      def orderRep:OrderRep = databaseLayer
    }
  }
}
