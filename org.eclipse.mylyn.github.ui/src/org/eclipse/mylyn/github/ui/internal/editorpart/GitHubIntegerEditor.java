package org.eclipse.mylyn.github.ui.internal.editorpart;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskDataModel;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class GitHubIntegerEditor extends AbstractAttributeEditor {

    public GitHubIntegerEditor(TaskDataModel manager, TaskAttribute taskAttribute) {
        super(manager, taskAttribute);
    }

    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        Text text = new Text(parent, SWT.FLAT | SWT.READ_ONLY);
        toolkit.adapt(text, false, false);
        text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
        text.setText(getTaskAttribute().getValue());
        setControl(text);
    }

}
