package com.eniro.states

sealed trait SYSTEM
case object SALESFORCEDGS extends SYSTEM
case object SALESFORCEKRAK extends SYSTEM
case object DAISYDGS extends SYSTEM
case object DAISYKRAK extends SYSTEM
case object POWERLISTING extends SYSTEM
case object NONCUSTOMER extends SYSTEM
case object LENGTHONE extends SYSTEM
case object NONE extends SYSTEM

sealed trait OP
case object AND extends OP
case object NEG extends OP

object Systems {

  def setStates(customerStatus: String): Map[SYSTEM, Boolean] = {
    Map(
      SALESFORCEDGS -> customerStatus.contains("salesforce_DGS"),
      SALESFORCEKRAK -> customerStatus.contains("salesforce_Krak"),
      DAISYDGS -> customerStatus.contains("DaisyDgs"),
      DAISYKRAK -> customerStatus.contains("DaisyKrak"),
      POWERLISTING -> customerStatus.contains("powerlisting"),
      NONCUSTOMER -> customerStatus.contains("noncustomer"),
      LENGTHONE -> (customerStatus.length <= 1)
    )
  }

  type Condition = (SYSTEM, SYSTEM, OP)

  def eval(condition: Condition)(states: Map[SYSTEM, Boolean]): Boolean = {
    val op1 = states.get(condition._1)
    val op2 = states.get(condition._2)
    (op1, op2, condition._3) match {
      case (Some(b1), Some(b2), AND) => b1 && b2
      case (Some(b1), Some(b2), NEG) => b1 && !b2
      case (Some(b1), None, _) => b1
      case (_, _, _) => false
    }
  }

  val rules: List[(List[Condition], Set[String], Boolean)] = List(
    (List((NONCUSTOMER, POWERLISTING, AND)), Set("krak", "dgs"), true),
    (List((LENGTHONE, NONE, AND)), Set("other"), true),
    (List((SALESFORCEDGS, SALESFORCEKRAK, AND)), Set("krak", "dgs"), true),
    (List((SALESFORCEDGS, NONE, AND)), Set("dgs"), false),
    (List((SALESFORCEDGS, DAISYKRAK, NEG)), Set("krak"), true),
    (List((POWERLISTING, DAISYDGS, AND)), Set("krak"), true),
    (List((POWERLISTING, DAISYKRAK, AND)), Set("dgs"), true)
  )

  def evalRules(customerStatus: String): Set[String] = {
    val states: Map[SYSTEM, Boolean] = setStates(customerStatus)
    rules.foldLeft(Set.empty[String])((set, rule) => {
      val ruleConditions: List[Condition] = rule._1
      val filteredStates = states.filter(s => {
        val l = ruleConditions.flatMap(cond => List(cond._1, cond._2))
        l.contains(s._1)
      })
      val result = ruleConditions.forall(c => eval(c)(filteredStates))
      if (result && rule._3) return set ++ rule._2 // `return` for early exit
      else if (result && !rule._3) set ++ rule._2 // continue to next (append)
      else set
    })
  }

  def main(args: Array[String]): Unit = {
    println(evalRules("noncustomer powerlisting"))
    println(evalRules("salesforce_DGS"))
    println(evalRules("salesforce_DGS, DaisyKrak"))
    println(evalRules("s"))
  }

}
