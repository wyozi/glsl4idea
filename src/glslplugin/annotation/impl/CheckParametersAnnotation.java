package glslplugin.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import glslplugin.annotation.Annotator;
import glslplugin.lang.elements.expressions.GLSLFunctionCallExpression;
import glslplugin.lang.elements.types.GLSLFunctionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by wyozi on 14.12.2015.
 */
public class CheckParametersAnnotation extends Annotator<GLSLFunctionCallExpression> {
    @Override
    public void annotate(GLSLFunctionCallExpression expr, AnnotationHolder holder) {
        List<GLSLFunctionType> possibleCalledFunctions = expr.getPossibleCalledFunctions(true);

        // A strictly typed match found; call should be safe
        if (!possibleCalledFunctions.isEmpty()) return;

        List<GLSLFunctionType> possibleCalledFunctions1 = expr.getPossibleCalledFunctions(false);

        // There were no matches with incompatible types; assume function comes from external source and we shouldn't care about it
        if (possibleCalledFunctions1.isEmpty()) return;

        holder.createWarningAnnotation(expr, "Possibly incorrect arguments: no function definition found that accepts these arguments");
    }

    @NotNull
    @Override
    public Class<GLSLFunctionCallExpression> getElementType() {
        return GLSLFunctionCallExpression.class;
    }
}
