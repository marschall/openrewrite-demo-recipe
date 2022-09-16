package com.github.marschall.openrewirte.demo.recipe;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class StringConcatRecipeTests implements RewriteTest {

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new StringConcatRecipe());
  }

  @Test
  void combinesToMultiLine() {
    rewriteRun(
        java("""
             package com.yourorg;

             class A {
                 private static final String S = "A" + "B";
             }
             """,
             """
             package com.yourorg;

             class A {
                 private static final String S = \"\"\"
             AB\"\"\";
             }
             """));
  }
  
  @Test
  void combinesToMultiLineMultipleLiterals() {
    rewriteRun(
        java("""
             package com.yourorg;
            
             class A {
                 private static final String S = "A" + "B" + "C";
             }
             """,
             """
             package com.yourorg;
             
             class A {
                 private static final String S = \"\"\"
             ABC\"\"\";
             }
             """));
  }
  
  @Test
  void doesNotcombineToMultiLine() {
    rewriteRun(
        java("""
             package com.yourorg;
            
             class A {
                 private static final String S = "A" + A.class.getName();
             }
             """));
  }

}
