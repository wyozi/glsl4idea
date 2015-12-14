package glslplugin.lang.elements.preprocessor;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.util.IncorrectOperationException;
import glslplugin.lang.elements.GLSLPsiElementFactory;
import glslplugin.lang.elements.GLSLTokenTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GLSLPragmaDirective extends GLSLPreprocessorDirective {
    public GLSLPragmaDirective(@NotNull ASTNode astNode) {
        super(astNode);
    }

    private static final Pattern inQuotationPattern = Pattern.compile("\"(.*)\"");

    /**
     * Returns <code>filename.glsl</code> if this pragma is in form of
     * <code>#pragma import "filename.glsl"</code>
     */
    public String getImportFileName() {
        PsiElement pragmaNameElement = getPragmaNameElement();
        if (pragmaNameElement == null || !"import".equals(pragmaNameElement.getText())) return null;

        String pragmaParameter = getPragmaParameter(pragmaNameElement).trim();
        Matcher matcher = inQuotationPattern.matcher(pragmaParameter);
        if (!matcher.find()) return null;

        return matcher.group(1);
    }

    @Nullable
    private PsiElement getPragmaNameElement() {
        PsiElement child = getFirstChild();
        while (child != null) { // we can't iterate over getChildren(), as that ignores leaf elements
            if (child.getNode().getElementType() == GLSLTokenTypes.IDENTIFIER) return child;
            child = child.getNextSibling();
        }
        return null;
    }

    /**
     * @return text after the name
     */
    @NotNull
    private String getPragmaParameter(PsiElement nameElement){
        if (nameElement == null)
             return "";

        int textStart = nameElement.getTextOffset() + nameElement.getTextLength();
        int textEnd = getTextOffset() + getTextLength();
        final String text = getContainingFile().getText();
        if(textStart >= textEnd || textStart < 0 || textEnd > text.length()) return "";
        return text.substring(textStart, textEnd).trim();
    }

    @Override
    public String toString() {
        PsiElement pragmaNameElement = getPragmaNameElement();
        return "#Pragma Directive "+ pragmaNameElement +" => "+getPragmaParameter(pragmaNameElement);
    }
}
