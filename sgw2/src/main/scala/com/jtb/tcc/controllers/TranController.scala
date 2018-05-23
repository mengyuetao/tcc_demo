package com.jtb.tcc.controllers

import javax.inject.Inject

import com.jtb.tcc.domain.http.{ModIdRequest, SubTranResp, TranCloseRequest, TranRequest}
import com.jtb.tcc.services.TranService
import com.twitter.finagle.http.Request
import com.twitter.finatra.http.Controller
import com.twitter.inject.Logging
import com.twitter.util.Future

class TranController @Inject()(tranService: TranService) extends Controller with Logging {


  post("/tran/start") { r: ModIdRequest =>
    val ret= tranService.startTran(r.modId).flatMap{
      x => Future(SubTranResp(x.modId,x.Id) )
    }
    ret.onFailure{
      case x:Throwable =>  error(x)
    }

    ret
  }

  post("/tran/close") { r: TranCloseRequest =>
    tranService.closeTran(r.tranId, r.modId, r.tryCommit)
  }

  post("/tran/sub/start") { r: TranRequest =>
    val ret =tranService.startSubTran(r.tranId, r.modId).flatMap{
      x => Future(SubTranResp(x.modId,x.Id) )
    }

    ret.onFailure{
      case x:Throwable =>  error(x)
    }
  }

  post("/tran/sub/close") { r: TranCloseRequest =>
    tranService.closeSubTran(r.tranId, r.modId, r.tryCommit)
  }

  post("/tran/sub/cancel") { r: TranRequest =>
    tranService.cancelSub(r.tranId, r.modId )
  }

  post("/tran/sub/comfirm") { r: TranRequest =>
    tranService.comfirmSub(r.tranId, r.modId )
  }


}
