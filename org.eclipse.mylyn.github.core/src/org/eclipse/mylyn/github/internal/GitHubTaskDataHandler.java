package org.eclipse.mylyn.github.internal;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.github.internal.connect.GitHubRepositoryConnector;
import org.eclipse.mylyn.github.internal.connect.GitHubService;
import org.eclipse.mylyn.github.internal.connect.GitHubServiceException;
import org.eclipse.mylyn.github.internal.model.GitHubComment;
import org.eclipse.mylyn.github.internal.model.GitHubComments;
import org.eclipse.mylyn.github.internal.model.GitHubIssue;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;

/**
 * 
 * @author Christian Trutz
 * @author Michael Handler
 */
public class GitHubTaskDataHandler extends AbstractTaskDataHandler {

    private static final String DATA_VERSION = "1";

    private GitHubTaskAttributeMapper taskAttributeMapper = null;

    private final GitHubRepositoryConnector connector;

    private DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();

    private DateFormat githubDateFormat = new SimpleDateFormat("yyyy/mm/dd HH:MM:ss Z");

    public GitHubTaskDataHandler(GitHubRepositoryConnector connector) {
        this.connector = connector;
    }

    @Override
    public TaskAttributeMapper getAttributeMapper(TaskRepository taskRepository) {
        if (this.taskAttributeMapper == null)
            this.taskAttributeMapper = new GitHubTaskAttributeMapper(taskRepository);
        return this.taskAttributeMapper;
    }

    public TaskData createPartialTaskData(TaskRepository repository, IProgressMonitor monitor,
            String user, String project, GitHubIssue issue) {

        TaskData data = new TaskData(getAttributeMapper(repository),
                GitHubRepositoryConnector.KIND, repository.getRepositoryUrl(), issue.getNumber());
        data.setVersion(DATA_VERSION);

        createOperations(data, issue);
        createAttributes(issue, data);
        updateTaskDataWithComments(repository, data, issue);

        if (isPartial(data)) {
            data.setPartial(true);
        }

        return data;
    }

    private void createAttributes(GitHubIssue issue, TaskData data) {
        createAttribute(data, GitHubTaskAttributes.COMMENT_NEW, "");
        createAttribute(data, GitHubTaskAttributes.KEY, issue.getNumber());
        createAttribute(data, GitHubTaskAttributes.TITLE, issue.getTitle());
        createAttribute(data, GitHubTaskAttributes.VOTES, issue.getVotes());
        createAttribute(data, GitHubTaskAttributes.BODY, issue.getBody());
        createAttribute(data, GitHubTaskAttributes.STATUS, issue.getState());
        createAttribute(data, GitHubTaskAttributes.CREATION_DATE,
                toLocalDate(issue.getCreated_at()));
        createAttribute(data, GitHubTaskAttributes.MODIFICATION_DATE, toLocalDate(issue
                .getCreated_at()));
        createAttribute(data, GitHubTaskAttributes.CLOSED_DATE, toLocalDate(issue.getClosed_at()));
    }

    private boolean isPartial(TaskData data) {
        for (GitHubTaskAttributes attribute : GitHubTaskAttributes.values()) {
            if (attribute.isRequiredForFullTaskData()) {
                TaskAttribute taskAttribute = data.getRoot().getAttribute(attribute.getId());
                if (taskAttribute == null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createOperations(TaskData data, GitHubIssue issue) {
        TaskAttribute operationAttribute = data.getRoot().createAttribute(TaskAttribute.OPERATION);
        operationAttribute.getMetaData().setType(TaskAttribute.TYPE_OPERATION);

        if (!data.isNew()) {
            if (issue.getState() != null) {
                addOperation(data, issue, GitHubTaskOperation.LEAVE, true);
                if (issue.getState().equals("open")) {
                    addOperation(data, issue, GitHubTaskOperation.CLOSE, false);
                } else if (issue.getState().equals("closed")) {
                    addOperation(data, issue, GitHubTaskOperation.REOPEN, false);
                }
            }
        }

    }

    private void addOperation(TaskData data, GitHubIssue issue, GitHubTaskOperation operation,
            boolean asDefault) {
        TaskAttribute attribute = data.getRoot().createAttribute(
                TaskAttribute.PREFIX_OPERATION + operation.getId());
        String label = createOperationLabel(issue, operation);
        TaskOperation.applyTo(attribute, operation.getId(), label);

        if (asDefault) {
            TaskAttribute operationAttribute = data.getRoot().getAttribute(TaskAttribute.OPERATION);
            TaskOperation.applyTo(operationAttribute, operation.getId(), label);
        }
    }

    private String createOperationLabel(GitHubIssue issue, GitHubTaskOperation operation) {
        return operation == GitHubTaskOperation.LEAVE ? operation.getLabel() + issue.getState()
                : operation.getLabel();
    }

    private String toLocalDate(String date) {
        if (date != null && date.trim().length() > 0) {
            // expect "2010/02/02 22:58:39 -0800"
            try {
                Date d = githubDateFormat.parse(date);
                date = dateFormat.format(d);
            } catch (ParseException e) {
                // ignore
            }
        }
        return date;
    }

    private String toGitHubDate(TaskData taskData, GitHubTaskAttributes attr) {
        TaskAttribute attribute = taskData.getRoot().getAttribute(attr.name());
        String value = attribute == null ? null : attribute.getValue();
        if (value != null) {
            try {
                Date d = dateFormat.parse(value);
                value = githubDateFormat.format(d);
            } catch (ParseException e) {
                // ignore
            }
        }
        return value;
    }

    public TaskData createTaskData(TaskRepository repository, IProgressMonitor monitor,
            String user, String project, GitHubIssue issue) {
        TaskData taskData = createPartialTaskData(repository, monitor, user, project, issue);
        taskData.setPartial(false);
        return taskData;
    }

    public void updateTaskDataWithComments(TaskRepository taskRepository, TaskData taskData,
            GitHubIssue nativeTask) {
        String user = connector.computeTaskRepositoryUser(taskRepository);
        String repo = connector.computeTaskRepositoryProject(taskRepository);
        GitHubComment[] commentArray = loadComments(nativeTask, user, repo);
        nativeTask.setComments(commentArray);

        // Initialize a counter, since you want to number each task, since the
        // editor part likes to display
        // them with numbers.
        int count = 0;

        // Loop through the comments in your native database.
        for (GitHubComment nativeComment : nativeTask.getComments()) {
            TaskCommentMapper mapper = new TaskCommentMapper();
            // Set properties and text associated with this comment.
            mapper.setAuthor(taskRepository.createPerson(nativeComment.getUser()));
            Date creationDate = parseDate(nativeComment.getCreated_at());
            mapper.setCreationDate(creationDate);
            mapper.setText(nativeComment.getBody());
            mapper.setNumber(count);

            TaskAttribute attribute = taskData.getRoot().createAttribute(
                    TaskAttribute.PREFIX_COMMENT + count);

            mapper.applyTo(attribute);
            count++;
        }

    }

    private GitHubComment[] loadComments(GitHubIssue nativeTask, String user, String repo) {
        GitHubComment[] commentArray = new GitHubComment[0];
        GitHubComments comments = null;
        try {
            comments = connector.getService().getComments(user, repo, nativeTask.getNumber());
        } catch (GitHubServiceException e) {
            // ignore
        }
        if (comments != null && comments.getComments() != null) {
            commentArray = comments.getComments();
        }
        return commentArray;
    }

    private Date parseDate(String dateString) {
        try {
            return githubDateFormat.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    private GitHubIssue createIssue(TaskData taskData) {
        GitHubIssue issue = new GitHubIssue();
        if (!taskData.isNew()) {
            issue.setNumber(taskData.getTaskId());
        }
        issue.setBody(getAttributeValue(taskData, GitHubTaskAttributes.BODY));
        issue.setTitle(getAttributeValue(taskData, GitHubTaskAttributes.TITLE));
        issue.setVotes(getAttributeValue(taskData, GitHubTaskAttributes.VOTES));
        issue.setState(getAttributeValue(taskData, GitHubTaskAttributes.STATUS));
        issue.setCreated_at(toGitHubDate(taskData, GitHubTaskAttributes.CREATION_DATE));
        issue.setCreated_at(toGitHubDate(taskData, GitHubTaskAttributes.MODIFICATION_DATE));
        issue.setCreated_at(toGitHubDate(taskData, GitHubTaskAttributes.CLOSED_DATE));
        return issue;
    }

    private String getAttributeValue(TaskData taskData, GitHubTaskAttributes attr) {
        TaskAttribute attribute = taskData.getRoot().getAttribute(attr.getId());
        return attribute == null ? null : attribute.getValue();
    }

    private void createAttribute(TaskData data, GitHubTaskAttributes attribute, String value) {
        TaskAttribute attr = data.getRoot().createAttribute(attribute.getId());
        TaskAttributeMetaData metaData = attr.getMetaData();
        metaData.defaults().setType(attribute.getType()).setKind(attribute.getKind()).setLabel(
                attribute.getLabel()).setReadOnly(attribute.isReadOnly());

        if (value != null) {
            attr.addValue(value);
        }
    }

    @Override
    public boolean initializeTaskData(TaskRepository repository, TaskData data,
            ITaskMapping initializationData, IProgressMonitor monitor) throws CoreException {

        data.setVersion(DATA_VERSION);

        for (GitHubTaskAttributes attr : GitHubTaskAttributes.values()) {
            if (attr.isInitTask()) {
                createAttribute(data, attr, null);
            }
        }

        return true;
    }

    @Override
    public RepositoryResponse postTaskData(TaskRepository repository, TaskData taskData,
            Set<TaskAttribute> oldAttributes, IProgressMonitor monitor) throws CoreException {

        GitHubIssue issue = createIssue(taskData);
        String user = connector.computeTaskRepositoryUser(repository);
        String repo = connector.computeTaskRepositoryProject(repository);
        try {
            GitHubService service = connector.getService();
            GitHubCredentials credentials = GitHubCredentials.create(repository);
            if (taskData.isNew()) {
                issue = service.openIssue(user, repo, issue, credentials);
            } else {
                checkAndAddComment(taskData, issue, user, repo, service, credentials);
                performOperation(taskData, issue, user, repo, service, credentials);
            }
            return new RepositoryResponse(taskData.isNew() ? ResponseKind.TASK_CREATED
                    : ResponseKind.TASK_UPDATED, issue.getNumber());
        } catch (GitHubServiceException e) {
            throw new CoreException(GitHub.createErrorStatus(e));
        }

    }

    private void performOperation(TaskData taskData, GitHubIssue issue, String user, String repo,
            GitHubService service, GitHubCredentials credentials) throws GitHubServiceException {

        TaskAttribute operationAttribute = taskData.getRoot().getAttribute(TaskAttribute.OPERATION);
        GitHubTaskOperation operation = null;
        if (operationAttribute != null) {
            String opId = operationAttribute.getValue();
            operation = GitHubTaskOperation.fromId(opId);
        }

        if (operation != null && operation != GitHubTaskOperation.LEAVE) {
            service.editIssue(user, repo, issue, credentials);
            switch (operation) {
            case REOPEN:
                service.reopenIssue(user, repo, issue, credentials);
                break;
            case CLOSE:
                service.closeIssue(user, repo, issue, credentials);
                break;
            default:
                throw new IllegalStateException("not implemented: " + operation);
            }
        } else {
            service.editIssue(user, repo, issue, credentials);
        }
    }

    private void checkAndAddComment(TaskData taskData, GitHubIssue issue, String user, String repo,
            GitHubService service, GitHubCredentials credentials) throws GitHubServiceException {
        TaskAttribute commentAttribute = taskData.getRoot().getAttribute(TaskAttribute.COMMENT_NEW);
        String comment = commentAttribute.getValue();
        if (comment != null && !comment.equals("")) {
            service.addComment(user, repo, issue, credentials, comment);
        }
    }

}
