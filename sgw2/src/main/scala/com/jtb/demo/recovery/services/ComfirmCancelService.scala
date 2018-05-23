package com.jtb.demo.recovery.services

import com.twitter.util.Future

trait ComfirmCancelService {

  def modId:String
  def comfirm(tranId:String):Future[Unit]
  def cancel(tranId:String):Future[Unit]


}


