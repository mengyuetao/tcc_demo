package com.jtb.tcc.client.filters

import com.twitter.finagle.context.Contexts
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.httpclient.{HttpClient, RequestBuilder}
import com.twitter.finatra.json.FinatraObjectMapper
import com.twitter.util.Future


class TccSubMainFilter(tccClient: HttpClient, mapper: FinatraObjectMapper) extends SimpleFilter[Request, Response] {


  private def tranId: TranId = TranInfo.tranId

  override def apply(request: Request, service: Service[Request, Response]): Future[Response] = {

    //获取TranId，设置SubTranId
    if (tranId == TranId.empty) {
      Future.exception(new RuntimeException("tranid doesn't exist"))
    } else {
      val subTranId = request.path
      request.headerMap.add("xx-tranId", tranId.id)
      request.headerMap.add("xx-subTranId", subTranId)


      val r = RequestBuilder.post("/tran/sub/start").body(mapper.writeValueAsString(TranContext(tranId.id,request.path)))

      val ret = tccClient.executeJson[TranContext](r)
      ret.flatMap { tranContext =>
        val tranCtx = TranInfo.tranCtx
        Contexts.local.let(tranCtx, TranId(tranContext.tranId)) {
          service(request).onSuccess { _ =>

            val r = RequestBuilder.post("/tran/sub/close").body(mapper.writeValueAsString(
              TranCloseRequest(tranContext.tranId, request.path, true)))
            tccClient.execute(r)
          }
            .onFailure { _ =>

              val r = RequestBuilder.post("/tran/sub/close").body(mapper.writeValueAsString(
                TranCloseRequest(tranContext.tranId, request.path, false)))
              tccClient.execute(r)
            }
        }
      }
    }


  }
}


