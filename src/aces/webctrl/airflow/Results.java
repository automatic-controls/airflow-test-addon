/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.airflow;
import com.controlj.green.addonsupport.access.*;
import com.controlj.green.addonsupport.web.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
public class Results extends HttpServlet {
  private volatile static String html = null;
  @Override public void init() throws ServletException {
    try{
      html = Utility.loadResourceAsString("aces/webctrl/airflow/Results.html").replaceAll(
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
      final String id = req.getParameter("ID");
      if (id==null){
        res.sendError(400, "Please specify a valid ID.");
      }else{
        try{
          final int ID = Integer.parseInt(id);
          final TaskCollection x = Initializer.get(ID);
          if (x==null){
            res.sendError(404, "Cannot locate the specified airflow test collection.");
          }else{
            final StringBuilder sb = new StringBuilder();
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
            DirectAccess.getDirectAccess().getRootSystemConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
              public void execute(SystemAccess sys){
                Tree tree = sys.getTree(SystemTree.Geographic);
                String equipmentName,equipmentLink,startTime,endTime,error;
                long start,end,min,max,ideal;
                int close,open,w;
                Throwable t;
                for (Airflow a:x.getFlows()){
                  equipmentName = "NULL";
                  equipmentLink = "#";
                  try{
                    equipmentName = tree.resolve(a.getEquipment()).getRelativeDisplayPath(null);
                    equipmentLink = Link.createLink(UITree.GEO, tree.resolve(a.getRoot())).getURL(req);
                  }catch(Throwable tt){}
                  start = a.getStartTime();
                  end = a.getEndTime();
                  startTime = start==-1?"NULL":Initializer.format.format(java.time.Instant.ofEpochMilli(start));
                  endTime = end==-1?"NULL":Initializer.format.format(java.time.Instant.ofEpochMilli(end));
                  min = a.getMinAirflow();
                  max = a.getMaxAirflow();
                  ideal = a.getIdealMaxAirflow();
                  t = a.getError();
                  error = t==null?"None":(t.getClass().getSimpleName()+" - "+t.getMessage());
                  w = a.testSuccess()?1:(end==-1?-1:2);
                  close = a.closeTestSuccess()?0:w;
                  open = a.openTestSuccess()?0:w;
                  sb.append("addTest(\"");
                  sb.append(Utility.escapeJS(equipmentName)).append("\",\"");
                  sb.append(Utility.escapeJS(equipmentLink)).append("\",");
                  sb.append(close).append(',').append(open).append(",\"");
                  sb.append(Utility.escapeJS(startTime)).append("\",\"");
                  sb.append(Utility.escapeJS(endTime)).append("\",\"");
                  sb.append(min).append("\",\"");
                  sb.append(max).append("\",\"");
                  sb.append(ideal).append("\",\"");
                  sb.append(error).append("\");\n");
                }
              }
            });
            res.setContentType("text/html");
            res.getWriter().print(html.replace("//__INIT_SCRIPT__", sb.toString()));
          }
        }catch(NumberFormatException e){
          res.sendError(400, "Please specify a valid ID.");
        }
      }
    }catch(Throwable t){
      Initializer.logger.println(t);
      res.sendError(500);
    }
  }
}