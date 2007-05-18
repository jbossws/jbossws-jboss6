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
package org.jboss.wsintegration.container.jboss50;

//$Id$

import java.util.Iterator;

import org.jboss.metadata.NameValuePair;
import org.jboss.metadata.WebMetaData;
import org.jboss.metadata.web.Servlet;
import org.jboss.wsintegration.spi.deployment.AbstractDeployer;
import org.jboss.wsintegration.spi.deployment.Deployment;
import org.jboss.wsintegration.spi.deployment.Endpoint;

/**
 * A deployer that modifies the web.xml meta data 
 *
 * @author Thomas.Diesler@jboss.org
 * @since 25-Apr-2007
 */
public class ModifyWebMetaDataDeployer extends AbstractDeployer
{
   private String servletClass;

   public String getServletClass()
   {
      return servletClass;
   }

   public void setServletClass(String servletClass)
   {
      this.servletClass = servletClass;
   }

   @Override
   public void create(Deployment dep)
   {
      WebMetaData webMetaData = dep.getContext().getAttachment(WebMetaData.class);
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
            if (!isAlreadyModified(servlet) && !isJavaxServlet(orgServletClass, dep.getClassLoader()))
            {
               servlet.setServletClass(servletClass);
               NameValuePair initParam = new NameValuePair(Endpoint.SEPID_DOMAIN_ENDPOINT, orgServletClass);
               servlet.addInitParam(initParam);
            }
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