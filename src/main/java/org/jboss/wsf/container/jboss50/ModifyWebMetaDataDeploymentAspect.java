/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.wsf.container.jboss50;

//$Id: ModifyWebMetaDataDeployer.java 3150 2007-05-20 00:29:48Z thomas.diesler@jboss.com $

import org.jboss.metadata.Listener;
import org.jboss.metadata.NameValuePair;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.web.ParamValue;
import org.jboss.metadata.web.ParamValue.ParamType;
import org.jboss.metadata.web.Servlet;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.deployment.Endpoint;

import java.util.Iterator;
import java.util.Map;

/**
 * A deployer that modifies the web.xml meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class ModifyWebMetaDataDeploymentAspect extends DeploymentAspect
{
   @Override
   public void create(Deployment dep)
   {
      String propKey = "org.jboss.ws.webapp.ServletClass";
      String servletClass = (String)dep.getProperty(propKey);
      if (servletClass == null)
         throw new IllegalStateException("Cannot obtain context property: " + propKey);

      modifyServletClass(dep, servletClass);

      propKey = "org.jboss.ws.webapp.ServletContextListener";
      String listenerClass = (String)dep.getProperty(propKey);
      if (listenerClass != null)
         modifyListener(dep, listenerClass);
      
      propKey = "org.jboss.ws.webapp.ContextParameterMap";
      Map<String, String> contextParams = (Map<String, String>)dep.getProperty(propKey);
      if (contextParams != null)
         modifyContextParams(dep, contextParams);
   }

   private void modifyServletClass(Deployment dep, String servletClass)
   {

      WebMetaData webMetaData = dep.getAttachment(WebMetaData.class);
      if (webMetaData != null)
      {
         for (Servlet servlet : webMetaData.getServlets())
         {
            String orgServletClass = servlet.getServletClass();

            // JSP
            if (orgServletClass == null || orgServletClass.length() == 0)
            {
               log.debug("Innore servlet class: " + orgServletClass);
               continue;
            }

            // Nothing to do if we have an <init-param>
            if (!isAlreadyModified(servlet) && !isJavaxServlet(orgServletClass, dep.getInitialClassLoader()))
            {
               servlet.setServletClass(servletClass);
               NameValuePair initParam = new NameValuePair(Endpoint.SEPID_DOMAIN_ENDPOINT, orgServletClass);
               servlet.addInitParam(initParam);
            }
         }
      }
   }

   private void modifyListener(Deployment dep, String listenerClass)
   {
      WebMetaData webMetaData = dep.getAttachment(WebMetaData.class);
      if (webMetaData != null)
      {
         Listener listener = new Listener();
         listener.setListenerClass(listenerClass);
         webMetaData.addListener(listener);
      }
   }

   private void modifyContextParams(Deployment dep, Map<String, String> contextParams)
   {
      WebMetaData webMetaData = dep.getAttachment(WebMetaData.class);
      if (webMetaData != null)
      {
         for (Map.Entry<String, String> entry : contextParams.entrySet())
         {
            ParamValue ctxParam = new ParamValue();
            ctxParam.setType(ParamType.CONTEXT_PARAM);
            ctxParam.setName(entry.getKey());
            ctxParam.setValue(entry.getValue());
            webMetaData.addContextParam(ctxParam);
         }
      }
   }

   private boolean isJavaxServlet(String orgServletClass, ClassLoader loader)
   {
      boolean isServlet = false;
      if (loader != null)
      {
         try
         {
            Class servletClass = loader.loadClass(orgServletClass);
            isServlet = javax.servlet.Servlet.class.isAssignableFrom(servletClass);
            if (isServlet == true)
            {
               log.info("Ignore servlet: " + orgServletClass);
            }
         }
         catch (ClassNotFoundException e)
         {
            log.warn("Cannot load servlet class: " + orgServletClass);
         }
      }
      return isServlet;
   }

   private boolean isAlreadyModified(Servlet servlet)
   {
      Iterator itParams = servlet.getInitParams().iterator();
      while (itParams.hasNext())
      {
         NameValuePair pair = (NameValuePair)itParams.next();
         if (Endpoint.SEPID_DOMAIN_ENDPOINT.equals(pair.getName()))
            return true;
      }
      return false;
   }
}