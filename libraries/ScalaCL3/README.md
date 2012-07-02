ScalaCLÉ v3 (yeah, yet another rewrite from scratch FTW!)

Features of the new design:
- Much better asynchronicity support (now requires OpenCL 1.1), and much better performance in general
- Support for captures of constants *and* OpenCL arrays
- Support for lazy clones (.zip will be free!)
- Kernels are now specialized on static input / output / captures types
- ScalaCL Collections no longer fit in regular Scala Collections, to avoid silent data transfers / conversions when using unaccelerated methods (syntax stays the same, though)
- No more CLRange: expecting compiler to do its job

TODO:
- Implement missing DataIO[T], including ASM-optimized case classes
- Catch up with compiler plugin:
  - Auto-vectorize for loops
  - Provide static types to OpenCL conversion
  - Create top-level objects for kernels code
- Benchmarks!
