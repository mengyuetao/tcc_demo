package com.jtb.tcc.modules

import com.google.inject.{Provides, Singleton}
import com.jtb.tcc.database.TccRep
import com.jtb.tcc.services.{TranService, TranServiceImpl}
import com.twitter.inject.TwitterModule

abstract class TranServiceModule extends TwitterModule {



  @Singleton
  @Provides
  def provideTranService( tcc:TccRep ): TranService = {

    new TranServiceImpl {
      override val tccRep: TccRep = tcc
    }

  }


}