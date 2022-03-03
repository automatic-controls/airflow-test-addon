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
public class Initializer implements ServletContextListener {
  private volatile static boolean running = true;
  private volatile static boolean stop = false;
  private volatile static Thread th = null;
  /** Specifies the  */
  public volatile static String prefix = null;
  /** The logging utility to use for this add-on */
  public volatile static FileLogger logger = null;
  /** Stores a collection of {@code TaskCollection} objects indexed by {@code ID} */
  private final static TreeMap<Integer,TaskCollection> map = new TreeMap<Integer,TaskCollection>();
  /** Controls access to {@code map} */
  private final static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
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