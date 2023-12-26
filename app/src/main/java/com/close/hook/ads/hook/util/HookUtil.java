package com.close.hook.ads.hook.util;

import java.lang.reflect.Method;
import androidx.collection.ArrayMap;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;

public class HookUtil {

    public static class HookInfo {
        public String className;
        public String[] methodNames;
        public Object returnValue;

        public HookInfo(String className, String[] methodNames, Object returnValue) {
            this.className = className;
            this.methodNames = methodNames;
            this.returnValue = returnValue;
        }

        public HookInfo(String className, String methodName, Object returnValue) {
            this(className, new String[]{methodName}, returnValue);
        }
    }

    public static void hookMethod(ClassLoader classLoader, String className, String methodName, Object returnValue) {
        hookMethods(classLoader, className, new String[]{methodName}, returnValue);
    }

    public static void hookMethods(ClassLoader classLoader, String className, String[] methodNames, Object returnValue) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            ArrayMap<String, Boolean> methodNameMap = new ArrayMap<>();
            for (String methodName : methodNames) {
                methodNameMap.put(methodName, true);
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (methodNameMap.containsKey(method.getName())) {
                    hookMethodWithReplacement(method, returnValue);
                }
            }
        } catch (ClassNotFoundException e) {
        }
    }

    private static void hookMethodWithReplacement(Method method, Object returnValue) {
        XposedBridge.hookMethod(method, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return returnValue;
            }
        });
    }

    public static String getFormattedStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTrace) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }

}
