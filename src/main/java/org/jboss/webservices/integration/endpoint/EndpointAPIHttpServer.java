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
package org.jboss.webservices.integration.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Endpoint;

import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.deployers.client.plugins.deployment.AbstractDeployment;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.client.spi.DeploymentFactory;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.structure.spi.ClassLoaderFactory;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.metadata.web.jboss.JBossServletsMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.webservices.integration.util.WebMetaDataHelper;
import org.jboss.wsf.spi.SPIProvider;
import org.jboss.wsf.spi.SPIProviderResolver;
import org.jboss.wsf.spi.deployment.AbstractExtensible;
import org.jboss.wsf.spi.deployment.WSFDeploymentException;
import org.jboss.wsf.spi.http.HttpContext;
import org.jboss.wsf.spi.http.HttpContextFactory;
import org.jboss.wsf.spi.http.HttpServer;

/**
 * JAXWS HTTP server implementation that nestles inside JBoss AS.
 * This implementation simply delegates deployment of dynamically created
 * JBoss web deployment to JBoss main deployer.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class EndpointAPIHttpServer extends AbstractExtensible implements HttpServer
{

   /** JBoss deployment factory. */
   private final DeploymentFactory factory = new DeploymentFactory();
   /** JBoss Main deployer. */
   private final DeployerClient mainDeployer;
   /** JBossWS SPI provider. */
   private final SPIProvider spiProvider = SPIProviderResolver.getInstance().getProvider();
   /** Registered deployments. */
   private final Map<String, Deployment> deployments = new HashMap<String, Deployment>();

   /**
    * Constructor - invoked by MC.
    *
    * @param mainDeployer JBoss main deployer
    */
   public EndpointAPIHttpServer(final DeployerClient mainDeployer)
   {
      super();
      this.mainDeployer = mainDeployer;
   }

   /**
    * Creates an requested HTTP context.
    *
    * @param contextRoot context root name
    * @return context instance
    */
   public synchronized HttpContext createContext(final String contextRoot)
   {
      return this.spiProvider.getSPI(HttpContextFactory.class).newHttpContext(this, contextRoot);
   }

   /**
    * Publishes a JAXWS endpoint to the JBoss server.
    *
    * @param context web context
    * @param endpoint to publish
    */
   public synchronized void publish(final HttpContext context, final Endpoint endpoint)
   {
      final String contextRoot = context.getContextRoot();
      if (this.deployments.keySet().contains(contextRoot))
      {
         throw new WSFDeploymentException("Context root '" + contextRoot + "' already exists");
      }

      final Class<?> endpointClass = this.getEndpointClass(endpoint);
      final ClassLoader endpointClassLoader = endpointClass.getClassLoader();
      final Deployment deployment = this.createSimpleDeployment("http://jaxws-endpoint-api" + contextRoot);
      final MutableAttachments mutableAttachments = (MutableAttachments) deployment.getPredeterminedManagedObjects();
      final JBossWebMetaData jbossWebMD = this.newJBossWebMetaData(contextRoot, endpointClass);

      mutableAttachments.addAttachment("org.jboss.web.explicitDocBase", "/", String.class);
      mutableAttachments.addAttachment(JBossWebMetaData.class, jbossWebMD);
      mutableAttachments.addAttachment(ClassLoaderFactory.class, new ContextClassLoaderFactory(endpointClassLoader));
      mutableAttachments.addAttachment(Module.class, ClassLoading.getModuleForClassLoader(endpointClassLoader));

      try
      {
         this.mainDeployer.deploy(deployment);
         this.deployments.put(contextRoot, deployment);
      }
      catch (DeploymentException de)
      {
         WSFDeploymentException.rethrow(de);
      }
   }

   /**
    * Destroys dynamically published JAXWS endpoint on the JBoss server.
    *
    * @param context to be destroyed
    * @param endpoint to be unpublished
    */
   public synchronized void destroy(final HttpContext context, final Endpoint endpoint)
   {
      try
      {
         final String contextRoot = context.getContextRoot();
         final Deployment deployment = this.deployments.remove(contextRoot);
         if (deployment != null)
         {
            this.mainDeployer.undeploy(deployment);
         }
      }
      catch (Exception ex)
      {
         WSFDeploymentException.rethrow(ex);
      }
   }

   /**
    * Returns implementor class associated with endpoint.
    *
    * @param endpoint to get implementor class from
    * @return implementor class
    */
   private Class<?> getEndpointClass(final Endpoint endpoint)
   {
      final Object implementor = endpoint.getImplementor();
      return implementor instanceof Class<?> ? (Class<?>) implementor : implementor.getClass();
   }

   /**
    * Creates new JBoss web meta data.
    *
    * @param contextRoot context root
    * @param endpointClass endpoint class
    * @return new JBoss web meta data
    */
   private JBossWebMetaData newJBossWebMetaData(final String contextRoot, final Class<?> endpointClass)
   {
      final JBossWebMetaData jbossWebMD = new JBossWebMetaData();
      final JBossServletsMetaData servletsMD = WebMetaDataHelper.getServlets(jbossWebMD);
      final List<ServletMappingMetaData> servletMappingMD = WebMetaDataHelper.getServletMappings(jbossWebMD);
      final String servletName = "jaxws-dynamic-endpoint";

      WebMetaDataHelper.newServlet(servletName, endpointClass.getName(), servletsMD);
      WebMetaDataHelper.newServletMapping(servletName, WebMetaDataHelper.getUrlPatterns("/*"), servletMappingMD);
      jbossWebMD.setContextRoot(contextRoot);

      return jbossWebMD;
   }

   /**
    * Creates simple web deployment using deployers client api.
    *
    * @param name deployment name
    * @return new deployment
    */
   private Deployment createSimpleDeployment(final String name)
   {
      final Deployment unit = new AbstractDeployment(name);
      this.factory.addContext(unit, "");

      return unit;
   }

   /**
    * @see org.jboss.deployers.structure.spi.ClassLoaderFactory
    */
   private static class ContextClassLoaderFactory implements ClassLoaderFactory
   {
      /** Delegee. */
      private ClassLoader classLoader;

      /**
       * Constructor.
       *
       * @param classLoader class loader
       */
      public ContextClassLoaderFactory(final ClassLoader classLoader)
      {
         this.classLoader = classLoader;
      }

      /**
       * @see org.jboss.deployers.structure.spi.ClassLoaderFactory#createClassLoader(DeploymentUnit)
       *
       * @param unit deployment unit
       * @return class loader
       * @throws Exception never thrown in our case
       */
      public ClassLoader createClassLoader(final DeploymentUnit unit) throws Exception
      {
         return this.classLoader;
      }

      /**
       * @see org.jboss.deployers.structure.spi.ClassLoaderFactory#removeClassLoader(DeploymentUnit)
       *
       * @param unit deployment unit
       * @throws Exception never thrown in our case
       */
      public void removeClassLoader(final DeploymentUnit unit) throws Exception
      {
         this.classLoader = null;
      }
   }

}
