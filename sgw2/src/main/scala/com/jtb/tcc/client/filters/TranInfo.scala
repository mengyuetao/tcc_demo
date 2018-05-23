package com.jtb.tcc.client.filters

import com.twitter.finagle.context.Contexts


object TranInfo {

  val tranCtx = new Contexts.local.Key[TranId]
  val EmptyTranCtxFn = () => TranId.empty
  def tranId:TranId = Contexts.local.getOrElse(tranCtx,EmptyTranCtxFn)


}

case class TranId(id:String)

object TranId {
  val empty = TranId("")
}
