package scalacl.impl

import language.experimental.macros

import reflect.makro.Context

object KernelMacros {
  def kernelImpl(c: Context)(block: c.Expr[Unit])(context: c.Expr[scalacl.Context]): c.Expr[Unit] = {
    c.reify {
      {}
    }
  }
  def taskImpl(c: Context)(block: c.Expr[Unit])(context: c.Expr[scalacl.Context]): c.Expr[Unit] = {
    kernelImpl(c)(block)(context)
  }
}