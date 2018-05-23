package com.jtb.tcc.modules

import com.jtb.common.database.Profile
import com.jtb.tcc.database.{TccRep, TccRepJdbc}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.JdbcProfile

object TranPdtServiceModule extends TranServiceModule {

  class DatabaseLayer(val profile: JdbcProfile,
                      val db: slick.jdbc.JdbcBackend.Database) extends Profile with TccRepJdbc



  override def configure() {
    val db = Database.forConfig("mysql")
    val databaseLayer = new DatabaseLayer(slick.jdbc.MySQLProfile, db)


    bind[TccRep].toInstance( databaseLayer)


  }
}