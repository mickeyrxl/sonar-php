/*
 * SonarQube PHP Plugin
 * Copyright (C) 2010 SonarSource and Akram Ben Aissi
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.php.checks;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.php.tree.visitors.PHPIssue;
import org.sonar.plugins.php.TestUtils;
import org.sonar.plugins.php.api.tests.PHPCheckTest;
import org.sonar.plugins.php.api.visitors.Issue;

import java.util.List;

public class StringLiteralDuplicatedCheckTest {

  private StringLiteralDuplicatedCheck check = new StringLiteralDuplicatedCheck();

  @Test
  public void defaultValue() throws Exception {
    PHPCheckTest.check(check, TestUtils.getCheckFile("StringLiteralDuplicatedCheck.php"));
  }

  @Test
  public void custom() throws Exception {
    check.threshold = 4;
    List<Issue> issue = ImmutableList.<Issue>of(new PHPIssue(check, "Define a constant instead of duplicating this literal \"name1\" 4 times.").line(19));
    PHPCheckTest.check(check, TestUtils.getCheckFile("StringLiteralDuplicatedCheck.php"), issue);
  }
}
