package com.lh.servlet;

import com.lh.annotation.LHAutowrited;
import com.lh.annotation.LHController;
import com.lh.annotation.LHRequestMapping;
import com.lh.annotation.LHService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Created by Linhao on 2018/4/22.
 */
public class LHDispatherServlet extends HttpServlet {

    /**
     * 配置文件
     */
    private Properties contextConfig = new Properties();

    /**
     * IOC容器
     */
    private Map<String, Object> ioc = new HashMap<>();

    /**
     * RequestMapping处理
     */
    private Map<String,Method> handleMapping = new HashMap<>();

    /**
     * 存储所有要扫描的类
     */
    private List<String> classNames = new ArrayList<>();


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"");

        if(!handleMapping.containsKey(url)){
            resp.getWriter().write("404 - Not Found.");
            return;
        }

        Method method = handleMapping.get(url);

        //TODO:此处需要动态获取方法所属实例，获取方法的所有参数
        //method.invoke(instance,req,resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.解析配置（加载所有要实例化的类）
        doParseConfig(contextConfig.getProperty("packageName"));

        //3.实例化所有的要扫描的类
        doInstance();

        //4.依赖注入
        doAutowrited();

        //5.RequestMapping(MVC部分)
        initRequestMapping();

        System.out.println("LHSpring start success.");
    }

    /**
     * 加载配置
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream in = this.getClass().getClassLoader()
                .getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解析配置（加载所有要实例化的类）
     * @param packageName
     */
    private void doParseConfig(String packageName) {
        //加载文件夹下的所有的文件
        URL url = this.getClass().getClassLoader().getResource("/"
                + packageName.replaceAll("\\.", "/"));

        File file = new File(url.getFile());
        for (File f : file.listFiles()){
            if(f.isDirectory()){
                doParseConfig(packageName + "." + f.getName());
            }else{
                //存储类（带包名）
                classNames.add(packageName + "." + f.getName().replace(".class",""));
            }
        }
    }

    /**
     * 实例化所有的要扫描的类
     */
    private void doInstance() {
        if(classNames.isEmpty()){
            return;
        }

        for (String className : classNames){
            try {
                Class<?> clazz = Class.forName(className);

                if(clazz.isAnnotationPresent(LHController.class)){
                    //实例化带有controller的类,bean的id: 类名首字母小写
                    String beanName = lowerCaseFirstChar(clazz.getSimpleName());

                    ioc.put(beanName, clazz.newInstance());
                }else if(clazz.isAnnotationPresent(LHService.class)){
                    //实例化带有service的类(注意：service中可能存在service接口)

                    LHService service = clazz.getAnnotation(LHService.class);
                    String beanName = service.value();
                    if("".equals(beanName)){
                        //bean的id: 类名首字母小写
                        beanName = lowerCaseFirstChar(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);


                    Class<?>[] intetfaces = clazz.getInterfaces();
                    for (Class<?> i : intetfaces){
                        //存储接口的实例
                        ioc.put(i.getName(), instance);
                    }
                }else{
                    continue;
                }
            }catch (ClassNotFoundException e){
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 依赖注入
     */
    private void doAutowrited() {
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String, Object> entry : ioc.entrySet()){
            //对类的属性进行依赖注入判断
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields){
                //只解析的依赖注入
                if(!field.isAnnotationPresent(LHAutowrited.class)){
                    continue;
                }

                LHAutowrited autowrited = field.getAnnotation(LHAutowrited.class);

                String beanName = autowrited.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }

                //设置ptivate私有变量可以访问
                field.setAccessible(true);

                //将ioc容器中的实例赋值
                try {
                    //第一个参数：实参；第二个参数：实例
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * RequestMapping(MVC部分)
     */
    private void initRequestMapping() {
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String, Object> entry : ioc.entrySet()){

            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(LHController.class)){
                continue;
            }

            String baseUrl = "";
            //先取class上LHRequestMapping注解
            if(clazz.isAnnotationPresent(LHRequestMapping.class)){
                LHRequestMapping requestMapping = clazz.getAnnotation(LHRequestMapping.class);
                baseUrl = requestMapping.value();
            }
            baseUrl = baseUrl.startsWith("/") ? baseUrl : ("/" + baseUrl);

            //对类的方法进行RequestMapping判断
            Method[] methods = clazz.getMethods();
            for (Method method : methods){
                //去除不包含LHRequestMapping注解的方法
                if(!method.isAnnotationPresent(LHRequestMapping.class)){
                    continue;
                }

                LHRequestMapping requestMapping = method.getAnnotation(LHRequestMapping.class);

                String url = (baseUrl + requestMapping.value().replaceAll("/+","/"));
                handleMapping.put(url, method);

                System.out.println("Mapping Url:" + url);
            }

        }
    }

    /**
     * 首字符
     * @param s
     * @return
     */
    private String lowerCaseFirstChar(String s){
        char[] chars = s.toCharArray();
        if(chars[0] >= 65 && chars[0] <= 90){
            chars[0] += 32;
        }

        return String.valueOf(chars);
    }
}
