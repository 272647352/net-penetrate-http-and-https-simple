package com.kele.penetrate.utils;

import com.kele.penetrate.factory.Register;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("unused")
@Slf4j
public class Events<T>
{
    private final ConcurrentLinkedQueue<Func<T, Boolean>> events = new ConcurrentLinkedQueue<>();
    private static List<Class<?>> classesByPackageName;

    public Events(String name, Class<T> t, String registerPath)
    {
        classesByPackageName = ScanPackage.getClassesByPackageName(registerPath);
        for (Class<?> clazz : classesByPackageName)
        {
            boolean isSimilar = false;
            boolean isParameterType = false;
            boolean isReturn = false;
            Register register = clazz.getAnnotation(Register.class);
            if (register == null)
            {
                continue;
            }

            for (Class<?> interfaceClazz : clazz.getInterfaces())
            {
                Type[] genericInterfaces = clazz.getGenericInterfaces();
                if (genericInterfaces[0] instanceof ParameterizedType)
                {
                    ParameterizedType parameterizedType = (ParameterizedType) genericInterfaces[0];
                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments[0] == t)
                    {
                        isParameterType = true;
                    }
                    if (actualTypeArguments[1] == Boolean.class)
                    {
                        isReturn = true;
                    }
                }
                if (interfaceClazz == Func.class)
                {
                    isSimilar = true;
                }
            }

            if (isSimilar)
            {
                if (!isParameterType)
                {
                    log.error(name + "  " + clazz.getName() + "自动注册失败:参数类型错误");
                    return;
                }
                if (!isReturn)
                {
                    log.error(name + "  " + clazz.getName() + "自动注册失败:return类型错误");
                    return;
                }

                try
                {
                    Func<T, Boolean> func = (Func<T, Boolean>) clazz.newInstance();
                    events.add(func);
                    log.info(name + "  " + clazz.getName() + "自动注册成功");
                }
                catch (Exception ex)
                {
                    log.error(name + "  " + clazz.getName() + "自动注册失败", ex);
                }
            }
        }
    }

    public synchronized void add(Func<T, Boolean> func)
    {
        events.add(func);
    }

    public synchronized void remove(Func<T, Boolean> func)
    {
        events.remove(func);
    }

    public synchronized void notice(T t)
    {
        for (Func<T, Boolean> func : events)
        {
            if (func.func(t))
            {
                break;
            }
        }
    }
}
