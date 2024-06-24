package org.e2immu.language.inspection.impl.parser;


import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.FieldInfo;
import org.e2immu.language.cst.api.info.Info;
import org.e2immu.language.cst.api.info.MethodInfo;
import org.e2immu.language.cst.api.info.TypeInfo;
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

    public ResolverImpl(ParseHelper parseHelper) {
        this.parseHelper = parseHelper;
    }

    record Todo(Info.Builder<?> infoBuilder, ForwardType forwardType, Object eci, Object expression, Context context) {
    }

    private final List<Todo> todos = new LinkedList<>();
    private final List<TypeInfo.Builder> types = new LinkedList<>();

    public Resolver newEmpty() {
        return new ResolverImpl(parseHelper);
    }

    public void add(Info.Builder<?> infoBuilder, ForwardType forwardType, Object eci, Object expression,
                    Context context) {
        todos.add(new Todo(infoBuilder, forwardType, eci, expression, context));
    }

    @Override
    public void add(TypeInfo.Builder typeInfoBuilder) {
        types.add(typeInfoBuilder);
    }

    public void resolve() {
        LOGGER.info("Start resolving {} type(s), {} field(s)/method(s)", types.size(), todos.size());

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
                    success = false;
                    todo.context.summary().addParserError(re);
                }
                todo.context.summary().addType(todo.context.enclosingType().primaryType(), success);
                todo.context.summary().addMethod(success);
            } else throw new UnsupportedOperationException("In java, we cannot have expressions in other places");
        }
        for (TypeInfo.Builder builder : types) {
            builder.commit();
        }
    }

    private void resolveField(Todo todo, FieldInfo.Builder builder) {
        Expression e = parseHelper.parseExpression(todo.context, "", todo.forwardType, todo.expression);
        builder.setInitializer(e);
        builder.commit();
    }

    private void resolveMethod(Todo todo, MethodInfo.Builder builder) {
        parseHelper.resolveMethodInto(builder, todo.context, todo.forwardType, todo.eci, todo.expression);
    }

    @Override
    public ParseHelper parseHelper() {
        return parseHelper;
    }
}
