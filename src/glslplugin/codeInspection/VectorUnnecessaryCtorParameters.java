package glslplugin.codeInspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import glslplugin.lang.elements.expressions.GLSLExpression;
import glslplugin.lang.elements.expressions.GLSLFunctionCallExpression;
import glslplugin.lang.elements.expressions.GLSLParameterList;
import glslplugin.lang.elements.types.GLSLType;
import glslplugin.lang.elements.types.GLSLTypes;
import glslplugin.lang.elements.types.GLSLVectorType;
import glslplugin.lang.parser.GLSLFile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by wyozi on 13.12.2015.
 */
public class VectorUnnecessaryCtorParameters extends LocalInspectionTool {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Unnecessary Vector Constructor Parameters";
    }

    /**
     * Returns whether this constructor's parameters can be converted into a single value.
     */
    private boolean isVectorConstructor(GLSLFunctionCallExpression funcCall) {
        if (!funcCall.isConstructor())
            return false;

        GLSLType functionType = funcCall.getType();
        return functionType instanceof GLSLVectorType;
    }

    private boolean isUnnecessarilyComplex(GLSLFunctionCallExpression constructorCall) {
        GLSLParameterList parameterList = constructorCall.getParameterList();
        if (parameterList == null) return false;

        GLSLExpression[] parameters = parameterList.getParameters();
        if (parameters.length <= 1) return false;

        // Check that all parameters are (at least implicitly) of same type and equal in value
        for (int i = 0;i < parameters.length-1; i++) {
            GLSLExpression parameter = parameters[i];
            GLSLExpression nextParameter = parameters[i + 1];

            // Must be able to convert at least one of the parameters implicitly to the other parameter.
            GLSLType paramType = parameter.getType();
            GLSLType nextParamType = nextParameter.getType();
            if (!paramType.isConvertibleTo(nextParamType) && !nextParamType.isConvertibleTo(paramType))
                return false;

            // Check for value equality
            Object constant0 = parameter.getConstantValue(), constant1 = nextParameter.getConstantValue();

            // If numerical, convert to doubles. This allows equality checking no matter what numeric type params are
            // We already checked for implicit convertability above, so this is a valid operation here
            if (constant0 instanceof Number) constant0 = ((Number) constant0).doubleValue();
            if (constant1 instanceof Number) constant1 = ((Number) constant1).doubleValue();

            if (constant0 == null || constant1 == null || !constant0.equals(constant1))
                return false;
        }

        return true;
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (!(file instanceof GLSLFile)) return null;

        List<ProblemDescriptor> descriptorList = new ArrayList<ProblemDescriptor>();

        Collection<GLSLFunctionCallExpression> functionCalls = PsiTreeUtil.findChildrenOfAnyType(file, GLSLFunctionCallExpression.class);
        for (GLSLFunctionCallExpression funcCall : functionCalls) {
            if (!isVectorConstructor(funcCall)) continue;
            if (!isUnnecessarilyComplex(funcCall)) continue;

            descriptorList.add(manager.createProblemDescriptor(funcCall, "Unnecessary parameters", new RemoveUnnecessaryParameters(), ProblemHighlightType.WEAK_WARNING, true));
        }

        return descriptorList.toArray(new ProblemDescriptor[descriptorList.size()]);
    }

    private static class RemoveUnnecessaryParameters implements LocalQuickFix {

        @Nls
        @NotNull
        @Override
        public String getName() {
            return "Remove Unnecessary Parameters";
        }

        @Nls
        @NotNull
        @Override
        public String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
            GLSLFunctionCallExpression constructorCall = (GLSLFunctionCallExpression) problemDescriptor.getPsiElement();

            assert constructorCall != null;
            GLSLParameterList parameterList = constructorCall.getParameterList();

            if (parameterList == null)
                return;

            GLSLExpression firstParameter = parameterList.getParameters()[0];

            // Remove everything between first parameter (glslexpression) and last child
            parameterList.deleteChildRange(firstParameter.getNextSibling(), parameterList.getLastChild());
        }
    }
}
