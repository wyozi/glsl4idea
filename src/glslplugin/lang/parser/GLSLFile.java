/*
 *     Copyright 2010 Jean-Paul Balabanian and Yngve Devik Hammersland
 *
 *     This file is part of glsl4idea.
 *
 *     Glsl4idea is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as
 *     published by the Free Software Foundation, either version 3 of
 *     the License, or (at your option) any later version.
 *
 *     Glsl4idea is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with glsl4idea.  If not, see <http://www.gnu.org/licenses/>.
 */

package glslplugin.lang.parser;

import com.intellij.psi.*;
import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import glslplugin.GLSLSupportLoader;
import glslplugin.lang.elements.preprocessor.GLSLPragmaDirective;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class GLSLFile extends PsiFileBase {
    public GLSLFile(FileViewProvider fileViewProvider) {
        super(fileViewProvider, GLSLSupportLoader.GLSL.getLanguage());
    }

    public static GLSLFile resolveImport(GLSLFile source, String fileName) {
        // TODO only look from same folder
        PsiFile[] files = PsiShortNamesCache.getInstance(source.getProject()).getFilesByName(fileName);
        if (files.length == 1) {
            return ((GLSLFile) files[0]);
        } else {
            return null;
        }
    }

    public String[] getImportedFilenames() {
        final List<String> fileNames = new ArrayList<String>();

        Iterator<GLSLPragmaDirective> pragmaDirectiveIterator = PsiTreeUtil.childIterator(this, GLSLPragmaDirective.class);
        while (pragmaDirectiveIterator.hasNext()) {
            String fileName = pragmaDirectiveIterator.next().getImportFileName();
            if (fileName != null)
                fileNames.add(fileName);
        }

        return fileNames.toArray(new String[fileNames.size()]);
    }

    private GLSLFile[] importedFiles;
    public GLSLFile[] getImportedFiles() {
        //calcTreeElement().getChildrenAsPsiElements()
        if (importedFiles == null) {
            List<GLSLFile> tmpImportedFiles = new ArrayList<GLSLFile>();
            for (String importedFileName : getImportedFilenames()) {
                GLSLFile importedFile = resolveImport(this, importedFileName);
                if (importedFile != null) tmpImportedFiles.add(importedFile);
            }
            return tmpImportedFiles.toArray(new GLSLFile[tmpImportedFiles.size()]); // TODO cache
        }
        return importedFiles;
    }

    private <T extends PsiNamedElement> void findAndAddTopLevelElements(List<T> out, PsiElement source, String typeName, Class<T> clazz) {
        Collection<T> namedChildren = PsiTreeUtil.findChildrenOfType(source, clazz);
        for (T pne : namedChildren) {
            if (typeName.equals(pne.getName())) {
                out.add(pne);
            }
        }
    }

    public <T extends PsiNamedElement> List<T> findElementsFromImportedFiles(String typeName, Class<T> clazz) {
        List<T> elements = new ArrayList<T>();
        // TODO this might infinite loop if cyclic imports
        for (GLSLFile importedFile : getImportedFiles()) {
            findAndAddTopLevelElements(elements, importedFile, typeName, clazz);
        }
        return elements;
    }
    public <T extends PsiNamedElement> T findElementFromImportedFiles(String typeName, Class<T> clazz) {
        List<T> elements = findElementsFromImportedFiles(typeName, clazz);
        return elements.size() > 0 ? elements.get(0) : null;
    }

    /**
     * Finds top level named elements of given type with given name from this file and all its imports.
     */
    public <T extends PsiNamedElement> List<T> findGlobalNamedElements(String typeName, Class<T> clazz) {
        List<T> elements = new ArrayList<T>();
        findAndAddTopLevelElements(elements, this, typeName, clazz);

        // TODO this might infinite loop if cyclic imports
        for (GLSLFile importedFile : getImportedFiles()) {
            findAndAddTopLevelElements(elements, importedFile, typeName, clazz);
        }
        return elements;
    }
    public <T extends PsiNamedElement> T findGlobalNamedElement(String typeName, Class<T> clazz) {
        List<T> elements = findGlobalNamedElements(typeName, clazz);
        return elements.size() > 0 ? elements.get(0) : null;
    }

    @NotNull
    public FileType getFileType() {
        return GLSLSupportLoader.GLSL;
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
        PsiElement child = lastParent.getPrevSibling();
        while (child != null) {
            if (!child.processDeclarations(processor, state, lastParent, place)) return false;
            child = child.getPrevSibling();
        }
        return true;
    }
}
