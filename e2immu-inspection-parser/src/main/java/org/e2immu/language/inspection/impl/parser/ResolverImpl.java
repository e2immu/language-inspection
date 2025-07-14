package org.e2immu.language.inspection.impl.parser;


import org.e2immu.language.cst.api.element.JavaDoc;
import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Assignment;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.cst.api.statement.Statement;
import org.e2immu.language.cst.api.type.TypeParameter;
import org.e2immu.language.inspection.api.parser.*;
import org.e2immu.util.internal.graph.util.TimedLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ResolverImpl implements Resolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverImpl.class);
    private static final TimedLogger TIMED_LOGGER = new TimedLogger(LOGGER, 1000L);
    private final ParseHelper parseHelper;
    private final ComputeMethodOverrides computeMethodOverrides;

    public ResolverImpl(ComputeMethodOverrides computeMethodOverrides, ParseHelper parseHelper, boolean parallel) {
        this.parseHelper = parseHelper;
        this.computeMethodOverrides = computeMethodOverrides;
        this.parallel = parallel;
    }

    record Todo(Info info,
                Info.Builder<?> infoBuilder,
                ForwardType forwardType,
                Object eci,
                Object expression,
                Context context,
                List<Statement> recordAssignments) {
    }

    record AnnotationTodo(Info.Builder<?> infoBuilder,
                          TypeInfo annotationType,
                          AnnotationExpression.Builder annotationExpressionBuilder,
                          int indexInAnnotationList,
                          Object annotation,
                          Context context) {
    }

    record JavaDocToDo(Info info, Info.Builder<?> infoBuilder, Context context, JavaDoc javaDoc) {
    }

    private final List<Todo> todos = new LinkedList<>();
    private final List<AnnotationTodo> annotationTodos = new LinkedList<>();
    private final List<TypeInfo.Builder> types = new LinkedList<>();
    private final List<MethodInfo> recordAccessors = new LinkedList<>();
    private final List<FieldInfo> recordFields = new LinkedList<>();
    private final List<JavaDocToDo> javaDocs = new LinkedList<>();
    private final boolean parallel;

    @Override
    public Resolver newEmpty() {
        return new ResolverImpl(computeMethodOverrides, parseHelper, parallel);
    }

    @Override
    public void add(Info info, Info.Builder<?> infoBuilder, ForwardType forwardType, Object eci, Object expression,
                    Context context, List<Statement> recordAssignments) {
        synchronized (todos) {
            todos.add(new Todo(info, infoBuilder, forwardType, eci, expression, context, recordAssignments));
        }
    }

    @Override
    public void addJavadoc(Info info, Info.Builder<?> infoBuilder, Context context, JavaDoc javaDoc) {
        synchronized (javaDocs) {
            javaDocs.add(new JavaDocToDo(info, infoBuilder, context, javaDoc));
        }
    }

    @Override
    public void addAnnotationTodo(Info.Builder<?> infoBuilder,
                                  TypeInfo annotationType,
                                  AnnotationExpression.Builder ab,
                                  int indexInAnnotationList,
                                  Object annotation, Context context) {
        synchronized (annotationTodos) {
            annotationTodos.add(new AnnotationTodo(infoBuilder, annotationType, ab, indexInAnnotationList, annotation, context));
        }
    }

    @Override
    public void addRecordAccessor(MethodInfo accessor) {
        synchronized (recordAccessors) {
            recordAccessors.add(accessor);
        }
    }

    @Override
    public void addRecordField(FieldInfo recordField) {
        synchronized (recordFields) {
            recordFields.add(recordField);
        }
    }

    @Override
    public void add(TypeInfo.Builder typeInfoBuilder) {
        synchronized (types) {
            types.add(typeInfoBuilder);
        }
    }

    public void resolve(boolean primary) {
        if (primary) {
            LOGGER.info("Phase 4: Start resolving {} annotations, {} type(s), {} field(s)/method(s)", annotationTodos.size(),
                    types.size(), todos.size());
        }

        Stream<AnnotationTodo> annotationStream = parallel ? annotationTodos.parallelStream() : annotationTodos.stream();
        annotationStream.forEach(annotationTodo -> {
            try {
                AnnotationExpression ae = parseAnnotationExpression(annotationTodo);
                annotationTodo.infoBuilder.setAnnotationExpression(annotationTodo.indexInAnnotationList, ae);
            } catch (RuntimeException | AssertionError re) {
                LOGGER.error("Caught exception resolving annotation {}", annotationTodo);
                Summary.ParseException pe = new Summary.ParseException(annotationTodo.context, annotationTodo.infoBuilder, re.getMessage(), re);
                annotationTodo.context.summary().addParseException(pe);
            }
        });
        Stream<JavaDocToDo> javaDocToDoStream = parallel ? javaDocs.parallelStream() : javaDocs.stream();
        javaDocToDoStream.forEach(javaDocToDo -> {
            try {
                JavaDoc resolved = resolveJavaDoc(javaDocToDo);
                javaDocToDo.infoBuilder.setJavaDoc(resolved);
            } catch (RuntimeException | AssertionError re) {
                LOGGER.error("Caught exception resolving javaDoc {}", javaDocToDo.info);
                Summary.ParseException pe = new Summary.ParseException(javaDocToDo.context, javaDocToDo.info, re.getMessage(), re);
                javaDocToDo.context.summary().addParseException(pe);
            }
        });

        AtomicInteger done = new AtomicInteger();
        Stream<Todo> todoStream = parallel ? todos.parallelStream() : todos.stream();
        todoStream.forEach(todo -> {
            if (todo.infoBuilder instanceof FieldInfo.Builder builder) {
                try {
                    resolveField(todo, builder);
                } catch (RuntimeException | AssertionError re) {
                    LOGGER.error("Caught exception resolving field {}, done {}", todo.info, done);
                    Summary.ParseException pe = new Summary.ParseException(todo.context, todo.info, re.getMessage(), re);
                    todo.context.summary().addParseException(pe);
                }
                todo.context.summary().addType(todo.context.enclosingType().primaryType());
            } else if (todo.infoBuilder instanceof MethodInfo.Builder builder) {
                try {
                    resolveMethod(todo, builder);
                } catch (RuntimeException | AssertionError re) {
                    LOGGER.error("Caught exception resolving method {}, done {}", todo.info, done);
                    Summary.ParseException pe = new Summary.ParseException(todo.context, todo.info, re.getMessage(), re);
                    todo.context.summary().addParseException(pe);
                }
                todo.context.summary().addType(todo.context.enclosingType().primaryType());
            } else throw new UnsupportedOperationException("In java, we cannot have expressions in other places");
            done.incrementAndGet();
            TIMED_LOGGER.info("Phase 4: parsing bodies {} of {} methods/field initializers", done, todos.size());
        });

        for (FieldInfo recordField : recordFields) {
            recordField.builder().commit();
        }
        for (MethodInfo accessor : recordAccessors) {
            accessor.builder().addOverrides(computeMethodOverrides.overrides(accessor));
            accessor.builder().commit();
        }
        for (TypeInfo.Builder builder : types) {
            builder.commit();
        }
    }

    private JavaDoc resolveJavaDoc(JavaDocToDo javaDocToDo) {
        List<JavaDoc.Tag> newTags = javaDocToDo.javaDoc.tags().stream()
                .filter(tag -> tag.identifier() != null)
                .map(tag -> {
                    if (tag.identifier().isReference()) {
                        return parseHelper.parseJavaDocReferenceInTag(javaDocToDo.context, javaDocToDo.info, tag);
                    }
                    if (tag.identifier() == JavaDoc.TagIdentifier.PARAM) {
                        String trimmedContent = tag.content();
                        if (trimmedContent.startsWith("<") && trimmedContent.endsWith(">")) {
                            String typeParameterName = trimmedContent.substring(1, trimmedContent.length() - 1);
                            List<TypeParameter> typeParameters;
                            if (javaDocToDo.info instanceof TypeInfo ti) typeParameters = ti.typeParameters();
                            else if (javaDocToDo.info instanceof MethodInfo mi) typeParameters = mi.typeParameters();
                            else typeParameters = null;
                            if (typeParameters != null) {
                                TypeParameter typeParameter = typeParameters.stream()
                                        .filter(tp -> typeParameterName.equals(tp.simpleName()))
                                        .findFirst().orElse(null);
                                return tag.withResolvedReference(typeParameter);
                            }
                        } else if (javaDocToDo.info instanceof MethodInfo mi) {
                            ParameterInfo pi = mi.parameters().stream()
                                    .filter(p -> p.name().equals(trimmedContent))
                                    .findFirst().orElse(null);
                            return tag.withResolvedReference(pi);
                        }
                    }
                    return tag;
                }).toList();
        return javaDocToDo.javaDoc.withTags(newTags);
    }

    private AnnotationExpression parseAnnotationExpression(AnnotationTodo at) {
        List<AnnotationExpression.KV> kvs = parseHelper.parseAnnotationExpression(at.annotationType, at.annotation,
                at.context);
        at.annotationExpressionBuilder.setKeyValuesPairs(kvs);
        return at.annotationExpressionBuilder().build();
    }

    private void resolveField(Todo todo, FieldInfo.Builder builder) {
        Expression e = parseHelper.parseExpression(todo.context, "", todo.forwardType, todo.expression);
        builder.setInitializer(e);
        builder.commit();
    }

    private void resolveMethod(Todo todo, MethodInfo.Builder builder) {
        parseHelper.resolveMethodInto(builder, todo.context, todo.forwardType, todo.eci, todo.expression,
                todo.recordAssignments);
        MethodInfo methodInfo = (MethodInfo) todo.info;
        builder.addOverrides(computeMethodOverrides.overrides(methodInfo));
        builder.commit();
    }

    @Override
    public ParseHelper parseHelper() {
        return parseHelper;
    }
}
