package com.jtb.tcc.domain

case class Tran( Id:String,modId:String,state:Tran.State)


object Tran {

  sealed trait State

  object Processing extends State
  object Ok extends State
  object Fail extends State
  object Cancel extends State
  object Comfirm extends State

}