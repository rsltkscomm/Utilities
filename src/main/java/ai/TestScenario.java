package ai;

/**
 * Represents a test scenario generated from a user story
 */
public class TestScenario {
    private String name;
    private String description;
    private String type; // positive, negative, edge
    private String userStory;
    private String actor;
    private String action;
    private String goal;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getUserStory() {
        return userStory;
    }
    
    public void setUserStory(String userStory) {
        this.userStory = userStory;
    }
    
    public String getActor() {
        return actor;
    }
    
    public void setActor(String actor) {
        this.actor = actor;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getGoal() {
        return goal;
    }
    
    public void setGoal(String goal) {
        this.goal = goal;
    }
}


