/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.airflow;
import com.controlj.green.addonsupport.access.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
public class MainServlet extends HttpServlet {
  private volatile static String html = null;
  @Override public void init() throws ServletException {
    try{
      html = Utility.loadResourceAsString("aces/webctrl/airflow/MainPage.html").replaceAll(
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
        final String[] arr = DirectAccess.getDirectAccess().getRootSystemConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadActionResult<String[]>(){
          public String[] execute(SystemAccess sys){
            final Location geo = sys.getGeoRoot();
            return new String[]{geo.getDisplayName(), geo.getPersistentLookupString(true)};
          }
        });
        res.setContentType("text/html");
        res.getWriter().print(html.replace("__GEO_NAME__", Utility.escapeJS(arr[0])).replace("__GEO_PATH__", Utility.escapeJS(arr[1])));
      }else{
        switch (type){
          case "expand":{
            final String ID = req.getParameter("ID");
            if (ID==null){
              res.setStatus(400);
            }else{
              final StringBuilder sb = new StringBuilder();
              try{
                DirectAccess.getDirectAccess().getRootSystemConnection().runReadAction(FieldAccessFactory.newDisabledFieldAccess(), new ReadAction(){
                  public void execute(SystemAccess sys) throws Exception {
                    final Location loc = sys.getTree(SystemTree.Geographic).resolve(ID);
                    LocationType t = loc.getType();
                    if (t==LocationType.Area || t==LocationType.System || t==LocationType.Directory){
                      for (final Location x:loc.getChildren()){
                        t = x.getType();
                        if (t==LocationType.Area || t==LocationType.Equipment || t==LocationType.Directory || t==LocationType.System){
                          sb.append(Utility.encodeAJAX(x.getDisplayName())).append(';');
                          sb.append(Utility.encodeAJAX(x.getPersistentLookupString(true))).append(';');
                        }
                      }
                    }
                  }
                });
              }catch(Throwable t){}
              res.setContentType("text/plain");
              res.getWriter().print(sb.toString());
            }
            break;
          }
          case "create":{
            final String data = req.getParameter("data");
            final String delay = req.getParameter("delay");
            final String timeout = req.getParameter("timeout");
            final String closeTolerance = req.getParameter("close");
            final String openTolerance = req.getParameter("open");
            if (data==null || delay==null || timeout==null || closeTolerance==null || openTolerance==null){
              res.setStatus(400);
            }else{
              try{
                final TaskCollection x = new TaskCollection(Utility.decodeAJAX(data), System.currentTimeMillis()+Long.parseLong(delay), Long.parseLong(timeout), Integer.parseInt(closeTolerance), Integer.parseInt(openTolerance), DirectAccess.getDirectAccess().getUserSystemConnection(req).getOperator().getLoginName());
                Initializer.add(x);
                res.setContentType("text/plain");
                res.getWriter().print(String.valueOf(x.getID()));
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