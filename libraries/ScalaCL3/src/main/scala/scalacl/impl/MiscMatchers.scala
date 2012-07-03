package scalacl
package impl

trait MiscMatchers {
  val global: reflect.api.Universe
  import global._
  
  object WhileLoop {
    def unapply(tree: Tree) = tree match {
      case
        LabelDef(
          lab,
          List(),
          If(
            condition,
            Block(
              content,
              Apply(
                Ident(lab2),
                List()
              )
            ),
            Literal(Constant(()))
          )
        ) if (lab == lab2) =>
        Some(condition, content)
      case _ =>
        None
    }
  }
}
