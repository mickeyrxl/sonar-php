/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010-2020 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.php.checks.phpunit;

import com.google.common.collect.ImmutableSet;
import org.sonar.check.Rule;
import org.sonar.php.checks.utils.CheckUtils;
import org.sonar.php.checks.utils.PhpUnitCheck;
import org.sonar.php.tree.TreeUtils;
import org.sonar.plugins.php.api.symbols.Symbol;
import org.sonar.plugins.php.api.symbols.SymbolTable;
import org.sonar.plugins.php.api.tree.Tree;
import org.sonar.plugins.php.api.tree.declaration.MethodDeclarationTree;
import org.sonar.plugins.php.api.tree.expression.ExpressionTree;
import org.sonar.plugins.php.api.tree.expression.FunctionCallTree;
import org.sonar.plugins.php.api.tree.expression.MemberAccessTree;
import org.sonar.plugins.php.api.visitors.PHPVisitorCheck;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Rule(key = "S2699")
public class NoAssertionInTestCheck extends PhpUnitCheck {
  private static final String MESSAGE = "Add at least one assertion to this test case.";

  private static final Pattern ASSERTION_METHODS_PATTERN =
    Pattern.compile("(assert|verify|fail|pass|should|check|expect|validate|test|.*Test).*");

  private final Map<MethodDeclarationTree, Boolean> assertionInMethod = new HashMap<>();

  @Override
  public void visitMethodDeclaration(MethodDeclarationTree tree) {
    if (!isTestCaseMethod(tree)) {
      return;
    }

    if (CheckUtils.hasAnnotation(tree, "expectedException") ||
      CheckUtils.hasAnnotation(tree, "doesNotPerformAssertions") ||
      CheckUtils.hasAnnotation(tree, "expectedDeprecation")) {
      return;
    }

    AssertionsFindVisitor assertionsFindVisitor = new AssertionsFindVisitor(context().symbolTable());
    tree.accept(assertionsFindVisitor);

    if (!assertionsFindVisitor.didFindAssertion) {
      context().newIssue(this, tree.name(), MESSAGE);
    }
  }

  private class AssertionsFindVisitor extends PHPVisitorCheck {
    private boolean didFindAssertion = false;
    private final SymbolTable symbolTable;

    private AssertionsFindVisitor(SymbolTable symbolTable) {
      this.symbolTable = symbolTable;
    }

    @Override
    public void visitFunctionCall(FunctionCallTree tree) {
      String functionName = getFunctionName(tree);

      if (isAssertion(tree) ||
        (functionName != null && (ASSERTION_METHODS_PATTERN.matcher(functionName).matches())) ||
        isLocalMethodWithAssertion(tree)) {
        didFindAssertion = true;
      }

      super.visitFunctionCall(tree);
    }

    private boolean isLocalMethodWithAssertion(FunctionCallTree tree) {
      MethodDeclarationTree methodDeclaration;
      Optional<MethodDeclarationTree> optionalMethodDeclaration = getMethodDeclarationTree(tree);
      if (optionalMethodDeclaration.isPresent()) {
        methodDeclaration = optionalMethodDeclaration.get();
      } else {
        return false;
      }

      if (!assertionInMethod.containsKey(methodDeclaration)) {
        assertionInMethod.put(methodDeclaration, false);
        AssertionsFindVisitor c = new AssertionsFindVisitor(symbolTable);

        c.scan(methodDeclaration);
        assertionInMethod.put(methodDeclaration, c.didFindAssertion);
      }
      return assertionInMethod.get(methodDeclaration);
    }

    private Optional<MethodDeclarationTree> getMethodDeclarationTree(FunctionCallTree tree) {
      ExpressionTree callee = tree.callee();
      if (!callee.is(Tree.Kind.CLASS_MEMBER_ACCESS, Tree.Kind.OBJECT_MEMBER_ACCESS)) {
        return Optional.empty();
      }

      Symbol symbol = symbolTable.getSymbol(((MemberAccessTree) callee).member());
      if (symbol != null && symbol.is(Symbol.Kind.FUNCTION)) {
        return Optional.ofNullable((MethodDeclarationTree) TreeUtils.findAncestorWithKind(symbol.declaration(),
          ImmutableSet.of(Tree.Kind.METHOD_DECLARATION)));
      }

      return Optional.empty();
    }

    private @Nullable String getFunctionName(FunctionCallTree tree) {
      String functionName = CheckUtils.getFunctionName(tree);

      if (functionName == null && tree.callee().is(Tree.Kind.OBJECT_MEMBER_ACCESS)) {
        functionName = CheckUtils.nameOf(((MemberAccessTree) tree.callee()).member());
      }

      if (functionName != null && functionName.contains("::")) {
        functionName = functionName.substring(functionName.indexOf("::") + 2);
      }

      return functionName;
    }
  }
}
