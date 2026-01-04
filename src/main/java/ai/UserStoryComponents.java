package ai;

/**
 * Represents the components of a parsed user story
 */
public class UserStoryComponents {
    private String fullStory;
    private String actor;
    private String action;
    private String goal;
    
    public String getFullStory() {
        return fullStory;
    }
    
    public void setFullStory(String fullStory) {
        this.fullStory = fullStory;
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


