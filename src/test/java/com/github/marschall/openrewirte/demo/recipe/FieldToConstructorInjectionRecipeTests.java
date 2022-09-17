package com.github.marschall.openrewirte.demo.recipe;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class FieldToConstructorInjectionRecipeTests implements RewriteTest {

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new FieldToConstructorInjectionRecipe())
      .parser(JavaParser.fromJavaVersion()
//          .classpath("spring-jdbc", "spring-tx", "spring-beans", "spring-core")
          .classpath("spring-beans"));
  }

  @Test
  void sampleBean() {
    rewriteRun(
        java("""
             import org.springframework.beans.factory.annotation.Autowired;

             public class SampleBean {

               @Autowired
               private Integer firstDependency;

               @Autowired
               private String secondDependency;

             }
             """,
             """
             import org.springframework.beans.factory.annotation.Autowired;

             public class SampleBean {


                 private final Integer firstDependency;


                 private final String secondDependency;

                 @Autowired
                 public SampleBean(Integer firstDependency, String secondDependency) {
                     this.firstDependency = firstDependency;
                     this.secondDependency = secondDependency;
                 }
             
             }
             """));
  }


}
