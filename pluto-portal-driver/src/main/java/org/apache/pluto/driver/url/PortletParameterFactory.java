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

package org.apache.pluto.driver.url;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pluto.driver.services.portal.PublicRenderParameterMapper;
import org.apache.pluto.driver.url.PortalURL.URLType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Scott Nicklous
 * 
 * This class bridges between the URL parameter representation and the parameter
 * representation presented through the portlet API. It also bridges between the 
 * JSR 286 and the JSR 362 parameter handling APIs. 
 *
 */
public class PortletParameterFactory {
   private static final Logger LOGGER = LoggerFactory.getLogger(PortletParameterFactory.class);
   private final static boolean isDebug = LOGGER.isDebugEnabled();
   
   PortalURL url;
   
   HashMap<String, ArrayList<PortalURLParameter>> wid2Render;
   HashMap<String, ArrayList<PortalURLParameter>> wid2Action;
   HashMap<String, ArrayList<PortalURLParameter>> wid2Resource;
   
   static final HashSet<PortalURL.URLType> actionTypes = new HashSet<PortalURL.URLType>();
   static {
      actionTypes.add(URLType.Action);
      actionTypes.add(URLType.AjaxAction);
      actionTypes.add(URLType.PartialAction);
   }
   
   private final static HashMap<PortalURL.URLType, String> urlTypeMap  = 
         new HashMap<PortalURL.URLType, String>();
   static {
      urlTypeMap.put(URLType.Action, PortalURLParameter.PARAM_TYPE_ACTION);
      urlTypeMap.put(URLType.Render, PortalURLParameter.PARAM_TYPE_RENDER);
      urlTypeMap.put(URLType.Resource, PortalURLParameter.PARAM_TYPE_RESOURCE);
   }
   
   public PortletParameterFactory(PortalURL url) {
      this.url = url;
   }
   
   private void addParam(Map<String, ArrayList<PortalURLParameter>> map, PortalURLParameter parm) {
      String wid = parm.getWindowId();
      if (!map.keySet().contains(wid)) {
         map.put(wid, new ArrayList<PortalURLParameter>());
      }
      map.get(wid).add(parm);
   }
   
   /**
    * process the URL parameters. Do this as late as possible so that 
    * the servlet rquest parameters get handled properly.
    */
   private void processParams() {
      wid2Render = new HashMap<String, ArrayList<PortalURLParameter>>();
      wid2Action = new HashMap<String, ArrayList<PortalURLParameter>>();
      wid2Resource = new HashMap<String, ArrayList<PortalURLParameter>>();
      
      for (PortalURLParameter parm : url.getParameters()) {
         if (parm.getType().equals(PortalURLParameter.PARAM_TYPE_ACTION)) {
            addParam(wid2Action, parm);
         } else if (parm.getType().equals(PortalURLParameter.PARAM_TYPE_RESOURCE)) {
            addParam(wid2Resource, parm);
         } else {
            addParam(wid2Render, parm);
         }
      }

   }

   /**
    * This V2.0 method returns the private parameters for the given window ID.
    * In the case of a render URL, it returns only the private render parameters.
    * 
    * In the case of an action or resource URL, it returns the action or resource 
    * parameters combined with the render parameters, with the action or resource
    * parameters taking precedence.
    * 
    * Note that the  latter circumstance will only occur with V3.0 portlets.
    *  
    * @param windowId
    * @return
    */
   public Map<String, String[]> getPrivateParameterMap(String windowId) {
      HashMap<String, String[]> parameters = new HashMap<String, String[]>();
      processParams();
      
      boolean isV3 = url.isVersion3(windowId);

      // get the action or resource parameters
      
      if (wid2Action.containsKey(windowId)) {
         for (PortalURLParameter parm : wid2Action.get(windowId)) {
            parameters.put(parm.getName(), parm.getValues().clone());
         }
      } else if (wid2Resource.containsKey(windowId)) {
         for (PortalURLParameter parm : wid2Resource.get(windowId)) {
            parameters.put(parm.getName(), parm.getValues().clone());
         }
      }
      
      // Now merge in the render parameters if we're not dealing with a V2 action

      if (isV3 || !actionTypes.contains(url.getType())) {
         if (wid2Render.containsKey(windowId)) {
            for (PortalURLParameter parm : wid2Render.get(windowId)) {
               if (parameters.containsKey(parm.getName())) {
                  ArrayList<String> vals = 
                        new ArrayList<String>(Arrays.asList(parameters.get(parm.getName())));
                  vals.addAll(Arrays.asList(parm.getValues()));
                  parameters.put(parm.getName(), vals.toArray(new String[0]));
               } else {
                  parameters.put(parm.getName(), parm.getValues().clone());
               }
            }
         }
      }

      if (isDebug) {
         StringBuffer sb = new StringBuffer();
         sb.append("Dump private Parameter Map:");
         for (String k : parameters.keySet()) {
            sb.append("\nName: " + k + ", Values: ");
            String sep = "";
            for (String v : parameters.get(k)) {
               sb.append(sep + v);
               sep = ", ";
            }
         }
         LOGGER.debug(sb.toString());
      }
      return parameters;
   }
   
   /**
    * Returns the active (= have been set) public render parameters for the
    * given window ID.
    *  
    * @param windowId
    * @return
    */
   public Map<String, String[]> getPublicParameterMap(String windowId) {
      HashMap<String, String[]> parameters = new HashMap<String, String[]>();

      PublicRenderParameterMapper mapper = url.getPublicRenderParameterMapper();
      
      // get the active PRPs only
      
      List<PortalURLPublicParameter> prps = mapper.getPRPsForWindow(windowId, true);
      for (PortalURLPublicParameter prp : prps) {
         parameters.put(prp.getName(), prp.getValues().clone());
      }
      
      if (isDebug) {
         LOGGER.debug("getPublicParameterMap returning " + parameters.size() + " parameters.");
      }
      
      return parameters;
   }

   /**
    * This is a V2 method to get the private render parameter map during
    * a resource request.
    * 
    * @return
    */
   public Map<String, String[]> getResourceRenderParameterMap(String windowId) {
      HashMap<String, String[]> parameters = new HashMap<String, String[]>();

      processParams();
      
      if (wid2Render.containsKey(windowId)) {
         for (PortalURLParameter parm : wid2Render.get(windowId)) {
            parameters.put(parm.getName(), parm.getValues().clone());
         }
      }
      
      return parameters;
   }

   public void addPublicRenderParameter(String windowId, String name, String[] values) {
      PublicRenderParameterMapper prpMapper = url.getPublicRenderParameterMapper();
      if (isDebug) {
         StringBuilder txt = new StringBuilder("Add PRP. Window: ");
         txt.append(windowId).append(", name: ").append(name)
               .append(", values = ").append(Arrays.toString(values));
         LOGGER.debug(txt.toString());
      }
      int index = prpMapper.getIndex(windowId, name);
      if (index >= 0) {
         prpMapper.setValues(index, values);
      } else {
         StringBuilder txt = new StringBuilder(
               "Public render parameter for window: ");
         txt.append(windowId).append(", name: ").append(name)
               .append(" not found in mapper");
         LOGGER.warn(txt.toString());
      }
   }

   public void removePublicRenderParameter(String windowId, String name) {
      PublicRenderParameterMapper prpMapper = url.getPublicRenderParameterMapper();
      if (isDebug) {
         StringBuilder txt = new StringBuilder("Remove PRP. Window: ");
         txt.append(windowId).append(", name: ").append(name);
         LOGGER.debug(txt.toString());
      }
      int index = prpMapper.getIndex(windowId, name);
      if (index >= 0) {
         prpMapper.setRemoved(index, true);
      } else {
         StringBuilder txt = new StringBuilder("Public render parameter for window: ");
         txt.append(windowId).append(", name: ").append(name)
            .append(" not found in mapper");
         LOGGER.warn(txt.toString());
      }
   }

   public boolean isPublicRenderParameter(String windowId, String name) {
      PublicRenderParameterMapper prpMapper = url.getPublicRenderParameterMapper();
      if (isDebug) {
         StringBuilder txt = new StringBuilder(
               "isPublicRenderParameter Window: ");
         txt.append(windowId).append(", name: ").append(name);
         LOGGER.debug(txt.toString());
      }
      int index = prpMapper.getIndex(windowId, name);
      return (index >= 0);
   }

   // This method is for V2 compatibility. It always returns the private 
   // parameter names for the active URL type - Render parameters for a render URL,
   // Action parameters for an action URL, and resouce parameters for a Resource URL.
   public Set<String> getPrivateParameterNames(String windowId) {
      HashSet<String> names = new HashSet<String>();
      String type = urlTypeMap.get(url.getType());
      
      for (PortalURLParameter p : url.getParameters()) {
         if (p.getWindowId().equals(windowId) && p.getType().equals(type)) {
            names.add(p.getName());
         }
      }

      if (isDebug) {
         StringBuilder txt = new StringBuilder();
         txt.append("Window ID: ").append(windowId);
         txt.append(", URL / Parameter type: ").append(type);
         txt.append(", names: ").append(names.toString());
         LOGGER.debug(txt.toString());
      }

      return names;
   }

   // This method is for V2 compatibility. It always returns the private 
   // parameter values for the active URL type - Render parameters for a render URL,
   // Action parameters for an action URL, and resouce parameters for a Resource URL.
   public String[] getParameterValues(String windowId, String name) {
      PublicRenderParameterMapper prpMapper = url.getPublicRenderParameterMapper();
      String type = urlTypeMap.get(url.getType());
      String[] vals = null;
      
      if (isDebug) {
         StringBuilder txt = new StringBuilder("Setting ");
         txt.append("name: ").append(name);
         txt.append(", URL / Parameter type: ").append(type);
         txt.append(", window ID: ").append(windowId);
         LOGGER.debug(txt.toString());
      }
      
      if (isPublicRenderParameter(windowId, name)) {
         int index = prpMapper.getIndex(windowId, name);
         if (index >= 0) {
            vals = prpMapper.getValues(index).clone();
         } else {
            LOGGER.debug("Public render parameter name not found in index.");
         }
      } else {
         for (PortalURLParameter p : url.getParameters()) {
            if (p.getWindowId().equals(windowId)
               && p.getName().equals(name) && p.getType().equals(type)) {
               vals = p.getValues().clone();
            }
         }
      }
      
      return vals;
   }

   /**
    * Sets a parameter. This method sets a parameter
    * of type corresponding to the URL type - Action parameter for an Action URL, etc.
    * V2 compatibility method.
    */
   public void setParameter(String windowId, String name, String[] values) {
      String type = urlTypeMap.get(url.getType());
      
      if (isDebug) {
         StringBuilder txt = new StringBuilder("Setting ");
         txt.append("name: ").append(name);
         txt.append(", values: ").append(Arrays.toString(values));
         txt.append(", type: ").append(type);
         txt.append(", window ID: ").append(windowId);
         LOGGER.debug(txt.toString());
      }
      
      if (url.getType() == URLType.Render && isPublicRenderParameter(windowId, name)) {
         addPublicRenderParameter(windowId, name, values);
      } else {
         if (urlTypeMap.containsKey(url.getType())) {
            PortalURLParameter pup = new PortalURLParameter(windowId, name, values, type);
            url.setParameter(pup);
         } else {
            LOGGER.warn("Unsupported URL / Parameter type: " + url.getType());
         }
      }
   }

   /**
    * Remove parameter. If the parameter is a public render parameter, 
    * an <code>IllegalArgumentException</code> is thrown. This method removes a parameter
    * of type corresponding to the URL type - Action parameter for an Action URL, etc. 
    * (V3 Spec errata)
    */
   public void removeParameter(String windowId, String name) {
      String type = urlTypeMap.get(url.getType());
      if (isDebug) {
         StringBuilder txt = new StringBuilder("Removing ");
         txt.append("name: ").append(name);
         txt.append(", type: ").append(type);
         txt.append(", window ID: ").append(windowId);
         LOGGER.debug(txt.toString());
      }
      if (isPublicRenderParameter(windowId, name)) {
         throw new IllegalArgumentException("Cannot set a public render parameter to null. Parameter name: " + name);
      } else {
         PortalURLParameter pup = new PortalURLParameter(windowId, name, null, type);
         url.removeParameter(pup);
      }
   }
}
