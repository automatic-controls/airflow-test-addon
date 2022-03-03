package aces.webctrl.airflow;
import com.controlj.green.addonsupport.access.*;
import java.util.*;
import java.util.concurrent.atomic.*;
/**
 * Encapsulates a collection of airflow tests.
 */
public class TaskCollection {
  private volatile static AtomicInteger nextID = new AtomicInteger();
  private volatile int ID = nextID.getAndIncrement();
  private volatile Container<Boolean> cancel = new Container<Boolean>(false);
  private volatile ArrayList<Airflow> flows;
  private volatile long runtime;
  private volatile int index = -1;
  private volatile long timeout;
  private volatile int closeTolerance;
  private volatile int openTolerance;
  private volatile boolean completed = false;
  private volatile String operator;
  /**
   * Constructs a new {@code TaskCollection} with the given parameters.
   * @param arr contains location identifiers to search under where each identifier has been retrieved as a result of {@code Location.getPersistentLookupString(true)}.
   * @param runtime specifies the desired execution time for this collection of airflow tests.
   * @param timeout specifies the wait period between changing damper positions and measuring the airflow response.
   * @param closeTolerance is an integer between 1 and 100 (inclusive) that indicates a percentage for how near the airflow should be to 0 when the damper position is set to 0%.
   * @param openTolerator is an integer between 1 and 100 (inclusive) that indicates a percentage for how near the airflow should be to the ideal maximum when the damper position is set to 100%.
   * @param operator specifies the username of the operator which has initiated this collection of airflow tests.
   */
  public TaskCollection(final ArrayList<String> arr, long runtime, long timeout, int closeTolerance, int openTolerance, String operator) throws Throwable {
    if (closeTolerance<=0){
      closeTolerance = 1;
    }else if (closeTolerance>100){
      closeTolerance = 100;
    }
    if (openTolerance<=0){
      openTolerance = 1;
    }else if (openTolerance>100){
      openTolerance = 100;
    }
    if (timeout>3600000L){
      timeout = 3600000L;
    }else if (timeout<10000L){
      timeout = 10000L;
    }
    this.operator = operator;
    this.runtime = Math.max(runtime, System.currentTimeMillis());
    this.timeout = timeout;
    this.closeTolerance = closeTolerance;
    this.openTolerance = openTolerance;
    flows = new ArrayList<Airflow>(Math.max(arr.size(), 32));
    final Container<Throwable> err = new Container<Throwable>();
    DirectAccess.getDirectAccess().getRootSystemConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
      public void execute(SystemAccess sys){
        try{
          final Tree tree = sys.getTree(SystemTree.Geographic);
          for (String s:arr){
            try{
              recurse(tree.resolve(s));
            }catch(UnresolvableException t){}
          }
        }catch(Throwable t){
          err.x = t;
        }
      }
    });
    if (err.x!=null){
      throw err.x;
    }
  }
  /**
   * @return the username of the operator which initiated this collection of airflow tests.
   */
  public String getOperator(){
    return operator;
  }
  /**
   * @return the identification number for this collection of airflow tests.
   */
  public int getID(){
    return ID;
  }
  /**
   * @return a {@code Runnable} which executes the next airflow test, or {@code null} if there are no tests ready for execution.
   */
  public Runnable next(){
    if (completed || runtime>System.currentTimeMillis() || ++index>=flows.size()){
      return null;
    }else{
      final Airflow flow = flows.get(index);
      final boolean finish = index+1==flows.size();
      return new Runnable(){
        public void run(){
          flow.test(cancel, timeout, closeTolerance, openTolerance);
          if (finish){
            completed = true;
          }
        }
      };
    }
  }
  /**
   * @return the list of {@code Airflow} objects managed by this collection.
   */
  public ArrayList<Airflow> getFlows(){
    return flows;
  }
  /**
   * Cancel all outstanding airflow tests.
   */
  public void cancel(){
    cancel.x = true;
    completed = true;
  }
  /**
   * @return whether all outstanding airflow tests have been cancelled.
   */
  public boolean isCancelled(){
    return cancel.x;
  }
  /**
   * @return whether this collection of tests has been completed.
   */
  public boolean isCompleted(){
    return completed;
  }
  /**
   * @return an estimate of the start time for this collection of airflow tests.
   */
  public long getRuntime(){
    return runtime;
  }
  /**
   * Used for recursively collecting all airflow microblocks under a given location on the geographic tree.
   */
  private void recurse(Location loc) throws Throwable {
    LocationType type = loc.getType();
    if (type==LocationType.Equipment){
      for (Location l:loc.getChildren()){
        if (l.toNode().eval(".node-type").equals("270")){
          flows.add(new Airflow(l));
        }
      }
    }else if (type==LocationType.Area || type==LocationType.System){
      for (Location l:loc.getChildren()){
        recurse(l);
      }
    }
  }
}