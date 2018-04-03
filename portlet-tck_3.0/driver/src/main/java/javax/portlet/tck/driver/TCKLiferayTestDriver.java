/*  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package javax.portlet.tck.driver;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;


/**
 * @author Vernon Singleton
 *
 */
@RunWith(value = Parameterized.class)
public class TCKLiferayTestDriver extends TCKSimpleTestDriver {

   private static Properties nonExclusiveTCs = new Properties();
   private static String baseUrl;

   private void getBaseUrl() {

      if (baseUrl == null || "".equals(baseUrl)) {
         StringBuilder sb = new StringBuilder();
         sb.append("http://");
         sb.append(host);
         if (port != null && !port.isEmpty()) {
            sb.append(":");
            sb.append(port);
         }
         sb.append("/");
         sb.append(testContextBase);
         baseUrl = sb.toString();
      }
   }

   private void getNonExclusiveTCs() {
      String nonExclusiveTCsFile = System.getProperty("test.non.exclusive.file", "");
      System.out.println("   nonExclusiveTCsFile  =" + nonExclusiveTCsFile);

      if ("".equals(nonExclusiveTCsFile)) {
         System.out.println("   no nonExclusiveTCsFile given.");
      } else {
         try {
            FileInputStream fis = new FileInputStream(nonExclusiveTCsFile);
            nonExclusiveTCs.loadFromXML(fis);
         } catch (Exception e) {
            System.err.println("Could not read nonExclusiveTCs file. Attempted to read file " + nonExclusiveTCsFile);
            e.printStackTrace();
         }
      }
      System.out.println("   # nonExclusiveTCs =" + nonExclusiveTCs.size());
   }

   public TCKLiferayTestDriver(String p, String t) {
      super(p, t);
      if (baseUrl == null) {
         getBaseUrl();
      }
      if (nonExclusiveTCs.size() == 0) {
		  getNonExclusiveTCs();
	  }
   }

   private void getExclusive() {

      String[] tokens = tcName.split("_");
      String liferayWar = "_WAR_tck" + tokens[0];

      String noV = tcName.substring(2);
      StringBuilder b = new StringBuilder(noV);
      String portletName = b.substring(0, noV.lastIndexOf("_"));
      String portletId =  portletName + liferayWar;

      String url = baseUrl +
                   page +
                   "?p_p_id=" + portletId +
                   "&p_p_state=exclusive";

      // System.out.println("getExclusive: url = " + url);
      driver.get(url);

   }

   @Test
   public void test() {
      debugLines.add("   execute test.");

      if (dryrun) {
         return;
      }

      try {

         // This is optimized for many results being present on the same page.
         // First look for the test results or links already being present on the page.

         List<WebElement> wels;

         if (nonExclusiveTCs.getProperty(tcName) == null) {

            // tcName is NOT in the list of nonExclusive tests, so it should be tested in exclusive state
            // System.out.println("test: " + tcName + " should be tested in exclusive state ...");
            // System.out.println("test: driver.getCurrentUrl() = " + driver.getCurrentUrl());

            // get exclusive portlet for this test case if not already exclusive
            List<WebElement> metas = driver.findElements(By.tagName("meta"));
            if (!metas.isEmpty()) {
               System.out.println("test: getExclusive ...");
               getExclusive();
            }

            // get exclusive portlet for this test case if we are on the wrong page
            // this usually occurs when the last test case was for a different portlet
            wels = driver.findElements(By.name(tcName));
            if (wels.isEmpty()) {
               // System.out.println("test: wels.isEmpty() true ... getting exclusive portlet ...");
               getExclusive();
            }

         } else {

            // tcName is in the list of nonExclusive tests, so it should NOT be tested in exclusive state
            System.out.println("test: " + tcName + " NOT exclusive ... this should NOT be tested using exclusive state.");
            accessPage();

         }

         wels = driver.findElements(By.name(tcName));
         debugLines.add("   TC elements already on page: " + !wels.isEmpty() + ", tcname===" + tcName + "===");
         if (wels.isEmpty()) {
            System.out.println("test: wels.isEmpty() true ... accessing page normally ...");
            wels = accessPage();
         }

         // process links if present
         wels = processClickable(wels);
         debugLines.add("   After processing clickable, results found: " + !wels.isEmpty());

         // wait for any async JavaScript tests to complete
         processAsync();

         checkResults(wels);

      } catch(Exception e) {

         // Some type of unexpected error occurred, so generate text
         // and mark the TC as failed.

         System.out.println("   Exception occurred: " + e.getMessage());
         for (String line : debugLines) {
            System.out.println(line);
         }

         assertTrue("Test case " + tcName + " failed. " +
               " Setup link could not be accessed. \nException: " + e.toString(), false);
      }
   }
}
