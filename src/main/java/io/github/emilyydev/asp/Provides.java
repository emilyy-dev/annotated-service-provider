//
// MIT License
//
// Copyright (c) 2021 emilyy-dev
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

package io.github.emilyydev.asp;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares an array of services (non-final classes/interfaces) that the class annotated with this annotation provides.
 *
 * <p>
 * Details on how this annotation behaves at compile-time are specified by the {@link java.util.ServiceLoader} class
 * documentation.
 * </p>
 *
 * <p>
 * Types that are not valid service types (array and primitive types) are ignored by the annotation
 * processor, yielding a warning.
 * </p>
 *
 * <p>
 * If the annotated service provider does not extend/implement <b>any</b> of the listed services, does not provide a
 * "default"/"no args" constructor, or is not a {@code public} element, the annotation processor yields an error.
 * </p>
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Provides {

  /**
   * An array of classes and/or interfaces the class annotated with this interface provides.
   *
   * @return the services this class provides
   */
  Class<?>[] value();
}
