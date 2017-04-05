/**
 * Renjin : JVM-based interpreter for the R language for the statistical analysis
 * Copyright © 2010-2016 BeDataDriven Groep B.V. and contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, a copy is available at
 * https://www.gnu.org/licenses/gpl-2.0.txt
 */
package org.renjin.primitives.files;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.renjin.EvalTestCase;
import org.renjin.repackaged.guava.base.Charsets;
import org.renjin.repackaged.guava.io.Files;
import org.renjin.sexp.LogicalVector;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.StringVector;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


public class FilesTest extends EvalTestCase {

  @Before
  public void setUpTests() throws FileSystemException {
    assumingBasePackagesLoad();

    // For reproducible tests, we've included a hierarchy of files in src/test/resources
    // These should be on the classpath when tests are run
    URL resourceURL = FilesTest.class.getResource("FilesTest/a.txt");
    FileObject rootDir = topLevelContext.resolveFile(resourceURL.getPath()).getParent();

    topLevelContext.getGlobalEnvironment().setVariable(topLevelContext, "rootDir", StringVector.valueOf(rootDir.toString()));
  }


  @Test
  @Ignore("setwd should throw error if path doesn't exist")
  public void getSetWd(){
    eval("older<-getwd()");
    eval("setwd('/path/to/file')");
    assertThat(eval("getwd()"), equalTo(c("file:///path/to/file")));
    eval("setwd(older)");
  }

  @Test
  public void listFiles() throws URISyntaxException, FileSystemException {


    assertThat(eval("list.files(rootDir)"), equalTo(c("a.txt", "b.txt", "c")));

    assertThat(eval("list.files(rootDir, all.files=TRUE)"), equalTo(c(".", "..", ".hidden.txt", "a.txt", "b.txt", "c")));

    assertThat(eval("list.files(rootDir, pattern='txt$')"), equalTo(c("a.txt", "b.txt")));

    assertThat(eval("list.files(rootDir, pattern='TXT$', ignore.case=TRUE)"), equalTo(c("a.txt", "b.txt")));

    assertThat(eval("list.files(rootDir, pattern='txt$', all.files=TRUE)"),
        equalTo(c(".hidden.txt", "a.txt", "b.txt")));

    assertThat(eval("list.files(rootDir, pattern='txt$', recursive=TRUE)"),
        equalTo(c("a.txt", "b.txt", "c/ca.txt", "c/cb.txt", "c/d/cda.txt")));

    assertThat(eval("list.files(rootDir, pattern='c', recursive=TRUE, include.dirs=TRUE)"),
        equalTo(c("c", "c/ca.txt", "c/cb.txt", "c/d/cda.txt")));
  }

  @Test
  public void renameFiles() throws IOException {

    // Create a temp directory with a source file
    File tempDir = org.renjin.repackaged.guava.io.Files.createTempDir();
    File sourceFile = new File(tempDir, "a.txt");
    File destFile = new File(tempDir, "b.txt");
    Files.write("ABC", sourceFile, Charsets.UTF_8);

    topLevelContext.getGlobalEnvironment().setVariable(topLevelContext, "rootDir", StringVector.valueOf(tempDir.getAbsolutePath()));
    
    eval("setwd(rootDir)");
    eval("x <- file.rename('a.txt', 'b.txt')");
    
    assertThat(eval("x"), equalTo(c(true)));
    
    assertTrue("source file does not exist", !sourceFile.exists());
    assertTrue("dest file exists", destFile.exists());
  }

  @Test
  public void renameFilesFailure() throws IOException {

    // Failure returns false, does not throw error
    
    eval("x <- file.rename('doesnotexist.txt', 'b.txt')");

    assertThat(eval("x"), equalTo(c(false)));

  }

  @Test
  public void removeFile() throws IOException {

    // Create a temp directory with a source file
    File tempDir = org.renjin.repackaged.guava.io.Files.createTempDir();
    File file = new File(tempDir, "a.txt");
    Files.write("ABC", file, Charsets.UTF_8);

    topLevelContext.getGlobalEnvironment().setVariable(topLevelContext, "rootDir", StringVector.valueOf(tempDir.getAbsolutePath()));

    eval("setwd(rootDir)");
    eval("x <- file.remove('a.txt')");

    assertThat(eval("x"), equalTo(c(true)));

    assertTrue("source file does not exist", !file.exists());

  }

  @Test
  public void removeDirRecursive() throws IOException {

    // Create a temp directory with a source file
    File tempDir = org.renjin.repackaged.guava.io.Files.createTempDir();
    File file = new File(tempDir, "a.txt");
    Files.write("ABC", file, Charsets.UTF_8);

    topLevelContext.getGlobalEnvironment().setVariable(topLevelContext, "tempDir", StringVector.valueOf(tempDir.getAbsolutePath()));

    eval("x <- unlink(tempDir, recursive = TRUE)");

    assertThat(eval("x"), equalTo(c_i(1)));

    assertTrue("child file has been removed", !file.exists());
    assertTrue("temp dir has been removed", !tempDir.exists());
  }

  @Test
  public void removeDirRecursiveTwoLevels() throws IOException {

    // Create a temp directory with a source file
    File tempDir = org.renjin.repackaged.guava.io.Files.createTempDir();
    File childDir  = new File(tempDir, "child");
    File grandChild = new File(childDir, "grandChild.txt");

    Files.createParentDirs(grandChild);
    Files.write("ABC", grandChild, Charsets.UTF_8);

    topLevelContext.getGlobalEnvironment().setVariable(topLevelContext, "tempDir", StringVector.valueOf(tempDir.getAbsolutePath()));

    eval("x <- unlink(tempDir, recursive = TRUE)");

    assertThat(eval("x"), equalTo(c_i(1)));

    assertTrue("grand child file has been removed", !grandChild.exists());
    assertTrue("child dir has been removed", !childDir.exists());
    assertTrue("temp dir has been removed", !tempDir.exists());
  }

  @Test
  public void removeFilesFailure() throws IOException {

    // Failure returns false, does not throw error

    eval("x <- file.remove('doesnotexist.txt')");

    assertThat(eval("x"), equalTo(c(false)));

  }

  @Test
  public void fileExists() {

    assertThat(eval("file.exists(rootDir)"), equalTo(c(true)));
    assertThat(eval("file.exists(file.path(rootDir, 'a.txt'))"), equalTo(c(true)));

    assertThat(eval("file.exists(c(FALSE, NA, NaN, NULL, \"/bla/bla\", 12.5, 11L))"),
        equalTo(c(false, false, false, false, false, false)));
    assertThat(eval("file.exists(character(0))"),
        equalTo((SEXP) LogicalVector.EMPTY));
  }

  @Test
  public void dirExists() {

    assertThat(eval("dir.exists(rootDir)"), equalTo(c(true)));
    assertThat(eval("dir.exists(file.path(rootDir, 'a.txt'))"), equalTo(c(false)));

    assertThat(eval(" dir.exists(c(FALSE, NA, NaN, NULL, \"/bla/bla\", 12.5, 11L))"),
        equalTo(c(false, false, false, false, false, false)));
    assertThat(eval("dir.exists(character(0))"),
        equalTo((SEXP) LogicalVector.EMPTY));
  }


}
