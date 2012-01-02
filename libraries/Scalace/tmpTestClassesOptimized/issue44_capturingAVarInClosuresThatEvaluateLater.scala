package tests
class Test_issue44_capturingAVarInClosuresThatEvaluateLater_Optimized_9(n: Int) {
      
      def issue44_capturingAVarInClosuresThatEvaluateLater() = {
      
      (1 to 10).map {i => j: Int => i + j}.map(_(10))
    
      }
    }
