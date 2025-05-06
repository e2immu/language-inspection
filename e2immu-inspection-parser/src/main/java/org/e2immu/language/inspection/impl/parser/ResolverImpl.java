package org.e2immu.language.inspection.impl.parser;


import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.*;
import org.e2immu.language.inspection.api.parser.Context;
import org.e2immu.language.inspection.api.parser.ForwardType;
import org.e2immu.language.inspection.api.parser.ParseHelper;
import org.e2immu.language.inspection.api.parser.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class ResolverImpl implements Resolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Resolver.class);

    private final ParseHelper parseHelper;
    private final ComputeMethodOverrides computeMethodOverrides;

    public ResolverImpl(ComputeMethodOverrides computeMethodOverrides,
                        ParseHelper parseHelper) {
        this.parseHelper = parseHelper;
        this.computeMethodOverrides = computeMethodOverrides;
    }

    record Todo(Info info,
                Info.Builder<?> infoBuilder,
                ForwardType forwardType,
                Object eci,
                Object expression,
                Context context) {
    }

    record AnnotationTodo(Info.Builder<?> infoBuilder, AnnotationExpression.Builder annotationExpressionBuilder,
                          int indexInAnnotationList, Object annotation, Context context) {
    }

    private final List<Todo> todos = new LinkedList<>();
    private final List<AnnotationTodo> annotationTodos = new LinkedList<>();
    private final List<TypeInfo.Builder> types = new LinkedList<>();
    private final List<MethodInfo> recordAccessors = new LinkedList<>();
    private final List<FieldInfo> recordFields = new LinkedList<>();

    @Override
    public Resolver newEmpty() {
        return new ResolverImpl(computeMethodOverrides, parseHelper);
    }

    @Override
    public void add(Info info, Info.Builder<?> infoBuilder, ForwardType forwardType, Object eci, Object expression,
                    Context context) {
        todos.add(new Todo(info, infoBuilder, forwardType, eci, expression, context));
    }

    @Override
    public void addAnnotationTodo(Info.Builder<?> infoBuilder, AnnotationExpression.Builder ab, int indexInAnnotationList,
                                  Object annotation, Context context) {
        annotationTodos.add(new AnnotationTodo(infoBuilder, ab, indexInAnnotationList, annotation, context));
    }

    @Override
    public void addRecordAccessor(MethodInfo accessor) {
        recordAccessors.add(accessor);
    }

    @Override
    public void addRecordField(FieldInfo recordField) {
        recordFields.add(recordField);
    }

    @Override
    public void add(TypeInfo.Builder typeInfoBuilder) {
        types.add(typeInfoBuilder);
    }

    public void resolve() {
        LOGGER.info("Start resolving {} annotations, {} type(s), {} field(s)/method(s)", annotationTodos.size(),
                types.size(), todos.size());

        for (AnnotationTodo annotationTodo : annotationTodos) {
            AnnotationExpression ae = parseAnnotationExpression(annotationTodo);
            annotationTodo.infoBuilder.setAnnotationExpression(annotationTodo.indexInAnnotationList, ae);
        }

        int cnt = 0;
        try {
            for (Todo todo : todos) {
                if (todo.infoBuilder instanceof FieldInfo.Builder builder) {
                    boolean success = true;
                    try {
                        resolveField(todo, builder);
                    } catch (RuntimeException re) {
                        success = false;
                        todo.context.summary().addParserError(re);
                    }
                    todo.context.summary().addType(todo.context.enclosingType().primaryType(), success);
                } else if (todo.infoBuilder instanceof MethodInfo.Builder builder) {
                    boolean success = true;
                    try {
                        resolveMethod(todo, builder);
                    } catch (RuntimeException re) {
                        LOGGER.error("Caught exception resolving {}", todo.info);
                        success = false;
                        todo.context.summary().addParserError(re);
                    }
                    todo.context.summary().addType(todo.context.enclosingType().primaryType(), success);
                    todo.context.summary().addMethod(success);
                } else throw new UnsupportedOperationException("In java, we cannot have expressions in other places");
                ++cnt;
            }
        } catch (RuntimeException re) {
            LOGGER.error("Failed after resolving {} of {} fields/methods", cnt, todos.size());
            throw re;
        }

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

    private AnnotationExpression parseAnnotationExpression(AnnotationTodo at) {
        List<AnnotationExpression.KV> kvs = parseHelper.parseAnnotationExpression(at.annotation, at.context);
        at.annotationExpressionBuilder.setKeyValuesPairs(kvs);
        return at.annotationExpressionBuilder().build();
    }

    private void resolveField(Todo todo, FieldInfo.Builder builder) {
        Expression e = parseHelper.parseExpression(todo.context, "", todo.forwardType, todo.expression);
        builder.setInitializer(e);
        builder.commit();
    }

    private void resolveMethod(Todo todo, MethodInfo.Builder builder) {
        parseHelper.resolveMethodInto(builder, todo.context, todo.forwardType, todo.eci, todo.expression);
        MethodInfo methodInfo = (MethodInfo) todo.info;
        builder.addOverrides(computeMethodOverrides.overrides(methodInfo));
        builder.commit();
    }

    @Override
    public ParseHelper parseHelper() {
        return parseHelper;
    }
}
