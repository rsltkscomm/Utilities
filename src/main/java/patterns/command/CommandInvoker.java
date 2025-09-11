package patterns.command;

import reporting.TestLogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Invoker class that manages command execution, history, and undo functionality.
 * This implements the Command pattern's invoker role.
 */
public class CommandInvoker {
    
    private final List<UICommand> commandHistory;
    private final List<UICommand> undoableCommands;
    private final AtomicInteger executionCount;
    private final boolean enableHistory;
    private final int maxHistorySize;
    
    public CommandInvoker() {
        this(true, 100);
    }
    
    public CommandInvoker(boolean enableHistory, int maxHistorySize) {
        this.commandHistory = new ArrayList<>();
        this.undoableCommands = new ArrayList<>();
        this.executionCount = new AtomicInteger(0);
        this.enableHistory = enableHistory;
        this.maxHistorySize = maxHistorySize;
    }
    
    /**
     * Executes a command and adds it to history if enabled.
     * @param command The command to execute
     * @return true if execution was successful, false otherwise
     */
    public boolean executeCommand(UICommand command) {
        if (command == null) {
            TestLogManager.error("Cannot execute null command");
            return false;
        }
        
        TestLogManager.info("Executing command: " + command.getDescription());
        
        boolean result = command.execute();
        executionCount.incrementAndGet();
        
        if (enableHistory) {
            addToHistory(command);
        }
        
        if (result) {
            TestLogManager.success("Command executed successfully: " + command.getDescription());
        } else {
            TestLogManager.error("Command execution failed: " + command.getDescription());
            if (command.getResult() != null && command.getResult().getErrorMessage() != null) {
                TestLogManager.error("Error: " + command.getResult().getErrorMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Executes multiple commands in sequence.
     * @param commands List of commands to execute
     * @return true if all commands executed successfully, false otherwise
     */
    public boolean executeCommands(List<UICommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return true;
        }
        
        boolean allSuccessful = true;
        for (UICommand command : commands) {
            if (!executeCommand(command)) {
                allSuccessful = false;
                // Continue executing remaining commands
            }
        }
        
        return allSuccessful;
    }
    
    /**
     * Undoes the last undoable command.
     * @return true if undo was successful, false otherwise
     */
    public boolean undoLastCommand() {
        if (undoableCommands.isEmpty()) {
            TestLogManager.warning("No undoable commands available");
            return false;
        }
        
        UICommand lastCommand = undoableCommands.get(undoableCommands.size() - 1);
        
        TestLogManager.info("Undoing command: " + lastCommand.getDescription());
        
        boolean result = lastCommand.undo();
        
        if (result) {
            undoableCommands.remove(lastCommand);
            TestLogManager.success("Command undone successfully: " + lastCommand.getDescription());
        } else {
            TestLogManager.error("Failed to undo command: " + lastCommand.getDescription());
        }
        
        return result;
    }
    
    /**
     * Undoes all undoable commands in reverse order.
     * @return Number of commands successfully undone
     */
    public int undoAllCommands() {
        int undoneCount = 0;
        
        // Undo in reverse order
        for (int i = undoableCommands.size() - 1; i >= 0; i--) {
            UICommand command = undoableCommands.get(i);
            if (command.undo()) {
                undoneCount++;
                TestLogManager.info("Undone command: " + command.getDescription());
            } else {
                TestLogManager.warning("Failed to undo command: " + command.getDescription());
            }
        }
        
        undoableCommands.clear();
        TestLogManager.info("Undone " + undoneCount + " commands");
        
        return undoneCount;
    }
    
    /**
     * Gets the command history.
     * @return Unmodifiable list of executed commands
     */
    public List<UICommand> getCommandHistory() {
        return Collections.unmodifiableList(commandHistory);
    }
    
    /**
     * Gets the undoable commands.
     * @return Unmodifiable list of undoable commands
     */
    public List<UICommand> getUndoableCommands() {
        return Collections.unmodifiableList(undoableCommands);
    }
    
    /**
     * Gets the total number of executed commands.
     * @return Execution count
     */
    public int getExecutionCount() {
        return executionCount.get();
    }
    
    /**
     * Gets execution statistics.
     * @return CommandExecutionStats object
     */
    public CommandExecutionStats getExecutionStats() {
        int totalCommands = commandHistory.size();
        int successfulCommands = 0;
        int failedCommands = 0;
        long totalExecutionTime = 0;
        
        for (UICommand command : commandHistory) {
            if (command.getResult() != null) {
                if (command.getResult().isPassed()) {
                    successfulCommands++;
                } else {
                    failedCommands++;
                }
                totalExecutionTime += command.getExecutionDuration();
            }
        }
        
        return new CommandExecutionStats(
                totalCommands,
                successfulCommands,
                failedCommands,
                undoableCommands.size(),
                totalExecutionTime
        );
    }
    
    /**
     * Clears the command history.
     */
    public void clearHistory() {
        commandHistory.clear();
        undoableCommands.clear();
        executionCount.set(0);
        TestLogManager.info("Command history cleared");
    }
    
    /**
     * Gets the last executed command.
     * @return Last command or null if no commands executed
     */
    public UICommand getLastCommand() {
        return commandHistory.isEmpty() ? null : commandHistory.get(commandHistory.size() - 1);
    }
    
    /**
     * Gets the last failed command.
     * @return Last failed command or null if no failed commands
     */
    public UICommand getLastFailedCommand() {
        for (int i = commandHistory.size() - 1; i >= 0; i--) {
            UICommand command = commandHistory.get(i);
            if (command.getResult() != null && command.getResult().isFailed()) {
                return command;
            }
        }
        return null;
    }
    
    private void addToHistory(UICommand command) {
        commandHistory.add(command);
        
        // Add to undoable commands if applicable
        if (command.isUndoable()) {
            undoableCommands.add(command);
        }
        
        // Maintain history size limit
        if (commandHistory.size() > maxHistorySize) {
            UICommand removedCommand = commandHistory.remove(0);
            undoableCommands.remove(removedCommand);
        }
    }
    
    /**
     * Data class for command execution statistics.
     */
    public static class CommandExecutionStats {
        private final int totalCommands;
        private final int successfulCommands;
        private final int failedCommands;
        private final int undoableCommands;
        private final long totalExecutionTime;
        
        public CommandExecutionStats(int totalCommands, int successfulCommands, int failedCommands,
                                   int undoableCommands, long totalExecutionTime) {
            this.totalCommands = totalCommands;
            this.successfulCommands = successfulCommands;
            this.failedCommands = failedCommands;
            this.undoableCommands = undoableCommands;
            this.totalExecutionTime = totalExecutionTime;
        }
        
        // Getters
        public int getTotalCommands() { return totalCommands; }
        public int getSuccessfulCommands() { return successfulCommands; }
        public int getFailedCommands() { return failedCommands; }
        public int getUndoableCommands() { return undoableCommands; }
        public long getTotalExecutionTime() { return totalExecutionTime; }
        
        public double getSuccessRate() {
            return totalCommands > 0 ? (double) successfulCommands / totalCommands * 100 : 0.0;
        }
        
        public double getAverageExecutionTime() {
            return totalCommands > 0 ? (double) totalExecutionTime / totalCommands : 0.0;
        }
        
        @Override
        public String toString() {
            return "CommandExecutionStats{" +
                    "totalCommands=" + totalCommands +
                    ", successfulCommands=" + successfulCommands +
                    ", failedCommands=" + failedCommands +
                    ", undoableCommands=" + undoableCommands +
                    ", successRate=" + String.format("%.2f", getSuccessRate()) + "%" +
                    ", averageExecutionTime=" + String.format("%.2f", getAverageExecutionTime()) + "ms" +
                    '}';
        }
    }
}
