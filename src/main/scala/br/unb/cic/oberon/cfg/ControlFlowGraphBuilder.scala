package br.unb.cic.oberon.cfg

import br.unb.cic.oberon.ast.{IfElseStmt, Procedure, SequenceStmt, Statement, WhileStmt}
import scalax.collection.mutable.Graph
import scalax.collection.GraphEdge
import scalax.collection.GraphPredef.EdgeAssoc

trait GraphNode

case class StartNode() extends GraphNode
case class SimpleNode(stmt: Statement) extends GraphNode
case class EndNode() extends GraphNode

trait ControlFlowGraphBuilder {
  def createControlFlowGraph(procedure: Procedure): Graph[GraphNode, GraphEdge.DiEdge] // Graph
  def createControlFlowGraph(stmt: Statement): Graph[GraphNode, GraphEdge.DiEdge] // Graph
}

class IntraProceduralGraphBuilder extends ControlFlowGraphBuilder {
  override def createControlFlowGraph(procedure: Procedure): Graph[GraphNode, GraphEdge.DiEdge] = {
    createControlFlowGraph(procedure.stmt)
  }

  override def createControlFlowGraph(stmt: Statement): Graph[GraphNode, GraphEdge.DiEdge] = {
    var g = Graph[GraphNode, GraphEdge.DiEdge]()

    stmt match {
      case SequenceStmt(stmts) =>
        g += StartNode() ~> SimpleNode(stmts.head)
        createControlFlowGraph(stmts, g)
      case _ =>
        g += StartNode() ~> SimpleNode(stmt)
        g += SimpleNode(stmt) ~> EndNode()
    }
  }

  /**
   * A recursive definition that generates a graph from a list of stmts
   * @param stmts list of statements
   * @param g a graph used as accumulator
   * @return a new version of the graph
   */
  def createControlFlowGraph(stmts: List[Statement], g: Graph[GraphNode, GraphEdge.DiEdge]): Graph[GraphNode, GraphEdge.DiEdge] =
    stmts match {
      case s1 :: s2 :: rest => {                     // case the list has at least two elements. this is the recursive case
          s1 match {
            case IfElseStmt(_, _, Some(_)) => {      // in this case, we do not create an edge from s1 -> s2
              processStmtNode(s1, Some(s2), g)
              createControlFlowGraph(s2 :: rest, g)  
            }
            case _ => {
              g += SimpleNode(s1) ~> SimpleNode(s2)
              processStmtNode(s1, Some(s2), g)
              createControlFlowGraph(s2 :: rest, g)
            }
          }
        }
        case s1 :: List() => {                       // case the list has just one element. this is the base case
          g += SimpleNode(s1) ~> EndNode()
          processStmtNode(s1, None, g)                     // process the singleton node of the stmts list of statements
          return g
        }
      }


  /**
   * Handle the specific logic for building a graph from the "from" stmt
   * @param from the from statement
   * @param target the optional target statement
   * @param g the cumulative graph
   * @return a new version of the graph
   */
  def processStmtNode(from: Statement, target: Option[Statement], g: Graph[GraphNode, GraphEdge.DiEdge]): Graph[GraphNode, GraphEdge.DiEdge] = {
    from match {
      case IfElseStmt(_, thenStmt, optionalElseStmt) => {
        processStmtNode(from, thenStmt, target, g)
        if(optionalElseStmt.isDefined) {
          processStmtNode(from, optionalElseStmt.get, target, g)
        }
        g   // returns g
      }
      case WhileStmt(_, whileStmt) => {
        processStmtNode(from, whileStmt, target, g)
      }
      // TODO: write here the remaining "compound" stmts: e.g.,: ForStmt, CaseStmt, ...
      //       This is particularly important fro groups 04 and 09.
      case _ => g    // if not a compound stmt (e.g., procedure call, assignment, ...), just return the graph g
     }
  }

  /**
   * Deals with the cases where the "target" statement is a sequence stmt
   * @param from the from statement
   * @param target the target statement
   * @param end the possible "end" statement
   * @param g the cumulative graph
   * @return a new version of the graph.
   */
  def processStmtNode(from: Statement, target: Statement, end: Option[Statement], g: Graph[GraphNode, GraphEdge.DiEdge]): Graph[GraphNode, GraphEdge.DiEdge] = {
    target match {
      case SequenceStmt(stmts) => {             // if the target is a SequenceStmt, we have to create "sub-graph"
        g += SimpleNode(from) ~> SimpleNode(stmts.head)                 // create an edge from "from" to the first elements of the list stmts
        if(end.isDefined) {
          g += SimpleNode(stmts.last) ~> SimpleNode(end.get)
        }
        createControlFlowGraph(stmts, g)
      }
      case _ => {
        g += SimpleNode(from) ~> SimpleNode(target)
        if(end.isDefined) {
          g += SimpleNode(target) ~> SimpleNode(end.get)
        }
        g  // remember, the last statement corresponds to the "return statement"
      }
   }
  }
}
