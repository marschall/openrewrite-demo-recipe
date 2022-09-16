package com.github.marschall.openrewirte.demo.recipe;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class SayHelloRecipeTests implements RewriteTest {

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new SayHelloRecipe("com.yourorg.A"));
  }

  @Test
  void addsHelloToA() {
    rewriteRun(
        java("""
             package com.yourorg;

             class A {
             }
             """,
             """
             package com.yourorg;

             class A {
                 public String hello() {
                     return "Hello from com.yourorg.A!";
                 }
             }
             """));
  }

  @Test
  void doesNotChangeExistingHello() {
    rewriteRun(
        java("""
             package com.yourorg;

             class A {
               public String hello() { return ""; }
             }
             """));
  }

  @Test
  void doesNotChangeOtherClass() {
    rewriteRun(
        java(
            """
            package com.yourorg;

            class B {
            }
            """));
  }

}
