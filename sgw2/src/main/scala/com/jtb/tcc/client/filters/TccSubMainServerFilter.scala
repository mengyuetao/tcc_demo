package com.jtb.tcc.client.filters

import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.{Request, Response}
import com.twitter.util.Future


class TccSubMainServerFilter extends SimpleFilter[Request,Response] {


  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {


    val tranId= request.headerMap.get("xx-tranId" ).getOrElse("")


    Contexts.local.let(TranInfo.tranCtx, TranId(tranId)){
      service(request)
    }
  }
}