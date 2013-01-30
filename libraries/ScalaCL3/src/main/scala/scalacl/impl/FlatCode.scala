package scalacl
package impl
  
object FlatCodes {
  
  def merge[T](fcs: FlatCode[T]*)(f: Seq[T] => Seq[T]) =
    fcs.reduceLeft(_ ++ _).mapValues(f)
 
}
case class FlatCode[T](
  /// External functions that are referenced by statements and / or values 
  outerDefinitions: Seq[T] = Seq(), 
  /// List of variable definitions and other instructions (if statements, do / while loops...)
  statements: Seq[T] = Seq(), 
  /// Final values of the code in a "flattened tuple" style
  values: Seq[T] = Seq()
) {
  def mapEachValue(f: T => Seq[T]): FlatCode[T] =
    copy(values = values.flatMap(f))
  
  def mapValues(f: Seq[T] => Seq[T]): FlatCode[T] =
    copy(values = f(values))
  
  def ++(fc: FlatCode[T]) =
    FlatCode(outerDefinitions ++ fc.outerDefinitions, statements ++ fc.statements, values ++ fc.values)

  def >>(fc: FlatCode[T]) =
        FlatCode(outerDefinitions ++ fc.outerDefinitions, statements ++ fc.statements ++ values, fc.values)

  def noValues =
        FlatCode(outerDefinitions, statements ++ values, Seq())

  def addOuters(outerDefs: Seq[T]) =
    copy(outerDefinitions = outerDefinitions ++ outerDefs)
    
  def addStatements(stats: Seq[T]) = 
    copy(statements = statements ++ stats)

  def printDebug(name: String = "") = {
    def pt(seq: Seq[T]) = println("\t" + seq.map(_.toString.replaceAll("\n", "\n\t")).mkString("\n\t"))
    println("FlatCode(" + name + "):")
    pt(outerDefinitions)
    println("\t--")
    pt(statements)
    println("\t--")
    pt(values)
  }
}

