package patterns.command;

import patterns.repository.TestResult;

/**
 * Interface for UI commands that can be executed, undone, and logged.
 * This provides a foundation for implementing the Command pattern in UI automation.
 */
public interface UICommand {
    
    /**
     * Executes the command.
     * @return true if execution was successful, false otherwise
     */
    boolean execute();
    
    /**
     * Undoes the command if possible.
     * @return true if undo was successful, false otherwise
     */
    boolean undo();
    
    /**
     * Gets a description of what this command does.
     * @return Command description
     */
    String getDescription();
    
    /**
     * Gets the result of the command execution.
     * @return TestResult containing execution details
     */
    TestResult getResult();
    
    /**
     * Checks if this command can be undone.
     * @return true if undoable, false otherwise
     */
    boolean isUndoable();
    
    /**
     * Gets the timestamp when the command was executed.
     * @return Execution timestamp
     */
    long getExecutionTime();
    
    /**
     * Gets the duration of command execution in milliseconds.
     * @return Execution duration in milliseconds
     */
    long getExecutionDuration();
}
