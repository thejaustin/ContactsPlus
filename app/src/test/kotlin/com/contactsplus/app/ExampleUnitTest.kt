package com.contactsplus.app

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun string_concatenation() {
        val str1 = "Hello"
        val str2 = "World"
        assertEquals("Hello World", "$str1 $str2")
    }
}
