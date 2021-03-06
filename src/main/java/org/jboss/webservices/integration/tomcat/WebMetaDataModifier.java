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
package org.jboss.webservices.integration.tomcat;

import java.util.List;
import java.util.ResourceBundle;

import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMetaData;
import org.jboss.webservices.integration.util.ASHelper;
import org.jboss.webservices.integration.util.WebMetaDataHelper;
import org.jboss.ws.api.util.BundleUtils;
import org.jboss.ws.common.integration.WSConstants;
import org.jboss.ws.common.integration.WSHelper;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * The modifier of jboss web meta data.
 * It configures WS transport for every webservice endpoint
 * plus propagates WS stack specific context parameters if required.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
final class WebMetaDataModifier
{
   private static final ResourceBundle bundle = BundleUtils.getBundle(WebMetaDataModifier.class);
   /** Logger. */
   private final Logger log = Logger.getLogger(WebMetaDataModifier.class);

   /**
    * Constructor.
    */
   WebMetaDataModifier()
   {
      super();
   }

   /**
    * Modifies web meta data to configure webservice stack transport and properties.
    *
    * @param dep webservice deployment
    */
   void modify(final Deployment dep)
   {
      final JBossWebMetaData jbossWebMD = WSHelper.getRequiredAttachment(dep, JBossWebMetaData.class);

      this.configureEndpoints(dep, jbossWebMD);
      this.modifyContextRoot(dep, jbossWebMD);
   }

   /**
    * Configures transport servlet class for every found webservice endpoint. 
    *
    * @param dep webservice deployment
    * @param jbossWebMD web meta data
    */
   private void configureEndpoints(final Deployment dep, final JBossWebMetaData jbossWebMD)
   {
      final String transportClassName = this.getTransportClassName(dep);
      final ClassLoader loader = dep.getInitialClassLoader();
      this.log.trace("Modifying servlets");

      for (final ServletMetaData servletMD : jbossWebMD.getServlets())
      {
         final boolean isWebserviceEndpoint = ASHelper.getEndpointClass(servletMD, loader) != null;

         if (isWebserviceEndpoint)
         {
            // set transport servlet
            servletMD.setServletClass(transportClassName);

            // configure webservice endpoint
            final String endpointClassName = servletMD.getServletClass();
            this.log.debug("Setting transport class: " + transportClassName + " for servlet: " + endpointClassName);
            final List<ParamValueMetaData> initParams = WebMetaDataHelper.getServletInitParams(servletMD);
            WebMetaDataHelper.newParamValue(Endpoint.SEPID_DOMAIN_ENDPOINT, endpointClassName, initParams);
         }
      }
   }

   /**
    * Modifies context root. 
    *
    * @param dep webservice deployment
    * @param jbossWebMD web meta data
    */
   private void modifyContextRoot(final Deployment dep, final JBossWebMetaData jbossWebMD)
   {
      final String contextRoot = dep.getService().getContextRoot();
      this.log.debug("Setting context root: " + contextRoot + " for deployment: " + dep.getSimpleName());
      jbossWebMD.setContextRoot(contextRoot);
   }

   /**
    * Returns stack specific transport class name.
    *
    * @param dep webservice deployment
    * @return stack specific transport class name
    * @throws IllegalStateException if transport class name is not found in deployment properties map
    */
   private String getTransportClassName(final Deployment dep)
   {
      String transportClassName = (String) dep.getProperty(WSConstants.STACK_TRANSPORT_CLASS);

      if (transportClassName == null)
      {
         throw new IllegalStateException(BundleUtils.getMessage(bundle, "CANNOT_OBTAIN_DEPLOYMENT_PROPERTY",  WSConstants.STACK_TRANSPORT_CLASS));
      }

      return transportClassName;
   }
}
