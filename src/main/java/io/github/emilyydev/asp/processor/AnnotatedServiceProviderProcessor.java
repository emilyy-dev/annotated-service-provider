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

package io.github.emilyydev.asp.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * Annotation processor for the {@link io.github.emilyydev.asp.Provides} annotation.
 */
@SupportedAnnotationTypes("io.github.emilyydev.asp.Provides")
public final class AnnotatedServiceProviderProcessor extends AbstractProcessor {

  @SuppressWarnings("unchecked")
  private static <X extends Throwable> void sneakyThrow(final Throwable exception) throws X {
    throw (X) exception;
  }

  private final Map<String, Set<String>> serviceProvidersMap = new ConcurrentHashMap<>();

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    final Messager messager = this.processingEnv.getMessager();
    final Filer filer = this.processingEnv.getFiler();
    final Elements elementUtils = this.processingEnv.getElementUtils();
    final Types typeUtils = this.processingEnv.getTypeUtils();

    annotations.forEach(annotationType -> {
      final TypeMirror annotationTypeMirror = annotationType.asType();
      roundEnv.getElementsAnnotatedWith(annotationType).forEach(element -> {
        final ElementKind elementKind = element.getKind();
        if (!elementKind.isClass() && !elementKind.isInterface()) {
          return;
        }

        final TypeElement provider = (TypeElement) element;
        // error if the annotated type is not public
        if (!provider.getModifiers().contains(Modifier.PUBLIC)) {
          final String errorMessage = String.format("Annotated provider '%s' is not a public type", provider);
          messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
          throw new Error(errorMessage);
        }

        final boolean hasDefaultConstructor = provider.getEnclosedElements().stream()
            .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
            .map(ExecutableElement.class::cast)
            .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
            .map(ExecutableElement::getParameters)
            .mapToInt(List::size)
            .anyMatch(i -> i == 0);

        // error if the annotated type does not have a default/no-args constructor
        if (!hasDefaultConstructor) {
          final String errorMessage = String.format(
              "Annotated provider '%s' does not contain a default/no-args constructor",
              provider
          );
          messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
          throw new Error(errorMessage);
        }

        final String providerName = elementUtils.getBinaryName((TypeElement) element).toString();
        element.getAnnotationMirrors().stream()
            .filter(mirror -> typeUtils.isSameType(mirror.getAnnotationType(), annotationTypeMirror))
            .map(AnnotationMirror::getElementValues)
            .map(Map::values)
            .flatMap(Collection::stream)
            .map(AnnotationValue::getValue)
            .map(o -> (List<? extends AnnotationValue>) o)
            .flatMap(List::stream)
            .map(AnnotationValue::getValue)
            .filter(maybeService -> {
              // ignore array and primitive types
              if (!(maybeService instanceof DeclaredType)) {
                messager.printMessage(
                    Diagnostic.Kind.WARNING,
                    String.format(
                        "Annotation value '%s' is not a valid service type (non-final class or interface)%n"
                        + "Ignoring entry",
                        maybeService
                    ),
                    element
                );
                return false;
              }
              return true;
            })
            .map(DeclaredType.class::cast)
            .map(DeclaredType::asElement)
            .map(TypeElement.class::cast)
            .filter(service -> {
              // error if any of the listed types is not a supertype of the annotated type
              if (typeUtils.isAssignable(provider.asType(), service.asType())) {
                return true;
              }
              final String errorMessage = String.format(
                  "Annotated provider '%s' is not assignable from service '%s'",
                  provider, service
              );
              messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
              throw new Error(errorMessage);
            })
            .map(elementUtils::getBinaryName)
            .map(Object::toString)
            .forEach(serviceName -> this.serviceProvidersMap.computeIfAbsent(serviceName, s -> new HashSet<>())
                .add(providerName));
      });
    });

    for (final Map.Entry<String, Set<String>> entry : this.serviceProvidersMap.entrySet()) {
      final String service = entry.getKey();
      final Set<String> providers = entry.getValue();

      try {
        final FileObject resource = filer.createResource(CLASS_OUTPUT, "", "META-INF/services/" + service);
        try (
            final OutputStream stream = resource.openOutputStream();
            final OutputStreamWriter streamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            final BufferedWriter bufferedWriter = new BufferedWriter(streamWriter)
        ) {
          for (final String provider : providers) {
            bufferedWriter.write(provider);
            bufferedWriter.newLine();
          }
        }
      } catch (final FilerException exception) {
        // ignore, file already exists (?)
      } catch (final IOException exception) {
        sneakyThrow(exception);
      }
    }

    return true;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }
}
