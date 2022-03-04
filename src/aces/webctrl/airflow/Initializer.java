/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.airflow;
import com.controlj.green.addonsupport.*;
import javax.servlet.*;
import java.util.*;
import java.util.concurrent.locks.*;
import java.time.format.*;
import java.time.*;
public class Initializer implements ServletContextListener {
  public final static DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
  private volatile static boolean running = true;
  private volatile static boolean stop = false;
  private volatile static Thread th = null;
  /** Used for creating HREF links to servlets of this add-on */
  public volatile static String prefix = null;
  /** Used for recording log messages */
  public volatile static FileLogger logger = null;
  /** Stores a collection of {@code TaskCollection} objects indexed by {@code ID} */
  private final static TreeMap<Integer,TaskCollection> map = new TreeMap<Integer,TaskCollection>();
  /** Controls access to {@code map} */
  private final static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  /**
   * Interrupt the asynchronous processing thread.
   */
  public static void interrupt(){
    th.interrupt();
  }
  /**
   * Appends the given {@code TaskCollection}.
   */
  public static void add(TaskCollection tasks){
    lock.writeLock().lock();
    try{
      map.put(tasks.getID(), tasks);
    }finally{
      lock.writeLock().unlock();
    }
    th.interrupt();
  }
  /**
   * Removes the {@code TaskCollection} with the given {@code ID}.
   */
  public static void remove(Integer ID){
    lock.writeLock().lock();
    try{
      map.remove(ID);
    }finally{
      lock.writeLock().unlock();
    }
  }
  /**
   * @return the {@code TaskCollection} with the given {@code ID}.
   */
  public static TaskCollection get(Integer ID){
    lock.readLock().lock();
    try{
      return map.get(ID);
    }finally{
      lock.readLock().unlock();
    }
  }
  /**
   * Invokes the given {@code Consumer} for every {@code TaskCollection} in the mapping using a read lock.
   */
  public static void forEach(java.util.function.Consumer<TaskCollection> x){
    lock.readLock().lock();
    try{
      for (TaskCollection col:map.values()){
        x.accept(col);
      }
    }finally{
      lock.readLock().unlock();
    }
  }
  /**
   * Starts a thread which asynchronously monitors and executes airflow tests.
   */
  @Override public void contextInitialized(ServletContextEvent sce){
    AddOnInfo info = AddOnInfo.getAddOnInfo();
    prefix = '/'+info.getName()+'/';
    logger = info.getDateStampLogger();
    th = new Thread(){
      public void run(){
        while (!stop){
          Runnable r = null;
          lock.readLock().lock();
          try{
            for (TaskCollection tasks:map.values()){
              r = tasks.next();
              if (r!=null){
                break;
              }
            }
          }finally{
            lock.readLock().unlock();
          }
          if (r==null){
            try{
              Thread.sleep(2000);
            }catch(InterruptedException e){}
          }else{
            r.run();
          }
        }
        running = false;
        synchronized (Initializer.class){
          Initializer.class.notifyAll();
        }
      }
    };
    th.start();
  }
  /**
   * Handles termination of the asynchronous processing thread whenever the application is killed.
   */
  @Override public void contextDestroyed(ServletContextEvent sce){
    stop = true;
    th.interrupt();
    synchronized (Initializer.class){
      while (running){
        try{
          Initializer.class.wait(1000);
        }catch(Throwable t){}
      }
    }
  }
  /**
   * All threads should attempt to halt their execution when this method returns {@code true}.
   */
  public static boolean isStopped(){
    return stop;
  }
}