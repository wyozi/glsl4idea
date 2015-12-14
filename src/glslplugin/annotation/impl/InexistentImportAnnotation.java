package glslplugin.annotation.impl;

import com.intellij.lang.annotation.AnnotationHolder;
import glslplugin.annotation.Annotator;
import glslplugin.lang.elements.preprocessor.GLSLPragmaDirective;
import glslplugin.lang.parser.GLSLFile;
import org.jetbrains.annotations.NotNull;

/**
 * Created by wyozi on 14.12.2015.
 */
public class InexistentImportAnnotation extends Annotator<GLSLPragmaDirective> {
    @Override
    public void annotate(GLSLPragmaDirective expr, AnnotationHolder holder) {
        String importingFileName = expr.getImportFileName();
        if (importingFileName == null) return;

        GLSLFile file = (GLSLFile) expr.getContainingFile();
        if (GLSLFile.resolveImport(file, importingFileName) == null) {
            holder.createErrorAnnotation(expr, "Unable to find file '" + importingFileName + "' to import.");
        }
    }

    @NotNull
    @Override
    public Class<GLSLPragmaDirective> getElementType() {
        return GLSLPragmaDirective.class;
    }
}
