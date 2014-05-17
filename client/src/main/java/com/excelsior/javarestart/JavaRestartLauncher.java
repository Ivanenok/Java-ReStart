/*
 * Copyright (c) 2013-2014, Nikita Lipsky, Excelsior LLC.
 *
 *  Java ReStart is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Java ReStart is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.excelsior.javarestart;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.awt.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class JavaRestartLauncher {

    public static void fork(String ... args)  {

        String javaHome = System.getProperty("java.home");
        System.out.println(javaHome);
        String codeSource = JavaRestartLauncher.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm().substring(6);
//        String fxrt = javaHome + "\\lib\\jfxrt.jar";
        String classpath = System.getProperty("java.class.path");
//        String classpath = "\"" + codeSource + ";" + fxrt + "\"";
        System.out.println(codeSource);
        String javaLauncher = "\"" + javaHome + "\\bin\\javaw.exe\"" + " -splash:" + codeSource + "defaultSplash.gif" + " -Dbinary.css=false -cp \"" + classpath + "\" " + JavaRestartLauncher.class.getName();
        for (String arg: args) {
            javaLauncher = javaLauncher + " " + arg;
        }

        System.out.println(javaLauncher);

        final String finalJavaLauncher = javaLauncher;
        (new Thread(){
            @Override
            public void run() {
                try {
                    Runtime.getRuntime().exec(finalJavaLauncher).waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    public static String getText(String url) throws IOException {
        URL website = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) website.openConnection();
        try (LineNumberReader in = new LineNumberReader(
                    new InputStreamReader(
                            connection.getInputStream())))
        {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);

            return response.toString();
        }
    }

    public static JSONObject getJSON(String url) throws IOException {
        return (JSONObject) JSONValue.parse(getText(url));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: <URL> {<MainClass>}");
            return;
        }

        if (args[0].equals("fork")) {
            String[] args2 = new String[args.length - 1];
            for (int i = 0; i < args.length -1; i++) {
                args2[i] = args[i + 1];
            }
            fork(args2);
            return;
        }

        AppClassloader loader = new AppClassloader(args[0]);
        Thread.currentThread().setContextClassLoader(loader);
        String main;
        JSONObject obj = getJSON(args[0]);
        if (args.length < 2) {
            main = (String) obj.get("main");
        } else {
            main = args[1];
        }

        String splash = (String) obj.get("splash");
        if ( splash != null) {
            SplashScreen scr = SplashScreen.getSplashScreen();
            if (scr != null) {
                URL url = loader.getResource(splash);
                scr.setImageURL(url);
            }
        }

        //auto close splash after 45 seconds
        Thread splashClose = new Thread(){
            @Override
            public void run() {
                try {
                    sleep(45000);
                } catch (InterruptedException e) {
                }
                SplashScreen scr = SplashScreen.getSplashScreen();
                if ((scr!=null) && (scr.isVisible())) {
                    scr.close();
                }
            }
        };
        splashClose.setDaemon(true);
        splashClose.start();

        Class mainClass = loader.loadClass(main);
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.setAccessible(true);
        mainMethod.invoke(null, new Object[]{new String[0]});
    }
}