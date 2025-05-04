package org.e2immu.language.inspection.api.parser;

import org.e2immu.language.cst.api.expression.AnnotationExpression;
import org.e2immu.language.cst.api.expression.Expression;
import org.e2immu.language.cst.api.info.MethodInfo;

import java.util.List;

public interface ParseHelper {

    List<AnnotationExpression.KV> parseAnnotationExpression(Object annotation, Context context);

    Expression parseExpression(Context context, String index, ForwardType forward, Object expression);

    void resolveMethodInto(MethodInfo.Builder methodInfoBuilder, Context context, ForwardType forwardType,
                           Object eci, Object expression);
}
