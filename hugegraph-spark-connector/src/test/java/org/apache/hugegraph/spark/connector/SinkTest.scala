package org.apache.hugegraph.spark.connector

import org.junit.Assert._
import org.junit._

object UsingJUnit {
  @BeforeClass
  def beforeClass(): Unit = {
    println("before class")
  }

  @AfterClass
  def afterClass(): Unit = {
    println("after class")
  }
}

class UsingJUnit {

  @Before
  def before(): Unit = {
    println("before test")
  }

  @After
  def after(): Unit = {
    println("after test")
  }

  @Test
  def testList(): Unit = {
    println("testList")
    val list = List("a", "b")

    assertEquals(List("a", "b"), list)
    assertNotEquals(List("b", "a"), list)
  }
}

// junit check exception
class JunitCheckException {

  val _thrown = rules.ExpectedException.none

  @Rule
  def thrown = _thrown

  @Test(expected = classOf[IndexOutOfBoundsException])
  def testStringIndexOutOfBounds(): Unit = {
    val s = "test string"
    s.charAt(-1)
  }

  @Test
  def testStringIndexOutOfBoundsExceptionMessage(): Unit = {
    val s = "test string"
    thrown.expect(classOf[IndexOutOfBoundsException])
    thrown.expectMessage("String index out of range: -1")
    s.charAt(-1)
  }
}
