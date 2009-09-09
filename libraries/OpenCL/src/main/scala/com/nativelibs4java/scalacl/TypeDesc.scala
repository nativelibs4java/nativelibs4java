package com.nativelibs4java.scalacl

case class TypeDesc(channels: Int, valueType: ValueType, primType: PrimType) {
  def combineWith(o: TypeDesc): TypeDesc = {
    var cn = 0;
      if (channels != o.channels) {
        if (o.channels == 1)
          cn = channels;
        else if (channels == 1)
          cn = o.channels;
        else
          throw new RuntimeException("Mismatching channels : " + channels +" vs. " + o.channels)
      } else
        cn = channels;
      
      return TypeDesc(cn, valueType combineWith o.valueType, primType combineWith o.primType)
  }
  def globalCType = primType.toString + (if (valueType != Scalar) "*" else "")
}