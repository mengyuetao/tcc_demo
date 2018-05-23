package com.jtb.common.database

import slick.jdbc.JdbcProfile

trait  Profile {
  val profile: JdbcProfile
}

trait  DatabaseX {
  val db: slick.jdbc.JdbcBackend.Database


}

trait  LogDbFail {  self: Profile =>

   import profile.api._
   def dbIoFail(msg: String )= DBIO.failed(new RuntimeException(msg))
}

