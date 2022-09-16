package com.github.marschall.openrewirte.demo.recipe;

import java.util.ArrayList;
import java.util.List;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Annotation;
import org.openrewrite.java.tree.J.ClassDeclaration;
import org.openrewrite.java.tree.J.Identifier;
import org.openrewrite.java.tree.J.MethodDeclaration;
import org.openrewrite.java.tree.J.MethodDeclaration.IdentifierWithAnnotations;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.J.VariableDeclarations;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import com.fasterxml.jackson.annotation.JsonCreator;

public class FieldToConstructorInjectionRecipe extends Recipe {

  @JsonCreator
  public FieldToConstructorInjectionRecipe() {
    super();
  }

  @Override
  public String getDisplayName() {
    return "Field to Constructor Injections";
  }

  @Override
  public String getDescription() {
    return "Converts Spring beans from field to constructor injection";
  }

  @Override
  protected JavaVisitor<ExecutionContext> getVisitor() {
    return new FieldToConstructorVisitor();
  }

  public class FieldToConstructorVisitor extends JavaIsoVisitor<ExecutionContext> {

    private static final JavaType AUTOWIRED = JavaType.buildType("org.springframework.beans.factory.annotation.Autowired");

    @Override
    public ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {

      J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);

      J.Block body = cd.getBody();
      List<Statement> statements = body.getStatements();
      List<Statement> newStatements = new ArrayList<>(statements.size());
      List<J.VariableDeclarations> autowiredFields = new ArrayList<>();
      boolean modified = false;
      for (Statement statement : statements) {
        if (isAutowiredField(statement)) {
          J.VariableDeclarations fieldDeclaration = (J.VariableDeclarations) statement;
          autowiredFields.add(fieldDeclaration);
          if (!isFinal(fieldDeclaration)) {
            newStatements.add(makeFinal(fieldDeclaration, executionContext));
            modified = true;
          } else {
            newStatements.add(statement);
          }
        } else {
          newStatements.add(statement);
        }
      }

      if (modified) {
        newStatements.add(assignmentConstructor(newIdentifier(cd.getSimpleName()), autowiredFields));
        return cd.withBody(autoFormat(body.withStatements(newStatements), executionContext));
      } else {
        return cd;
      }
    }

    private static boolean isAutowiredField(Statement statement) {
      return statement instanceof VariableDeclarations
          && isAutowired((VariableDeclarations) statement);
    }

    private static boolean isAutowired(J.VariableDeclarations variableDeclarations) {
      return variableDeclarations.getLeadingAnnotations().stream()
          .map(Annotation::getType)
          .filter(annotationType -> TypeUtils.isOfType(annotationType, AUTOWIRED))
          .findAny()
          .isPresent();
    }

    private J.MethodDeclaration assignmentConstructor(Identifier className, List<J.VariableDeclarations> fieldDeclarations) {
      List<Annotation> leadingAnnotations = List.of(newAnnotation("org.springframework.beans.factory.annotation.Autowired"));
      List<Modifier> modifiers = List.of(newModifier(J.Modifier.Type.Public));
      IdentifierWithAnnotations name = new IdentifierWithAnnotations(className, List.of());
      JContainer<Statement> parameters = asParameters(fieldDeclarations);
      J.Block body = new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), buildAssignments(fieldDeclarations), Space.EMPTY);

      MethodDeclaration constructor = new J.MethodDeclaration(Tree.randomId(), Space.EMPTY, Markers.EMPTY, leadingAnnotations, modifiers, null, null, name, parameters, null, body, null, null);
      return constructor;
    }
    
    private static List<JRightPadded<Statement>> buildAssignments(List<J.VariableDeclarations> fieldDeclarations) {
      return fieldDeclarations.stream()
          .map(variableDeclarations -> asAssignment(variableDeclarations))
          .map(Statement.class::cast)
          .map(JRightPadded::build)
          .toList();
    }

    private static J.Annotation newAnnotation(String name) {
      return new J.Annotation(Tree.randomId(), Space.EMPTY, Markers.EMPTY, newIdentifier(name), JContainer.empty());
    }

    private static J.Identifier newIdentifier(String name) {
      return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, name, null, null);
    }

    private static JContainer<Statement> asParameters(List<J.VariableDeclarations> fieldDeclarations) {
      List<JRightPadded<Statement>> elements = fieldDeclarations.stream()
          .map(variableDeclarations -> asParameter(variableDeclarations))
          .map(Statement.class::cast)
          .map(JRightPadded::build)
          .toList();
      return JContainer.build(elements);
    }
    
    private static J.Assignment asAssignment(J.VariableDeclarations variableDeclarations) {
      NamedVariable parameter = variableDeclarations.getVariables().get(0);
      // name of the parameter and field
      Identifier name = newIdentifier(parameter.getSimpleName());
      // type of the name and field
      JavaType type = parameter.getType();
      Expression target = newIdentifier("this");
      Expression variable = new J.FieldAccess(Tree.randomId(), Space.EMPTY, Markers.EMPTY, target , JLeftPadded.build(name), type);
      return new J.Assignment(Tree.randomId(), Space.EMPTY, Markers.EMPTY, variable, JLeftPadded.build(name), type);
    }

    private static J.VariableDeclarations asParameter(J.VariableDeclarations variableDeclarations) {
      List<JRightPadded<NamedVariable>> variables = variableDeclarations.getVariables().stream()
          .map(namedVariable -> JRightPadded.build(namedVariable))
          .toList();
      return new J.VariableDeclarations(Tree.randomId(), Space.EMPTY, Markers.EMPTY, List.of(), List.of(), variableDeclarations.getTypeExpression(), null, List.of(), variables);
    }

    private static J.Modifier newModifier(J.Modifier.Type modifier) {
      return new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, modifier, List.of());
    }

    private J.VariableDeclarations makeFinal(J.VariableDeclarations variableDeclarations, ExecutionContext executionContext) {
      J.Modifier finalModifier = newModifier(J.Modifier.Type.Final);
      List<J.Modifier> withFinal = ListUtils.concat(variableDeclarations.getModifiers(), finalModifier);
//      return autoFormat(variableDeclarations.withModifiers(withFinal), executionContext, getCursor());
      return variableDeclarations.withModifiers(withFinal);
    }

    private static boolean isFinal(J.VariableDeclarations variableDeclarations) {
      return variableDeclarations.hasModifier(Modifier.Type.Final);
    }

  }

}
