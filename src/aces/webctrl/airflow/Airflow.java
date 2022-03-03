package aces.webctrl.airflow;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.access.node.*;
/**
 * Represents testing of a single airflow microblock.
 */
public class Airflow {
  private volatile Throwable error = null;
  private volatile Node damperLockFlag = null;
  private volatile Node damperLockValue = null;
  private volatile Node maxFlow = null;
  private volatile Node currentFlow = null;
  private volatile long startTime = -1;
  private volatile long endTime = -1;
  private volatile int minAirflow = -1;
  private volatile int maxAirflow = -1;
  private volatile int maxFlowValue = -1;
  private volatile String initialLockFlag = null;
  private volatile String initialLockValue = null;
  private volatile boolean testSuccess = false;
  private volatile boolean closeSuccess = false;
  private volatile boolean openSuccess = false;
  private volatile boolean attempted = false;
  /**
   * @return the error associated with this airflow, or {@code null} if no error has occurred.
   */
  public Throwable getError(){
    return error;
  }
  /**
   * Ensures only the first error is recorded.
   */
  private void setError(Throwable t){
    if (error==null){
      error = t;
    }
  }
  /**
   * @return whether this test has been attempted.
   */
  public boolean testAttempted(){
    return attempted;
  }
  /**
   * @return whether the test completed without encountering any errors.
   */
  public boolean testSuccess(){
    return testSuccess;
  }
  /**
   * @return whether the damper close test completed successfully.
   */
  public boolean closeTestSuccess(){
    return closeSuccess;
  }
  /**
   * @return whether the damper open test completed successfully.
   */
  public boolean openTestSuccess(){
    return openSuccess;
  }
  /**
   * @return the airflow measured when the damper locked to 0%; or {@code -1} if no test has been successful.
   */
  public int getMinAirflow(){
    return minAirflow;
  }
  /**
   * @return the airflow measured when the damper locked to 100%; or {@code -1} if no test has been successful.
   */
  public int getMaxAirflow(){
    return maxAirflow;
  }
  /**
   * @return the start time of this airflow test; or {@code -1} if no test has been performed.
   */
  public long getStartTime(){
    return startTime;
  }
  /**
   * @return the end time of this airflow test; or {@code -1} if no test has been performed.
   */
  public long getEndTime(){
    return endTime;
  }
  /**
   * @return the ideal maximum airflow; or {@code -1} if no test has been successful.
   */
  public int getIdealMaxAirflow(){
    return maxFlowValue;
  }
  /**
   * Maps relevant airflow microblock nodes.
   * May be used inside a readAction without field-access.
   * @param root is any {@code Location} with a node-type of 270.
   */
  public Airflow(Location root){
    if (root==null){
      setError(new NullPointerException("Airflow root location was null."));
      return;
    }
    for (Location l:root.getChildren()){
      if (l.getReferenceName().equals("flow_tab")){
        int x = 0;
        for (Node n:l.toNode().getChildren()){
          switch (n.getReferenceName()){
            case "actual_flow":{
              currentFlow = n;
              ++x;
              break;
            }
            case "damper_lock":{
              damperLockValue = n;
              ++x;
              break;
            }
            case "max_cool":{
              maxFlow = n;
              ++x;
              break;
            }
            case "lock_flags":{
              for (Node m:n.getChildren()){
                if (m.getReferenceName().equals("damper")){
                  damperLockFlag = m;
                  break;
                }
              }
              ++x;
              break;
            }
          }
          if (x==4){
            break;
          }
        }
        break;
      }
    }
    if (damperLockFlag==null || damperLockValue==null || maxFlow==null || currentFlow==null){
      setError(new NullPointerException("Failed to map airflow nodes."));
    }
  }
  /**
   * Initiate damper tests for this airflow.
   * You should expect this method to block for at least {@code timeout*2} milliseconds.
   * You do not need to be in a WriteAction or ReadAction to invoke this method.
   * @param cancel if set to {@code true}, this method will attempt to pre-maturely terminate test.
   * @param timeout specifies how long to wait for a response after changing the damper position.
   * @param closeTolerance when the damper position is set to 0%, verify that airflow is less than this percentage of the idealMax
   * @param openTolerance when the damper position is set to 100%, verify that airflow is within this percentage of idealMax
   */
  public void test(final Container<Boolean> cancel, final long timeout, final int closeTolerance, final int openTolerance){
    if (error!=null && !cancel.x && !attempted){
      attempted = true;
      startTime = System.currentTimeMillis();
      try{
        final SystemConnection con = DirectAccess.getDirectAccess().getRootSystemConnection();
        try{
          con.runWriteAction(FieldAccessFactory.newFieldAccess(), "Initiating closed damper test.", new WriteAction(){
            public void execute(WritableSystemAccess sys){
              if (cancel.x || Initializer.isStopped()){ return; }
              try{
                maxFlowValue = parse(maxFlow.getValue());
                initialLockFlag = damperLockFlag.getValue();
                initialLockValue = damperLockValue.getValue();
                damperLockValue.setValue("0");
                damperLockFlag.setValue("true");
              }catch(Throwable t){
                setError(t);
              }
            }
          });
          if (error!=null || cancel.x || Initializer.isStopped()){ return; }
          Utility.waitUntil(System.currentTimeMillis()+timeout);
          if (cancel.x || Initializer.isStopped()){ return; }
          con.runWriteAction(FieldAccessFactory.newFieldAccess(), "Initiating open damper test.", new WriteAction(){
            public void execute(WritableSystemAccess sys){
              if (cancel.x || Initializer.isStopped()){ return; }
              try{
                minAirflow = parse(currentFlow.getValue());
                damperLockValue.setValue("100");
              }catch(Throwable t){
                setError(t);
              }
            }
          });
          if (error!=null || cancel.x || Initializer.isStopped()){ return; }
          closeSuccess = minAirflow*100<=maxFlowValue*closeTolerance;
          Utility.waitUntil(System.currentTimeMillis()+timeout);
          if (cancel.x || Initializer.isStopped()){ return; }
          con.runWriteAction(FieldAccessFactory.newFieldAccess(), "Finalizing tests.", new WriteAction(){
            public void execute(WritableSystemAccess sys){
              if (cancel.x || Initializer.isStopped()){ return; }
              try{
                maxAirflow = parse(currentFlow.getValue());
                damperLockFlag.setValue(initialLockFlag);
                initialLockFlag = null;
                damperLockValue.setValue(initialLockValue);
                initialLockValue = null;
              }catch(Throwable t){
                setError(t);
              }
            }
          });
          if (error!=null || cancel.x || Initializer.isStopped()){ return; }
          openSuccess = maxFlowValue!=-1 && maxAirflow!=-1 && Math.abs(maxAirflow-maxFlowValue)*100<=maxFlowValue*openTolerance;
          testSuccess = true;
        }catch(Throwable t){
          setError(t);
        }finally{
          if (initialLockValue!=null || initialLockFlag!=null){
            con.runWriteAction(FieldAccessFactory.newFieldAccess(), "Reverting to previous damper configuration.", new WriteAction(){
              public void execute(WritableSystemAccess sys){
                if (initialLockValue!=null){
                  try{
                    damperLockValue.setValue(initialLockValue);
                  }catch(Throwable t){
                    setError(t);
                  }
                }
                if (initialLockFlag!=null){
                  try{
                    damperLockFlag.setValue(initialLockFlag);
                  }catch(Throwable t){
                    setError(t);
                  }
                }
              }
            });
          }
        }
      }catch(Throwable t){
        setError(t);
      }finally{
        damperLockFlag = null;
        damperLockValue = null;
        maxFlow = null;
        currentFlow = null;
        endTime = System.currentTimeMillis();
      }
    }else{
      attempted = true;
      startTime = System.currentTimeMillis();
      endTime = startTime;
      damperLockFlag = null;
      damperLockValue = null;
      maxFlow = null;
      currentFlow = null;
    }
    if (cancel.x){
      setError(new java.util.concurrent.CancellationException());
    }
  }
  /**
   * @return an integral representation of the given string.
   */
  private static int parse(String s){
    return (int)Math.round(Double.parseDouble(s));
  }
}