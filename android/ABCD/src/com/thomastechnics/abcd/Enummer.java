package com.thomastechnics.abcd;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import android.util.Pair;

public class Enummer {
  private static final String NEWLINE = " \n";

  public static void append(StringBuilder sb, Object object) {
    Method methods[] = object.getClass().getMethods();
    for (Method method : methods) {
      if (method.getName().startsWith("get")) {
        if (method.getParameterTypes().length == 0) {

          Object getValue;
          String getValueString;

          Pair<Object, String> getPair = invokeGet(object, method);
          getValue = getPair.first;
          getValueString = getPair.second;

          append(sb, method.getName(), getValueString);

          if (getValue instanceof Map) {
            sb.append(NEWLINE);
            appendMap(method.getName(), sb, (Map<?, ?>) getValue);
          } else if (getValue instanceof Iterable) {
            sb.append(NEWLINE);
            appendIterable(method.getName(), sb, (Iterable<?>) getValue);
          } else if ((getValue instanceof Integer) && (method.getName().endsWith("Count"))) {

            String countedName = method.getName().substring(0,
                method.getName().length() - "Count".length());
            Method countedMethod;
            try {
              countedMethod = object.getClass().getMethod(countedName, int.class);
            } catch (NoSuchMethodException e) {
              countedMethod = null;
            }

            sb.append(NEWLINE);
            appendCounted(sb, object, getValue, countedMethod);
          }
        }
      }
    }

    sb.append(NEWLINE);
  }

  public static void appendCounted(StringBuilder sb, Object object, Object count,
      Method countedMethod) {
    int listCount;
    if (count instanceof Integer) {
      listCount = (Integer) count;
    } else {
      listCount = -1;
    }

    for (int listIndex = 0; listIndex < listCount; listIndex++) {
      Pair<Object, String> getPair = invokeGet(object, countedMethod, listIndex);
      if (getPair != null && getPair.first != null) {
        sb.append(countedMethod.getName() + "[" + listIndex + "]" + NEWLINE);
        append(sb, getPair.first);
      }
    }
  }

  private static Pair<Object, String> invokeGet(Object object, Method method, Object... args) {
    Object value = null;
    String valueString;

    try {
      value = method.invoke(object, args);
      valueString = "" + value;
    } catch (IllegalArgumentException e) {
      valueString = e.getMessage();
    } catch (IllegalAccessException e) {
      valueString = e.getMessage();
    } catch (InvocationTargetException e) {
      valueString = e.getMessage();
    }

    Pair<Object, String> getPair = new Pair<Object, String>(value, valueString);
    return getPair;
  }

  public static void appendIterable(String name, StringBuilder sb, Iterable<?> iterable) {
    Iterator<?> iter = iterable.iterator();
    int count = 0;
    while (iter.hasNext()) {
      sb.append(name + "<" + count + ">" + NEWLINE);
      append(sb, iter.next());
      count++;
    }
  }

  public static void appendMap(String name, StringBuilder sb, Map<?, ?> map) {
    Iterator<?> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      Object key = iter.next();
      sb.append(name + "[" + key + "]" + NEWLINE);
      append(sb, map.get(key));
    }
  }

  public static void append(StringBuilder sb, String name, Object value) {
    sb.append(name);
    sb.append("(");
    sb.append(value);
    sb.append(")" + NEWLINE);
  }

  static String printBytes(byte[] bytes) {
    String bPrint = "";
    String aPrint = "";
    for (byte byteValue : bytes) {
      bPrint += byteValue + " ";
      aPrint += "'" + (char) byteValue + "' ";
    }
//    String print = "(" + bytes.length + "){ " + bPrint + "} a{ " + aPrint + "}";
    String print = "(" + bytes.length + "){ " + bPrint + "}";
    return print;
  }
}
