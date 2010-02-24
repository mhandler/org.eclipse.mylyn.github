package org.eclipse.mylyn.github.ui.internal.editorpart;

import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorPartDescriptor;

public class GitHubTaskEditorPartDescriptor extends TaskEditorPartDescriptor {

    public GitHubTaskEditorPartDescriptor() {
        super("org.eclipse.mylyn.github.editor.parts.github");
        setPath(AbstractTaskEditorPage.PATH_ATTRIBUTES);
    }

    @Override
    public AbstractTaskEditorPart createPart() {
        return new GitHubTaskEditorPart();
    }

}
