package patterns.command;

/**
 * Command interface for the Command pattern.
 * This defines the contract for all command implementations.
 */
public interface Command {
    
    /**
     * Executes the command.
     * @return true if successful, false otherwise
     */
    boolean execute();
    
    /**
     * Undoes the command.
     * @return true if successful, false otherwise
     */
    boolean undo();
    
    /**
     * Gets a description of the command.
     * @return Command description
     */
    String getDescription();
}