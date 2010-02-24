package org.eclipse.mylyn.github.ui.internal.editorpart;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPart;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class GitHubTaskEditorPart extends AbstractTaskEditorPart {

    @Override
    public String getPartName() {
        return "GitHub Attributes";
    }

    @Override
    public void createControl(Composite parent, FormToolkit toolkit) {
        int style = ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE
                | ExpandableComposite.EXPANDED;

        Section section = createSection(parent, toolkit, style);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(section);
        section.setText("Github Attributes");
        GridDataFactory.fillDefaults().grab(true, false).applyTo(section);
        setSection(toolkit, section);

        Composite composite = toolkit.createComposite(section);
        GridLayout layout2 = createSectionClientLayout();
        layout2.numColumns = 1;
        composite.setLayout(layout2);

        Composite headerComposite = toolkit.createComposite(composite);
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        headerComposite.setLayout(layout);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(headerComposite);

        TaskAttribute statusAtribute = getTaskData().getRoot().getMappedAttribute(
                "task.github.votes");
        addAttribute(headerComposite, toolkit, statusAtribute, 0);

        // ensure layout does not wrap
        layout.numColumns = headerComposite.getChildren().length;

        // ensure that the composite does not show a bunch of blank space
        if (layout.numColumns == 0) {
            layout.numColumns = 1;
            toolkit.createLabel(headerComposite, " "); //$NON-NLS-1$
        }

        toolkit.paintBordersFor(composite);
        section.setClient(composite);
    }

    private GridLayout createSectionClientLayout() {
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        // leave 1px for borders
        layout.marginTop = 2;
        // spacing if a section is expanded
        layout.marginBottom = 8;
        return layout;
    }

    private void addAttribute(Composite composite, FormToolkit toolkit, TaskAttribute attribute,
            int indent) {
        AbstractAttributeEditor editor = createAttributeEditor(attribute);
        if (editor != null) {
            // having editable controls in the header looks odd
            editor.setReadOnly(true);
            editor.setDecorationEnabled(false);

            editor.createLabelControl(composite, toolkit);
            GridDataFactory.defaultsFor(editor.getLabelControl()).indent(indent, 0).applyTo(
                    editor.getLabelControl());

            editor.createControl(composite, toolkit);
            getTaskEditorPage().getAttributeEditorToolkit().adapt(editor);
        }
    }

}
