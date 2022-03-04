/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.airflow;
import java.io.*;
import java.util.*;
public class Utility {
  /**
   * Freezes the current thread until the given time has been reached as compared with {@code System.currentTimeMillis()}.
   */
  public static void waitUntil(long time) throws InterruptedException {
    long x;
    while (true){
      x = time-System.currentTimeMillis();
      if (x<=0){
        break;
      }else{
        Thread.sleep(x);
      }
    }
  }
  /**
   * Loads all bytes from the given resource and convert to a {@code UTF-8} string.
   * @return the {@code UTF-8} string representing the given resource.
   */
  public static String loadResourceAsString(String name) throws Throwable {
    java.util.ArrayList<byte[]> list = new java.util.ArrayList<byte[]>();
    int len = 0;
    byte[] buf;
    int read;
    try(
      InputStream s = Utility.class.getClassLoader().getResourceAsStream(name);
    ){
      while (true){
        buf = new byte[8192];
        read = s.read(buf);
        if (read==-1){
          break;
        }
        len+=read;
        list.add(buf);
        if (read!=buf.length){
          break;
        }
      }
    }
    byte[] arr = new byte[len];
    int i = 0;
    for (byte[] bytes:list){
      read = Math.min(bytes.length,len);
      len-=read;
      System.arraycopy(bytes, 0, arr, i, read);
      i+=read;
    }
    return new String(arr, java.nio.charset.StandardCharsets.UTF_8);
  }
  /**
   * Encodes a string to be parsed as a list.
   * Intended to be used to encode AJAX responses.
   * Escapes semi-colons and backslashes using the backslash character.
   */
  public static String encodeAJAX(String str){
    int len = str.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    for (int i=0;i<len;++i){
      c = str.charAt(i);
      if (c=='\\' || c==';'){
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }
  /**
   * @return a list of strings decoded from the input parameter.
   */
  public static ArrayList<String> decodeAJAX(String str){
    final ArrayList<String> list = new ArrayList<String>();
    final int len = str.length();
    if (len==0){ return list; }
    final StringBuilder sb = new StringBuilder();
    char c;
    boolean b = false;
    for (int i=0;i<len;++i){
      c = str.charAt(i);
      if (b || c!='\\'){
        if (!b && c==';'){
          list.add(sb.toString());
          sb.setLength(0);
        }else{
          sb.append(c);
          b = false;
        }
      }else{
        b = true;
      }
    }
    if (sb.length()>0){
      list.add(sb.toString());
    }
    return list;
  }
  /**
   * Escapes a {@code String} for usage in HTML attribute values.
   * @param str is the {@code String} to escape.
   * @return the escaped {@code String}.
   */
  public static String escapeHTML(String str){
    int len = str.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    int j;
    for (int i=0;i<len;++i){
      c = str.charAt(i);
      j = c;
      if (j>=32 && j<127){
        switch (c){
          case '&':{
            sb.append("&amp;");
            break;
          }
          case '"':{
            sb.append("&quot;");
            break;
          }
          case '\'':{
            sb.append("&apos;");
            break;
          }
          case '<':{
            sb.append("&lt;");
            break;
          }
          case '>':{
            sb.append("&gt;");
            break;
          }
          default:{
            sb.append(c);
          }
        }
      }else if (j<1114111 && (j<=55296 || j>57343)){
        sb.append("&#").append(Integer.toString(j)).append(";");
      }
    }
    return sb.toString();
  }
  /**
   * Intended to escape strings for use in Javascript.
   * Escapes backslashes, single quotes, and double quotes.
   * Replaces new-line characters with the corresponding escape sequences.
   */
  public static String escapeJS(String str){
    int len = str.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    for (int i=0;i<len;++i){
      c = str.charAt(i);
      switch (c){
        case '\\': case '\'': case '"': {
          sb.append('\\').append(c);
          break;
        }
        case '\n': {
          sb.append("\\n");
          break;
        }
        case '\t': {
          sb.append("\\t");
          break;
        }
        case '\r': {
          sb.append("\\r");
          break;
        }
        case '\b': {
          sb.append("\\b");
          break;
        }
        case '\f': {
          sb.append("\\f");
          break;
        }
        default: {
          sb.append(c);
        }
      }
    }
    return sb.toString();
  }
}
