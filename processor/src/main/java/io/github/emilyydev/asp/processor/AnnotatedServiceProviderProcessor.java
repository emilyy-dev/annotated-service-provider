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

import io.github.emilyydev.asp.Provides;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static javax.tools.StandardLocation.CLASS_OUTPUT;

/**
 * Annotation processor for the {@link Provides} annotation.
 */
public final class AnnotatedServiceProviderProcessor extends AbstractProcessor {

  private static final String PROVIDES_ANNOTATION_NAME = Provides.class.getCanonicalName();
  private static final Set<String> SUPPORTED_ANNOTATIONS = Collections.singleton(PROVIDES_ANNOTATION_NAME);
  private static final Set<String> SUPPORTED_OPTIONS = Collections.emptySet();

  private final Map<String, Set<String>> serviceProvidersMap = new HashMap<>();

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return SUPPORTED_ANNOTATIONS;
  }

  @Override
  public Set<String> getSupportedOptions() {
    return SUPPORTED_OPTIONS;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      writeServiceFiles();
    } else {
      processAnnotations(roundEnv);
    }

    return true;
  }

  private Set<String> getProvidersFor(final String service) {
    return this.serviceProvidersMap.computeIfAbsent(service, s -> new HashSet<>());
  }

  private void processAnnotations(final RoundEnvironment roundEnv) {
    final Messager messager = this.processingEnv.getMessager();
    final Elements elementUtils = this.processingEnv.getElementUtils();
    final Types typeUtils = this.processingEnv.getTypeUtils();

    for (final Element element : roundEnv.getElementsAnnotatedWith(Provides.class)) {
      final ElementKind elementKind = element.getKind();
      if (!elementKind.isClass() && !elementKind.isInterface()) {
        continue;
      }

      final TypeElement provider = (TypeElement) element;
      final String providerSimpleName = provider.getSimpleName().toString();
      final String providerName = elementUtils.getBinaryName(provider).toString();

      // error if the annotated type is not public
      if (!provider.getModifiers().contains(Modifier.PUBLIC)) {
        final String errorMessage = String.format(
            "Annotated provider '%s' is not a public type",
            providerSimpleName
        );
        messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
        continue;
      }

      // error if the annotated type is an inner class (non-static "nested" class)
      final Element enclosingElement = provider.getEnclosingElement();
      if (enclosingElement.getKind().isClass() && !provider.getModifiers().contains(Modifier.STATIC)) {
        final String errorMessage = String.format(
            "Annotated provider '%s' must not be an inner class",
            providerSimpleName
        );
        messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
        continue;
      }

      final boolean hasDefaultConstructor = provider.getEnclosedElements().stream()
          .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR)
          .map(ExecutableElement.class::cast)
          .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
          .map(ExecutableElement::getParameters)
          .mapToInt(List::size)
          .anyMatch(i -> i == 0);

      // TODO: not error? Since Java 9+ a provider can have a `public static T provider()` function
      //  (where T is the service type)
      // error if the annotated type does not have a default/no-args constructor
      if (!hasDefaultConstructor) {
        final String errorMessage = String.format(
            "Annotated provider '%s' is an invalid provider type%n"
            + "It does not contain a public default/no-args constructor",
            providerSimpleName
        );
        messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
        continue;
      }

      final boolean allEntriesProcessed = getAnnotationServices(element)
          .filter(maybeService -> {
            // ignore array, void and primitive types (in reality just anything that's not a class/interface)
            if (maybeService.getKind() != TypeKind.DECLARED) {
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
          .map(service -> {
            // TODO: not error? Since Java 9+ a provider can not extend the service type if there is a provider method
            // error if any of the listed types is not a supertype of the annotated type
            if (typeUtils.isAssignable(provider.asType(), service.asType())) {
              return Optional.of(elementUtils.getBinaryName(service).toString());
            }
            final String errorMessage = String.format(
                "Annotated provider '%s' is not assignable from service '%s'",
                providerSimpleName, service.getSimpleName()
            );
            messager.printMessage(Diagnostic.Kind.ERROR, errorMessage, element);
            return Optional.<String>empty();
          })
          .peek(opt -> opt.ifPresent(serviceName -> getProvidersFor(serviceName).add(providerName)))
          .allMatch(Optional::isPresent);

      if (!allEntriesProcessed) {
        this.serviceProvidersMap.clear();
        return;
      }
    }
  }

  private Stream<? extends TypeMirror> getAnnotationServices(final Element element) {
    final Elements elementUtils = this.processingEnv.getElementUtils();
    final Types typeUtils = this.processingEnv.getTypeUtils();
    final TypeMirror annotationType = elementUtils.getTypeElement(PROVIDES_ANNOTATION_NAME).asType();

    return element.getAnnotationMirrors().stream()
        .filter(mirror -> typeUtils.isSameType(mirror.getAnnotationType(), annotationType))
        .map(AnnotationMirror::getElementValues)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .filter(entry -> entry.getKey().getSimpleName().contentEquals("value"))
        .map(Map.Entry::getValue)
        .map(AnnotationValue::getValue)
        .map(o -> (List<? extends AnnotationValue>) o)
        .flatMap(Collection::stream)
        .map(AnnotationValue::getValue)
        .map(TypeMirror.class::cast);
  }

  private void writeServiceFiles() {
    final Messager messager = this.processingEnv.getMessager();
    final Filer filer = this.processingEnv.getFiler();

    for (final Map.Entry<String, Set<String>> entry : this.serviceProvidersMap.entrySet()) {
      final String service = entry.getKey();
      final String serviceFile = "META-INF/services/" + service;
      final Set<String> providers = entry.getValue();

      try {
        final FileObject resource = filer.createResource(CLASS_OUTPUT, "", serviceFile);
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
      } catch (final IOException exception) {
        messager.printMessage(Diagnostic.Kind.ERROR, exception.getMessage());
        return;
      }
    }
  }
}
