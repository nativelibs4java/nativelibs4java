/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl



//class ViewCol
trait CLView[T, C <: CLCol[T]] extends CLCol[T] {
    def force: C
}