/*
 * Copyright 2016-2022 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */
package foo

@Target(AnnotationTarget.PROPERTY)
annotation class HiddenProperty

public class Foo {
    companion object {
        @HiddenProperty
        const val bar = "barValue"
    }
}