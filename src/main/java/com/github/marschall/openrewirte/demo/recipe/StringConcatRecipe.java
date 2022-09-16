package com.github.marschall.openrewirte.demo.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Binary;
import org.openrewrite.java.tree.J.Literal;
import org.openrewrite.java.tree.TypeUtils;

import com.fasterxml.jackson.annotation.JsonCreator;

// REVIEW:
// dependency on Jackson
// dependency on internal package
// class named J
public class StringConcatRecipe extends Recipe {

  // Recipes must be serializable. This is verified by RecipeTest.assertChanged() and RecipeTest.assertUnchanged()
  @JsonCreator
  public StringConcatRecipe() {
    super();
  }

  @Override
  public String getDisplayName() {
    return "String concat";
  }

  @Override
  public String getDescription() {
    return "Replaces string concat with multi line strings";
  }


  @Override
  protected JavaVisitor<ExecutionContext> getVisitor() {
    return new StringConcatVisitor();
  }

  public class StringConcatVisitor extends JavaVisitor<ExecutionContext> {

    private final JavaTemplate multiLineStringTemplate =
        JavaTemplate.builder(this::getCursor, "\"\"\"\n#{}\"\"\"")
        .build();

    @Override
    public J visitLiteral(Literal literal, ExecutionContext p) {
      return super.visitLiteral(literal, p);
    }

    @Override
    public J visitBinary(J.Binary b, ExecutionContext p) {
      J j = super.visitBinary(b, p);
      if (j instanceof J.Binary) {
        J.Binary binary = (Binary) j;
        if (isStringConcat(binary)) {
          if (isLiteral(binary.getLeft()) && isLiteral(binary.getRight())) {
            J.Literal left = binary.getLeft().cast();
            J.Literal right = binary.getRight().cast();
            String value = "" + left.getValue() + right.getValue();
            return binary.withTemplate(multiLineStringTemplate, binary.getCoordinates().replace(), value);
          }
        }
        return binary;
      } else {
        return j;
      }
    }

    private static boolean isStringConcat(J.Binary binary) {
      return binary.getOperator() == Binary.Type.Addition && TypeUtils.isString(binary.getType());
    }

    private static boolean isLiteral(Expression expression) {
      return expression instanceof J.Literal;
    }

  }

}