package com.jtb.demo.warehouse.modules

import com.google.inject.{Provides, Singleton}
import com.jtb.common.database.Profile
import com.jtb.demo.warehouse.database.{WarehouseRep, WarehouseRepJdbc}
import com.jtb.demo.warehouse.services.services.{WarehouseService, WarehouseServiceImpl}
import com.twitter.inject.TwitterModule
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile


class DatabaseLayer(val profile: JdbcProfile,
                    val db: slick.jdbc.JdbcBackend.Database) extends Profile with WarehouseRepJdbc

object WarehouseModule extends WarehouseModule

class WarehouseModule extends TwitterModule {

  @Singleton
  @Provides
  def provideWarehouseService(): WarehouseService = {
    val db = Database.forConfig("mysql")
    val databaseLayer = new DatabaseLayer(slick.jdbc.MySQLProfile, db)
    new WarehouseServiceImpl {
      def warehouseRep: WarehouseRep = databaseLayer
    }
  }
}
