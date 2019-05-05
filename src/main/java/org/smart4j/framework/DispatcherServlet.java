package org.smart4j.framework;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.smart4j.framework.bean.Data;import org.smart4j.framework.bean.Handler;import org.smart4j.framework.bean.Param;import org.smart4j.framework.bean.View;import org.smart4j.framework.helper.BeanHelper;import org.smart4j.framework.helper.ConfigHelper;import org.smart4j.framework.helper.ControllerHelper;import org.smart4j.framework.util.*;import javax.servlet.ServletConfig;import javax.servlet.ServletContext;import javax.servlet.ServletException;import javax.servlet.ServletRegistration;import javax.servlet.annotation.WebServlet;import javax.servlet.http.HttpServlet;import javax.servlet.http.HttpServletRequest;import javax.servlet.http.HttpServletResponse;import java.io.IOException;import java.io.PrintWriter;import java.lang.reflect.Method;import java.util.Enumeration;import java.util.HashMap;import java.util.Map;/** * @ClassName DispatcherServlet * @Description TODO * @Author mac * @Date 2019-05-04 07:21 PM * Version 1.0 **/@WebServlet(urlPatterns = "/*", loadOnStartup = 0)public class DispatcherServlet extends HttpServlet {    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherServlet.class);    @Override    public void init(ServletConfig servletConfig) throws ServletException {        LOGGER.info("DispatcherServlet.init execute start...");        HelperLoader.init();//装载ClassHelper, BeanHelper, IocHelper, ControllerHelper        ServletContext servletContext = servletConfig.getServletContext();        ServletRegistration jspServlet = servletContext.getServletRegistration("jsp");        jspServlet.addMapping(ConfigHelper.getAppJspPath() + "*");        ServletRegistration defaultServlet = servletContext.getServletRegistration("default");        defaultServlet.addMapping(ConfigHelper.getAppAssetPath() + "*");        LOGGER.info("DispatcherServlet.init execute finish");    }    @Override    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {        LOGGER.info("DispatcherServlet.service execute start...");        String requestMethod = req.getMethod().toLowerCase();        String requestPath = req.getPathInfo();        LOGGER.info("DispatcherServlet.service execute requestMethod=" + requestMethod + ",requestPath" + requestPath);        Handler handler = ControllerHelper.getHandler(requestMethod, requestPath);        if(handler != null){            Class<?> controllerClass = handler.getControllerClass();            Object controllerBean = BeanHelper.getBean(controllerClass);            Map<String, Object> paramMap = new HashMap<String, Object>();            Enumeration<String> paramNames = req.getParameterNames();            System.out.println("paramNames=" + paramNames.toString());            while(((Enumeration) paramNames).hasMoreElements()){                String paramName = paramNames.nextElement();                String paramValue = req.getParameter(paramName);                paramMap.put(paramName, paramValue);            }            String body = CodecUtil.decodeURL(StreamUtil.getString(req.getInputStream()));            System.out.println("body=" + body);            if(StringUtil.isNotEmpty(body)){                String[] params = StringUtil.splitString(body, "&");                if(ArrayUtil.isNotEmpty(params)){                    for(String param : params){                        String[] array = StringUtil.splitString(param, "=");                        if(ArrayUtil.isNotEmpty(array) && array.length == 2){                            String paramName = array[0];                            String paramValue = array[1];                            paramMap.put(paramName, paramValue);                        }                    }                }            }            Param param = new Param(paramMap);            Method actionMethod = handler.getActionMethod();            Object result = ReflectionUtil.invokeMethod(controllerBean, actionMethod, param);            if(result instanceof View){                System.out.println("result is view");                View view = (View) result;                String path = view.getPath();                if(StringUtil.isNotEmpty(path)){                    if(path.startsWith("/")){                        System.out.println("req.getContextPath() + path:" + req.getContextPath() + path);                        resp.sendRedirect(req.getContextPath() + path);                    }else{                        Map<String, Object> model = view.getModel();                        for(Map.Entry<String, Object> entry : model.entrySet()){                            req.setAttribute(entry.getKey(), entry.getValue());                        }                        System.out.println("ConfigHelper.getAppJspPath() + path:" + ConfigHelper.getAppJspPath() + path);                        req.getRequestDispatcher(ConfigHelper.getAppJspPath() + path).forward(req, resp);                    }                }else if(result instanceof Data){                    System.out.println("result is Data");                    Data data = (Data)result;                    Object model = data.getModel();                    if(model != null){                        resp.setContentType("application/json");                        resp.setCharacterEncoding("utf-8");                        PrintWriter write = resp.getWriter();                        String json = JsonUtil.toJson(model);                        write.write(json);                        write.flush();                        write.close();                    }                }            }        }        LOGGER.info("DispatcherServlet.service execute finish");    }}