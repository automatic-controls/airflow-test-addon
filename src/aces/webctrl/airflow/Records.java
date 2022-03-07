/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.airflow;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
public class Records extends HttpServlet {
  private volatile static String html = null;
  @Override public void init() throws ServletException {
    try{
      html = Utility.loadResourceAsString("aces/webctrl/airflow/Records.html").replaceAll(
        "(?m)^[ \\t]++",
        ""
      ).replace(
        "__PREFIX__",
        Initializer.prefix
      );
    }catch(Throwable e){
      if (e instanceof ServletException){
        throw (ServletException)e;
      }else{
        throw new ServletException(e);
      }
    }
  }
  @Override public void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
    doPost(req, res);
  }
  @Override public void doPost(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
    try{
      req.setCharacterEncoding("UTF-8");
      res.setCharacterEncoding("UTF-8");
      final String type = req.getParameter("type");
      if (type==null){
        final StringBuilder sb = new StringBuilder();
        Initializer.forEach(new java.util.function.Consumer<TaskCollection>(){
          public void accept(final TaskCollection x){
            final boolean complete = x.isCompleted();
            String status;
            if (x.isCancelled()){
              status = "Cancelled";
            }else if (!x.isStarted()){
              status = "Not Started";
            }else if (complete){
              status = "Complete";
            }else{
              status = "Working";
            }
            sb.append("addRow(\"").append(x.getID()).append("\",\"");
            sb.append(Utility.escapeJS(status)).append("\",\"");
            sb.append(x.getSuccessRate()).append("%\",\"");
            sb.append(Utility.escapeJS(Initializer.format.format(java.time.Instant.ofEpochMilli(x.getRuntime())))).append("\",\"");
            sb.append(Utility.escapeJS(x.getOperator())).append("\",\"");
            sb.append(x.getCloseTolerance()).append("%\",\"");
            sb.append(x.getOpenTolerance()).append("%\",\"");
            sb.append(x.getTimeout()).append("ms\",");
            sb.append(complete).append(");\n");
          }
        });
        res.setContentType("text/html");
        res.getWriter().print(html.replace("//__INIT_SCRIPT__", sb.toString()));
      }else{
        switch (type){
          case "cancel":{
            final String id = req.getParameter("ID");
            if (id==null){
              res.setStatus(400);
            }else{
              try{
                final int ID = Integer.parseInt(id);
                final TaskCollection x = Initializer.get(ID);
                if (x==null){
                  res.setStatus(404);
                }else if (x.isCancelled() || x.isCompleted()){
                  Initializer.remove(ID);
                }else{
                  x.cancel();
                }
              }catch(NumberFormatException e){
                res.setStatus(400);
              }
            }
            break;
          }
          default:{
            res.sendError(400, "Invalid type request parameter.");
          }
        }
      }
    }catch(Throwable t){
      Initializer.logger.println(t);
      res.sendError(500);
    }
  }
}