package com.jtb.tcc.domain.http


case class TranRequest (tranId: String,modId:String)
case class TranCloseRequest (tranId: String,modId:String,tryCommit:Boolean)
