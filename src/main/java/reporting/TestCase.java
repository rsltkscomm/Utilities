package reporting;


public interface TestCase
{
	String getModuleName();
    String getExecutionId();
    String getTestCaseId();
    String getDescription();
    String getAction();
    String getExpectedResult();
    String getActualResult();
    int getTotalSteps();

}
