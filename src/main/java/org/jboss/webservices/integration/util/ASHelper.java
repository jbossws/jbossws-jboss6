/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.jboss.webservices.integration.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.jws.WebService;
import javax.servlet.Servlet;
import javax.xml.ws.WebServiceProvider;

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.metadata.common.jboss.WebserviceDescriptionMetaData;
import org.jboss.metadata.common.jboss.WebserviceDescriptionsMetaData;
import org.jboss.metadata.ejb.jboss.JBossMetaData;
import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.ws.api.util.BundleUtils;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.metadata.webservices.WebservicesMetaData;
import org.jboss.webservices.integration.WebServiceDeclaration;
import org.jboss.webservices.integration.WebServiceDeployment;

/**
 * JBoss AS integration helper class.
 *
 * @author <a href="ropalka@redhat.com">Richard Opalka</a>
 */
public final class ASHelper
{
   private static final ResourceBundle bundle = BundleUtils.getBundle(ASHelper.class);
   /**
    * EJB invocation property.
    */
   public static final String CONTAINER_NAME = "org.jboss.wsf.spi.invocation.ContainerName";

   /** Logger. */
   private static final Logger LOGGER = Logger.getLogger(ASHelper.class);

   /**
    * Forbidden constructor.
    */
   private ASHelper()
   {
      super();
   }

   /**
    * Returns true if unit contains JAXWS JSE, JAXRPC JSE, JAXWS EJB or JAXRPC EJB deployment.
    *
    * @param unit deployment unit
    * @return true if JAXWS JSE, JAXRPC JSE, JAXWS EJB or JAXRPC EJB deployment, false otherwise.
    */
   public static boolean isWebServiceDeployment(final DeploymentUnit unit)
   {
      return ASHelper.getOptionalAttachment(unit, Deployment.class) != null;
   }

   /**
    * Returns true if unit contains JAXRPC EJB deployment.
    *
    * @param unit deployment unit
    * @return true if JAXRPC EJB deployment, false otherwise
    */
   public static boolean isJaxrpcEjbDeployment(final DeploymentUnit unit)
   {
      final boolean hasWebservicesMD = hasAttachment(unit, WebservicesMetaData.class);
      final boolean hasJBossMD = unit.getAllMetaData(JBossMetaData.class).size() > 0;

      return hasWebservicesMD && hasJBossMD;
   }

   /**
    * Returns true if unit contains JAXRPC JSE deployment.
    *
    * @param unit deployment unit
    * @return true if JAXRPC JSE deployment, false otherwise
    */
   public static boolean isJaxrpcJseDeployment(final DeploymentUnit unit)
   {
      final boolean hasWebservicesMD = hasAttachment(unit, WebservicesMetaData.class);
      final boolean hasJBossWebMD = hasAttachment(unit, JBossWebMetaData.class);

      if (hasWebservicesMD && hasJBossWebMD)
      {
         return getJaxrpcServlets(unit).size() > 0;
      }

      return false;
   }

   /**
    * Returns true if unit contains JAXWS EJB deployment.
    *
    * @param unit deployment unit
    * @return true if JAXWS EJB deployment, false otherwise
    */
   public static boolean isJaxwsEjbDeployment(final DeploymentUnit unit)
   {
      final boolean hasWSDeployment = hasAttachment(unit, WebServiceDeployment.class);

      if (hasWSDeployment)
      {
         return getJaxwsEjbs(unit).size() > 0;
      }

      return false;
   }

   /**
    * Returns true if unit contains JAXWS JSE deployment.
    *
    * @param unit deployment unit
    * @return true if JAXWS JSE deployment, false otherwise
    */
   public static boolean isJaxwsJseDeployment(final DeploymentUnit unit)
   {
      final boolean hasJBossWebMD = ASHelper.hasAttachment(unit, JBossWebMetaData.class);

      if (hasJBossWebMD)
      {
         return ASHelper.getJaxwsServlets(unit).size() > 0;
      }

      return false;
   }

   /**
    * Gets list of JAXWS servlets meta data.
    *
    * @param unit deployment unit
    * @return list of JAXWS servlets meta data
    */
   public static List<ServletMetaData> getJaxwsServlets(final DeploymentUnit unit)
   {
      return ASHelper.getWebServiceServlets(unit, true);
   }

   /**
    * Gets list of JAXRPC servlets meta data.
    *
    * @param unit deployment unit
    * @return list of JAXRPC servlets meta data
    */
   public static List<ServletMetaData> getJaxrpcServlets(final DeploymentUnit unit)
   {
      return ASHelper.getWebServiceServlets(unit, false);
   }

   /**
    * Gets list of JAXWS EJBs meta data.
    *
    * @param unit deployment unit
    * @return list of JAXWS EJBs meta data
    */
   public static List<WebServiceDeclaration> getJaxwsEjbs(final DeploymentUnit unit)
   {
      final WebServiceDeployment wsDeployment = ASHelper.getRequiredAttachment(unit, WebServiceDeployment.class);
      final List<WebServiceDeclaration> endpoints = new ArrayList<WebServiceDeclaration>();

      final Iterator<WebServiceDeclaration> ejbIterator = wsDeployment.getServiceEndpoints().iterator();
      while (ejbIterator.hasNext())
      {
         final WebServiceDeclaration ejbContainer = ejbIterator.next();
         if (ASHelper.isWebServiceBean(ejbContainer))
         {
            endpoints.add(ejbContainer);
         }
      }

      return endpoints;
   }

   /**
    * Returns true if EJB container is webservice endpoint.
    *
    * @param ejbContainerAdapter EJB container adapter
    * @return true if EJB container is webservice endpoint, false otherwise
    */
   public static boolean isWebServiceBean(final WebServiceDeclaration ejbContainerAdapter)
   {
      final boolean isWebService = ejbContainerAdapter.getAnnotation(WebService.class) != null;
      final boolean isWebServiceProvider = ejbContainerAdapter.getAnnotation(WebServiceProvider.class) != null;

      return isWebService || isWebServiceProvider;
   }

   /**
    * Returns endpoint class name.
    *
    * @param servletMD servlet meta data
    * @return endpoint class name
    */
   public static String getEndpointName(final ServletMetaData servletMD)
   {
      final String endpointClass = servletMD.getServletClass();

      return endpointClass != null ? endpointClass.trim() : null;
   }

   /**
    * Returns servlet meta data for requested servlet name.
    *
    * @param jbossWebMD jboss web meta data
    * @param servletName servlet name
    * @return servlet meta data
    */
   public static ServletMetaData getServletForName(final JBossWebMetaData jbossWebMD, final String servletName)
   {
      for (JBossServletMetaData servlet : jbossWebMD.getServlets())
      {
         if (servlet.getName().equals(servletName))
         {
            return servlet;
         }
      }

      throw new IllegalStateException(BundleUtils.getMessage(bundle, "CANNOT_FIND_SERVLET_FOR_LINK",  servletName));
   }

   /**
    * Returns webservice endpoint class or null if passed servlet meta data belong to either JSP or servlet instance.
    *
    * @param servletMD servlet meta data
    * @param loader class loader
    * @return webservice endpoint class or null
    */
   public static Class<?> getEndpointClass(final ServletMetaData servletMD, final ClassLoader loader)
   {
      final String endpointClassName = ASHelper.getEndpointName(servletMD);
      final boolean notJSP = endpointClassName != null && endpointClassName.length() > 0;

      if (notJSP)
      {
         try
         {
            final Class<?> endpointClass = loader.loadClass(endpointClassName);
            final boolean notServlet = !Servlet.class.isAssignableFrom(endpointClass);

            if (notServlet)
            {
               return endpointClass;
            }
         }
         catch (ClassNotFoundException cnfe)
         {
            ASHelper.LOGGER.warn(BundleUtils.getMessage(bundle, "CANNOT_LOAD_SERVLET_CLASS",  endpointClassName),  cnfe);
         }
      }

      return null;
   }

   /**
    * Returns required attachment value from deployment unit.
    *
    * @param <A> expected value
    * @param unit deployment unit
    * @param key attachment key
    * @return required attachment
    * @throws IllegalStateException if attachment value is null
    */
   public static <A> A getRequiredAttachment(final DeploymentUnit unit, final Class<A> key)
   {
      final A value = unit.getAttachment(key);
      if (value == null)
      {
         ASHelper.LOGGER.error(BundleUtils.getMessage(bundle, "CANNOT_FIND_ATTACHMENT_IN_DEPLOYMENT_UNIT",  key));
         throw new IllegalStateException();
      }

      return value;
   }

   /**
    * Returns optional attachment value from deployment unit or null if not bound.
    *
    * @param <A> expected value
    * @param unit deployment unit
    * @param key attachment key
    * @return optional attachment value or null
    */
   public static <A> A getOptionalAttachment(final DeploymentUnit unit, final Class<A> key)
   {
      return unit.getAttachment(key);
   }

   /**
    * Returns true if deployment unit have attachment value associated with the <b>key</b>.
    *
    * @param unit deployment unit
    * @param key attachment key
    * @return true if contains attachment, false otherwise
    */
   public static boolean hasAttachment(final DeploymentUnit unit, final Class<?> key)
   {
      return ASHelper.getOptionalAttachment(unit, key) != null;
   }

   /**
    * Returns first webservice description meta data or null if not found.
    *
    * @param wsDescriptionsMD webservice descriptions
    * @return webservice description
    */
   public static WebserviceDescriptionMetaData getWebserviceDescriptionMetaData(
         final WebserviceDescriptionsMetaData wsDescriptionsMD)
   {
      if (wsDescriptionsMD != null)
      {
         if (wsDescriptionsMD.size() > 1)
         {
            ASHelper.LOGGER.warn(BundleUtils.getMessage(bundle, "MULTIPLE_WS_DESP_ELEMENTS_NOT_SUPPORTED"));
         }

         if (wsDescriptionsMD.size() > 0)
         {
            return wsDescriptionsMD.iterator().next();
         }
      }

      return null;
   }

   /**
    * Gets list of JAXRPC or JAXWS servlets meta data.
    *
    * @param unit deployment unit
    * @param jaxws if passed value is <b>true</b> JAXWS servlets list will be returned, otherwise JAXRPC servlets list
    * @return either JAXRPC or JAXWS servlets list
    */
   private static List<ServletMetaData> getWebServiceServlets(final DeploymentUnit unit, final boolean jaxws)
   {
      final JBossWebMetaData jbossWebMD = ASHelper.getRequiredAttachment(unit, JBossWebMetaData.class);
      final ClassLoader loader = unit.getClassLoader();
      final List<ServletMetaData> endpoints = new ArrayList<ServletMetaData>();

      for (ServletMetaData servletMD : jbossWebMD.getServlets())
      {
         final Class<?> endpointClass = ASHelper.getEndpointClass(servletMD, loader);

         if (endpointClass != null)
         {
            // check webservice annotations
            final boolean isWebService = endpointClass.isAnnotationPresent(WebService.class);
            final boolean isWebServiceProvider = endpointClass.isAnnotationPresent(WebServiceProvider.class);
            // detect webservice type
            final boolean isJaxwsEndpoint = jaxws && (isWebService || isWebServiceProvider);
            final boolean isJaxrpcEndpoint = !jaxws && (!isWebService && !isWebServiceProvider);

            if (isJaxwsEndpoint || isJaxrpcEndpoint)
            {
               endpoints.add(servletMD);
            }
         }
      }

      return endpoints;
   }
}
