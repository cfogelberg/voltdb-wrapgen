package uk.co.bristlecone.vpt.source.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFacade;
import me.tomassetti.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import me.tomassetti.symbolsolver.resolution.typesolvers.JreTypeSolver;
import uk.co.bristlecone.vpt.VptRuntimeException;
import uk.co.bristlecone.vpt.source.ProcReturnType;
import uk.co.bristlecone.vpt.source.RunParameter;
import uk.co.bristlecone.vpt.source.RunParameterClass;
import uk.co.bristlecone.vpt.source.RunParameterPrimitive;
import uk.co.bristlecone.vpt.source.SourceFile;

/**
 * Implements the {@link SourceFile} interface using a <code>com.github.javaparser.ast.CompilationUnit</code>.
 *
 * @author christo
 */
public class JavaparserSourceFile implements SourceFile {
  private static Logger LOGGER = LoggerFactory.getLogger(JavaparserSourceFile.class);

  private final CompilationUnit ast;
  private final String filepath;

  public JavaparserSourceFile(final CompilationUnit ast, final String filepath) {
    this.ast = ast;
    this.filepath = filepath;
  }

  @Override
  public String identifier() {
    return filepath;
  }

  @Override
  public boolean isValidVoltProcedure() {
    final Optional<ClassOrInterfaceDeclaration> storedProc = getClassExtendingVoltProcedure();
    final Optional<MethodDeclaration> runMethod = getRunMethod();

    if(!storedProc.isPresent()) {
      return false;
    }

    if(!runMethod.isPresent()) {
      return false;
    }

    final Optional<String> returnType = getRunMethodReturnTypeAsString();
    if(!returnType.isPresent() || !ProcReturnType.isValidJavaType(getRunMethodReturnTypeAsString().get())) {
      return false;
    }

    // TODO - checks to do, eg throws the wrong type of chcekced exception, that class extending voltproc isn't abstract

    return true;
  }

  @Override
  public boolean isIntendedVoltProcedure() {
    // TODO also check that the class extending volt procedure is not abstract
    return getClassExtendingVoltProcedure().isPresent();
  }

  @Override
  public String voltProcedureName() {
    return getClassExtendingVoltProcedure().map(c -> c.getName()).orElseThrow(
        () -> new VptRuntimeException(String.format("No VoltProcedure-extending type found in %s", filepath)));
  }

  @Override
  public List<RunParameter> runMethodParameters() {
    return getRunMethod().map(m -> m.getParameters()).map(pl -> pl.stream().map(JavaparserSourceFile::mapParam))
        .map(s -> s.collect(Collectors.toList())).orElseThrow(() -> new VptRuntimeException(
            String.format("Either no VoltProcedure-extending type found in %s, or no run method defined", filepath)));
  }

  @Override
  public ProcReturnType runMethodReturnType() {
    return getRunMethodReturnTypeAsString().map(ProcReturnType::parseJavaType)
        .orElseThrow(() -> new VptRuntimeException(
            String.format("Either no VoltProcedure-extending type found in %s, or no run method defined", filepath)));
  }

  @Override
  public String packageName() {
    return Optional.ofNullable(ast.getPackage()).map(p -> p.getPackageName()).orElse("");
  }

  @Override
  public String classJavaDoc() {
    return getClassExtendingVoltProcedure().map(c -> c.getJavaDoc()).map(jc -> jc.getContent())
        .map(s -> s.replaceAll("\\n\\s\\*\\s", "\n")).orElse(SourceFile.NO_CLASS_JAVADOC_TEXT);
  }

  /**
   * @return the <code>run</code> method's return type as Optional<String>
   */
  private Optional<String> getRunMethodReturnTypeAsString() {
    return getRunMethod().map(m -> m.getType()).map(t -> t.toString());
  }

  /**
   * @return the first {@link ClassOrInterfaceDeclaration} type in this source file which extends a class called
   *         "VoltProcedure" (NB: Does not do full symbol resolution, so this method could be misled by another class of
   *         the same name in a different package).
   */
  private Optional<ClassOrInterfaceDeclaration> getClassExtendingVoltProcedure() {
    final ClassOrInterfaceType voltProcedure = new ClassOrInterfaceType("VoltProcedure");
    return ast.getTypes().stream().filter(d -> d instanceof ClassOrInterfaceDeclaration)
        .map(d -> (ClassOrInterfaceDeclaration) d).filter(d -> d.getExtends().contains(voltProcedure)).findFirst();
  }

  /**
   * @return the first method overload for "run" in the class returned by
   *         {@link JavaparserSourceFile#getClassExtendingVoltProcedure}
   */
  private Optional<MethodDeclaration> getRunMethod() {
    return getClassExtendingVoltProcedure().flatMap(c -> c.getMethodsByName("run").stream().findFirst());
  }

  public static JavaparserSourceFile make(final Path path) {
    try {
      LOGGER.debug("Making JavaparserSourceFile instance from: {}", path);
      return new JavaparserSourceFile(JavaParser.parse(path), path.toString());
    } catch (final IOException e) {
      throw new VptRuntimeException("Error creating JavaparserSourceFile object", e);
    }
  }

  /**
   * @return a RunParameter instance representing the JavaParser {@link com.github.javaparser.ast.body.Parameter} p.
   */
  private static RunParameter mapParam(final Parameter p) {
    switch (parameterToTypeNameAsString(p)) {
    case "boolean":
    case "byte":
    case "short":
    case "int":
    case "long":
    case "char":
    case "float":
    case "double":
      return RunParameterPrimitive.of(parameterToTypeNameAsString(p), parameterToVariableNameAsString(p));
    default:
      return RunParameterClass.of(parameterToPackageNameAsString(p), parameterToTypeNameAsString(p),
          parameterToVariableNameAsString(p));
    }
  }

  /**
   * @param param a JavaParser Parameter
   * @return the type of <code>param</code>, as a String
   */
  private static String parameterToPackageNameAsString(final Parameter param) {
    final CombinedTypeSolver cts = new CombinedTypeSolver();
    cts.add(new JreTypeSolver());
    final JavaParserFacade jpf = JavaParserFacade.get(cts);
    final String fqcn = jpf.getType(param, false).asReferenceType().getTypeDeclaration().asClass().getQualifiedName();
    return fqcn.substring(0, fqcn.lastIndexOf("."));
  }

  /**
   * @param param a JavaParser Parameter
   * @return the type of <code>param</code>, as a String
   */
  private static String parameterToTypeNameAsString(final Parameter param) {
    return param.getType().toString();
  }

  /**
   * @param param a JavaParser Parameter
   * @return the variable name of <code>param</code>, as a String
   */
  private static String parameterToVariableNameAsString(final Parameter param) {
    return param.getName();
  }
}
